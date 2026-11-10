package com.aurora.camera.compat.vignette;

import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.compat.classic.ClassicCatalog239;
import com.aurora.camera.compat.classic.ClassicPreview241;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Formula revisions must invalidate the Candy pipeline used by RenderEngineV2. */
public final class AuthRuntime241 {
    private static final String TAG="AuroraAUTH241",REV="aurora.auth241.sdkRevision";
    private final ClassicPreview241 preview;
    private final Method draw,configure,update,process;
    private final Field attribute,token,sdk,rebuild;
    private final Class<?> classic;
    private final ThreadLocal<StrictHook.MethodHookParam> drawing=new ThreadLocal<>();
    private volatile Formula formula=new Formula(0,"unloaded");
    private static final class Formula {
        final long revision;final String sha256;
        Formula(long revision,String sha256){this.revision=revision;this.sha256=sha256;}
    }
    public AuthRuntime241(Host host)throws ReflectiveOperationException{
        preview=new ClassicPreview241(host);classic=host.type("Tu.c");
        draw=host.method("Tu.h","g",host.type("Iu.w"));
        attribute=host.field("Tu.h","G");token=host.field("Mu.d","b");
        sdk=host.field("Tu.h","K");rebuild=host.field("Tu.h","N");
        configure=host.method("com.xiaomi.milab.filtersdk.CandySDK","configPipeline",long.class,String.class);
        update=host.method("com.xiaomi.milab.filtersdk.CandySDK","updatePipeline",long.class,String.class);
        process=host.method("com.xiaomi.milab.filtersdk.CandySDK","d",float[].class,int.class,int.class,int.class,int.class,float[].class);
    }
    private static String identity(Formula current){return "revision="+current.revision+" formula_sha256="+current.sha256;}
    public String identity(){return identity(formula);}
    public void changed(long revision,String body){
        byte[] digest;
        try{digest=MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        StringBuilder hex=new StringBuilder(64);for(byte b:digest)hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        formula=new Formula(revision,hex.toString());
        Log.i(TAG,"formula_committed revision="+revision+" formula_sha256="+hex+" bytes="+body.getBytes(StandardCharsets.UTF_8).length);
        preview.request("auth_formula_"+revision,false,true);
    }
    private static void failure(Object renderer,String key,String value,Throwable failure){
        String state=value+" failure="+failure;
        if(state.equals(XposedHelpers.getAdditionalInstanceField(renderer,key)))return;
        XposedHelpers.setAdditionalInstanceField(renderer,key,state);Log.e(TAG,value,failure);
    }
    public void install(){
        XposedBridge.hookMethod(draw,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                p.setObjectExtra("auth241.draw.previous",drawing.get());drawing.set(p);
                Formula current=formula;p.setObjectExtra("auth241.formula",current);
                Object a=attribute.get(p.thisObject);String lut=a==null?null:(String)token.get(a);
                if(!classic.isInstance(p.thisObject)||a==null||ClassicCatalog239.scene(lut)==null)return;
                p.setObjectExtra("auth241.tracked",true);
                Object previous=XposedHelpers.getAdditionalInstanceField(p.thisObject,REV);
                if((!(previous instanceof Long)||((Long)previous).longValue()!=current.revision)&&sdk.get(p.thisObject)!=null)rebuild.setBoolean(p.thisObject,true);
            }
            @Override protected void after(MethodHookParam p){
                try{
                    if(!p.hasThrowable()){
                        Formula current=(Formula)p.getObjectExtra("auth241.formula");
                        if(Boolean.TRUE.equals(p.getObjectExtra("auth241.tracked")))XposedHelpers.setAdditionalInstanceField(p.thisObject,REV,current.revision);
                    }else failure(p.thisObject,"aurora.auth248.draw.failure","preview_failed "+identity((Formula)p.getObjectExtra("auth241.formula")),p.getThrowable());
                }finally{
                    StrictHook.MethodHookParam previous=(StrictHook.MethodHookParam)p.getObjectExtra("auth241.draw.previous");if(previous==null)drawing.remove();else drawing.set(previous);
                }
            }
        });
        pipeline(configure,"config");pipeline(update,"update");
        XposedBridge.hookMethod(process,new StrictHook(){
            @Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())return;
                StrictHook.MethodHookParam current=drawing.get();if(current!=null)failure(current.thisObject,"aurora.auth248.process.failure","preview_consumer_failed "+identity((Formula)current.getObjectExtra("auth241.formula")),p.getThrowable());
            }
        });
    }
    private void pipeline(Method method,String event){
        XposedBridge.hookMethod(method,new StrictHook(){
            @Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())return;
                StrictHook.MethodHookParam current=drawing.get();if(current!=null)failure(current.thisObject,"aurora.auth248.pipeline.failure."+event,"sdk_"+event+"_failed "+identity((Formula)current.getObjectExtra("auth241.formula")),p.getThrowable());
            }
        });
    }
}
