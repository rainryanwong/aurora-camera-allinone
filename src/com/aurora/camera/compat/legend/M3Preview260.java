package com.aurora.camera.compat.legend;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.opengl.GLES20;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** M3's OES monochrome and capture-parameter CvStyle stages on the main preview. */
public final class M3Preview260 {
    private static final String TAG="AuroraM3Preview",UNIFORM="260.m3.uniform",PREVIEW="260.m3.preview";
    private static final String DECLARATION="uniform float uMixAlpha; uniform samplerExternalOES sTexture;",OUTPUT="outColor = color*uAlpha;";
    private static final CaptureRequest.Key<int[]> MODULE=new CaptureRequest.Key<>("xiaomi.app.module",int[].class);
    private static final CaptureResult.Key<byte[]> AEC=new CaptureResult.Key<>("org.quic.camera2.statsconfigs.AECFrameControl",byte[].class);
    private final Host host;
    private final Method global,mode,repository,component,value,controller,engine,shader,initialize,hasRender,render,release,metadata;
    private final Method config,update,draw,sdkRelease,bindFramebuffer,bufferWidth,bufferHeight,bufferTexture,bufferFbo,swap;
    private final Field intent,mainEngine,oes,textureTarget,program,ready,buffers,input,output,rendererEngine,handle;
    private final Constructor<?> sdkConstructor;
    private final Class<?> legend;
    private final List<Method> draws=new ArrayList<>();
    private volatile Exposure exposure;
    private M3Parameters260 parameters;
    private static final class Exposure {final int lux;final float zoom;Exposure(int lux,float zoom){this.lux=lux;this.zoom=zoom;}}
    public M3Preview260(Host h)throws ReflectiveOperationException{
        host=h;global=h.method("g2.a","g");mode=h.method("u2.Y","D",int.class);intent=h.field("u2.Y","u");
        repository=h.method("g2.a","a");component=h.method("Xh.b","x",Class.class);legend=h.type("r2.B");value=h.method("com.android.camera.data.data.c","getComponentValue",int.class);
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");mainEngine=h.field("com.xiaomi.camera.effect.EffectController","V");engine=h.method("F8.j","Z0");oes=h.field("Iu.s","B");
        shader=h.method("com.xiaomi.gl.ShaderManager","getShaderByType",int.class);initialize=h.method("Ru.a","c");textureTarget=h.field("Ru.a","b");program=h.field("Ru.a","c");
        for(Method m:h.type("Ru.a").getDeclaredMethods())if(m.getName().equals("a"))draws.add(m);
        hasRender=h.method("Iu.s","l");render=h.method("Iu.s","e",boolean.class);ready=h.field("Iu.s","f0");buffers=h.field("Iu.s","D");input=h.field("Ju.a","a");output=h.field("Ju.a","b");swap=h.method("Ju.a","d");
        bufferWidth=h.method("Ju.b","d");bufferHeight=h.method("Ju.b","b");bufferTexture=h.method("Ju.b","c");bufferFbo=h.method("Ju.b","a");
        release=h.method("Tu.w","f");rendererEngine=h.field("Tu.x","c");metadata=h.method("l9.y0$k","onCaptureCompleted",CameraCaptureSession.class,CaptureRequest.class,TotalCaptureResult.class);
        Class<?> sdk=h.type("com.xiaomi.milab.filtersdk.CandySDK");sdkConstructor=sdk.getDeclaredConstructor(int.class);sdkConstructor.setAccessible(true);handle=h.field(sdk,"a");
        config=h.method(sdk,"configPipeline",long.class,String.class);update=h.method(sdk,"updatePipeline",long.class,String.class);draw=h.method(sdk,"d",float[].class,int.class,int.class,int.class,int.class,float[].class);sdkRelease=h.method(sdk,"e");
        bindFramebuffer=h.method("com.xiaomi.gl.MIGL","glBindFramebuffer",int.class);
    }
    private Object main()throws ReflectiveOperationException{
        Object reference=mainEngine.get(controller.invoke(null));Object owner=reference instanceof WeakReference?((WeakReference<?>)reference).get():null;
        return owner==null?null:engine.invoke(owner);
    }
    private boolean m3()throws ReflectiveOperationException{
        Object data=global.invoke(null);if((Integer)mode.invoke(data,intent.getInt(data))!=256)return false;
        return "M3".equals(value.invoke(component.invoke(repository.invoke(null),legend),256));
    }
    static String inject(String source){
        if(source.indexOf(DECLARATION)<0||source.indexOf(DECLARATION)!=source.lastIndexOf(DECLARATION)||source.indexOf(OUTPUT)<0||source.indexOf(OUTPUT)!=source.lastIndexOf(OUTPUT))throw new IllegalArgumentException("M3 OES shader source differs from host renderer");
        return source.replace(DECLARATION,"uniform float uMixAlpha; uniform float uM3Gray; uniform samplerExternalOES sTexture;")
                .replace(OUTPUT,"float _m3y = dot(color.rgb, vec3(0.299, 0.587, 0.114)); color.rgb = mix(color.rgb, vec3(_m3y), uM3Gray); outColor = color*uAlpha;");
    }
    public void install()throws Exception{
        try(ZipFile apk=new ZipFile(host.moduleApkPath);InputStream in=apk.getInputStream(apk.getEntry("assets/legend260/leica_filter_param_m3.bin"));ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];for(int n;(n=in.read(buffer))!=-1;)out.write(buffer,0,n);parameters=new M3Parameters260(out.toByteArray());
        }
        XposedBridge.hookMethod(shader,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&(Integer)p.args[0]==2)p.setResult(inject((String)p.getResult()));}});
        XposedBridge.hookMethod(initialize,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||textureTarget.getInt(p.thisObject)!=36197)return;
            Uniform state=new Uniform(program.getInt(p.thisObject));XposedHelpers.setAdditionalInstanceField(p.thisObject,UNIFORM,state);state.apply(false);
        }});
        for(Method method:draws)XposedBridge.hookMethod(method,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            if(textureTarget.getInt(p.thisObject)!=36197)return;
            Object owner=main();boolean enabled=owner!=null&&oes.get(owner)==p.thisObject&&m3();
            ((Uniform)XposedHelpers.getAdditionalInstanceField(p.thisObject,UNIFORM)).apply(enabled);
        }});
        XposedBridge.hookMethod(metadata,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||!m3())return;TotalCaptureResult result=(TotalCaptureResult)p.args[2];int[] module=result.getRequest().get(MODULE);
            if(module==null||module.length!=1||module[0]!=256)return;
            byte[] aec=result.get(AEC);Float zoom=result.get(CaptureResult.CONTROL_ZOOM_RATIO);if(aec==null||aec.length<100||zoom==null)return;
            float lux=ByteBuffer.wrap(aec).order(ByteOrder.LITTLE_ENDIAN).getFloat(96);
            if(!Float.isFinite(lux)||lux<0||lux>65535||!Float.isFinite(zoom)||zoom<=0)throw new IllegalArgumentException("Invalid M3 preview exposure metadata");
            exposure=new Exposure((int)lux,zoom);
        }});
        XposedBridge.hookMethod(hasRender,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable()&&p.thisObject==main()&&m3())p.setResult(true);}});
        XposedBridge.hookMethod(render,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            Exposure current=exposure;if(p.hasThrowable()||current==null||!ready.getBoolean(p.thisObject)||p.thisObject!=main()||!m3())return;
            Preview preview=(Preview)XposedHelpers.getAdditionalInstanceField(p.thisObject,PREVIEW);
            if(preview==null){preview=new Preview();XposedHelpers.setAdditionalInstanceField(p.thisObject,PREVIEW,preview);}
            preview.render(p.thisObject,current);
        }});
        XposedBridge.hookMethod(release,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            Object owner=rendererEngine.get(p.thisObject);Preview preview=(Preview)XposedHelpers.removeAdditionalInstanceField(owner,PREVIEW);
            if(preview!=null)sdkRelease.invoke(preview.sdk);
        }});
    }
    private static final class Uniform {
        final int program,location;int applied=-1;
        Uniform(int program){this.program=program;location=GLES20.glGetUniformLocation(program,"uM3Gray");if(location<0)throw new IllegalStateException("M3 grayscale uniform missing");}
        void apply(boolean enabled){
            int wanted=enabled?1:0;if(applied==wanted)return;
            int[] previous=new int[1];GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM,previous,0);GLES20.glUseProgram(program);GLES20.glUniform1f(location,wanted);GLES20.glUseProgram(previous[0]);applied=wanted;
            Log.i(TAG,"main preview grayscale="+wanted+" program="+program);
        }
    }
    private final class Preview {
        final Object sdk;final long id;String configuration;boolean observed;
        final float[] identity={1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1};
        Preview()throws ReflectiveOperationException{sdk=sdkConstructor.newInstance(6);id=handle.getLong(sdk);if(id==0)throw new IllegalStateException("M3 preview renderer initialization failed");}
        void render(Object owner,Exposure exposure)throws ReflectiveOperationException{
            Object pair=buffers.get(owner),from=input.get(pair),to=output.get(pair);int width=(Integer)bufferWidth.invoke(from),height=(Integer)bufferHeight.invoke(from);
            String next=parameters.configuration(width,height,exposure.lux,exposure.zoom);
            if(!next.equals(configuration)){
                if(!(Boolean)(configuration==null?config:update).invoke(null,id,next))throw new IllegalStateException("M3 preview CvStyle configuration failed");configuration=next;
            }
            draw.invoke(sdk,identity,(Integer)bufferTexture.invoke(from),(Integer)bufferFbo.invoke(to),width,height,new float[]{0,0,width,height});bindFramebuffer.invoke(null,0);swap.invoke(pair);
            if(!observed){observed=true;Log.i(TAG,"M3 CvStyle preview rendered lux="+exposure.lux+" zoom="+exposure.zoom+" size="+width+"x"+height);}
        }
    }
}
