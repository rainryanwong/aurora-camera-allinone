"""Package the rear-camera Natural Night preview routing for All in One 2.0.95."""

import copy
import hashlib
import zipfile

import build_natural_preview as preview
from package_module308 import OUTPUT as BASE, RELEASE, ROOT


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.95_Stock1770_Compat01181_LiveMotion053_AUTH.zip'
BUILD = ROOT / 'build/module309'


def module_updates(previous):
    scenes = preview.night_preview_scenes(preview.author_lookup)
    updates = {}
    for filename in preview.NIGHT_PREVIEW_SENSORS:
        name = f'natural-payload/{filename}'
        updates[name] = preview.tuned_payload(
            previous.read(name), filename,
            bind=lambda current, filename: preview.night_preview_bindings(current, filename, scenes),
        )

    gs = f'natural-payload/{preview.GS}'
    with zipfile.ZipFile(preview.BASE) as original:
        updates[gs] = preview.gs_payload(
            original.read(gs), build=BUILD, sky_scenes=True, photo_scenes=True, scene4=True,
            extra_sky_routes=(*preview.SCENE42_EXTRA_ROUTES, *preview.CONTINUITY_ROUTES),
            night_scenes=scenes,
        )
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
    updates['module.prop'] = previous.read('module.prop').replace(
        b'version=2.0.94\nversionCode=20094\n', b'version=2.0.95\nversionCode=20095\n'
    )
    return updates


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    hashes = {}
    with zipfile.ZipFile(BASE) as previous:
        updates = module_updates(previous)
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                name = item.filename
                if name == 'runtime.sha256':
                    continue
                data = updates[name] if name in updates else previous.read(item)
                output.writestr(copy.copy(item), data)
                hashes[name] = hashlib.sha256(data).hexdigest()
            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))

    with OUTPUT.open('rb') as stream:
        print(OUTPUT, OUTPUT.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest())


if __name__ == '__main__':
    main()
