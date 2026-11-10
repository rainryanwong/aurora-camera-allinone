"""Build the editor's native environment bridge with the project NDK."""
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
CLANG = Path("D:/DevTools/AndroidSdk/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe")

if __name__ == "__main__":
    subprocess.run([
        str(CLANG), "--target=aarch64-linux-android31", "-std=c11", "-Oz", "-fPIC",
        "-fvisibility=hidden", "-Wall", "-Wextra", "-shared",
        "-Wl,-z,max-page-size=16384", "-Wl,-z,common-page-size=16384",
        "-Wl,--no-undefined", "-Wl,-soname,libaurora_editor.so",
        str(HERE / "environment.c"), "-ldl", "-o", str(HERE / "libaurora_editor.so"),
    ], check=True)
