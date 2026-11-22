"""Package All in One 2.0.93 with preview continuity and ArcSoft compatibility."""

import copy
import hashlib
import zipfile

import build_natural_preview as preview
from package_module306 import OUTPUT as BASE, RELEASE, ROOT, TOOL, build_odm_compat


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.93_Stock1770_Compat01180_LiveMotion053_AUTH.zip'
BUILD = ROOT / 'build/module307'


def module_updates(previous):
    night = 'arcsoft-night-activate.sh'
    help_file = 'webroot/feature-help.js'
    updates = {
        TOOL: build_odm_compat(BUILD),
        night: (ROOT / 'runtime307' / night).read_bytes(),
        help_file: (ROOT / help_file).read_bytes(),
        'module.prop': previous.read('module.prop').replace(
            b'version=2.0.92\nversionCode=20092\n', b'version=2.0.93\nversionCode=20093\n'
        ),
    }
    for filename in preview.SCENE42_SENSORS:
        name = f'natural-payload/{filename}'
        updates[name] = preview.tuned_payload(previous.read(name), filename,
                                              bind=preview.continuity_bindings)

    name = f'natural-payload/{preview.GS}'
    with zipfile.ZipFile(preview.BASE) as original:
        updates[name] = preview.gs_payload(
            original.read(name), build=BUILD, sky_scenes=True, photo_scenes=True, scene4=True,
            extra_sky_routes=(*preview.SCENE42_EXTRA_ROUTES, *preview.CONTINUITY_ROUTES),
        )

    trace_libraries = 'webroot/preview-libraries.txt'
    old_gs_hash = hashlib.sha256(previous.read(name)).hexdigest().encode()
    new_gs_hash = hashlib.sha256(updates[name]).hexdigest().encode()
    updates[trace_libraries] = previous.read(trace_libraries).replace(old_gs_hash, new_gs_hash)

    manifest = []
    for line in previous.read('natural-files.list').decode().splitlines():
        filename, stock, payload_hash = line.split()
        member = f'natural-payload/{filename}'
        if member in updates:
            payload_hash = hashlib.sha256(updates[member]).hexdigest()
        manifest.append(f'{filename} {stock} {payload_hash}\n')
    updates['natural-files.list'] = ''.join(manifest).encode()
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
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode())

    with OUTPUT.open('rb') as stream:
        print(OUTPUT, OUTPUT.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest(), flush=True)


if __name__ == '__main__':
    main()
