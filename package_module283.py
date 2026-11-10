"""Package Natural full-resolution 16:9 RawSR tuning."""
import sys
sys.dont_write_bytecode = True
from package_module279 import capture

capture.VERSION = '2.0.64'
capture.OUTPUT = capture.ROOT / 'release/camera68_aurora_allinone_v2.0.64_Stock1770_Compat0195_AUTH.zip'
capture.SOURCE = capture.ROOT / 'release/CameraCompat_2.0.64_source.zip'
capture.BASE_SOURCE = capture.ROOT / 'release/CameraCompat_2.0.63_source.zip'
capture.BUILD = capture.ROOT / 'build/module283'
capture.SOURCE_FILES = (*capture.SOURCE_FILES, 'package_module280.py',
                        'package_module281.py', 'package_module282.py',
                        'package_module283.py')

if __name__ == '__main__':
    capture.main()
