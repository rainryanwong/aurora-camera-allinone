package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Routes the Aurora director's lens buttons through the native physical-camera switch. */
final class DirectorCapture {
    private static final int DIRECTOR = 164;
    private static final String EXTENDED = "director.lens.extended";
    private static final String SWITCHED = "director.lens.switched";
    private static final String RECORDING_TAP = "director.lens.recording-tap";
    private static final String[] LENSES = {"ultra", "wide", "tele", "Standalone"};
    private static final int[] LENS_LABELS = {0x7f14100c, 0x7f14100e, 0x7f141009, 0x7f141008};
    private static final String FRAGMENT = "com.android.camera.fragment.h";
    private static final String ZOOM_VIEW = "com.android.camera.ui.zoom.ZoomRatioToggleView";
    private static final String COMPONENT = "com.android.camera.data.data.c";

    private DirectorCapture() {}

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        Runnable lenses = prepareLenses(host);
        Runnable aperture = prepareAperture(host);
        return new HookPlan() {
            @Override public String id() { return "DirectorCapture"; }
            @Override public void install() {
                lenses.run();
                aperture.run();
            }
        };
    }

    private static Runnable prepareLenses(Host host) throws ReflectiveOperationException {
        Class<?> lensComponent = host.type("r2.C0");
        Method lensModel = host.method(lensComponent, "q", int.class);
        Method modelEntries = host.method(lensModel.getReturnType(), "a");
        Constructor<?> lensEntry = host.type("r2.C0$a").getDeclaredConstructor();
        lensEntry.setAccessible(true);
        Field lensName = host.field(lensEntry.getDeclaringClass(), "a");
        Field lensLabel = host.field(lensEntry.getDeclaringClass(), "b");
        Field lensId = host.field(lensEntry.getDeclaringClass(), "c");
        Method cameras = host.method("v6.f", "R");
        Method[] cameraIds = {
                host.method("v6.f", "j"), host.method("v6.f", "e"),
                host.method("v6.f", "q"), host.method("v6.f", "K")
        };
        Method usableVideoCamera = host.method("r2.h0", "G", int.class);
        Method ultraRatio = host.method("Kr.i", "j");
        Method teleRatio = host.method("Kr.i", "h");
        Method standaloneRatio = host.method("Kr.i", "i");
        Method zoomRatios = host.method("com.android.camera.data.data.i", "S", int.class);
        Method zoomClick = host.method("H4.p0", "Ds", int.class, int.class);
        Method canClick = host.method("H4.p0", "I0");
        Method refreshZoom = host.method("H4.p0", "Us", int.class, boolean.class);
        Field zoomView = host.field("H4.p0", "j");
        Field fragmentMode = host.field(FRAGMENT, "mCurrentMode");
        Method module = host.method(FRAGMENT, "getBaseModule");
        Method zoomAtIndex = host.method(ZOOM_VIEW, "m", int.class, int.class,
                boolean.class, boolean.class);
        Method vertical = host.method("K2.b", "W");
        Method configs = host.method("g2.a", "a");
        Method component = host.method("Xh.b", "x", Class.class);
        Method readLens = host.method(COMPONENT, "getComponentValue", int.class);
        Method writeLens = host.method(COMPONENT, "setComponentValue", int.class, String.class);
        Method videoRecording = host.method("com.android.camera.features.mode.cinemaster.CinemasterModule", "isRecording");
        Method prepareRecording = host.method("com.android.camera.module.VideoBase", "isPrepareRecording");
        Method stoppingRecording = host.method("com.android.camera.module.VideoBase", "isRecorderStopping");
        Method manualProtocol = host.method("R6.C0", "b");
        Method switchLens = host.method("R6.C0", "G6", lensComponent, int.class, boolean.class);

        return () -> {
            HookDiagnostics.hookMethod(lensModel, new StrictHook() {
                @SuppressWarnings("unchecked")
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable() || (int) param.args[0] != DIRECTOR) return;
                    Map<String, Object> entries = (Map<String, Object>) modelEntries.invoke(param.getResult());
                    LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
                    Object registry = cameras.invoke(null);
                    for (int index = 0; index < LENSES.length; index++) {
                        String role = LENSES[index];
                        Object entry = entries.get(role);
                        if (entry != null) {
                            ordered.put(role, entry);
                            continue;
                        }
                        int id = (int) cameraIds[index].invoke(registry);
                        if (id < 0 || !(boolean) usableVideoCamera.invoke(null, id)) continue;
                        entry = lensEntry.newInstance();
                        lensName.set(entry, role);
                        lensLabel.setInt(entry, LENS_LABELS[index]);
                        lensId.setInt(entry, id);
                        ordered.put(role, entry);
                        param.setObjectExtra(EXTENDED, true);
                    }
                    if (!Boolean.TRUE.equals(param.getObjectExtra(EXTENDED))) return;
                    entries.forEach(ordered::putIfAbsent);
                    entries.clear();
                    entries.putAll(ordered);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(EXTENDED));
                }
            });
            HookDiagnostics.hookMethod(zoomRatios, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable() || (int) param.args[0] != DIRECTOR) return;
                    int id = (int) cameraIds[3].invoke(cameras.invoke(null));
                    if (id < 0 || !(boolean) usableVideoCamera.invoke(null, id)) return;
                    float[] ratios = (float[]) param.getResult();
                    float standalone = (float) standaloneRatio.invoke(null);
                    for (float ratio : ratios) if (sameRatio(ratio, standalone)) return;
                    float[] withStandalone = Arrays.copyOf(ratios, ratios.length + 1);
                    withStandalone[ratios.length] = standalone;
                    Arrays.sort(withStandalone);
                    param.setResult(withStandalone);
                    param.setObjectExtra(EXTENDED, true);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(EXTENDED));
                }
            });
            HookDiagnostics.hookMethod(zoomClick, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object fragment = param.thisObject;
                    if (fragmentMode.getInt(fragment) != DIRECTOR) return;
                    Object video = ((Optional<?>) module.invoke(fragment)).orElse(null);
                    if (!videoRecording.getDeclaringClass().isInstance(video)) return;
                    if ((boolean) videoRecording.invoke(video)) {
                        // The single recording readout also dispatches a tap to the lens selector.
                        param.setResult(null);
                        param.setObjectExtra(RECORDING_TAP, true);
                        return;
                    }
                    Object protocol = manualProtocol.invoke(null);
                    if (protocol == null) return;
                    if (!(boolean) canClick.invoke(fragment) || (boolean) prepareRecording.invoke(video)
                            || (boolean) stoppingRecording.invoke(video)) {
                        param.setResult(null);
                        return;
                    }
                    Object lens = component.invoke(configs.invoke(null), lensComponent);
                    float ratio = (float) zoomAtIndex.invoke(zoomView.get(fragment), DIRECTOR,
                            (int) param.args[0], vertical.invoke(null), false);
                    String role;
                    if (sameRatio(ratio, 1f)) role = "wide";
                    else if (sameRatio(ratio, (float) ultraRatio.invoke(null))) role = "ultra";
                    else if (sameRatio(ratio, (float) teleRatio.invoke(null))) role = "tele";
                    else if (sameRatio(ratio, (float) standaloneRatio.invoke(null))) role = "Standalone";
                    else return;

                    Map<?, ?> entries = (Map<?, ?>) modelEntries.invoke(lensModel.invoke(lens, DIRECTOR));
                    if (!entries.containsKey(role)) return;
                    param.setResult(null);
                    if (role.equals(readLens.invoke(lens, DIRECTOR))) return;

                    // The native 164 route reads this role, opens its camera and rechecks its
                    // recording profile. The zoom protocol only changes the active camera's crop.
                    writeLens.invoke(lens, DIRECTOR, role);
                    switchLens.invoke(protocol, lens, DIRECTOR, true);
                    refreshZoom.invoke(fragment, (int) param.args[1], false);
                    param.setObjectExtra(SWITCHED, true);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(SWITCHED))
                            || Boolean.TRUE.equals(param.getObjectExtra(RECORDING_TAP));
                }
            });
        };
    }

    @SuppressWarnings("unchecked")
    private static Runnable prepareAperture(Host host) throws ReflectiveOperationException {
        Class<?> directorAdapter = host.type("da.a");
        Method startStop = host.method("J9.s", "B", float.class);
        Method endStop = host.method("J9.s", "w", float.class);
        Method initAngles = host.method(directorAdapter, "N");
        Method mapAngle = host.method("J9.s", "D", float.class);
        Method crossesTick = host.method("J9.s", "O", float.class, float.class, ArrayList.class);
        Field angles = host.field("J9.s", "T0");
        Field stopAngles = host.field("J9.s", "U0");
        Field stopValues = host.field("J9.s", "L0");
        return () -> {
            StrictHook keepDragOrigin = new StrictHook() {
                @Override protected void after(MethodHookParam param) {
                    if (!param.hasThrowable() && directorAdapter.isInstance(param.thisObject)
                            && (boolean) param.getResult()) {
                        // The 1770 zoom wheel resets ACTION_MOVE's origin at either end.
                        // The aperture scale uses its own stop-point and AUTO damping.
                        param.setResult(false);
                        param.setObjectExtra("director.aperture.keepOrigin", true);
                    }
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director.aperture.keepOrigin"));
                }
            };
            HookDiagnostics.hookMethod(startStop, keepDragOrigin);
            HookDiagnostics.hookMethod(endStop, keepDragOrigin);
            HookDiagnostics.hookMethod(initAngles, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    // The panel changes handedness after binding, which recomputes every angle.
                    // Keep only the positions from the current geometry, as the base adapter does.
                    ((List<?>) angles.get(param.thisObject)).clear();
                    ((List<?>) stopAngles.get(param.thisObject)).clear();
                }

                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (param.hasThrowable()) return;
                    // Drawing can reverse the arc, while touch always travels from 0 to -120.
                    ((List<Float>) angles.get(param.thisObject)).replaceAll(angle -> -Math.abs(angle));
                    ((List<Float>) stopAngles.get(param.thisObject)).replaceAll(angle -> -Math.abs(angle));
                }
            });
            HookDiagnostics.hookMethod(mapAngle, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (!directorAdapter.isInstance(param.thisObject) || param.getResult() == null) return;
                    float value = Float.parseFloat((String) param.getResult());
                    // Interpolation can shift an exact stop, such as F1.63, by one ULP.
                    // Keep the original stop value for the native vibration lookup.
                    for (float stop : (List<Float>) stopValues.get(param.thisObject)) {
                        float difference = Math.abs(value - stop);
                        if (difference == 0.0f) return;
                        if (difference <= Math.ulp(stop)) {
                            param.setResult(Float.toString(stop));
                            param.setObjectExtra("director.aperture.stopValue", true);
                            return;
                        }
                    }
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director.aperture.stopValue"));
                }
            });
            HookDiagnostics.hookMethod(crossesTick, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    if (!directorAdapter.isInstance(param.thisObject)) return;
                    float previousAngle = (float) param.args[0];
                    float nextAngle = (float) param.args[1];
                    // Exclude the starting angle so a held or departed tick cannot trigger again.
                    if (previousAngle == nextAngle) {
                        param.setResult(false);
                    } else {
                        param.args[0] = Math.nextAfter(previousAngle, nextAngle);
                    }
                    param.setObjectExtra("director.aperture.crossing", true);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director.aperture.crossing"));
                }
            });
        };
    }

    private static boolean sameRatio(float left, float right) {
        return Math.abs(left - right) < 0.01f;
    }
}
