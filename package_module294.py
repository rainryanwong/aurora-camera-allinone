"""Package All in One 2.0.75 from the verified Compat and LiveMotion builds."""

from pathlib import Path
import copy
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent
RELEASE = ROOT / 'release'
BASE_RELEASE = ROOT.parent / 'lsp_fix_249_import_preview_20261031' / 'release'
BASE = BASE_RELEASE / 'camera68_aurora_allinone_v2.0.74_Stock1770_Compat01114_AUTH.zip'
BASE_SOURCE = BASE_RELEASE / 'CameraCompat_2.0.74_source.zip'
COMPAT_APK = RELEASE / 'CameraCompat-0.1.124.apk'
COMPAT_METADATA = RELEASE / 'CameraCompat-0.1.124.json'
LIVE_ROOT = ROOT.parent.parent / 'analysis' / 'live-motion-lsp-20261023'
LIVE_APK = LIVE_ROOT / 'release' / '6.8Cam-LiveMotion-0.5.1.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.75_Stock1770_Compat01124_LiveMotion051_AUTH.zip'
SOURCE = RELEASE / 'CameraCompat_2.0.75_source.zip'


def package_runtime():
    updates = {
        'camera-compat.apk': COMPAT_APK.read_bytes(),
        'live-motion.apk': LIVE_APK.read_bytes(),
    }
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = updates[name] if name in updates else previous.read(name)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.74\n', b'version=2.0.75\n')
                data = data.replace(b'versionCode=20074\n', b'versionCode=20075\n')
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))


def package_source():
    metadata = json.loads(COMPAT_METADATA.read_text(encoding='utf-8'))
    snapshot = Path(metadata['input_snapshot'])
    updates = {p.relative_to(snapshot).as_posix(): p for p in snapshot.rglob('*') if p.is_file()}
    for name in ('build_lsp.py', 'notes.md', Path(__file__).name):
        updates[name] = ROOT / name
    live_sources = [LIVE_ROOT / 'build.py']
    for name in ('src', 'api-stubs'):
        live_sources.extend((LIVE_ROOT / name).rglob('*.java'))
    for p in live_sources:
        updates['live-motion/' + p.relative_to(LIVE_ROOT).as_posix()] = p

    with zipfile.ZipFile(BASE_SOURCE) as previous, \
            zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for name in previous.namelist():
            source = updates.pop(name, None)
            if source is None:
                output.writestr(name, previous.read(name))
            else:
                output.write(source, name)
        for name, source in sorted(updates.items()):
            output.write(source, name)


if __name__ == '__main__':
    package_runtime()
    package_source()
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)
