"""Package the second optimization pass in All in One 2.1.3."""

from package_module317 import OUTPUT as BASE, RELEASE, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.90.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.3_Stock1770_Compat0290_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.2\nversionCode=20103\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.2 as the packaging input')
    installer = previous.read('customize.sh')
    source_hash = b'  expected=$(file_sha "$MODPATH/$source")\n'
    if installer.count(source_hash) != 1:
        raise ValueError('Expected one APK source digest in the installer')
    manifest_hash = (
        b'  # The full runtime manifest has already been verified above.\n'
        b'  expected=$(awk -v name="$source" \'$2 == name { print $1; exit }\' "$MODPATH/runtime.sha256")\n'
    )
    return {
        'module.prop': metadata.replace(version, b'version=2.1.3\nversionCode=20104\n', 1),
        'customize.sh': installer.replace(source_hash, manifest_hash, 1),
        'camera-compat.apk': COMPAT.read_bytes(),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
