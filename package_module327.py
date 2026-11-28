"""Package the three additional ordinary Photo Natural inputs for 2.1.12."""
import hashlib
from functools import partial

import build_natural_preview as preview
from package_module310 import HELP, RELEASE, ROOT, update_natural_metadata, write_module
from package_module315 import STOCK_TUNING
from package_module323 import lookup
from package_module324 import assemble_sections, lens_bindings, old_selector, values
from package_module326 import OUTPUT as BASE


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.12_Stock1770_Compat0312_LiveMotion053_AUTH.zip'
PHOTO_INPUTS = {24: 111, 61: 112, 65: 113}


def module_updates(previous):
    gs_member = 'natural-payload/' + preview.GS
    original = previous.read(gs_member)
    if hashlib.sha256(original).hexdigest() != '4ad6de86d45ed19affd008cf5c2d64e6b0bdf641531836825618ef80222339c6':
        raise ValueError('Expected the 2.1.11 GS code at the Photo dispatch addresses')
    updated = assemble_sections(
        original, 'gs_photo_inputs.S', 'photo_inputs', {'.photo_special': (0x155854, 0x1558b0)},
        {f'PHOTO_{source}': target for source, target in PHOTO_INPUTS.items()},
        dict(known_route=0x155dcc, previous_photo=0x155c68),
    )
    scenes = set(preview.night_preview_scenes(preview.author_lookup))
    for filename in preview.ROUTE_SENSORS:
        stock, _ = lookup((STOCK_TUNING / filename).read_bytes())
        scenes.update(values(stock, 5))
    bind = partial(lens_bindings, scenes=sorted(scenes), previous=old_selector(original),
                   families=((163, 36, preview.SKY_PROFILES, PHOTO_INPUTS),),
                   select=old_selector(updated), match_photo_looks=True)
    updates = {gs_member: updated}
    for filename in preview.ROUTE_SENSORS:
        member = 'natural-payload/' + filename
        blob = previous.read(member)
        updates[member] = preview.tuned_payload(
            blob, filename, bind=partial(bind, data=preview.model.sections(blob)[1]),
            share_new_effects=True,
        )
    update_natural_metadata(previous, updates)
    # The development help file also contains experimental features. Carry
    # only the revised Natural diagnostic entry into the official help table.
    prefix = b"  'natural-graph':"
    old_help = previous.read(HELP)
    old_row, = (row for row in old_help.splitlines() if row.startswith(prefix))
    new_row, = (row for row in (ROOT / HELP).read_bytes().splitlines() if row.startswith(prefix))
    updates[HELP] = old_help.replace(old_row, new_row, 1)
    updates['module.prop'] = previous.read('module.prop').replace(
        b'version=2.1.11\nversionCode=20112\n', b'version=2.1.12\nversionCode=20113\n')
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
