"""Package the two-batch runtime optimizations on the accepted 2.1.9 payload."""
from package_module298 import web_files
from package_module310 import RELEASE, ROOT, write_module
from package_module324 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.3.11.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.10_Stock1770_Compat0311_LiveMotion053_AUTH.zip'


def module_updates(previous):
    old = b'version=2.1.9\nversionCode=20110\n'
    metadata = previous.read('module.prop')
    if old not in metadata:
        raise ValueError('Expected All in One 2.1.9')
    scripts = {
        'service.sh': 'runtime239/service.sh',
        'm3-relay.sh': 'runtime263/m3-relay.sh',
        'post-fs-data.sh': 'runtime306/post-fs-data.sh',
        'palette-activate.sh': 'runtime306/palette-activate.sh',
        'natural-activate.sh': 'runtime306/natural-activate.sh',
        'arcsoft-night-activate.sh': 'runtime307/arcsoft-night-activate.sh',
        'auth239/service.sh': 'auth239/service.sh',
    }
    updates = {name: (ROOT / path).read_bytes().replace(b'\r\n', b'\n')
               for name, path in scripts.items()}
    web = web_files()
    for name in ('webroot/app.js', 'webroot/feature-help.js', 'webroot/groups.js'):
        updates[name] = web[name]
    updates['module.prop'] = metadata.replace(old, b'version=2.1.10\nversionCode=20111\n', 1)
    updates['camera-compat.apk'] = COMPAT.read_bytes()
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
