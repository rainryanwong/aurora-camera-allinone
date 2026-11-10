.class public final Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;
.super Ljava/lang/Object;
.field private static final HEX_COLOR:Ljava/util/regex/Pattern;
.field private static final ALPHA_LUT:[I

.method static constructor <clinit>()V
    .locals 1
    const-string v0, "xiaomi_([0-9A-Fa-f]{6})\\.webp"

    invoke-static {v0}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;

    move-result-object v0

    sput-object v0, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->HEX_COLOR:Ljava/util/regex/Pattern;
    const/16 v0, 0x100

    .line 80
    new-array v0, v0, [I

    fill-array-data v0, :array_0

    sput-object v0, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->ALPHA_LUT:[I

    return-void

    nop

    :array_0
    .array-data 4
        0x0
        0x1
        0x1
        0x2
        0x2
        0x3
        0x3
        0x4
        0x4
        0x5
        0x5
        0x6
        0x6
        0x7
        0x7
        0x8
        0x9
        0x9
        0xa
        0xa
        0xb
        0xb
        0xc
        0xc
        0xd
        0xd
        0xe
        0xe
        0xf
        0xf
        0x10
        0x11
        0x12
        0x13
        0x14
        0x15
        0x16
        0x17
        0x18
        0x18
        0x19
        0x1a
        0x1b
        0x1c
        0x1d
        0x1e
        0x1f
        0x20
        0x21
        0x22
        0x23
        0x24
        0x25
        0x26
        0x27
        0x28
        0x28
        0x29
        0x2a
        0x2b
        0x2c
        0x2d
        0x2e
        0x2f
        0x30
        0x31
        0x32
        0x33
        0x35
        0x36
        0x37
        0x38
        0x39
        0x3a
        0x3b
        0x3d
        0x3e
        0x3f
        0x40
        0x41
        0x42
        0x43
        0x43
        0x44
        0x45
        0x46
        0x47
        0x48
        0x48
        0x49
        0x4a
        0x4b
        0x4c
        0x4d
        0x4d
        0x4e
        0x4f
        0x50
        0x51
        0x52
        0x52
        0x53
        0x54
        0x55
        0x56
        0x56
        0x57
        0x58
        0x59
        0x5a
        0x5a
        0x5b
        0x5c
        0x5d
        0x5e
        0x5e
        0x5f
        0x60
        0x61
        0x62
        0x63
        0x64
        0x64
        0x65
        0x66
        0x67
        0x68
        0x69
        0x6a
        0x6b
        0x6c
        0x6c
        0x6d
        0x6e
        0x6f
        0x70
        0x71
        0x72
        0x73
        0x75
        0x76
        0x77
        0x78
        0x79
        0x7a
        0x7b
        0x7d
        0x7e
        0x7f
        0x80
        0x84
        0x88
        0x8c
        0x90
        0x92
        0x94
        0x96
        0x98
        0x9a
        0x9c
        0x9e
        0xa0
        0xa2
        0xa4
        0xa5
        0xa7
        0xa9
        0xab
        0xac
        0xae
        0xb0
        0xb1
        0xb2
        0xb3
        0xb5
        0xb6
        0xb7
        0xb8
        0xb9
        0xba
        0xbb
        0xbd
        0xbe
        0xbf
        0xc0
        0xc1
        0xc2
        0xc2
        0xc3
        0xc4
        0xc5
        0xc6
        0xc6
        0xc7
        0xc8
        0xc9
        0xca
        0xca
        0xcb
        0xcc
        0xcd
        0xce
        0xce
        0xcf
        0xd0
        0xd1
        0xd3
        0xd4
        0xd6
        0xd7
        0xd9
        0xda
        0xdc
        0xdd
        0xdf
        0xe0
        0xe1
        0xe2
        0xe3
        0xe4
        0xe5
        0xe6
        0xe8
        0xe9
        0xea
        0xeb
        0xec
        0xed
        0xee
        0xef
        0xef
        0xf0
        0xf1
        0xf1
        0xf2
        0xf2
        0xf3
        0xf4
        0xf4
        0xf5
        0xf6
        0xf6
        0xf7
        0xf7
        0xf8
        0xf9
        0xf9
        0xfa
        0xfb
        0xfb
        0xfc
        0xfc
        0xfd
        0xfe
        0xfe
        0xff
    .end array-data
.end method
.method private static applyAlphaLut(Landroid/graphics/Bitmap;)V
    .locals 12

    .line 655
    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v8

    .line 656
    invoke-virtual {p0}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v9

    mul-int v10, v8, v9

    .line 657
    new-array v11, v10, [I

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, v11

    move v3, v8

    move v6, v8

    move v7, v9

    .line 658
    invoke-virtual/range {v0 .. v7}, Landroid/graphics/Bitmap;->getPixels([IIIIIII)V

    const/4 v0, 0x0

    :goto_0
    if-ge v0, v10, :cond_0

    .line 660
    aget v1, v11, v0

    .line 661
    sget-object v2, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->ALPHA_LUT:[I

    invoke-static {v1}, Landroid/graphics/Color;->alpha(I)I

    move-result v3

    aget v2, v2, v3

    .line 662
    invoke-static {v1}, Landroid/graphics/Color;->red(I)I

    move-result v3

    .line 663
    invoke-static {v1}, Landroid/graphics/Color;->green(I)I

    move-result v4

    invoke-static {v1}, Landroid/graphics/Color;->blue(I)I

    move-result v1

    .line 662
    invoke-static {v2, v3, v4, v1}, Landroid/graphics/Color;->argb(IIII)I

    move-result v1

    aput v1, v11, v0

    add-int/lit8 v0, v0, 0x1

    goto :goto_0

    :cond_0
    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v2, 0x0

    move-object v0, p0

    move-object v1, v11

    move v3, v8

    move v6, v8

    move v7, v9

    .line 665
    invoke-virtual/range {v0 .. v7}, Landroid/graphics/Bitmap;->setPixels([IIIIIII)V

    return-void
.end method
.method private static colorFor(Ljava/lang/String;)I
    .locals 2

    .line 669
    const-string v0, "black"

    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_0

    const/high16 p0, -0x1000000

    return p0

    .line 670
    :cond_0
    const-string v0, "white"

    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v0

    const/4 v1, -0x1

    if-eqz v0, :cond_1

    return v1

    .line 671
    :cond_1
    const-string v0, "yellow"

    invoke-virtual {p0, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v0

    if-eqz v0, :cond_2

    const p0, -0xb2dffe

    return p0

    .line 672
    :cond_2
    sget-object v0, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->HEX_COLOR:Ljava/util/regex/Pattern;

    invoke-virtual {v0, p0}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;

    move-result-object p0

    .line 673
    invoke-virtual {p0}, Ljava/util/regex/Matcher;->matches()Z

    move-result v0

    if-eqz v0, :cond_3

    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "#"

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const/4 v1, 0x1

    invoke-virtual {p0, v1}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    invoke-static {p0}, Landroid/graphics/Color;->parseColor(Ljava/lang/String;)I

    move-result p0

    return p0

    :cond_3
    return v1
.end method
.method public static compose(Landroid/content/Context;Landroid/graphics/Bitmap;Ljava/lang/String;Ljava/lang/String;)Landroid/graphics/Bitmap;
    .locals 16

    move-object/from16 v0, p1

    move-object/from16 v1, p2

    move-object/from16 v2, p3

    if-eqz v1, :cond_6

    .line 562
    invoke-virtual/range {p2 .. p2}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/String;->isEmpty()Z

    move-result v3

    if-nez v3, :cond_6

    .line 565
    invoke-virtual/range {p0 .. p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;

    move-result-object v3

    .line 566
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v4

    int-to-float v4, v4

    const/high16 v5, 0x43200000    # 160.0f

    div-float/2addr v4, v5

    .line 565
    invoke-static {v3, v2, v4}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->createTextPaint(Landroid/content/res/AssetManager;Ljava/lang/String;F)Landroid/graphics/Paint;

    move-result-object v3

    .line 567
    invoke-virtual {v3, v1}, Landroid/graphics/Paint;->measureText(Ljava/lang/String;)F

    move-result v4

    const-string v5, "17 Ultra"

    .line 568
    invoke-virtual {v3, v5}, Landroid/graphics/Paint;->measureText(Ljava/lang/String;)F

    move-result v5

    sub-float/2addr v4, v5

    const v5, 0x3f7fdd98

    mul-float/2addr v4, v5

    .line 569
    invoke-static {v4}, Ljava/lang/Math;->round(F)I

    move-result v5

    .line 570
    invoke-static {v2}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->isWhiteShadow(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_0

    const v6, 0x3f7968bc

    mul-float/2addr v4, v6

    .line 574
    invoke-static {v4}, Ljava/lang/Math;->round(F)I

    move-result v4

    goto :goto_0

    :cond_0
    move v4, v5

    .line 575
    :goto_0
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v6

    add-int/2addr v6, v4

    if-lez v6, :cond_5

    .line 581
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v7

    sget-object v8, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    .line 580
    invoke-static {v6, v7, v8}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v7

    .line 582
    new-instance v8, Landroid/graphics/Canvas;

    invoke-direct {v8, v7}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    .line 583
    new-instance v9, Landroid/graphics/Paint;

    const/4 v10, 0x3

    invoke-direct {v9, v10}, Landroid/graphics/Paint;-><init>(I)V

    if-eqz v2, :cond_1

    const/16 v12, 0x27a

    goto :goto_1

    :cond_1
    const/16 v12, 0x288

    :goto_1
    if-eqz v2, :cond_2

    const/16 v14, 0x4b6

    goto :goto_2

    :cond_2
    const/16 v14, 0x4d7

    :goto_2
    add-int/2addr v4, v14

    .line 587
    new-instance v15, Landroid/graphics/Rect;

    .line 588
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v10

    const/4 v11, 0x0

    invoke-direct {v15, v11, v11, v12, v10}, Landroid/graphics/Rect;-><init>(IIII)V

    new-instance v10, Landroid/graphics/Rect;

    .line 589
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v13

    invoke-direct {v10, v11, v11, v12, v13}, Landroid/graphics/Rect;-><init>(IIII)V

    .line 587
    invoke-virtual {v8, v0, v15, v10, v9}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V

    .line 592
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v10

    add-int/lit16 v12, v5, 0x6c4

    .line 591
    const v13, -0x41b33333    # -0.2f

    if-eqz v2, :cond_3

    const v13, -0x41666666    # -0.3f

    :cond_3
    invoke-static {v3, v1, v10, v12, v13}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->renderLayer(Landroid/graphics/Paint;Ljava/lang/String;IIF)Landroid/graphics/Bitmap;

    move-result-object v1

    if-eqz v2, :cond_4

    move-object/from16 v2, p3

    invoke-static {v8, v1, v9, v2}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->drawWhiteShadow(Landroid/graphics/Canvas;Landroid/graphics/Bitmap;Landroid/graphics/Paint;Ljava/lang/String;)V

    goto :goto_3

    :cond_4
    move-object/from16 v2, p3

    invoke-static {v9, v2}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->setModelColor(Landroid/graphics/Paint;Ljava/lang/String;)V

    const/4 v2, 0x0

    .line 600
    invoke-virtual {v8, v1, v2, v2, v9}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V

    .line 602
    :goto_3
    const/4 v2, 0x0

    invoke-virtual {v9, v2}, Landroid/graphics/Paint;->setColorFilter(Landroid/graphics/ColorFilter;)Landroid/graphics/ColorFilter;

    invoke-virtual {v1}, Landroid/graphics/Bitmap;->recycle()V

    .line 604
    new-instance v1, Landroid/graphics/Rect;

    .line 605
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getWidth()I

    move-result v2

    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v3

    invoke-direct {v1, v14, v11, v2, v3}, Landroid/graphics/Rect;-><init>(IIII)V

    new-instance v2, Landroid/graphics/Rect;

    .line 606
    invoke-virtual/range {p1 .. p1}, Landroid/graphics/Bitmap;->getHeight()I

    move-result v3

    invoke-direct {v2, v4, v11, v6, v3}, Landroid/graphics/Rect;-><init>(IIII)V

    .line 604
    invoke-virtual {v8, v0, v1, v2, v9}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;Landroid/graphics/Paint;)V

    return-object v7

    .line 577
    :cond_5
    new-instance v0, Ljava/lang/IllegalArgumentException;

    const-string v1, "Rendered watermark width is invalid"

    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 563
    :cond_6
    new-instance v0, Ljava/lang/IllegalArgumentException;

    const-string v1, "Model name is empty"

    invoke-direct {v0, v1}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw v0
.end method
.method private static createTextPaint(Landroid/content/res/AssetManager;Ljava/lang/String;F)Landroid/graphics/Paint;
    .locals 3

    .line 613
    invoke-static {}, Lcom/aurora/camera/features/watermark/WatermarkFonts217;->builder()Landroid/graphics/Typeface$Builder;
    move-result-object v0

    sget-object p0, Ljava/util/Locale;->US:Ljava/util/Locale;

    const v1, 0x43dfc000    # 447.5f

    .line 615
    invoke-static {v1}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v1

    filled-new-array {v1}, [Ljava/lang/Object;

    move-result-object v1

    .line 614
    const-string v2, "\'wght\' %.1f"

    invoke-static {p0, v2, v1}, Ljava/lang/String;->format(Ljava/util/Locale;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v0, p0}, Landroid/graphics/Typeface$Builder;->setFontVariationSettings(Ljava/lang/String;)Landroid/graphics/Typeface$Builder;

    move-result-object p0

    .line 616
    invoke-virtual {p0}, Landroid/graphics/Typeface$Builder;->build()Landroid/graphics/Typeface;

    move-result-object p0

    .line 617
    new-instance v0, Landroid/graphics/Paint;

    const/16 v1, 0x81

    invoke-direct {v0, v1}, Landroid/graphics/Paint;-><init>(I)V

    .line 618
    invoke-virtual {v0, p0}, Landroid/graphics/Paint;->setTypeface(Landroid/graphics/Typeface;)Landroid/graphics/Typeface;

    const p0, 0x430bc000    # 139.75f

    mul-float/2addr p0, p2

    .line 619
    invoke-virtual {v0, p0}, Landroid/graphics/Paint;->setTextSize(F)V

    const/high16 p0, 0x3f800000    # 1.0f

    .line 620
    invoke-virtual {v0, p0}, Landroid/graphics/Paint;->setTextScaleX(F)V

    .line 621
    const/4 p0, -0x1

    invoke-virtual {v0, p0}, Landroid/graphics/Paint;->setColor(I)V

    return-object v0
.end method
.method private static drawWhiteShadow(Landroid/graphics/Canvas;Landroid/graphics/Bitmap;Landroid/graphics/Paint;Ljava/lang/String;)V
    .locals 3

    invoke-virtual {p0}, Landroid/graphics/Canvas;->getWidth()I

    move-result v0

    invoke-virtual {p0}, Landroid/graphics/Canvas;->getHeight()I

    move-result v1

    invoke-static {p1, v0, v1}, Lcom/aurora/camera/features/watermark/LeicaShadowRenderer;->render(Landroid/graphics/Bitmap;II)Landroid/graphics/Bitmap;

    move-result-object p1

    const-string v1, "xiaomi_CCFFFFFF_shadow.webp"

    invoke-virtual {v1, p3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    const/16 v1, 0xcc

    invoke-virtual {p2, v1}, Landroid/graphics/Paint;->setAlpha(I)V

    :cond_0
    const/4 v0, 0x0

    invoke-virtual {p0, p1, v0, v0, p2}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;FFLandroid/graphics/Paint;)V

    const/16 v1, 0xff

    invoke-virtual {p2, v1}, Landroid/graphics/Paint;->setAlpha(I)V

    invoke-virtual {p1}, Landroid/graphics/Bitmap;->recycle()V

    return-void
.end method
.method private static isWhiteShadow(Ljava/lang/String;)Z
    .locals 1

    const-string v0, "xiaomi_white_shadow.webp"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_0

    const-string v0, "xiaomi_CCFFFFFF_shadow.webp"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    return v0

    :cond_0
    const/4 v0, 0x1

    return v0
.end method
.method private static renderLayer(Landroid/graphics/Paint;Ljava/lang/String;IIF)Landroid/graphics/Bitmap;
    .locals 6

    .line 632
    sget-object v0, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {p3, p2, v0}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object v0

    .line 633
    new-instance v1, Landroid/graphics/Canvas;

    invoke-direct {v1, v0}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    .line 634
    new-instance v2, Landroid/graphics/Rect;

    invoke-direct {v2}, Landroid/graphics/Rect;-><init>()V

    const/4 v3, 0x0

    .line 635
    invoke-virtual {p1}, Ljava/lang/String;->length()I

    move-result v4

    invoke-virtual {p0, p1, v3, v4, v2}, Landroid/graphics/Paint;->getTextBounds(Ljava/lang/String;IILandroid/graphics/Rect;)V

    .line 636
    invoke-virtual {v1}, Landroid/graphics/Canvas;->save()I

    move-result v3

    const v4, 0x3f7fdd98

    .line 637
    invoke-virtual {v1, v4, v4}, Landroid/graphics/Canvas;->scale(FF)V

    .line 638
    iget v4, v2, Landroid/graphics/Rect;->left:I

    int-to-float v4, v4

    const v5, 0x4430ccc3

    sub-float/2addr v5, v4

    iget v2, v2, Landroid/graphics/Rect;->top:I

    int-to-float v2, v2

    const v4, 0x41ec9fcd

    sub-float/2addr v4, v2

    invoke-virtual {v1, p1, v5, v4, p0}, Landroid/graphics/Canvas;->drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V

    .line 641
    invoke-virtual {v1, v3}, Landroid/graphics/Canvas;->restoreToCount(I)V

    .line 643
    sget-object p0, Landroid/graphics/Bitmap$Config;->ARGB_8888:Landroid/graphics/Bitmap$Config;

    invoke-static {p3, p2, p0}, Landroid/graphics/Bitmap;->createBitmap(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;

    move-result-object p0

    .line 644
    new-instance p1, Landroid/graphics/Canvas;

    invoke-direct {p1, p0}, Landroid/graphics/Canvas;-><init>(Landroid/graphics/Bitmap;)V

    .line 645
    new-instance p3, Landroid/graphics/Matrix;

    invoke-direct {p3}, Landroid/graphics/Matrix;-><init>()V

    move v1, p4

    int-to-float p2, p2

    mul-float/2addr p2, v1

    const/high16 v1, 0x43200000    # 160.0f

    div-float/2addr p2, v1

    const/4 v1, 0x0

    .line 646
    invoke-virtual {p3, v1, p2}, Landroid/graphics/Matrix;->setTranslate(FF)V

    .line 647
    new-instance p2, Landroid/graphics/Paint;

    const/4 v1, 0x3

    invoke-direct {p2, v1}, Landroid/graphics/Paint;-><init>(I)V

    invoke-virtual {p1, v0, p3, p2}, Landroid/graphics/Canvas;->drawBitmap(Landroid/graphics/Bitmap;Landroid/graphics/Matrix;Landroid/graphics/Paint;)V

    .line 649
    invoke-virtual {v0}, Landroid/graphics/Bitmap;->recycle()V

    .line 650
    invoke-static {p0}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->applyAlphaLut(Landroid/graphics/Bitmap;)V

    return-object p0
.end method
.method private static setModelColor(Landroid/graphics/Paint;Ljava/lang/String;)V
    .locals 2

    invoke-static {p1}, Lcom/aurora/camera/features/watermark/OnlineLeicaRenderer;->colorFor(Ljava/lang/String;)I

    move-result p1

    new-instance v0, Landroid/graphics/PorterDuffColorFilter;

    sget-object v1, Landroid/graphics/PorterDuff$Mode;->SRC_IN:Landroid/graphics/PorterDuff$Mode;

    invoke-direct {v0, p1, v1}, Landroid/graphics/PorterDuffColorFilter;-><init>(ILandroid/graphics/PorterDuff$Mode;)V

    invoke-virtual {p0, v0}, Landroid/graphics/Paint;->setColorFilter(Landroid/graphics/ColorFilter;)Landroid/graphics/ColorFilter;

    return-void
.end method