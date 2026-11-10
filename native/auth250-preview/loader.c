/* The Stock1770 MiFilterSDK getter uses the NDK 24-byte long-string layout.
 * Entry glue passes its hidden sret x8 as ordinary C argument x0. This code
 * calls only the existing library's PLT, including its operator new.
 */
#include <stddef.h>
#include <stdint.h>

typedef struct NativeFILE NativeFILE;
typedef struct {size_t capacity_tag,length;char *data;} AuthString;
_Static_assert(sizeof(AuthString)==24,"NDK long-string size");
_Static_assert(offsetof(AuthString,data)==16,"NDK long-string data offset");

extern NativeFILE *fopen(const char*,const char*);
extern int fseek(NativeFILE*,long,int);
extern long ftell(NativeFILE*);
extern size_t fread(void*,size_t,size_t,NativeFILE*);
extern int fclose(NativeFILE*);
extern void *memcpy(void*,const void*,size_t);
extern void *memchr(const void*,int,size_t);
extern int __android_log_print(int,const char*,const char*,...);
extern void abort(void) __attribute__((noreturn));
extern void *auth_cpp_new(size_t) __asm__("_Znwm");
extern const char auth_original_shader[] __attribute__((visibility("hidden")));

static const char formula_path[]="/data/user/0/com.android.camera/files/phoenix-vignette/colorDark.glsl";
static const char tag[]="AuroraAUTH250Preview";
static __attribute__((noreturn)) void fail(const char *reason){
    __android_log_print(6,tag,"%s failed: %s",reason,formula_path);
    abort();
}

__attribute__((visibility("hidden")))
void auth_get_cv_shader(AuthString *out){
    NativeFILE *stream=fopen(formula_path,"rb");
    if(!stream)fail("open");
    if(fseek(stream,0,2)!=0)fail("seek end");
    long length=ftell(stream);
    if(length<=0||length>16384)fail("length");
    if(fseek(stream,0,0)!=0)fail("rewind");
    const size_t prefix=1223,suffix=341,original_length=2015;
    /* Match the Java validation template's line boundary on both sides. This
       permits a leading #if and a trailing //comment without a final LF. */
    size_t total=prefix+1+(size_t)length+1+suffix;
    size_t capacity=(total+1+15)&~(size_t)15;
    /* Pair the current library's _Znwm allocation with its normal destructor.
       The allocator throws on failure, as it did in the unmodified getter. */
    char *text=auth_cpp_new(capacity);
    memcpy(text,auth_original_shader,prefix);
    text[prefix]='\n';char *body=text+prefix+1;
    if(fread(body,1,(size_t)length,stream)!=(size_t)length)fail("read");
    if(fclose(stream)!=0)fail("close");
    if(memchr(body,0,(size_t)length))fail("embedded NUL");
    body[length]='\n';memcpy(body+length+1,auth_original_shader+original_length-suffix,suffix);
    text[total]=0;
    out->capacity_tag=capacity|1;out->length=total;out->data=text;
    __android_log_print(4,tag,"loaded %s bytes=%ld body=%.120s",formula_path,length,body);
}
