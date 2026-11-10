package com.aurora.camera.compat.classic;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Own only the software-CV state introduced for an explicitly selected model. */
public final class ClassicCv240 {
    public interface NativeState {
        Object effect();
        int current(Object effect);
        int none();
        int common();
        void write(Object effect,int value);
        void log(String text);
    }
    private static final class Owned {
        final Object effect;
        final int original,forced;
        Owned(Object effect,int original,int forced){this.effect=effect;this.original=original;this.forced=forced;}
    }
    private static final class Write {
        final Object effect;
        final int value;
        final long nativeEpoch;
        final Owned claim;
        Write(Object effect,int value,long nativeEpoch,Owned claim){this.effect=effect;this.value=value;this.nativeEpoch=nativeEpoch;this.claim=claim;}
    }
    private final NativeState state;
    private final AtomicReference<Owned> owned=new AtomicReference<>();
    private final AtomicLong nativeEpoch=new AtomicLong();
    private final ThreadLocal<Write> writing=new ThreadLocal<>();
    public ClassicCv240(NativeState state){this.state=state;}

    /** Called after the actual native setter. It never changes native arguments. */
    public void nativeWriteCompleted(Object effect,int value){
        Write own=writing.get();
        if(own!=null&&own.effect==effect&&own.value==value){
            if(own.claim!=null&&nativeEpoch.get()==own.nativeEpoch&&state.current(effect)==value)owned.set(own.claim);
            return;
        }
        nativeEpoch.incrementAndGet();
        owned.set(null);
    }

    public void sync(String model,int mode,int cvType,String reason){
        boolean eligible=!"native".equals(model)&&mode==163&&cvType==1;
        Object effect=state.effect();
        int current=state.current(effect),none=state.none();
        if(eligible&&current==none){
            int common=state.common();
            if(common==none)throw new IllegalStateException("Native classic common mapping is CV NONE");
            Owned claim=new Owned(effect,none,common);
            write(effect,common,claim);
            state.log("cv_sync reason="+reason+" model="+model+" mode="+mode+" cvType="+cvType+" none="+none+" common="+common+" previous="+current+" current="+state.current(effect)+" owned="+(owned.get()==claim));
            return;
        }
        if(!eligible){
            Owned previous=owned.getAndSet(null);
            if(previous!=null&&previous.effect==effect&&current==previous.forced){
                write(effect,previous.original,null);
                state.log("cv_restore reason="+reason+" model="+model+" mode="+mode+" cvType="+cvType+" none="+none+" common="+previous.forced+" previous="+current+" current="+state.current(effect));
            }
        }
    }
    private void write(Object effect,int value,Owned claim){
        Write previous=writing.get();
        writing.set(new Write(effect,value,nativeEpoch.get(),claim));
        try{state.write(effect,value);}
        finally{if(previous==null)writing.remove();else writing.set(previous);}
    }
}
