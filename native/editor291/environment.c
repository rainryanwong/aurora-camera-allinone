#define _GNU_SOURCE
#include <jni.h>
#include <link.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

typedef struct {
    unsigned libraries, count;
    unsigned char *flags[2];
} Environment;

static int writable(const struct dl_phdr_info *image, uintptr_t address) {
    for (ElfW(Half) i = 0; i < image->dlpi_phnum; i++) {
        const ElfW(Phdr) *p = &image->dlpi_phdr[i];
        uintptr_t begin = image->dlpi_addr + p->p_vaddr;
        if (p->p_type == PT_LOAD && (p->p_flags & PF_W) &&
                address >= begin && address - begin < p->p_memsz) return 1;
    }
    return 0;
}

static int locate(struct dl_phdr_info *image, size_t size, void *opaque) {
    (void) size;
    const char *name = strrchr(image->dlpi_name, '/');
    name = name ? name + 1 : image->dlpi_name;
    if (strcmp(name, "libeditor_config_native.so")) return 0;
    Environment *result = opaque;
    result->libraries++;
    for (ElfW(Half) i = 0; i < image->dlpi_phnum; i++) {
        const ElfW(Phdr) *p = &image->dlpi_phdr[i];
        if (p->p_type != PT_LOAD || !(p->p_flags & PF_X)) continue;
        const uint32_t *code = (const uint32_t *)(image->dlpi_addr + p->p_vaddr);
        size_t words = p->p_memsz / sizeof(*code);
        for (size_t at = 0; at + 4 < words; at++) {
            uint32_t page = code[at], load = code[at + 1], test = code[at + 2];
            uint32_t zero = code[at + 3], jump = code[at + 4];
            /* Both JNI gates have ADRP/LDRB, TBZ bit 0 over a zero result,
             * then a branch to the return epilogue. Filter and unwrap share
             * one flag; nativeQuery uses the original OnLoad flag. */
            if ((page & 0x9f000000u) != 0x90000000u ||
                    (load & 0xffc00000u) != 0x39400000u ||
                    ((load >> 5) & 31) != (page & 31) ||
                    (test & 0xfff80000u) != 0x36000000u ||
                    (test & 31) != (load & 31) || ((test >> 5) & 0x3fff) != 3 ||
                    (zero & 0x7fffffe0u) != 0x2a1f03e0u ||
                    (jump & 0xfc000000u) != 0x14000000u) continue;
            int64_t delta = ((page >> 5) & 0x7ffff) << 2 | ((page >> 29) & 3);
            if (delta & 0x100000) delta -= 0x200000;
            uintptr_t address = ((uintptr_t)&code[at] & ~(uintptr_t)4095) +
                    delta * 4096 + ((load >> 10) & 4095);
            if (!writable(image, address)) continue;
            unsigned j;
            for (j = 0; j < result->count && j < 2; j++)
                if (result->flags[j] == (unsigned char *)address) break;
            if (j < result->count) continue;
            if (result->count < 2) result->flags[result->count] = (unsigned char *)address;
            result->count++;
        }
    }
    return 0;
}

JNIEXPORT void JNICALL
Java_com_aurora_camera_compat_editor_EditorNativeCloud291_restoreEnvironment(JNIEnv *env, jclass type) {
    (void) type;
    Environment found = {0};
    dl_iterate_phdr(locate, &found);
    if (found.libraries != 1 || found.count != 2 ||
            *found.flags[0] > 1 || *found.flags[1] > 1) {
        char message[128];
        snprintf(message, sizeof(message), "Native watermark environment gates differ: libraries=%u flags=%u",
                found.libraries, found.count);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), message);
        return;
    }
    __atomic_store_n(found.flags[0], 0, __ATOMIC_RELEASE);
    __atomic_store_n(found.flags[1], 0, __ATOMIC_RELEASE);
}
