package com.aurora.camera.compat.lens;

import java.lang.reflect.Method;

/** Scope an inserted call to its actual native caller, not nested consumers. */
final class NativeCaller {
    private final ClassLoader loader;
    NativeCaller(ClassLoader loader) { this.loader=loader; }

    boolean matches(Method target,Method caller) {
        for (StackTraceElement frame:Thread.currentThread().getStackTrace()) {
            String name=frame.getClassName();
            if (name.startsWith("com.aurora.camera.compat.") || name.startsWith("de.robv.android.xposed.")) continue;
            if (name.equals(target.getDeclaringClass().getName()) && frame.getMethodName().equals(target.getName())) continue;
            Class<?> type;
            try { type=Class.forName(name,false,loader); }
            catch (ClassNotFoundException otherLoaderFrame) { continue; }
            if (type.getClassLoader()!=loader) continue;
            return type==caller.getDeclaringClass() && frame.getMethodName().equals(caller.getName());
        }
        return false;
    }
}
