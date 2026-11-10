package com.aurora.camera.compat;

import com.aurora.camera.compat.aperture.AperturePort;

/** AI real aperture, six-selector reset mirror and professional programmatic sync. */
public final class ApertureHooks {
    private ApertureHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final AperturePort port = new AperturePort(host);
        return new HookPlan() {
            @Override public String id() { return "ai-aperture-reset-programmatic-1770"; }
            @Override public void install() { port.install(); }
        };
    }
}
