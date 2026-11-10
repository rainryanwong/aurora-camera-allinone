package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Share the native shutter picker across the requested capture modes. */
public final class ShutterStyleHooks {
    private static final int SHUTTER = 266;
    private ShutterStyleHooks() {}

    static boolean supported(int mode) {
        switch (mode) {
            case 163: case 166: case 167: case 168: case 171: case 173: case 175:
            case 186: case 187: case 188: case 205: case 231:
                return true;
            default: return false;
        }
    }

    private static ArrayList<Integer> withShutter(List<Integer> previous) {
        ArrayList<Integer> result = new ArrayList<>(previous);
        if (!result.contains(SHUTTER)) result.add(SHUTTER);
        return result;
    }

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method factory = host.method("X9.w5", "E", int.class);
        final Field itemId = host.field("b5.i", "c");
        final Method providerMode = host.type("y3.s").getMethod("getModuleId");
        final Method available = host.method("u2.y", "T", int.class, ArrayList.class);
        final Method editor = host.method("u2.y", "Y", ArrayList.class, boolean.class);
        final Method saved = host.method("u2.y", "U", int.class);
        final Field menuMode = host.field("com.android.camera.data.data.c", "mCurrentMode");
        final Method draw = host.method("A8.h", "k", host.type("v2.I0"));
        final Field drawMode = host.field("v2.I0", "a");
        final Method selected = host.method("com.android.camera.data.data.x", "v");
        final Set<Method> providers = new LinkedHashSet<>();
        for (String owner : new String[]{
                "com.android.camera.features.mode.capture.r",
                "com.android.camera.features.mode.capture.f1",
                "com.android.camera.features.mode.capture.g1",
                "j4.b", "z3.d", "b4.a", "f4.a",
                "com.android.camera.features.mode.portrait.e",
                "com.android.camera.features.mode.portrait.k",
                "com.android.camera.features.mode.portrait.l",
                "Rn.e", "d4.b", "U3.c", "V3.b", "B3.h",
                "com.android.camera.features.mode.aiwatermark.a"}) {
            providers.add(host.type(owner).getMethod("l"));
        }
        return new HookPlan() {
            public String id() { return "native-shutter-style-modes"; }
            public void install() {
                XposedBridge.hookMethod(draw, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        int mode = drawMode.getInt(p.args[0]);
                        if (!supported(mode) || "custom_shutter_default".equals(selected.invoke(null))) return;
                        // 225 is the native shared material/URI drawing branch.
                        // Only drawable initialization uses it; restore the real
                        // capture mode before the caller sets up animations.
                        p.setObjectExtra("shutter.actualMode", mode);
                        drawMode.setInt(p.args[0], 225);
                    }
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        Integer mode = (Integer) p.getObjectExtra("shutter.actualMode");
                        if (mode != null) drawMode.setInt(p.args[0], mode);
                    }
                });
                StrictHook provider = new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || !supported((Integer) providerMode.invoke(p.thisObject))) return;
                        @SuppressWarnings("unchecked") List<Object> previous = (List<Object>) p.getResult();
                        for (Object row : previous) if (itemId.getInt(row) == SHUTTER) return;
                        ArrayList<Object> result = new ArrayList<>(previous);
                        result.add(factory.invoke(null, SHUTTER));
                        p.setResult(result);
                    }
                };
                // Native editor candidates come from l(); its saved first six
                // positions become the top bar through the same 266 factory.
                for (Method method : providers) XposedBridge.hookMethod(method, provider);
                XposedBridge.hookMethod(available, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        int mode = (Integer) p.args[0];
                        if (!supported(mode)) return;
                        @SuppressWarnings("unchecked") List<Integer> values = (List<Integer>) p.args[1];
                        p.args[1] = withShutter(values);
                        @SuppressWarnings("unchecked") List<Integer> previous = (List<Integer>) saved.invoke(p.thisObject, mode);
                        if (!previous.isEmpty() && !previous.contains(SHUTTER))
                            editor.invoke(p.thisObject, withShutter(previous), false);
                    }
                });
                XposedBridge.hookMethod(editor, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        if (!supported(menuMode.getInt(p.thisObject))) return;
                        @SuppressWarnings("unchecked") List<Integer> previous = (List<Integer>) p.args[0];
                        if (!previous.contains(SHUTTER)) p.args[0] = withShutter(previous);
                    }
                });
            }
        };
    }
}
