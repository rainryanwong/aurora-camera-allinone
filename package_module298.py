"""Package All in One 2.0.81 over the accepted 2.0.78 runtime."""

import copy
import hashlib
import json
from pathlib import Path
import re
import zipfile


ROOT = Path(__file__).resolve().parent
RELEASE = ROOT / 'release'
BASE = RELEASE / 'camera68_aurora_allinone_v2.0.78_Stock1770_Compat01125_LiveMotion052_AUTH.zip'
OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.0.81_Stock1770_Compat01130_LiveMotion053_AUTH.zip'


def web_files():
    registry = (ROOT / 'src/com/aurora/camera/compat/FeatureRegistry.java').read_text(encoding='utf-8')
    groups = {
        'compat': dict(re.findall(r'add\(plans, "([^"]+)", "([^"]+)",', registry)),
        'live-motion': {'LiveMotionAttach': '实况运镜加载入口', 'LiveMotionModule': '实况运镜'},
        'editor': {
            'EditorAbi250': '相册编辑版本适配',
            'EditorLegendaryGallery': '传奇一瞬相册入口',
            'EditorFeatureDevices293': '相册机型与功能识别',
            'Editor245Hooks': '相册水印与签名',
            'EditorMore246Hooks': '相册导入 LUT',
        },
    }
    files = {path.relative_to(ROOT).as_posix(): path.read_bytes()
             for path in (ROOT / 'webroot').iterdir() if path.is_file()}
    files['webroot/groups.js'] = ('window.auroraGroups = ' + json.dumps(groups, ensure_ascii=False)
                                  + ';\n').encode('utf-8')
    return files


def package_runtime():
    updates = {
        'camera-compat.apk': (RELEASE / 'CameraCompat-0.1.130.apk').read_bytes(),
        'live-motion.apk': (ROOT / 'live-motion/release/6.8Cam-LiveMotion-0.5.3.apk').read_bytes(),
    }
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, 'x') as output:
        for item in previous.infolist():
            name = item.filename
            if name == 'runtime.sha256':
                continue
            data = updates[name] if name in updates else previous.read(item)
            if name == 'module.prop':
                data = data.replace(b'version=2.0.78\n', b'version=2.0.81\n')
                data = data.replace(b'versionCode=20078\n', b'versionCode=20081\n')
            output.writestr(copy.copy(item), data)
            hashes[name] = hashlib.sha256(data).hexdigest()
        for name, data in web_files().items():
            output.writestr(name, data, compress_type=zipfile.ZIP_DEFLATED)
            hashes[name] = hashlib.sha256(data).hexdigest()
        manifest = ''.join(f'{digest}  {name}\n' for name, digest in sorted(hashes.items()))
        output.writestr(copy.copy(previous.getinfo('runtime.sha256')), manifest.encode('utf-8'))


if __name__ == '__main__':
    package_runtime()
    with OUTPUT.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    print(OUTPUT, OUTPUT.stat().st_size, digest)
