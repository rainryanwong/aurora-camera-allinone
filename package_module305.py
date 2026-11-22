"""Package All in One 2.0.91 with CameraCompat 0.1.180."""

import copy
import hashlib
import zipfile

from package_module304 import HELP, OUTPUT as BASE
from package_module298 import RELEASE, ROOT


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.91_Stock1770_Compat01180_LiveMotion053_AUTH.zip'


def main():
    updates = {
        'camera-compat.apk': (RELEASE / 'CameraCompat-0.1.180.apk').read_bytes(),
        HELP: (ROOT / HELP).read_bytes(),
    }
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = updates[name] if name in updates else previous.read(item)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.90\n', b'version=2.0.91\n')
                data = data.replace(b'versionCode=20090\n', b'versionCode=20091\n')
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()

        manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))

    with OUTPUT.open('rb') as stream:
        print(OUTPUT, OUTPUT.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest())


if __name__ == '__main__':
    main()
