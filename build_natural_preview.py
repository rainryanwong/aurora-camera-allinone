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


def bind_preview_queries(current, filename, queries, *, appearance_only=False):
    author = author_lookup(filename)
    names = APPEARANCE if appearance_only else sorted({name for _, name in current.modules})
    additions = {}
    for source, private, gs_feature, use_natural in queries:
        target = (*source[:-1], private)
        changes = []
        for name in names:
            existing = current.get_record(name, target)
            if use_natural and name in APPEARANCE:
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


def tuned_payload(blob, filename, *, bind=preview_bindings):
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
    for target, changes in additions.items():
        node = current.ensure_path(target, modes, records)
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


def gs_payload(blob, *, build=BUILD, sky_scenes=False, photo_scenes=False):
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
        (build / 'sky_preview_routes.inc').write_text(
            ''.join(f'    .byte {sensor}, {scene}, {source}, {private}\n'
                    for sensor in sorted(SKY_SENSORS.values())
                    for scene, source, private in SKY_EFFECTS) + '    .byte 255\n', encoding='utf-8')
        flags.append('-DSKY_PREVIEW')
    if photo_scenes:
        (build / 'photo_scene_routes.inc').write_text(
            ''.join(f'    .byte {sensor}, {feature1}, {scene}\n'
                    for sensor, feature1, scene in PHOTO_SCENE_GS) + '    .byte 255\n', encoding='utf-8')
        flags.extend(('-DPHOTO_SCENES', f'-DPHOTO_EFFECT={PHOTO_SCENE_EFFECTS[0]}'))
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
