"""Package the integrated Natural tuning release from the tested lens payload."""
from pathlib import Path
import copy
import hashlib
import struct
import zipfile
import package_natural_lenses as lenses

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.56-natural5_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.56-natural5_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.57_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.57_source.zip'
NOP = 0xd503201f


def branch(start, target):
    return 0x14000000 | (((target - start) // 4) & 0x3ffffff)


def release_native(name, payload):
    result = bytearray(payload)
    if name == 'com.qti.feature2.gs.sm8650.so':
        # Remove the observation-only applied/stage stores and stage constants.
        for address in (0x155310, 0x155314, 0x155318, 0x155344, 0x155348,
                        0x155388, 0x15538c, 0x1556b8):
            struct.pack_into('<I', result, address, NOP)
        # All former diagnostic exits restore the original register context.
        # The atomic sample counter and log preparation are removed together.
        for address in range(0x1556c8, 0x15573c, 4):
            struct.pack_into('<I', result, address, NOP)
        struct.pack_into('<I', result, 0x1556c8, branch(0x1556c8, 0x15573c))
        result[0x155854:0x1558b0] = bytes(0x1558b0 - 0x155854)
    elif name == 'com.xiaomi.plugin.offlinepostproc.so':
        # Metadata update failures retain their original error and context.
        # Successful updates and nonmatching requests return without logging.
        struct.pack_into('<I', result, 0x319fc,
                         0x35000000 | (((0x31a14 - 0x319fc) // 4) << 5))
        struct.pack_into('<I', result, 0x31a00, branch(0x31a00, 0x31a6c))
        for address in range(0x31a04, 0x31a14, 4):
            struct.pack_into('<I', result, address, NOP)
        struct.pack_into('<I', result, 0x31a14, 0x528000c0)  # ANDROID_LOG_ERROR
    return bytes(result)


def main():
    with zipfile.ZipFile(BASE) as previous:
        wide = 'natural-payload/com.qti.tuned.aurora_semco_LYT900_wide.bin'
        additions = {wide: lenses.tuned_payload(previous.read(wide), wide.split('/')[-1],
                     ((0, 8, 1, 24, 31, 46, 76), (0, 8, 1, 51, 106, 46, 76)))}
        for name in ('com.qti.feature2.gs.sm8650.so', 'com.xiaomi.plugin.offlinepostproc.so'):
            member = 'natural-payload/' + name
            payload = previous.read(member)
            payload = lenses.gs_payload(payload) if name.startswith('com.qti.feature2') else lenses.mivi_payload(payload)
            additions[member] = release_native(name, payload)
        lines = []
        for line in previous.read('natural-files.list').decode('utf-8').splitlines():
            fields = line.split()
            member = 'natural-payload/' + fields[0]
            if member in additions:
                fields[-1] = hashlib.sha256(additions[member]).hexdigest()
            lines.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(lines) + '\n').encode('utf-8')
        activation = previous.read('natural-activate.sh')
        activation = activation.replace(b'/dev/aurora_natural_continuous1711', b'/dev/aurora_natural')
        activation = activation.replace(
            b"echo 'natural_continuous1711=active; scope=Natural supported rear photo modes; continuous zoom; static JPEG candidate'",
            b"echo 'natural_tuning=active'")
        activation = activation.replace(
            b"echo 'runtime_acceptance=pending; mount/hash success does not prove lens rendering'\n", b'')
        additions['natural-activate.sh'] = activation
        hashes = {}
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                if item.filename == 'runtime.sha256':
                    continue
                payload = additions.get(item.filename)
                if payload is None:
                    payload = previous.read(item.filename)
                if item.filename == 'module.prop':
                    payload = payload.replace(b'version=2.0.56-natural5\n', b'version=2.0.57\n')
                    payload = payload.replace(b'versionCode=20056\n', b'versionCode=20057\n')
                output.writestr(copy.copy(item), payload)
                hashes[item.filename] = hashlib.sha256(payload).hexdigest()
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')),
                             ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items())).encode('utf-8'))
    replacements = {'notes.md': ROOT / 'notes.md', Path(__file__).name: Path(__file__),
                    'package_natural_lenses.py': ROOT / 'package_natural_lenses.py'}
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
