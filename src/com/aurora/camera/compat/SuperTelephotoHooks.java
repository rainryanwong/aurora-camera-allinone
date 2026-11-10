package com.aurora.camera.compat;

import android.content.SharedPreferences;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Opt-in SDSR controls shared by native menu, detection and capture requests. */
public final class SuperTelephotoHooks {
    public static final String THREE_STATES="aurora_sdsr_three_states",ZOOM10="aurora_sdsr_zoom20",EXTENDED="aurora_sdsr_tele3";
    private SuperTelephotoHooks(){}
    static void mergeLensOptions(SharedPreferences preferences){
        if(!preferences.contains("aurora_sdsr_wide"))return;
        boolean enabled=preferences.getBoolean(EXTENDED,false)||preferences.getBoolean("aurora_sdsr_wide",false);
        if(!preferences.edit().putBoolean(EXTENDED,enabled).remove("aurora_sdsr_wide").commit())
            throw new IllegalStateException("Cannot merge SDSR lens options");
    }
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        Options229 options=new Options229(h);
        Class<?> capabilities=h.type("l9.g");
        Method supported=h.method("l9.h","P3",capabilities),force=h.method("l9.h","Q3",capabilities),threshold=h.method(capabilities,"s0");
        // R's raw DEX prototype is (Ljava/lang/Object;)V; the decompiler
        // displays its cast to the configuration type instead.
        Method initialize=h.method("r2.V","R",Object.class);
        Method detector=h.method("s6.p0","initAndGetPriorCondition");
        Method accept=h.method("s6.p0","acceptResult"),capture=h.method("com.android.camera.features.mode.capture.CaptureModule","sdsrCaptureRequired");
        Method running=h.method("g2.a","j"),configuration=h.method("g2.a","a"),get=h.method("Xh.b","x",Class.class);
        Class<?> masterType=h.type("v2.k"),configType=h.type("r2.V");
        Field master=h.field(masterType,"r"),detectorModule=h.field("com.android.camera.module.interceptor.base.c","module"),detectorThreshold=h.field("s6.p0","c");
        Field captureSupported=h.field("com.android.camera.features.mode.capture.CaptureModule","mSdsrCaptureSupported");
        Method mode=h.method("com.android.camera.module.u","getModuleIndex"),zoom=h.method("com.android.camera.features.mode.capture.CaptureModule","getZoomManager"),ratio=h.method("i9.e","e1"),on=h.method(configType,"n",int.class);
        return new HookPlan(){
            final Map<Object,Object> configurations=Collections.synchronizedMap(new WeakHashMap<>());
            final Map<Object,Boolean> detectors=Collections.synchronizedMap(new WeakHashMap<>());
            SharedPreferences preferences;
            final SharedPreferences.OnSharedPreferenceChangeListener changed=(prefs,key)->{
                if(!THREE_STATES.equals(key)&&!ZOOM10.equals(key))return;
                try{
                    Map<Object,Object> current; synchronized(configurations){current=new java.util.HashMap<>(configurations);}
                    for(Map.Entry<Object,Object> entry:current.entrySet())initialize.invoke(entry.getKey(),entry.getValue());
                    Object[] active; synchronized(detectors){active=detectors.keySet().toArray();}
                    for(Object owner:active)detector.invoke(owner);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot refresh super telephoto settings",failure);}
            };
            public String id(){return "super-telephoto-options";}
            int enabledLens(Object module)throws ReflectiveOperationException {
                if(!options.enabled(EXTENDED)||(Integer)mode.invoke(module)!=163)return 0;
                Object current=get.invoke(running.invoke(null),masterType);
                if(current==null)return 0;
                int lens=master.getInt(current);
                if(lens==2&&(Float)ratio.invoke(zoom.invoke(module))>=1.0f)return 2;
                if(lens==3&&(Float)ratio.invoke(zoom.invoke(module))>=3.2f)return 3;
                return 0;
            }
            synchronized void observe(){
                if(preferences!=null)return;
                preferences=options.prefs();mergeLensOptions(preferences);preferences.registerOnSharedPreferenceChangeListener(changed);
            }
            public void install(){
                XposedBridge.hookMethod(force,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(!p.hasThrowable()&&options.enabled(THREE_STATES)&&(Boolean)supported.invoke(null,p.args[0]))p.setResult(true);
                }});
                XposedBridge.hookMethod(threshold,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(!p.hasThrowable()&&options.enabled(ZOOM10)&&(Boolean)supported.invoke(null,p.thisObject))p.setResult(10);
                }});
                XposedBridge.hookMethod(initialize,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(p.hasThrowable())return;
                    observe();configurations.put(p.thisObject,p.args[0]);
                }});
                XposedBridge.hookMethod(detector,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())detectors.put(p.thisObject,true);}});
                XposedBridge.hookMethod(accept,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable {
                        int lens=enabledLens(detectorModule.get(p.thisObject));
                        if(lens==0)return;
                        int previous=detectorThreshold.getInt(p.thisObject);
                        int minimum=lens==2?1:3;
                        if(previous<=minimum)return;
                        // The host stores an int threshold. enabledLens checks the
                        // exact float minimum before this native-call-only change.
                        p.setObjectExtra("sdsr.previousThreshold",previous);detectorThreshold.setInt(p.thisObject,minimum);
                    }
                    @Override protected void after(MethodHookParam p)throws Throwable {
                        Integer previous=(Integer)p.getObjectExtra("sdsr.previousThreshold");
                        if(previous!=null)detectorThreshold.setInt(p.thisObject,previous);
                    }
                });
                XposedBridge.hookMethod(capture,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(p.hasThrowable()||(Boolean)p.getResult()||!captureSupported.getBoolean(p.thisObject))return;
                    int lens=enabledLens(p.thisObject);
                    if(lens==0)return;
                    Object current=get.invoke(configuration.invoke(null),configType);
                    if(current!=null&&(Boolean)on.invoke(current,(Integer)mode.invoke(p.thisObject))){
                        XposedBridge.log("Compat SDSR "+(lens==2?"wide":"tele3")+" capture eligible, zoom="+ratio.invoke(zoom.invoke(p.thisObject)));
                        p.setResult(true);
                    }
                }});
            }
        };
    }
}
