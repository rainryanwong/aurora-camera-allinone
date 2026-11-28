// These entries run inside the existing Natural dispatch with x8 pointing to
// the seven tuning selectors. The original scene and readout stay in the query.
.section .mode_entry,"ax"
.global mode_preview
mode_preview:
    ldr w9, [x8, #0x18]
    cbnz w9, previous_route
    ldr w9, [x8, #0x28]
    cbnz w9, previous_route
    ldr w13, [sp, #0x324]
    ldr w11, [x8, #0x20]
    ldr w12, [x8, #0x38]
    cmp w13, #163
    b.ne .Lother
    cmp w11, #22
    b.ne photo_special
    mov w10, #CROP
    sub w9, w12, #36
    cmp w9, #1
    b.hi common_sources
    add w10, w9, #CROP_HDR
    b known_route
.Lother:
    cmp w13, #173
    b.ne .Lday
    cmp w11, #37
    b.ne previous_route
    mov w10, #NIGHT
    b common_sources
.Lday:
    cbnz w11, previous_route
    cmp w13, #225
    b.eq street
    sub w9, w13, #167
    cmp w9, #1
    b.hi previous_route
    mov w10, #DAY
    b common_sources

.section .mode_sources,"ax"
street:
    mov w10, #STREET
    cmp w12, #24
    b.ne common_sources
    ldr w9, [x8, #0x10]
    cmp w9, #6
    b.ne previous_route
    ldr w9, [x8, #0x30]
    cmp w9, #32
    b.ne previous_route
    mov w10, #STREET_SPECIAL
    b select_effect
common_sources:
    cbz w12, known_route
    cmp w12, #28
    b.ne previous_mode
    add w10, w10, #1
known_route:
    ldr w9, [x8, #0x10]
    cmp w9, #LAST_READOUT
    b.hi previous_mode
    ldr w9, [x8, #0x30]
    cmp w9, #63
    b.hi previous_mode
    ldr x14, .Lscenes
    lsr x14, x14, x9
    tbz x14, #0, previous_mode
select_effect:
    mov w9, w10
    b store_effect
previous_mode:
    cmp w13, #163
    b.eq previous_photo
    b previous_route
.Lscenes:
    .quad KNOWN_SCENES

// The stock Photo effect 24 was promoted only on these four scenes and
// readouts. This input was recorded in the ordinary 9:16 Photo preview.
.section .photo_special,"ax"
photo_special:
    cmp w11, #36
    b.ne previous_photo
    cmp w12, #24
    b.ne previous_photo
    ldr w9, [x8, #0x10]
    cmp w9, #19
    b.ls .Lscene
    cmp w9, #35
    b.ne previous_photo
.Lscene:
    ldr w9, [x8, #0x30]
    cmp w9, #63
    b.hi previous_photo
    ldr x14, .Lspecial
    lsr x14, x14, x9
    tbz x14, #0, previous_photo
    mov w9, #PHOTO_SPECIAL
    b store_effect
.Lspecial:
    .quad 0x1000000000405
