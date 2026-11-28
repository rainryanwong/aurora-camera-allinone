"""Package the first whole-module optimization pass in All in One 2.1.2."""

from package_module316 import OUTPUT as BASE, RELEASE, ROOT, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.89.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.2_Stock1770_Compat0289_LiveMotion053_AUTH_r2.zip'
RUNTIME_FILES = {
    'root-compat.sh': 'runtime306/root-compat.sh',
    'post-fs-data.sh': 'runtime306/post-fs-data.sh',
    'palette-activate.sh': 'runtime306/palette-activate.sh',
    'auth239/post-fs-data.sh': 'runtime306/auth239/post-fs-data.sh',
    'auth239/service.sh': 'auth239/service.sh',
    'webroot/diagnostics.sh': 'webroot/diagnostics.sh',
    'webroot/feature-help.js': 'webroot/feature-help.js',
}


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.1.1\nversionCode=20102\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.1.1 as the packaging input')
    updates = {name: (ROOT / source).read_bytes() for name, source in RUNTIME_FILES.items()}
    updates['module.prop'] = metadata.replace(version, b'version=2.1.2\nversionCode=20103\n', 1)
    updates['camera-compat.apk'] = COMPAT.read_bytes()
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
