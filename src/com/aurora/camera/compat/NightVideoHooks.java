package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Keep the stock night-photo / night-video pair on the 14 Ultra. */
public final class NightVideoHooks {
    private NightVideoHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Method supported = host.method("com.android.camera.features.mode.night.video.NightVideoModuleEntry", "support");
        Field device = host.field("Pe.b$b", "a");
        Field profile = host.field("Pe.b", "e");
        Method nightVideo = host.method(profile.getType(), "S5");
        return new HookPlan() {
            @Override public String id() { return "night-video"; }
            @Override public void install() {
                HookDiagnostics.hookMethod(supported, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws ReflectiveOperationException {
                        // The port enables the night photo entry independently of the donor's t4 flag.
                        // Use its video capability, the same one used to offer the stock switch button.
                        if (!p.hasThrowable() && Boolean.FALSE.equals(p.getResult()))
                            p.setResult(nightVideo.invoke(profile.get(device.get(null))));
                    }
                });
            }
        };
    }
}
