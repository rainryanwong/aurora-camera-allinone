"""Package Compat 0.1.98 over All in One 2.0.70."""
from pathlib import Path
import copy
import hashlib
import zipfile

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.70_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.70_source.zip'
APK = ROOT / 'release/CameraCompat-0.1.98.apk'
COMPAT_SOURCE = ROOT / 'release/CameraCompat-0.1.98_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.71_Stock1770_Compat0198_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.71_source.zip'


def main():
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = APK.read_bytes() if name == 'camera-compat.apk' else previous.read(name)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.70\n', b'version=2.0.71\n')
                data = data.replace(b'versionCode=20070\n', b'versionCode=20071\n')
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')),
                        ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items())).encode('utf-8'))

    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(COMPAT_SOURCE) as compat, \
            zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        replacements = set(compat.namelist())
        for item in previous.infolist():
            name = item.filename
            data = compat.read(name) if name in replacements else previous.read(name)
            output.writestr(name, data)
            replacements.discard(name)
        for name in sorted(replacements):
            output.writestr(name, compat.read(name))
        output.write(ROOT / 'package_module290.py', 'package_module290.py')

    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
