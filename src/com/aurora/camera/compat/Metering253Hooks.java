package com.aurora.camera.compat;

import android.hardware.camera2.CaptureRequest;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Native metering controls, preferences and request consumers for photo/video modes. */
public final class Metering253Hooks {
    private static final int METER = 214;
    private Metering253Hooks() {}

    private static boolean extended(int mode) {
        return mode == 162 || mode == 163 || mode == 168 || mode == 173
                || mode == 175 || mode == 225 || mode == 256;
    }

    public static HookPlan resolve(final Host host) throws ReflectiveOperationException {
        final Class<?> meterType = host.type("r2.G");
        final Method support = host.method(meterType, "isSupportMode", int.class);
        final Method size = host.method(meterType, "getSize");
        final Method value = host.method(meterType, "getComponentValue", int.class);
        final Method repository = host.method("g2.a", "a");
        final Method lookup = host.method("Xh.b", "x", Class.class);
        final Method global = host.method("g2.a", "g");
        final Field globalState = host.field("u2.Y", "u");
        final Method globalMode = host.method("u2.Y", "D", int.class);
        final Method selected = host.method("com.android.camera.data.data.i", "v");
        final Method nativeRequest = host.method("l9.o0", "X",
                CaptureRequest.Builder.class, host.type("l9.j0"));
        final Field requestMode = host.field("l9.j0", "K3");
        final Field requestMeter = host.field("l9.j0", "L0");
        final Field cameraConfig = host.field("l9.y0", "E");
        final Field configValues = host.field("l9.i0", "a");
        final Method capture = host.method("l9.y0", "D1", int.class, CaptureRequest.Builder.class);
        final Method record = host.method("l9.y0", "O1", CaptureRequest.Builder.class);
        final Method factory = host.method("X9.w5", "E", int.class);
        final Field itemId = host.field("b5.i", "c");
        final Method providerMode = host.type("y3.s").getMethod("getModuleId");
        final Method available = host.method("u2.y", "T", int.class, ArrayList.class);
        final Method editor = host.method("u2.y", "Y", ArrayList.class, boolean.class);
        final Method saved = host.method("u2.y", "U", int.class);
        final Field menuMode = host.field("com.android.camera.data.data.c", "mCurrentMode");
        final Method aiParameters = host.method("z3.c", "i", int.class);
        final Field parameters = host.field("U9.S", "q");
        final Set<Method> providers = new LinkedHashSet<>();
        for (String owner : new String[]{
                "com.android.camera.features.mode.capture.r",
                "com.android.camera.features.mode.capture.f1",
                "com.android.camera.features.mode.capture.g1",
                "r4.b", "com.android.camera.features.mode.video.a",
                "com.android.camera.features.mode.video.b", "z3.d", "b4.a", "f4.a",
                "com.android.camera.features.mode.street.a", "T3.b"}) {
            providers.add(host.type(owner).getMethod("l"));
        }
        return new HookPlan() {
            public String id() { return "253.native-metering-modes"; }

            Object component() throws ReflectiveOperationException {
                return lookup.invoke(repository.invoke(null), meterType);
            }

            boolean ready(Object component) throws ReflectiveOperationException {
                // r2.G.r(C) supplies three entries only after the native p1 capability check.
                return (Integer) size.invoke(component) > 1;
            }

            int currentMode() throws ReflectiveOperationException {
                Object data = global.invoke(null);
                return (Integer) globalMode.invoke(data, globalState.getInt(data));
            }

            void apply(Object camera, Object builder) throws ReflectiveOperationException {
                Object config = configValues.get(cameraConfig.get(camera));
                if (extended(requestMode.getInt(config)) && ready(component())) nativeRequest.invoke(null, builder, config);
            }

            public void install() {
                XposedBridge.hookMethod(support, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        if (extended((Integer) p.args[0])) p.setResult(true);
                    }
                });
                XposedBridge.hookMethod(selected, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        int mode = currentMode();
                        if (!extended(mode)) return;
                        Object meter = component();
                        if (ready(meter)) p.setResult(Integer.parseInt((String) value.invoke(meter, mode)));
                    }
                });
                XposedBridge.hookMethod(nativeRequest, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        int mode = requestMode.getInt(p.args[1]);
                        if (!extended(mode)) return;
                        Object meter = component();
                        if (ready(meter)) requestMeter.setInt(p.args[1],
                                Integer.parseInt((String) value.invoke(meter, mode)));
                    }
                });
                // Native N1 preview always calls X. D1 already includes the ordinary
                // photo/AI/night/ultra-pixel/street modes, but its group omits Legend.
                XposedBridge.hookMethod(capture, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable()) return;
                        Object config = configValues.get(cameraConfig.get(p.thisObject));
                        if (requestMode.getInt(config) == 256) apply(p.thisObject, p.args[1]);
                    }
                });
                // O1's native manual-video group omits ordinary video (162).
                XposedBridge.hookMethod(record, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable()) return;
                        Object config = configValues.get(cameraConfig.get(p.thisObject));
                        if (requestMode.getInt(config) == 162) apply(p.thisObject, p.args[0]);
                    }
                });
                StrictHook provider = new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || !extended((Integer) providerMode.invoke(p.thisObject))) return;
                        if (!ready(component())) return;
                        @SuppressWarnings("unchecked") List<Object> previous = (List<Object>) p.getResult();
                        for (Object row : previous) if (itemId.getInt(row) == METER) return;
                        ArrayList<Object> result = new ArrayList<>(previous);
                        result.add(factory.invoke(null, METER));
                        p.setResult(result);
                    }
                };
                // Y9.O.b/f both obtain dropdown candidates from l(); the saved first
                // six slots and the native 214 factory also support dragging it on top.
                for (Method method : providers) XposedBridge.hookMethod(method, provider);
                XposedBridge.hookMethod(available, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        int mode = (Integer) p.args[0];
                        if (!extended(mode) || !ready(component())) return;
                        @SuppressWarnings("unchecked") ArrayList<Integer> values = (ArrayList<Integer>) p.args[1];
                        if (!values.contains(METER)) values.add(METER);
                        @SuppressWarnings("unchecked") List<Integer> previous = (List<Integer>) saved.invoke(p.thisObject, mode);
                        if (!previous.isEmpty() && !previous.contains(METER)) {
                            ArrayList<Integer> updated = new ArrayList<>(previous);
                            updated.add(METER);
                            editor.invoke(p.thisObject, updated, false);
                        }
                    }
                });
                XposedBridge.hookMethod(editor, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        if (!extended(menuMode.getInt(p.thisObject)) || !ready(component())) return;
                        @SuppressWarnings("unchecked") ArrayList<Integer> previous = (ArrayList<Integer>) p.args[0];
                        if (!previous.contains(METER)) {
                            ArrayList<Integer> result = new ArrayList<>(previous);
                            result.add(METER);
                            p.args[0] = result;
                        }
                    }
                });
                XposedBridge.hookMethod(aiParameters, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || (Integer) p.args[0] != 168) return;
                        // z3.c copies U9.u0's manual components. Exclude metering from
                        // that owned list before workspace matching and recipe writes.
                        for (Iterator<?> it = ((ArrayList<?>) parameters.get(p.thisObject)).iterator(); it.hasNext();) {
                            if (meterType.isInstance(it.next())) it.remove();
                        }
                    }
                });
            }
        };
    }
}

