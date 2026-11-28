"""Build the shared Natural preview routes for All in One 2.1.9 on the host."""
import hashlib
import subprocess
import sys
import tempfile
from collections import defaultdict
from functools import cache, lru_cache, partial
from pathlib import Path

import build_natural_preview as preview
from package_module310 import HELP, ROOT, update_natural_metadata, write_module
from package_module315 import STOCK_TUNING
from package_module323 import OUTPUT as BASE, RELEASE, lookup


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.9_Stock1770_Compat0296_LiveMotion053_AUTH.zip'
# A private Effect distinguishes cases that converge on the same downstream
# profile (especially 7/0), while preserving the original public Effect variant.
CROP, DAY, STREET, STREET_SPECIAL, NIGHT, PHOTO_SPECIAL, CROP_HDR = 99, 101, 103, 105, 106, 108, 109
DAY_PROFILES = ((0, 0), (7, 0), (0, 80))
# (real GS mode, primary feature, affected IPE profiles, public -> private)
FAMILIES = (
    (163, 22, preview.CROP_SCENE32_PROFILES, {0: CROP, 28: CROP+1, 36: CROP_HDR, 37: CROP_HDR+1}),
    (167, 0, DAY_PROFILES, {0: DAY, 28: DAY+1}),
    (225, 0, DAY_PROFILES, {0: STREET, 28: STREET+1, 24: STREET_SPECIAL}),
    (173, 37, preview.NIGHT_PREVIEW_PROFILES, {0: NIGHT, 28: NIGHT+1}),
    (163, 36, preview.SKY_PROFILES, {24: PHOTO_SPECIAL}),
)


def values(model, kind):
    return {value >> 16 for value in model.packed.values() if value & 65535 == kind}


def gs_payload(blob, scenes, last_sensor):
    # The two replaced blocks were the 2.1.8 crop and night selectors. The
    # third range is padding after a terminated table. All three are already RX.
    if hashlib.sha256(blob).hexdigest() != '1b43f176dbb6100a73d8c2b82f3a40e3e69b0841823ad037dcd66eb88174e99e':
        raise ValueError('Expected the 2.1.8 GS instructions for this incremental patch')
    sections = {'.mode_entry': (0x155be0, 0x155c68), '.mode_sources': (0x155d90, 0x155e10),
                '.photo_special': (0x155854, 0x1558b0)}
    constants = dict(CROP=CROP, CROP_HDR=CROP_HDR, DAY=DAY, STREET=STREET, STREET_SPECIAL=STREET_SPECIAL,
                     NIGHT=NIGHT, PHOTO_SPECIAL=PHOTO_SPECIAL, LAST_READOUT=last_sensor,
                     KNOWN_SCENES=sum(1 << scene for scene in scenes))
    symbols = dict(previous_photo=0x155c68, previous_route=0x1558b0, store_effect=0x1556b4)
    return assemble_sections(blob, 'gs_mode_preview.S', 'mode_preview', sections, constants, symbols)


def assemble_sections(blob, source, entry, sections, constants, symbols):
    result = bytearray(blob)
    with tempfile.TemporaryDirectory(dir=ROOT / 'build') as directory:
        obj, elf = (Path(directory) / name for name in ('modes.o', 'modes.elf'))
        subprocess.run([preview.NDK / 'clang.exe', '--target=aarch64-linux-android29', '-c',
                        *(f'-D{k}={v:#x}' for k, v in constants.items()),
                        ROOT / 'native' / source, '-o', obj], check=True)
        subprocess.run([preview.NDK / 'ld.lld.exe', f'--entry={entry}',
                        *(f'--section-start={name}={start:#x}' for name, (start, _) in sections.items()),
                        *(f'--defsym={name}={address:#x}' for name, address in symbols.items()),
                        obj, '-o', elf], check=True)
        with elf.open('rb') as stream:
            image = preview.ELFFile(stream)
            for name, (start, limit) in sections.items():
                section = image.get_section_by_name(name)
                code = section.data()
                if section['sh_addr'] != start or len(code) > limit-start:
                    raise ValueError(f'{name} outside its reserved instructions')
                result[start:start+len(code)] = code
    return bytes(result)


def old_selector(blob):
    # Replay the previous executable on the PC. Some older private selections
    # share a feature number but carry different per-scene or non-color objects.
    import struct
    sys.path.append('D:/cam18pm/analysis/14u-15u-camera-rom-20261117/tmp/unicorn-host')
    from unicorn import Uc, UC_ARCH_ARM64, UC_MODE_ARM
    from unicorn.arm64_const import UC_ARM64_REG_PC, UC_ARM64_REG_SP, UC_ARM64_REG_X8

    cpu = Uc(UC_ARCH_ARM64, UC_MODE_ARM)
    cpu.mem_map(0x155000, 0x1000)
    cpu.mem_write(0x155000, blob[0x155000:0x156000])
    sp, query = 0x300000, 0x301000
    cpu.mem_map(sp, 0x2000)
    cpu.reg_write(UC_ARM64_REG_SP, sp)
    cpu.reg_write(UC_ARM64_REG_X8, query)

    @cache
    def select(mode, feature, sensor, scene, source):
        fields = (0, sensor, 0, feature, 0, scene, source)
        cpu.mem_write(sp+0x324, struct.pack('<I', mode))
        cpu.mem_write(query, struct.pack('<15I', 7, *(x for pair in enumerate(fields) for x in pair)))
        cpu.emu_start(0x155454, 0x1556c8, count=12000)
        if cpu.reg_read(UC_ARM64_REG_PC) != 0x1556c8:
            raise ValueError('Previous GS preview route did not return')
        return int.from_bytes(cpu.mem_read(query+56, 4), 'little')

    return select


class Index:
    """Resolve the same first-match tree while reusing common query prefixes."""
    def __init__(self, model):
        self.model = model
        self.children = {up: {model.packed[n]: model.follow(n) for n in reversed(nodes)}
                         for up, nodes in model.children.items()}
        self.records = defaultdict(dict)
        for (node, name), row in model.modules.items():
            self.records[node][name] = row
        self.branch = lru_cache(maxsize=16384)(self._branch)

    def _branch(self, query):
        if len(query) == 1:
            node = self.model.follow(self.model.root)
            inherited = (None,) * len(preview.APPEARANCE)
        else:
            parent, inherited = self.branch(query[:-1])
            node = self.children.get(parent, {}).get((query[-1] << 16) | (len(query)-1))
        rows = self.records.get(node)
        if rows:
            inherited = tuple(rows.get(n, r) for n, r in zip(preview.APPEARANCE, inherited))
        return node, inherited

    def profiles(self, head):
        node = self.branch(head)[0]
        return {(first >> 16, second >> 16)
                for first, child in self.children.get(node, {}).items() if first & 65535 == 3
                for second in self.children.get(child, {}) if second & 65535 == 4}


def lens_bindings(current, filename, *, data, scenes, previous, families=FAMILIES,
                  select=None, match_photo_looks=False):
    stock, stock_data = lookup((STOCK_TUNING / filename).read_bytes())
    author, author_data = lookup((preview.AUTHOR / filename).read_bytes())
    if author_data != data:
        raise ValueError(f'Natural object offsets differ for {filename}')
    old, ref, stock_index = map(Index, (current, author, stock))
    views = (memoryview(data), memoryview(stock_data))

    @cache
    def equal(bank, left, other_bank, right):
        if bank == other_bank and left == right:
            return True
        off, size = left
        other, length = right
        return size == length and views[bank][off:off+size] == views[other_bank][other:other+length]

    def different(bank, record, other_bank, other):
        return not equal(bank, record[3:5], other_bank, other[3:5])

    @cache
    def has_stock_look(head, profiles, scene, source):
        for pair in profiles:
            prefix = (*head, *pair)
            actual = stock_index.branch((*prefix, scene, source))[1]
            for reference in ((0, source), (scene, 0)):
                neutral = stock_index.branch((*prefix, *reference))[1]
                if any(different(1, a, 1, b) for a, b in zip(actual, neutral)):
                    return True
        return False

    def appearance_changes(public, before, mode, primary):
        prefix, scene, source = public[:5], public[5], public[6]
        original = stock_index.branch(public)[1]
        selected = ref.branch(public)[1]
        if match_photo_looks:
            if has_stock_look(prefix[:3], preview.SKY_PROFILES, scene, source):
                return
            neutral = previous(mode, primary, prefix[1], scene, 0)
            neighbor = old.branch((*prefix, scene, neutral))[1]
        if scene and prefix[3:] != (primary, 0):
            base = (*prefix[:3], primary, 0)
            parent = ref.branch(prefix)[1]
            first = ref.branch((*base, scene, source))[1]
            default = ref.branch(base)[1]
            selected = tuple(f if r[3:5] == p[3:5] and f[3:5] != d[3:5] else r
                             for r, p, f, d in zip(selected, parent, first, default))
        # Preserve genuine stock scene and effect-specific looks when the author
        # provides only a generic object. All comparisons use the same lens.
        neutral_scene = (*prefix, 0, source)
        neutral_effect = (*prefix, scene, 0)
        stock_scene, author_scene = stock_index.branch(neutral_scene)[1], ref.branch(neutral_scene)[1]
        stock_effect, author_effect = stock_index.branch(neutral_effect)[1], ref.branch(neutral_effect)[1]
        for j, name in enumerate(preview.APPEARANCE):
            cur, src, dest = before[j], original[j], selected[j]
            if different(0, cur, 1, src) or not different(0, cur, 0, dest):
                continue
            if scene and different(1, src, 1, stock_scene[j]) and not different(0, dest, 0, author_scene[j]):
                continue
            if source and different(1, src, 1, stock_effect[j]) and not different(0, dest, 0, author_effect[j]):
                continue
            # Adjacent requests can reach identical data through different
            # registered objects. Keep the already working SID on this profile.
            if match_photo_looks and not different(0, dest, 0, neighbor[j]):
                yield name, (neighbor[j], neighbor[j][5])
            else:
                yield name, (dest, None)

    additions = {}
    for sensor in sorted(values(current, 1) | values(stock, 1)):
        head = (0, sensor, 0)
        configured = old.profiles(head)
        for mode, primary, profiles, effects in families:
            for pair in sorted(configured | set(profiles)):
                prefix = (*head, *pair)
                for scene in scenes:
                    for source, private in effects.items():
                        if select is not None and select(mode, primary, sensor, scene, source) != private:
                            continue
                        if private == STREET_SPECIAL and (sensor, scene) != (6, 32):
                            continue
                        if private == PHOTO_SPECIAL and (sensor > 19 and sensor != 35 or scene not in (0, 2, 10, 48)):
                            continue
                        prior = previous(mode, primary, sensor, scene, source)
                        parent = (*prefix, scene)
                        leaf, before = old.branch((*parent, prior))
                        changes = {n: (r, r[5]) for n, r in old.records.get(leaf, {}).items()}
                        if pair in profiles:
                            changes.update(appearance_changes((*parent, source), before, mode, primary))
                        if changes:
                            target = (*parent, private)
                            bindings = [(r, sid) for n, (r, sid) in changes.items()
                                        if sid is None or preview.selection(r) != preview.selection(
                                            current.get_record(n, target))]
                            if bindings:
                                additions[target] = bindings
    return additions


def make_payloads(previous):
    scenes = set(preview.night_preview_scenes(preview.author_lookup))
    sensors = set()
    for filename in preview.ROUTE_SENSORS:
        stock, _ = lookup((STOCK_TUNING / filename).read_bytes())
        scenes.update(values(stock, 5))
        sensors.update(values(stock, 1))
    scenes = sorted(scenes)
    gs = previous.read('natural-payload/' + preview.GS)
    yield preview.GS, gs_payload(gs, scenes, max(sensors))
    select = old_selector(gs)
    for filename in preview.ROUTE_SENSORS:
        blob = previous.read('natural-payload/' + filename)
        bind = partial(lens_bindings, data=preview.model.sections(blob)[1], scenes=scenes, previous=select)
        yield filename, preview.tuned_payload(blob, filename, bind=bind, share_new_effects=True)


def module_updates(previous, payloads=None):
    updates = {'natural-payload/' + name: blob for name, blob in
               (make_payloads(previous) if payloads is None else payloads)}
    version = b'version=2.1.8\nversionCode=20109\n'
    updates['module.prop'] = previous.read('module.prop').replace(version, b'version=2.1.9\nversionCode=20110\n')
    updates[HELP] = (ROOT / HELP).read_bytes()
    update_natural_metadata(previous, updates)
    return updates


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
