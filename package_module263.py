"""Package installer and boot reliability changes over the accepted 2.0.49 module."""
from pathlib import Path
import copy
import hashlib
import json
import sys
import zipfile

ROOT = Path(__file__).resolve().parent
BASE = ROOT / "release/camera68_aurora_allinone_v2.0.49_Stock1770_Compat0175_Native226_AUTH.zip"
BASE_SOURCE = ROOT / "release/CameraCompat_2.0.49_source.zip"
OUTPUT = ROOT / "release/camera68_aurora_allinone_v2.0.50_Stock1770_Compat0175_Native226_AUTH.zip"
SOURCE = ROOT / "release/CameraCompat_2.0.50_source.zip"
RUNTIME = {
    "post-fs-data.sh": "runtime239/post-fs-data.sh",
    "auth239/service.sh": "auth239/service.sh",
    "uninstall.sh": "runtime247/uninstall.sh",
    "customize.sh": "runtime247/customize.sh",
    "m3-relay.sh": "runtime263/m3-relay.sh",
    "account.sh": "runtime263/account.sh",
}

def main():
    helper = Path(sys.argv[1])
    replacements = {name: (ROOT / path).read_bytes() for name, path in RUNTIME.items()}
    replacements["camera-lsp-setup.jar"] = helper.read_bytes()
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, "x") as output:
        for item in previous.infolist():
            if item.filename == "runtime.sha256":
                continue
            payload = replacements.get(item.filename)
            if payload is None:
                payload = previous.read(item.filename)
            if item.filename == "module.prop":
                payload = payload.replace(b"version=2.0.49\n", b"version=2.0.50\n").replace(b"versionCode=20049\n", b"versionCode=20050\n").replace(b"Xioami", b"Xiaomi")
            output.writestr(copy.copy(item), payload)
            hashes[item.filename] = hashlib.sha256(payload).hexdigest()
        payload = "".join(f"{value}  {name}\n" for name, value in sorted(hashes.items())).encode("utf8")
        output.writestr(copy.copy(previous.getinfo("runtime.sha256")), payload)
    updates = set(RUNTIME.values()) | {"installer/java/CameraLspSetup.java", "installer/build_setup.py", "tests/installer/run.py", "package_module263.py"}
    with zipfile.ZipFile(BASE_SOURCE) as previous, zipfile.ZipFile(SOURCE, "x", zipfile.ZIP_DEFLATED, compresslevel=9) as output:
        for item in previous.infolist():
            payload = (ROOT / item.filename).read_bytes() if item.filename in updates else previous.read(item.filename)
            output.writestr(item.filename, payload)
            updates.discard(item.filename)
        for name in sorted(updates):
            output.write(ROOT / name, name)
    for path in (OUTPUT, SOURCE):
        with path.open("rb") as stream:
            sha = hashlib.file_digest(stream, "sha256").hexdigest()
        print(json.dumps({"file": str(path), "bytes": path.stat().st_size, "sha256": sha}, ensure_ascii=False))

if __name__ == "__main__":
    main()
