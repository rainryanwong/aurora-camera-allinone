package com.aurora.camera.compat.lens;

import com.aurora.camera.diagnostics.ObservedHook;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.InvocationTargetException;

/** Xposed normally swallows callback exceptions; preserve failed native calls. */
abstract class LensHook extends ObservedHook {
    protected void before(MethodHookParam p) throws Throwable {}
    protected void after(MethodHookParam p) throws Throwable {}

    @Override protected final void runBefore(MethodHookParam p) {
        try { before(p); } catch (Throwable failure) { fail(p,failure); }
    }
    @Override protected final void runAfter(MethodHookParam p) {
        try { after(p); } catch (Throwable failure) { fail(p,failure); }
    }
    private static void fail(MethodHookParam p,Throwable failure) {
        while (failure instanceof InvocationTargetException && failure.getCause()!=null) failure=failure.getCause();
        XposedBridge.log("AuroraCameraCompat lens callback failed "+p.method);
        XposedBridge.log(failure);
        p.setThrowable(failure);
    }
}
