package com.aurora.camera.compat.street;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.SparseArray;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Entry selection and the native alpha-end exit transaction have distinct scopes. */
public final class StreetMode218 {
    private final Method global,support,start,lookup,registry,alphaEnd,onMode,onNewIntent,target,data,getMode,setMode,activeConfig,activeModule,moduleIndex;
    private final java.lang.reflect.Field intentType;
    private final Constructor<?> equipment;
    private final ThreadLocal<Boolean> exiting=new ThreadLocal<>();
    private volatile Shortcut shortcut;

    private static final class Shortcut {
        final WeakReference<Activity> camera;
        final int style;
        final Thread parserThread = Thread.currentThread();
        volatile boolean selected;
        Object entry; // Access under the native registry monitor.

        Shortcut(Activity owner, int style) {
            camera = new WeakReference<>(owner);
            this.style = style;
        }
    }
    public StreetMode218(Host h)throws ReflectiveOperationException {
        global=h.method("com.xiaomi.camera.basic.Global","getContext");
        support=h.method("com.android.camera.features.mode.equipstreet.EquipStreetModuleEntry","support");
        start=h.method("com.android.camera.module.loader.base.StartControl","create",int.class);
        lookup=h.method("t3.a","c",int.class);registry=h.method("t3.a","b");
        equipment=h.type("com.android.camera.features.mode.equipstreet.EquipStreetModuleEntry").getDeclaredConstructor(Context.class);equipment.setAccessible(true);
        alphaEnd=h.method("q4.h","As",h.type("q4.h"),boolean.class,int.class);
        onMode=h.method("com.android.camera.Camera","i8",start.getReturnType());
        onNewIntent=h.method("com.android.camera.Camera","onNewIntent",Intent.class);
        target=h.method(start.getReturnType(),"getTargetMode");data=h.method("g2.a","g");
        getMode=h.method("u2.Y","D",int.class);setMode=h.method("u2.Y","b0",int.class);intentType=h.field("u2.Y","u");
        activeConfig=h.method("R6.D","a");activeModule=h.method("r6.W","ja");moduleIndex=h.method("com.android.camera.module.a0","getModuleIndex");
    }
    private SharedPreferences prefs()throws ReflectiveOperationException {
        Context c=(Context)global.invoke(null);return c==null?null:c.getSharedPreferences("aurora_street_ui217",0);
    }
    private int selection()throws ReflectiveOperationException {SharedPreferences p=prefs();return p==null?0:p.getInt("pref_aurora_street_ui217",0);}

    public int selection(Activity owner) throws ReflectiveOperationException {
        Shortcut current = activeShortcut();
        if (current != null && current.selected && current.camera.get() == owner) return current.style;
        return selection();
    }

    private Shortcut activeShortcut() {
        Shortcut current = shortcut;
        if (current == null) return null;
        Activity owner = current.camera.get();
        return owner == null || owner.isFinishing() || owner.isDestroyed() ? null : current;
    }

    private Shortcut availableShortcut() {
        Shortcut current = activeShortcut();
        return current != null && (current.selected || current.parserThread == Thread.currentThread())
                ? current : null;
    }

    public synchronized Consumer<Boolean> prepareShortcut(Activity owner, Integer style,
            HookDiagnostics.Operation operation) {
        if (!onMode.getDeclaringClass().isInstance(owner) || style == null) return null;
        if (style == 0) {
            clearShortcut(owner);
            return null;
        }
        if ((style != 1 && style != 2) || owner.isFinishing() || owner.isDestroyed()) return null;
        Shortcut request = new Shortcut(owner, style);
        shortcut = request;
        return selected -> finishShortcut(request, Boolean.TRUE.equals(selected), operation);
    }

    private synchronized void finishShortcut(Shortcut request, boolean selected,
            HookDiagnostics.Operation operation) {
        if (shortcut != request) return;
        if (selected && activeShortcut() == request) {
            request.selected = true;
            operation.ran();
        } else {
            shortcut = null;
            if (!selected) operation.failed(new IllegalStateException("街拍快捷入口未选中 229"));
        }
    }

    private synchronized void clearShortcut(Activity owner) {
        if (shortcut != null && shortcut.camera.get() == owner) shortcut = null;
    }

    private boolean nativeStreetActive()throws ReflectiveOperationException {
        java.util.Optional<?> config=(java.util.Optional<?>)activeConfig.invoke(null);if(!config.isPresent())return false;
        java.util.Optional<?> module=(java.util.Optional<?>)activeModule.invoke(config.get());
        return module.isPresent()&&(Integer)moduleIndex.invoke(module.get())==225;
    }
    public void install(){
        HookDiagnostics.hookMethod(support,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(selection()>0)p.setResult(true);}});
        HookDiagnostics.hookMethod(start,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            if(Boolean.TRUE.equals(exiting.get()))return;
            if ((Integer)p.args[0] == 229 && availableShortcut() != null) return;
            // A ratio/lens change rebuilds the current native street module.
            // Preserve that actual UI after X instead of reapplying entry style.
            if((Integer)p.args[0]==225&&nativeStreetActive())return;
            SharedPreferences preferences=prefs();if(preferences!=null)p.args[0]=StreetSelection217.route((Integer)p.args[0],preferences.getInt("pref_aurora_street_ui217",0),preferences.contains("pref_aurora_street_ui217"));
        }});
        HookDiagnostics.hookMethod(onNewIntent,new StrictHook(){
            @Override protected void before(MethodHookParam p){clearShortcut((Activity)p.thisObject);}
        });
        HookDiagnostics.hookMethod(onMode,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                Activity owner=(Activity)p.thisObject;
                if(owner.isFinishing()||owner.isDestroyed()||(Integer)target.invoke(p.args[0])!=229){
                    clearShortcut(owner);
                    return;
                }
                if(selection(owner)==0)return;
                Object state=data.invoke(null);int actual=(Integer)getMode.invoke(state,intentType.getInt(state));
                // Commit the actual module for the native exit gate and filter layout.
                if(actual!=229)setMode.invoke(state,229);
            }
            @Override protected void after(MethodHookParam p){
                if(p.hasThrowable())clearShortcut((Activity)p.thisObject);
            }
        });
        HookDiagnostics.hookMethod(lookup,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||(Integer)p.args[0]!=229||p.getResult()!=null)return;
            Shortcut current = availableShortcut();
            if(current==null&&selection()==0)return;
            synchronized(lookup.getDeclaringClass()){
                if (current != null) {
                    if (current.entry == null) current.entry = equipment.newInstance(global.invoke(null));
                    p.setResult(current.entry);
                    return;
                }
                @SuppressWarnings("unchecked") SparseArray<Object> entries=(SparseArray<Object>)registry.invoke(null);
                Object entry=entries.get(229);if(entry==null){entry=equipment.newInstance(global.invoke(null));entries.put(229,entry);}p.setResult(entry);
            }
        }});
        // This callback is invoked by the native animation listener. Both its
        // persistent 225 write and StartControl(225) belong to the same exit.
        HookDiagnostics.hookMethod(alphaEnd,new StrictHook(){
            @Override protected void before(MethodHookParam p){p.setObjectExtra("218.street.exit",exiting.get());exiting.set(!(Boolean)p.args[1]);}
            @Override protected void after(MethodHookParam p){Boolean previous=(Boolean)p.getObjectExtra("218.street.exit");if(previous==null)exiting.remove();else exiting.set(previous);}
        });
    }
}
