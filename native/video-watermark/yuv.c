#include <android/bitmap.h>
#include <android/hardware_buffer_jni.h>
#include <arm_neon.h>
#include <errno.h>
#include <jni.h>
#include <stdint.h>
#include <string.h>
#include <math.h>

// One entry per 2x2 video block touched by the native template.
typedef struct {
    uint16_t x, y;
    uint8_t alpha[4], luma[4], chroma_alpha, u, v;
} StampBlock;

static uint8_t rounded(double value) {
    return (uint8_t)fmin(255.0, fmax(0.0, floor(value + 0.5)));
}

JNIEXPORT jobject JNICALL
Java_com_aurora_camera_videowm_AuroraVideoWatermarkBridge_makeStamp(
        JNIEnv *env, jclass clazz, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) || info.format != ANDROID_BITMAP_FORMAT_RGBA_8888
            || info.width != 1920 || info.height != 1080
            || AndroidBitmap_lockPixels(env, bitmap, &pixels)) return NULL;
    jobject buffer = NULL;
    StampBlock *entries = NULL;
    for (int pass = 0; pass < 2; ++pass) {
        size_t count = 0;
        int has_clear_pixel = 0;
        for (uint32_t y = 0; y < info.height; y += 2) {
            const uint8_t *row = (const uint8_t *)pixels + y * info.stride;
            for (uint32_t x = 0; x < info.width; x += 2) {
                const uint8_t *p = row + x * 4;
                if (pass == 0 && !has_clear_pixel)
                    has_clear_pixel = !p[3] || !p[7] || !p[info.stride+3] || !p[info.stride+7];
                if (!(p[3] | p[7] | p[info.stride+3] | p[info.stride+7])) continue;
                if (pass == 0) { ++count; continue; }
                StampBlock *block = &entries[count++];
                block->x = x;
                block->y = y;
                unsigned sum_alpha = 0;
                double sum_u = 0, sum_v = 0;
                for (unsigned k = 0; k < 4; ++k) {
                    const uint8_t *rgba = p + (k >> 1) * info.stride + (k & 1) * 4;
                    unsigned a = block->alpha[k] = rgba[3];
                    if (!a) continue;
                    double r = rgba[0] * 255.0 / a, g = rgba[1] * 255.0 / a, b = rgba[2] * 255.0 / a;
                    double luma = 0.2126*r + 0.7152*g + 0.0722*b;
                    block->luma[k] = rounded(16 + luma * (219.0/255.0));
                    sum_u += a * (b - luma);
                    sum_v += a * (r - luma);
                    sum_alpha += a;
                }
                block->chroma_alpha = (sum_alpha + 2) / 4;
                block->u = rounded(128 + (sum_u / sum_alpha) * (112.0 / (255.0 * 0.9278)));
                block->v = rounded(128 + (sum_v / sum_alpha) * (112.0 / (255.0 * 0.7874)));
            }
        }
        if (pass == 0) {
            if (!count || !has_clear_pixel) break;
            jclass cls = (*env)->FindClass(env, "java/nio/ByteBuffer");
            jmethodID alloc = (*env)->GetStaticMethodID(env, cls, "allocateDirect", "(I)Ljava/nio/ByteBuffer;");
            buffer = (*env)->CallStaticObjectMethod(env, cls, alloc, (jint)(count * sizeof(StampBlock)));
            if (!buffer) break;
            entries = (*env)->GetDirectBufferAddress(env, buffer);
            if (!entries) { buffer = NULL; break; }
        }
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    return buffer;
}

static inline void blend(uint8_t *pixel, unsigned color, unsigned alpha) {
    *pixel = (uint8_t)((*pixel * (255 - alpha) + color * alpha + 127) / 255);
}

// The 14 Ultra AVC encoder interprets the flexible camera buffer's U and V
// in reverse order. Keep the original buffer and reorder its chroma in place.
static void swap_chroma(const AHardwareBuffer_Plane *u, const AHardwareBuffer_Plane *v) {
    uintptr_t a = (uintptr_t)u->data, b = (uintptr_t)v->data;
    if (u->pixelStride == 2 && v->pixelStride == 2 && u->rowStride == v->rowStride
            && (a == b + 1 || b == a + 1)) {
        uint8_t *base = a < b ? u->data : v->data;
        for (unsigned y = 0; y < 540; ++y) {
            uint8_t *row = base + y * u->rowStride;
            for (unsigned x = 0; x < 1920; x += 16)
                vst1q_u8(row+x, vrev16q_u8(vld1q_u8(row+x)));
        }
    } else {
        for (unsigned y = 0; y < 540; ++y) {
            uint8_t *ur = (uint8_t *)u->data + y * u->rowStride;
            uint8_t *vr = (uint8_t *)v->data + y * v->rowStride;
            for (unsigned x = 0; x < 960; ++x) {
                uint8_t value = ur[x * u->pixelStride];
                ur[x * u->pixelStride] = vr[x * v->pixelStride];
                vr[x * v->pixelStride] = value;
            }
        }
    }
}

JNIEXPORT jint JNICALL
Java_com_aurora_camera_videowm_AuroraVideoWatermarkBridge_stampBuffer(
        JNIEnv *env, jclass clazz, jobject buffer, jobject stamp) {
    if (!buffer) return -EINVAL;
    AHardwareBuffer *hardware = AHardwareBuffer_fromHardwareBuffer(env, buffer);
    AHardwareBuffer_Desc desc;
    AHardwareBuffer_describe(hardware, &desc);
    if (desc.width != 1920 || desc.height != 1080) return -EINVAL;
    AHardwareBuffer_Planes image;
    int status = AHardwareBuffer_lockPlanes(hardware, AHARDWAREBUFFER_USAGE_CPU_READ_OFTEN |
            AHARDWAREBUFFER_USAGE_CPU_WRITE_OFTEN, -1, NULL, &image);
    if (status) return status;
    if (image.planeCount != 3) {
        AHardwareBuffer_unlock(hardware, NULL);
        return -ENOTSUP;
    }
    const StampBlock *blocks = stamp ? (*env)->GetDirectBufferAddress(env, stamp) : NULL;
    size_t count = stamp ? (size_t)(*env)->GetDirectBufferCapacity(env, stamp) / sizeof(StampBlock) : 0;
    for (size_t i = 0; i < count; ++i) {
        const StampBlock *b = &blocks[i];
        const AHardwareBuffer_Plane *y = &image.planes[0];
        uint8_t *p = (uint8_t *)y->data + b->y * y->rowStride + b->x * y->pixelStride;
        for (unsigned k = 0; k < 4; ++k) {
            if (b->alpha[k]) blend(p + (k >> 1)*y->rowStride + (k & 1)*y->pixelStride, b->luma[k], b->alpha[k]);
        }
        for (unsigned channel = 1; channel < 3; ++channel) {
            const AHardwareBuffer_Plane *chroma = &image.planes[channel];
            uint8_t *sample = (uint8_t *)chroma->data + (b->y/2)*chroma->rowStride + (b->x/2)*chroma->pixelStride;
            // Blend in camera order. The final swap puts both the scene and the stamp in encoder order.
            blend(sample, channel == 1 ? b->v : b->u, b->chroma_alpha);
        }
    }
    swap_chroma(&image.planes[1], &image.planes[2]);
    return AHardwareBuffer_unlock(hardware, NULL);
}
