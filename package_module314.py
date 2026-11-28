"""Package Scene 42 Natural Photo preview coverage for All in One 2.0.100."""

import hashlib
import io
import struct

import build_natural_preview as preview
from package_module310 import update_natural_metadata
from package_module313 import HELP, OUTPUT as BASE, RELEASE, ROOT, write_module


COMPAT = RELEASE / 'CameraCompat-0.2.87.apk'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.100_Stock1770_Compat0287_LiveMotion053_AUTH.zip'
BASE_GS_SHA256 = 'd51212b17e9a77e2a58ec07ccfde5bd085d212982aa92f954b5b5a395eb936ed'
SKY_TABLE_ADR = 0x155cd4  # adr x13, existing Scene route table
SKY_TABLE = 0x155e62


def gs_scene42_payload(blob):
    if hashlib.sha256(blob).hexdigest() != BASE_GS_SHA256:
        raise ValueError('Expected the All in One 2.0.99 GS library')
    image = preview.ELFFile(io.BytesIO(blob))
    loads = [(index, segment) for index, segment in enumerate(image.iter_segments())
             if segment['p_type'] == 'PT_LOAD']
    index, text = next((index, segment) for index, segment in loads if segment['p_flags'] == 5)
    file_delta = text['p_offset'] - text['p_vaddr']
    old_routes = preview.photo_sky_routes(
        scene4=True, extra=(*preview.SCENE42_EXTRA_ROUTES, *preview.CONTINUITY_ROUTES),
    )
    old_table = b''.join(bytes(row) for row in old_routes) + b'\xff'
    old_at = SKY_TABLE + file_delta
    if blob[old_at:old_at + len(old_table)] != old_table:
        raise ValueError('Unexpected GS Scene route table')

    overrides = {row[:3] for row in preview.PHOTO_SCENE42_ROUTES}
    routes = (*preview.PHOTO_SCENE42_ROUTES, *(row for row in old_routes if row[:3] not in overrides))
    table = b''.join(bytes(row) for row in routes) + b'\xff'
    end = text['p_vaddr'] + text['p_filesz']
    end_offset = end + file_delta
    next_load = min(segment['p_vaddr'] for _, segment in loads if segment['p_vaddr'] > text['p_vaddr'])
    next_offset = min(segment['p_offset'] for _, segment in loads if segment['p_offset'] > text['p_offset'])
    if (end + len(table) > next_load or end_offset + len(table) > next_offset
            or any(blob[end_offset:end_offset + len(table)])):
        raise ValueError('GS table would overlap an occupied range')

    # Retarget the existing ADR, leaving the mode, feature and Effect guards intact.
    distance = end - SKY_TABLE_ADR
    adr = 0x1000000d | ((distance & 3) << 29) | ((distance >> 2) << 5)
    result = bytearray(blob)
    struct.pack_into('<I', result, SKY_TABLE_ADR + file_delta, adr)
    result[end_offset:end_offset + len(table)] = table
    size = text['p_filesz'] + len(table)
    struct.pack_into('<QQ', result, image['e_phoff'] + index * image['e_phentsize'] + 32, size, size)
    return result


def module_updates(previous):
    metadata = previous.read('module.prop')
    version = b'version=2.0.99\nversionCode=20099\n'
    if version not in metadata:
        raise ValueError('Expected All in One 2.0.99 as the packaging input')
    updates = {
        'camera-compat.apk': COMPAT.read_bytes(),
        HELP: (ROOT / HELP).read_bytes(),
        'module.prop': metadata.replace(version, b'version=2.0.100\nversionCode=20100\n', 1),
    }
    for filename in preview.PHOTO_SCENE42_READOUTS:
        name = f'natural-payload/{filename}'
        updates[name] = preview.tuned_payload(previous.read(name), filename,
                                              bind=preview.photo_scene42_bindings)

    gs = f'natural-payload/{preview.GS}'
    updates[gs] = gs_scene42_payload(previous.read(gs))
    update_natural_metadata(previous, updates)
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
