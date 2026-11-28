"""Package the lens-specific Natural tuning for ordinary Photo preview."""
import hashlib
import subprocess
import tempfile
from collections import defaultdict
from functools import cache, partial
from pathlib import Path

import build_natural_preview as preview
from package_module310 import HELP, ROOT, update_natural_metadata, write_module
from package_module315 import STOCK_TUNING
from package_module322 import OUTPUT as BASE, RELEASE


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.8_Stock1770_Compat0296_LiveMotion053_AUTH.zip'
GS_SHA = '6e8009f6ef0970fca0326e30300acbb242106447adc281d4302ebaec06ee1e59'
EFFECTS = {0: 97, 28: 98}
READOUTS = range(1, 20)
PROFILE = (36, 0)


def gs_payload(blob, scenes):
    # The old non-HDR Photo dispatch branches away at the first instruction;
    # the bytes up to the next live entry are available for the new dispatch.
    if hashlib.sha256(blob).hexdigest() != GS_SHA:
        raise ValueError('Unexpected GS input for the Photo dispatch')
    start, limit = 0x155cc4, 0x155d1c
    with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
        obj, elf = (Path(directory) / name for name in ('photo.o', 'photo.elf'))
        mask = sum(1 << scene for scene in scenes)
        subprocess.run([preview.NDK / 'clang.exe', '--target=aarch64-linux-android29', '-c',
                        f'-DPHOTO_SCENE_MASK={mask:#x}', f'-DPHOTO_EFFECT={EFFECTS[0]}',
                        ROOT / 'native/gs_photo_preview.S', '-o', obj], check=True)
        subprocess.run([preview.NDK / 'ld.lld.exe', f'-Ttext={start:#x}', '--entry=photo_preview',
                        '--defsym=previous_scene=0x155f00', '--defsym=store_effect=0x1556b4',
                        obj, '-o', elf], check=True)
        with elf.open('rb') as stream:
            image = preview.ELFFile(stream)
            section = image.get_section_by_name('.text')
            address, code = section['sh_addr'], section.data()
            entry = image.header['e_entry']
    if address != start or entry != start or len(code) > limit - start:
        raise ValueError('Photo dispatch exceeds its reserved instructions')
    return blob[:start] + code + blob[start + len(code):]


def prior_effects():
    # Reconstruct the 2.1.7 routes so that existing scene-specific appearances
    # and all non-appearance module bindings survive the new private Effects.
    table = {}
    for sensor in READOUTS:
        for scene in preview.SCENES:
            for source in EFFECTS:
                table[sensor, scene, source] = preview.IPE_EFFECTS[source]
    for sensor, feature, scene in set(preview.PHOTO_SCENE_GS) | preview.SCENE4_GS:
        if feature == PROFILE[0]:
            for source in EFFECTS:
                table[sensor, scene, source] = preview.PHOTO_SCENE_EFFECTS[source]
    sky = preview.photo_sky_routes(scene4=True, extra=(*preview.SCENE42_EXTRA_ROUTES,
                                                        *preview.CONTINUITY_ROUTES))
    for sensor, scene, source, effect in (*reversed(sky), *preview.PHOTO_SCENE42_ROUTES):
        table[sensor, scene, source] = effect
    return table


def lookup(blob):
    symbols, data, tree = preview.model.sections(blob)
    return preview.model.LookupModel(list(preview.model.MODE.iter_unpack(tree)),
                                     list(preview.model.REC.iter_unpack(symbols))), data


def lens_bindings(current, filename, *, data, scenes, prior):
    stock, stock_data = lookup((STOCK_TUNING / filename).read_bytes())
    author, author_data = lookup((preview.AUTHOR / filename).read_bytes())
    if data != author_data:
        raise ValueError(f'Natural object offsets do not match {filename}')

    @cache
    def same_as_stock(at, size, stock_at, stock_size):
        return size == stock_size and memoryview(data)[at:at+size] == memoryview(stock_data)[stock_at:stock_at+size]

    def matches_stock(record, original):
        return same_as_stock(*record[3:5], *original[3:5])

    @cache
    def same_object(is_stock, left, right):
        if left == right:
            return True
        blob = memoryview(stock_data if is_stock else data)
        at, size = left
        other, length = right
        return size == length and blob[at:at+size] == blob[other:other+length]

    # The old and new Effect leaves share the complete parent chain. Only
    # overrides on the previous leaf need copying, including via linked nodes.
    records_at = defaultdict(dict)
    for (node, name), record in current.modules.items():
        records_at[node][name] = record

    @cache
    def node_at(query):
        if len(query) == 1:
            return current.follow(current.root)
        parent = node_at(query[:-1])
        packed = len(query) - 1 | (query[-1] << 16)
        for child in current.children.get(parent, ()):
            if current.packed[child] == packed:
                return current.follow(child)

    def children(query, kind):
        return [current.packed[node] >> 16 for node in current.children.get(node_at(query), ())
                if current.packed[node] & 0xffff == kind]

    additions = {}
    for sensor in READOUTS:
        head = (0, sensor, 0)
        primary = (*head, *PROFILE, 0)
        has_reference = any(not matches_stock(current.get_record(name, (*primary, 76)),
                                              stock.get_record(name, (*primary, 0)))
                            for name in preview.APPEARANCE)
        profiles = set(preview.SKY_PROFILES)
        profiles.update((f1, f2) for f1 in children(head, 3) for f2 in children((*head, f1), 4))
        for f1, f2 in sorted(profiles):
            prefix = (*head, f1, f2)
            for scene in scenes:
                for source, private in EFFECTS.items():
                    public = (*prefix, scene, source)
                    old = (*public[:-1], prior.get((sensor, scene, source), source))
                    target = (*public[:-1], private)
                    changes = {name: (record, record[5]) for name, record in
                               records_at.get(node_at(old), {}).items()}
                    if has_reference and (f1, f2) in preview.SKY_PROFILES:
                        for name in preview.APPEARANCE:
                            record = current.get_record(name, old)
                            original = stock.get_record(name, public)
                            if matches_stock(record, original):
                                neutral = (*prefix, 0, source)
                                reference = preview.scene_appearance(author, name, public, PROFILE[0])
                                # Keep independent stock scene tuning when the author
                                # supplies only a generic look for that object.
                                if (same_object(True, original[3:5], stock.get(name, neutral))
                                        or not same_object(False, reference[3:5], author.get(name, neutral))):
                                    if not matches_stock(reference, original):
                                        changes[name] = (reference, None)
                    if changes:
                        changes = [(r, sid) for name, (r, sid) in changes.items()
                                   if sid is None or preview.selection(r) != preview.selection(
                                       current.get_record(name, target))]
                        if changes:
                            additions[target] = changes
    return additions


def module_updates(previous):
    metadata = previous.read('module.prop')
    updates = {'module.prop': metadata.replace(b'version=2.1.7\nversionCode=20108\n',
                                               b'version=2.1.8\nversionCode=20109\n'),
               HELP: (ROOT / HELP).read_bytes()}
    scenes = set(preview.night_preview_scenes(preview.author_lookup))
    for filename in preview.ROUTE_SENSORS:
        stock, _ = lookup((STOCK_TUNING / filename).read_bytes())
        scenes.update(value >> 16 for value in stock.packed.values() if value & 0xffff == 5)
    scenes = sorted(scenes)
    gs = f'natural-payload/{preview.GS}'
    updates[gs] = gs_payload(previous.read(gs), scenes)
    prior = prior_effects()
    for filename in preview.ROUTE_SENSORS:
        path = f'natural-payload/{filename}'
        blob = previous.read(path)
        bind = partial(lens_bindings, data=preview.model.sections(blob)[1], scenes=scenes, prior=prior)
        updates[path] = preview.tuned_payload(blob, filename, bind=bind)
    update_natural_metadata(previous, updates)
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
