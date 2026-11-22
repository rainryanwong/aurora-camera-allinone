package com.aurora.camera.compat;

import android.util.Log;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Method;

/** Preserve the photo setting across the camera's session reset. */
final class MotionCaptureRetainHooks {
    private static final String KEY = "pref_motion_capture_status";
    private static final String RESET_STORE = "motion-capture.reset-store";
    private static final String TAG = "AuroraMotionCaptureRetain";

    private MotionCaptureRetainHooks() {}

    static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Class<?> configuration = host.type("r2.n1");
        Class<?> preferences = host.type("Xh.a");
        Method reset = host.method("u6.a", "c", configuration, preferences, configuration,
                configuration, host.type("u2.Y"), preferences);
        Method remove = host.method(preferences, "s", String.class);
        ThreadLocal<Object> resetStore = new ThreadLocal<>();

        return new HookPlan() {
            @Override public String id() { return "motion-capture-retain"; }

            @Override public void install() {
                HookDiagnostics.hookMethod(reset, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        param.setObjectExtra(RESET_STORE, resetStore.get());
                        resetStore.set(param.args[1]);
                    }

                    @Override protected void after(MethodHookParam param) {
                        Object previous = param.getObjectExtra(RESET_STORE);
                        if (previous == null) resetStore.remove();
                        else resetStore.set(previous);
                    }
                });
                HookDiagnostics.hookMethod(remove, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if (param.thisObject == resetStore.get() && KEY.equals(param.args[0])) {
                            param.setResult(param.thisObject);
                            Log.d(TAG, "Kept motion capture setting during session reset");
                        }
                    }
                });
            }
        };
    }
}
