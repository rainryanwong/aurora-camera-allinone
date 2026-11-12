"""Package All in One 2.0.78 with the Compat and LiveMotion updates."""

import copy
import hashlib
from pathlib import Path
import zipfile


ROOT = Path(__file__).resolve().parent
RELEASE = ROOT / 'release'
LIVE_ROOT = ROOT / 'live-motion'
BASE = RELEASE / 'camera68_aurora_allinone_v2.0.77_Stock1770_Compat01124_LiveMotion051_AUTH.zip'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.78_Stock1770_Compat01125_LiveMotion052_AUTH.zip'


def package_runtime():
    updates = {
        'camera-compat.apk': (RELEASE / 'CameraCompat-0.1.125.apk').read_bytes(),
        'live-motion.apk': (LIVE_ROOT / 'release/6.8Cam-LiveMotion-0.5.2.apk').read_bytes(),
    }
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = updates[name] if name in updates else previous.read(name)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.77\n', b'version=2.0.78\n')
                data = data.replace(b'versionCode=20077\n', b'versionCode=20078\n')
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))


if __name__ == '__main__':
    package_runtime()
    with OUTPUT.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    print(OUTPUT, OUTPUT.stat().st_size, digest, flush=True)
