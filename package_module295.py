"""Package All in One 2.0.76 with the Natural Photo preview payloads."""

import copy
import hashlib
import sys
import zipfile

sys.dont_write_bytecode = True
import build_natural_preview as preview


ROOT = preview.ROOT
RELEASE = ROOT / 'release'
PAYLOAD = preview.BUILD / 'natural-payload'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.76_Stock1770_Compat01124_LiveMotion051_AUTH.zip'
BASE_SOURCE = RELEASE / 'CameraCompat_2.0.75_source.zip'
SOURCE = RELEASE / 'CameraCompat_2.0.76_source.zip'
SOURCE_FILES = ('package_module295.py', 'build_natural_preview.py',
                'native/gs_preview.S', 'native/natural_lookup_model.py')


def package_runtime(*, base=preview.BASE, output_path=OUTPUT, payload=PAYLOAD,
                    version=('2.0.75', '2.0.76'), version_code=(20075, 20076)):
    updates = {f'natural-payload/{name}': payload / name
               for name in (preview.GS, *preview.ROUTE_SENSORS)}
    hashes = {}
    with zipfile.ZipFile(base) as previous, zipfile.ZipFile(output_path, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name in ('natural-files.list', 'runtime.sha256'):
                continue
            data = updates[name].read_bytes() if name in updates else previous.read(name)
            if name == 'module.prop':
                for key, (old, new) in (('version', version), ('versionCode', version_code)):
                    data = data.replace(f'{key}={old}\n'.encode(), f'{key}={new}\n'.encode())
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()

        lines = []
        for line in previous.read('natural-files.list').decode('utf-8').splitlines():
            fields = line.split()
            fields[-1] = hashes['natural-payload/' + fields[0]]
            lines.append(' '.join(fields))
        manifest = ('\n'.join(lines) + '\n').encode('utf-8')
        output.writestr(copy.copy(previous.getinfo('natural-files.list')), manifest)
        hashes['natural-files.list'] = hashlib.sha256(manifest).hexdigest()

        manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))


def package_source():
    updates = {name: ROOT / name for name in SOURCE_FILES}
    with zipfile.ZipFile(BASE_SOURCE) as previous, \
            zipfile.ZipFile(SOURCE, 'x', zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            path = updates.pop(item.filename, None)
            output.writestr(copy.copy(item), path.read_bytes() if path else previous.read(item.filename))
        for name, path in updates.items():
            output.write(path, name)


if __name__ == '__main__':
    package_runtime()
    package_source()
    for path in (OUTPUT, SOURCE):
        with path.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        print(path, path.stat().st_size, digest, flush=True)
