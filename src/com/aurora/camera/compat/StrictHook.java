package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.ObservedHook;

import java.lang.reflect.InvocationTargetException;

/** Transfer callback failures through Xposed's result instead of its swallow path. */
public abstract class StrictHook extends ObservedHook {
    protected StrictHook(){super();}
    protected StrictHook(int priority){super(priority);}
    protected void before(MethodHookParam param) throws Throwable {}
    protected void after(MethodHookParam param) throws Throwable {}

    @Override protected final void runBefore(MethodHookParam param) {
        try { before(param); } catch (Throwable failure) { failed(param,"before",failure); }
    }

    @Override protected final void runAfter(MethodHookParam param) {
        try { after(param); } catch (Throwable failure) { failed(param,"after",failure); }
    }

    private void failed(MethodHookParam param,String phase,Throwable failure) {
        while (failure instanceof InvocationTargetException && failure.getCause()!=null)
            failure=failure.getCause();
        System.err.println("AuroraCameraCompat callback failure "+getClass().getName()+"."+phase);
        failure.printStackTrace(System.err);
        param.setThrowable(failure);
    }
}
