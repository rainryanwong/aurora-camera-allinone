package com.aurora.camera.compat;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class DirectorRecording {
    private static final int MODE = 164;
    private static final int QUALITY = 208;
    private static final int QUALITY_TEXT_LAYOUT = 1;
    private static final int TOP_CONFIG_COLOR = 0x7f060b96;
    private static final String QUALITY_SCOPE = "director-quality-view";

    private DirectorRecording() {}

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        Class<?> quality = host.type("r2.h0");
        Method qualityKey = host.method(quality, "getKey", int.class);
        Method dataConfig = host.method("g2.a", "a");
        Method component = host.method("Xh.b", "x", Class.class);
        Field resolution = host.field(quality, "g");
        Field frameRate = host.field(quality, "h");
        Method nextResolution = host.method(resolution.getType(), "n", int.class);
        Method nextFrameRate = host.method(frameRate.getType(), "o", int.class);
        Method applyResolution = host.method("X9.i0", "G2", String.class);
        Method applyFrameRate = host.method("X9.i0", "B2", String.class);
        Method clickResolution = host.method("X9.i0", "Gq", View.class);
        Method clickFrameRate = host.method("X9.i0", "E9", View.class);
        Field topMode = host.field("X9.i0", "k");
        Field itemId = host.field("b5.i", "c");

        String top = "com.android.camera2.compat.theme.custom.mm.top.topbarview.TopBarView";
        Method obtainHolder = host.method(top + "$h", "a", int.class);
        Field holderParent = host.field(top + "$h", "b");
        Field scrap = host.field(top + "$h", "a");
        Field adapter = host.field(top, "h");
        Field adapterItems = host.field("ba.q", "e");
        Field holderView = host.field(top + "$l", "d");
        Field holderPosition = host.field(top + "$l", "e");
        Field holderLayoutPosition = host.field(top + "$l", "g");
        Method markHolder = host.method(top + "$l", "a", int.class);
        Method inflate = host.method("ba.u", "f", ViewGroup.class, int.class);
        Method bindIconHolder = host.method("ba.f", "g", host.type("b5.j"));
        Class<?> qualityView = host.type("com.xiaomi.camera.ui.base.top.ui.topbar.view.VideoQualityTextView");
        Method setTexts = host.method(qualityView, "setTexts", String[].class);
        Method setTextColor = host.method(qualityView, "setTextColor", int.class);
        Field qualityLabels = host.field("b5.j", "c");
        Field themeColorFlag = host.field("b5.j", "m");
        Field themeColors = host.field("f2.e", "c");
        Method color = host.method("f2.e", "a", int.class, boolean.class);
        ThreadLocal<Object> qualityParent = new ThreadLocal<>();
        Class<?> director = host.type("C3.m");
        Class<?> manager = host.type("k6.k");
        Method tagMode = host.method("y3.e", "p", manager);
        Method videoQuality = host.method("com.android.camera.data.data.n", "w", int.class, int.class);
        Method cameraFacing = host.method(manager, "n0");
        Method requestState = host.method(manager, "K0");
        Field settings = host.field("l9.i0", "a");
        Field overrideMode = host.field("l9.j0", "K3");
        Field sessionParams = host.field("l9.i0", "b");
        Object moduleTag = host.field("ia.y0", "Y").get(null);
        Method writeTag = host.method("l9.z1", "a", host.type("ia.D0"), Object.class);

        Runnable install = () -> {
            HookDiagnostics.hookMethod(tagMode, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    if (!director.isInstance(param.thisObject)) return;
                    int facing = (int)cameraFacing.invoke(param.args[0]);
                    if ((int)videoQuality.invoke(null, facing, MODE) != 3001) return;
                    Object state = requestState.invoke(param.args[0]);
                    int requested = overrideMode.getInt(settings.get(state));
                    if (requested != 0 && requested != MODE) return;
                    // Select the physical-lens pro-video tuning for the director's 8K session.
                    writeTag.invoke(sessionParams.get(state), moduleTag, 180);
                    param.setResult(null);
                    param.setObjectExtra("director-8k", true);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-8k"));
                }
            });
            HookDiagnostics.hookMethod(qualityKey, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    if (countsAsRun(param)) param.setResult("pref_camera_cinemaster_quality");
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return (int) param.args[0] == MODE;
                }
            });
            HookDiagnostics.hookMethod(obtainHolder, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object parent = holderParent.get(param.thisObject);
                    List<?> items = (List<?>) adapterItems.get(adapter.get(parent));
                    int position = (int) param.args[0];
                    boolean combined = items != null && !items.isEmpty()
                            && itemId.getInt(items.get(position)) == QUALITY;
                    param.setObjectExtra(QUALITY_SCOPE, qualityParent.get());
                    if (combined) qualityParent.set(parent);
                    else qualityParent.remove();

                    // The recycler keys scrap by position, even when the item at that position changes.
                    for (Object holder : (List<?>) scrap.get(param.thisObject)) {
                        int oldPosition = holderLayoutPosition.getInt(holder);
                        if (oldPosition == -1) oldPosition = holderPosition.getInt(holder);
                        if (oldPosition == position && qualityView.isInstance(holderView.get(holder)) != combined) {
                            markHolder.invoke(holder, 4);
                        }
                    }
                    param.setObjectExtra("director-quality-slot", combined);
                }

                @Override protected void after(MethodHookParam param) {
                    Object previous = param.getObjectExtra(QUALITY_SCOPE);
                    if (previous == null) qualityParent.remove();
                    else qualityParent.set(previous);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-quality-slot"));
                }
            });
            HookDiagnostics.hookMethod(inflate, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    if (countsAsRun(param)) param.args[1] = QUALITY_TEXT_LAYOUT;
                }

                @Override protected void after(MethodHookParam param) {
                    if (param.hasThrowable() || !countsAsRun(param)) return;
                    View view = (View) param.getResult();
                    ((TextView) view.findViewById(1)).setSingleLine(true);
                    ((TextView) view.findViewById(2)).setSingleLine(true);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return param.args[0] == qualityParent.get();
                }
            });
            HookDiagnostics.hookMethod(bindIconHolder, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable()) return;
                    View view = (View) holderView.get(param.thisObject);
                    if (!qualityView.isInstance(view)) return;
                    Object data = param.args[0];
                    String[] labels = (String[]) qualityLabels.get(data);
                    int tint = (int) color.invoke(themeColors.get(null), TOP_CONFIG_COLOR,
                            themeColorFlag.getBoolean(data));
                    setTextColor.invoke(view, tint);
                    // The legacy director presents the values as 1080 and 30 in one compact group.
                    setTexts.invoke(view, (Object) new String[] {
                            shortLabel(labels[0], "P"), shortLabel(labels[1], "FPS")
                    });
                    param.setObjectExtra(QUALITY_SCOPE, Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(QUALITY_SCOPE));
                }
            });

            StrictHook cycleQuality = new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    View view = (View) param.args[0];
                    if (topMode.getInt(param.thisObject) != MODE || view == null
                            || !qualityView.isInstance(view.getParent())) return;
                    Object model = component.invoke(dataConfig.invoke(null), quality);
                    boolean changeResolution = param.method.equals(clickResolution);
                    Object axis = (changeResolution ? resolution : frameRate).get(model);
                    String next = (String) (changeResolution ? nextResolution : nextFrameRate).invoke(axis, MODE);
                    (changeResolution ? applyResolution : applyFrameRate).invoke(param.thisObject, next);
                    param.setResult(null);
                    param.setObjectExtra("director-quality-cycle", Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-quality-cycle"));
                }
            };
            HookDiagnostics.hookMethod(clickResolution, cycleQuality);
            HookDiagnostics.hookMethod(clickFrameRate, cycleQuality);

        };
        return new HookPlan() {
            @Override public String id() { return "DirectorRecording"; }
            @Override public void install() { install.run(); }
        };
    }

    private static String shortLabel(String label, String suffix) {
        if (label == null) return null;
        int end = label.length() - suffix.length();
        return end > 0 && label.regionMatches(true, end, suffix, 0, suffix.length())
                ? label.substring(0, end).trim() : label;
    }

}
