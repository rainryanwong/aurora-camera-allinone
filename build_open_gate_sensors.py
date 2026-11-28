"""Build the four full-height 60 fps rows on the desktop, preserving stock mode indices."""
import struct
import zlib
from pathlib import Path

ANALYSIS = Path('D:/cam18pm/analysis')
STOCK = ANALYSIS / 'natural-photo-audit-20261108/device/odm/lib64/camera'
MAIN = ANALYSIS / 'open-gate-main60-20261124'
AUX = ANALYSIS / 'open-gate-imx858-60-20261124'
LENSES = {
    'semco_LYT900_wide': (MAIN / 'full60-window-test.bin', 3),
    'semco_imx858_ultra': (AUX / 'uw-phase-aligned.bin', 3),
    'ofilm_imx858_tele3x': (AUX / 'tele-phase-aligned.bin', 3),
    'semco_imx858_tele5x': (AUX / 'peri-phase-aligned.bin', 5),
}
ENTRY = struct.Struct('<I40s4I')
SECTION = struct.Struct('<3I')


def word(data, index):
    return struct.unpack_from('<I', data, 4 * index)[0]


def put(data, index, value):
    struct.pack_into('<I', data, 4 * index, value)


def entry_at(table, count, index):
    width, remainder = divmod(len(table), count)
    if remainder or not 0 <= index < count:
        raise ValueError('invalid camera mode table')
    return bytearray(table[width * index:width * (index + 1)])


class Sensor:
    def __init__(self, path):
        raw = path.read_bytes()
        sections = list(SECTION.iter_unpack(raw[168:204]))
        if [s[0] for s in sections] != [0, 1, 2]:
            raise ValueError(f'Unexpected sections in {path}')
        (_, start, size), (_, data, length), (_, tail, extra) = sections
        self.header = bytearray(raw[:start])
        self.entries = {r[0]: r for r in ENTRY.iter_unpack(raw[start:start + size])}
        self.data = bytearray(raw[data:data + length])
        self.tail = raw[tail:tail + extra]
        self.roots = {r[1].split(b'\0')[0].decode(): r[0] for r in self.entries.values()
                      if r[2] != 0xffffffff}
        self.last = max(self.entries)

    def read(self, sid):
        _, _, _, pos, size, _ = self.entries[sid]
        return self.data[pos:pos + size]

    def append(self, name, content):
        self.last += 1
        self.entries[self.last] = (self.last, name, 0xffffffff, len(self.data), len(content), self.last)
        self.data.extend(content)
        return self.last

    def set_word(self, sid, index, value):
        pos = self.entries[sid][3]
        struct.pack_into('<I', self.data, pos + 4 * index, value)

    def encode(self):
        directory = b''.join(ENTRY.pack(*entry) for entry in self.entries.values())
        sections = (directory, self.data, self.tail)
        offset = len(self.header)
        for sid, section in enumerate(sections):
            SECTION.pack_into(self.header, 168 + sid * SECTION.size, sid, offset, len(section))
            offset += len(section)
        struct.pack_into('<I', self.header, 28, offset)
        content = self.header + b''.join(sections)
        return content + struct.pack('<I', zlib.crc32(content))


def integrate(stock, trial, donor_index, auxiliary):
    def clone(sid, content):
        return stock.append(trial.entries[sid][1], content)

    def extend(owner, count_word, new_row):
        info = stock.read(owner)
        count, table = word(info, count_word), word(info, count_word + 1)
        content = stock.read(table) + new_row
        stock.set_word(owner, count_word, count + 1)
        stock.set_word(owner, count_word + 1, clone(table, content))

    info = word(stock.read(stock.roots['sensorDriverData']), 74)
    resolution = stock.read(info)
    count, table = word(resolution, 2), word(resolution, 3)
    max_stock_sid = stock.last
    row = entry_at(trial.read(table), count, donor_index)
    if auxiliary:
        stream, settings = word(row, 8), word(row, 40)
        put(row, 8, clone(stream, trial.read(stream)))
        registers = trial.read(settings)
        register_count = word(row, 39)
        stride = len(registers) // register_count
        new_constants = {}
        for offset in range(0, len(registers), stride):
            constant = struct.unpack_from('<I', registers, offset + 16)[0]
            if constant > max_stock_sid:
                if constant not in new_constants:
                    new_constants[constant] = clone(constant, trial.read(constant))
                struct.pack_into('<I', registers, offset + 16, new_constants[constant])
        put(row, 40, clone(settings, registers))
        pd = stock.roots['PDConfigData']
        pd_root = stock.read(pd)
        num, profiles = word(pd_root, 9), word(pd_root, 10)
        old_key = word(row, 78)
        profile = entry_at(trial.read(profiles), num, word(trial.read(old_key), 0))
        native_id = word(profile, 1)
        native = trial.read(native_id)
        block_id = word(native, 3)
        block = trial.read(block_id)
        pixels = word(block, 2)
        put(block, 2, clone(pixels, trial.read(pixels)))
        put(native, 3, clone(block_id, block))
        put(profile, 1, clone(native_id, native))
        key = clone(old_key, struct.pack('<I', num))
        put(row, 78, key)
        put(profile, 8, key)
        extend(pd, 9, profile)
    # The multi-camera selector also searches NORMAL rows while switching into
    # ordinary video. Use the provider's internal row and exact session rule.
    put(row, 65, 1)
    put(row, 66, stock.append(b'capability', struct.pack('<I', 10)))
    # The auxiliary cameras retain their native Dolby session.
    sessions = (0, 0x8024) if auxiliary else (0,)
    conditions = b''.join(struct.pack('<9I', session, 4096, 3072, 60, 60, 0, 255, 0, 0)
                          for session in sessions)
    put(row, 79, len(sessions))
    put(row, 80, stock.append(b'forcePickCondition', conditions))
    extend(info, 2, row)
    return stock.encode()


def build():
    for suffix, (path, index) in LENSES.items():
        filename = f'com.qti.sensormodule.aurora_{suffix}.bin'
        stock = Sensor(STOCK / filename)
        yield filename, integrate(stock, Sensor(path), index, 'imx858' in suffix)


def link_tuning(blob, added_mode, source_mode):
    """Let the provider use the tested lens tuning when it requests the new index."""
    from native.natural_lookup_model import MODE, mode_graph, sections

    _, _, tree = sections(blob)
    modes = list(MODE.iter_unpack(tree))
    canon, parents, packed = mode_graph(modes)
    root = next(node for node, parent in parents.items() if parent is None)
    by_sensor = {value >> 16: node for node, value in packed.items()
                 if parents[node] == root and value & 0xffff == 1}
    target = by_sensor[source_mode]
    while modes[target][4] != 0xffffffff:
        target = canon[modes[target][4]]
    body = bytearray(blob[:-4])
    if added_mode in by_sensor:
        node = by_sensor[added_mode]
        tree_start = len(body) - len(tree)
        struct.pack_into('<I', body, tree_start + MODE.size * node + 16, target)
    else:
        body.extend(MODE.pack(len(modes), (added_mode << 16) | 1, 0, root, target))
        struct.pack_into('<I', body, 200, len(tree) + MODE.size)
        struct.pack_into('<I', body, 28, len(body))
    return body + struct.pack('<I', zlib.crc32(body))
