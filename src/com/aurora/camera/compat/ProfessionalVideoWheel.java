package com.aurora.camera.compat;

import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.SparseArray;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

/** The Aurora professional video ruler, expressed in the camera's displayed zoom ratios. */
final class ProfessionalVideoWheel {
    private static final int DIRECTOR = 164;
    private static final int PRO_VIDEO = 180;
    private static final String SCOPE = "professional-video-ruler";

    private enum Lens {
        ULTRA("ultra", 2f, 2f, 1f),
        WIDE("wide", 6f, 2f, 2f, 3f),
        TELE("tele", 9.6f, 0f, 6f),
        STANDALONE("Standalone", 15f, 0f, 10f);

        final String token;
        final float maximum;
        final float denseEnd;
        final float[] marks;

        Lens(String token, float maximum, float denseEnd, float... marks) {
            this.token = token;
            this.maximum = maximum;
            this.denseEnd = denseEnd;
            this.marks = marks;
        }

        ArrayList<Float> stops(Range<Float> range) {
            ArrayList<Float> values = new ArrayList<>();
            values.add(range.getLower());
            for (float mark : marks) {
                if (mark > range.getLower() && mark < range.getUpper()) values.add(mark);
            }
            if (!range.getUpper().equals(range.getLower())) values.add(range.getUpper());
            return values;
        }
    }

    private static final class Ruler {
        final Lens lens;
        final int cameraId;
        final Range<Float> range;

        Ruler(Lens lens, int cameraId, Range<Float> range) {
            this.lens = lens;
            this.cameraId = cameraId;
            this.range = range;
        }
    }

    private ProfessionalVideoWheel() {}

    static Runnable prepare(Host host) throws ReflectiveOperationException {
        Class<?> adapter = host.type("J9.w");
        Class<?> zoomManager = host.type("i9.e");
        Class<?> caps = host.method("v6.f", "M", int.class).getReturnType();
        Method physicalProfile = host.method(zoomManager, "m6",
                int.class, int.class, caps, boolean.class);
        Method physicalRange = host.method(zoomManager, "a6", int.class);
        Method recordingRange = host.method(zoomManager, "O4");
        Method recordingTeleMaximum = host.method(zoomManager, "z5", caps);
        Method proPreviewRange = host.method("j9.A", "P5");
        Method directorPreviewRange = host.method("j9.e", "P5");
        Method proRestoredRange = host.method("j9.A", "c5");
        Method directorRestoredRange = host.method("j9.K", "c5");
        Method proFixedRange = host.method("com.android.camera.data.data.F", "K", int.class);
        Method croppingVideo = host.method("com.android.camera.data.data.F", "G", int.class);
        Method fixedVideoRange = host.method("com.android.camera.data.data.F", "J", int.class);
        Method specialLayout = host.method("K2.b", "a0");
        Method ultra = host.method("v6.f", "f0", int.class);
        Method wide = host.method("v6.f", "c0", int.class);
        Method tele = host.method("v6.f", "Z", int.class);
        Method standalone = host.method("v6.f", "e0", int.class);
        Method sat = host.method("com.android.camera.data.data.i", "n1", int.class, boolean.class);
        Method build = host.method("J9.e", "W");
        Method lensToken = host.method("com.android.camera.data.data.n", "h", int.class);
        Method markValues = host.method("com.android.camera.data.data.i", "W", int.class, String.class);
        Method fineTickEnd = host.method(adapter, "O");
        Method sparseTicks = host.method("J9.e", "l0", float.class);
        Method labelFocalLength = host.method(adapter, "K", Pair.class);
        Method getModule = host.method("com.android.camera.fragment.h", "getBaseModule");
        Method getModuleMode = host.method("com.android.camera.module.a0", "getModuleIndex");
        Method cameraManager = host.method("com.android.camera.module.a0", "getCameraManager");
        Method getId = host.method(cameraManager.getReturnType(), "getActualCameraId");
        Method getRunning = host.method("g2.a", "j");
        Method getComponent = host.method("Xh.b", "x", Class.class);
        Class<?> focalLengths = host.type("v2.Y");
        Field focalByCamera = host.field(focalLengths, "c");
        Field wheelMode = host.field(adapter, "W0");
        Field owner = host.field(adapter, "G0");
        Field boundRange = host.field(adapter, "S0");
        Field moduleRef = host.field(zoomManager, "b");
        Field managerMode = host.field(zoomManager, "c");
        ThreadLocal<Ruler> drawing = new ThreadLocal<>();
        ThreadLocal<Range<Float>> standaloneRecording = new ThreadLocal<>();

        return () -> {
            HookDiagnostics.hookMethod(physicalProfile, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    int mode = (int) param.args[1];
                    if (mode != DIRECTOR && mode != PRO_VIDEO) return;
                    int id = (int) param.args[0];
                    Lens lens = lens(id, ultra, wide, tele, standalone);
                    if (lens == null || (mode == PRO_VIDEO && (lens != Lens.STANDALONE
                            || (boolean) sat.invoke(null, mode, false)))) return;
                    param.setResult(limit(range(physicalRange.invoke(null, id)), lens));
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(recordingRange, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    int mode = managerMode.getInt(param.thisObject);
                    if (mode != DIRECTOR && mode != PRO_VIDEO) return;
                    Object module = ((WeakReference<?>) moduleRef.get(param.thisObject)).get();
                    if (module == null) return;
                    int id = (int) getId.invoke(cameraManager.invoke(module));
                    if (!(boolean) standalone.invoke(null, id)) return;
                    standaloneRecording.set(limit(range(physicalRange.invoke(null, id)), Lens.STANDALONE));
                    mark(param);
                }

                @Override protected void after(MethodHookParam param) {
                    if (countsAsRun(param)) standaloneRecording.remove();
                }
            });
            HookDiagnostics.hookMethod(recordingTeleMaximum, new WheelHook() {
                @Override protected void before(MethodHookParam param) {
                    Range<Float> range = standaloneRecording.get();
                    if (range == null) return;
                    param.setResult(range.getUpper());
                    mark(param);
                }
            });
            WheelHook extendStandaloneSession = new WheelHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable()) return;
                    int mode = managerMode.getInt(param.thisObject);
                    if (mode == PRO_VIDEO) {
                        if ((boolean) sat.invoke(null, mode, false)
                                || (boolean) proFixedRange.invoke(null, mode)) return;
                    } else if (mode != DIRECTOR) {
                        return;
                    }
                    if (param.method.equals(directorRestoredRange)
                            && ((boolean) specialLayout.invoke(null)
                            || (boolean) croppingVideo.invoke(null, mode)
                            || (boolean) fixedVideoRange.invoke(null, mode))) return;
                    Object module = ((WeakReference<?>) moduleRef.get(param.thisObject)).get();
                    if (module == null) return;
                    int id = (int) getId.invoke(cameraManager.invoke(module));
                    if (!(boolean) standalone.invoke(null, id)) return;
                    Range<Float> physical = limit(range(physicalRange.invoke(null, id)), Lens.STANDALONE);
                    Range<Float> original = range(param.getResult());
                    // The aggregate preview range is built from Qg.c's camera list, which
                    // omits Aurora's standalone lens. Include the active lens's upper bound.
                    param.setResult(Range.create(original.getLower(),
                            Math.max(original.getUpper(), physical.getUpper())));
                    mark(param);
                }
            };
            HookDiagnostics.hookMethod(proPreviewRange, extendStandaloneSession);
            HookDiagnostics.hookMethod(directorPreviewRange, extendStandaloneSession);
            HookDiagnostics.hookMethod(proRestoredRange, extendStandaloneSession);
            HookDiagnostics.hookMethod(directorRestoredRange, extendStandaloneSession);
            HookDiagnostics.hookMethod(build, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object wheel = param.thisObject;
                    int mode = wheelMode.getInt(wheel);
                    if (mode != PRO_VIDEO) return;
                    Object fragment = owner.get(wheel);
                    Object module = ((Optional<?>) getModule.invoke(fragment)).orElse(null);
                    if (module == null) return;
                    int id = (int) getId.invoke(cameraManager.invoke(module));
                    Lens lens = lens(id, ultra, wide, tele, standalone);
                    int actualMode = (int) getModuleMode.invoke(module);
                    if (lens == null || (actualMode != DIRECTOR
                            && (actualMode != PRO_VIDEO || lens != Lens.STANDALONE))) return;
                    Range<Float> range = range(boundRange.get(wheel));
                    if (range == null || range.getUpper().equals(range.getLower())) return;
                    param.setObjectExtra(SCOPE, drawing.get());
                    drawing.set(new Ruler(lens, id, range));
                    mark(param);
                }

                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (!countsAsRun(param)) return;
                    Ruler wheel = drawing.get();
                    Ruler previous = (Ruler) param.getObjectExtra(SCOPE);
                    if (previous == null) drawing.remove();
                    else drawing.set(previous);
                    if (param.hasThrowable()) return;
                    if (wheel.lens == Lens.STANDALONE) {
                        Object data = getComponent.invoke(getRunning.invoke(null), focalLengths);
                        SparseArray<?> lengths = (SparseArray<?>) focalByCamera.get(data);
                        Float focal = lengths == null ? null : (Float) lengths.get(wheel.cameraId);
                        if (focal != null) labelFocalLength.invoke(param.thisObject,
                                Pair.create(Collections.singletonList(focal.toString()),
                                        Collections.singletonList(wheel.cameraId)));
                    }
                    Log.i("AuroraVideoWheel", wheel.lens.token + " camera=" + wheel.cameraId
                            + " range=" + wheel.range + " stops=" + wheel.lens.stops(wheel.range));
                }
            });
            HookDiagnostics.hookMethod(lensToken, new WheelHook() {
                @Override protected void before(MethodHookParam param) {
                    Ruler wheel = drawing.get();
                    if (wheel == null || (int) param.args[0] != PRO_VIDEO) return;
                    param.setResult(wheel.lens.token);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(markValues, new WheelHook() {
                @Override protected void before(MethodHookParam param) {
                    Ruler wheel = drawing.get();
                    if (wheel == null || (int) param.args[0] != PRO_VIDEO
                            || !wheel.lens.token.equals(param.args[1])) return;
                    param.setResult(wheel.lens.stops(wheel.range));
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(fineTickEnd, new WheelHook() {
                @Override protected void before(MethodHookParam param) {
                    Ruler wheel = drawing.get();
                    if (wheel == null) return;
                    param.setResult(wheel.range.clamp(wheel.lens.denseEnd));
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(sparseTicks, new WheelHook() {
                @Override @SuppressWarnings("unchecked")
                protected void after(MethodHookParam param) {
                    Ruler wheel = drawing.get();
                    if (param.hasThrowable() || wheel == null
                            || (float) param.args[0] != wheel.range.getLower()) return;
                    // With no fine ticks, the native generator skips the boundary in both lists.
                    ((ArrayList<Float>) param.getResult()).add(0, wheel.range.getLower());
                    mark(param);
                }
            });
        };
    }

    private static Lens lens(int id, Method ultra, Method wide, Method tele, Method standalone)
            throws ReflectiveOperationException {
        if ((boolean) ultra.invoke(null, id)) return Lens.ULTRA;
        if ((boolean) wide.invoke(null, id)) return Lens.WIDE;
        if ((boolean) tele.invoke(null, id)) return Lens.TELE;
        if ((boolean) standalone.invoke(null, id)) return Lens.STANDALONE;
        return null;
    }

    private static Range<Float> limit(Range<Float> range, Lens lens) {
        return Range.create(range.getLower(), Math.min(range.getUpper(), lens.maximum));
    }

    @SuppressWarnings("unchecked")
    private static Range<Float> range(Object value) {
        return (Range<Float>) value;
    }

    private abstract static class WheelHook extends StrictHook {
        final void mark(MethodHookParam param) {
            param.setObjectExtra("professional-video-wheel-applied", Boolean.TRUE);
        }

        @Override protected boolean countsAsRun(MethodHookParam param) {
            return Boolean.TRUE.equals(param.getObjectExtra("professional-video-wheel-applied"));
        }
    }
}
