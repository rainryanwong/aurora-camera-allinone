.text
.p2align 2
.global natural_preview

// The GS caller has already checked Natural, the camera owner, and the query layout.
natural_preview:
    ldr w9, [sp, #0x324]
    cmp w9, #163
    b.ne previous_route
    ldr w9, [x8, #0x18]
    cbnz w9, previous_route
    ldr w9, [x8, #0x28]
    cbnz w9, previous_route
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
