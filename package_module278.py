"""Package aurora Natural HDR, night, and scene-effect tuning routes."""
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
import natural_capture_profiles as capture
import package_natural_lenses as lenses
import package_module277 as previous_routes

BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.58_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.58_source.zip'
VERSION = '2.0.59'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.59_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.59_source.zip'
BUILD = ROOT / 'build/module278'
SOURCE_FILES = ('package_module278.py', 'native/natural_capture_profiles.py',
                'native/natural_capture_match.inc', 'native/mivi_capture.S',
                'native/gs_capture.S')


def effect_node(current, values):
    node = current.root
    for typ, value in enumerate(values[1:], 1):
        effective = current.follow(node)
        packed = typ | (value << 16)
        node = next((child for child in current.children.get(effective, ())
                     if current.packed[child] == packed), None)
        if node is None:
            return None
    return current.follow(node)


def tuned_payload(blob, filename, bindings=None):
    parts = model.sections(blob)
    author_parts = model.sections((lenses.AUTHOR / filename).read_bytes())
    records = list(model.REC.iter_unpack(parts[0]))
    original_count = len(records)
    modes = list(model.MODE.iter_unpack(parts[2]))
    current = model.LookupModel(modes, records)
    author = model.LookupModel(list(model.MODE.iter_unpack(author_parts[2])),
                               list(model.REC.iter_unpack(author_parts[0])))
    sensors = tuple(sensor for sensor in capture.SENSOR_CANDIDATES
                    if (1 | (sensor << 16)) in current.packed.values())
    if bindings is None:
        bindings = tuple(capture.bindings(sensors, filename))
    for source, target in bindings:
        current.ensure_path(target, modes, records, privatize=True)
    by_node = {}
    for (node, name), record in current.modules.items():
        by_node.setdefault(node, {})[name] = record
    positions = {record[0]: index for index, record in enumerate(records)}
    for source, target in bindings:
        mid = current.ensure_path(target, modes, records)
        source_node = effect_node(current, source)
        replacements = {name: record for name, record in by_node.get(source_node, {}).items()
                        if name not in lenses.APPEARANCE}
        replacements.update((name, author.get_record(name, source)) for name in lenses.APPEARANCE)
        for name, record in replacements.items():
            previous = current.modules.get((mid, name))
            if previous is not None and previous[3:5] == record[3:5]:
                continue
            sid = previous[0] if previous is not None else len(records) + 1
            replacement = (sid, record[1], mid, record[3], record[4], sid)
            if previous is None:
                positions[sid] = len(records)
                records.append(replacement)
            else:
                records[positions[sid]] = replacement
            current.modules[(mid, name)] = replacement
    # Source SIDs identify shared, already-loaded module objects in the native
    # factories. Keep mode registrations separate without deserializing a copy
    # of the same module for each query.
    canonical = {}
    for record in records[:original_count]:
        if record[2] != 0xffffffff and record[0] == record[5]:
            canonical.setdefault((record[1], record[3], record[4]), record[0])
    for index in range(original_count, len(records)):
        record = records[index]
        if record[2] == 0xffffffff:
            continue
        source_sid = canonical.setdefault((record[1], record[3], record[4]), record[0])
        records[index] = (*record[:5], source_sid)
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


def bridge_payload(blob, kind):
    sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
    from elftools.elf.elffile import ELFFile
    mivi = kind == 'mivi'
    start = 0x31bb0 if mivi else 0x1558b0
    site = 0x317e8 if mivi else 0x15545c
    source = ROOT / 'native' / ('mivi_capture.S' if mivi else 'gs_capture.S')
    obj, elf = BUILD / f'{kind}.o', BUILD / f'{kind}.elf'
    subprocess.run([str(lenses.NDK / 'clang.exe'), '--target=aarch64-linux-android29',
                    '-I', str(BUILD), '-I', str(ROOT / 'native'), '-c', str(source),
                    '-o', str(obj)], check=True)
    symbols = ('--defsym=copy_query=0x319b4', '--defsym=original_sensor=0x317ec') if mivi else (
        '--defsym=store_effect=0x1556b4', '--defsym=original_gs_sensor=0x155460')
    subprocess.run([str(lenses.NDK / 'ld.lld.exe'), f'-Ttext={start:#x}',
                    '--entry=' + ('lens_profile' if mivi else 'gs_lens_profile'),
                    *symbols, str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    result = bytearray(blob)
    result[start:start + len(code)] = code
    struct.pack_into('<I', result, site, 0x14000000 | ((start - site) // 4))
    image = ELFFile(io.BytesIO(blob))
    for index, segment in enumerate(image.iter_segments()):
        if segment['p_type'] == 'PT_LOAD' and segment['p_flags'] == 5:
            size = start + len(code) - segment['p_vaddr']
            header = image['e_phoff'] + index * image['e_phentsize']
            struct.pack_into('<QQ', result, header + 32, size, size)
    print(kind, 'code end', hex(start + len(code)), flush=True)
    return bytes(result)


def write_bridge_tables():
    BUILD.mkdir(parents=True, exist_ok=True)
    masks = ''.join(f'    .quad {sum(1 << scene for scene in scenes):#x}\n'
                    for scenes in capture.SCENES)
    (BUILD / 'capture_scenes.inc').write_text(masks, encoding='utf-8')
    rows = ''.join('    .byte ' + ', '.join(map(str, row)) + '\n' for row in capture.PROFILES)
    (BUILD / 'capture_profiles.inc').write_text(rows + '    .byte 255\n', encoding='utf-8')
    old_queries = lenses.BRIDGE_QUERIES + previous_routes.BRIDGE_ADDITIONS
    for filename, queries in (
            ('lens_queries.inc', old_queries),
            ('gs_queries.inc', tuple(q for q in old_queries if q[3:5] != (36, 61)))):
        entries = tuple((q, 76) for q in queries) + tuple(
            (source, target[6]) for source, target in capture.EXACT_BINDINGS + capture.EXACT_ROUTES)
        content = ''.join('    .byte ' + ', '.join(map(str, q + (effect,))) + '\n'
                          for q, effect in entries)
        (BUILD / filename).write_text(content + '    .byte 255\n', encoding='utf-8')


def main():
    write_bridge_tables()
    with zipfile.ZipFile(BASE) as previous:
        additions = {}
        for item in previous.infolist():
            if item.filename.startswith('natural-payload/com.qti.tuned.') and item.filename.endswith('.bin'):
                print('tuning', item.filename, flush=True)
                additions[item.filename] = tuned_payload(previous.read(item.filename), Path(item.filename).name)
        for kind, name in (('mivi', 'com.xiaomi.plugin.offlinepostproc.so'),
                           ('gs', 'com.qti.feature2.gs.sm8650.so')):
            member = 'natural-payload/' + name
            additions[member] = bridge_payload(previous.read(member), kind)
        lines = []
        for line in previous.read('natural-files.list').decode('utf-8').splitlines():
            fields = line.split()
            fields[-1] = hashlib.sha256(additions['natural-payload/' + fields[0]]).hexdigest()
            lines.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(lines) + '\n').encode('utf-8')
        hashes = {}
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                if item.filename == 'runtime.sha256':
                    continue
                payload = additions.get(item.filename)
                if payload is None:
                    payload = previous.read(item.filename)
                if item.filename == 'module.prop':
                    payload = payload.replace(b'version=2.0.58\n', f'version={VERSION}\n'.encode())
                    payload = payload.replace(b'versionCode=20058\n',
                                              f'versionCode={20000 + int(VERSION.split(".")[-1])}\n'.encode())
                output.writestr(copy.copy(item), payload)
                hashes[item.filename] = hashlib.sha256(payload).hexdigest()
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')),
                            ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items())).encode('utf-8'))
    replacements = {name: ROOT / name for name in SOURCE_FILES}
    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            path = replacements.pop(item.filename, None)
            output.writestr(copy.copy(item), path.read_bytes() if path else previous.read(item.filename))
        for name, path in replacements.items():
            output.write(path, name)
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
