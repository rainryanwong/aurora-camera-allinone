.text
.p2align 2
.global natural_night_bridge
.type natural_night_bridge,%function
// Adapt only ARC_SND_GetEVList's unsupported Natural input mode.
// x19 is AlgoSceneDetectionChecker; x24 is the current ASDDatabase.
natural_night_bridge:
    ldr w9, [sp, #0x38]
    cmp w9, #2
    b.ne original
    add x10, x19, #0x1000
    ldrb w9, [x10, #0xbac]
    cmp w9, #2
    b.ne original
    ldr w9, [x19, #0x1a98]
    cmp w9, #3
    b.hi original
    mov w10, #0x8a10
    ldrb w9, [x24, x10]
    cmp w9, #1
    b.ne original
    mov w10, #0x8a40
    ldr w9, [x24, x10]
    cmp w9, #173
    b.eq dedicated_night
    cmp w9, #163
    b.ne original
    ldr w9, [x19, #0x1a88]
    mov w10, #0x8001
    cmp w9, w10
    b.eq eligible
    mov w10, #0x9002
    cmp w9, w10
    b.ne original
    b eligible
dedicated_night:
    ldr w9, [x19, #0x1a88]
    mov w10, #0x800a
    cmp w9, w10
    b.ne original
eligible:
    // Preserve all argument registers and the original stack argument layout.
    sub sp, sp, #224
    stp x0, x1, [sp]
    stp x2, x3, [sp, #16]
    stp x4, x5, [sp, #32]
    stp x6, x7, [sp, #48]
    stp x8, x30, [sp, #64]
    stp q0, q1, [sp, #80]
    stp q2, q3, [sp, #112]
    stp q4, q5, [sp, #144]
    stp q6, q7, [sp, #176]
    adr x0, natural_night_property
    mov w1, wzr
    bl property_get_bool
    cbz w0, restore
    str wzr, [sp, #280] // original SP + 0x38, current Vivid-compatible mode 0
restore:
    ldp q0, q1, [sp, #80]
    ldp q2, q3, [sp, #112]
    ldp q4, q5, [sp, #144]
    ldp q6, q7, [sp, #176]
    ldp x0, x1, [sp]
    ldp x2, x3, [sp, #16]
    ldp x4, x5, [sp, #32]
    ldp x6, x7, [sp, #48]
    ldp x8, x30, [sp, #64]
    add sp, sp, #224
original:
    b original_arcsoft
natural_night_property:
    .asciz "vendor.debug.camera.natural_arc_sn"
.p2align 2
