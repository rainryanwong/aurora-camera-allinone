"""Add isolated NaturalTuning selection to the 2.0.56-entry1 module."""
from pathlib import Path
import copy
import hashlib
import struct
import zipfile

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.56-entry1_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.56-entry1_source.zip'
TUNING_SOURCE = ROOT / 'release/camera68_aurora_allinone_v2.0.56_Stock1770_Compat0195_Native226_AUTH.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.56-natural1_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.56-natural1_source.zip'
MIVI = 'natural-payload/com.xiaomi.plugin.offlinepostproc.so'
# Fixed 2.0.56 plugin: these ELF RVAs equal file offsets in its executable segment.
# MiMetadata::Entry begins at SP+0x2a0; type is uint32 at Entry+8.
ENTRY_TYPE_OFFSETS = (0x31514, 0x31564, 0x315b4, 0x31604, 0x3165c, 0x316a8)
LDR_W8_SP_2A8 = 0xb9400000 | ((0x2a8 // 4) << 10) | (31 << 5) | 8


def main():
    hashes = {}
    with zipfile.ZipFile(BASE) as base, zipfile.ZipFile(TUNING_SOURCE) as tuning:
        additions = {
            name: tuning.read(name)
            for name in tuning.namelist()
            if name.startswith('natural-payload/')
            or name in ('natural-activate.sh', 'natural-files.list', 'bin/natural-remount-ro')
        }
        plugin = bytearray(additions[MIVI])
        for offset in ENTRY_TYPE_OFFSETS:
            struct.pack_into('<I', plugin, offset, LDR_W8_SP_2A8)
        additions[MIVI] = bytes(plugin)
        lines = []
        for line in additions['natural-files.list'].decode('utf-8').splitlines():
            fields = line.split()
            fields[-1] = hashlib.sha256(additions['natural-payload/' + fields[0]]).hexdigest()
            lines.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(lines) + '\n').encode('utf-8')

        activation = (
            b'# Load NaturalTuning selection before provider startup.\n'
            b'NATURAL_RC=0\n'
            b'/system/bin/sh "$MODDIR/natural-activate.sh" || NATURAL_RC=$?\n'
            b'echo "natural_activation_exit=$NATURAL_RC; details: $MODDIR/natural-activation.log"\n\n'
        )
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in base.infolist():
                name = item.filename
                if name == 'runtime.sha256':
                    continue
                payload = base.read(name)
                if name == 'module.prop':
                    payload = payload.replace(b'version=2.0.56-entry1\n', b'version=2.0.56-natural1\n')
                elif name == 'customize.sh':
                    payload = payload.replace(b'lutshare-access.sh root-compat.sh', b'lutshare-access.sh natural-activate.sh root-compat.sh')
                    anchor = b'set_perm "$MODPATH/bin/m3_aux_relay" 0 0 0755\n'
                    payload = payload.replace(anchor, anchor + b'set_perm "$MODPATH/bin/natural-remount-ro" 0 0 0755\n')
                elif name == 'post-fs-data.sh':
                    pos = payload.index(b'# AUTHVignette:')
                    payload = payload[:pos] + activation + payload[pos:]
                output.writestr(copy.copy(item), payload)
                hashes[name] = hashlib.sha256(payload).hexdigest()
            for name, payload in additions.items():
                output.writestr(copy.copy(tuning.getinfo(name)), payload)
                hashes[name] = hashlib.sha256(payload).hexdigest()
            manifest = ''.join(f'{value}  {name}\n' for name, value in sorted(hashes.items()))
            output.writestr(copy.copy(base.getinfo('runtime.sha256')), manifest.encode('utf-8'))

    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            output.writestr(copy.copy(item), previous.read(item.filename))
        output.write(Path(__file__), Path(__file__).name)
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(f'{path}\nbytes={path.stat().st_size}\nsha256={digest}')


if __name__ == '__main__':
    main()
