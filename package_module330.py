"""Package the tested JPEG Super quality fix for All in One 2.1.15."""
from package_module310 import HELP, RELEASE, ROOT, write_module
from package_module329 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.3.16.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.15_Stock1770_Compat0316_LiveMotion053_AUTH.zip'


def module_updates(previous):
    properties = previous.read('module.prop')
    version = b'version=2.1.14\nversionCode=20115\n'
    if version not in properties:
        raise ValueError('Expected All in One 2.1.14 metadata')

    # Only the PhotoQualityHooks help entry belongs to this Compat update.
    help_text = previous.read(HELP)
    prefix = b'  PhotoQualityHooks:'
    old_entry, = (line for line in help_text.splitlines() if line.startswith(prefix))
    new_entry, = (line for line in (ROOT / HELP).read_bytes().splitlines() if line.startswith(prefix))
    return {
        'module.prop': properties.replace(version, b'version=2.1.15\nversionCode=20116\n', 1),
        'camera-compat.apk': COMPAT.read_bytes(),
        HELP: help_text.replace(old_entry, new_entry, 1),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
