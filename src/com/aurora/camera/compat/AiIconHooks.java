package com.aurora.camera.compat;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** F28: keep the existing binding/animation and hide only the exact stock icon. */
public final class AiIconHooks {
    private AiIconHooks() {}
    // Mapping data for the SHA-pinned 1770 resource table, not a cross-version ID.
    private static final int SMART_COMPOSITION_ID = 0x7f0b0097;

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method bind = host.method("com.android.camera.features.mode.ai.FragmentAi",
                "us", int.class, View.class);
        if (bind.getReturnType() != View.class)
            throw new NoSuchMethodException("F28 FragmentAi.us return type");
        return new HookPlan() {
            public String id() { return "F28.ai-middle-icon"; }
            public void install() {
                XposedBridge.hookMethod(bind, new StrictHook() {
                    @Override protected void after(MethodHookParam p) {
                        if (!p.hasThrowable() && (Integer) p.args[0] == SMART_COMPOSITION_ID) {
                            View icon = (View) p.getResult();
                            if (icon != null) icon.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };
    }
}
