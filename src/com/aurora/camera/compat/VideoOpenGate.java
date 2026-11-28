package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.SessionConfiguration;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Expose the stock Open Gate format in rear pro video and director mode. */
final class VideoOpenGate implements HookPlan {
    private static final int DIRECTOR = 164;
    private static final int PRO_VIDEO = 180;
    private static final int LAYOUT_4_3 = 0;
    private static final String OPEN_GATE = "open_gate";
    private final Method buildRatios, appendNativeRatio, ratioIcon, isOpenGate, layout, transitionLayout;
    private final Method operation, qualityMenuLimits, cameras, mainId;
    private final Field sessionMode, sessionCamera;
    private static final CameraCharacteristics.Key<int[]> SENSOR_MODES = new CameraCharacteristics.Key<>(
            "org.quic.camera2.sensormode.info.SensorModeTable", int[].class);
    private final Field maxFps, characteristics;
    private final Field items, selectable, value, iconProvider, title, description;
    private final Field[] iconFields;
    private final int openGateTitle, openGateDescription;

    VideoOpenGate(Host host) throws ReflectiveOperationException {
        Class<?> ratios = host.type("r2.T");
        Class<?> row = host.type("com.android.camera.data.data.d");
        buildRatios = host.method(ratios, "t", int.class, int.class, host.type("l9.g"));
        appendNativeRatio = host.method(ratios, "m", ArrayList.class);
        selectable = host.field(ratios, "d");
        items = host.field("com.android.camera.data.data.c", "mItems");
        value = host.field(row, "r");
        title = host.field(row, "l");
        description = host.field(row, "n");
        iconFields = new Field[] {host.field(row, "c"), host.field(row, "f"),
                host.field(row, "g"), host.field(row, "h")};
        iconProvider = host.field("Y6.j", "a");
        ratioIcon = host.method("Y6.k", "V", String.class);
        openGateTitle = host.field("Rh.e", "pref_camera_picturesize_entry_opengate").getInt(null);
        openGateDescription = host.field("Rh.e", "accessibility_picturesize_opengate_button").getInt(null);
        isOpenGate = host.method("com.android.camera.data.data.n", "W", int.class);
        Class<?> qualityRange = host.type("r2.r1$a");
        qualityMenuLimits = host.method("r2.h0", "C", qualityRange, ArrayList.class,
                int.class, host.type("l9.g"), int.class);
        sessionMode = host.field("y3.w", "a");
        sessionCamera = host.field("y3.w", "c");
        operation = host.method("y3.f", "A", host.type("y3.x"));
        cameras = host.method("v6.f", "R");
        mainId = host.method("v6.f", "e");
        characteristics = host.field("l9.g", "d");
        maxFps = host.field(qualityRange, "d");
        layout = host.method("A8.O", "f", int.class, int.class);
        transitionLayout = host.method("Lr.X", "b", int.class, int.class, String.class);
    }

    @Override public String id() { return "video-open-gate"; }

    private static boolean videoMode(int mode) {
        return mode == PRO_VIDEO || mode == DIRECTOR;
    }

    @Override public void install() {
        HookDiagnostics.hookMethod(buildRatios, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws ReflectiveOperationException {
                if (p.hasThrowable() || !videoMode((int) p.args[0]) || (int) p.args[1] != 0
                        || !selectable.getBoolean(p.thisObject)) return;
                List<?> nativeRows = (List<?>) items.get(p.thisObject);
                for (Object row : nativeRows) if (OPEN_GATE.equals(value.get(row))) return;
                ArrayList<Object> rows = new ArrayList<>(nativeRows);
                // m() constructs one ordinary ratio row with the host's unset-field defaults.
                // The native Open Gate row differs in its value, four icons and two labels.
                appendNativeRatio.invoke(null, rows);
                Object row = rows.get(rows.size() - 1);
                value.set(row, OPEN_GATE);
                int icon = (int) ratioIcon.invoke(iconProvider.get(null), OPEN_GATE);
                for (Field field : iconFields) field.setInt(row, icon);
                title.setInt(row, openGateTitle);
                description.setInt(row, openGateDescription);
                items.set(p.thisObject, Collections.unmodifiableList(rows));
                p.setObjectExtra(OPEN_GATE, true);
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(OPEN_GATE));
            }
        });

        StrictHook frameLayout = new StrictHook() {
            @Override protected void before(MethodHookParam p) throws ReflectiveOperationException {
                int mode = (int) p.args[0];
                if (!videoMode(mode)) return;
                boolean selected = p.method.equals(transitionLayout)
                        ? OPEN_GATE.equals(p.args[2]) : (boolean) isOpenGate.invoke(null, mode);
                if (selected) {
                    p.setResult(LAYOUT_4_3);
                    p.setObjectExtra(OPEN_GATE, true);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(OPEN_GATE));
            }
        };
        HookDiagnostics.hookMethod(layout, frameLayout);
        HookDiagnostics.hookMethod(transitionLayout, frameLayout);
        // Full-height recording uses the native independent lens. Its FOVC session
        // requires stabilization margins, and the main sensor's HDR session rejects
        // this stream. Other lenses keep their working native HDR session.
        HookDiagnostics.hookMethod(operation, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws ReflectiveOperationException {
                if (p.hasThrowable()) return;
                int mode = sessionMode.getInt(p.args[0]);
                if (!videoMode(mode)) return;
                int session = (int) p.getResult();
                if (session != 0xf010 && session != 0x8024 && session != 0x803c) return;
                if (!(boolean) isOpenGate.invoke(null, mode)) return;
                if (session == 0xf010 || session == 0x803c || sessionCamera.getInt(p.args[0]) == (int) mainId.invoke(cameras.invoke(null))) {
                    p.setResult(SessionConfiguration.SESSION_REGULAR);
                    p.setObjectExtra(OPEN_GATE, true);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(OPEN_GATE));
            }
        });
        HookDiagnostics.hookMethod(qualityMenuLimits, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws ReflectiveOperationException {
                int mode = (int) p.args[4];
                if (!p.hasThrowable() && videoMode(mode) && (boolean) isOpenGate.invoke(null, mode)) {
                    CameraCharacteristics caps = (CameraCharacteristics) characteristics.get(p.args[3]);
                    int limit = fullHeightFps(caps.get(SENSOR_MODES));
                    maxFps.setInt(p.args[0], Math.min(limit, maxFps.getInt(p.args[0])));
                    p.setObjectExtra(OPEN_GATE, true);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(OPEN_GATE));
            }
        });
    }
    // Provider format: number of modes, row width, then width, height and fps per row.
    private static int fullHeightFps(int[] modes) {
        if (modes != null && modes.length >= 2 && modes[1] == 3) {
            for (int i = 2; i + 2 < modes.length; i += 3) {
                if (modes[i] == 4096 && modes[i + 1] == 3072 && modes[i + 2] >= 60) return 60;
            }
        }
        return 30;
    }
}
