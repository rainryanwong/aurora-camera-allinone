"""Package the tested filter module over All in One 2.0.53."""
from pathlib import Path
import copy
import hashlib
import json
import zipfile
ROOT=Path(__file__).resolve().parent
BASE=ROOT/'release/camera68_aurora_allinone_v2.0.53_Stock1770_Compat0190_Native226_AUTH.zip'
BASE_SOURCE=ROOT/'release/CameraCompat_2.0.53_source.zip'
APK=ROOT/'release/CameraCompat-0.1.92-r4.apk'
OUTPUT=ROOT/'release/camera68_aurora_allinone_v2.0.54_Stock1770_Compat0192_Native226_AUTH.zip'
SOURCE=ROOT/'release/CameraCompat_2.0.54_source.zip'
def main():
    hashes={}
    with zipfile.ZipFile(BASE) as previous,zipfile.ZipFile(OUTPUT,'x') as output:
        for item in previous.infolist():
            if item.filename=='runtime.sha256':continue
            payload=APK.read_bytes() if item.filename=='camera-compat.apk' else previous.read(item.filename)
            if item.filename=='module.prop':payload=payload.replace(b'version=2.0.53\n',b'version=2.0.54\n').replace(b'versionCode=20053\n',b'versionCode=20054\n')
            output.writestr(copy.copy(item),payload)
            hashes[item.filename]=hashlib.sha256(payload).hexdigest()
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')),''.join(f'{value}  {name}\n' for name,value in sorted(hashes.items())).encode('utf-8'))
    build=json.loads(APK.with_suffix('.json').read_text('utf-8'))
    snapshot=Path(build['input_snapshot'])
    files={name:snapshot/name for name in set(build['source_files'])|set(build['asset_files'])|set(build['extra_input_files'])}
    for name in ['build_lsp.py','package_module273.py','tools/editor250_contract_types.txt','notes.md','native/m3_aux_relay.c','native/m3_aux_relay']:files[name]=ROOT/name
    with zipfile.ZipFile(BASE_SOURCE) as previous,zipfile.ZipFile(SOURCE,'x',zipfile.ZIP_DEFLATED,compresslevel=9) as output:
        for item in previous.infolist():
            path=files.pop(item.filename,None)
            output.writestr(item.filename,path.read_bytes() if path else previous.read(item.filename))
        for name,path in sorted(files.items()):output.write(path,name)
    for path in [OUTPUT,SOURCE]:
        with path.open('rb') as stream:sha=hashlib.file_digest(stream,'sha256').hexdigest()
        print(json.dumps({'file':str(path),'bytes':path.stat().st_size,'sha256':sha},ensure_ascii=False))
if __name__=='__main__':main()
