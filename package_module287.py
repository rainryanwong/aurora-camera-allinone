"""Package Natural night exposure input compatibility."""
from pathlib import Path
import copy
import hashlib
import struct
import subprocess
import sys
import zipfile

sys.dont_write_bytecode = True
sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
from elftools.elf.elffile import ELFFile

ROOT = Path(__file__).resolve().parent
NDK = Path('D:/DevTools/AndroidSdk/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin')
BUILD = ROOT / 'build/module287'
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.67_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.67_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.68_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.68_source.zip'
STOCK = ROOT / 'references/NaturalNight_aiasd_stock.so'
CALL, CAVE = 0x2b720, 0x75760
SOURCE_FILES = ('package_module287.py', 'native/natural_night.S',
                'native/arcsoft-night-activate.sh',
                'references/NaturalNight_aiasd_stock.so', 'notes.md')


def night_payload():
    obj, elf = BUILD / 'night.o', BUILD / 'night.elf'
    subprocess.run([str(NDK / 'clang.exe'), '--target=aarch64-linux-android29',
                    '-c', str(ROOT / 'native/natural_night.S'), '-o', str(obj)], check=True)
    subprocess.run([str(NDK / 'ld.lld.exe'), f'-Ttext={CAVE:#x}',
                    '--entry=natural_night_bridge', '--defsym=property_get_bool=0x753b0',
                    '--defsym=original_arcsoft=0x74960', str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    data = bytearray(STOCK.read_bytes())
    with STOCK.open('rb') as stream:
        image = ELFFile(stream)
        ph = image['e_phoff'] + 2 * image['e_phentsize']
    struct.pack_into('<I', data, CALL, 0x94000000 | ((CAVE - CALL) // 4))
    data[CAVE:CAVE + len(code)] = code
    size = CAVE + len(code) - 0x1b000
    struct.pack_into('<QQ', data, ph + 32, size, size)
    print('night bridge end', hex(CAVE + len(code)), flush=True)
    return bytes(data)


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    payload = night_payload()
    activate = (ROOT / 'native/arcsoft-night-activate.sh').read_bytes().replace(
        b'@AIASD_SHA256@', hashlib.sha256(payload).hexdigest().encode())
    additions = {
        'arcsoft-night-payload/com.mi.node.aiasd.so': payload,
        'arcsoft-night-activate.sh': activate,
        'enable-natural-arcsoft-night': b'',
    }
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        hashes = {}
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = previous.read(name)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.67\n', b'version=2.0.68\n')
                data = data.replace(b'versionCode=20067\n', b'versionCode=20068\n')
            elif name == 'customize.sh':
                data = data.replace(b' natural-activate.sh ',
                                    b' natural-activate.sh arcsoft-night-activate.sh ')
            elif name == 'post-fs-data.sh':
                activation = (
                    b'# Natural night exposure input compatibility.\n'
                    b'ARCSOFT_NIGHT_RC=0\n'
                    b'/system/bin/sh "$MODDIR/arcsoft-night-activate.sh" || ARCSOFT_NIGHT_RC=$?\n'
                    b'echo "arcsoft_night_activation_exit=$ARCSOFT_NIGHT_RC; details: $MODDIR/arcsoft-night-activation.log"\n\n'
                )
                pos = data.index(b'# AUTHVignette:')
                data = data[:pos] + activation + data[pos:]
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        for name, data in additions.items():
            output.writestr(name, data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')),
                        ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items())).encode())
    replacements = {name: ROOT / name for name in SOURCE_FILES}
    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            path = replacements.pop(item.filename, None)
            output.writestr(copy.copy(item), path.read_bytes() if path else previous.read(item.filename))
        for name, path in replacements.items():
            output.write(path, name)
    for path in (OUTPUT, SOURCE):
        print(path, path.stat().st_size, hashlib.sha256(path.read_bytes()).hexdigest(), flush=True)


if __name__ == '__main__':
    main()
