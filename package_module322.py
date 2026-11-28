"""Package the verified video watermark color fix in All in One 2.1.7."""

from package_module310 import RELEASE, write_module
from package_module321 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.2.96.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.7_Stock1770_Compat0296_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.6\nversionCode=20107\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.6 as the packaging input')
    return {
        'module.prop': metadata.replace(version, b'version=2.1.7\nversionCode=20108\n', 1),
        'camera-compat.apk': COMPAT.read_bytes(),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
