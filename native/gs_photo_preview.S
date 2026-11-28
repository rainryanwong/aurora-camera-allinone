// The caller has checked Natural, Photo mode, preview use case and Feature2=0.
// Keep the scene and source variant available to the lens's own tuning tree.
.text
.global photo_preview
photo_preview:
    ldr w9, [x8, #0x20]
    cmp w9, #36
    b.ne previous_scene
    ldr w10, [x8, #0x38]
    cbz w10, .Lreadout
    cmp w10, #28
    b.ne previous_scene
.Lreadout:
    ldr w9, [x8, #0x10]
    sub w9, w9, #1
    cmp w9, #18
    b.hi previous_scene
    ldr w9, [x8, #0x30]
    cmp w9, #63
    b.hi previous_scene
    ldr x13, .Lscenes
    lsr x13, x13, x9
    tbz x13, #0, previous_scene
    mov w9, #PHOTO_EFFECT
    add w9, w9, w10, lsr #4  // Source 0 selects 97, source 28 selects 98.
    b store_effect
.p2align 2
.Lscenes:
    .quad PHOTO_SCENE_MASK
