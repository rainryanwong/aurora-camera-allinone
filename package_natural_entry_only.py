"""Build the natural-entry variant from All in One 2.0.56."""
from pathlib import Path
import copy
import hashlib
import zipfile

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.56_Stock1770_Compat0195_Native226_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.56_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.56-entry1_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.56-entry1_source.zip'
REMOVED = {
    'natural-activate.sh', 'natural-files.list', 'bin/natural-remount-ro',
    'arcsoft-night-activate.sh', 'enable-natural-arcsoft-night',
    'hdr-face-activate.sh', 'enable-natural-hdr-face',
}
REMOVED_PREFIXES = ('natural-payload/', 'arcsoft-night-payload/', 'hdr-face-payload/')


def main():
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256' or name in REMOVED or name.startswith(REMOVED_PREFIXES):
                continue
            payload = previous.read(name)
            if name == 'module.prop':
                payload = payload.replace(b'version=2.0.56\n', b'version=2.0.56-entry1\n')
            elif name == 'customize.sh':
                payload = payload.replace(b' natural-activate.sh ', b' ')
                payload = payload.replace(b'set_perm "$MODPATH/bin/natural-remount-ro" 0 0 0755\n', b'')
            elif name == 'post-fs-data.sh':
                start = payload.index(b'# Independent activators run before provider startup.\n')
                end = payload.index(b'# AUTHVignette:', start)
                payload = payload[:start] + payload[end:]
            output.writestr(copy.copy(item), payload)
            hashes[name] = hashlib.sha256(payload).hexdigest()
        manifest = ''.join(f'{value}  {name}\n' for name, value in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))
    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            output.writestr(copy.copy(item), previous.read(item.filename))
        output.write(Path(__file__), Path(__file__).name)
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(f'{path}\nbytes={path.stat().st_size}\nsha256={digest}')


if __name__ == '__main__':
    main()
