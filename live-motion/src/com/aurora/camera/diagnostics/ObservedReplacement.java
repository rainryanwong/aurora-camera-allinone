package com.aurora.camera.diagnostics;

import de.robv.android.xposed.XposedBridge;

public abstract class ObservedReplacement extends ObservedHook {
    @Override protected final void runBefore(MethodHookParam param) {
        try {
            param.setResult(replaceHookedMethod(param));
        } catch (Throwable error) {
            XposedBridge.log(error);
            param.setThrowable(error);
        }
    }

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    public static ObservedReplacement returnConstant(Object value) {
        return new ObservedReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam param) {
                return value;
            }
        };
    }
}
