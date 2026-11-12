from pathlib import Path
import hashlib
import json
import os
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent
JAVA = Path('D:/DevTools/jdks/jdk-21.0.12.1+1/bin')
BT = Path('D:/DevTools/AndroidSdk/build-tools/37.0.0')
SDK = Path('D:/DevTools/AndroidSdk/platforms/android-37.0/android.jar')
STUBS = ROOT / 'api-stubs'
KEY = Path(os.environ.get('AURORA_LSP_KEYSTORE', 'D:/cam18pm/m3-port/m3-v16-probe-debug.jks'))
BASE = Path('D:/cam18pm/analysis/watermark-topbar-perf-20261111/static/camera1770.apk')
BASE_SHA = 'e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07'
VERSION = '0.5.2'
VERSION_CODE = 107
WORK = ROOT / 'build-v052'
OUT = ROOT / f'release/6.8Cam-LiveMotion-{VERSION}.apk'
SOURCES = list((ROOT/'src').rglob('*.java'))

def sha(p):
    return hashlib.file_digest(p.open('rb'), 'sha256').hexdigest()

def run(*args):
    env = os.environ.copy()
    env['JAVA_HOME'] = str(JAVA.parent)
    p = subprocess.run([str(x) for x in args], capture_output=True, env=env)
    with (WORK/'build.log').open('ab') as log:
        log.write(p.stdout + p.stderr)
    if p.returncode:
        raise RuntimeError(f'{Path(args[0]).name} exit {p.returncode}; see build.log')
    return p.stdout.decode('utf-8', errors='replace')

def main():
    assert sha(BASE) == BASE_SHA, 'Camera input identity changed'
    WORK.mkdir(exist_ok=True)
    OUT.parent.mkdir(exist_ok=True)
    (WORK/'build.log').write_bytes(b'')
    tree = WORK/'package'
    for d in [tree/'res/values', tree/'assets', WORK/'classes', WORK/'dex']:
        d.mkdir(parents=True, exist_ok=True)
    manifest = f'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.aurora.camera.livemotion" android:versionCode="{VERSION_CODE}" android:versionName="{VERSION}"><uses-sdk android:minSdkVersion="31" android:targetSdkVersion="35"/><application android:label="6.8Cam实况运镜" android:allowBackup="false" android:hasCode="true"><meta-data android:name="xposedmodule" android:value="true"/><meta-data android:name="xposeddescription" android:value="Aurora 6.8.001770 独立运镜适配"/><meta-data android:name="xposedminversion" android:value="82"/><meta-data android:name="xposedscope" android:resource="@array/xposed_scope"/></application></manifest>'''
    (tree/'AndroidManifest.xml').write_text(manifest, encoding='utf-8')
    (tree/'res/values/arrays.xml').write_text('<resources><string-array name="xposed_scope"><item>com.android.camera</item></string-array></resources>', encoding='utf-8')
    (tree/'assets/xposed_init').write_text('com.aurora.camera.livemotion.LiveMotionModule\n', encoding='utf-8')
    files = SOURCES + list(STUBS.rglob('*.java'))
    run(JAVA/'javac.exe', '--release', '8', '-encoding', 'UTF-8', '-cp', SDK, '-d', WORK/'classes', *files)
    run(JAVA/'jar.exe', 'cf', WORK/'module.jar', '-C', WORK/'classes', 'com')
    run(BT/'d8.bat', '--min-api', '31', '--lib', SDK, '--output', WORK/'dex', WORK/'module.jar')
    # aapt builds only the small independent module resource table.
    run(BT/'aapt.exe', 'package', '-f', '-M', tree/'AndroidManifest.xml', '-S', tree/'res',
        '-I', SDK, '-F', WORK/'resources.apk')
    with zipfile.ZipFile(WORK/'resources.apk') as source, zipfile.ZipFile(WORK/'unsigned.apk', 'w') as out:
        for item in source.infolist(): out.writestr(item, source.read(item.filename))
        out.writestr('assets/xposed_init', (tree/'assets/xposed_init').read_bytes())
        out.writestr('classes.dex', (WORK/'dex/classes.dex').read_bytes())
    run(BT/'zipalign.exe', '-P', '16', '-f', '4', WORK/'unsigned.apk', WORK/'aligned.apk')
    run(BT/'apksigner.bat', 'sign', '--ks', KEY,
        '--ks-key-alias', os.environ.get('AURORA_LSP_KEY_ALIAS', 'm3probe'),
        '--ks-pass', 'env:AURORA_LSP_KS_PASS', '--key-pass', 'env:AURORA_LSP_KEY_PASS',
        '--out', OUT, WORK/'aligned.apk')
    signed = run(BT/'apksigner.bat', 'verify', '--verbose', '--print-certs', OUT)
    (WORK/'signature.txt').write_text(signed, encoding='utf-8')
    run(BT/'zipalign.exe', '-c', '-P', '16', '4', OUT)
    badging = run(BT/'aapt.exe', 'dump', 'badging', OUT)
    (WORK/'badging.txt').write_text(badging, encoding='utf-8')
    with zipfile.ZipFile(OUT) as z:
        assert z.testzip() is None
        assert len(z.namelist()) == len(set(z.namelist()))
        assert z.read('assets/xposed_init').decode().strip() == 'com.aurora.camera.livemotion.LiveMotionModule'
    assert f"package: name='com.aurora.camera.livemotion' versionCode='{VERSION_CODE}' versionName='{VERSION}'" in badging
    assert 'uses-permission:' not in badging
    result = dict(apk=str(OUT), sha256=sha(OUT), size=OUT.stat().st_size,
                  package='com.aurora.camera.livemotion', version=VERSION, version_code=VERSION_CODE,
                  original_module_sha256=sha(ROOT/'inputs/Phoenix-Q0Fix-8.10-a810.apk'),
                  camera=str(BASE), camera_sha256=BASE_SHA,
                  scope=['com.android.camera'], config_types=['2', '3', '0'],
                  signatures_verified=True, alignment_verified=True, zip_verified=True,
                  installed=False, runtime_verified=False,
                  source_files={str(p.relative_to(ROOT)): sha(p) for p in SOURCES})
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == '__main__': main()
