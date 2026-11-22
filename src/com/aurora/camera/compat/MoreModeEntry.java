package com.aurora.camera.compat;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.ColorSpace;
import android.graphics.RectF;
import android.hardware.HardwareBuffer;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.ImageReader;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedReplacement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Native 1770 registrations and the matching Aurora runtime paths. */
final class MoreModeEntry implements HookPlan {
    private static final int DIRECTOR_MODE = 164;
    private static final int DIRECTOR_DISPLAY_MODE = 517;
    private static final int RECORDING_TIMER_ID = 0x7f0b0672; // ll_alert_recording_time_view
    private static final int COMMON_TOP_ALERT_LAYOUT = 0x7f0e01bf; // f6o
    private static final int GRAPH_CONTAINER = 0x7f0b0902; // pro_parameter_graph_container
    private static final int LIGHT_GRAPH_ROOT = 0x7f0b0905; // pro_parameter_light_graph_root_container
    private static final int AUDIO_GRAPH_CONTAINER = 0x7f0b0901; // pro_parameter_audio_graph_container
    private static final int WAVEFORM_TEXTURE = 0x7f0b073f; // mishot_gl_textureview
    private static final int PHOTO_MODE = 163;
    private static final int CLONE_CAPTURE_MODE = 185;
    private static final int CLONE_GALLERY_MODE = 210;
    private static final int MODE_SELECTOR_CONTAINER = 2;
    private static final int SHOW_CONTAINER = 20;
    private static final String CLONE_SCOPE = "clone-resource-extraction";
    private static final String CLONE_RESTORE_SELECTOR = "clone-restore-selector";
    private static final String CLONE_RESTORED_SELECTOR = "clone-restored-selector";
    private static final String PIXEL_SCOPE = "mirror-pixel-format";
    private static final String CLONE_MODEL_DIRECTORY = "2.3.1_3794/";

    private final Method support;
    private final Runnable installCompatibility;

    private MoreModeEntry(Method support, Runnable installCompatibility) {
        this.support = support;
        this.installCompatibility = installCompatibility;
    }

    static HookPlan mirror(Host host) throws ReflectiveOperationException {
        String prefix = "com.android.camera.features.mode.cosmeticmirror.CosmeticMirrorModule";
        Method support = host.method(prefix + "Entry", "support");
        Method operatingMode = host.method("E3.s", "i", host.type("y3.w"));
        Method readImage = host.method("Tu.E$b", "onImageAvailable", ImageReader.class);
        Class<?> previewSaver = host.type("com.android.camera.module.e0");
        Method savePixels = host.method(previewSaver, "c", byte[].class, int.class, int.class,
                int.class, ColorSpace.class);
        Field mirrorOwner = host.field(previewSaver, "f");
        ThreadLocal<Boolean> fromTenBitBuffer = new ThreadLocal<>();
        return new MoreModeEntry(support, () -> {
            // Aurora's 0x900a path requires a still stream, but the mirror uses only preview.
            HookDiagnostics.hookMethod(operatingMode,
                    ObservedReplacement.returnConstant(SessionConfiguration.SESSION_REGULAR));
            HookDiagnostics.hookMethod(readImage, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    param.setObjectExtra(PIXEL_SCOPE, fromTenBitBuffer.get());
                    ImageReader reader = (ImageReader) param.args[0];
                    fromTenBitBuffer.set(reader.getHardwareBufferFormat() == HardwareBuffer.RGBA_1010102);
                }

                @Override protected void after(MethodHookParam param) {
                    Boolean previous = (Boolean) param.getObjectExtra(PIXEL_SCOPE);
                    if (previous == null) fromTenBitBuffer.remove();
                    else fromTenBitBuffer.set(previous);
                }
            });
            HookDiagnostics.hookMethod(savePixels, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    if (!Boolean.TRUE.equals(fromTenBitBuffer.get()) || mirrorOwner.get(param.thisObject) == null) return;

                    // Native readback already extracts 8-bit RGB. Its fourth byte is only 0..3.
                    // A mirror screenshot is opaque and the saver expects 8-bit alpha.
                    byte[] rgba = (byte[]) param.args[0];
                    for (int offset = 3; offset < rgba.length; offset += 4) rgba[offset] = (byte) 0xff;
                    param.setObjectExtra(PIXEL_SCOPE, Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(PIXEL_SCOPE));
                }
            });
        });
    }

    static HookPlan director(Host host) throws ReflectiveOperationException {
        Method support = host.method("com.android.camera.features.mode.cinemaster.CinemasterModuleEntry", "support");
        Method savedTopBar = host.method("u2.y", "W", int.class);
        Method directorTopItems = host.method("C3.b", "e");
        Field itemGravity = host.field("b5.i", "a");
        Field itemId = host.field("b5.i", "c");
        Method selectedMode = host.method("u2.Y", "D", int.class);
        Field intentType = host.field("u2.Y", "u");
        Field activeMode = host.field("com.android.camera.module.c0", "a");
        Method initManualScale = host.method("R4.a", "P");
        Method initApertureScale = host.method("da.a", "t", Context.class);
        Method initZoomScale = host.method("com.android.camera2.compat.theme.custom.mm.zoom.b", "t", Context.class);
        Field manualScaleMode = host.field("R4.a", "G0");
        Class<?> scaleAdapter = host.type("com.android.camera2.compat.theme.custom.mm.zoom.a$b");
        Field scaleTick = host.field(scaleAdapter, "Y");
        Field scaleMajorTick = host.field(scaleAdapter, "Z");
        Method directorControlsLayout = host.method("z4.n", "b", int.class, boolean.class);
        Method refreshDirectorControl = host.method("z4.n", "c", int.class);
        Field directorControls = host.field("z4.n", "b");
        Class<?> flashConfig = host.type("r2.w");
        Class<?> iconBuilder = host.type("b5.j$a");
        Method buildFlashIcon = host.method("r9.o", "l", iconBuilder, flashConfig, int.class);
        Method flashDrawable = host.method(flashConfig, "E", int.class);
        Field staticIcon = host.field(iconBuilder, "a");
        Method initAlerts = host.method("X9.i", "initView", View.class);
        return new MoreModeEntry(support, () -> {
            HookDiagnostics.hookMethod(savedTopBar, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    // Keep the mode's own video quality item. The generic item factory rejects ID 208.
                    if (countsAsRun(param)) param.setResult(Collections.emptyList());
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return (int) param.args[0] == DIRECTOR_MODE;
                }
            });
            HookDiagnostics.hookMethod(directorTopItems, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable()) return;
                    // The native layout pads around the More item by list index. Its Start items
                    // must precede More to occupy the left slots instead of crowding the center.
                    List<?> items = (List<?>) param.getResult();
                    ArrayList<Object> ordered = new ArrayList<>(items.size());
                    int startCount = 0;
                    for (Object item : items) {
                        if (itemGravity.getInt(item) == Gravity.START) ordered.add(startCount++, item);
                        else ordered.add(item);
                    }
                    param.setResult(ordered);
                }
            });
            HookDiagnostics.hookMethod(initManualScale, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (param.hasThrowable() || manualScaleMode.getInt(param.thisObject) != DIRECTOR_MODE) return;

                    // The shared painter uses Z for major ticks. The director scale computes
                    // one tick size for both passes, but only initializes the ordinary bounds.
                    scaleMajorTick.set(param.thisObject, new RectF((RectF) scaleTick.get(param.thisObject)));
                    param.setObjectExtra("director-scale-initialized", Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-scale-initialized"));
                }
            });
            StrictHook initDirectorMajorTick = new StrictHook() {
                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (param.hasThrowable()) return;
                    // Both director adapters use the same native bounds for ordinary and major ticks.
                    scaleMajorTick.set(param.thisObject, new RectF((RectF) scaleTick.get(param.thisObject)));
                }
            };
            HookDiagnostics.hookMethod(initApertureScale, initDirectorMajorTick);
            HookDiagnostics.hookMethod(initZoomScale, initDirectorMajorTick);
            HookDiagnostics.hookMethod(directorControlsLayout, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable()) return;
                    List<?> controls = (List<?>) directorControls.get(param.thisObject);
                    if (controls == null) return; // No camera capabilities, so no controls were created.
                    for (int index = 0; index < controls.size(); index++) {
                        if (itemId.getInt(controls.get(index)) == DIRECTOR_DISPLAY_MODE) {
                            refreshDirectorControl.invoke(param.thisObject, index);
                            return;
                        }
                    }
                }
            });
            HookDiagnostics.hookMethod(buildFlashIcon, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable() || (int) param.args[2] != DIRECTOR_MODE) return;
                    Object icon = param.getResult();
                    if (staticIcon.getInt(icon) > 0) return;

                    // The director's ImageView reads the drawable, while the shared flash
                    // builder provides only an animation for a two-state flash component.
                    int drawable = (int) flashDrawable.invoke(param.args[1], DIRECTOR_MODE);
                    if (drawable > 0) {
                        staticIcon.setInt(icon, drawable);
                        param.setObjectExtra("director-flash-drawable", Boolean.TRUE);
                    }
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-flash-drawable"));
                }
            });
            HookDiagnostics.hookMethod(selectedMode, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    // The saved-mode reader migrates 164 to Pro Video. Live settings must keep
                    // the director mode selected when reconfiguring resolution or frame rate.
                    if (activeMode.getInt(null) != DIRECTOR_MODE
                            || (int) param.args[0] != intentType.getInt(param.thisObject)) return;
                    param.setResult(DIRECTOR_MODE);
                    param.setObjectExtra("director-active-mode", Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-active-mode"));
                }
            });
            HookDiagnostics.hookMethod(initAlerts, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    ViewGroup layout = (ViewGroup) param.args[0];
                    ViewGroup common = (ViewGroup) LayoutInflater.from(layout.getContext())
                            .inflate(COMMON_TOP_ALERT_LAYOUT, layout, false);

                    // Supply the inherited alert widgets from the matching 1770 layout.
                    View timer = detach(common, RECORDING_TIMER_ID);
                    int timerPosition = layout.indexOfChild(layout.findViewById(RECORDING_TIMER_ID));
                    layout.removeViewAt(timerPosition);
                    layout.addView(timer, timerPosition);

                    ViewGroup graphs = (ViewGroup) detach(common, GRAPH_CONTAINER);
                    detach(layout, LIGHT_GRAPH_ROOT);
                    detach(graphs, WAVEFORM_TEXTURE);
                    detach(graphs, AUDIO_GRAPH_CONTAINER);
                    graphs.addView(detach(layout, AUDIO_GRAPH_CONTAINER), 0);

                    // The director draws its own light graph. Keep the inherited graph available
                    // by ID for the shared alert code without attaching it to the visible layout.
                    View inheritedLightGraph = detach(graphs, LIGHT_GRAPH_ROOT);
                    FrameLayout hiddenLightGraph = new FrameLayout(layout.getContext());
                    hiddenLightGraph.setVisibility(View.GONE);
                    hiddenLightGraph.addView(inheritedLightGraph, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    graphs.addView(hiddenLightGraph);
                    layout.addView(graphs);
                }
            });
        });
    }

    private static View detach(ViewGroup root, int id) {
        View view = root.findViewById(id);
        ((ViewGroup) view.getParent()).removeView(view);
        return view;
    }

    static HookPlan clone(Host host) throws ReflectiveOperationException {
        Method support = host.method("Pe.b", "K0");
        Method initResources = host.method("com.xiaomi.fenshen.CloneUtil", "initResources", Context.class);
        Class<?> cloneProcess = host.type("com.android.camera.fragment.clone.b");
        Method animateMode = host.method(cloneProcess, "provideAnimateElement", int.class, List.class, int.class);
        Method fragmentId = host.method(cloneProcess, "getFragmentId");
        Field fragmentMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        Method featureUi = host.method("R6.j0", "a");
        Method operateContainer = host.method("R6.j0", "d", int.class, int.class, int.class);
        Method openAsset = AssetManager.class.getDeclaredMethod("open", String.class);
        ThreadLocal<Boolean> extractingModel = new ThreadLocal<>();
        return new MoreModeEntry(support, () -> {
            HookDiagnostics.hookMethod(animateMode, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    if ((int) param.args[0] != PHOTO_MODE) return;
                    int previousMode = fragmentMode.getInt(param.thisObject);
                    if (previousMode == CLONE_CAPTURE_MODE || previousMode == CLONE_GALLERY_MODE) {
                        param.setObjectExtra(CLONE_RESTORE_SELECTOR, Boolean.TRUE);
                    }
                }

                @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                    if (param.hasThrowable() || !Boolean.TRUE.equals(param.getObjectExtra(CLONE_RESTORE_SELECTOR))) return;
                    Optional<?> protocol = (Optional<?>) featureUi.invoke(null);
                    if (protocol.isPresent()) {
                        // Pair CloneProcess's container hide with the same owner used by its native quit path.
                        operateContainer.invoke(protocol.get(), MODE_SELECTOR_CONTAINER,
                                fragmentId.invoke(param.thisObject), SHOW_CONTAINER);
                        param.setObjectExtra(CLONE_RESTORED_SELECTOR, Boolean.TRUE);
                    }
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(CLONE_RESTORED_SELECTOR));
                }
            });
            HookDiagnostics.hookMethod(initResources, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    param.setObjectExtra(CLONE_SCOPE, extractingModel.get());
                    extractingModel.set(Boolean.TRUE);
                }

                @Override protected void after(MethodHookParam param) {
                    if (param.getObjectExtra(CLONE_SCOPE) == null) extractingModel.remove();
                    else extractingModel.set(Boolean.TRUE);
                }
            });
            HookDiagnostics.hookMethod(openAsset, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    if (!Boolean.TRUE.equals(extractingModel.get())) return;
                    String path = (String) param.args[0];
                    // Use the official plug-in's SNPE 2.x model and its matching cache version.
                    if ("default/xseg2_dlc".equals(path) || "default/snpe_model.json".equals(path)) {
                        param.args[0] = CLONE_MODEL_DIRECTORY + path.substring("default/".length());
                    }
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(extractingModel.get())
                            && ((String) param.args[0]).startsWith(CLONE_MODEL_DIRECTORY);
                }
            });
        });
    }

    @Override public String id() { return support.getDeclaringClass().getName() + "." + support.getName(); }

    @Override public void install() {
        installCompatibility.run();
        HookDiagnostics.hookMethod(support, ObservedReplacement.returnConstant(true));
    }
}
