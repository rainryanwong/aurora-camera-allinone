"""Package the Feature 22 Natural Photo crop preview fix for All in One 2.1.0."""

import hashlib
import struct
from functools import partial

import build_natural_preview as preview
from package_module310 import update_natural_metadata
from package_module314 import HELP, OUTPUT as BASE, RELEASE, ROOT, write_module


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.0_Stock1770_Compat0287_LiveMotion053_AUTH.zip'
STOCK_TUNING = ROOT.parent.parent / 'analysis/aurora-stock-306-gs-20261117'
BASE_GS_SHA256 = 'a4cfa7d75623bf979635f4806b165794b95505c189869915052b74c990f41fb8'
CROP_MOV = {0: 0x155c60, 28: 0x155c58}
CROP_MASKS = 0x155ef0


def gs_crop_photo_payload(blob):
    if hashlib.sha256(blob).hexdigest() != BASE_GS_SHA256:
        raise ValueError('Expected the All in One 2.0.100 GS library')
    old_sensor_mask, scene_mask = struct.unpack_from('<QQ', blob, CROP_MASKS)
    scenes = tuple(scene for scene in range(64) if scene_mask & (1 << scene))
    sensor_mask = old_sensor_mask | sum(1 << sensor for sensor in preview.CROP_PHOTO_SENSORS)

    # Only the guarded F22 branch gets the new Effect pair and sensor mask.
    # Its scene mask, caller guards and branch destinations stay in place.
    result = bytearray(blob)
    for source, address in CROP_MOV.items():
        mov_w9 = 0x52800009 | (preview.CROP_PHOTO_EFFECTS[source] << 5)
        struct.pack_into('<I', result, address, mov_w9)
    struct.pack_into('<Q', result, CROP_MASKS, sensor_mask)
    return bytes(result), scenes


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.0.100\nversionCode=20100\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.0.100 as the packaging input')

    gs = f'natural-payload/{preview.GS}'
    gs_payload, scenes = gs_crop_photo_payload(previous.read(gs))
    updates = {
        'module.prop': metadata.replace(version, b'version=2.1.0\nversionCode=20101\n', 1),
        HELP: (ROOT / HELP).read_bytes(),
        gs: gs_payload,
    }
    stock_hashes = {name: digest for name, digest, _ in
                    (line.split() for line in previous.read('natural-files.list').decode().splitlines())}
    for filename in preview.CROP_PHOTO_READOUTS:
        stock = (STOCK_TUNING / filename).read_bytes()
        if hashlib.sha256(stock).hexdigest() != stock_hashes[filename]:
            raise ValueError(f'Unexpected stock tuning for {filename}')
        path = f'natural-payload/{filename}'
        tuning = previous.read(path)
        bind = partial(preview.crop_photo_bindings, data=preview.model.sections(tuning)[1],
                       stock_blob=stock, scenes=scenes)
        updates[path] = preview.tuned_payload(tuning, filename, bind=bind)

    update_natural_metadata(previous, updates)
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
