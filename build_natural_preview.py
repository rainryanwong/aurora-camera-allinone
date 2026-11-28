"""Build the aurora Natural Photo preview routing and tuning payloads."""

import argparse
import io
import struct
import subprocess
import sys
import zipfile
import zlib

sys.dont_write_bytecode = True
from package_natural_lenses import APPEARANCE, AUTHOR, NDK, ROOT, model

sys.path.insert(0, 'D:/cam18pm/tools/native_analysis')
from elftools.elf.elffile import ELFFile


BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.75_Stock1770_Compat01124_LiveMotion051_AUTH.zip'
BUILD = ROOT / 'build/natural-preview-followup'
SKY_BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.76_Stock1770_Compat01124_LiveMotion051_AUTH.zip'
SKY_BUILD = ROOT / 'build/natural-preview-sky'
PHOTO_BUILD = ROOT / 'build/natural-preview-complete'
MODE_IPE_BUILD = ROOT / 'build/natural-preview-mode-ipe'
GS = 'com.qti.feature2.gs.sm8650.so'
MAIN = 'com.qti.tuned.aurora_semco_LYT900_wide.bin'
TELE3 = 'com.qti.tuned.aurora_ofilm_imx858_tele3x.bin'
TELE5 = 'com.qti.tuned.aurora_semco_imx858_tele5x.bin'
ROUTE_SENSORS = {
    MAIN: (4, 7),
    'com.qti.tuned.aurora_semco_imx858_ultra.bin': (1,),
    TELE3: (1,),
    TELE5: (1,),
}
SCENES = (0, 2, 10, 32, 40, 46, 48, 55)
EFFECTS = {36: 81, 37: 82}
LENS_ROUTES = {filename: {(sensor, 36, scene) for sensor in sensors for scene in SCENES}
               for filename, sensors in ROUTE_SENSORS.items()}
LENS_ROUTES[MAIN].update(((6, 36, 0), (6, 36, 46)))
LENS_ROUTES[TELE5].add((1, 22, 0))
GS_ROUTES = sorted(set().union(*LENS_ROUTES.values()))
IPE_EFFECTS = {0: 76, 28: 77, **EFFECTS}
SKY_SENSORS = {MAIN: 7, TELE5: 1}
# Use effects reserved for this Photo preview route. Capture also uses Effect 76.
SKY_EFFECTS = ((42, 0, 83), (42, 28, 84), (4, 28, 84))
SKY_PROFILES = ((36, 0), (7, 0), (36, 80))
PHOTO_SCENE_ROUTES = {
    MAIN: {(4, 36, 4), (6, 36, 4)},
    TELE3: {(1, 36, 6)},
    TELE5: {(1, 36, scene) for scene in (6, 11, 12, 13, 15, 17, 18, 20, 21)} | {(1, 22, 32)},
}
# F1=22 and F1=36 can both reach the same downstream IPE profile in Scene 32.
PHOTO_SCENE_EFFECTS = {0: 85, 28: 86, 36: 87, 37: 88}
PHOTO_SCENE_GS = sorted(set().union(*PHOTO_SCENE_ROUTES.values()))
SCENE4_ROUTES = {
    filename: {(sensor, 36, 4) for sensor in sensors}
    for filename, sensors in ROUTE_SENSORS.items()
}
SCENE4_GS = set().union(*SCENE4_ROUTES.values())
SCENE42_SENSORS = {
    MAIN: (4, 6, 7),
    'com.qti.tuned.aurora_semco_imx858_ultra.bin': (1,),
    TELE3: (1,),
    TELE5: (1, 3),
}
SCENE42_EXTRA_ROUTES = ((3, 42, 0, 83), (4, 42, 0, 83), (6, 42, 0, 83))
CONTINUITY_SCENE_SOURCES = {4: 0, 42: 0, 45: 10}
CONTINUITY_ROUTES = (
    (3, 4, 0, 85), (3, 4, 28, 86),
    *((sensor, 42, 28, 84) for sensor, _, _, _ in SCENE42_EXTRA_ROUTES),
    *((sensor, 45, source, private)
      for sensor in sorted({sensor for sensors in SCENE42_SENSORS.values() for sensor in sensors})
      for source, private in ((0, 83), (28, 84))),
)
# Readouts with a Scene 0 Natural reference and a confirmed Scene 42 gap.
# Sensor indices are local to each lens, even though the GS table shares them.
PHOTO_SCENE42_READOUTS = {
    MAIN: (1, 2, 3, 5, 8, 9, 10, 11, 12, 13, 14, 15, 17, 18, 19),
    'com.qti.tuned.aurora_semco_imx858_ultra.bin': (2, 3, 6, 7),
    TELE3: (2, 3),
    TELE5: (2, 4, 5, 16, 17),
}
PHOTO_SCENE42_EFFECTS = {0: 93, 28: 94}
PHOTO_SCENE42_ROUTES = tuple(
    (sensor, 42, source, effect)
    for sensor in sorted(set().union(*PHOTO_SCENE42_READOUTS.values()))
    for source, effect in PHOTO_SCENE42_EFFECTS.items()
)
NIGHT_PREVIEW_PROFILES = ((37, 0), (7, 0), (37, 80))
NIGHT_PREVIEW_EFFECTS = {0: 89, 28: 90}
NIGHT_PREVIEW_SENSORS = SCENE42_SENSORS
CROP_SCENE32_PROFILES = ((22, 0), (7, 0), (22, 80), (36, 80))
CROP_SCENE32_EFFECT = 91
CROP_PREVIEW_EFFECTS = {0: CROP_SCENE32_EFFECT, 28: 92}
CROP_PREVIEW_SENSORS = {**SCENE42_SENSORS, MAIN: (4, 5, 6, 7)}
# Normal Photo readouts with an existing Scene 0 Natural reference. Sensor
# indices are local to each lens. GS selects from their union.
CROP_PHOTO_READOUTS = {
    MAIN: tuple(range(1, 20)),
    'com.qti.tuned.aurora_semco_imx858_ultra.bin': (1, 2, 3, 6, 7),
    TELE3: (1, 2, 3),
    TELE5: (1, 2, 3, 4, 5, 16, 17),
}
CROP_PHOTO_SENSORS = sorted(set().union(*CROP_PHOTO_READOUTS.values()))
CROP_PHOTO_EFFECTS = {0: 95, 28: 96}
# The observed scene values also cover requests that inherit a parent node.
OBSERVED_PREVIEW_SCENES = (4, 10, 42, 45)


def binding(record):
    if record is not None:
        return record[1], record[3], record[4]


def selection(record):
    if record is not None:
        return binding(record), record[5]


def author_lookup(filename):
    records, _, tree = model.sections((AUTHOR / filename).read_bytes())
    return model.LookupModel(list(model.MODE.iter_unpack(tree)), list(model.REC.iter_unpack(records)))


def preview_bindings(current, filename):
    author = author_lookup(filename)

    # Sensor modes can occur in more than one lens file. Keep the complete public
    # selection for other lenses and keep all non-appearance selections per Effect.
    additions = {}
    names = sorted({name for _, name in current.modules})
    for sensor, feature1, scene in GS_ROUTES:
        prefix = (0, sensor, 0, feature1, 0, scene)
        use_natural = (sensor, feature1, scene) in LENS_ROUTES[filename]
        for source, private in EFFECTS.items():
            target = prefix + (private,)
            changes = []
            for name in names:
                effect = 76 if use_natural and name in APPEARANCE else source
                record = current.get_record(name, prefix + (effect,))
                if selection(record) != selection(current.get_record(name, target)):
                    changes.append((record, record[5]))
            if changes:
                additions[target] = changes

    # IPE keeps the Effect when it changes the processing profile. Use each
    # original author appearance binding with the matching public non-appearance.
    groups = ((7, 0), (6, 46)) if filename == MAIN else ((1, 0),)
    for sensor, scene in groups:
        for feature1, feature2 in ((7, 0), (36, 80)):
            prefix = (0, sensor, 0, feature1, feature2, scene)
            for source, private in IPE_EFFECTS.items():
                target = prefix + (private,)
                source_query = prefix + (source,)
                changes = []
                for name in names:
                    existing = current.get_record(name, target)
                    if name in APPEARANCE:
                        record = author.get_record(name, source_query)
                        if binding(record) != binding(existing):
                            changes.append((record, None))
                    elif private != 76:
                        record = current.get_record(name, source_query)
                        if selection(record) != selection(existing):
                            changes.append((record, record[5]))
                if changes:
                    additions[target] = changes

    return additions


def sky_bindings(current, filename):
    additions = {}
    names = sorted({name for _, name in current.modules})
    for sensor in sorted(SKY_SENSORS.values()):
        use_natural = sensor == SKY_SENSORS.get(filename)
        for feature1, feature2 in SKY_PROFILES:
            profile = (0, sensor, 0, feature1, feature2)
            for scene, source, private in SKY_EFFECTS:
                source_query = profile + (scene, source)
                target = profile + (scene, private)
                # The author's appearance for these scenes matches this lens's
                # existing Scene 0 binding for the same processing profile.
                natural = profile + (0, IPE_EFFECTS[source])
                changes = []
                for name in names:
                    query = natural if use_natural and name in APPEARANCE else source_query
                    record = current.get_record(name, query)
                    if selection(record) != selection(current.get_record(name, target)):
                        changes.append((record, record[5]))
                if changes:
                    additions[target] = changes
    return additions


def scene_appearance(author, name, query, gs_feature):
    record = author.get_record(name, query)
    if not query[5] or query[3:5] == (gs_feature, 0):
        return record
    if binding(record) != binding(author.get_record(name, query[:5])):
        return record
    # An IPE profile can inherit its default while the GS profile has a real
    # scene override. Carry only those scene-specific objects across profiles.
    primary = (*query[:3], gs_feature, 0, *query[5:])
    scene_record = author.get_record(name, primary)
    if binding(scene_record) != binding(author.get_record(name, primary[:5])):
        return scene_record
    return record


def bind_preview_queries(current, filename, queries, *, appearance_only=False, preserve=None):
    author = author_lookup(filename)
    names = APPEARANCE if appearance_only else sorted({name for _, name in current.modules})
    additions = {}
    for source, private, gs_feature, use_natural in queries:
        target = (*source[:-1], private)
        existing_effect = (preserve or {}).get((source[1], source[5], source[6]))
        changes = []
        for name in names:
            existing = current.get_record(name, target)
            if existing_effect is not None:
                record = current.get_record(name, (*source[:-1], existing_effect))
                if selection(record) != selection(existing):
                    changes.append((record, record[5]))
            elif use_natural and name in APPEARANCE:
                record = scene_appearance(author, name, source, gs_feature)
                if binding(record) != binding(existing):
                    changes.append((record, None))
            else:
                record = current.get_record(name, source)
                if selection(record) != selection(existing):
                    changes.append((record, record[5]))
        if changes:
            additions[target] = changes
    return additions


def ipe_bindings(current, filename):
    installed = ((7, 0), (6, 46)) if filename == MAIN else ((1, 0),)
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, feature1, feature2, scene, source), private, 36, True)
        for sensor, gs_feature, scene in sorted(LENS_ROUTES[filename])
        if gs_feature == 36 and (sensor, scene) not in installed
        for feature1, feature2 in SKY_PROFILES[1:]
        for source, private in EFFECTS.items()
    ))


def photo_scene_bindings(current, filename):
    routes = PHOTO_SCENE_ROUTES.get(filename, ())
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, f1, f2, scene, source), private, feature, (sensor, feature, scene) in routes)
        for sensor, feature, scene in PHOTO_SCENE_GS
        for f1, f2 in ((feature, 0), *SKY_PROFILES[1:])
        for source, private in PHOTO_SCENE_EFFECTS.items()
    ))


def scene4_bindings(current, filename):
    # The two telephotos and ultrawide share Sensor=1 in GS. Give their
    # Effect 28 a fresh private route, leaving the existing Scene 4/84 leaf
    # available for the main camera's Sensor=7.
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, f1, f2, scene, source), private, feature,
         (sensor, feature, scene) in SCENE4_ROUTES[filename])
        for sensor, feature, scene in sorted(SCENE4_GS - set(PHOTO_SCENE_GS))
        for f1, f2 in SKY_PROFILES
        for source, private in PHOTO_SCENE_EFFECTS.items()
        if (sensor, source) != (SKY_SENSORS[MAIN], 28)
    ), preserve={(1, 4, 28): 84} if filename == TELE5 else None)


def scene32_ipe_bindings(current, filename):
    # GS already routes Scene 32 to 76/77. The wide camera has four sensor
    # modes on these downstream preview profiles.
    sensors = (4, 5, 6, 7) if filename == MAIN else ROUTE_SENSORS[filename]
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, f1, f2, 32, source), IPE_EFFECTS[source], 36, True)
        for sensor in sensors
        for f1, f2 in ((7, 0), (36, 80))
        for source in (0, 28)
    ), appearance_only=True)


def observed_mode_ipe_bindings(current, filename):
    if filename not in (MAIN, TELE3, TELE5):
        return {}

    sensor = 7 if filename == MAIN else 1
    # Professional JPEG, shared Street/AI, and Night preview traces.
    profiles = [(sensor, 7, 0, 40, 0), (sensor, 0, 80, 40, 0),
                (sensor, 0, 80, 0, 0), (sensor, 37, 80, 0, 37)]
    if filename == MAIN:
        profiles.extend(((4, 7, 0, 0, 37), (4, 37, 80, 0, 37)))
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, f1, f2, scene, 0), 76, gs_feature, True)
        for sensor, f1, f2, scene, gs_feature in profiles
    ), appearance_only=True)


def scene42_bindings(current, filename):
    """Keep each lens's Scene 0 appearance when its original scene changes to 42."""
    additions = {}
    names = sorted({name for _, name in current.modules})
    legacy_sensors = set(SKY_SENSORS.values())
    sensors = legacy_sensors | {sensor for sensor, _, _, _ in SCENE42_EXTRA_ROUTES}
    for sensor in sorted(sensors):
        use_natural = sensor in SCENE42_SENSORS[filename]
        for feature1, feature2 in SKY_PROFILES:
            prefix = (0, sensor, 0, feature1, feature2)
            for source, private in ((0, 83), (28, 84)) if sensor in legacy_sensors else ((0, 83),):
                if sensor == SKY_SENSORS.get(filename):
                    continue  # Already uses this lens's matching Scene 0 appearance.
                target = prefix + (42, private)
                changes = []
                for name in names:
                    query = (prefix + (0, IPE_EFFECTS[source]) if use_natural and name in APPEARANCE
                             else prefix + (42, source))
                    record = current.get_record(name, query)
                    if selection(record) != selection(current.get_record(name, target)):
                        changes.append((record, record[5]))
                if changes:
                    additions[target] = changes
    return additions


def continuity_bindings(current, filename):
    additions = {}
    names = sorted({name for _, name in current.modules})
    for sensor, scene, source, private in CONTINUITY_ROUTES:
        use_natural = sensor in SCENE42_SENSORS[filename]
        for feature1, feature2 in SKY_PROFILES:
            profile = (0, sensor, 0, feature1, feature2)
            target = profile + (scene, private)
            natural_query = profile + (CONTINUITY_SCENE_SOURCES[scene], IPE_EFFECTS[source])
            stock_query = profile + (scene, source)
            changes = []
            for name in names:
                query = natural_query if use_natural and name in APPEARANCE else stock_query
                record = current.get_record(name, query)
                if selection(record) != selection(current.get_record(name, target)):
                    changes.append((record, record[5]))
            if changes:
                additions[target] = changes
    return additions


def photo_scene42_bindings(current, filename):
    """Preserve Scene 42's effective bindings and fill verified appearance gaps."""
    old_effects = {}
    for sensor, scene, source, effect in photo_sky_routes(
            scene4=True, extra=(*SCENE42_EXTRA_ROUTES, *CONTINUITY_ROUTES)):
        old_effects.setdefault((sensor, scene, source), effect)

    additions = {}
    names = sorted({name for _, name in current.modules})
    for sensor, scene, source, effect in PHOTO_SCENE42_ROUTES:
        prior = old_effects.get((sensor, scene, source), source)
        for feature1, feature2 in SKY_PROFILES:
            prefix = (0, sensor, 0, feature1, feature2)
            target = (*prefix, scene, effect)
            changes = []
            for name in names:
                record = current.get_record(name, (*prefix, scene, prior))
                if name in APPEARANCE and sensor in PHOTO_SCENE42_READOUTS[filename]:
                    original = current.get_record(name, (*prefix, scene, source))
                    neutral = current.get_record(name, (*prefix, 0, source))
                    natural = current.get_record(name, (*prefix, 0, IPE_EFFECTS[source]))
                    if (binding(record) == binding(original) == binding(neutral)
                            and binding(natural) != binding(neutral)):
                        record = natural
                if selection(record) != selection(current.get_record(name, target)):
                    changes.append((record, record[5]))
            if changes:
                additions[target] = changes
    return additions


def night_preview_scenes(lookup):
    return sorted(set(OBSERVED_PREVIEW_SCENES) | {
        value >> 16
        for filename in NIGHT_PREVIEW_SENSORS
        for value in lookup(filename).packed.values()
        if value & 0xffff == 5
    })


def night_preview_bindings(current, filename, scenes):
    sensors = sorted({sensor for values in NIGHT_PREVIEW_SENSORS.values() for sensor in values})
    return bind_preview_queries(current, filename, (
        ((0, sensor, 0, f1, f2, scene, source), effect, 37,
         sensor in NIGHT_PREVIEW_SENSORS[filename])
        for sensor in sensors
        for f1, f2 in NIGHT_PREVIEW_PROFILES
        for scene in scenes
        for source, effect in NIGHT_PREVIEW_EFFECTS.items()
    ))


def crop_scene32_bindings(current, filename):
    names = sorted({name for _, name in current.modules})
    sensors = sorted(set().union(*SCENE42_SENSORS.values()))
    additions = {}
    for sensor in sensors:
        for f1, f2 in CROP_SCENE32_PROFILES:
            prefix = (0, sensor, 0, f1, f2)
            source = (*prefix, 32, 0)
            target = (*prefix, 32, CROP_SCENE32_EFFECT)
            changes = []
            for name in names:
                query = source
                if name in APPEARANCE and sensor in SCENE42_SENSORS[filename]:
                    if filename == TELE5 and sensor == 1:
                        # Keep the existing 5x Scene 32 appearance in its native sensor mode.
                        query = (*prefix, 32, PHOTO_SCENE_EFFECTS[0])
                    elif binding(current.get_record(name, (*prefix, 0, IPE_EFFECTS[0]))) != binding(
                            current.get_record(name, (*prefix, 0, 0))):
                        # A cropped stream inheriting Scene 0 keeps its current appearance.
                        query = (*prefix, 0, IPE_EFFECTS[0])
                record = current.get_record(name, query)
                if selection(record) != selection(current.get_record(name, target)):
                    changes.append((record, record[5]))
            if changes:
                additions[target] = changes
    return additions


def crop_preview_bindings(current, filename, scenes):
    names = sorted({name for _, name in current.modules})
    sensors = sorted(set().union(*CROP_PREVIEW_SENSORS.values()))
    additions = {}
    for sensor in sensors:
        for f1, f2 in CROP_SCENE32_PROFILES:
            prefix = (0, sensor, 0, f1, f2)
            for scene in scenes:
                for source, effect in CROP_PREVIEW_EFFECTS.items():
                    if scene == 32 and source == 0 and sensor != 5:
                        continue  # Already authored for Effect 91 in 2.0.96.
                    source_query = (*prefix, scene, source)
                    target = (*prefix, scene, effect)
                    changes = []
                    for name in names:
                        record = current.get_record(name, source_query)
                        if name in APPEARANCE and sensor in CROP_PREVIEW_SENSORS[filename]:
                            if scene == 32 and filename == TELE5 and sensor == 1:
                                record = current.get_record(name, (*prefix, scene, PHOTO_SCENE_EFFECTS[source]))
                            else:
                                natural = IPE_EFFECTS[source]
                                existing = current.get_record(name, (*prefix, scene, natural))
                                if scene in (40, 46) and binding(existing) != binding(record):
                                    # These GS routes already selected the public Natural Effect.
                                    record = existing
                                else:
                                    neutral = current.get_record(name, (*prefix, 0, source))
                                    reference = current.get_record(name, (*prefix, 0, natural))
                                    if binding(reference) != binding(neutral):
                                        record = reference
                        if selection(record) != selection(current.get_record(name, target)):
                            changes.append((record, record[5]))
                    if changes:
                        additions[target] = changes
    return additions


def crop_photo_bindings(current, filename, data, stock_blob, scenes):
    """Repair cropped preview only where the stock scene inherits its neutral look."""
    symbols, stock_data, tree = model.sections(stock_blob)
    stock = model.LookupModel(list(model.MODE.iter_unpack(tree)), list(model.REC.iter_unpack(symbols)))
    names = sorted({name for _, name in current.modules})
    legacy_sensors = set().union(*CROP_PREVIEW_SENSORS.values())
    photo_readouts = CROP_PHOTO_READOUTS[filename]

    def content(blob, record):
        return blob[record[3]:record[3] + record[4]] if record is not None else None

    additions = {}
    for sensor in CROP_PHOTO_SENSORS:
        for f1, f2 in CROP_SCENE32_PROFILES:
            prefix = (0, sensor, 0, f1, f2)
            for source, effect in CROP_PHOTO_EFFECTS.items():
                natural = IPE_EFFECTS[source]
                stock_neutral = (*prefix, 0, source)
                reference_query = (*prefix, 0, natural)
                references = {}
                if sensor in photo_readouts:
                    for name in APPEARANCE:
                        neutral = content(stock_data, stock.get_record(name, stock_neutral))
                        reference = current.get_record(name, reference_query)
                        if neutral is not None and reference is not None and content(data, reference) != neutral:
                            references[name] = (neutral, reference)

                for scene in scenes:
                    # The older GS already sends Scene 40/46 to public Natural
                    # when the sensor misses the private crop route.
                    prior = source
                    if sensor in legacy_sensors:
                        prior = CROP_PREVIEW_EFFECTS[source]
                    elif scene in (40, 46):
                        prior = natural
                    effective = (*prefix, scene, prior)
                    original = (*prefix, scene, source)
                    target = (*prefix, scene, effect)
                    changes = []
                    for name in names:
                        record = current.get_record(name, effective)
                        if name in references:
                            neutral, reference = references[name]
                            if content(data, record) == neutral == content(stock_data, stock.get_record(name, original)):
                                record = reference
                        if selection(record) != selection(current.get_record(name, target)):
                            changes.append((record, record[5]))
                    if changes:
                        additions[target] = changes
    return additions


def tuned_payload(blob, filename, *, bind=preview_bindings, share_new_effects=False):
    symbols, data, tree = model.sections(blob)
    records = list(model.REC.iter_unpack(symbols))
    modes = list(model.MODE.iter_unpack(tree))
    current = model.LookupModel(modes, records, reuse_source_sid=True)
    additions = bind(current, filename)
    if not additions:
        return blob

    # Author records can be inactive in the baseline. Reuse a loaded source
    # with the same data, or register the first new binding as its source.
    canonical = {}
    for record in records:
        if record[2] != 0xffffffff and record[0] == record[5]:
            canonical.setdefault(binding(record), record[0])
    for target in additions:
        current.ensure_path(target, modes, records, privatize=True)
    shared = {}
    for target, changes in additions.items():
        node = current.ensure_path(target, modes, records)
        if share_new_effects:
            # Stock Chromatix can link identical Effect leaves across parents.
            # Keep the parent-specific inheritance and write each set of local
            # overrides once. Callers use this only for previously unused Effects.
            key = (target[-1], tuple(sorted((binding(row), sid) for row, sid in changes)))
            owner = shared.setdefault(key, node)
            if owner != node:
                modes[node] = (*modes[node][:4], owner)
                continue
        for record, source_sid in changes:
            sid = len(records) + 1
            if source_sid is None:
                source_sid = canonical.setdefault(binding(record), sid)
            records.append((sid, record[1], node, record[3], record[4], source_sid))

    parts = (b''.join(model.REC.pack(*r) for r in records), data,
             b''.join(model.MODE.pack(*r) for r in modes))
    header = bytearray(blob[:204])
    offset = 204
    for index, part in enumerate(parts):
        struct.pack_into('<III', header, 168 + 12 * index, index, offset, len(part))
        offset += len(part)
    struct.pack_into('<I', header, 28, offset)
    body = bytes(header) + b''.join(parts)
    return body + struct.pack('<I', zlib.crc32(body))


def photo_sky_routes(*, scene4=False, extra=()):
    routes = tuple((sensor, scene, source, private)
                   for sensor in sorted(SKY_SENSORS.values())
                   for scene, source, private in SKY_EFFECTS
                   if not (scene4 and scene == 4 and sensor != SKY_SENSORS[MAIN]))
    return (*routes, *extra)


def gs_payload(blob, *, build=BUILD, sky_scenes=False, photo_scenes=False, scene4=False,
               extra_sky_routes=(), night_scenes=(), crop_scene32=False, crop_scenes=()):
    image = ELFFile(io.BytesIO(blob))
    index, segment = next((index, segment) for index, segment in enumerate(image.iter_segments())
                          if segment['p_type'] == 'PT_LOAD' and segment['p_flags'] == 5)
    start = (segment['p_vaddr'] + segment['p_filesz'] + 15) & ~15
    site = 0x15545c
    obj, elf = build / 'gs_preview.o', build / 'gs_preview.elf'
    (build / 'preview_routes.inc').write_text(
        ''.join(f'    .byte {sensor}, {feature1}, {scene}\n'
                for sensor, feature1, scene in GS_ROUTES) + '    .byte 255\n', encoding='utf-8')
    flags = []
    if sky_scenes:
        routes = photo_sky_routes(scene4=scene4, extra=extra_sky_routes)
        (build / 'sky_preview_routes.inc').write_text(
            ''.join(f'    .byte {sensor}, {scene}, {source}, {private}\n'
                    for sensor, scene, source, private in routes)
            + '    .byte 255\n', encoding='utf-8')
        flags.append('-DSKY_PREVIEW')
    if photo_scenes:
        routes = sorted(set(PHOTO_SCENE_GS) | SCENE4_GS) if scene4 else PHOTO_SCENE_GS
        (build / 'photo_scene_routes.inc').write_text(
            ''.join(f'    .byte {sensor}, {feature1}, {scene}\n'
                    for sensor, feature1, scene in routes) + '    .byte 255\n', encoding='utf-8')
        flags.extend(('-DPHOTO_SCENES', f'-DPHOTO_EFFECT={PHOTO_SCENE_EFFECTS[0]}'))
    if night_scenes:
        sensors = set().union(*NIGHT_PREVIEW_SENSORS.values())
        (build / 'night_preview_routes.inc').write_text(
            f'    .quad {sum(1 << scene for scene in night_scenes):#x}\n'
            f'    .quad {sum(1 << sensor for sensor in sensors):#x}\n', encoding='utf-8')
        flags.extend(('-DNIGHT_PREVIEW',
                      f'-DNIGHT_EFFECT_0={NIGHT_PREVIEW_EFFECTS[0]}',
                      f'-DNIGHT_EFFECT_28={NIGHT_PREVIEW_EFFECTS[28]}'))
    if crop_scene32 or crop_scenes:
        sensor_map = CROP_PREVIEW_SENSORS if crop_scenes else SCENE42_SENSORS
        sensors = set().union(*sensor_map.values())
        flags.extend(('-DCROP_SCENE32_PREVIEW', f'-DCROP_SCENE32_EFFECT={CROP_SCENE32_EFFECT}',
                      f'-DCROP_SCENE32_SENSORS={sum(1 << sensor for sensor in sensors):#x}'))
    if crop_scenes:
        flags.extend((f'-DCROP_PREVIEW_SCENES={sum(1 << scene for scene in crop_scenes):#x}',
                      f'-DCROP_EFFECT_28={CROP_PREVIEW_EFFECTS[28]}'))
    subprocess.run([str(NDK / 'clang.exe'), '--target=aarch64-linux-android29', '-c',
                    '-I', str(build), *flags,
                    f'-DPREVIEW_EFFECT={EFFECTS[36]}',
                    str(ROOT / 'native/gs_preview.S'), '-o', str(obj)], check=True)
    subprocess.run([str(NDK / 'ld.lld.exe'), f'-Ttext={start:#x}', '--entry=natural_preview',
                    '--defsym=previous_route=0x1558b0', '--defsym=store_effect=0x1556b4',
                    str(obj), '-o', str(elf)], check=True)
    with elf.open('rb') as stream:
        code = ELFFile(stream).get_section_by_name('.text').data()
    result = bytearray(blob)
    start_offset = start - segment['p_vaddr'] + segment['p_offset']
    result[start_offset:start_offset + len(code)] = code
    site_offset = site - segment['p_vaddr'] + segment['p_offset']
    struct.pack_into('<I', result, site_offset, 0x14000000 | ((start - site) // 4))
    size = start + len(code) - segment['p_vaddr']
    struct.pack_into('<QQ', result, image['e_phoff'] + index * image['e_phentsize'] + 32,
                     size, size)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    variant = parser.add_mutually_exclusive_group()
    variant.add_argument('--sky-scenes', action='store_true', help='Build the Photo preview update for Scene 4 and 42')
    variant.add_argument('--complete-photo-preview', action='store_true', help='Build with the additional Photo preview routes and IPE bindings')
    variant.add_argument('--observed-mode-ipe', action='store_true', help='Add the observed Effect 76 preview IPE bindings to the Photo preview build')
    args = parser.parse_args()
    photo_scenes = args.complete_photo_preview or args.observed_mode_ipe
    sky_scenes = args.sky_scenes or photo_scenes
    build = PHOTO_BUILD if photo_scenes else SKY_BUILD if sky_scenes else BUILD
    stages = (sky_bindings,) if sky_scenes else (preview_bindings,)
    if photo_scenes:
        stages += (ipe_bindings, photo_scene_bindings)
    if args.observed_mode_ipe:
        build = MODE_IPE_BUILD
        stages += (observed_mode_ipe_bindings,)
    payload = build / 'natural-payload'
    payload.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(SKY_BASE if sky_scenes else BASE) as previous:
        for filename in ROUTE_SENSORS:
            print(filename, flush=True)
            tuning = previous.read('natural-payload/' + filename)
            for bind in stages:
                tuning = tuned_payload(tuning, filename, bind=bind)
            (payload / filename).write_bytes(tuning)
    with zipfile.ZipFile(BASE) as previous:
        (payload / GS).write_bytes(gs_payload(previous.read('natural-payload/' + GS),
                                              build=build, sky_scenes=sky_scenes,
                                              photo_scenes=photo_scenes))
    print(payload, flush=True)


if __name__ == '__main__':
    main()
