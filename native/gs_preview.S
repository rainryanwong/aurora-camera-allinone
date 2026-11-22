.text
.p2align 2
.global natural_preview

// The GS caller has already checked Natural, the camera owner, and the query layout.
natural_preview:
    ldr w9, [sp, #0x324]
#ifdef NIGHT_PREVIEW
    cmp w9, #173
    b.eq .Lnight
#endif
    cmp w9, #163
    b.ne previous_route
    ldr w9, [x8, #0x18]
    cbnz w9, previous_route
    ldr w9, [x8, #0x28]
    cbnz w9, previous_route
#ifdef CROP_SCENE32_PREVIEW
    ldr w9, [x8, #0x20]
    cmp w9, #22
    b.ne .Lphoto_hdr_preview
    ldr w9, [x8, #0x30]
#ifdef CROP_PREVIEW_SCENES
    cmp w9, #63
    b.hi .Lphoto_hdr_preview
    adr x13, .Lcrop_scene32_sensors
    ldr x14, [x13, #8]
    lsr x14, x14, x9
    tbz x14, #0, .Lphoto_hdr_preview
#else
    cmp w9, #32
    b.ne .Lphoto_hdr_preview
    ldr w9, [x8, #0x38]
    cbnz w9, .Lphoto_hdr_preview
#endif
    ldr w9, [x8, #0x10]
    cmp w9, #63
    b.hi .Lphoto_hdr_preview
    adr x13, .Lcrop_scene32_sensors
    ldr x14, [x13]
    lsr x14, x14, x9
    tbz x14, #0, .Lphoto_hdr_preview
#ifdef CROP_PREVIEW_SCENES
    ldr w9, [x8, #0x38]
    cbz w9, .Lcrop_original
    cmp w9, #28
    b.ne .Lphoto_hdr_preview
    mov w9, #CROP_EFFECT_28
    b store_effect
.Lcrop_original:
#endif
    mov w9, #CROP_SCENE32_EFFECT
    b store_effect
.Lphoto_hdr_preview:
#endif
    ldr w10, [x8, #0x38]
    sub w10, w10, #36
    cmp w10, #1
#ifdef SKY_PREVIEW
    b.hi .Lsky
#else
    b.hi previous_route
#endif
    adr x13, .Lroutes
.Lnext:
    ldrb w14, [x13]
    cmp w14, #255
#ifdef PHOTO_SCENES
    b.eq .Lphoto_scene
#else
    b.eq previous_route
#endif
    ldr w9, [x8, #0x10]
    cmp w9, w14
    b.ne .Ladvance
    ldr w9, [x8, #0x20]
    ldrb w14, [x13, #1]
    cmp w9, w14
    b.ne .Ladvance
    ldr w9, [x8, #0x30]
    ldrb w14, [x13, #2]
    cmp w9, w14
    b.ne .Ladvance
    add w9, w10, #PREVIEW_EFFECT
    b store_effect
.Ladvance:
    add x13, x13, #3
    b .Lnext

#ifdef SKY_PREVIEW
.Lsky:
    ldr w9, [x8, #0x20]
    cmp w9, #36
#ifdef PHOTO_SCENES
    b.ne .Lphoto_scene
#else
    b.ne previous_route
#endif
    ldr w10, [x8, #0x38]
    adr x13, .Lsky_routes
.Lsky_next:
    ldrb w14, [x13]
    cmp w14, #255
#ifdef PHOTO_SCENES
    b.eq .Lphoto_scene
#else
    b.eq previous_route
#endif
    ldr w9, [x8, #0x10]
    cmp w9, w14
    b.ne .Lsky_advance
    ldrb w14, [x13, #1]
    ldr w9, [x8, #0x30]
    cmp w9, w14
    b.ne .Lsky_advance
    ldrb w14, [x13, #2]
    cmp w10, w14
    b.ne .Lsky_advance
    ldrb w9, [x13, #3]
    b store_effect
.Lsky_advance:
    add x13, x13, #4
    b .Lsky_next
#endif

#ifdef PHOTO_SCENES
.Lphoto_scene:
    ldr w10, [x8, #0x38]
    cbz w10, .Lphoto_find
    cmp w10, #28
    b.ne .Lphoto_hdr
    mov w10, #1
    b .Lphoto_find
.Lphoto_hdr:
    sub w10, w10, #36
    cmp w10, #1
    b.hi previous_route
    add w10, w10, #2
.Lphoto_find:
    adr x13, .Lphoto_routes
.Lphoto_next:
    ldrb w14, [x13]
    cmp w14, #255
    b.eq previous_route
    ldr w9, [x8, #0x10]
    cmp w9, w14
    b.ne .Lphoto_advance
    ldr w9, [x8, #0x20]
    ldrb w14, [x13, #1]
    cmp w9, w14
    b.ne .Lphoto_advance
    ldr w9, [x8, #0x30]
    ldrb w14, [x13, #2]
    cmp w9, w14
    b.ne .Lphoto_advance
    add w9, w10, #PHOTO_EFFECT
    b store_effect
.Lphoto_advance:
    add x13, x13, #3
    b .Lphoto_next
#endif

#ifdef NIGHT_PREVIEW
// Night viewfinder requests select the same Effect in each downstream IPE
// profile. Keep the original Scene so its tuning and fallback still apply.
.Lnight:
    ldr w9, [x8, #0x18]
    cbnz w9, previous_route
    ldr w9, [x8, #0x20]
    cmp w9, #37
    b.ne previous_route
    ldr w9, [x8, #0x28]
    cbnz w9, previous_route

    adr x13, .Lnight_routes
    ldr w9, [x8, #0x30]
    cmp w9, #63
    b.hi previous_route
    ldr x14, [x13]
    lsr x14, x14, x9
    tbz x14, #0, previous_route
    ldr w9, [x8, #0x10]
    cmp w9, #63
    b.hi previous_route
    ldr x14, [x13, #8]
    lsr x14, x14, x9
    tbz x14, #0, previous_route

    ldr w9, [x8, #0x38]
    cbz w9, .Lnight_source_zero
    cmp w9, #28
    b.ne previous_route
    mov w9, #NIGHT_EFFECT_28
    b store_effect
.Lnight_source_zero:
    mov w9, #NIGHT_EFFECT_0
    b store_effect

.p2align 3
.Lnight_routes:
    .include "night_preview_routes.inc"
#endif

.Lroutes:
    .include "preview_routes.inc"

#ifdef SKY_PREVIEW
.Lsky_routes:
    .include "sky_preview_routes.inc"
#endif

#ifdef PHOTO_SCENES
.Lphoto_routes:
    .include "photo_scene_routes.inc"
#endif

#ifdef CROP_SCENE32_PREVIEW
.p2align 3
.Lcrop_scene32_sensors:
    .quad CROP_SCENE32_SENSORS
#ifdef CROP_PREVIEW_SCENES
    .quad CROP_PREVIEW_SCENES
#endif
#endif
