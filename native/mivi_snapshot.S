.text
.p2align 2
.global snapshot_profile
// Called after the existing style, owner, module and query-layout checks.
// Keep the original matcher for every combination outside this capture path.
snapshot_profile:
    ldr w13, [x8, #16]
    cmp w13, #9
    b.ne legacy_profile
    cmp w10, #1
    b.ne legacy_profile
    cmp w11, #36
    b.ne legacy_profile
    cmp w9, #32
    b.ne legacy_profile
    cmp w12, #85
    b.eq snapshot_effect
    cmp w12, #86
    b.ne legacy_profile
snapshot_effect:
    ldr w13, [x8, #56]
    cbnz w13, legacy_profile
    mov w9, #76
    str w9, [sp, #872]
    b copy_query
legacy_profile:
    cmp w11, #36
    b original_matcher
