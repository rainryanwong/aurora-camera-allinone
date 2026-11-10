"""Package camera startup and preview optimizations."""
from pathlib import Path
import copy
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent
BASE = ROOT / "release/camera68_aurora_allinone_v2.0.48_Stock1770_Compat0174_Native226_AUTH.zip"
APK = ROOT / "release/CameraCompat-0.1.75.apk"
OUTPUT = ROOT / "release/camera68_aurora_allinone_v2.0.49_Stock1770_Compat0175_Native226_AUTH.zip"
SOURCE = ROOT / "release/CameraCompat_2.0.49_source.zip"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    hashes = {}
    with zipfile.ZipFile(BASE) as previous, zipfile.ZipFile(OUTPUT, "x") as output:
        for item in previous.infolist():
            if item.filename == "runtime.sha256":
                continue
            payload = previous.read(item.filename)
            if item.filename == "camera-compat.apk":
                payload = APK.read_bytes()
            elif item.filename == "module.prop":
                payload = payload.replace(b"version=2.0.48\n", b"version=2.0.49\n").replace(b"versionCode=20048\n", b"versionCode=20049\n")
            output.writestr(copy.copy(item), payload)
            hashes[item.filename] = digest(payload)
        payload = "".join(f"{value}  {name}\n" for name, value in sorted(hashes.items())).encode("utf8")
        output.writestr(copy.copy(previous.getinfo("runtime.sha256")), payload)

    build = json.loads(APK.with_suffix(".json").read_text("utf8"))
    snapshot = Path(build["input_snapshot"])
    inputs = set(build["source_files"]) | set(build["asset_files"]) | set(build["extra_input_files"])
    files = {name: snapshot / name for name in inputs}
    for folder in ("auth239", "installer", "runtime239", "runtime240", "runtime241", "runtime242", "runtime243", "runtime245", "runtime246", "runtime247", "runtime248"):
        for path in (ROOT / folder).rglob("*"):
            if path.is_file():
                files[path.relative_to(ROOT).as_posix()] = path
    for name in ("build_lsp.py", "package_module262.py", "notes.md"):
        files[name] = ROOT / name
    for path in ROOT.glob("LICENSE*"):
        if path.is_file():
            files[path.name] = path
    with zipfile.ZipFile(SOURCE, "x", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, path in sorted(files.items()):
            archive.write(path, name)
    for path in (OUTPUT, SOURCE):
        with path.open("rb") as stream:
            sha = hashlib.file_digest(stream, "sha256").hexdigest()
        print(json.dumps({"file": str(path), "bytes": path.stat().st_size, "sha256": sha}, ensure_ascii=False))


if __name__ == "__main__":
    main()
