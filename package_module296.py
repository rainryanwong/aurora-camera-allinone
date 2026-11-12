"""Package All in One 2.0.77 with the Natural preview payloads."""

import hashlib
import sys

sys.dont_write_bytecode = True
from build_natural_preview import MODE_IPE_BUILD, ROOT, SKY_BASE
from package_module295 import package_runtime


OUTPUT = ROOT / 'release/camera68_aurora_allinone_v2.0.77_Stock1770_Compat01124_LiveMotion051_AUTH.zip'


if __name__ == '__main__':
    package_runtime(base=SKY_BASE, output_path=OUTPUT, payload=MODE_IPE_BUILD / 'natural-payload',
                    version=('2.0.76', '2.0.77'), version_code=(20076, 20077))
    with OUTPUT.open('rb') as stream:
        digest = hashlib.file_digest(stream, 'sha256').hexdigest()
    print(OUTPUT, OUTPUT.stat().st_size, digest, flush=True)
