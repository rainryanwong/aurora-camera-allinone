package com.aurora.camera.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

/** Uses the stock bottom tip and zoom protocols for the 75 mm near-range route. */
final class NearRangeUi {
    static final int TIP_ID = 36;
    private static final int REFRESH_ZOOM_SELECTION = -1;
    private static final int PANEL_CONTENT = 7;
    private static final Object[] NO_TIP_DATA = new Object[0];

    private java.lang.ref.WeakReference<Object> lastTip;
    private boolean lastActive;
    final Method showTip;
    final ThreadLocal<Boolean> rendering = new ThreadLocal<>();
    private final Method tips, tipVisible, baseModule, needNearTip;
    private final Method zoom, refreshZoom, zoomSlider, sliderShowing;
    private final Method mainContent, panelShowing, seekBar, seekBarShowing, ocr, ocrShowing;
    private final Method alerts;
    private final Constructor<?> nearToast;

    NearRangeUi(Host host) throws ReflectiveOperationException {
        tips = host.method("R6.p", "a");
        showTip = host.method("y4.j", "d6", int.class, boolean.class, boolean.class, Object[].class);
        tipVisible = host.method("R6.p", "t5", int.class);
        baseModule = host.method("com.android.camera.fragment.h", "getBaseModule");
        needNearTip = host.method("com.android.camera.module.Camera2Module", "isNeedNearRangeTip");
        zoom = host.method("W6.e", "a");
        refreshZoom = host.method("W6.e", "Gj", int.class, boolean.class);
        zoomSlider = host.method("W6.c", "a");
        sliderShowing = host.method("W6.c", "Ce");
        mainContent = host.method("R6.j0", "a");
        panelShowing = host.method("R6.j0", "e", int.class, int.class);
        seekBar = host.method("R6.y0", "a");
        seekBarShowing = host.method("R6.y0", "jg");
        ocr = host.method("Yp.a", "a");
        ocrShowing = host.method("Yp.a", "k7");
        alerts = host.method("R6.m1", "a");
        nearToast = host.type("X9.X0").getDeclaredConstructor(boolean.class, int.class);
        nearToast.setAccessible(true);
    }

    Object owner(Object tipFragment) throws ReflectiveOperationException {
        return ((Optional<?>) baseModule.invoke(tipFragment)).orElse(null);
    }

    void refreshZoomLabel() throws ReflectiveOperationException {
        Object panel = protocol(zoom);
        if (panel != null) refreshZoom.invoke(panel, REFRESH_ZOOM_SELECTION, false);
    }

    void renderTip(Object module, boolean nearby, boolean active) throws ReflectiveOperationException {
        Object tipFragment = protocol(tips);
        if (tipFragment == null || owner(tipFragment) != module) return;
        boolean visible = nearby && canShowTip(module);
        boolean shown = (boolean) tipVisible.invoke(tipFragment, TIP_ID);
        if (!visible && !shown) return;
        if (visible && shown && lastTip != null && lastTip.get() == tipFragment && lastActive == active) return;
        rendering.set(visible && active);
        try {
            showTip.invoke(tipFragment, TIP_ID, visible, visible && active, NO_TIP_DATA);
            if (lastTip == null || lastTip.get() != tipFragment) lastTip = new java.lang.ref.WeakReference<>(tipFragment);
            lastActive = active;
        } finally {
            rendering.remove();
        }
    }

    private boolean canShowTip(Object module) throws ReflectiveOperationException {
        // Match the visibility restrictions in the stock near-range interceptor s6.Z.
        if (!(boolean) needNearTip.invoke(module) || showing(zoomSlider, sliderShowing)
                || showing(seekBar, seekBarShowing) || showing(ocr, ocrShowing)) return false;
        Object panels = protocol(mainContent);
        return panels == null || (!(boolean) panelShowing.invoke(panels, PANEL_CONTENT, 251)
                && !(boolean) panelShowing.invoke(panels, PANEL_CONTENT, 4018)
                && !(boolean) panelShowing.invoke(panels, PANEL_CONTENT, 254));
    }

    @SuppressWarnings("unchecked")
    void showToggleMessage(boolean wasActive) throws ReflectiveOperationException {
        Object alert = protocol(alerts);
        if (alert != null) {
            ((Consumer<Object>) nearToast.newInstance(wasActive, 1)).accept(alert);
        }
    }

    private static boolean showing(Method getter, Method visibility) throws ReflectiveOperationException {
        Object panel = protocol(getter);
        return panel != null && (boolean) visibility.invoke(panel);
    }

    private static Object protocol(Method getter) throws ReflectiveOperationException {
        return ((Optional<?>) getter.invoke(null)).orElse(null);
    }
}
