package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import java.lang.reflect.Method;

final class SlowMotionBatteryHooks {
    private static final String SCOPE = "slow-motion-battery-scope";

    private SlowMotionBatteryHooks() {}

    static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Method checkHighSpeedRecording = host.method("X9.R0", "m7");
        Method batteryAtMost = host.method("com.android.camera.b", "a", int.class);
        ThreadLocal<Boolean> bypassBattery = new ThreadLocal<>();

        return new HookPlan() {
            @Override public String id() { return "slow-motion-low-battery"; }

            @Override public void install() {
                HookDiagnostics.hookMethod(checkHighSpeedRecording, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        p.setObjectExtra(SCOPE, bypassBattery.get());
                        bypassBattery.set(true);
                    }

                    @Override protected void after(MethodHookParam p) {
                        Boolean previous = (Boolean) p.getObjectExtra(SCOPE);
                        if (previous == null) bypassBattery.remove();
                        else bypassBattery.set(previous);
                    }
                });
                // Leave the host's temperature checks and warning in the same call.
                HookDiagnostics.hookMethod(batteryAtMost, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        if (Boolean.TRUE.equals(bypassBattery.get()) && (Integer) p.args[0] == 20) {
                            p.setResult(false);
                        }
                    }
                });
            }
        };
    }
}
