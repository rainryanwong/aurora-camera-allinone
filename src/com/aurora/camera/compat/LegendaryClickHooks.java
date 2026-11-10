package com.aurora.camera.compat;

import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** F28: block unsupported cards; use black-white film for an unsaved legendary choice. */
public final class LegendaryClickHooks {
    private LegendaryClickHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method click = host.method("c6.d", "onClick", View.class);
        final Method defaultValue = host.method("r2.B", "getDefaultValue", int.class);
        final Method busy = host.method("V6.b", "b");
        final Method position = host.method(
                "androidx.recyclerview.widget.RecyclerView$C", "getBindingAdapterPosition");
        final Field adapter = host.field("c6.d", "a");
        final Field holder = host.field("c6.d", "b");
        final Field selected = host.field("c6.e", "c");
        final Field items = host.field("c6.e", "b");
        final Field token = host.field("com.android.camera.data.data.d", "r");
        final com.aurora.camera.compat.legend.M3Preview260 preview=new com.aurora.camera.compat.legend.M3Preview260(host);
        if (click.getReturnType() != void.class || defaultValue.getReturnType() != String.class
                || busy.getReturnType() != boolean.class
                || position.getReturnType() != int.class || selected.getType() != int.class
                || token.getType() != String.class || items.getType() != List.class
                || adapter.getType() != host.type("c6.e")
                || holder.getType() != host.type("c6.e$a")) {
            throw new NoSuchMethodException("F28 legendary card contract differs from stock 1770");
        }
        return new HookPlan() {
            public String id() { return "F28.legendary-card-click"; }
            public void install() {
                try{preview.install();}catch(Exception failure){throw new IllegalStateException("M3 preview installation failed",failure);}
                XposedBridge.hookMethod(defaultValue, new StrictHook() {
                    @Override protected void after(MethodHookParam p) {
                        // ComponentData computes this default, then reads its key using
                        // the computed fallback. Stored values retain precedence; no
                        // preference write or explicit-selection override occurs here.
                        if (!p.hasThrowable() && ((Integer) p.args[0]) == 256) {
                            p.setResult("M3");
                        }
                    }
                });
                XposedBridge.hookMethod(click, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        Object owner = adapter.get(p.thisObject);
                        owner.getClass(); // The original callback also requires its captured adapter.
                        // This is the original read-only busy query, before accessing a stale holder.
                        if ((Boolean) busy.invoke(null)) return;
                        int index = (Integer) position.invoke(holder.get(p.thisObject));
                        if (index == -1 || index == selected.getInt(owner)) return;
                        List<?> list = (List<?>) items.get(owner);
                        if (index >= 0 && index < list.size()
                                && blocked((String) token.get(list.get(index)))) {
                            p.setResult(null);
                        }
                    }
                });
            }
        };
    }

    static boolean blocked(String token) {
        return "M10R".equals(token) || "M9".equals(token);
    }
}
