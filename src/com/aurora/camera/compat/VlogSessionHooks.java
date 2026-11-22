package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import com.aurora.camera.diagnostics.ObservedReplacement;
import java.lang.reflect.Method;

/** F19: aurora's accepted normal-session/OIS route; module entry guards the device. */
public final class VlogSessionHooks {
    private VlogSessionHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method session = host.method("com.xiaomi.microfilm.vlogpro.mode.b",
                "i", host.type("y3.w"));
        final Method eis = host.method("com.xiaomi.microfilm.vlogpro.mode.VlogProModule", "isEisOn");
        if (session.getReturnType() != int.class || eis.getReturnType() != boolean.class)
            throw new NoSuchMethodException("F19 vlog session/EIS return type");
        return new HookPlan() {
            public String id() { return "F19.vlog-aurora-session"; }
            public void install() {
                HookDiagnostics.hookMethod(session, ObservedReplacement.returnConstant(0));
                HookDiagnostics.hookMethod(eis, ObservedReplacement.returnConstant(false));
            }
        };
    }
}
