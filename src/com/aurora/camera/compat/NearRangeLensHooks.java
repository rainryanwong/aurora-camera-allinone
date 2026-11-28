package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CaptureResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.aurora.camera.compat.ui.MiuixDialogs;
import com.aurora.camera.compat.ui.MiuixSeekBar241;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedHook;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class NearRangeLensHooks implements HookPlan {
    private static final String KEY = "aurora_near_range_75mm";
    private static final String DISTANCE_ROW = KEY + "_distance";
    private static final String ENTER_KEY = KEY + "_enter_cm";
    private static final String EXIT_KEY = KEY + "_exit_cm";
    private static final String NATIVE_KEY = "pref_camera_near_range_fallback_key";
    private static final String STATE = "aurora.near-range-lens";
    private static final String HANDLED = STATE + ".handled";
    private static final String TAG = "AuroraNearRangeLens";
    private static final int PHOTO = 163;
    private static final int AI_PHOTO = 168;
    private static final String DEVICE_OWNER = STATE + ".owner";
    private static final int UPDATE_FALLBACK = 77;
    private static final int DIRECT_ZOOM = 25;
    private static final int NATIVE_NEAR_CLICK = 12;
    private static final int DEFAULT_ENTER_CM = 15;
    private static final int DEFAULT_EXIT_CM = 25;
    private static final int MIN_ENTER_CM = 10;
    private static final int MAX_ENTER_CM = 25;
    private static final int MIN_EXIT_CM = 20;
    private static final int MAX_EXIT_CM = 35;
    private static final int MIN_GAP_CM = 5;
    private static final float NEAR_ZOOM_LIMIT = 3.19f;
    private static final float TELE_ZOOM = 3.2f;
    private static final int STABLE_SAMPLES = 3;
    private static final CaptureResult.Key<byte[]> TOF =
            new CaptureResult.Key<>("xiaomi.sat.tofInfo.tofData", byte[].class);

    private volatile int enterDistance = DEFAULT_ENTER_CM;
    private volatile int exitDistance = DEFAULT_EXIT_CM;
    private final Options229 options;
    private final MiuixDialogs dialogs;
    private final MiuixSeekBar241 sliders;
    private final Method settings, update, preview, inactive, nearClick;
    private final Method nativeEnabled, photoEnabled, moduleIndex, alive, busy;
    private final Method capabilities, physicalIds, supported, registry, teleId, zoomRange;
    private final Method streetMode, streetTable, wideFinder, streetRing, selectFocal, focalLength;
    private final Field deviceConfig, streetZoom, finderZoom;
    private final Method getDevice, setFallbackDisabled, getZoom, ratio, changeZoom, saveZoom;
    private final Field managerModule, clickKind;
    private final NearRangeUi ui;
    private final Handler main = new Handler(Looper.getMainLooper());

    NearRangeLensHooks(Host host) throws ReflectiveOperationException {
        options = new Options229(host);
        dialogs = new MiuixDialogs(host);
        sliders = new MiuixSeekBar241(host);
        ui = new NearRangeUi(host);
        String module = "com.android.camera.module.u";
        String zoom = "h9.a";
        settings = host.method("com.android.camera.fragment.settings.common.AutoFallbackFragment",
                "registerPreferenceListener");
        update = host.method("k6.e", "f1", int.class);
        managerModule = host.field("k6.e", "b");
        getDevice = host.method("k6.e", "T");
        setFallbackDisabled = host.method("l9.y0", "r0", boolean.class);
        capabilities = host.method("l9.a", "q");
        physicalIds = host.method("l9.g", "J");
        supported = host.method("l9.h", "o5", host.type("l9.g"));
        registry = host.method("v6.f", "R");
        teleId = host.method("v6.f", "q");
        zoomRange = host.method(zoom, "H0");
        streetMode = host.method("com.android.camera.module.c0", "l", int.class);
        deviceConfig = host.field("Pe.b$b", "a");
        streetTable = host.method("Pe.b", "f");
        wideFinder = host.method("com.android.camera.data.data.n", "A");
        streetZoom = host.field("Re.b", "b");
        finderZoom = host.field("Re.b", "a");
        streetRing = host.method("R6.H1", "a");
        selectFocal = host.method("R6.H1", "Yl", int.class, int.class);
        focalLength = host.method("com.android.camera.data.data.n", "k", float.class, int.class);
        preview = host.method(module, "onPreviewMetaDataUpdate", CaptureResult.class);
        inactive = host.method(module, "onInactive");
        nativeEnabled = host.method("com.android.camera.data.data.x", "q0");
        photoEnabled = host.method("com.android.camera.data.data.i", "X0", int.class);
        nearClick = host.method("H4.x", "accept", Object.class);
        clickKind = host.field("H4.x", "a");
        moduleIndex = host.method(module, "getModuleIndex");

        alive = host.method(module, "isDeviceAndModuleAlive");
        busy = host.method("com.android.camera.module.Camera2Module", "isDoingAction");
        getZoom = host.method(module, "getZoomManager");
        ratio = host.method(zoom, "e1");
        changeZoom = host.method(zoom, "y0", float.class, int.class);
        saveZoom = host.method("com.android.camera.data.data.F", "D0", float.class, int.class);
    }

    @Override public String id() {
        return "near-range-lens";
    }

    @Override public void install() {
        HookDiagnostics.hookMethod(settings, new StrictHook() {
            @Override protected void after(MethodHookParam param) {
                if (param.hasThrowable()) return;
                Object root = XposedHelpers.getObjectField(param.thisObject, "mPreferenceGroup");
                if (XposedHelpers.callMethod(root, "k0", NATIVE_KEY) != null) {
                    options.addSwitch(param.thisObject, KEY, "近距自适应镜头切换优化",
                            "打开近距自适应开关时，将自动切换至超广角镜头的策略，改为切换至75mm中焦镜头",
                            NATIVE_KEY, null);
                    Object fragment = param.thisObject;
                    Object row = options.addAction(fragment, DISTANCE_ROW, "75mm 切换距离",
                            distanceSummary(), KEY, () -> showDistances(fragment));
                    XposedHelpers.callMethod(row, "c0", distanceSummary());
                }
            }
        });
        HookDiagnostics.hookMethod(update, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                if ((Integer) param.args[0] != UPDATE_FALLBACK) return;
                Object module = managerModule.get(param.thisObject);
                Object device = getDevice.invoke(param.thisObject);
                if (device != null && busy.getDeclaringClass().isInstance(module)) bind(module, device);
            }
        });
        HookDiagnostics.hookMethod(setFallbackDisabled, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                Object device = param.thisObject;
                ZoomState state = (ZoomState) XposedHelpers.getAdditionalInstanceField(device, DEVICE_OWNER);
                if (state == null) return;
                boolean enabled = !(boolean) param.args[0] && options.enabled(KEY)
                        && (boolean) nativeEnabled.invoke(null);
                boolean wasEnabled = state.enabled;
                state.enabled = enabled;
                if (enabled) param.args[0] = true;
                else if (wasEnabled) main.post(() -> endRoute(state));
            }
        });
        HookDiagnostics.hookMethod(ui.showTip, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam param) throws ReflectiveOperationException {
                if ((int) param.args[0] != NearRangeUi.TIP_ID || ui.rendering.get() != null
                        || !ownsTip(ui.owner(param.thisObject))) return;
                // The stock near-range interceptor drives this slot from the ultrawide HAL role.
                param.setResult(null);
                param.setObjectExtra(HANDLED, Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HANDLED));
            }
        });
        HookDiagnostics.hookMethod(photoEnabled, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam param) {
                Boolean rendering = ui.rendering.get();
                if (hasTip((int) param.args[0]) && rendering != null) {
                    param.setResult(rendering);
                    param.setObjectExtra(HANDLED, Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HANDLED));
            }
        });
        HookDiagnostics.hookMethod(nearClick, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam param) throws ReflectiveOperationException {
                if (clickKind.getInt(param.thisObject) != NATIVE_NEAR_CLICK) return;
                Object module = param.args[0];
                if (!ownsTip(module)) return;
                param.setResult(null);
                ZoomState state = stateOf(module);
                if (state == null || !state.nearby() || !(boolean) alive.invoke(module)
                        || (boolean) busy.invoke(module)) return;
                Object zoom = getZoom.invoke(module);
                if ((float) ratio.invoke(zoom) != state.expectedZoom()) return;
                boolean wasTele = !state.mainSelected;
                if (switchZoom(zoom, state, wasTele ? state.previous : state.target, state.distance)) {
                    state.mainSelected = wasTele;
                    state.samples = 0;
                    ui.showToggleMessage(wasTele);
                    param.setObjectExtra(HANDLED, Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HANDLED));
            }
        });
        HookDiagnostics.hookMethod(preview, new ObservedHook() {
            private volatile boolean sampled;
            @Override protected void runAfter(MethodHookParam param) throws ReflectiveOperationException {
                CaptureResult result = (CaptureResult) param.args[0];
                Object module = param.thisObject;
                if (param.hasThrowable() || result == null || result.getFrameNumber() % 3 != 0) return;
                ZoomState state = stateOf(module);
                if (state == null || !state.enabled) return;
                float distance = distanceCm(result.get(TOF));
                if (!sampled) sampled = true;
                offerSample(module, state, distance);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return sampled;
            }
        });
        HookDiagnostics.hookMethod(inactive, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                ZoomState state = (ZoomState) XposedHelpers.removeAdditionalInstanceField(param.thisObject, STATE);
                if (state == null) return;
                detach(state);
                if (state.nearby() && !state.mainSelected) {
                    Object zoom = getZoom.invoke(param.thisObject);
                    if (zoom != null && (Float) ratio.invoke(zoom) == state.target) {
                        saveZoom.invoke(null, state.previous, state.mode);
                    }
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void bind(Object module, Object device) throws ReflectiveOperationException {
        ZoomState prior = stateOf(module);
        if (prior != null && prior.device.get() == device) return;
        if (prior != null) {
            detach(prior);
            XposedHelpers.removeAdditionalInstanceField(module, STATE);
        }
        if (!options.enabled(KEY)) return;
        Object caps = capabilities.invoke(device);
        if (!(boolean) supported.invoke(null, caps)) return;
        Object rearTele = teleId.invoke(registry.invoke(null));
        if (!((java.util.Set<?>) physicalIds.invoke(caps)).contains(String.valueOf(rearTele))) return;
        int mode = (int) moduleIndex.invoke(module);
        float target = TELE_ZOOM;
        if ((boolean) streetMode.invoke(null, mode)) {
            android.util.SparseArray<?> table = (android.util.SparseArray<?>) streetTable.invoke(deviceConfig.get(null));
            Object entry = table == null ? null : table.get(75);
            if (entry == null) return;
            target = ((boolean) wideFinder.invoke(null) ? finderZoom : streetZoom).getFloat(entry);
        }
        Object zoom = getZoom.invoke(module);
        android.util.Range<Float> range = zoom == null ? null : (android.util.Range<Float>) zoomRange.invoke(zoom);
        if (range == null || !range.contains(target)) return;
        SharedPreferences prefs = options.prefs();
        enterDistance = prefs.getInt(ENTER_KEY, DEFAULT_ENTER_CM);
        exitDistance = prefs.getInt(EXIT_KEY, DEFAULT_EXIT_CM);
        ZoomState state = new ZoomState(module, device, mode, target);
        XposedHelpers.setAdditionalInstanceField(module, STATE, state);
        XposedHelpers.setAdditionalInstanceField(device, DEVICE_OWNER, state);
    }

    private static void detach(ZoomState state) {
        state.enabled = false;
        Object device = state.device.get();
        if (device != null && XposedHelpers.getAdditionalInstanceField(device, DEVICE_OWNER) == state)
            XposedHelpers.removeAdditionalInstanceField(device, DEVICE_OWNER);
    }

    private void endRoute(ZoomState state) {
        Object module = state.module.get();
        if (module == null || stateOf(module) != state || state.enabled) return;
        try {
            if (state.nearby() && !state.mainSelected && (boolean) alive.invoke(module)) {
                Object zoom = getZoom.invoke(module);
                if ((float) ratio.invoke(zoom) == state.target) switchZoom(zoom, state, state.previous, state.distance);
            }
            state.leave();
            state.manualOverride = false;
            if (hasTip(state.mode)) ui.renderTip(module, false, false);
        } catch (ReflectiveOperationException error) {
            Log.e(TAG, "Unable to leave near-range zoom", error);
        }
    }

    private static boolean hasTip(int mode) {
        return mode == PHOTO || mode == AI_PHOTO;
    }

    private static boolean ownsTip(Object module) {
        ZoomState state = module == null ? null : stateOf(module);
        return state != null && state.enabled && hasTip(state.mode);
    }

    private static ZoomState stateOf(Object module) {
        return (ZoomState) XposedHelpers.getAdditionalInstanceField(module, STATE);
    }

    private String distanceSummary() {
        SharedPreferences prefs = options.prefs();
        return "进入 " + prefs.getInt(ENTER_KEY, DEFAULT_ENTER_CM)
                + " 厘米 · 退出 " + prefs.getInt(EXIT_KEY, DEFAULT_EXIT_CM) + " 厘米";
    }

    private void showDistances(Object fragment) {
        Context context = (Context) XposedHelpers.callMethod(fragment, "requireContext");
        SharedPreferences prefs = options.prefs();
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * context.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding / 2);
        TextView enterLabel = new TextView(context);
        TextView exitLabel = new TextView(context);
        SeekBar enter = addDistanceSlider(content, enterLabel, "进入距离", MIN_ENTER_CM, MAX_ENTER_CM,
                prefs.getInt(ENTER_KEY, DEFAULT_ENTER_CM));
        SeekBar exit = addDistanceSlider(content, exitLabel, "退出距离", MIN_EXIT_CM, MAX_EXIT_CM,
                prefs.getInt(EXIT_KEY, DEFAULT_EXIT_CM));
        SeekBar.OnSeekBarChangeListener linked = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
                if (fromUser) {
                    int enterCm = MIN_ENTER_CM + enter.getProgress();
                    int exitCm = MIN_EXIT_CM + exit.getProgress();
                    if (seek == enter) {
                        exit.setProgress(Math.max(exitCm, enterCm + MIN_GAP_CM) - MIN_EXIT_CM);
                    } else {
                        enter.setProgress(Math.min(enterCm, exitCm - MIN_GAP_CM) - MIN_ENTER_CM);
                    }
                }
                enterLabel.setText("进入距离  " + (MIN_ENTER_CM + enter.getProgress()) + " 厘米");
                exitLabel.setText("退出距离  " + (MIN_EXIT_CM + exit.getProgress()) + " 厘米");
            }

            @Override public void onStartTrackingTouch(SeekBar seek) {}
            @Override public void onStopTrackingTouch(SeekBar seek) {}
        };
        enter.setOnSeekBarChangeListener(linked);
        exit.setOnSeekBarChangeListener(linked);
        linked.onProgressChanged(enter, enter.getProgress(), false);
        dialogs.builder(context).setTitle("75mm 切换距离").setView(content)
                .setNegativeButton("取消", null)
                .setPositiveButton("应用", (dialog, which) -> {
                    int enterCm = MIN_ENTER_CM + enter.getProgress();
                    int exitCm = MIN_EXIT_CM + exit.getProgress();
                    if (!prefs.edit().putInt(ENTER_KEY, enterCm).putInt(EXIT_KEY, exitCm).commit()) {
                        Log.e(TAG, "Unable to save near-range distances");
                        Toast.makeText(context, "切换距离保存失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Object root = XposedHelpers.getObjectField(fragment, "mPreferenceGroup");
                    Object row = XposedHelpers.callMethod(root, "k0", DISTANCE_ROW);
                    XposedHelpers.callMethod(row, "c0", distanceSummary());
                    enterDistance = enterCm;
                    exitDistance = exitCm;
                    Log.i(TAG, "enterCm=" + enterCm + " exitCm=" + exitCm);
                }).show();
    }

    private SeekBar addDistanceSlider(LinearLayout content, TextView label, String title,
            int min, int max, int value) {
        Context context = content.getContext();
        label.setTextSize(16);
        content.addView(label);
        SeekBar slider = sliders.create(context);
        slider.setContentDescription(title);
        slider.setMax(max - min);
        slider.setKeyProgressIncrement(1);
        slider.setProgress(value - min);
        int height = Math.round(48 * context.getResources().getDisplayMetrics().density);
        content.addView(slider, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
        return slider;
    }

    private static float distanceCm(byte[] bytes) {
        if (bytes == null || bytes.length != 8) return Float.NaN;
        ByteBuffer reading = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (reading.getFloat() != 1f) return Float.NaN;
        float cm = reading.getFloat();
        return cm > 0f ? cm : Float.NaN;
    }

    private void offerSample(Object module, ZoomState state, float distance) {
        synchronized (state) {
            if (!state.pending && !state.processing && state.idle && !(distance <= enterDistance)) return;
            state.latest = distance;
            if (state.pending) {
                state.skipped = true;
                return;
            }
            state.pending = true;
        }
        main.post(() -> {
            float latest;
            synchronized (state) {
                latest = state.latest;
                // Discard an interrupted sample sequence instead of combining stale distances.
                if (state.skipped) state.samples = 0;
                state.skipped = false;
                state.pending = false;
                state.processing = true;
            }
            try {
                updateZoom(module, state, latest);
            } finally {
                synchronized (state) {
                    state.idle = !state.nearby() && !state.manualOverride && state.samples == 0;
                    state.processing = false;
                }
            }
        });
    }

    private void updateZoom(Object module, ZoomState state, float distance) {
        try {
            if (!state.enabled || stateOf(module) != state || !(Boolean) alive.invoke(module)) return;
            if ((Boolean) busy.invoke(module)) {
                if (hasTip(state.mode) && state.nearby()) ui.renderTip(module, false, false);
                return;
            }
            Object zoom = getZoom.invoke(module);
            float current = (Float) ratio.invoke(zoom);
            int exitCm = exitDistance;

            state.distance = distance;
            boolean wasNearby = state.nearby();
            if (wasNearby) {
                if (current != state.expectedZoom()) {
                    state.leave();
                    state.manualOverride = true;
                } else if (state.stable(distance > exitCm)
                        && (state.mainSelected || switchZoom(zoom, state, state.previous, distance))) {
                    state.leave();
                }
            } else if (state.manualOverride) {
                if (state.stable(distance > exitCm)) state.manualOverride = false;
            } else if (state.stable(current >= 1f && current < Math.min(NEAR_ZOOM_LIMIT, state.target)
                    && distance <= enterDistance)
                    && switchZoom(zoom, state, state.target, distance)) {
                state.previous = current;
            }
            if (hasTip(state.mode) && (wasNearby || state.nearby())) ui.renderTip(module, state.nearby(), !state.mainSelected);
        } catch (ReflectiveOperationException error) {
            if (!state.sampleErrorLogged) {
                state.sampleErrorLogged = true;
                Log.e(TAG, "Unable to update near-range zoom or its native button", error);
            }
        }
    }

    private boolean switchZoom(Object zoom, ZoomState state, float target, float distance) throws ReflectiveOperationException {
        changeZoom.invoke(zoom, target, DIRECT_ZOOM);
        if ((Float) ratio.invoke(zoom) != target) return false;
        if ((boolean) streetMode.invoke(null, state.mode)) {
            Object ring = ((java.util.Optional<?>) streetRing.invoke(null)).orElse(null);
            if (ring != null) {
                int mm = target == state.target ? 75 : (int) focalLength.invoke(null, target, state.mode);
                selectFocal.invoke(ring, mm, DIRECT_ZOOM);
            }
        } else {
            ui.refreshZoomLabel();
        }
        Log.i(TAG, "zoom=" + target + " distanceCm=" + distance);
        return true;
    }

    private static final class ZoomState {
        final java.lang.ref.WeakReference<Object> module, device;
        final int mode;
        final float target;
        volatile boolean enabled;
        boolean pending, processing, skipped;
        boolean idle = true;
        float latest;
        boolean sampleErrorLogged;

        ZoomState(Object module, Object device, int mode, float target) {
            this.module = new java.lang.ref.WeakReference<>(module);
            this.device = new java.lang.ref.WeakReference<>(device);
            this.mode = mode;
            this.target = target;
        }

        float previous;
        float distance;
        int samples;
        boolean mainSelected;
        boolean manualOverride;

        boolean stable(boolean matches) {
            samples = matches ? samples + 1 : 0;
            if (samples != STABLE_SAMPLES) return false;
            samples = 0;
            return true;
        }

        boolean nearby() {
            return previous > 0f;
        }

        float expectedZoom() {
            return mainSelected ? previous : target;
        }

        void leave() {
            previous = 0f;
            samples = 0;
            mainSelected = false;
        }
    }
}
