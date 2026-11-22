"""Package All in One 2.0.94 with CameraCompat 0.1.181."""

import copy
import hashlib
import zipfile

from package_module307 import OUTPUT as BASE, RELEASE, ROOT


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.94_Stock1770_Compat01181_LiveMotion053_AUTH.zip'
COMPAT = RELEASE / 'CameraCompat-0.1.181.apk'
HELP = 'webroot/feature-help.js'


def main():
    hashes = {}
    with zipfile.ZipFile(BASE) as previous:
        updates = {
            'camera-compat.apk': COMPAT.read_bytes(),
            HELP: (ROOT / HELP).read_bytes(),
            'module.prop': previous.read('module.prop').replace(
                b'version=2.0.93\nversionCode=20093\n', b'version=2.0.94\nversionCode=20094\n'
            ),
        }
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                name = item.filename
                if name == 'runtime.sha256':
                    continue
                data = updates[name] if name in updates else previous.read(item)
                output.writestr(copy.copy(item), data)
                hashes[name] = hashlib.sha256(data).hexdigest()

            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))

    with OUTPUT.open('rb') as stream:
        print(OUTPUT, OUTPUT.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest())


if __name__ == '__main__':
    main()
