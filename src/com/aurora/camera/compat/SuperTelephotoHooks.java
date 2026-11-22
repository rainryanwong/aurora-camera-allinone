package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.content.SharedPreferences;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Opt-in SDSR controls shared by native menu, detection and capture requests. */
public final class SuperTelephotoHooks {
    public static final String TOGGLE_OPTIMIZATION="aurora_sdsr_three_states",ZOOM10="aurora_sdsr_zoom20";
    private SuperTelephotoHooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        Options229 options=new Options229(h);
        Class<?> capabilities=h.type("l9.g");
        Method supported=h.method("l9.h","P3",capabilities),force=h.method("l9.h","Q3",capabilities),threshold=h.method(capabilities,"s0");
        // R's raw DEX prototype is (Ljava/lang/Object;)V; the decompiler
        // displays its cast to the configuration type instead.
        Method initialize=h.method("r2.V","R",Object.class);
        Method detector=h.method("s6.p0","initAndGetPriorCondition");
        Class<?> configType=h.type("r2.V");
        Class<?> componentType=h.type("com.android.camera.data.data.c");
        Method defaultValue=h.method(configType,"getDefaultValue",int.class);
        Method getValue=h.method(componentType,"getComponentValue",int.class);
        Method setValue=h.method(componentType,"setComponentValue",int.class,String.class);
        Field items=h.field(componentType.getName(),"mItems");
        Field itemValue=h.field("com.android.camera.data.data.d","r");
        Field sdsrSupported=h.field(configType.getName(),"a");
        return new HookPlan(){
            final Map<Object,Object> configurations=Collections.synchronizedMap(new WeakHashMap<>());
            final Map<Object,Boolean> detectors=Collections.synchronizedMap(new WeakHashMap<>());
            SharedPreferences preferences;
            final SharedPreferences.OnSharedPreferenceChangeListener changed=(prefs,key)->{
                if(!TOGGLE_OPTIMIZATION.equals(key)&&!ZOOM10.equals(key))return;
                try{
                    Map<Object,Object> current; synchronized(configurations){current=new java.util.HashMap<>(configurations);}
                    for(Map.Entry<Object,Object> entry:current.entrySet())initialize.invoke(entry.getKey(),entry.getValue());
                    Object[] active; synchronized(detectors){active=detectors.keySet().toArray();}
                    for(Object owner:active)detector.invoke(owner);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot refresh super telephoto settings",failure);}
            };
            public String id(){return "super-telephoto-options";}
            synchronized void observe(){
                if(preferences!=null)return;
                preferences=options.prefs();preferences.registerOnSharedPreferenceChangeListener(changed);
            }
            public void install(){
                HookDiagnostics.hookMethod(force,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(!p.hasThrowable()&&options.enabled(TOGGLE_OPTIMIZATION)&&(Boolean)supported.invoke(null,p.args[0]))p.setResult(true);
                }});
                HookDiagnostics.hookMethod(defaultValue,new StrictHook(){
                    @Override protected void after(MethodHookParam p) {
                        if(!p.hasThrowable()&&options.enabled(TOGGLE_OPTIMIZATION))p.setResult("off");
                    }
                });
                HookDiagnostics.hookMethod(threshold,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(!p.hasThrowable()&&options.enabled(ZOOM10)&&(Boolean)supported.invoke(null,p.thisObject))p.setResult(10);
                }});
                HookDiagnostics.hookMethod(initialize,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(p.hasThrowable())return;
                    observe();configurations.put(p.thisObject,p.args[0]);
                    if(!options.enabled(TOGGLE_OPTIMIZATION)||!sdsrSupported.getBoolean(p.thisObject))return;
                    List<?> stockItems=(List<?>)items.get(p.thisObject);
                    List<Object> toggleItems=new ArrayList<>(2);
                    for(Object item:stockItems){
                        if(!"auto".equals(itemValue.get(item)))toggleItems.add(item);
                    }
                    if("auto".equals(getValue.invoke(p.thisObject,163)))setValue.invoke(p.thisObject,163,"off");
                    items.set(p.thisObject,Collections.unmodifiableList(toggleItems));
                }});
                HookDiagnostics.hookMethod(detector,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())detectors.put(p.thisObject,true);}});
            }
        };
    }
}
