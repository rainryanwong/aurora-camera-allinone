package com.aurora.camera.compat;

import com.aurora.camera.compat.lens.LensPort;
import java.util.ArrayList;

/** Physical lens roles and their accepted professional/high-pixel consumers. */
public final class LensRouteHooks {
    private static volatile LensPort installed;
    private LensRouteHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final LensPort port=new LensPort(host);
        return new HookPlan() {
            @Override public String id() { return "physical-lens-route-1770"; }
            @Override public void install() { port.install(); installed=port; }
        };
    }

    /** Shared X5/f contract; preserves the accepted professional stop completion. */
    public static void completeProStops(ArrayList<Float> values,int mode)
            throws ReflectiveOperationException {
        LensPort port=installed;
        if (port==null) throw new IllegalStateException("physical lens hooks are not installed");
        port.completeProStops(values,mode);
    }
}
