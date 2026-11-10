.text
.p2align 2
.global gs_lens_profile
.macro capture_found
    b store_effect
.endm
.macro capture_legacy
    ldr w9, [x8, #16]
    b original_gs_sensor
.endm
gs_lens_profile:
    adr x13, gs_lens_queries
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
gs_lens_queries:
    .include "gs_queries.inc"
