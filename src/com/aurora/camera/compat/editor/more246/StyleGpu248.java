package com.aurora.camera.compat.editor.more246;

import android.graphics.Bitmap;
import android.opengl.*;
import com.aurora.camera.compat.sharedlut.Recipe248;
import java.io.IOException;
import java.nio.*;

/** One private GLES program per recipe renderer; the original spatial shader is unchanged. */
final class StyleGpu248 {
    private static final String[] NAMES={"SharpenIntensity","ContrastIntensity","SaturationIntensity","Temperature","Tint","Grain","VignetteIntensity"};
    private final String fragment;
    private EGLDisplay display=EGL14.EGL_NO_DISPLAY;
    private EGLContext context=EGL14.EGL_NO_CONTEXT;
    private EGLSurface surface=EGL14.EGL_NO_SURFACE;
    private int program,maxTexture,width,height,position,tex,sourceLocation,algosLocation,resolutionLocation;
    private final int[] textures=new int[2],fbo=new int[1],uniforms=new int[NAMES.length];
    private FloatBuffer quad;
    private ByteBuffer pixels;
    StyleGpu248(String fragment){this.fragment=fragment;}

    synchronized Bitmap apply(Bitmap source,Recipe248 recipe)throws IOException {
        EGLDisplay previousDisplay=EGL14.eglGetCurrentDisplay();EGLContext previousContext=EGL14.eglGetCurrentContext();
        EGLSurface previousDraw=EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),previousRead=EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
        Bitmap result=null;Throwable primary=null;
        try {
            if(EGL14.EGL_NO_CONTEXT.equals(context))initialize();
            else if(!EGL14.eglMakeCurrent(display,surface,surface,context))throw new IOException("Cannot bind recipe GLES context");
            int w=source.getWidth(),h=source.getHeight();
            if(w>maxTexture||h>maxTexture)throw new IOException("Recipe bitmap exceeds device GLES texture limit: "+w+"x"+h+" / "+maxTexture);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);
            if(w!=width||h!=height) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,source,0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[1]);
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,null);
                width=w;height=h;
            } else GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D,0,0,0,source);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0]);
            if(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)!=GLES20.GL_FRAMEBUFFER_COMPLETE)throw new IOException("Recipe framebuffer is incomplete");
            GLES20.glViewport(0,0,w,h);GLES20.glUseProgram(program);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textures[0]);
            GLES20.glUniform1i(sourceLocation,0);GLES20.glUniform1i(algosLocation,recipe.algos());
            GLES20.glUniform2f(resolutionLocation,w,h);
            float[] values=recipe.uniforms();for(int i=0;i<NAMES.length;i++)GLES20.glUniform1f(uniforms[i],values[i]);
            quad.position(0);GLES20.glEnableVertexAttribArray(position);GLES20.glEnableVertexAttribArray(tex);
            GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,16,quad);quad.position(2);
            GLES20.glVertexAttribPointer(tex,2,GLES20.GL_FLOAT,false,16,quad);
            int needed=Math.multiplyExact(Math.multiplyExact(w,h),4);
            if(pixels==null||pixels.capacity()<needed)pixels=ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder());
            pixels.clear();pixels.limit(needed);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
            GLES20.glReadPixels(0,0,w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,pixels);
            int error=GLES20.glGetError();if(error!=GLES20.GL_NO_ERROR)throw new IOException("Recipe GLES failed: 0x"+Integer.toHexString(error));
            pixels.position(0);result=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888,source.hasAlpha(),source.getColorSpace());result.setDensity(source.getDensity());result.copyPixelsFromBuffer(pixels);return result;
        } catch(Throwable failure) {
            primary=failure;if(result!=null)result.recycle();destroy();
            if(failure instanceof IOException)throw (IOException)failure;
            if(failure instanceof RuntimeException)throw (RuntimeException)failure;
            if(failure instanceof Error)throw (Error)failure;
            throw new IOException("Recipe GLES execution failed",failure);
        } finally {restore(previousDisplay,previousContext,previousDraw,previousRead,primary,result);}
    }

    private void initialize()throws IOException {
        display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] version=new int[2];
        if(EGL14.EGL_NO_DISPLAY.equals(display)||!EGL14.eglInitialize(display,version,0,version,1))throw new IOException("Cannot initialize recipe GLES display");
        int[] attrs={EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_NONE};
        EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];
        if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,count,0)||count[0]<1)throw new IOException("No recipe GLES config");
        context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
        surface=EGL14.eglCreatePbufferSurface(display,configs[0],new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
        if(EGL14.EGL_NO_CONTEXT.equals(context)||EGL14.EGL_NO_SURFACE.equals(surface)||!EGL14.eglMakeCurrent(display,surface,surface,context))throw new IOException("Cannot bind recipe GLES context");
        int[] maximum=new int[1];GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE,maximum,0);maxTexture=maximum[0];
        int vertex=0,pixel=0;
        try {
            vertex=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 aPos;attribute vec2 aTex;varying highp vec2 vTex;void main(){gl_Position=vec4(aPos,0.,1.);vTex=aTex;}");
            pixel=shader(GLES20.GL_FRAGMENT_SHADER,fragment);program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,pixel);GLES20.glLinkProgram(program);
            int[] status=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,status,0);
            if(status[0]==0)throw new IOException("Recipe shader link failed: "+GLES20.glGetProgramInfoLog(program));
        } finally {if(vertex!=0)GLES20.glDeleteShader(vertex);if(pixel!=0)GLES20.glDeleteShader(pixel);}
        GLES20.glGenTextures(2,textures,0);
        for(int texture:textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        }
        GLES20.glGenFramebuffers(1,fbo,0);GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER,fbo[0]);GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,GLES20.GL_COLOR_ATTACHMENT0,GLES20.GL_TEXTURE_2D,textures[1],0);
        sourceLocation=GLES20.glGetUniformLocation(program,"uSource");algosLocation=GLES20.glGetUniformLocation(program,"Algos");resolutionLocation=GLES20.glGetUniformLocation(program,"Resolution");
        GLES20.glUseProgram(program);GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"CurrentTime"),0);
        GLES20.glUniform2f(GLES20.glGetUniformLocation(program,"VignetteCenter"),.5f,.5f);GLES20.glUniform3f(GLES20.glGetUniformLocation(program,"VignetteColor"),0,0,0);
        for(int i=0;i<NAMES.length;i++)uniforms[i]=GLES20.glGetUniformLocation(program,NAMES[i]);
        position=GLES20.glGetAttribLocation(program,"aPos");tex=GLES20.glGetAttribLocation(program,"aTex");
        quad=ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).asFloatBuffer();quad.put(new float[]{-1,-1,0,0,1,-1,1,0,-1,1,0,1,1,1,1,1});
    }

    synchronized void close() {
        if(EGL14.EGL_NO_CONTEXT.equals(context))return;
        // apply() leaves this unshared context detached. Destroying it releases
        // its GL objects without rebinding a host surface being torn down.
        releaseOwned();
    }
    private void destroy() {
        if(!EGL14.EGL_NO_CONTEXT.equals(context)&&context.equals(EGL14.eglGetCurrentContext())) {
            if(program!=0)GLES20.glDeleteProgram(program);GLES20.glDeleteTextures(2,textures,0);GLES20.glDeleteFramebuffers(1,fbo,0);
            EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
        }
        releaseOwned();
    }
    private void releaseOwned() {
        if(!EGL14.EGL_NO_SURFACE.equals(surface))EGL14.eglDestroySurface(display,surface);
        if(!EGL14.EGL_NO_CONTEXT.equals(context))EGL14.eglDestroyContext(display,context);
        display=EGL14.EGL_NO_DISPLAY;context=EGL14.EGL_NO_CONTEXT;surface=EGL14.EGL_NO_SURFACE;program=width=height=0;
        textures[0]=textures[1]=fbo[0]=0;quad=null;pixels=null;
    }
    private static void restore(EGLDisplay display,EGLContext context,EGLSurface draw,EGLSurface read,Throwable primary,Bitmap result) {
        boolean ok=true;
        if(!EGL14.EGL_NO_DISPLAY.equals(display))ok=EGL14.eglMakeCurrent(display,draw,read,context);
        else {EGLDisplay current=EGL14.eglGetCurrentDisplay();if(!EGL14.EGL_NO_DISPLAY.equals(current))ok=EGL14.eglMakeCurrent(current,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);}
        if(!ok) {IllegalStateException failure=new IllegalStateException("Cannot restore host EGL context");if(primary!=null)primary.addSuppressed(failure);else {if(result!=null)result.recycle();throw failure;}}
    }
    private static int shader(int type,String source)throws IOException {
        int shader=GLES20.glCreateShader(type);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);int[] status=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,status,0);
        if(status[0]==0){String error=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new IOException("Camera recipe shader compile failed: "+error);}return shader;
    }
}
