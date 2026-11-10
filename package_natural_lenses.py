"""Extend NaturalTuning private registrations for captured lens requests."""
from pathlib import Path
import copy
import hashlib
import io
import struct
import subprocess
import sys
import zipfile
import zlib

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT / 'native'))
import natural_lookup_model as model

VERSION = 'natural6'
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.56-natural3_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.56-natural3_source.zip'
OUTPUT = ROOT / f'release/camera68_aurora_allinone_v2.0.56-{VERSION}_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / f'release/CameraCompat_2.0.56-{VERSION}_source.zip'
AUTHOR = Path('D:/cam18pm/analysis/natural-tuning-v140-20261013/upstream-ksu/payload')
NDK = Path('D:/DevTools/AndroidSdk/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin')
BUILD = ROOT / 'build' / VERSION
APPEARANCE = (b'cc14_ipe_v2', b'cv12_ipe_v2', b'gamma15_ipe_v2', b'ltm202_ipe_v2', b'tdl11_ipe_v2')
QUERIES = {
    'com.qti.tuned.aurora_semco_LYT900_wide.bin': (
        (0, 53, 1, 24, 31, 0, 76),
        (0, 53, 1, 51, 106, 0, 76),
        (0, 8, 1, 51, 105, 46, 76),
        (0, 8, 1, 24, 31, 46, 76),
        (0, 8, 1, 51, 106, 46, 76),
    ),
    'com.qti.tuned.aurora_semco_imx858_ultra.bin': (
        (0, 1, 1, 36, 86, 0, 76),
        (0, 1, 1, 51, 107, 0, 76),
    ),
    'com.qti.tuned.aurora_ofilm_imx858_tele3x.bin': (
        (0, 1, 1, 24, 31, 0, 76),
        (0, 1, 1, 51, 106, 0, 76),
    ),
    'com.qti.tuned.aurora_semco_imx858_tele5x.bin': (
        (0, 1, 1, 51, 105, 0, 76),
        (0, 12, 1, 51, 105, 0, 76),
        (0, 16, 1, 51, 105, 0, 76),
        (0, 16, 1, 51, 105, 46, 76),
    ),
}
BRIDGE_QUERIES = ((0, 8, 1, 24, 31, 46, 0), (0, 53, 1, 24, 31, 0, 0), (0, 1, 1, 36, 61, 0, 0), (0, 1, 1, 24, 31, 0, 0))


def tuned_payload(blob, filename, queries):
    parts = model.sections(blob)
    donor = model.sections((AUTHOR / filename).read_bytes())
    records = list(model.REC.iter_unpack(parts[0]))
    modes = list(model.MODE.iter_unpack(parts[2]))
    current = model.LookupModel(modes, records)
    author = model.LookupModel(list(model.MODE.iter_unpack(donor[2])), list(model.REC.iter_unpack(donor[0])))
    for query in queries:
        current.ensure_path(query, modes, records, privatize=True)
    positions = {record[0]: index for index, record in enumerate(records)}
    for query in queries:
        mid = current.ensure_path(query, modes, records)
        for name in APPEARANCE:
            source = author.get_record(name, query[:6] + (0,))
            if current.get(name, query) == source[3:5]:
                continue
            previous = current.modules.get((mid, name))
            sid = previous[0] if previous is not None else len(records) + 1
            record = (sid, source[1], mid, source[3], source[4], sid)
            if previous is None:
                positions[sid] = len(records)
                records.append(record)
            else:
                records[positions[sid]] = record
            current.modules[(mid, name)] = record
    sections = (b''.join(model.REC.pack(*r) for r in records), parts[1], b''.join(model.MODE.pack(*r) for r in modes))
    header = bytearray(blob[:204])
    offset = 204
    for index, data in enumerate(sections):
        struct.pack_into('<III', header, 168 + 12 * index, index, offset, len(data))
        offset += len(data)
    struct.pack_into('<I', header, 28, offset)
    body = header + b''.join(sections)
    return bytes(body) + struct.pack('<I', zlib.crc32(body))


def mivi_payload(blob, queries=BRIDGE_QUERIES):
    sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
    from elftools.elf.elffile import ELFFile
    BUILD.mkdir(parents=True, exist_ok=True)
    (BUILD / 'lens_queries.inc').write_text(''.join('    .byte ' + ', '.join(map(str, q + (0,))) + '\n' for q in queries) + '    .byte 255\n', encoding='utf-8')
    obj, elf = BUILD / 'lenses.o', BUILD / 'lenses.elf'
    subprocess.run([str(NDK / 'clang.exe'), '--target=aarch64-linux-android29', '-I', str(BUILD), '-c', str(ROOT / 'native/mivi_lenses.S'), '-o', str(obj)], check=True)
    subprocess.run([str(NDK / 'ld.lld.exe'), '-Ttext=0x31bb0', '--entry=lens_profile', '--defsym=copy_query=0x319b4', '--defsym=original_sensor=0x317ec', str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    result = bytearray(blob)
    result[0x31bb0:0x31bb0 + len(code)] = code
    struct.pack_into('<I', result, 0x317e8, 0x14000000 | ((0x31bb0 - 0x317e8) // 4))
    image = ELFFile(io.BytesIO(blob))
    for index, segment in enumerate(image.iter_segments()):
        if segment['p_type'] == 'PT_LOAD' and segment['p_flags'] == 5:
            size = 0x31bb0 + len(code) - segment['p_vaddr']
            header = image['e_phoff'] + index * image['e_phentsize']
            struct.pack_into('<QQ', result, header + 32, size, size)
    return bytes(result)


def gs_payload(blob, queries=None):
    sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
    from elftools.elf.elffile import ELFFile
    BUILD.mkdir(parents=True, exist_ok=True)
    if queries is None:
        queries = ((0, 8, 1, 24, 31, 46, 0), (0, 53, 1, 24, 31, 0, 0), (0, 1, 1, 24, 31, 0, 0))
    (BUILD / 'gs_queries.inc').write_text(''.join('    .byte ' + ', '.join(map(str, q + (0,))) + '\n' for q in queries) + '    .byte 255\n', encoding='utf-8')
    obj, elf = BUILD / 'gs_lenses.o', BUILD / 'gs_lenses.elf'
    subprocess.run([str(NDK / 'clang.exe'), '--target=aarch64-linux-android29', '-I', str(BUILD), '-c', str(ROOT / 'native/gs_lenses.S'), '-o', str(obj)], check=True)
    subprocess.run([str(NDK / 'ld.lld.exe'), '-Ttext=0x1558b0', '--entry=gs_lens_profile', '--defsym=store_effect=0x1556b4', '--defsym=original_gs_sensor=0x155460', str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    result = bytearray(blob)
    result[0x1558b0:0x1558b0 + len(code)] = code
    struct.pack_into('<I', result, 0x15545c, 0x14000000 | ((0x1558b0 - 0x15545c) // 4))
    image = ELFFile(io.BytesIO(blob))
    for index, segment in enumerate(image.iter_segments()):
        if segment['p_type'] == 'PT_LOAD' and segment['p_flags'] == 5:
            size = 0x1558b0 + len(code) - segment['p_vaddr']
            header = image['e_phoff'] + index * image['e_phentsize']
            struct.pack_into('<QQ', result, header + 32, size, size)
    return bytes(result)


def main():
    with zipfile.ZipFile(BASE) as base:
        additions = {'natural-payload/' + name: tuned_payload(base.read('natural-payload/' + name), name, queries) for name, queries in QUERIES.items()}
        mivi = 'natural-payload/com.xiaomi.plugin.offlinepostproc.so'
        additions[mivi] = mivi_payload(base.read(mivi))
        gs = 'natural-payload/com.qti.feature2.gs.sm8650.so'
        additions[gs] = gs_payload(base.read(gs))
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
                    payload = payload.replace(b'version=2.0.56-natural3\n', f'version=2.0.56-{VERSION}\n'.encode())
                output.writestr(copy.copy(item), payload)
                hashes[item.filename] = hashlib.sha256(payload).hexdigest()
            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(base.getinfo('runtime.sha256')), manifest.encode())
    with zipfile.ZipFile(BASE_SOURCE) as base, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in base.infolist():
            output.writestr(copy.copy(item), base.read(item.filename))
        for path in (Path(__file__), ROOT / 'native/mivi_lenses.S', ROOT / 'native/gs_lenses.S'):
            output.write(path, path.relative_to(ROOT).as_posix())
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
