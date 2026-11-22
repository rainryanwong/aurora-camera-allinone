"""Package the Natural Photo Feature 22 preview routes for All in One 2.0.97."""

import zipfile

import build_natural_preview as preview
from package_module310 import HELP, OUTPUT as BASE, RELEASE, ROOT, update_natural_metadata, write_module


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.97_Stock1770_Compat01182_LiveMotion053_AUTH.zip'
BUILD = ROOT / 'build/module311'


def module_updates(previous):
    scenes = preview.night_preview_scenes(preview.author_lookup)
    crop_scenes = [scene for scene in scenes if scene != 0]
    updates = {
        HELP: (ROOT / HELP).read_bytes(),
        'module.prop': previous.read('module.prop').replace(
            b'version=2.0.96\nversionCode=20096\n', b'version=2.0.97\nversionCode=20097\n',
        ),
    }
    for filename in preview.CROP_PREVIEW_SENSORS:
        name = f'natural-payload/{filename}'
        updates[name] = preview.tuned_payload(
            previous.read(name), filename,
            bind=lambda current, filename: preview.crop_preview_bindings(current, filename, crop_scenes),
        )

    gs = f'natural-payload/{preview.GS}'
    with zipfile.ZipFile(preview.BASE) as original:
        updates[gs] = preview.gs_payload(
            original.read(gs), build=BUILD, sky_scenes=True, photo_scenes=True, scene4=True,
            extra_sky_routes=(*preview.SCENE42_EXTRA_ROUTES, *preview.CONTINUITY_ROUTES),
            night_scenes=scenes, crop_scenes=crop_scenes,
        )
    update_natural_metadata(previous, updates)
    return updates


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    write_module(BASE, OUTPUT, module_updates)


if __name__ == '__main__':
    main()
