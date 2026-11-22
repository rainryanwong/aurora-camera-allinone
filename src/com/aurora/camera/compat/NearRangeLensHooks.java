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
    private static final int REAR_SAT = 6;
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

    private final Options229 options;
    private final MiuixDialogs dialogs;
    private final MiuixSeekBar241 sliders;
    private final Method settings, update, preview, inactive, nearClick;
    private final Method nativeEnabled, photoEnabled, moduleIndex, cameraId, alive, busy;
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
        setFallbackDisabled = host.method("l9.a", "r0", boolean.class);
        preview = host.method(module, "onPreviewMetaDataUpdate", CaptureResult.class);
        inactive = host.method(module, "onInactive");
        nativeEnabled = host.method("com.android.camera.data.data.x", "q0");
        photoEnabled = host.method("com.android.camera.data.data.i", "X0", int.class);
        nearClick = host.method("H4.x", "accept", Object.class);
        clickKind = host.field("H4.x", "a");
        moduleIndex = host.method(module, "getModuleIndex");
        cameraId = host.method(module, "getActualCameraId");
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
                if (ownsNearRange(module)) {
                    Object device = getDevice.invoke(param.thisObject);
                    if (device != null) {
                        setFallbackDisabled.invoke(device, true);
                        param.setResult(true);
                    }
                }
            }
        });
        HookDiagnostics.hookMethod(ui.showTip, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam param) throws ReflectiveOperationException {
                if ((int) param.args[0] != NearRangeUi.TIP_ID || ui.rendering.get() != null
                        || !ownsNearRange(ui.owner(param.thisObject))) return;
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
                if ((int) param.args[0] == PHOTO && rendering != null) {
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
                if (!ownsNearRange(module)) return;
                param.setResult(null);
                ZoomState state = stateOf(module);
                if (state == null || !state.nearby() || !(boolean) alive.invoke(module)
                        || (boolean) busy.invoke(module)) return;
                Object zoom = getZoom.invoke(module);
                if ((float) ratio.invoke(zoom) != state.expectedZoom()) return;
                boolean wasTele = !state.mainSelected;
                if (switchZoom(zoom, wasTele ? 1f : TELE_ZOOM, state.distance)) {
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
            @Override protected void runAfter(MethodHookParam param) throws ReflectiveOperationException {
                CaptureResult result = (CaptureResult) param.args[0];
                Object module = param.thisObject;
                if (param.hasThrowable() || result == null || result.getFrameNumber() % 3 != 0
                        || !isRearPhoto(module) || (!options.enabled(KEY) && stateOf(module) == null)) return;
                float distance = distanceCm(result.get(TOF));
                main.post(() -> updateZoom(module, distance));
                param.setObjectExtra(HANDLED, Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HANDLED));
            }
        });
        HookDiagnostics.hookMethod(inactive, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                ZoomState state = (ZoomState) XposedHelpers.removeAdditionalInstanceField(param.thisObject, STATE);
                if (state != null && state.nearby() && !state.mainSelected) {
                    Object zoom = getZoom.invoke(param.thisObject);
                    if (zoom != null && (Float) ratio.invoke(zoom) == TELE_ZOOM) {
                        saveZoom.invoke(null, state.previous, PHOTO);
                    }
                }
            }
        });
    }

    private boolean isRearPhoto(Object module) throws ReflectiveOperationException {
        return module != null && (Integer) moduleIndex.invoke(module) == PHOTO
                && (Integer) cameraId.invoke(module) == REAR_SAT;
    }

    private boolean ownsNearRange(Object module) throws ReflectiveOperationException {
        return options.enabled(KEY) && isRearPhoto(module) && (Boolean) nativeEnabled.invoke(null);
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

    private void updateZoom(Object module, float distance) {
        try {
            if (!(Boolean) alive.invoke(module)) return;
            ZoomState state = stateOf(module);
            if (!ownsNearRange(module)) {
                if (state != null) {
                    XposedHelpers.removeAdditionalInstanceField(module, STATE);
                    if (state.nearby()) ui.renderTip(module, false, false);
                }
                return;
            }
            if ((Boolean) busy.invoke(module)) {
                if (state != null && state.nearby()) ui.renderTip(module, false, false);
                return;
            }
            Object zoom = getZoom.invoke(module);
            float current = (Float) ratio.invoke(zoom);
            SharedPreferences prefs = options.prefs();
            int exitCm = prefs.getInt(EXIT_KEY, DEFAULT_EXIT_CM);
            if (state == null) {
                state = new ZoomState();
                XposedHelpers.setAdditionalInstanceField(module, STATE, state);
                ui.renderTip(module, false, false);
            }
            state.distance = distance;
            boolean wasNearby = state.nearby();
            if (wasNearby) {
                if (current != state.expectedZoom()) {
                    state.leave();
                    state.manualOverride = true;
                } else if (state.stable(distance > exitCm)
                        && (state.mainSelected || switchZoom(zoom, state.previous, distance))) {
                    state.leave();
                }
            } else if (state.manualOverride) {
                if (state.stable(distance > exitCm)) state.manualOverride = false;
            } else if (state.stable(current >= 1f && current < NEAR_ZOOM_LIMIT
                    && distance <= prefs.getInt(ENTER_KEY, DEFAULT_ENTER_CM))
                    && switchZoom(zoom, TELE_ZOOM, distance)) {
                state.previous = current;
            }
            if (wasNearby || state.nearby()) ui.renderTip(module, state.nearby(), !state.mainSelected);
        } catch (ReflectiveOperationException error) {
            Log.e(TAG, "Unable to update near-range zoom or its native button", error);
        }
    }

    private boolean switchZoom(Object zoom, float target, float distance) throws ReflectiveOperationException {
        changeZoom.invoke(zoom, target, DIRECT_ZOOM);
        if ((Float) ratio.invoke(zoom) != target) return false;
        ui.refreshZoomLabel();
        Log.i(TAG, "zoom=" + target + " distanceCm=" + distance);
        return true;
    }

    private static final class ZoomState {
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
            return mainSelected ? 1f : TELE_ZOOM;
        }

        void leave() {
            previous = 0f;
            samples = 0;
            mainSelected = false;
        }
    }
}
