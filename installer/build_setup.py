"""Compile only the local three-LSP setup helper. Never opens a device/database."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import uuid
import zipfile

ROOT=Path(__file__).resolve().parent.parent
JAVA=Path('D:/DevTools/jdks/jdk-21.0.12.1+1/bin')
BT=Path('D:/DevTools/AndroidSdk/build-tools/37.0.0')
SDK=Path('D:/DevTools/AndroidSdk/platforms/android-37.0/android.jar')

def main():
    source=ROOT/'installer/java/CameraLspSetup.java'
    test=json.loads((ROOT/'tests/installer/RESULT.json').read_text('utf-8'))
    digest=hashlib.sha256(source.read_bytes()).hexdigest()
    if not test['passed'] or test['source_sha256']!=digest:
        raise RuntimeError('Actual three-LSP installer tests do not match current source')
    work=ROOT/'installer/build'/('setup-'+uuid.uuid4().hex[:10])
    for d in [work/'classes',work/'dex']:d.mkdir(parents=True,exist_ok=True)
    frozen=work/'CameraLspSetup.java';frozen.write_bytes(source.read_bytes())
    env=dict(os.environ,JAVA_HOME=str(JAVA.parent))
    steps=[('javac',[JAVA/'javac.exe','--release','8','-encoding','UTF-8','-cp',SDK,'-d',work/'classes',frozen]),
           ('jar',[JAVA/'jar.exe','cf',work/'classes.jar','-C',work/'classes','.']),
           ('d8',[BT/'d8.bat','--min-api','31','--lib',SDK,'--output',work/'dex',work/'classes.jar'])]
    for name,args in steps:
        p=subprocess.run([str(x) for x in args],capture_output=True,env=env)
        (work/(name+'.log')).write_bytes(p.stdout+p.stderr)
        if p.returncode:raise RuntimeError(f'{name} failed; see {work/(name+".log")}')
    output=work/'camera-lsp-setup.jar'
    with zipfile.ZipFile(output,'x') as z:z.write(work/'dex/classes.dex','classes.dex')
    with zipfile.ZipFile(output) as z:
        assert z.namelist()==['classes.dex'] and z.testzip() is None
    report=dict(artifact=str(output),sha256=hashlib.sha256(output.read_bytes()).hexdigest(),
                source=str(source),source_sha256=digest,source_snapshot=str(frozen),
                modules=['com.prometheus.camera.m3v16save','com.aurora.camera.livemotion','com.aurora.camera.compat'],
                scopes=[['com.android.camera','com.miui.mediaeditor'],['com.android.camera'],['com.android.camera','com.miui.mediaeditor']],
                legacy_two_module_backup_supported=True,tests=test['result'],
                device_operations=False,installed=False)
    (work/'BUILD_RESULT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=='__main__':main()
