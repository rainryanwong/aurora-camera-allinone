"""Package the third optimization pass in All in One 2.1.4."""

from package_module318 import OUTPUT as BASE, RELEASE, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.91.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.4_Stock1770_Compat0291_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.3\nversionCode=20104\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.3 as the packaging input')
    installer = previous.read('customize.sh').splitlines(keepends=True)
    grants = [line for line in installer if b'"$MODPATH/lutshare-access.sh" apply' in line]
    if len(grants) != 1:
        raise ValueError('Expected one LUT auto-start grant in the installer')
    installer.remove(grants[0])
    return {
        'module.prop': metadata.replace(version, b'version=2.1.4\nversionCode=20105\n', 1),
        'customize.sh': b''.join(installer),
        'camera-compat.apk': COMPAT.read_bytes(),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
