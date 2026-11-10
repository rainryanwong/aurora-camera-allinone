package com.aurora.camera.compat;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.InvocationTargetException;

/** Transfer callback failures through Xposed's result instead of its swallow path. */
public abstract class StrictHook extends XC_MethodHook {
    protected StrictHook(){super();}
    protected StrictHook(int priority){super(priority);}
    protected void before(MethodHookParam param) throws Throwable {}
    protected void after(MethodHookParam param) throws Throwable {}

    @Override protected final void beforeHookedMethod(MethodHookParam param) {
        try { before(param); } catch (Throwable failure) { failed(param,"before",failure); }
    }

    @Override protected final void afterHookedMethod(MethodHookParam param) {
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
