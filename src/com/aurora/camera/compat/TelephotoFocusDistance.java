package com.aurora.camera.compat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.camera2.CaptureResult;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedHook;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

final class TelephotoFocusDistance implements HookPlan {
    private static final String KEY = "aurora_focus_distance_75mm";
    private static final String ANCHOR = "pref_camera_near_range_fallback_key";
    private static final String OVERLAY = "aurora.focus-distance-75mm";
    private static final String HAS_READING = OVERLAY + ".reading";
    private static final int PREVIEW_SURFACE_LAYOUT = 0x7f0b08df;
    private static final int ZOOM_BACKGROUND = 0x7f060c5f;
    private static final int LEICA_RED = 0x7f0601c0;
    private static final int MASTER_LIVE = 231;
    private static final int SAT_TELE = 3;
    private static final int SAT_ULTRA_TELE = 4;
    private static final int TELE_LIMIT_CM = 10;
    private static final int ULTRA_TELE_LIMIT_CM = 30;

    private final Options229 options;
    private final Class<?> photoModule;
    private final Method settings, layout, preview, inactive, activity, alive, mode, cameraId;
    private final Method moduleState, previewRect, appState, cameras, teleId, ultraTeleId, satMaster;
    private final Field orientation;

    TelephotoFocusDistance(Host host) throws ReflectiveOperationException {
        options = new Options229(host);
        photoModule = host.type("com.android.camera.module.Camera2Module");
        Class<?> module = host.type("com.android.camera.module.u");
        settings = host.method("com.android.camera.fragment.settings.common.AutoFallbackFragment",
                "registerPreferenceListener");
        layout = host.method("com.android.camera.Camera", "Yr");
        preview = host.method(module, "onPreviewMetaDataUpdate", CaptureResult.class);
        inactive = host.method(module, "onInactive");
        activity = host.method(module, "getActivity");
        alive = host.method(module, "isDeviceAndModuleAlive");
        mode = host.method(module, "getModuleIndex");
        cameraId = host.method(module, "getActualCameraId");
        moduleState = host.method(module, "getModuleState");
        previewRect = host.method(moduleState.getReturnType(), "v");
        appState = host.method(module, "getAppStateMgr");
        orientation = host.field("k6.a", "b");
        cameras = host.method("v6.f", "R");
        teleId = host.method("v6.f", "q");
        ultraTeleId = host.method("v6.f", "K");
        satMaster = host.method("l9.p0", "e", CaptureResult.class);
    }

    @Override public String id() { return "focus-distance-75mm"; }

    @Override public void install() {
        HookDiagnostics.hookMethod(settings, new StrictHook() {
            @Override protected void after(MethodHookParam param) {
                if (param.hasThrowable()) return;
                Object fragment = param.thisObject;
                Object group = XposedHelpers.getObjectField(fragment, "mPreferenceGroup");
                String anchor = XposedHelpers.callMethod(group, "k0", ANCHOR) == null ? null : ANCHOR;
                options.addSwitch(fragment, KEY, "中长焦镜头对焦距离",
                        "开启后，普通拍照模式下，取景框将展示中长焦镜头的最近对焦距离。", anchor, null);
            }
        });
        HookDiagnostics.hookMethod(layout, new ObservedHook() {
            @Override protected void runAfter(MethodHookParam param) {
                if (param.hasThrowable()) return;
                Activity camera = (Activity) param.thisObject;
                FrameLayout surface = camera.findViewById(PREVIEW_SURFACE_LAYOUT);
                DistanceView view = new DistanceView(camera);
                surface.addView(view, new FrameLayout.LayoutParams(-1, -1));
                XposedHelpers.setAdditionalInstanceField(camera, OVERLAY, view);
            }
        });
        HookDiagnostics.hookMethod(preview, new ObservedHook() {
            @Override protected void runAfter(MethodHookParam param) throws ReflectiveOperationException {
                CaptureResult result = (CaptureResult) param.args[0];
                Object module = param.thisObject;
                if (param.hasThrowable() || result == null || result.getFrameNumber() % 3 != 0
                        || !photoModule.isInstance(module) || (int) mode.invoke(module) == MASTER_LIVE) return;
                DistanceView view = overlay(module);
                if (view == null) return;
                synchronized (view) {
                    if (!view.enabled()) {
                        view.clear(module);
                        return;
                    }
                    if (!(boolean) alive.invoke(module)) return;
                    int nearLimit = nearLimitCm(module, result);
                    Float diopters = nearLimit == 0 ? null : result.get(CaptureResult.LENS_FOCUS_DISTANCE);
                    if (diopters == null || !Float.isFinite(diopters) || diopters < 0f) {
                        view.clear(module);
                        return;
                    }
                    Rect rect = (Rect) previewRect.invoke(moduleState.invoke(module));
                    if (rect == null || rect.isEmpty()) {
                        view.clear(module);
                        return;
                    }
                    int degrees = orientation.getInt(appState.invoke(module));
                    double cm = diopters == 0f ? Double.POSITIVE_INFINITY : 100.0 / diopters;
                    view.show(module, rect, degrees, cm, cm < nearLimit);
                    param.setObjectExtra(HAS_READING, Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HAS_READING));
            }
        });
        HookDiagnostics.hookMethod(inactive, new ObservedHook() {
            @Override protected void runAfter(MethodHookParam param) throws ReflectiveOperationException {
                if (!photoModule.isInstance(param.thisObject)) return;
                DistanceView view = overlay(param.thisObject);
                if (view != null && view.clear(param.thisObject)) {
                    param.setObjectExtra(HAS_READING, Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(HAS_READING));
            }
        });
    }

    private DistanceView overlay(Object module) throws ReflectiveOperationException {
        Object camera = activity.invoke(module);
        return camera == null ? null : (DistanceView) XposedHelpers.getAdditionalInstanceField(camera, OVERLAY);
    }

    private int nearLimitCm(Object module, CaptureResult result) throws ReflectiveOperationException {
        Object cameraRegistry = cameras.invoke(null);
        int tele = (int) teleId.invoke(cameraRegistry);
        int ultraTele = (int) ultraTeleId.invoke(cameraRegistry);
        String active = result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID);
        if (active != null) {
            if (tele >= 0 && active.equals(Integer.toString(tele))) return TELE_LIMIT_CM;
            if (ultraTele >= 0 && active.equals(Integer.toString(ultraTele))) return ULTRA_TELE_LIMIT_CM;
            return 0;
        }
        int camera = (int) cameraId.invoke(module);
        if (tele >= 0 && camera == tele) return TELE_LIMIT_CM;
        if (ultraTele >= 0 && camera == ultraTele) return ULTRA_TELE_LIMIT_CM;
        int role = (int) satMaster.invoke(null, result);
        if (role == SAT_TELE) return TELE_LIMIT_CM;
        if (role == SAT_ULTRA_TELE) return ULTRA_TELE_LIMIT_CM;
        return 0;
    }

    private static final class DistanceView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final SharedPreferences preferences;
        private final float inset;
        private final float padX, padY;
        private final int nearColor;
        private volatile Reading reading;

        DistanceView(Context context) {
            super(context);
            preferences = context.getSharedPreferences(Options229.PREFERENCES, Context.MODE_PRIVATE);
            float density = getResources().getDisplayMetrics().density;
            inset = 12 * density;
            padX = 10 * density;
            padY = 4 * density;
            background.setColor(context.getColor(ZOOM_BACKGROUND));
            nearColor = context.getColor(LEICA_RED);
            paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    13, getResources().getDisplayMetrics()));
            paint.setShadowLayer(inset / 4, 0, 1, Color.BLACK);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        boolean enabled() { return preferences.getBoolean(KEY, false); }

        void show(Object module, Rect rect, int degrees, double cm, boolean near) {
            Reading current = reading;
            if (current != null && current.module == module && current.degrees == degrees
                    && current.cm == cm && current.near == near && current.rect.equals(rect)) return;
            reading = new Reading(module, new Rect(rect), degrees, cm, near);
            postInvalidateOnAnimation();
        }

        synchronized boolean clear(Object module) {
            Reading current = reading;
            if (current == null || current.module != module) return false;
            reading = null;
            postInvalidateOnAnimation();
            return true;
        }

        @Override protected void onDraw(Canvas canvas) {
            Reading current = reading;
            if (current == null || !enabled()) return;
            Rect rect = current.rect;
            boolean landscape = current.degrees % 180 != 0;
            float width = landscape ? rect.height() : rect.width();
            float height = landscape ? rect.width() : rect.height();
            String value = Double.isInfinite(current.cm) ? "∞" : String.format(Locale.ROOT, "%.1f", current.cm);
            String label = "对焦距离（cm）：" + value;
            float pillWidth = paint.measureText(label) + 2 * padX;
            float pillHeight = paint.descent() - paint.ascent() + 2 * padY;
            float left = landscape ? -width / 2 + inset : width / 2 - inset - pillWidth;
            float top = -height / 2 + inset;
            float radius = pillHeight / 2;
            canvas.save();
            canvas.clipRect(rect);
            canvas.translate(rect.exactCenterX(), rect.exactCenterY());
            canvas.rotate(-current.degrees);
            canvas.drawRoundRect(left, top, left + pillWidth, top + pillHeight, radius, radius, background);
            paint.setColor(current.near ? nearColor : Color.WHITE);
            canvas.drawText(label, left + padX, top + padY - paint.ascent(), paint);
            canvas.restore();
        }
    }

    private static final class Reading {
        final Object module;
        final Rect rect;
        final int degrees;
        final double cm;
        final boolean near;

        Reading(Object module, Rect rect, int degrees, double cm, boolean near) {
            this.module = module;
            this.rect = rect;
            this.degrees = degrees;
            this.cm = cm;
            this.near = near;
        }
    }
}
