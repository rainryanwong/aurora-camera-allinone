package com.aurora.camera.compat;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Mode229 used a fixed equip material despite exposing the standard picker. */
public final class StreetShutter229Hooks {
    private StreetShutter229Hooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        Method setup=h.method("com.android.camera.ui.CameraSnapView","setParameters",h.type("v2.I0"));
        Method draw=h.method("A8.h","l",h.type("v2.I0")),selected=h.method("com.android.camera.data.data.x","v");
        Method material=h.method("j7.d","e",Context.class,String.class,h.type("A8.E"));
        Method clicked=h.method("j7.b","v",int.class,boolean.class,boolean.class);
        Field mode=h.field("v2.I0","a"),context=h.field("A8.h","o"),paint=h.field("A8.h","j"),token=h.field("A8.E","L");
        Method refresh=h.method("A8.E","u"),uri=h.method("com.android.camera.data.data.x","h",String.class),custom=h.method("A8.E","w",Context.class,String.class);
        Map<Object,Object> views=Collections.synchronizedMap(new WeakHashMap<>());
        return new HookPlan(){public String id(){return "229.street-shutter-material";}
            public void install(){
                XposedBridge.hookMethod(setup,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{int current=mode.getInt(p.args[0]);if(!p.hasThrowable()&&(current==229||ShutterStyleHooks.supported(current)))views.put(p.thisObject,p.args[0]);else views.remove(p.thisObject);}});
                XposedBridge.hookMethod(draw,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable()||mode.getInt(p.args[0])!=229)return;String value=(String)selected.invoke(null);
                    if("custom_shutter_default".equals(value))return;
                    // The picker owns the valid preset/custom identities. Use the same
                    // material loader as the native normal-mode drawable.
                    Object target=paint.get(p.thisObject);
                    if(value.startsWith("custom_shutter_custom"))custom.invoke(target,context.get(p.thisObject),uri.invoke(null,value));
                    else material.invoke(null,context.get(p.thisObject),value,target);
                    token.set(target,value);refresh.invoke(target);((Drawable)p.thisObject).invalidateSelf();
                    Log.i("AuroraCameraCompat","street229 shutter material="+value);
                }});
                XposedBridge.hookMethod(clicked,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;Map<Object,Object> copy; synchronized(views){copy=new java.util.HashMap<>(views);}
                    for(Map.Entry<Object,Object> entry:copy.entrySet()){View view=(View)entry.getKey();if(view.isAttachedToWindow())setup.invoke(view,entry.getValue());}
                }});
            }
        };
    }
}
