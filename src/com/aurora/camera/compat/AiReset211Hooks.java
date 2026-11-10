package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** The shutter-side onAllReset tail, without its zoom animator or zoom protocol calls. */
public final class AiReset211Hooks {
    private AiReset211Hooks() {}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        Class<?> ai=h.type("com.android.camera.features.mode.ai.FragmentAi");
        final Method reset=h.method(ai,"lg"),global=h.method("g2.a","g");
        final Method preference=h.method("Xh.a","h",String.class,boolean.class);
        final Field mode=h.field("com.android.camera.fragment.h","mCurrentMode"),finished=h.field(ai,"y0");
        final Method hide=h.method(ai,"qt",boolean.class),clearImage=h.method(ai,"Xs"),clearUi=h.method(ai,"Gs");
        final Method resetGuide=h.method(ai,"Ys",h.type("C4.w")),component=h.method(ai,"Cs");
        final Constructor<?> recognize=h.type("C4.w").getDeclaredConstructor(Object.class,int.class);
        final Field empty=h.field("hv.v","a"),poses=h.field("v2.e","b"),old=h.field("v2.e","c"),next=h.field("v2.e","d"),token=h.field("v2.e","e"),retained=h.field(ai,"a0");
        recognize.setAccessible(true);
        return new HookPlan(){
            public String id(){return "211.ai-reset-keep-zoom-opt-in";}
            public void install(){
                XposedBridge.hookMethod(reset,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        if(mode.getInt(p.thisObject)!=168||!finished.getBoolean(p.thisObject)||!(Boolean)preference.invoke(global.invoke(null),"pref_aurora_ai_reset_keep_zoom",false))return;
                        // Exact native lg() non-zoom tail. Ys keeps its own async pose-reset
                        // callback and C4.w(12) performs the original EFFECT reset request.
                        hide.invoke(p.thisObject,false);
                        Object callback=recognize.newInstance(p.thisObject,12);
                        clearImage.invoke(p.thisObject);resetGuide.invoke(p.thisObject,callback);clearUi.invoke(p.thisObject);
                        Object item=component.invoke(null),list=empty.get(null);
                        if(item!=null){poses.set(item,list);old.setInt(item,-1);next.setInt(item,-1);token.set(item,"");}
                        retained.set(p.thisObject,list);p.setResult(null);
                    }
                });
            }
        };
    }
}
