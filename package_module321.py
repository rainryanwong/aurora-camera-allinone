"""Package the integrated video watermark in All in One 2.1.6."""

from package_module298 import web_files
from package_module310 import RELEASE, ROOT, write_module
from package_module320 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.2.95.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.6_Stock1770_Compat0295_LiveMotion053_AUTH_r2.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.5\nversionCode=20106\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.5 as the packaging input')
    return {
        'module.prop': metadata.replace(version, b'version=2.1.6\nversionCode=20107\n', 1),
        'camera-compat.apk': COMPAT.read_bytes(),
        'webroot/groups.js': web_files()['webroot/groups.js'],
        'webroot/feature-help.js': (ROOT / 'webroot/feature-help.js').read_bytes(),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
