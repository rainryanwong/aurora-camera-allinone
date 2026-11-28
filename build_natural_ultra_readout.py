"""Add Photo Natural to the ultra-wide's ordinary 16:9 sensor readout."""
import argparse
import struct
import zipfile
from pathlib import Path

from build_open_gate_sensors import Sensor, entry_at, word


SENSOR = 'com.qti.sensormodule.aurora_semco_imx858_ultra.bin'
MEMBER = 'open-gate-sensors/' + SENSOR
PICK = struct.Struct('<9I')
PHOTO_16_9_VIVID = (0x9002, 8192, 4608, 14, 30, 0, 255, 0, 0)


def natural_readout(source):
    sensor = Sensor(source)
    resolution_id = word(sensor.read(sensor.roots['sensorDriverData']), 74)
    resolution = sensor.read(resolution_id)
    count, table_id = word(resolution, 2), word(resolution, 3)
    modes = sensor.read(table_id)
    mode_index = 2  # 14 Ultra IMX858 ultra-wide, ordinary 4096 x 2304.
    mode = entry_at(modes, count, mode_index)
    rule_count, rules_id = word(mode, 79), word(mode, 80)
    rules = sensor.read(rules_id)
    rows = list(PICK.iter_unpack(rules))
    if len(rows) != rule_count or PHOTO_16_9_VIVID not in rows:
        raise ValueError('Missing ordinary 16:9 Photo rule on ultra-wide mode 2')
    natural = list(PHOTO_16_9_VIVID)
    natural[5] = 2  # Xiaomi Photo style: Vivid = 0, Classic = 1, Natural = 2.
    if tuple(natural) in rows:
        return source.read_bytes()
    updated_id = sensor.append(sensor.entries[rules_id][1], rules + PICK.pack(*natural))
    base = mode_index * (len(mode) // 4)
    sensor.set_word(table_id, base + 79, rule_count + 1)
    sensor.set_word(table_id, base + 80, updated_id)
    return sensor.encode()


def candidate_updates(base):
    properties = base.read('module.prop')
    version, = (line for line in properties.splitlines() if line.startswith(b'version='))
    return {
        MEMBER: natural_readout(zipfile.Path(base, MEMBER)),
        'module.prop': properties.replace(version, version + b'-uw16-test', 1),
    }


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('base_zip', type=Path)
    parser.add_argument('output', type=Path, help='Sensor directory, or ZIP path with --module')
    parser.add_argument('--module', action='store_true', help='Build a local All in One test ZIP')
    args = parser.parse_args()
    if args.module:
        from package_module310 import write_module

        write_module(args.base_zip, args.output, candidate_updates)
    else:
        with zipfile.ZipFile(args.base_zip) as base:
            blob = natural_readout(zipfile.Path(base, MEMBER))
        args.output.mkdir(parents=True, exist_ok=True)
        (args.output / SENSOR).write_bytes(blob)
