package com.aurora.camera.compat.filter;

import java.util.concurrent.Executor;

/** Coalesces real events on one worker; no timer, polling, or retry after an unchanged failure. */
public final class LutSync246 {
    public interface Work {void run(long revision)throws Exception;}
    public interface Failure {void failed(long revision,Exception failure);}
    private final Executor executor;private final Work work;private final Failure failure;
    private long revision;private boolean ready,running;
    public LutSync246(Executor executor,Work work,Failure failure){this.executor=executor;this.work=work;this.failure=failure;}
    public synchronized void request(){revision++;schedule();}
    public synchronized void ready(){ready=true;revision++;schedule();}
    public synchronized boolean current(long value){return ready&&revision==value;}
    private void schedule(){
        if(!ready||running)return;running=true;executor.execute(this::drain);
    }
    private void drain(){
        for(;;){
            final long target;synchronized(this){target=revision;}
            try{work.run(target);}catch(Exception error){failure.failed(target,error);}
            synchronized(this){if(target==revision){running=false;return;}}
        }
    }
}
