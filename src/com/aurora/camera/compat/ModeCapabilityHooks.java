package com.aurora.camera.compat;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Exact small entry/consumer patches; the remaining per-feature flows stay separate. */
public final class ModeCapabilityHooks {
    private ModeCapabilityHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method night = host.method(
                "com.android.camera.features.mode.night.photo.NightModuleEntry", "support");
        final Method nightEv = host.method(
                "com.android.camera.features.mode.night.photo.NightModule", "supportEvOverlap");
        final Method evModes = host.method("r2.F0", "x", int.class);
        final Method hiPixel = host.method("r2.e0", "isSupportMode", int.class);
        final Method ai = host.method("com.android.camera.features.mode.ai.AiModuleEntry", "support");
        final Method protection = host.method("com.camera.LSsdQFvLalapDwvA", "mJtBkWqRzXnVcLpY");
        final Method aiSupported = host.method("Pe.b", "C0");
        final Method initEv = host.method("Pe.b", "j2");
        final Field singleton = host.field("Pe.b$b", "a");
        final Method audio = host.method("Pe.b", "C1");
        final Method audioMode = host.method("Pe.b", "y0");
        final Field profile = host.field("Pe.b", "e");
        final Class<?> profileType = profile.getType();
        final Method b3 = host.method(profileType, "B3");
        final Method n2 = host.method(profileType, "n2");
        for (Method method : new Method[] {night, nightEv, evModes, hiPixel, ai,
                aiSupported, audio, audioMode, b3, n2}) {
            if (method.getReturnType() != boolean.class)
                throw new NoSuchMethodException("capability return type: " + method);
        }
        if (initEv.getReturnType() != void.class || protection.getReturnType() != int.class
                || singleton.getType() != host.type("Pe.b"))
            throw new NoSuchMethodException("capability singleton/initialization contract");
        return new HookPlan() {
            public String id() { return "F04.F07.F09.F11.F17.capability-consumers"; }
            public void install() {
                XposedBridge.hookMethod(night, XC_MethodReplacement.returnConstant(true));
                XposedBridge.hookMethod(nightEv, new StrictHook() {
                    @Override protected void after(MethodHookParam p) {
                        // The original Pe.b.j2 initialization and errors still run.
                        if (!p.hasThrowable()) p.setResult(true);
                    }
                });
                XposedBridge.hookMethod(evModes, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        if ((Integer) p.args[0] == 173) {
                            try {
                                initEv.invoke(singleton.get(null));
                                p.setResult(true);
                            } catch (InvocationTargetException failure) {
                                p.setThrowable(failure.getCause());
                            }
                        }
                    }
                });
                XposedBridge.hookMethod(hiPixel, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        if ((Integer) p.args[0] == 168) p.setResult(true);
                    }
                });
                XposedBridge.hookMethod(ai, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                            // The accepted patch drops only the parity branch, not the protection call.
                            protection.invoke(null);
                            return aiSupported.invoke(singleton.get(null));
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    }
                });
                XposedBridge.hookMethod(audio, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                            if (!(Boolean) audioMode.invoke(p.thisObject)) return false;
                            Object capabilities = profile.get(p.thisObject);
                            b3.invoke(capabilities); // Original query remains; only its exclusion was removed.
                            return n2.invoke(capabilities);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                    }
                });
            }
        };
    }
}
