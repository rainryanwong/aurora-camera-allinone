.text
.p2align 2
.global lens_profile
.macro capture_found
    str w9, [sp, #0x368]
    b copy_query
.endm
.macro capture_legacy
    ldr w9, [x8, #16]
    b original_sensor
.endm
lens_profile:
    adr x13, lens_queries
next_query:
    ldrb w14, [x13]
    cmp w14, #255
    b.eq capture_profile
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
    ldrb w9, [x13, #7]
    capture_found
advance_query:
    add x13, x13, #8
    b next_query
    .include "natural_capture_match.inc"
.p2align 2
lens_queries:
    .include "lens_queries.inc"
