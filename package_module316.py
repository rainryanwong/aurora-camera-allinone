"""Package the Natural execution-path optimizations in All in One 2.1.1."""

import hashlib
import io
import struct
import subprocess
import tempfile
from pathlib import Path

import build_natural_preview as preview
from package_module310 import update_natural_metadata
from package_module315 import HELP, OUTPUT as BASE, RELEASE, ROOT, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.88.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.1_Stock1770_Compat0288_LiveMotion053_AUTH.zip'
BASE_GS_SHA256 = '633d01050a8b60cb3eec0d7ad0a5243c0d663cc9fbee291eea7292dd33861061'
SKY_ENTRY = 0x155cc4
SKY_TABLE = 0x155f00
CAPTURE_ENTRY = 0x1558b0
CAPTURE_GATE = 0x155e64  # Former Scene table, superseded in 2.0.100.
EXACT_CAPTURE_TABLE = 0x155ab0
RUNTIME_FILES = {
    'natural-activate.sh': 'runtime306/natural-activate.sh',
    'arcsoft-night-activate.sh': 'runtime307/arcsoft-night-activate.sh',
    'webroot/diagnostics.sh': 'webroot/diagnostics.sh',
    'webroot/preview-diagnostics.sh': 'webroot/preview-diagnostics.sh',
    HELP: HELP,
}


def packed_rows(blob, start, stride):
    end = start
    while blob[end] != 255:
        end += stride
    return [tuple(blob[pos:pos + stride]) for pos in range(start, end, stride)], end + 1


def fast_sections(short_routes, sensor_mask):
    with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
        work = Path(directory)
        rows = ''.join(f'    .byte {", ".join(map(str, row))}\n' for row in short_routes)
        (work / 'gs_natural_short.inc').write_text(rows + '    .byte 255\n', encoding='utf-8')
        obj = work / 'gs.o'
        elf = work / 'gs.elf'
        subprocess.run([str(preview.NDK / 'clang.exe'), '--target=aarch64-linux-android29',
                        '-I', str(work), f'-DSCENE42_SENSOR_MASK={sensor_mask:#x}', '-c',
                        str(ROOT / 'native/gs_natural_fast.S'), '-o', str(obj)], check=True)
        subprocess.run([str(preview.NDK / 'ld.lld.exe'), f'-Ttext={SKY_TABLE:#x}',
                        f'--section-start=.capture={CAPTURE_GATE:#x}', '--entry=natural_scene',
                        '--defsym=photo_scene=0x155d1c', '--defsym=store_effect=0x1556b4',
                        '--defsym=capture_profile=0x1558f8',
                        f'--defsym=exact_capture_table={EXACT_CAPTURE_TABLE:#x}',
                        '--defsym=exact_capture_loop=0x1558b4', str(obj), '-o', str(elf)], check=True)
        with elf.open('rb') as stream:
            image = preview.ELFFile(stream)
            return tuple((section['sh_addr'], section.data()) for section in
                         (image.get_section_by_name('.text'), image.get_section_by_name('.capture')))


def gs_routes(blob):
    if hashlib.sha256(blob).hexdigest() != BASE_GS_SHA256:
        raise ValueError('Expected the All in One 2.1.0 GS library')
    sky_rows, sky_end = packed_rows(blob, SKY_TABLE, 4)
    if [row for row in sky_rows if row[1] == 42] != list(preview.PHOTO_SCENE42_ROUTES):
        raise ValueError('Unexpected Scene 42 routes')
    short_routes = [row for row in sky_rows if row[1] != 42]
    if any(row[1] not in (4, 45) for row in short_routes):
        raise ValueError('Unexpected legacy scene')
    capture_rows, _ = packed_rows(blob, EXACT_CAPTURE_TABLE, 8)
    if any(row[2] != 1 for row in capture_rows):
        raise ValueError('Capture table contains a non-capture request')
    mask = sum(1 << sensor for sensor in {row[0] for row in preview.PHOTO_SCENE42_ROUTES})
    patched = bytearray(blob)
    for (address, code), (start, end) in zip(fast_sections(short_routes, mask),
                                             ((SKY_TABLE, sky_end), (CAPTURE_GATE, 0x155ebf))):
        if address != start or address + len(code) > end:
            raise ValueError(f'GS code would overwrite occupied bytes at {address:#x}')
        patched[address:end] = code.ljust(end - address, b'\0')
    for site, target in ((SKY_ENTRY, SKY_TABLE), (CAPTURE_ENTRY, CAPTURE_GATE)):
        jump = 0x14000000 | (((target - site) // 4) & 0x03ffffff)
        struct.pack_into('<I', patched, site, jump)
    return patched


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.0\nversionCode=20101\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.0 as the packaging input')
    path = f'natural-payload/{preview.GS}'
    updates = {name: (ROOT / source).read_bytes() for name, source in RUNTIME_FILES.items()}
    updates[path] = gs_routes(previous.read(path))
    updates['module.prop'] = metadata.replace(version, b'version=2.1.1\nversionCode=20102\n', 1)
    updates['camera-compat.apk'] = COMPAT.read_bytes()
    update_natural_metadata(previous, updates)
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
