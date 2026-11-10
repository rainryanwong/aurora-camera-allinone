package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Match Photo's filter/picture-style nodes without changing Night's CV node. */
public final class NightPaletteRenderHooks {
    private NightPaletteRenderHooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        final String night="com.android.camera.features.mode.night.photo.NightModule";
        final Method create=h.method(night,"onRenderEngineCreate"),destroy=h.method(night,"onRenderEngineDestroy");
        final Field mode=h.field("com.android.camera.module.u","mModuleIndex");
        final Field callback=h.field("com.android.camera.module.u","mCallback");
        final Method engine=h.method("com.android.camera.module.b0","fi");
        final Method supported=h.method("com.android.camera.data.data.F","R0");
        final Field pictureStyle=h.field("Ku.d","P");
        final Field[] filters={h.field("Ku.d","h"),h.field("Ku.d","i"),h.field("Ku.d","j"),h.field("Ku.d","f")};
        final Method add=h.method("F8.j","V0",h.type("Ku.d")),remove=h.method("F8.j","c1",h.type("Ku.d"));
        return new HookPlan(){
            public String id(){return "214.night-native-filter-renderers";}
            public void install(){
                XposedBridge.hookMethod(create,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||mode.getInt(p.thisObject)!=173)return;
                        Object owner=callback.get(p.thisObject);
                        if(owner==null)return;
                        Object render=engine.invoke(owner);
                        if(render==null)return;
                        // e0 updates Ku.d.f; attribute updates do not put a node
                        // in the local preview graph. Photo also registers h/i/j
                        // for its built-in soft-focus filter consumers.
                        for(Field filter:filters)add.invoke(render,filter.get(null));
                        if((Boolean)supported.invoke(null))add.invoke(render,pictureStyle.get(null));
                    }
                });
                XposedBridge.hookMethod(destroy,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||mode.getInt(p.thisObject)!=173)return;
                        // Native CaptureModule removes P unconditionally: global
                        // palette qualification can change before old mode teardown.
                        Object owner=callback.get(p.thisObject);
                        // Native teardown explicitly permits an already-detached
                        // callback. Match that boundary before reflective invoke.
                        if(owner==null)return;
                        Object render=engine.invoke(owner);
                        if(render==null)return;
                        for(Field filter:filters)remove.invoke(render,filter.get(null));
                        remove.invoke(render,pictureStyle.get(null));
                    }
                });
            }
        };
    }
}
