.text
.p2align 2
.global gs_lens_profile
// The caller has checked style, owner, module and the seven-entry query layout.
// Match the captured lens requests before the legacy sensor-range restriction.
gs_lens_profile:
    adr x13, gs_lens_queries
next_query:
    ldrb w14, [x13]
    cmp w14, #255
    b.eq legacy_sensor
    add x10, x8, #8
    mov w11, #0
compare_value:
    ldr w9, [x10]
    ldrb w14, [x13, x11]
    cmp w9, w14
    b.ne advance_query
    add x10, x10, #8
    add w11, w11, #1
    cmp w11, #7
    b.ne compare_value
    mov w9, #76
    b store_effect
advance_query:
    add x13, x13, #8
    b next_query
legacy_sensor:
    ldr w9, [x8, #0x10]
    b original_gs_sensor
.p2align 2
gs_lens_queries:
    .include "gs_queries.inc"
