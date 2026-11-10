"""Package Natural tuning registrations for the supported SR input modes."""
from pathlib import Path
import copy
import hashlib
import zipfile
import package_natural_lenses as lenses

ROOT = Path(__file__).resolve().parent
BASE = ROOT / 'release/camera68_aurora_allinone_v2.0.57_Stock1770_Compat0195_AUTH.zip'
BASE_SOURCE = ROOT / 'release/CameraCompat_2.0.57_source.zip'
OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.58_Stock1770_Compat0195_AUTH.zip'
SOURCE = ROOT / 'release/CameraCompat_2.0.58_source.zip'
QUERIES = {
    'com.qti.tuned.aurora_semco_LYT900_wide.bin': (
        (0, 53, 1, 36, 31, 0, 76),
        (0, 53, 1, 51, 105, 0, 76),
    ),
    'com.qti.tuned.aurora_ofilm_imx858_tele3x.bin': (
        (0, 1, 1, 51, 105, 0, 76),
    ),
    'com.qti.tuned.aurora_semco_imx858_tele5x.bin': (
        (0, 1, 1, 24, 31, 0, 76),
        (0, 1, 1, 51, 106, 0, 76),
        (0, 12, 1, 24, 31, 0, 76),
        (0, 12, 1, 51, 106, 0, 76),
        (0, 16, 1, 24, 31, 0, 76),
        (0, 16, 1, 51, 106, 0, 76),
        (0, 16, 1, 24, 31, 46, 76),
        (0, 16, 1, 51, 106, 46, 76),
    ),
}
BRIDGE_ADDITIONS = (
    (0, 53, 1, 36, 31, 0, 0),
    (0, 12, 1, 24, 31, 0, 0),
    (0, 16, 1, 24, 31, 0, 0),
    (0, 16, 1, 24, 31, 46, 0),
)


def main():
    lenses.BUILD = ROOT / 'build' / 'module277'
    with zipfile.ZipFile(BASE) as previous:
        additions = {'natural-payload/' + name: lenses.tuned_payload(
            previous.read('natural-payload/' + name), name, queries)
            for name, queries in QUERIES.items()}
        mivi = 'natural-payload/com.xiaomi.plugin.offlinepostproc.so'
        gs = 'natural-payload/com.qti.feature2.gs.sm8650.so'
        additions[mivi] = lenses.mivi_payload(previous.read(mivi), lenses.BRIDGE_QUERIES + BRIDGE_ADDITIONS)
        gs_queries = tuple(q for q in lenses.BRIDGE_QUERIES if q[3:5] != (36, 61)) + BRIDGE_ADDITIONS
        additions[gs] = lenses.gs_payload(previous.read(gs), gs_queries)
        lines = []
        for line in previous.read('natural-files.list').decode('utf-8').splitlines():
            fields = line.split()
            member = 'natural-payload/' + fields[0]
            if member in additions:
                fields[-1] = hashlib.sha256(additions[member]).hexdigest()
            lines.append(' '.join(fields))
        additions['natural-files.list'] = ('\n'.join(lines) + '\n').encode('utf-8')
        hashes = {}
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                if item.filename == 'runtime.sha256':
                    continue
                payload = additions.get(item.filename)
                if payload is None:
                    payload = previous.read(item.filename)
                if item.filename == 'module.prop':
                    payload = payload.replace(b'version=2.0.57\n', b'version=2.0.58\n')
                    payload = payload.replace(b'versionCode=20057\n', b'versionCode=20058\n')
                output.writestr(copy.copy(item), payload)
                hashes[item.filename] = hashlib.sha256(payload).hexdigest()
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')),
                ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items())).encode('utf-8'))
    replacements = {'notes.md': ROOT / 'notes.md', Path(__file__).name: Path(__file__),
                    'package_natural_lenses.py': ROOT / 'package_natural_lenses.py'}
    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            path = replacements.pop(item.filename, None)
            output.writestr(copy.copy(item), path.read_bytes() if path else previous.read(item.filename))
        for name, path in replacements.items():
            output.write(path, name)
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)


if __name__ == '__main__':
    main()
