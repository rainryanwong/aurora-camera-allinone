"""Package Natural capture tuning with shared native module objects."""
import sys
sys.dont_write_bytecode = True
import package_module278 as capture

capture.VERSION = '2.0.60'
capture.OUTPUT = capture.ROOT / 'release/camera68_aurora_allinone_v2.0.60_Stock1770_Compat0195_AUTH.zip'
capture.SOURCE = capture.ROOT / 'release/CameraCompat_2.0.60_source.zip'
capture.BUILD = capture.ROOT / 'build/module279'
capture.SOURCE_FILES = (*capture.SOURCE_FILES, 'package_module279.py', 'notes.md')

if __name__ == '__main__':
    capture.main()
