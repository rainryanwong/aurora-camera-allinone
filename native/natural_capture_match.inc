// The enclosing bridge already checked Natural, app owner, and query layout.
capture_profile:
    ldr w9, [x8, #16]
    cmp w9, #19
    b.ls capture_sensor
    cmp w9, #35
    b.ne capture_unmatched
capture_sensor:
    ldr w9, [x8, #24]
    cmp w9, #1
    b.ne capture_unmatched
    ldr w9, [x8, #56]
    mov w15, #1
    mov w16, #76
    cbz w9, capture_effect
    mov w15, #2
    mov w16, #78
    cmp w9, #24
    b.eq capture_effect
    mov w15, #4
    mov w16, #79
    cmp w9, #36
    b.eq capture_effect
    mov w15, #8
    mov w16, #80
    cmp w9, #37
    b.ne capture_unmatched
capture_effect:
    ldr w9, [x8, #48]
    cmp w9, #63
    b.hi capture_unmatched
    mov x17, #1
    lsl x17, x17, x9
    ldr w11, [x8, #32]
    ldr w12, [x8, #40]
    adr x13, capture_profiles
capture_next:
    ldrb w14, [x13]
    cmp w14, #255
    b.eq capture_unmatched
    cmp w11, w14
    b.ne capture_advance
    ldrb w14, [x13, #1]
    cmp w12, w14
    b.ne capture_advance
    ldrb w14, [x13, #3]
    tst w14, w15
    b.eq capture_advance
    ldrb w14, [x13, #2]
    adr x10, capture_scenes
    ldr x14, [x10, x14, lsl #3]
    tst x14, x17
    b.eq capture_advance
    mov w9, w16
    capture_found
capture_advance:
    add x13, x13, #4
    b capture_next
capture_unmatched:
    capture_legacy
.p2align 3
capture_scenes:
    .include "capture_scenes.inc"
capture_profiles:
    .include "capture_profiles.inc"
