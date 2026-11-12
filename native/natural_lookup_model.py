"""Chromatix mode lookup and private-path construction shared by the payload builder."""
import struct
import zlib

REC = struct.Struct("<I40s4I")
MODE = struct.Struct("<5I")

def sections(b):
    assert b.startswith(b'QTI Chromatix Header')
    assert struct.unpack_from('<I', b, 0x1c)[0] == len(b) - 4
    assert struct.unpack_from('<I', b, 0xa4)[0] == 3
    assert struct.unpack_from('<I', b, len(b)-4)[0] == zlib.crc32(b[:-4])
    rows = [struct.unpack_from('<III', b, 0xa8+i*12) for i in range(3)]
    assert [x[0] for x in rows] == [0, 1, 2]
    assert rows[0][1] == 204
    assert all(o+n <= len(b)-4 for _, o, n in rows)
    assert all(rows[i][1]+rows[i][2] == rows[i+1][1] for i in range(2))
    assert rows[-1][1]+rows[-1][2] == len(b)-4
    return [b[o:o+n] for _, o, n in rows]


def mode_graph(rows):
    """Reproduce loader's alias-to-parent type/value comparison, not raw paths."""
    canon, parent, packed = {}, {}, {}
    for idx, value, extra, up, link in rows:
        assert idx == len(canon)
        assert link == 0xffffffff or link < len(rows)
        assert up <= idx
        cp = canon.get(up)
        if idx != up and cp is not None and packed[cp] == value:
            canon[idx] = cp
        else:
            canon[idx] = idx
            packed[idx] = value
            parent[idx] = None if idx == up else cp
    return canon, parent, packed


class LookupModel:
    """Bounded model of the captured loader and GetModule lookup instructions.

    This is a host-side selection model, not execution of Qualcomm's parser.
    Root data content and native object deserialization remain runtime gates.
    """
    def __init__(self, modes, records, *, reuse_source_sid=False):
        self.reuse_source_sid = reuse_source_sid
        self.canon, self.parent, self.packed = mode_graph(modes)
        self.children = {}
        self.links = {}
        self.modules = {}
        self.private_links = set()
        self.stock_clone_sids = set()
        self.root = None
        for idx, value, extra, up, link in modes:
            if self.canon[idx] != idx:
                continue
            if self.root is None:
                self.root = idx
            self.links[idx] = None if link == 0xffffffff else self.canon[link]
            parent = self.parent[idx]
            if parent is not None:
                self.children.setdefault(parent, []).append(idx)
        for record in records:
            sid, name, mid, off, size, alias = record
            if mid == 0xffffffff:
                continue
            # ReadSections checks existing module name before inserting (first wins).
            key = (self.canon[mid], name.split(b'\0')[0])
            self.modules.setdefault(key, record)

    def follow(self, node):
        seen = set()
        while self.links[node] is not None:
            assert node not in seen, 'Linked-mode cycle'
            seen.add(node)
            node = self.links[node]
        return node

    def get_record(self, name, values):
        current = self.root
        result = self.modules.get((self.follow(current), name))
        for typ, value in enumerate(values[1:], 1):
            effective = self.follow(current)
            target = typ | (value << 16)
            found = next((child for child in self.children.get(effective, [])
                          if self.packed[child] == target), None)
            if found is None:
                break
            current = found
            record = self.modules.get((self.follow(current), name))
            if record is not None:
                result = record
        return result

    def get(self, name, values):
        result = self.get_record(name, values)
        return None if result is None else (result[3], result[4])

    def privatize_link(self, node, modes, output):
        """Give each link entry a private target, retaining its original maps.

        The old node stays linked so its formerly hidden local modules remain
        hidden. A detached copy of the effective target supplies identical stock
        modules and linked child copies. Further copies are made only on paths
        required by Natural, before registering any Natural modules.
        """
        if self.links[node] is None or node in self.private_links:
            return
        target = self.follow(node)
        new_id = len(modes)
        modes.append((new_id, self.packed[target], modes[target][2], new_id, 0xffffffff))
        self.canon[new_id] = new_id
        self.parent[new_id] = None
        self.packed[new_id] = self.packed[target]
        self.links[new_id] = None
        for (at, name), source in list(self.modules.items()):
            if at != target:
                continue
            sid = len(output)+1
            source_sid = source[5] if self.reuse_source_sid else sid
            clone = (sid, source[1], new_id, source[3], source[4], source_sid)
            output.append(clone)
            self.modules[(new_id, name)] = clone
            self.stock_clone_sids.add(sid)
        for child in list(self.children.get(target, [])):
            child_id = len(modes)
            modes.append((child_id, self.packed[child], modes[child][2], new_id, child))
            self.canon[child_id] = child_id
            self.parent[child_id] = new_id
            self.packed[child_id] = self.packed[child]
            self.links[child_id] = child
            self.children.setdefault(new_id, []).append(child_id)
        row = modes[node]
        modes[node] = (*row[:4], new_id)
        self.links[node] = new_id
        self.private_links.add(node)

    def ensure_path(self, values, modes, output, privatize=False):
        """Fill empty intermediate nodes so the native early-stop reaches effect."""
        current = self.root
        for typ, value in enumerate(values[1:], 1):
            if privatize:
                self.privatize_link(current, modes, output)
            effective = self.follow(current)
            target = typ | (value << 16)
            found = next((c for c in self.children.get(effective, [])
                          if self.packed[c] == target), None)
            if found is None:
                found = len(modes)
                modes.append((found, target, 0, effective, 0xffffffff))
                self.canon[found] = found
                self.parent[found] = effective
                self.packed[found] = target
                self.links[found] = None
                self.children.setdefault(effective, []).append(found)
            current = found
        if privatize:
            self.privatize_link(current, modes, output)
        return self.follow(current)

    def queries(self):
        values = set()
        for node in self.packed:
            row = [0]*7
            seen = set()
            chain = []
            while node is not None:
                assert node not in seen
                seen.add(node)
                chain.append(self.packed[node])
                node = self.parent[node]
            for entry in reversed(chain):
                typ, value = entry & 65535, entry >> 16
                assert typ < 7
                row[typ] = value
            values.add(tuple(row))
        # Actual preview observation + capture Feature1=2 and classic sensor=33.
        for sensor in (7, 33):
            for feature1 in (2, 36):
                values.add((0, sensor, 0, feature1, 0, 46, 0))
        return values

