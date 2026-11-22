#include <elf.h>

#include <algorithm>
#include <cstring>
#include <fstream>
#include <iostream>
#include <iterator>
#include <stdexcept>
#include <string>
#include <string_view>
#include <vector>

#define JSMN_STATIC
#define JSMN_STRICT
#include "jsmn.h"

namespace {
std::string read_file(const char* path) {
    std::ifstream input(path, std::ios::binary);
    if (!input) throw std::runtime_error("cannot open input");
    return {std::istreambuf_iterator<char>(input), {}};
}

class Graph {
public:
    explicit Graph(const std::string& source) : source(source) {
        jsmn_parser parser;
        jsmn_init(&parser);
        const int count = jsmn_parse(&parser, source.data(), source.size(), nullptr, 0);
        if (count < 1) throw std::runtime_error("invalid camera graph");
        tokens.resize(count);
        jsmn_init(&parser);
        if (jsmn_parse(&parser, source.data(), source.size(), tokens.data(), tokens.size()) != count
                || tokens[0].type != JSMN_OBJECT || next(0) != count) {
            throw std::runtime_error("invalid camera graph");
        }
    }

    int member(int object, std::string_view name) const {
        if (object < 0 || tokens[object].type != JSMN_OBJECT) return -1;
        for (int key = object + 1; key < next(object); key = next(key + 1)) {
            if (string(key) == name) return key + 1;
        }
        return -1;
    }

    std::vector<int> items(int array) const {
        if (array < 0 || tokens[array].type != JSMN_ARRAY) {
            throw std::runtime_error("camera graph node or link list missing");
        }
        std::vector<int> result;
        for (int item = array + 1, end = next(array); item < end; item = next(item)) {
            result.push_back(item);
        }
        return result;
    }

    std::string_view raw(int token) const {
        if (token < 0) return {};
        const auto& span = tokens[token];
        return std::string_view(source).substr(span.start, span.end - span.start);
    }

    std::string_view string(int token) const {
        return token >= 0 && tokens[token].type == JSMN_STRING ? raw(token) : std::string_view{};
    }

    int begin(int token) const { return tokens[token].start; }
    int end(int token) const { return tokens[token].end; }

private:
    int next(int token) const {
        const auto limit = static_cast<int>(tokens.size());
        int next = token + 1;
        while (next < limit && tokens[next].start < tokens[token].end) ++next;
        return next;
    }

    const std::string& source;
    std::vector<jsmntok_t> tokens;
};

bool supports_nv12(const Graph& graph, int port) {
    const int format = graph.member(port, "PortFormat");
    return format < 0 || graph.string(format) == "YUV420_NV12";
}

std::string nv12_port(const char* instance) {
    return std::string(R"({"PortId":0,"NodeInstance":")") + instance
            + R"(","PortFormat":"YUV420_NV12","BufferType":"Gralloc"})";
}

std::string patch_m3(const std::string& input) {
    const Graph graph(input);
    const int nodes = graph.member(graph.member(0, "NodesList"), "Node");
    const int links = graph.member(graph.member(0, "PortLinkages"), "Link");
    std::vector<std::string_view> filters;
    std::vector<std::string_view> watermarks;
    for (int node : graph.items(nodes)) {
        const auto name = graph.string(graph.member(node, "NodeName"));
        const auto instance = graph.string(graph.member(node, "NodeInstance"));
        if (instance == "M3Style" || instance == "M3Grain"
                || name == "com.xiaomi.plugin.m3style" || name == "com.xiaomi.plugin.m3grain") {
            throw std::runtime_error("M3 nodes already present in camera graph");
        }
        if (instance.empty()) continue;
        if (name == "com.xiaomi.plugin.filter") filters.push_back(instance);
        if (name == "com.xiaomi.plugin.watermark") watermarks.push_back(instance);
    }

    int source = -1;
    for (int link : graph.items(links)) {
        const int src = graph.member(link, "SrcPort");
        const int dst = graph.member(link, "DstPort");
        const auto from = graph.string(graph.member(src, "NodeInstance"));
        const auto to = graph.string(graph.member(dst, "NodeInstance"));
        if (std::find(filters.begin(), filters.end(), from) == filters.end()
                || std::find(watermarks.begin(), watermarks.end(), to) == watermarks.end()
                || graph.raw(graph.member(src, "PortId")) != "0"
                || graph.raw(graph.member(dst, "PortId")) != "0"
                || graph.string(graph.member(src, "BufferType")) != "Gralloc"
                || !supports_nv12(graph, src) || !supports_nv12(graph, dst)) continue;
        const int dst_buffer = graph.member(dst, "BufferType");
        if (dst_buffer >= 0 && graph.string(dst_buffer) != "Gralloc") continue;
        if (source >= 0) throw std::runtime_error("ambiguous filter-to-watermark link");
        source = src;
    }
    if (source < 0) throw std::runtime_error("required filter-to-watermark port 0 link missing");

    const auto style = nv12_port("M3Style");
    const auto grain = nv12_port("M3Grain");
    const auto new_links = std::string(R"(,{"SrcPort":)") + std::string(graph.raw(source))
            + R"(,"DstPort":)" + style + R"(},{"SrcPort":)" + style + R"(,"DstPort":)" + grain + "}";
    const char* new_nodes = R"(,{"NodeName":"com.xiaomi.plugin.m3style","NodeInstance":"M3Style"},{"NodeName":"com.xiaomi.plugin.m3grain","NodeInstance":"M3Grain"})";
    struct Edit { int position; int length; std::string_view text; };
    Edit edits[] = {
        {graph.end(nodes) - 1, 0, new_nodes},
        {graph.begin(source), graph.end(source) - graph.begin(source), grain},
        {graph.end(links) - 1, 0, new_links},
    };
    std::sort(std::begin(edits), std::end(edits), [](const Edit& a, const Edit& b) {
        return a.position > b.position;
    });
    auto patched = input;
    for (const auto& edit : edits) patched.replace(edit.position, edit.length, edit.text);
    return patched;
}

bool within(std::string_view bytes, uint64_t offset, uint64_t length) {
    return offset <= bytes.size() && length <= bytes.size() - offset;
}

template<typename T>
T elf_entry(std::string_view bytes, uint64_t offset) {
    if (!within(bytes, offset, sizeof(T))) throw std::runtime_error("truncated ELF table");
    T entry;
    std::memcpy(&entry, bytes.data() + offset, sizeof(T));
    return entry;
}

bool exports_function(std::string_view bytes, std::string_view name) {
    const auto elf = elf_entry<Elf64_Ehdr>(bytes, 0);
    if (bytes.substr(0, SELFMAG) != std::string_view(ELFMAG, SELFMAG)
            || elf.e_ident[EI_CLASS] != ELFCLASS64 || elf.e_ident[EI_DATA] != ELFDATA2LSB
            || elf.e_machine != EM_AARCH64 || elf.e_type != ET_DYN
            || elf.e_shentsize != sizeof(Elf64_Shdr)
            || !within(bytes, elf.e_shoff, uint64_t(elf.e_shnum) * elf.e_shentsize)) return false;

    for (unsigned i = 0; i < elf.e_shnum; ++i) {
        const auto symbols = elf_entry<Elf64_Shdr>(bytes, elf.e_shoff + i * elf.e_shentsize);
        if (symbols.sh_type != SHT_DYNSYM) continue;
        if (symbols.sh_link >= elf.e_shnum || symbols.sh_entsize != sizeof(Elf64_Sym)
                || !within(bytes, symbols.sh_offset, symbols.sh_size)) return false;
        const auto strings = elf_entry<Elf64_Shdr>(bytes, elf.e_shoff + symbols.sh_link * elf.e_shentsize);
        if (strings.sh_type != SHT_STRTAB || !within(bytes, strings.sh_offset, strings.sh_size)) return false;
        const auto names = bytes.substr(strings.sh_offset, strings.sh_size);
        for (uint64_t offset = 0; offset + sizeof(Elf64_Sym) <= symbols.sh_size; offset += sizeof(Elf64_Sym)) {
            const auto symbol = elf_entry<Elf64_Sym>(bytes, symbols.sh_offset + offset);
            const auto bind = ELF64_ST_BIND(symbol.st_info);
            const auto visibility = symbol.st_other & 0x03;
            if (symbol.st_shndx != SHN_UNDEF && ELF64_ST_TYPE(symbol.st_info) == STT_FUNC
                    && (bind == STB_GLOBAL || bind == STB_WEAK)
                    && (visibility == STV_DEFAULT || visibility == STV_PROTECTED)
                    && within(names, symbol.st_name, name.size() + 1)
                    && names.substr(symbol.st_name, name.size()) == name
                    && names[symbol.st_name + name.size()] == '\0') return true;
        }
    }
    return false;
}
}

int main(int argc, char** argv) {
    const std::string_view command = argc > 1 ? argv[1] : "";
    try {
        if (command == "m3-graph" && argc == 4) {
            const auto patched = patch_m3(read_file(argv[2]));
            std::ofstream output(argv[3], std::ios::binary);
            output << patched;
            output.close();
            if (!output) throw std::runtime_error("cannot write generated camera graph");
            return 0;
        }
        if ((command == "filter-factory" || command == "arcsoft-night") && argc == 3) {
            const char* symbol = command == "filter-factory"
                    ? "_ZN16ProcessorCreator12createFilterEN12videoprocess11VideoFormatEb"
                    : "ARC_SND_GetEVList";
            if (!exports_function(read_file(argv[2]), symbol)) {
                throw std::runtime_error(std::string(symbol) + " export missing");
            }
            return 0;
        }
        std::cerr << "Usage: odm-compat m3-graph SOURCE DEST | filter-factory LIBRARY | arcsoft-night LIBRARY\n";
    } catch (const std::exception& error) {
        std::cerr << (argc > 2 ? argv[2] : "odm-compat") << ": " << error.what() << '\n';
    }
    return 1;
}
