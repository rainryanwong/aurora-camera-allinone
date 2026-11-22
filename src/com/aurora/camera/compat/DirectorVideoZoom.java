package com.aurora.camera.compat;

import android.util.Log;
import android.util.Range;
import android.view.View;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/** Binds the director's wheel to the active physical camera. */
final class DirectorVideoZoom {
    private static final int VIDEO = 162;
    private static final int DIRECTOR = 164;
    private static final int PRO_VIDEO = 180;
    private static final String SCOPE = "director-video-zoom";

    private DirectorVideoZoom() {}

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        Class<?> fragment = host.type("H4.J");
        Class<?> background = host.type("com.android.camera.fragment.w0");
        Class<?> adapter = host.type("J9.w");
        String drawAdapter = "com.android.camera2.compat.theme.custom.mm.zoom.a$b";
        Method show = host.method(fragment, "provideAnimateElement", int.class, List.class, int.class);
        Method startLayout = host.method(fragment, "Bs");
        Method beforeAdapter = host.method(fragment, "vs");
        Method backgroundLayout = host.method(fragment, "fs", fragment, host.type("v2.H0"), host.type("R6.O0"));
        Method showBackground = host.method(background, "b6", int.class, boolean.class, boolean.class);
        Method setRange = host.method(adapter, "c0", Range.class);
        Method configureLensRange = host.method("i9.e", "r1", int.class, int.class);
        Method alignLabels = host.method(adapter, "y", boolean.class);
        String wheelView = "com.android.camera2.compat.theme.custom.mm.zoom.a";
        Method rotateLabels = host.method(wheelView, "e", int.class, boolean.class);
        Method activeAdapter = host.method(wheelView, "getDrawAdapter");
        Method satProZoom = host.method("com.android.camera.data.data.i", "n1", int.class, boolean.class);
        Method zoomProtocol = host.method("W6.d", "a");
        Method activeZoomRange = host.method("W6.d", "H0");
        Method getBaseModule = host.method("com.android.camera.fragment.h", "getBaseModule");
        Method cameraManager = host.method("com.android.camera.module.a0", "getCameraManager");
        Method actualCameraId = host.method(cameraManager.getReturnType(), "getActualCameraId");
        Method rangeForVideoLens = host.method("i9.e", "d3", int.class, int.class);
        Method isLandscape = host.method("com.android.camera.fragment.h", "isLandScape");
        Method fragmentDegree = host.method("com.android.camera.fragment.h", "getDegree");
        Field mode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        Field owner = host.field(adapter, "G0");
        Field adapterMode = host.field(adapter, "W0");
        Field landscapeDirectorTicks = host.field(drawAdapter, "m0");
        Field portraitDirectorTicks = host.field(drawAdapter, "l0");
        Field cover = host.field(background, "a");
        Field directorCover = host.field("da.b", "l");
        Field landscapeCover = host.field("da.b", "k");
        ThreadLocal<Object> wheelOwner = new ThreadLocal<>();
        ThreadLocal<Object> videoCover = new ThreadLocal<>();
        Runnable professionalWheel = ProfessionalVideoWheel.prepare(host);

        Runnable install = () -> {
            professionalWheel.run();
            HookDiagnostics.hookMethod(show, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    param.setObjectExtra(SCOPE, wheelOwner.get());
                    if (mode.getInt(param.thisObject) == DIRECTOR && (int) param.args[0] == DIRECTOR) {
                        wheelOwner.set(param.thisObject);
                        param.setObjectExtra("director-wheel-active", Boolean.TRUE);
                    } else {
                        wheelOwner.remove();
                    }
                }

                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (Boolean.TRUE.equals(param.getObjectExtra("director-wheel-active"))
                            && mode.getInt(param.thisObject) != DIRECTOR) {
                        mode.setInt(param.thisObject, DIRECTOR);
                    }
                    restore(wheelOwner, param.getObjectExtra(SCOPE));
                }
            });
            HookDiagnostics.hookMethod(startLayout, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    if (param.thisObject != wheelOwner.get()) return;
                    mode.setInt(param.thisObject, VIDEO);
                    mark(param);
                }

                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (countsAsRun(param)) mode.setInt(param.thisObject, DIRECTOR);
                }
            });
            HookDiagnostics.hookMethod(beforeAdapter, new WheelHook() {
                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (param.hasThrowable() || param.thisObject != wheelOwner.get()) return;
                    // Only the wheel adapter is borrowed from Pro Video. The layout and camera
                    // remain owned by the director.
                    mode.setInt(param.thisObject, PRO_VIDEO);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(satProZoom, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    Object fragment = wheelOwner.get();
                    if (fragment == null || mode.getInt(fragment) != PRO_VIDEO
                            || (int) param.args[0] != PRO_VIDEO) return;
                    param.setResult(false);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(configureLensRange, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    Object fragment = wheelOwner.get();
                    if (fragment == null || mode.getInt(fragment) != PRO_VIDEO
                            || (int) param.args[1] != PRO_VIDEO) return;
                    param.args[1] = DIRECTOR;
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(setRange, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object fragment = owner.get(param.thisObject);
                    if (fragment == null || fragment != wheelOwner.get()
                            || adapterMode.getInt(param.thisObject) != PRO_VIDEO
                            || mode.getInt(fragment) != PRO_VIDEO) return;
                    mode.setInt(fragment, DIRECTOR);
                    Object zoom = ((Optional<?>) zoomProtocol.invoke(null)).orElse(null);
                    Object module = ((Optional<?>) getBaseModule.invoke(fragment)).orElse(null);
                    int id = module == null ? -1 : (int) actualCameraId.invoke(cameraManager.invoke(module));
                    Range<Float> videoRange = id < 0 ? zoomRange(param.args[0])
                            : zoomRange(rangeForVideoLens.invoke(null, id, DIRECTOR));
                    Range<Float> activeRange = zoom == null ? null : zoomRange(activeZoomRange.invoke(zoom));
                    Range<Float> range = compatibleRange(videoRange, activeRange);
                    if (range != null) param.args[0] = range;
                    Log.i("AuroraDirectorZoom", "adapter=" + param.thisObject.getClass().getSimpleName()
                            + " camera=" + id + " video=" + videoRange + " active=" + activeRange
                            + " wheel=" + range);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(alignLabels, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    if (adapterMode.getInt(param.thisObject) != PRO_VIDEO
                            || mode.getInt(owner.get(param.thisObject)) != DIRECTOR) return;
                    landscapeDirectorTicks.setBoolean(param.thisObject, false);
                    portraitDirectorTicks.setBoolean(param.thisObject, false);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(rotateLabels, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object wheel = activeAdapter.invoke(param.thisObject);
                    if (!adapter.isInstance(wheel) || adapterMode.getInt(wheel) != PRO_VIDEO) return;
                    Object fragment = owner.get(wheel);
                    if (mode.getInt(fragment) != DIRECTOR || (boolean) isLandscape.invoke(fragment)) return;
                    // The director's native path can send a quarter turn for its sideways wheel.
                    // The portrait arc uses the current portrait degree.
                    landscapeDirectorTicks.setBoolean(wheel, false);
                    portraitDirectorTicks.setBoolean(wheel, false);
                    param.args[0] = fragmentDegree.invoke(fragment);
                    mark(param);
                }
            });
            HookDiagnostics.hookMethod(backgroundLayout, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    param.setObjectExtra(SCOPE, videoCover.get());
                    if (mode.getInt(param.args[0]) != DIRECTOR) return;
                    videoCover.set(param.args[2]);
                    mode.setInt(param.args[0], VIDEO);
                    mark(param);
                }

                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (countsAsRun(param)) mode.setInt(param.args[0], DIRECTOR);
                    restore(videoCover, param.getObjectExtra(SCOPE));
                }
            });
            HookDiagnostics.hookMethod(showBackground, new WheelHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    if (!(boolean) param.args[1]) return;
                    boolean zoom = param.thisObject == videoCover.get();
                    int currentMode = mode.getInt(param.thisObject);
                    if (!zoom && currentMode != DIRECTOR) return;
                    View view = (View) cover.get(param.thisObject);
                    if (view == null) return;
                    directorCover.setBoolean(view, !zoom);
                    landscapeCover.setBoolean(view, !zoom && (boolean) isLandscape.invoke(param.thisObject));
                    if (zoom) {
                        param.setObjectExtra("director-background-mode", currentMode);
                        mode.setInt(param.thisObject, VIDEO);
                        mark(param);
                    }
                }

                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (countsAsRun(param)) {
                        mode.setInt(param.thisObject, (int) param.getObjectExtra("director-background-mode"));
                    }
                }
            });
        };
        return new HookPlan() {
            @Override public String id() { return "DirectorVideoZoom"; }
            @Override public void install() { install.run(); }
        };
    }

    @SuppressWarnings("unchecked")
    private static Range<Float> zoomRange(Object range) {
        return (Range<Float>) range;
    }

    private static Range<Float> compatibleRange(Range<Float> video, Range<Float> physical) {
        if (video == null || physical == null) return physical == null ? video : physical;
        float lower = Math.max(video.getLower(), physical.getLower());
        float upper = Math.min(video.getUpper(), physical.getUpper());
        return lower <= upper ? Range.create(lower, upper) : physical;
    }

    private static void restore(ThreadLocal<Object> scope, Object previous) {
        if (previous == null) scope.remove();
        else scope.set(previous);
    }

    private abstract static class WheelHook extends StrictHook {
        final void mark(MethodHookParam param) {
            param.setObjectExtra("director-video-zoom-applied", Boolean.TRUE);
        }

        @Override protected boolean countsAsRun(MethodHookParam param) {
            return Boolean.TRUE.equals(param.getObjectExtra("director-video-zoom-applied"));
        }
    }
}
