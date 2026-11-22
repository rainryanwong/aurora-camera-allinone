"""Package All in One 2.0.96 with Natural crop preview and CameraCompat 0.1.182."""

import copy
import hashlib
import zipfile

import build_natural_preview as preview
from package_module309 import OUTPUT as BASE, RELEASE, ROOT


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.96_Stock1770_Compat01182_LiveMotion053_AUTH.zip'
BUILD = ROOT / 'build/module310'
COMPAT = RELEASE / 'CameraCompat-0.1.182.apk'
HELP = 'webroot/feature-help.js'


def module_updates(previous):
    updates = {
        'camera-compat.apk': COMPAT.read_bytes(),
        HELP: (ROOT / HELP).read_bytes(),
        'module.prop': previous.read('module.prop').replace(
            b'version=2.0.95\nversionCode=20095\n', b'version=2.0.96\nversionCode=20096\n'
        ),
    }
    for filename in preview.SCENE42_SENSORS:
        name = f'natural-payload/{filename}'
        updates[name] = preview.tuned_payload(
            previous.read(name), filename, bind=preview.crop_scene32_bindings
        )

    scenes = preview.night_preview_scenes(preview.author_lookup)
    gs = f'natural-payload/{preview.GS}'
    with zipfile.ZipFile(preview.BASE) as original:
        updates[gs] = preview.gs_payload(
            original.read(gs), build=BUILD, sky_scenes=True, photo_scenes=True, scene4=True,
            extra_sky_routes=(*preview.SCENE42_EXTRA_ROUTES, *preview.CONTINUITY_ROUTES),
            night_scenes=scenes, crop_scene32=True,
        )

    update_natural_metadata(previous, updates)
    return updates


def update_natural_metadata(previous, updates):
    gs = f'natural-payload/{preview.GS}'
    old_gs_hash = hashlib.sha256(previous.read(gs)).hexdigest().encode()
    new_gs_hash = hashlib.sha256(updates[gs]).hexdigest().encode()
    trace_libraries = 'webroot/preview-libraries.txt'
    updates[trace_libraries] = previous.read(trace_libraries).replace(old_gs_hash, new_gs_hash)

    manifest = []
    for line in previous.read('natural-files.list').decode().splitlines():
        filename, stock_hash, payload_hash = line.split()
        member = f'natural-payload/{filename}'
        if member in updates:
            payload_hash = hashlib.sha256(updates[member]).hexdigest()
        manifest.append(f'{filename} {stock_hash} {payload_hash}\n')
    updates['natural-files.list'] = ''.join(manifest).encode('utf-8')


def write_module(base, output_path, build_updates):
    hashes = {}
    with zipfile.ZipFile(base) as previous:
        updates = build_updates(previous)
        with zipfile.ZipFile(output_path, 'x') as output:
            for item in previous.infolist():
                name = item.filename
                if name == 'runtime.sha256':
                    continue
                data = updates[name] if name in updates else previous.read(item)
                output.writestr(copy.copy(item), data)
                hashes[name] = hashlib.sha256(data).hexdigest()
            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))

    with output_path.open('rb') as stream:
        print(output_path, output_path.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest())


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    write_module(BASE, OUTPUT, module_updates)


if __name__ == '__main__':
    main()
