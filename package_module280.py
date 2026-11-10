"""Package Natural SR scene effects and HDR exposure-frame tuning."""
import sys
sys.dont_write_bytecode = True
from package_module279 import capture

capture.VERSION = '2.0.61'
capture.OUTPUT = capture.ROOT / 'release/camera68_aurora_allinone_v2.0.61_Stock1770_Compat0195_AUTH.zip'
capture.SOURCE = capture.ROOT / 'release/CameraCompat_2.0.61_source.zip'
capture.BASE_SOURCE = capture.ROOT / 'release/CameraCompat_2.0.60_source.zip'
capture.BUILD = capture.ROOT / 'build/module280'
capture.SOURCE_FILES = (*capture.SOURCE_FILES, 'package_module280.py')

if __name__ == '__main__':
    capture.main()
