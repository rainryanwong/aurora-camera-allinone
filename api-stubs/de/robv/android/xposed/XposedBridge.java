package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Compile-only API declaration; excluded from the module DEX. */
public final class XposedBridge {
    public static final int PRIORITY_DEFAULT = 50;
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) {
        return null;
    }
    public static Object invokeOriginalMethod(Member method, Object receiver, Object[] args)
            throws Throwable {
        return null;
    }
    public static void log(String message) {}
    public static void log(Throwable throwable) {}
}
