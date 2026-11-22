package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Method;

final class RetiredLaboratoryOptions {
    private RetiredLaboratoryOptions() {}

    static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Method attach = host.method("com.android.camera.CameraAppImpl", "attachBaseContext", Context.class);
        return new HookPlan() {
            @Override public String id() { return "retired-laboratory-options"; }

            @Override public void install() {
                HookDiagnostics.hookMethod(attach, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (param.hasThrowable()) return;
                        Context context = (Context) param.thisObject;
                        SharedPreferences prefs = context.getSharedPreferences(
                                Options229.PREFERENCES, Context.MODE_PRIVATE);
                        String extended = "aurora_sdsr_tele3";
                        String legacy = "aurora_sdsr_wide";
                        if (!prefs.getBoolean(extended, false) && !prefs.getBoolean(OisReinitHooks.KEY, false)
                                && !prefs.contains(legacy)) return;
                        if (!prefs.edit().putBoolean(extended, false).putBoolean(OisReinitHooks.KEY, false)
                                .remove(legacy).commit()) {
                            throw new IllegalStateException("Cannot turn off retired laboratory options");
                        }
                    }
                });
            }
        };
    }
}
