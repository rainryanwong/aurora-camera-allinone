package com.aurora.camera.compat.classic;

import android.util.Log;
import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/** Invalidate only an existing classic renderer, on its own RenderEngineV2 GL queue. */
public final class ClassicPreview241 {
    private static final String MODEL="aurora.classic240.nativeModel";
    private final Method effect,engine,post,request;
    private final Field viewRef,pool,renderers,cvKind,token,bitmap,rebuild;
    private final Class<?> classic;
    public ClassicPreview241(Host host)throws ReflectiveOperationException{
        effect=host.method("com.xiaomi.camera.effect.EffectController","u");
        viewRef=host.field("com.xiaomi.camera.effect.EffectController","V");
        engine=host.method("Iu.v","Z0");
        post=host.method("Iu.s","w",Runnable.class,String.class);
        request=host.method("Iu.v","requestRender");
        pool=host.field("Iu.s","G");renderers=host.field("Tu.y","a");cvKind=host.field("Ku.d","o");
        token=host.field("Tu.h","D");bitmap=host.field("Tu.h","J");rebuild=host.field("Tu.h","N");
        classic=host.type("Tu.c");
    }
    private static Object invoke(Method method,Object owner,Object...args){
        try{return method.invoke(owner,args);}
        catch(IllegalAccessException failure){throw new IllegalStateException(method.toString(),failure);}
        catch(InvocationTargetException failure){
            Throwable cause=failure.getCause();
            if(cause instanceof RuntimeException)throw(RuntimeException)cause;
            if(cause instanceof Error)throw(Error)cause;
            throw new IllegalStateException(method.toString(),cause);
        }
    }
    private Object view(){
        try{
            WeakReference<?> reference=(WeakReference<?>)viewRef.get(invoke(effect,null));
            return reference==null?null:reference.get();
        }catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    public Object cachedModel(Object renderer){return XposedHelpers.getAdditionalInstanceField(renderer,MODEL);}
    /** Called immediately before the native draw, never from onAttach. */
    public boolean prepareModel(Object renderer,String selected){
        if(selected.equals(cachedModel(renderer)))return false;
        try{token.set(renderer,null);bitmap.set(renderer,null);rebuild.setBoolean(renderer,true);return true;}
        catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    /** A failed/skipped native draw cannot claim that its cached bitmap belongs to the new model. */
    public void completeModel(Object renderer,String selected,boolean success){
        try{if(success&&bitmap.get(renderer)!=null)XposedHelpers.setAdditionalInstanceField(renderer,MODEL,selected);}
        catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    /** False means the native view/engine is absent; lifecycle hooks must handle later attachment. */
    public boolean request(String reason,boolean reloadLut,boolean rebuildSdk){
        return request(reason,reloadLut,rebuildSdk,null);
    }
    public boolean request(String reason,boolean reloadLut,boolean rebuildSdk,Runnable applied){
        final Object view=view();
        if(view==null){Log.i("AuroraPreview241","refresh_deferred reason="+reason+" state=no_view");return false;}
        final Object target=invoke(engine,view);
        if(target==null){Log.i("AuroraPreview241","refresh_deferred reason="+reason+" state=no_engine");return false;}
        invoke(post,target,(Runnable)()->{
            Object active=view();
            if(active!=view||invoke(engine,active)!=target){
                Log.i("AuroraPreview241","refresh_deferred reason="+reason+" state=engine_replaced");return;
            }
            try{
                // Iu.v.V0 is an add-local-renderer API, so use the existing pool map instead.
                Map<?,?> existing=(Map<?,?>)renderers.get(pool.get(target));
                Object renderer=existing.get(cvKind.get(null));
                if(renderer==null){Log.i("AuroraPreview241","refresh_deferred reason="+reason+" state=no_classic_renderer");return;}
                if(!classic.isInstance(renderer))throw new IllegalStateException("Unexpected native classic renderer "+renderer.getClass());
                if(reloadLut){token.set(renderer,null);bitmap.set(renderer,null);}
                if(rebuildSdk)rebuild.setBoolean(renderer,true);
                invoke(request,view);
                if(applied!=null)applied.run();
                Log.i("AuroraPreview241","refresh_applied reason="+reason+" renderer="+renderer.getClass().getName()+" reloadLut="+reloadLut+" rebuildSdk="+rebuildSdk+" thread="+Thread.currentThread().getName());
            }catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        },"AuroraPreview241:"+reason);
        return true;
    }
}
