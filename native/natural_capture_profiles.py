"""Natural capture queries produced by aurora's HDR and night configuration."""

EFFECTS = {0: 76, 24: 78, 36: 79, 37: 80}
SENSORS = (*range(20), 35)
SENSOR_CANDIDATES = (*SENSORS, 53, 54)
SCENES = (
    (0, 40, 46),
    (0, 32, 40, 46),
    (0, 2, 3, 4, 12, 32, 40, 43, 44, 46, 48, 50, 53, 55),
    (32,),
)

# Each row is Feature1, Feature2, scene group, and source-effect bits.
# Bits 0..3 select source effects 0, 24, 36, and 37 respectively.
_COMMON = (
    (0, 0), (0, 13), (0, 31), (0, 85), (2, 0), (2, 13),
    (22, 0), (22, 13), (22, 31), (30, 0), (30, 13),
    (36, 0), (36, 13), (37, 0), (37, 13),
    (51, 0), (51, 109),
)
PROFILES = tuple((f1, f2, 0, 4 | (8 if f2 == 13 else 0))
                 for f1, f2 in _COMMON) + (
    (24, 31, 1, 4), (36, 31, 1, 4),
    (24, 31, 3, 1), (36, 31, 3, 1),
    (8, 31, 1, 5),
    (44, 91, 0, 5),
    (51, 105, 1, 4), (51, 106, 1, 4),
    (36, 61, 1, 7), (36, 48, 1, 7),
    (11, 30, 1, 7), (11, 48, 1, 7),
    (36, 85, 1, 7), (36, 86, 1, 7), (51, 107, 1, 7),
    (36, 58, 2, 5), (37, 65, 2, 5),
    (37, 66, 2, 5), (37, 67, 2, 5),
    (36, 68, 2, 5), (36, 69, 2, 5),
    (36, 70, 2, 5), (37, 70, 2, 5),
    (36, 87, 2, 5), (37, 87, 2, 5),
    (36, 88, 2, 5), (37, 88, 2, 5),
    (37, 100, 2, 5), (51, 108, 2, 5), (51, 110, 2, 5),
)

EXACT_BINDINGS = tuple(
    ((0, 53, 1, f1, f2, 0, 36), (0, 53, 1, f1, f2, 0, 79))
    for f1, f2 in ((36, 31), (51, 105), (24, 31), (51, 106))
) + tuple(
    ((0, 53, 1, f1, 31, 32, effect),
     (0, 53, 1, f1, 31, 32, EFFECTS[effect]))
    for f1 in (36, 24) for effect in (0, 36)
) + tuple(
    ((0, sensor, 1, 51, f2, 32, effect),
     (0, sensor, 1, 51, f2, 32, EFFECTS[effect]))
    for sensor in (53, 54) for f2 in (105, 106) for effect in (0, 36)
) + tuple(
    ((0, 54, 1, f1, f2, scene, effect),
     (0, 54, 1, f1, f2, scene, EFFECTS[effect]))
    for f1, f2, scene in ((36, 31, 0), (36, 31, 32), (51, 105, 0),
                          (24, 31, 0), (24, 31, 32), (51, 106, 0))
    for effect in (0, 36)
)


LENS_BINDINGS = {
    filename: tuple(
        ((0, sensor, 1, 51, f2, 32, 0), (0, sensor, 1, 51, f2, 32, 76))
        for f2 in (105, 106)
    )
    for filename, sensor in (
        ('com.qti.tuned.aurora_semco_LYT900_wide.bin', 8),
        ('com.qti.tuned.aurora_ofilm_imx858_tele3x.bin', 1),
        ('com.qti.tuned.aurora_semco_imx858_tele5x.bin', 1),
    )
}

LENS_ADDITIONS = {
    filename: tuple(
        ((0, sensor, 1, f1, f2, 32, 0), (0, sensor, 1, f1, f2, 32, 76))
        for f1, f2 in ((36, 13), (51, 109))
    )
    for filename, sensor in (
        ('com.qti.tuned.aurora_semco_LYT900_wide.bin', 4),
        ('com.qti.tuned.aurora_semco_imx858_ultra.bin', 1),
    )
}
LENS_ADDITIONS['com.qti.tuned.aurora_semco_imx858_tele5x.bin'] = tuple(
    ((0, sensor, 1, 51, f2, 32, 0), (0, sensor, 1, 51, f2, 32, 76))
    for sensor in (12, 13, 16, 17) for f2 in (105, 106)
)

# The main-camera MFNR requests still carry source effect0 at both bridges.
EXACT_ROUTES = LENS_ADDITIONS['com.qti.tuned.aurora_semco_LYT900_wide.bin']


def bindings(sensors=SENSOR_CANDIDATES, filename=None):
    for sensor in sensors:
        if sensor not in SENSORS:
            continue
        for f1, f2, group, effect_bits in PROFILES:
            for scene in SCENES[group]:
                for bit, (source_effect, target_effect) in enumerate(EFFECTS.items()):
                    if effect_bits & (1 << bit):
                        source = (0, sensor, 1, f1, f2, scene, source_effect)
                        yield source, source[:6] + (target_effect,)
    for source, target in EXACT_BINDINGS:
        if source[1] in sensors:
            yield source, target
    for source, target in LENS_BINDINGS.get(filename, ()):
        if source[1] in sensors:
            yield source, target
    for source, target in LENS_ADDITIONS.get(filename, ()):
        if source[1] in sensors:
            yield source, target
