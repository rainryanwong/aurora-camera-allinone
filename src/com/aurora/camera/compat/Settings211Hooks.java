package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Add the Laboratory tab to general camera settings. */
public final class Settings211Hooks {
    private Settings211Hooks() {}
    public static HookPlan resolve(Host h) throws ReflectiveOperationException {
        final Class<?> common=h.type("com.android.camera.fragment.settings.CameraCommonPreferenceFragment");
        final Class<?> lab=h.type("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment");
        final Method settings=h.method("com.android.camera.CameraPreferenceActivity","Lq",Bundle.class);
        // ActionBar.j is abstract. Xposed cannot register it; virtual calls from
        // both activities dispatch to this exact stock AppCompat implementation.
        final Method append=h.method("miuix.appcompat.internal.app.widget.r","j",Bundle.class,h.type("i.a$d"),Class.class,String.class);
        final Method tab=h.method("miuix.appcompat.internal.app.widget.r","f");
        if(Modifier.isAbstract(append.getModifiers())||Modifier.isStatic(append.getModifiers())||append.getReturnType()!=int.class)
            throw new NoSuchMethodException("Concrete stock ActionBar tab append contract");
        final Method title=h.method("miuix.appcompat.internal.app.widget.r$h","d",int.class);
        final Field debug=h.field("Sa.b","u");
        final ThreadLocal<Integer> scope=new ThreadLocal<>();
        return new HookPlan() {
            public String id(){return "211.laboratory-tab";}
            public void install(){
                HookDiagnostics.hookMethod(settings,enter(scope,1));
                HookDiagnostics.hookMethod(append,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||!Integer.valueOf(1).equals(scope.get())||p.args[2]!=common||debug.getBoolean(null))return;
                        Object next=tab.invoke(p.thisObject);title.invoke(next,0x7f1403cc);
                        append.invoke(p.thisObject,new Bundle(),next,lab,Integer.toString(0x7f1403cc));
                    }
                });
            }
        };
    }
    private static StrictHook enter(final ThreadLocal<Integer> scope,final int kind){
        return new StrictHook(){
            @Override protected void before(MethodHookParam p){p.setObjectExtra("211.tab.scope",scope.get());scope.set(kind);}
            @Override protected void after(MethodHookParam p){Integer previous=(Integer)p.getObjectExtra("211.tab.scope");if(previous==null)scope.remove();else scope.set(previous);}
        };
    }
}
