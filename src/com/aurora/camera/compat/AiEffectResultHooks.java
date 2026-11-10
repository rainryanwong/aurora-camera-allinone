package com.aurora.camera.compat;

import android.os.Handler;
import android.view.View;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** F12: preserve the effect list on a null callback, with the original UI/task tail. */
public final class AiEffectResultHooks {
    private static final String INIT_SCOPE = "aurora.compat.ai.effectInitScope";
    private AiEffectResultHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> fragment = host.type("com.android.camera.features.mode.ai.FragmentAi");
        final Class<?> listType = host.type("z3.b");
        final Method result = host.method(fragment, "j5", listType);
        final Method init = host.method(fragment, "Qs");
        final Method updateSelection = host.method(fragment, "Us", int.class, boolean.class);
        final Method source = host.method(fragment, "Es");
        final Method mode = host.method(fragment, "Js");
        final Method select = host.method(fragment, "Bs", int.class, boolean.class);
        final Method finish = host.method(fragment, "ft", String.class, boolean.class);
        final Method baseModule = fragment.getMethod("getBaseModule");
        final Method getList = listType.getMethod("getList");
        final Method stage = host.method("G6.u", "i");
        final Method stamp = host.method("G6.u", "g", String.class);
        final Field current = host.field(fragment, "q0");
        final Field listFinal = host.field(listType, "f");
        final Field selected = host.field("U9.S", "n");
        final Field adapter = host.field(fragment, "N");
        final Field position = host.field("com.android.camera.features.mode.ai.FragmentAi$b", "l");
        final Field task = host.field(fragment, "f");
        final Method cancel = host.method("A3.q", "b");
        final Field state = host.field(fragment, "I0");
        final Field defaultState = host.field("com.android.camera.features.mode.ai.FragmentAi$c", "b");
        final Field controller = host.field(fragment, "W0");
        final Field complete = host.field("com.android.camera.features.mode.ai.a$b", "f");
        final Method transition = host.method("com.android.camera.features.mode.ai.a", "c", complete.getType());
        final Field handler = host.field(fragment, "c");
        final Field overlay = host.field(fragment, "H");
        final Constructor<?> callback = host.type("X1.g").getDeclaredConstructor(Object.class, int.class);
        final Constructor<?> consumer = host.type("A9.V").getDeclaredConstructor(Object.class, int.class);
        final Constructor<?> runnable = host.type("Cp.f").getDeclaredConstructor(Object.class, int.class);
        callback.setAccessible(true); consumer.setAccessible(true); runnable.setAccessible(true);
        getList.setAccessible(true); baseModule.setAccessible(true);
        if (source.getReturnType() != listType || current.getType() != listType
                || mode.getReturnType() != boolean.class || listFinal.getType() != boolean.class
                || position.getType() != int.class || handler.getType() != Handler.class
                || overlay.getType() != View.class || baseModule.getReturnType() != Optional.class)
            throw new NoSuchMethodException("F12 effect list/UI contract");
        return new HookPlan() {
            public String id() { return "F12.ai-effect-result-state"; }
            public void install() {
                final ThreadLocal<InitFrame> frames = new ThreadLocal<>();
                XposedBridge.hookMethod(init, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        InitFrame frame = new InitFrame(p.thisObject, frames.get());
                        p.setObjectExtra(INIT_SCOPE, frame);
                        frames.set(frame);
                    }
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        InitFrame frame = (InitFrame) p.getObjectExtra(INIT_SCOPE);
                        if (frame.previous == null) frames.remove(); else frames.set(frame.previous);
                        if (!p.hasThrowable() && frame.reachedSelection) {
                            View button = (View) overlay.get(p.thisObject);
                            if (button != null) button.setAlpha(1.0f);
                        }
                    }
                });
                XposedBridge.hookMethod(updateSelection, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        InitFrame frame = frames.get();
                        if (frame == null || frame.owner != p.thisObject) return;
                        if (current.get(p.thisObject) == null)
                            current.set(p.thisObject, source.invoke(p.thisObject));
                        frame.reachedSelection = true;
                    }
                });
                XposedBridge.hookMethod(result, new XC_MethodReplacement() {
                    @Override @SuppressWarnings("unchecked")
                    protected Object replaceHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                        Object owner = p.thisObject;
                        Object incoming = p.args[0];
                        stamp.invoke(stage.invoke(null), "ai_cloud_step6_show_result");
                        Object previous = current.get(owner);
                        // This guard must run before null fallback. Replacing args in a beforeHook
                        // would incorrectly short-circuit when the saved list has f=true.
                        if (previous == null || !listFinal.getBoolean(previous)
                                || incoming == null || !listFinal.getBoolean(incoming)) {
                            if (incoming == null) incoming = previous;
                            current.set(owner, incoming);
                            int index;
                            if ((Boolean) mode.invoke(owner)) {
                                index = -1;
                                if (incoming != null) {
                                    List<?> items = (List<?>) getList.invoke(incoming);
                                    if (items != null) {
                                        for (int i = 0; i < items.size(); i++) {
                                            if (selected.getBoolean(items.get(i))) { index = i; break; }
                                        }
                                    }
                                }
                            } else {
                                Object ui = adapter.get(owner);
                                index = ui == null ? 0 : position.getInt(ui);
                            }
                            if (index >= 0) select.invoke(owner, index, false);
                            updateSelection.invoke(owner, 0, false);
                        }
                        Object oldTask = task.get(owner);
                        if (oldTask != null) cancel.invoke(oldTask);
                        state.set(owner, defaultState.get(null));
                        finish.invoke(owner, "onAiEffectResult", true);
                        transition.invoke(controller.get(owner), complete.get(null));
                        Object function = callback.newInstance(incoming, 5);
                        ((Optional<?>) baseModule.invoke(owner)).ifPresent(
                                (Consumer<Object>) consumer.newInstance(function, 11));
                        ((Handler) handler.get(owner)).post((Runnable) runnable.newInstance(owner, 11));
                        return null;
                        } catch (InvocationTargetException failure) {
                            // Reflection adds this wrapper; the original callback did not.
                            throw failure.getCause();
                        }
                    }
                });
            }
        };
    }

    private static final class InitFrame {
        final Object owner;
        final InitFrame previous;
        boolean reachedSelection;
        InitFrame(Object owner, InitFrame previous) { this.owner = owner; this.previous = previous; }
    }
}
