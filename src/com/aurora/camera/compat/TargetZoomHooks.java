package com.aurora.camera.compat;

import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** F30: retain the accepted stale-J2 compatibility behavior and its known tradeoff. */
public final class TargetZoomHooks {
    private TargetZoomHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method gate = host.method("k6.e", "t0");
        if (gate.getReturnType() != boolean.class)
            throw new NoSuchMethodException("F30 target-zoom gate return type");
        return new HookPlan() {
            public String id() { return "F30.target-zoom-stale-gate"; }
            public void install() {
                XposedBridge.hookMethod(gate, XC_MethodReplacement.returnConstant(false));
            }
        };
    }
}
