package com.aurora.camera.compat.softlight;

import java.util.HashMap;
import java.util.Map;

/** Phoenix's per-mode, session-only selection; filter changes relinquish overrides. */
public final class SoftLightState245 {
    private static final class Selection { int filter,soft,override=-1; boolean initialized; }
    private final Map<Integer,Selection> modes=new HashMap<>();
    private boolean enabled;
    public static boolean supports(int mode){return mode==163||mode==171||mode==167||mode==225||mode==229;}
    public static boolean mist(int soft){return soft==1||soft==2;}
    private Selection selection(int mode){return modes.computeIfAbsent(mode,k->new Selection());}
    public synchronized boolean enabled(){return enabled;}
    public synchronized void enabled(boolean value){
        if(enabled==value)return;enabled=value;
        if(!value)for(Selection s:modes.values())s.override=-1;
    }
    public synchronized boolean accept(int mode,int filter,int nativeSoft){
        Selection s=selection(mode);
        if(s.initialized&&s.filter==filter&&s.soft==nativeSoft)return false;
        s.initialized=true;s.filter=filter;s.soft=nativeSoft;s.override=-1;return true;
    }
    public synchronized int effective(int mode){Selection s=selection(mode);return enabled&&s.override>=0?s.override:s.soft;}
    /** A manual choice belongs only to the native filter snapshot which the user adjusted. */
    public synchronized int override(int mode,int filter){
        Selection s=modes.get(mode);
        return enabled&&supports(mode)&&s!=null&&s.initialized&&s.filter==filter?s.override:-1;
    }
    public synchronized void select(int mode,int soft){
        if(!enabled||!supports(mode))throw new IllegalStateException("Soft-light selection outside an enabled photo mode");
        if(soft<0||soft>2)throw new IllegalArgumentException("Soft-light choice: "+soft);
        selection(mode).override=soft;
    }
}
