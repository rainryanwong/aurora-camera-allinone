"""Package the Natural ultra-wide 16:9 sensor rule for All in One 2.1.13."""
import zipfile

from build_natural_ultra_readout import MEMBER, natural_readout
from package_module310 import RELEASE, write_module
from package_module327 import OUTPUT as BASE


OUTPUT = RELEASE / 'camera68_aurora_allinone_v2.1.13_Stock1770_Compat0312_LiveMotion053_AUTH.zip'


def module_updates(previous):
    properties = previous.read('module.prop')
    version = b'version=2.1.12\nversionCode=20113\n'
    if version not in properties:
        raise ValueError('Expected All in One 2.1.12 metadata')
    return {
        'module.prop': properties.replace(version, b'version=2.1.13\nversionCode=20114\n', 1),
        MEMBER: natural_readout(zipfile.Path(previous, MEMBER)),
    }


if __name__ == '__main__':
    write_module(BASE, OUTPUT, module_updates)
