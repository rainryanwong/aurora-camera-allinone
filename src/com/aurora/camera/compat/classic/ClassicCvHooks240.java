package com.aurora.camera.compat.classic;

import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** 1770 lifecycle boundaries for the explicitly selected classic software LUT. */
public final class ClassicCvHooks240 {
    public interface Selection {String model();int mode();}
    private final Method effect,current,write,cvType,common,updateAiScene,attach;
    private final Field none;
    public ClassicCvHooks240(Host h)throws ReflectiveOperationException{
        effect=h.method("com.xiaomi.camera.effect.EffectController","u");
        current=h.method("com.xiaomi.camera.effect.EffectController","j");
        write=h.method("com.xiaomi.camera.effect.EffectController","b0",int.class);
        cvType=h.method("com.android.camera.data.data.i","o");
        common=h.method("m6.b","d",int.class);
        none=h.field("i3.b","R");
        updateAiScene=h.method("com.android.camera.module.Camera2Module","updateAiScene");
        attach=h.method("Tu.c","d",h.type("Iu.s"));
    }
    private static Object invoke(Method method,Object receiver,Object...args){
        try{return method.invoke(receiver,args);}
        catch(IllegalAccessException failure){throw new IllegalStateException(method.toString(),failure);}
        catch(InvocationTargetException failure){
            Throwable cause=failure.getCause();
            if(cause instanceof RuntimeException)throw(RuntimeException)cause;
            if(cause instanceof Error)throw(Error)cause;
            throw new IllegalStateException(method.toString(),cause);
        }
    }
    public final class Runtime {
        private final Selection selection;
        private final ClassicCv240 controller;
        private Runtime(Selection selection){
            this.selection=selection;
            controller=new ClassicCv240(new ClassicCv240.NativeState(){
                public Object effect(){return invoke(effect,null);}
                public int current(Object owner){return(Integer)invoke(current,owner);}
                public int none(){try{return none.getInt(null);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
                public int common(){return(Integer)invoke(common,null,0);}
                public void write(Object owner,int value){invoke(write,owner,value);}
                public void log(String text){Log.i("AuroraClassic240",text);}
            });
        }
        public void sync(String reason){
            controller.sync(selection.model(),selection.mode(),(Integer)invoke(cvType,null),reason);
        }
        private void install(){
            XposedBridge.hookMethod(write,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())controller.nativeWriteCompleted(p.thisObject,(Integer)p.args[0]);
            }});
            XposedBridge.hookMethod(updateAiScene,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(p.hasThrowable())return;
                // Ignore a stale module callback after the foreground mode moved.
                int source=(Integer)XposedHelpers.callMethod(p.thisObject,"getModuleIndex");
                if(source==selection.mode())sync("update_ai_scene");
            }});
            XposedBridge.hookMethod(attach,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())sync("classic_renderer_attach");
            }});
        }
    }
    public Runtime install(Selection selection){Runtime runtime=new Runtime(selection);runtime.install();return runtime;}
}
