package com.aurora.camera.compat;

import android.content.Context;
import android.content.res.Configuration;
import com.aurora.camera.compat.filter.LutRuntime229;
import com.aurora.camera.features.watermark.WatermarkLabels;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.Locale;

/** Resolve the captured names to the identifiers used by watermark artwork. */
public final class WatermarkNameHooks {
    private WatermarkNameHooks() {}

    public static HookPlan resolve(Host h, LutRuntime229 lut) throws ReflectiveOperationException {
        WatermarkLabels.initialize(h.moduleApkPath);
        Method name = h.method("com.xiaomi.camera.effect.EffectController", "r", Context.class, int.class);
        Method filter = h.method("com.xiaomi.cam.watermark.a", "w0", String.class);
        Method lens = h.method("com.xiaomi.cam.watermark.a", "u0", String.class);
        return new HookPlan() {
            public String id() { return "watermark-capture-names"; }
            public void install() {
                XposedBridge.hookMethod(name, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        String custom = lut.customName((Integer) p.args[1]);
                        if (custom != null) { p.setResult(custom); return; }
                        Context context = (Context) p.args[0];
                        Configuration config = new Configuration(context.getResources().getConfiguration());
                        config.setLocale(Locale.ENGLISH);
                        p.args[0] = context.createConfigurationContext(config);
                    }
                });
                XposedBridge.hookMethod(filter, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        p.args[0] = WatermarkLabels.displayFilter((String) p.args[0]);
                    }
                });
                XposedBridge.hookMethod(lens, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        p.args[0] = WatermarkLabels.lens((String) p.args[0]);
                    }
                });
            }
        };
    }
}
