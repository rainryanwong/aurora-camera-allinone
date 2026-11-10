package com.aurora.camera.compat.cinematic;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Accepted SmartDolly capability, request, session and ownership policy. */
public final class SmartDolly {
    public static final String ENABLE = "com.xiaomi.cinematicIntellTruck.Enable";
    public static final String TOUCH = "com.xiaomi.cinematicIntellTruck.TouchROI";
    public interface Runtime {
        Object componentCapabilities();
        String choice();
        boolean requestKey(Object capabilities,String name);
        boolean featureKey(Object capabilities);
        List<String> resultKeys(Object capabilities);
        String resultName();
        int cameraId(Object capabilities);
        void logGate(int cameraId,boolean registered);
        boolean alive(Object module);
        int moduleIndex(Object module);
        boolean current(Object module);
        int activeMode();
        Object capabilities(Object manager);
        void enableRequest(Object builder,boolean enabled);
        void enableSession(Object manager,boolean enabled);
        void preference(String key,boolean value);
    }
    private final Runtime runtime;
    private final Map<Object,Boolean> resultCache = Collections.synchronizedMap(new WeakHashMap<Object,Boolean>());

    public SmartDolly(Runtime runtime) { this.runtime=runtime; }

    private boolean registeredResult(Object capabilities) {
        Boolean cached=resultCache.get(capabilities);
        if(cached!=null)return cached;
        boolean registered=false;
        List<String> keys=runtime.resultKeys(capabilities);
        if(keys!=null){
            String expected=runtime.resultName();
            for(String name:keys)if(expected.equals(name)){registered=true;break;}
            if(resultCache.put(capabilities,registered)==null)
                runtime.logGate(runtime.cameraId(capabilities),registered);
        }
        return registered;
    }
    public boolean supported(Object capabilities) {
        return capabilities!=null && runtime.requestKey(capabilities,ENABLE)
                && runtime.requestKey(capabilities,TOUCH) && runtime.featureKey(capabilities)
                && registeredResult(capabilities);
    }
    public boolean selected() {
        return supported(runtime.componentCapabilities()) && "1".equals(runtime.choice());
    }
    public boolean active(Object module) {
        return module!=null && runtime.alive(module) && runtime.moduleIndex(module)==227
                && selected() && runtime.current(module);
    }
    public void apply(int mode,Object builder,Object capabilities) {
        if(mode==227 && builder!=null && supported(capabilities))
            runtime.enableRequest(builder,selected());
    }
    public void preview(Object builder,Object capabilities) {
        apply(runtime.activeMode(),builder,capabilities);
    }
    public void session(Object manager) {
        if(supported(runtime.capabilities(manager)))runtime.enableSession(manager,selected());
    }
    public void reset() {
        runtime.preference("pref_cinematic_intell_dolly_in_anime",false);
        runtime.preference("pref_cinematic_intell_dolly_is_double_click",false);
    }
}
