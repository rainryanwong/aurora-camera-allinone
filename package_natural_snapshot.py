"""Build the scoped MIVI snapshot selection update from natural1."""
from pathlib import Path
import copy
import hashlib
import importlib.util
import io
import struct
import subprocess
import sys
import zipfile
import zlib

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.56-natural1_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.56-natural1_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.56-natural3_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.56-natural3_source.zip'
AUTHOR = Path('D:/cam18pm/analysis/natural-tuning-v140-20261013/upstream-ksu/payload')
MODEL = ROOT / 'native/natural_lookup_model.py'
NDK = Path('D:/DevTools/AndroidSdk/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin')
BUILD = ROOT / 'build/natural3'
WIDE = 'com.qti.tuned.aurora_semco_LYT900_wide.bin'
MIVI = 'com.xiaomi.plugin.offlinepostproc.so'
QUERIES = tuple((0, 9, 1, f1, f2, scene, 76)
                for scene in (0, 32)
                for f1, f2 in ((36, 85), (36, 86), (51, 107)))
APPEARANCE = (b'cc14_ipe_v2', b'cv12_ipe_v2', b'gamma15_ipe_v2',
              b'ltm202_ipe_v2', b'tdl11_ipe_v2')


def wide_payload(blob):
    spec = importlib.util.spec_from_file_location('natural_lookup', MODEL)
    model = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(model)
    parts = model.sections(blob)
    donor = model.sections((AUTHOR / WIDE).read_bytes())
    records = list(model.REC.iter_unpack(parts[0]))
    modes = list(model.MODE.iter_unpack(parts[2]))
    current = model.LookupModel(modes, records)
    author = model.LookupModel(list(model.MODE.iter_unpack(donor[2])),
                               list(model.REC.iter_unpack(donor[0])))
    # The coexistence file already retains the complete author data section and
    # referenced symbols. Add only leaf registrations, including author's keeps.
    for query in QUERIES:
        current.ensure_path(query, modes, records, privatize=True)
    for query in QUERIES:
        mid = current.ensure_path(query, modes, records)
        for name in APPEARANCE:
            source = author.get_record(name, query[:6] + (0,))
            if current.get(name, query) == source[3:5]:
                continue
            previous = current.modules.get((mid, name))
            if previous is not None:
                # A private leaf can inherit a copied registration. Replace it
                # in place because the loader takes the first name at a node.
                sid = previous[0]
                record = (sid, source[1], mid, source[3], source[4], sid)
                records[sid - 1] = record
            else:
                sid = len(records) + 1
                record = (sid, source[1], mid, source[3], source[4], sid)
                records.append(record)
            current.modules[(mid, name)] = record
    sections = (b''.join(model.REC.pack(*r) for r in records), parts[1],
                b''.join(model.MODE.pack(*r) for r in modes))
    header = bytearray(blob[:204])
    offset = 204
    for index, data in enumerate(sections):
        struct.pack_into('<III', header, 168 + 12 * index, index, offset, len(data))
        offset += len(data)
    struct.pack_into('<I', header, 28, offset)
    body = header + b''.join(sections)
    return bytes(body) + struct.pack('<I', zlib.crc32(body))


def mivi_payload(blob):
    sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
    from elftools.elf.elffile import ELFFile
    BUILD.mkdir(parents=True, exist_ok=True)
    obj, elf = BUILD / 'snapshot.o', BUILD / 'snapshot.elf'
    subprocess.run([str(NDK / 'clang.exe'), '--target=aarch64-linux-android29',
                    '-c', str(ROOT / 'native/mivi_snapshot.S'), '-o', str(obj)], check=True)
    subprocess.run([str(NDK / 'ld.lld.exe'), '-Ttext=0x31b60',
                    '--entry=snapshot_profile', '--defsym=copy_query=0x319b4',
                    '--defsym=original_matcher=0x31810', str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    result = bytearray(blob)
    result[0x31b60:0x31b60 + len(code)] = code
    struct.pack_into('<I', result, 0x3180c, 0x14000000 | ((0x31b60 - 0x3180c) // 4))
    image = ELFFile(io.BytesIO(blob))
    for index, segment in enumerate(image.iter_segments()):
        if segment['p_type'] == 'PT_LOAD' and segment['p_flags'] == 5:
            size = 0x31b60 + len(code) - segment['p_vaddr']
            header = image['e_phoff'] + index * image['e_phentsize']
            struct.pack_into('<QQ', result, header + 32, size, size)
    return bytes(result)


def main():
    with zipfile.ZipFile(BASE) as base:
        additions = {'natural-payload/' + WIDE: wide_payload(base.read('natural-payload/' + WIDE)),
                     'natural-payload/' + MIVI: mivi_payload(base.read('natural-payload/' + MIVI))}
        rows = []
        for line in base.read('natural-files.list').decode().splitlines():
            fields = line.split()
            name = 'natural-payload/' + fields[0]
            if name in additions:
                fields[-1] = hashlib.sha256(additions[name]).hexdigest()
            rows.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(rows) + '\n').encode()
        hashes = {}
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in base.infolist():
                if item.filename == 'runtime.sha256':
                    continue
                payload = additions.get(item.filename)
                if payload is None:
                    payload = base.read(item.filename)
                if item.filename == 'module.prop':
                    payload = payload.replace(b'version=2.0.56-natural1\n', b'version=2.0.56-natural3\n')
                output.writestr(copy.copy(item), payload)
                hashes[item.filename] = hashlib.sha256(payload).hexdigest()
            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(base.getinfo('runtime.sha256')), manifest.encode())
    with zipfile.ZipFile(BASE_SOURCE) as base, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in base.infolist():
            output.writestr(copy.copy(item), base.read(item.filename))
        for path in (Path(__file__), ROOT / 'native/mivi_snapshot.S', MODEL):
            output.write(path, path.relative_to(ROOT).as_posix())
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
