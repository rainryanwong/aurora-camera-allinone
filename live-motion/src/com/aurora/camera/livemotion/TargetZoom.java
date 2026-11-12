package com.aurora.camera.livemotion;

import android.hardware.camera2.CaptureRequest;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Keeps the HAL target on the native per-frame zoom, rather than the animation endpoint. */
final class TargetZoom {
    private final Method type, cameraManager, zoomManager, configs, transform;
    private final Method apply, writeTag, supports;
    private final Field config, deviceZoom, userZoom, shotCamera, shotConfig;
    private final Method[] stillApply = new Method[2];
    private final Field[] stillShot = new Field[2], stillBuilder = new Field[2];
    private final Object targetTag;
    private final String targetName;
    private volatile Scope active;

    TargetZoom(ClassLoader cl, Class<?> master, Method type) throws ReflectiveOperationException {
        this.type = type;
        cameraManager = master.getMethod("getCameraManager");
        zoomManager = master.getMethod("getZoomManager");
        configs = Class.forName("k6.k", false, cl).getMethod("K0");
        config = LiveMotionModule.field(Class.forName("l9.i0", false, cl), "a");
        Class<?> values = Class.forName("l9.j0", false, cl);
        Class<?> caps = Class.forName("l9.g", false, cl);
        deviceZoom = LiveMotionModule.field(values, "d0");
        userZoom = LiveMotionModule.field(values, "K2");
        transform = Class.forName("W6.d", false, cl).getMethod("lh", float.class);
        apply = Class.forName("l9.o0", false, cl).getDeclaredMethod("U0",
                CaptureRequest.Builder.class, caps, values);
        Class<?> tag = Class.forName("ia.D0", false, cl);
        targetTag = LiveMotionModule.field(Class.forName("ia.A0", true, cl), "F2").get(null);
        targetName = (String) tag.getMethod("b").invoke(targetTag);
        writeTag = Class.forName("ia.E0", false, cl).getDeclaredMethod("d",
                CaptureRequest.Builder.class, tag, Object.class, boolean.class);
        supports = caps.getMethod("S0", String.class);
        shotCamera = LiveMotionModule.field(Class.forName("l9.L0", false, cl), "b");
        shotConfig = LiveMotionModule.field(Class.forName("l9.y0", false, cl), "E");
        String[] consumers = { "l9.G0", "l9.R0" };
        for (int i = 0; i < consumers.length; i++) {
            Class<?> consumer = Class.forName(consumers[i], false, cl);
            stillApply[i] = consumer.getDeclaredMethod("accept", Object.class);
            stillShot[i] = LiveMotionModule.field(consumer, "a");
            stillBuilder[i] = LiveMotionModule.field(consumer, "b");
        }
    }

    void activate(Object module) { active = new Scope(module); }
    void deactivate(Object module) {
        Scope scope = active;
        if (scope != null && scope.module == module) active = null;
    }

    void install() {
        XposedBridge.hookMethod(apply, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                Scope scope = selected();
                if (scope == null || p.hasThrowable() || p.args[0] == null || p.args[1] == null) return;
                Object current = currentConfig(scope);
                if (p.args[2] != current || !(Boolean) supports.invoke(p.args[1], targetName)) return;
                // Native g0(lh(user)) updates d0 and applies this request before K2 is updated.
                float ratio = deviceZoom.getFloat(current);
                writeTag.invoke(null, p.args[0], targetTag, Float.valueOf(ratio), false);
                if (Float.isNaN(scope.lastLogged) || Math.abs(ratio - scope.lastLogged) >= 0.2f) {
                    scope.lastLogged = ratio;
                    LiveMotionModule.log("target request config=" + System.identityHashCode(current)
                            + " device=" + ratio + " user=" + userZoom.getFloat(current));
                }
            }
        });
        for (int i = 0; i < stillApply.length; i++) {
            final Field shot = stillShot[i], builder = stillBuilder[i];
            XposedBridge.hookMethod(stillApply[i], new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                    Scope scope = selected();
                    if (scope == null || p.hasThrowable()
                            || p.args[0] != zoomManager.invoke(scope.module)) return;
                    Object current = currentConfig(scope);
                    Object owner = shotCamera.get(shot.get(p.thisObject));
                    if (config.get(shotConfig.get(owner)) != current) return;
                    float user = userZoom.getFloat(current);
                    float ratio = (Float) transform.invoke(p.args[0], user);
                    writeTag.invoke(null, builder.get(p.thisObject), targetTag,
                            Float.valueOf(ratio), false);
                    LiveMotionModule.log("target capture config=" + System.identityHashCode(current)
                            + " device=" + ratio + " user=" + user);
                }
            });
        }
    }

    private Object currentConfig(Scope scope) throws ReflectiveOperationException {
        return config.get(configs.invoke(cameraManager.invoke(scope.module)));
    }

    private Scope selected() throws ReflectiveOperationException {
        Scope scope = active;
        if (scope == null) return null;
        String selected = (String) type.invoke(null, 231);
        return "2".equals(selected) || "3".equals(selected) ? scope : null;
    }

    private static final class Scope {
        final Object module;
        float lastLogged = Float.NaN;
        Scope(Object module) { this.module = module; }
    }
}
