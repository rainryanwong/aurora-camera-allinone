package com.aurora.camera.compat.classic;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.app.Activity;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** 1770 lifecycle boundaries for the explicitly selected classic software LUT. */
public final class ClassicCvHooks240 {
    public interface Selection {String model();int mode();}
    private final Method effect,current,write,cvType,common,updateAiScene,attach;
    private final Method config,component,setStyle,raw,ultraRaw,onMode,targetMode,aiPreviewReady;
    private final Method streetCreate,streetDestroy,moduleCallback,renderView,addRenderer,removeRenderer,setRenderView;
    private final Class<?> styleType;
    private final Field none,styleMode,capabilities;
    private final Field effectView;
    private final Object cvRendererKind;
    public ClassicCvHooks240(Host h)throws ReflectiveOperationException{
        effect=h.method("com.xiaomi.camera.effect.EffectController","u");
        current=h.method("com.xiaomi.camera.effect.EffectController","j");
        write=h.method("com.xiaomi.camera.effect.EffectController","b0",int.class);
        cvType=h.method("com.android.camera.data.data.i","o");
        common=h.method("m6.b","d",int.class);
        none=h.field("i3.b","R");
        updateAiScene=h.method("com.android.camera.module.Camera2Module","updateAiScene");
        attach=h.method("Tu.c","d",h.type("Iu.s"));
        styleType=h.type("r2.m");
        config=h.method("g2.a","a");
        component=h.method("Xh.b","x",Class.class);
        styleMode=h.field("com.android.camera.data.data.c","mCurrentMode");
        setStyle=h.method(styleType,"setComponentValue",int.class,String.class);
        capabilities=h.field(styleType,"b");
        raw=h.method("com.android.camera.data.data.n","a0",int.class);
        ultraRaw=h.method("com.android.camera.data.data.n","p0",int.class,capabilities.getType());
        Class<?> startControl=h.type("com.android.camera.module.loader.base.StartControl");
        onMode=h.method("com.android.camera.Camera","i8",startControl);
        targetMode=h.method(startControl,"getTargetMode");
        aiPreviewReady=h.method("com.android.camera.features.mode.ai.AiModule","doWhenPreviewSessionSuccess");
        String street="com.android.camera.features.mode.street.StreetModule";
        streetCreate=h.method(street,"onRenderEngineCreate");
        streetDestroy=h.method(street,"onRenderEngineDestroy");
        moduleCallback=h.method("com.android.camera.module.u","getModuleCallback");
        renderView=h.method("com.android.camera.module.b0","fi");
        Class<?> kind=h.type("Ku.d");
        cvRendererKind=h.field(kind,"o").get(null);
        addRenderer=h.method("Iu.v","V0",kind);
        removeRenderer=h.method("Iu.v","c1",kind);
        setRenderView=h.method("com.xiaomi.camera.effect.EffectController","s0",h.type("Iu.v"));
        effectView=h.field("com.xiaomi.camera.effect.EffectController","V");
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
        private Object streetModule,streetView;
        private int streetMode;
        private boolean streetRendererAdded;
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
        private Object activeStyle() {
            return invoke(component,invoke(config,null),styleType);
        }

        private int effectiveCvType(int mode) {
            if (!ClassicCatalog239.supportsMode(mode)) return 0;
            Object style=activeStyle();
            try {
                if (styleMode.getInt(style)!=mode) return 0;
                // The active component resolves the shared or dedicated portrait preference.
                int selected=(Integer)invoke(cvType,null);
                if (selected!=1) return selected;
                if (mode==167 && ((Boolean)invoke(raw,null,mode)
                        || (Boolean)invoke(ultraRaw,null,mode,capabilities.get(style)))) return 0;
                return selected;
            } catch (IllegalAccessException failure) {
                throw new IllegalStateException("Cannot read active Leica CV mode",failure);
            }
        }

        public boolean classicSelected() {
            return effectiveCvType(selection.mode())==1;
        }

        public void sync(String reason){sync(reason,false);}

        private void sync(String reason,boolean viewChanged){
            int mode=selection.mode();
            String model=selection.model();
            int type=effectiveCvType(mode);
            boolean street=ClassicCatalog239.isStreetMode(mode);
            boolean rendererChanged=street && updateStreetRenderer(mode,type==1 && !"native".equals(model));
            controller.sync(model,mode,type,reason,rendererChanged || (street && viewChanged));
        }

        private synchronized boolean updateStreetRenderer(int mode,boolean enabled){
            if(streetView==null || streetMode!=mode || streetRendererAdded==enabled)return false;
            // StreetModule does not add this renderer to the GL draw list.
            if(enabled){
                if(invoke(addRenderer,streetView,cvRendererKind)==null)
                    throw new IllegalStateException("Street CV renderer is unavailable");
            }else invoke(removeRenderer,streetView,cvRendererKind);
            streetRendererAdded=enabled;
            Log.i("AuroraClassic240","street_renderer_queued mode="+mode+" added="+enabled);
            return enabled;
        }

        private synchronized void bindStreet(Object module,Object view,int mode){
            if(streetModule==module && streetView==view)return;
            releaseStreet(streetModule);
            streetModule=module;
            streetView=view;
            streetMode=mode;
        }

        private synchronized void releaseStreet(Object module){
            if(streetModule!=module)return;
            if(streetRendererAdded)updateStreetRenderer(streetMode,false);
            streetModule=null;
            streetView=null;
        }

        private synchronized boolean usesStreetView(Object view){
            return view!=null && streetView==view && streetMode==selection.mode();
        }
        private void install(){
            HookDiagnostics.hookMethod(write,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())controller.nativeWriteCompleted(p.thisObject,(Integer)p.args[0]);
            }});
            HookDiagnostics.hookMethod(updateAiScene,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(p.hasThrowable())return;
                // Ignore a stale module callback after the foreground mode moved.
                int source=(Integer)XposedHelpers.callMethod(p.thisObject,"getModuleIndex");
                if(source==selection.mode())sync("update_ai_scene");
            }});
            HookDiagnostics.hookMethod(attach,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(!p.hasThrowable())sync("classic_renderer_attach");
            }});
            HookDiagnostics.hookMethod(setStyle,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(p.hasThrowable())return;
                int mode=selection.mode(), written=(Integer)p.args[0];
                if((written==mode || written==160) && ClassicCatalog239.supportsMode(mode)
                        && p.thisObject==activeStyle())sync("photography_style_committed");
            }});
            HookDiagnostics.hookMethod(aiPreviewReady,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(!p.hasThrowable() && selection.mode()==168)sync("ai_preview_ready");
            }});
            HookDiagnostics.hookMethod(streetCreate,new StrictHook(){@Override protected void after(MethodHookParam p){
                if(p.hasThrowable())return;
                int mode=(Integer)XposedHelpers.callMethod(p.thisObject,"getModuleIndex");
                Object callback=invoke(moduleCallback,p.thisObject);
                if(callback==null)return;
                bindStreet(p.thisObject,invoke(renderView,callback),mode);
                if(mode==selection.mode())sync("street_renderer_create");
            }});
            HookDiagnostics.hookMethod(streetDestroy,new StrictHook(){@Override protected void before(MethodHookParam p){
                releaseStreet(p.thisObject);
            }});
            HookDiagnostics.hookMethod(setRenderView,new StrictHook(){
                @Override protected void before(MethodHookParam p)throws IllegalAccessException{
                    if(!usesStreetView(p.args[0]))return;
                    WeakReference<?> previous=(WeakReference<?>)effectView.get(p.thisObject);
                    if(previous==null || previous.get()!=p.args[0])p.setObjectExtra("classic.street.viewChanged",Boolean.TRUE);
                }
                @Override protected void after(MethodHookParam p){
                    if(!p.hasThrowable() && Boolean.TRUE.equals(p.getObjectExtra("classic.street.viewChanged"))
                            && usesStreetView(p.args[0]))sync("street_effect_view_bound",true);
                }
            });
            HookDiagnostics.hookMethod(onMode,new StrictHook(){@Override protected void before(MethodHookParam p){
                Activity owner=(Activity)p.thisObject;
                if(!owner.isFinishing() && !owner.isDestroyed()) {
                    controller.beforeModeChange((Integer)invoke(targetMode,p.args[0]));
                }
            }});
        }
    }
    public Runtime install(Selection selection){Runtime runtime=new Runtime(selection);runtime.install();return runtime;}
}
