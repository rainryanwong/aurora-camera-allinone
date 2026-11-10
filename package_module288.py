"""Package Natural face gain-map tuning for observed rear SR requests."""
from pathlib import Path
import copy
import hashlib
import sys
import zipfile

sys.dont_write_bytecode = True
import package_module278 as capture

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.68_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.68_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.69_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.69_source.zip'
SOURCE_FILES = ('package_module288.py', 'package_module278.py',
                'native/natural_capture_profiles.py', 'notes.md')


def main():
    with zipfile.ZipFile(BASE) as previous:
        additions = {}
        for filename in capture.capture.LENS_BINDINGS:
            member = 'natural-payload/' + filename
            print('tuning', filename, flush=True)
            additions[member] = capture.tuned_payload(
                previous.read(member), filename, capture.capture.LENS_BINDINGS[filename])
        lines = []
        for line in previous.read('natural-files.list').decode('utf-8').splitlines():
            fields = line.split()
            member = 'natural-payload/' + fields[0]
            if member in additions:
                fields[-1] = hashlib.sha256(additions[member]).hexdigest()
            lines.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(lines) + '\n').encode('utf-8')
        hashes = {}
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                name = item.filename
                if name == 'runtime.sha256':
                    continue
                data = additions[name] if name in additions else previous.read(name)
                if name == 'module.prop':
                    data = data.replace(b'version=2.0.68\n', b'version=2.0.69\n')
                    data = data.replace(b'versionCode=20068\n', b'versionCode=20069\n')
                output.writestr(copy.copy(item), data)
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
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
