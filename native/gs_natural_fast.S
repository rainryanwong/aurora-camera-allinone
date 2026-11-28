.section .text,"ax",%progbits
.global natural_scene
natural_scene:
    ldr w9, [x8, #0x20]
    cmp w9, #36
    b.ne photo_scene
    ldr w9, [x8, #0x30]
    cmp w9, #42
    b.eq .Lscene42
    cmp w9, #4
    b.eq .Lshort
    cmp w9, #45
    b.ne photo_scene
.Lshort:
    ldr w10, [x8, #0x38]
    ldr w11, [x8, #0x10]
    adr x13, .Lshort_routes
.Lfind:
    ldrb w14, [x13], #1
    cmp w14, #255
    b.eq photo_scene
    cmp w11, w14
    b.ne .Lnext
    ldrb w14, [x13]
    cmp w9, w14
    b.ne .Lnext
    ldrb w14, [x13, #1]
    cmp w10, w14
    b.ne .Lnext
    ldrb w9, [x13, #2]
    b store_effect
.Lnext:
    add x13, x13, #3
    b .Lfind

.Lscene42:
    ldr w9, [x8, #0x10]
    cmp w9, #63
    b.hi photo_scene
    mov w14, #SCENE42_SENSOR_MASK
    lsr x14, x14, x9
    tbz x14, #0, photo_scene
    ldr w9, [x8, #0x38]
    cbz w9, .Lscene42_zero
    cmp w9, #28
    b.ne photo_scene
    mov w9, #94
    b store_effect
.Lscene42_zero:
    mov w9, #93
    b store_effect

.section .capture,"ax",%progbits
.global natural_capture_gate
natural_capture_gate:
    ldr w9, [x8, #0x18]
    cmp w9, #1
    b.ne capture_profile
    adr x13, exact_capture_table
    b exact_capture_loop

.Lshort_routes:
    .include "gs_natural_short.inc"
