"""Integrate full-height 60 fps in All in One 2.1.11."""
import hashlib

from build_open_gate_sensors import LENSES, STOCK, Sensor, build, link_tuning, word
from package_module310 import RELEASE, ROOT, write_module
from package_module325 import OUTPUT as BASE


COMPAT = RELEASE / 'CameraCompat-0.3.12.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.11_Stock1770_Compat0312_LiveMotion053_AUTH.zip'


def module_updates(previous):
    metadata = previous.read('module.prop')
    old = b'version=2.1.10\nversionCode=20111\n'
    if old not in metadata:
        raise ValueError('Expected All in One 2.1.10')
    updates = {f'open-gate-sensors/{name}': data for name, data in build()}
    manifest = previous.read('natural-files.list').decode().splitlines()
    for lens, (_, source_index) in LENSES.items():
        sensor = Sensor(STOCK / f'com.qti.sensormodule.aurora_{lens}.bin')
        info = word(sensor.read(sensor.roots['sensorDriverData']), 74)
        added_index = word(sensor.read(info), 2)
        filename = f'com.qti.tuned.aurora_{lens}.bin'
        member = f'natural-payload/{filename}'
        updates[member] = link_tuning(previous.read(member), added_index, source_index)
        digest = hashlib.sha256(updates[member]).hexdigest()
        for i, line in enumerate(manifest):
            name, stock, _ = line.split()
            if name == filename:
                manifest[i] = f'{name} {stock} {digest}'
                break
    updates['natural-files.list'] = ('\n'.join(manifest) + '\n').encode()
    # Both groups run before provider startup. A sensor index needs matching tuning.
    boot = previous.read('post-fs-data.sh')
    activation = b"""
if [ "$NATURAL_RC" = 0 ]; then
    OPEN_GATE_RC=0
    /system/bin/sh "$MODDIR/open-gate-activate.sh" || OPEN_GATE_RC=$?
    echo "open_gate_activation_exit=$OPEN_GATE_RC"
fi
"""
    after_natural = b'# Natural night exposure input compatibility.'
    if boot.count(after_natural) != 1:
        raise ValueError('Cannot locate the end of early tuning activation')
    updates['post-fs-data.sh'] = boot.replace(after_natural, activation + after_natural)
    installer = previous.read('customize.sh')
    permissions = b'set_perm_recursive "$MODPATH" 0 0 0755 0644\n'
    if installer.count(permissions) != 1:
        raise ValueError('Cannot locate module file permissions')
    sensors = b'set_perm_recursive "$MODPATH/open-gate-sensors" 0 0 0755 0644 u:object_r:vendor_file:s0\n'
    updates['customize.sh'] = installer.replace(permissions, permissions + sensors)
    for member, source in (
        ('open-gate-activate.sh', 'runtime306/open-gate-activate.sh'),
        ('webroot/app.js', 'webroot/app.js'),
        ('webroot/feature-help.js', 'webroot/feature-help.js'),
        ('webroot/diagnostics.sh', 'webroot/diagnostics.sh'),
    ):
        updates[member] = (ROOT / source).read_bytes().replace(b'\r\n', b'\n')
    updates['module.prop'] = metadata.replace(old, b'version=2.1.11\nversionCode=20112\n', 1)
    updates['camera-compat.apk'] = COMPAT.read_bytes()
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
