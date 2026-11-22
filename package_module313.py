"""Package All in One 2.0.99 with signed CameraCompat 0.2.86."""

from package_module312 import HELP, OUTPUT as BASE, RELEASE, ROOT, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.86.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.99_Stock1770_Compat0286_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.0.98\nversionCode=20098\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.0.98 as the packaging input')
    return {
        'camera-compat.apk': COMPAT.read_bytes(),
        HELP: (ROOT / HELP).read_bytes(),
        'module.prop': metadata.replace(version, b'version=2.0.99\nversionCode=20099\n', 1),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
