"""Package All in One 2.0.98 with signed CameraCompat 0.1.184."""

from package_module310 import HELP, write_module
from package_module311 import OUTPUT as BASE, RELEASE, ROOT


COMPAT = RELEASE / 'CameraCompat-0.1.184.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.98_Stock1770_Compat01184_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.0.97\nversionCode=20097\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.0.97 as the packaging input')
    return {
        'camera-compat.apk': COMPAT.read_bytes(),
        HELP: (ROOT / HELP).read_bytes(),
        'module.prop': metadata.replace(version, b'version=2.0.98\nversionCode=20098\n', 1),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
