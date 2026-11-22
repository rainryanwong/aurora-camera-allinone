"""Package the camera ODM compatibility and Scene 42 updates for 2.0.92."""

import copy
import hashlib
import subprocess
import zipfile

import build_natural_preview as preview
from package_module305 import OUTPUT as BASE, RELEASE, ROOT


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.92_Stock1770_Compat01180_LiveMotion053_AUTH.zip'
BUILD = ROOT / 'build/module306'
RUNTIME = ROOT / 'runtime306'
SCRIPTS = (
    'root-compat.sh', 'post-fs-data.sh', 'natural-activate.sh',
    'palette-activate.sh', 'auth239/post-fs-data.sh',
)
STOCK_ELF_CONTENT = {
    preview.GS: '6d6c072fa6079bb92f52c120aa4f1293e4ffe250f2359293080b171a8314342e',
    'com.xiaomi.plugin.offlinepostproc.so': '26f954622d6d2294793d8786164188aea1c22fbc5870f28536fca30f9462d962',
}
STATIC_GRAPHS = {
    f'payload/odm/etc/camera/xiaomi/{name}snapshot.json' for name in ('leica', 'normal', 'sat')
}
TOOL = 'bin/odm-compat'


def build_odm_compat(build=BUILD):
    output = build / 'odm-compat'
    subprocess.run([
        str(preview.NDK / 'clang++.exe'), '--target=aarch64-linux-android29',
        '-std=c++17', '-O2', '-Wall', '-Wextra', '-Werror', '-static-libstdc++',
        '-Wl,-z,max-page-size=16384', '-Wl,--strip-all',
        str(ROOT / 'native/odm_compat.cpp'), '-o', str(output),
    ], check=True)
    return output.read_bytes()


def scene42_payloads(previous):
    updates = {}
    for filename in preview.SCENE42_SENSORS:
        entry = f'natural-payload/{filename}'
        updates[entry] = preview.tuned_payload(previous.read(entry), filename,
                                               bind=preview.scene42_bindings)
    with zipfile.ZipFile(preview.BASE) as original:
        entry = f'natural-payload/{preview.GS}'
        updates[entry] = preview.gs_payload(
            original.read(entry), build=BUILD, sky_scenes=True, photo_scenes=True, scene4=True,
            extra_sky_routes=preview.SCENE42_EXTRA_ROUTES,
        )
    return updates


def module_updates(previous):
    updates = {name: (RUNTIME / name).read_bytes() for name in SCRIPTS}
    for name in ('webroot/diagnostics.sh', 'webroot/feature-help.js'):
        updates[name] = (ROOT / name).read_bytes()
    updates['customize.sh'] = previous.read('customize.sh').replace(
        b'set_perm "$MODPATH/bin/natural-remount-ro" 0 0 0755\n',
        b'set_perm "$MODPATH/bin/natural-remount-ro" 0 0 0755\n'
        b'set_perm "$MODPATH/bin/odm-compat" 0 0 0755\n',
    )
    updates['module.prop'] = previous.read('module.prop').replace(
        b'version=2.0.91\nversionCode=20091\n', b'version=2.0.92\nversionCode=20092\n',
    )
    updates[TOOL] = build_odm_compat()
    updates.update(scene42_payloads(previous))
    trace_libraries = 'webroot/preview-libraries.txt'
    graph_hash = hashlib.sha256(updates[f'natural-payload/{preview.GS}']).hexdigest()
    rows = []
    for line in previous.read(trace_libraries).decode().splitlines():
        digest, path = line.split()
        if path == f'/odm/lib64/{preview.GS}':
            digest = graph_hash
        rows.append(f'{digest}  {path}\n')
    updates[trace_libraries] = ''.join(rows).encode()
    return updates


def natural_manifest(previous, hashes):
    rows = []
    for line in previous.read('natural-files.list').decode().splitlines():
        name, stock, _, _ = line.split()
        rows.append(f'{name} {STOCK_ELF_CONTENT.get(name, stock)} {hashes["natural-payload/" + name]}\n')
    return ''.join(rows).encode()


def main():
    BUILD.mkdir(parents=True, exist_ok=True)
    hashes = {}
    with zipfile.ZipFile(BASE) as previous:
        updates = module_updates(previous)
        with zipfile.ZipFile(OUTPUT, 'x') as output:
            for item in previous.infolist():
                name = item.filename
                if name in STATIC_GRAPHS or name in ('runtime.sha256', 'natural-files.list'):
                    continue
                data = updates[name] if name in updates else previous.read(item)
                output.writestr(copy.copy(item), data)
                hashes[name] = hashlib.sha256(data).hexdigest()

            item = copy.copy(previous.getinfo('bin/natural-remount-ro'))
            item.filename = TOOL
            output.writestr(item, updates[TOOL])
            hashes[TOOL] = hashlib.sha256(updates[TOOL]).hexdigest()

            manifest = natural_manifest(previous, hashes)
            output.writestr(copy.copy(previous.getinfo('natural-files.list')), manifest)
            hashes['natural-files.list'] = hashlib.sha256(manifest).hexdigest()
            manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
            output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode())

    with OUTPUT.open('rb') as stream:
        print(OUTPUT, OUTPUT.stat().st_size, hashlib.file_digest(stream, 'sha256').hexdigest(), flush=True)


if __name__ == '__main__':
    main()
