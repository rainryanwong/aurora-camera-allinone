package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;

/** Keep the selected photography style outside the AI recommendation workspace. */
public final class AiStyleLockHooks {
    private AiStyleLockHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> aiWorkspace = host.type("z3.c");
        final Class<?> photographyStyle = host.type("r2.m");
        final Method initParameters = host.method(aiWorkspace, "i", int.class);
        final Field parameters = host.field("U9.S", "q");
        if (initParameters.getReturnType() != void.class || parameters.getType() != ArrayList.class)
            throw new NoSuchMethodException("AI workspace parameter-list contract");
        return new HookPlan() {
            public String id() { return "F25.ai-recommendation-style-isolation"; }
            public void install() {
                XposedBridge.hookMethod(initParameters, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || (Integer) p.args[0] != 168) return;
                        // z3.c owns this list; its u0 donor was copied with addAll.
                        // U9.S.U/P/c/K all enumerate it. Excluding this one component
                        // prevents both the default cv_type=0 and cached recipe values
                        // from overriding the live AI-specific style preference.
                        // Filtering the apply setter alone would break c/K matching.
                        for (Iterator<?> it = ((ArrayList<?>) parameters.get(p.thisObject)).iterator();
                                it.hasNext();) {
                            if (photographyStyle.isInstance(it.next())) it.remove();
                        }
                    }
                });
            }
        };
    }
}
