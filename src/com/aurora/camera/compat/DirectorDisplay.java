package com.aurora.camera.compat;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;

/** Restores the native director panels and lens control positions. */
final class DirectorDisplay implements HookPlan {
    private static final int MODE = 164;
    private static final int MASTER_FILTER = 263;
    private static final int LENS_CONTAINER = 4;
    private static final int PARAMETER_CONTAINER = 6;
    private static final int CONTAINER_EXCLUDED = 16;
    private static final int PANEL_CONTAINER = 7;
    private static final int FILTER_PANEL = 0x00fffff7;
    private static final int BOTTOM_PANEL_TITLE = 245;
    private static final int AUDIO_TYPE_TITLE = 3810;
    private static final int ADD_PANEL = 2;
    private static final int DISMISS_ON_BACK = 6;
    private static final int SHUTTER_BUTTON = 0x7f0b0a06;
    private static final int FILTER_THUMBNAIL = 0x7f0b0393;
    private static final int FILTER_INDICATOR = 0x7f0b0395;
    private static final int FILTER_LIST = 0x7f0b06f5;

    private final Method dispatch;
    private final Method currentMode;
    private final Method featureUi;
    private final Method containsPanel;
    private final Method changePanel;
    private final Method filterCreated;
    private final Method configureExclusion;
    private final Method excludeContainer;
    private final Method filterLayout;
    private final Method dismissFilter;
    private final Method audioExclusionChanged;
    private final Method removeBottomTitle;
    private final Class<?> audioTypePanel;
    private final Method showCloudFilterTitle;
    private final Method filterItems;
    private final Method alerts;
    private final Method updateAlerts;
    private final Method alertFragment;
    private final Method layoutControls;
    private final Method topMenu;
    private final Method menuShowing;
    private final Method menuExclusionChanged;
    private final Method containerState;
    private final Method zoomToggle;
    private final Method showZoomToggle;
    private final Method showLensView;
    private final Method minimalDisplay;
    private final Method layoutZoom;
    private final Method animateRecordingZoomPosition;
    private final Method bindZoomButtons;
    private final Method resetZoomBar;
    private final Method rotateZoom;
    private final Method showWheel;
    private final Method hideWheel;
    private final Method fragmentView;
    private final Method landscape;
    private final Method vertical;
    private final Field fragmentMode;
    private final Field filterComponent;
    private final Field filterTitle;
    private final Field cloudFilterInfo;
    private final Field localFilterTitleId;
    private final Field localFilterTitle;
    private final Field zoomBar;
    private final Field lensToggle;
    private final Field lensToggleState;
    private final Field wheelOpen;
    private final Field monitorModel;
    private final Field standardMonitor;
    private final Field landscapeMonitor;
    private final Runnable filterCards;
    private final WeakHashMap<View, PointF> originalPosition = new WeakHashMap<>();
    private WeakReference<View> filterView = new WeakReference<>(null);
    private WeakReference<Object> filterFragment = new WeakReference<>(null);
    private WeakReference<Object> openWheel = new WeakReference<>(null);

    private DirectorDisplay(Host host) throws ReflectiveOperationException {
        dispatch = host.method("r6.W", "q", int.class, int.class);
        currentMode = host.method("r6.W", "Jc");
        featureUi = host.method("R6.j0", "a");
        containsPanel = host.method("R6.j0", "e", int.class, int.class);
        changePanel = host.method("R6.j0", "h", int.class, int.class, int.class);
        filterCreated = host.method("com.android.camera.fragment.u0", "onViewCreated",
                View.class, Bundle.class);
        Class<?> exclusion = host.type("Y1.b");
        configureExclusion = host.method("com.xiaomi.camera.base.ui.fragments.d",
                "configFragmentData", exclusion);
        excludeContainer = host.method(exclusion, "a", int.class, int[].class);
        filterLayout = host.method("com.android.camera.fragment.u0", "updateView", View.class, Bundle.class);
        dismissFilter = host.method("com.android.camera.fragment.u", "onBackEvent", int.class);
        audioExclusionChanged = host.method("F4.i", "onExclusionCallback", boolean.class);
        removeBottomTitle = host.method("com.android.camera.fragment.u", "qs", host.type("R6.j0"), int.class);
        audioTypePanel = host.type("F4.k");
        showCloudFilterTitle = host.method("com.android.camera.fragment.u0", "Cs", int.class);
        filterItems = host.method("r2.a", "getItems");
        alerts = host.method("R6.m1", "a");
        updateAlerts = host.method("R6.m1", "a3");
        alertFragment = host.method("X9.R0", "M");
        layoutControls = host.method("z4.n", "b", int.class, boolean.class);
        topMenu = host.method("R6.s1", "a");
        menuShowing = host.method("R6.s1", "Th");
        menuExclusionChanged = host.method("p5.W", "onExclusionCallback", boolean.class);
        containerState = host.method("R6.j0", "m", int.class, int.class);
        zoomToggle = host.method("W6.e", "a");
        showZoomToggle = host.method("H4.p0", "S0");
        showLensView = host.method("T1.b", "e", View.class);
        minimalDisplay = host.method("com.android.camera.data.data.F", "N");
        layoutZoom = host.method("H4.p0", "Rs", int.class);
        animateRecordingZoomPosition = host.method("H4.p0", "T8", boolean.class);
        bindZoomButtons = host.method("com.android.camera.ui.zoom.ZoomRatioToggleView", "M",
                int.class, int.class, boolean.class, boolean.class);
        resetZoomBar = host.method("H4.p0", "Ts");
        rotateZoom = host.method("H4.p0", "provideRotateItem", List.class, int.class);
        showWheel = host.method("H4.J", "provideAnimateElement", int.class, List.class, int.class);
        hideWheel = host.method("H4.J", "ys", int.class, boolean.class, boolean.class);
        fragmentView = host.method("androidx.fragment.app.Fragment", "getView");
        landscape = host.method("com.android.camera.fragment.h", "isLandScape");
        vertical = host.method("K2.b", "W");
        fragmentMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        filterComponent = host.field("com.android.camera.fragment.u0", "r");
        filterTitle = host.field("com.android.camera.fragment.u0", "l");
        cloudFilterInfo = host.field("com.android.camera.data.data.d", "a");
        localFilterTitleId = host.field("com.android.camera.data.data.d", "l");
        localFilterTitle = host.field("com.android.camera.data.data.d", "o");
        zoomBar = host.field("H4.p0", "b");
        lensToggle = host.field("H4.p0", "j");
        lensToggleState = host.field("H4.p0", "k");
        wheelOpen = host.field("H4.J", "s");
        monitorModel = host.field("X9.i", "T1");
        standardMonitor = host.field("z4.n", "c");
        landscapeMonitor = host.field("z4.n", "d");
        filterCards = DirectorFilterCards.prepare(host);
    }

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        return new DirectorDisplay(host);
    }

    @Override public String id() {
        return "DirectorDisplay";
    }

    @Override public void install() {
        filterCards.run();
        HookDiagnostics.hookMethod(dispatch, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                if ((int) param.args[0] != MASTER_FILTER
                        || (int) currentMode.invoke(param.thisObject) != MODE) return;
                Object ui = ((Optional<?>) featureUi.invoke(null)).orElse(null);
                if (ui == null) return;
                boolean present = (boolean) containsPanel.invoke(ui, PANEL_CONTAINER, FILTER_PANEL);
                if (present) {
                    Object fragment = filterFragment.get();
                    if (fragment == null) return;
                    // The panel's native back path also releases its exclusive UI requests.
                    dismissFilter.invoke(fragment, DISMISS_ON_BACK);
                } else {
                    changePanel.invoke(ui, PANEL_CONTAINER, FILTER_PANEL, ADD_PANEL);
                }
                param.setObjectExtra("director-filter-dispatched", Boolean.TRUE);
                param.setResult(null);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-filter-dispatched"));
            }
        });

        HookDiagnostics.hookMethod(filterCreated, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || fragmentMode.getInt(param.thisObject) != MODE) return;
                View panel = (View) param.args[0];
                filterView = new WeakReference<>(panel);
                filterFragment = new WeakReference<>(param.thisObject);
                panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override public void onViewAttachedToWindow(View view) {
                        refreshControlsAfterWindowChange();
                    }

                    @Override public void onViewDetachedFromWindow(View view) {
                        view.removeOnAttachStateChangeListener(this);
                        if (filterView.get() == view) {
                            filterView.clear();
                            filterFragment.clear();
                            refreshControlsAfterWindowChange();
                        }
                    }
                });
                refreshControls();
                param.setObjectExtra("director-filter-created", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-filter-created"));
            }
        });

        HookDiagnostics.hookMethod(configureExclusion, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || !filterCreated.getDeclaringClass().isInstance(param.thisObject)
                        || fragmentMode.getInt(param.thisObject) != MODE) return;
                excludeContainer.invoke(param.args[0], LENS_CONTAINER, new int[0]);
                excludeContainer.invoke(param.args[0], PARAMETER_CONTAINER, new int[0]);
                param.setObjectExtra("director-filter-excludes-controls", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-filter-excludes-controls"));
            }
        });

        HookDiagnostics.hookMethod(audioExclusionChanged, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || (boolean) param.args[0] || !audioTypePanel.isInstance(param.thisObject)
                        || fragmentMode.getInt(param.thisObject) != MODE) return;
                Object ui = ((Optional<?>) featureUi.invoke(null)).orElse(null);
                if (ui == null || !(boolean) containsPanel.invoke(ui, PANEL_CONTAINER, BOTTOM_PANEL_TITLE)) return;
                // Release the audio title with the panel, including when the exit animation is interrupted.
                removeBottomTitle.invoke(param.thisObject, ui, AUDIO_TYPE_TITLE);
                param.setObjectExtra("director-audio-title-released", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-audio-title-released"));
            }
        });

        HookDiagnostics.hookMethod(showCloudFilterTitle, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                Object fragment = param.thisObject;
                int index = (int) param.args[0];
                if (fragmentMode.getInt(fragment) != MODE || index < 0) return;

                List<?> items = (List<?>) filterItems.invoke(filterComponent.get(fragment));
                if (items.isEmpty()) return;
                Object selected = items.get(index);
                if (cloudFilterInfo.get(selected) != null) return;

                // Cloud support is device-wide. The director catalog can still contain built-in
                // rows whose title comes from the local component entry.
                TextView title = (TextView) filterTitle.get(fragment);
                if (title != null) {
                    int resourceId = localFilterTitleId.getInt(selected);
                    if (resourceId > 0) title.setText(resourceId);
                    else title.setText((CharSequence) localFilterTitle.get(selected));
                }
                param.setObjectExtra("director-local-filter-title", Boolean.TRUE);
                param.setResult(null);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-local-filter-title"));
            }
        });

        HookDiagnostics.hookMethod(filterLayout, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || fragmentMode.getInt(param.thisObject) != MODE) return;
                View panel = (View) param.args[0];
                View indicator = panel.findViewById(FILTER_INDICATOR);
                ViewGroup thumbnails = panel.findViewById(FILTER_LIST);
                if (indicator == null || thumbnails == null) return;
                boolean verticalList = (boolean) vertical.invoke(null);
                panel.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                    @Override public boolean onPreDraw() {
                        panel.getViewTreeObserver().removeOnPreDrawListener(this);
                        alignFilterIndicator(indicator, thumbnails, verticalList);
                        return true;
                    }
                });
                param.setObjectExtra("director-filter-indicator-layout", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-filter-indicator-layout"));
            }
        });

        HookDiagnostics.hookMethod(layoutControls, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                if ((boolean) param.args[1] && (filterOpen() || directorMenuOpen())) {
                    // A rotation calls the monitor layout while the menu still owns the UI.
                    param.args[0] = 2;
                    param.setObjectExtra("director-monitor-controls-hidden", Boolean.TRUE);
                }
            }

            @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                if (param.hasThrowable() || openWheel.get() == null) return;
                Field activeRow = (boolean) param.args[1] ? landscapeMonitor : standardMonitor;
                if (hideMonitor((View) activeRow.get(param.thisObject))) {
                    param.setObjectExtra("director-monitor-controls-hidden", Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-monitor-controls-hidden"));
            }
        });

        HookDiagnostics.hookMethod(menuExclusionChanged, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || (boolean) param.args[0]
                        || fragmentMode.getInt(param.thisObject) != MODE || directorMenuOpen()) return;
                refreshControls();
                restoreLensAfterMenu();
                param.setObjectExtra("director-menu-controls-restored", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-menu-controls-restored"));
            }
        });

        StrictHook updateWheelControls = new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable()) return;
                Object fragment = param.thisObject;
                boolean open = fragmentMode.getInt(fragment) == MODE && wheelOpen.getBoolean(fragment);
                boolean wasOpen = openWheel.get() == fragment;
                if (open == wasOpen) return;
                if (open) {
                    openWheel = new WeakReference<>(fragment);
                    Object protocol = ((Optional<?>) alerts.invoke(null)).orElse(null);
                    if (alertFragment.getDeclaringClass().isInstance(protocol)) {
                        Object alert = alertFragment.invoke(protocol);
                        if (monitorModel.getDeclaringClass().isInstance(alert)) {
                            Object model = monitorModel.get(alert);
                            if (model != null) {
                                hideMonitor((View) standardMonitor.get(model));
                                hideMonitor((View) landscapeMonitor.get(model));
                            }
                        }
                    }
                } else {
                    openWheel.clear();
                    // Reapply the native visibility for the current orientation and DISP state
                    // after the zoom fragment has finished releasing its UI requests.
                    View view = (View) fragmentView.invoke(fragment);
                    if (view != null) view.post(() -> {
                        if (openWheel.get() == null) refreshControlsAfterWindowChange();
                    });
                }
                param.setObjectExtra("director-wheel-monitor-controls", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-wheel-monitor-controls"));
            }
        };
        HookDiagnostics.hookMethod(showWheel, updateWheelControls);
        HookDiagnostics.hookMethod(hideWheel, updateWheelControls);

        HookDiagnostics.hookMethod(bindZoomButtons, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                if ((int) param.args[0] != MODE || !(boolean) param.args[3] || (boolean) param.args[2]) return;
                // Use the native recording counter that Pro Video uses for a single lens.
                param.args[2] = true;
                param.setObjectExtra("director-recording-single-zoom", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-recording-single-zoom"));
            }
        });

        HookDiagnostics.hookMethod(animateRecordingZoomPosition, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                Object fragment = param.thisObject;
                if (fragmentMode.getInt(fragment) != MODE) return;
                // The recording state has already rebound the buttons. Keep their director anchor
                // instead of running the shared translation animation on the same view.
                param.setResult(null);
                param.setObjectExtra("director-recording-lens-position-kept", Boolean.TRUE);
                View bar = (View) zoomBar.get(fragment);
                View toggle = (View) lensToggle.get(fragment);
                if (bar != null && toggle != null) {
                    placeLensAfterLayout(fragment, bar, toggle, false);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-recording-lens-position-kept"));
            }
        });

        StrictHook repositionZoom = new StrictHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable()) return;
                View bar = (View) zoomBar.get(param.thisObject);
                if (bar == null) return;
                Object fragment = param.thisObject;
                View toggle = (View) lensToggle.get(fragment);
                if (placeLensAfterLayout(fragment, bar, toggle, false)) {
                    param.setObjectExtra("director-lens-layout-requested", Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-lens-layout-requested"));
            }
        };
        HookDiagnostics.hookMethod(layoutZoom, repositionZoom);
        HookDiagnostics.hookMethod(resetZoomBar, repositionZoom);
        HookDiagnostics.hookMethod(rotateZoom, repositionZoom);

    }

    private boolean filterOpen() {
        View panel = filterView.get();
        return panel != null && panel.isAttachedToWindow();
    }

    private boolean directorMenuOpen() throws ReflectiveOperationException {
        Object menu = ((Optional<?>) topMenu.invoke(null)).orElse(null);
        return menu != null && fragmentMode.getInt(menu) == MODE && (boolean) menuShowing.invoke(menu);
    }

    private void restoreLensAfterMenu() throws ReflectiveOperationException {
        if (filterOpen() || openWheel.get() != null || (boolean) minimalDisplay.invoke(null)) return;
        Object ui = ((Optional<?>) featureUi.invoke(null)).orElse(null);
        if (ui == null || (boolean) containerState.invoke(ui, LENS_CONTAINER, CONTAINER_EXCLUDED)) return;

        Object fragment = ((Optional<?>) zoomToggle.invoke(null)).orElse(null);
        if (!showZoomToggle.getDeclaringClass().isInstance(fragment) || fragmentMode.getInt(fragment) != MODE) return;
        View bar = (View) zoomBar.get(fragment);
        View toggle = (View) lensToggle.get(fragment);
        if (bar == null || toggle == null) return;

        // The normal exclusion callback refreshes the zoom spec, but can leave the child hidden
        // during a repeating request. Restore an enabled toggle, or use the native show path.
        if (bar.isShown() && (toggle.getVisibility() != View.VISIBLE || toggle.getAlpha() == 0f)) {
            int state = lensToggleState.getInt(fragment);
            if (state == -1) {
                showZoomToggle.invoke(fragment);
            } else if (state == 1) {
                showLensView.invoke(null, toggle);
                resetZoomBar.invoke(fragment);
            }
        }
        // Ua(false) can rebind an already visible toggle without a rotation callback.
        placeLensAfterLayout(fragment, bar, toggle, true);
    }

    private boolean placeLensAfterLayout(Object fragment, View bar, View toggle, boolean menuClosed)
            throws ReflectiveOperationException {
        if (!usesLensColumn(fragment, bar)) {
            resetLensPosition(bar);
            return false;
        }
        ViewTreeObserver observer = bar.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                ViewTreeObserver registered = observer.isAlive() ? observer : bar.getViewTreeObserver();
                registered.removeOnPreDrawListener(this);
                if (bar.isAttachedToWindow() && usesLensColumnAfterLayout(fragment, bar)) {
                    alignLensColumn(bar, toggle, menuClosed);
                }
                return true;
            }
        });
        return true;
    }

    private static boolean hideMonitor(View row) {
        if (row == null || row.getVisibility() != View.VISIBLE) return false;
        row.setVisibility(View.INVISIBLE);
        return true;
    }

    private void refreshControls() throws ReflectiveOperationException {
        Object protocol = ((Optional<?>) alerts.invoke(null)).orElse(null);
        if (protocol != null) updateAlerts.invoke(protocol);
    }

    private void refreshControlsAfterWindowChange() {
        try {
            refreshControls();
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private boolean usesLensColumn(Object fragment, View bar) throws ReflectiveOperationException {
        View root = bar.getRootView();
        return fragmentMode.getInt(fragment) == MODE && ((boolean) landscape.invoke(fragment)
                || root.getWidth() > root.getHeight());
    }

    private boolean usesLensColumnAfterLayout(Object fragment, View bar) {
        try {
            return usesLensColumn(fragment, bar);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private void resetLensPosition(View bar) {
        PointF original = originalPosition.remove(bar);
        if (original != null) {
            bar.setTranslationX(original.x);
            bar.setTranslationY(original.y);
        }
    }

    private void alignLensColumn(View bar, View toggle, boolean menuClosed) {
        View root = bar.getRootView();
        View shutter = root.findViewById(SHUTTER_BUTTON);
        if (shutter == null || !shutter.isLaidOut() || !toggle.isLaidOut()) return;
        float[] shutterCenter = center(shutter);
        float[] lensCenter = center(toggle);
        originalPosition.putIfAbsent(bar, new PointF(bar.getTranslationX(), bar.getTranslationY()));
        boolean horizontalWindow = root.getWidth() > root.getHeight();
        float deltaX = horizontalWindow ? shutterCenter[0] - lensCenter[0] : 0f;
        float deltaY = horizontalWindow ? 0f : shutterCenter[1] - lensCenter[1];
        offsetInWindow(bar, deltaX, deltaY);
        if (menuClosed) {
            Log.i("AuroraDirectorLens", "Menu closed: lens column moved by " + deltaX + ", " + deltaY);
        }
    }

    private static void alignFilterIndicator(View indicator, ViewGroup thumbnails, boolean verticalList) {
        for (int index = 0; index < thumbnails.getChildCount(); index++) {
            View thumbnail = thumbnails.getChildAt(index).findViewById(FILTER_THUMBNAIL);
            if (thumbnail == null || !thumbnail.isShown() || !thumbnail.isLaidOut()) continue;
            // Keep the snap target fixed along the scroll axis. Match the image center on the other axis.
            float[] image = center(thumbnail);
            float[] border = center(indicator);
            if (verticalList) offsetInWindow(indicator, image[0] - border[0], 0f);
            else offsetInWindow(indicator, 0f, image[1] - border[1]);
            return;
        }
    }

    private static void offsetInWindow(View view, float x, float y) {
        View parent = (View) view.getParent();
        Matrix parentToWindow = new Matrix();
        Matrix windowToParent = new Matrix();
        parent.transformMatrixToGlobal(parentToWindow);
        if (!parentToWindow.invert(windowToParent)) return;
        float[] offset = {x, y};
        windowToParent.mapVectors(offset);
        view.setTranslationX(view.getTranslationX() + offset[0]);
        view.setTranslationY(view.getTranslationY() + offset[1]);
    }

    private static float[] center(View view) {
        Matrix viewToWindow = new Matrix();
        view.transformMatrixToGlobal(viewToWindow);
        float[] point = {view.getWidth() / 2f, view.getHeight() / 2f};
        viewToWindow.mapPoints(point);
        return point;
    }
}
