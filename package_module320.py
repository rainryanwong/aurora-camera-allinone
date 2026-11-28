"""Package the M3 and grip fixes in All in One 2.1.5."""

from package_module310 import RELEASE, ROOT, write_module
from package_module319 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.2.94-LegendGripZoom.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.5_Stock1770_Compat0294_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.4\nversionCode=20105\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.4 as the packaging input')
    return {
        'module.prop': metadata.replace(version, b'version=2.1.5\nversionCode=20106\n', 1),
        'camera-compat.apk': COMPAT.read_bytes(),
        'webroot/feature-help.js': (ROOT / 'webroot/feature-help.js').read_bytes(),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
