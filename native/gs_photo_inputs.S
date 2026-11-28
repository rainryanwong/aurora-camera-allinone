// Entered only from the existing ordinary Photo Natural preview dispatcher.
// Each input has its own leaf so lens- and scene-specific overrides survive.
.section .photo_special,"ax"
.global photo_inputs
photo_inputs:
    cmp w11, #36
    b.ne previous_photo
    mov w10, #PHOTO_24
    cmp w12, #24
    b.eq known_route
    mov w10, #PHOTO_61
    cmp w12, #61
    b.eq known_route
    mov w10, #PHOTO_65
    cmp w12, #65
    b.eq known_route
    b previous_photo
