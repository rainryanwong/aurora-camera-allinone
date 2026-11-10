package com.aurora.camera.compat;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

/** F11: keep the complete accepted click dispatcher without mutating y0 task state. */
public final class AiClickHooks {
    private AiClickHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> fragment = host.type("com.android.camera.features.mode.ai.FragmentAi");
        final Method click = host.method(fragment, "onClick", View.class);
        final Method busy = host.method("V6.b", "b");
        final Field smartView = host.field(fragment, "i");
        final Field mode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        final Method smartDefault = host.method("com.android.camera.data.data.x", "t0", int.class);
        final Method event = host.method("yq.c", "g", Object.class, String.class, String.class);
        final Method toggle = host.method(fragment, "zt", boolean.class);
        final Method data = host.method("g2.a", "g");
        final Method component = host.method("Xh.b", "x", Class.class);
        final Class<?> smartComponent = host.type("u2.H");
        final Method store = host.method("com.android.camera.data.data.c", "setComponentValue", int.class, String.class);
        final Method protocol = host.method("R6.D", "a");
        final Constructor<?> action = host.type("Q4.Q").getDeclaredConstructor(Object.class, int.class);
        final Constructor<?> consumer = host.type("N6.s").getDeclaredConstructor(Object.class, int.class);
        final Method panel = host.method(fragment, "pt", fragment, boolean.class);
        final Method refresh = host.method(fragment, "Os", boolean.class);
        final Method effect = host.method(fragment, "Qs");
        final Method pose = host.method(fragment, "Rs", boolean.class);
        final Class<?> stateType = host.type("com.android.camera.features.mode.ai.FragmentAi$c");
        final Field tuningState = host.field(stateType, "c");
        final Field poseState = host.field(stateType, "d");
        final Method enterState = host.method(fragment, "ys", stateType);
        final Method isState = host.method(fragment, "vs", stateType);
        final Field handler = host.field(fragment, "c");
        final Constructor<?> delayed = host.type("E1.r3").getDeclaredConstructor(Object.class, int.class);
        final Field poseUi = host.field(fragment, "h");
        final Field poseUiState = host.field("com.android.camera.features.mode.ai.c", "f0");
        final Field poseBlocked = host.field("com.android.camera.features.mode.ai.c$g", "d");
        final Method resetPose = host.method(fragment, "Xs");
        final Method resetGuide = host.method(fragment, "Ys", host.type("C4.w"));
        final Method resetUi = host.method(fragment, "Gs");
        final Method poseComponent = host.method(fragment, "Cs");
        final Field empty = host.field("hv.v", "a");
        final Field poses = host.field("v2.e", "b");
        final Field oldIndex = host.field("v2.e", "c");
        final Field newIndex = host.field("v2.e", "d");
        final Field poseToken = host.field("v2.e", "e");
        final Field storedPoses = host.field(fragment, "a0");
        final Method finish = host.method(fragment, "ft", String.class, boolean.class);
        final Method context = fragment.getMethod("getContext");
        final Method requireContext = host.method("wv.l", "f", Object.class, String.class);
        final Class<?> camera = host.type("com.android.camera.Camera");
        final Method manager = host.method("com.android.camera.a", "as");
        final Field activeModule = host.field("ph.b", "o");
        final Field state = host.field(fragment, "I0");
        final Method moduleIndex = host.method("com.android.camera.module.a0", "getModuleIndex");
        final Field controller = host.field("A3.D", "a");
        final Method resetRequest = host.method("A3.g", "x", int.class);
        final Field tag = host.field("com.xiaomi.camera.base.ui.fragments.d", "TAG");
        final Method log = host.method("com.android.camera.log.Log", "d", String.class, String.class, Object[].class);
        for (Constructor<?> constructor : new Constructor<?>[] {action, consumer, delayed}) constructor.setAccessible(true);
        context.setAccessible(true);
        if (click.getReturnType() != void.class || mode.getType() != int.class
                || busy.getReturnType() != boolean.class || smartView.getType() != View.class
                || isState.getReturnType() != boolean.class || protocol.getReturnType() != Optional.class
                || context.getReturnType() != Context.class || handler.getType() != Handler.class)
            throw new NoSuchMethodException("F11 AI click dispatcher contract");
        return new HookPlan() {
            public String id() { return "F11.ai-click-policy"; }
            public void install() {
                XposedBridge.hookMethod(click, new XC_MethodReplacement() {
                    @Override @SuppressWarnings("unchecked")
                    protected Object replaceHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                        Object owner = p.thisObject;
                        if ((Boolean) busy.invoke(null)) return null;
                        View view = (View) p.args[0];
                        if (view == null) return null;
                        int id = view.getId();
                        // IDs belong to the byte-pinned 1770 table; stable names are in the evidence.
                        if (id == 0x7f0b0097) {
                            log.invoke(null, tag.get(owner), "onFeatureSmartComposition", new Object[0]);
                            View bound = (View) smartView.get(owner);
                            Object value = bound == null ? null : bound.getTag();
                            boolean enabled = value instanceof Boolean ? (Boolean) value
                                    : (Boolean) smartDefault.invoke(null, mode.getInt(owner));
                            String selected = enabled ? "OFF" : "ON";
                            event.invoke(null, selected, "M_ai_mode_", "ai_mode_composition");
                            toggle.invoke(owner, !enabled);
                            Object item = component.invoke(data.invoke(null), smartComponent);
                            if (item != null) store.invoke(item, mode.getInt(owner), selected);
                            Object callback = action.newInstance(selected, 1);
                            ((Optional<?>) protocol.invoke(null)).ifPresent(
                                    (Consumer<Object>) consumer.newInstance(callback, 10));
                            panel.invoke(null, owner, false);
                            refresh.invoke(owner, true);
                        } else if (id == 0x7f0b0092) {
                            effect.invoke(owner);
                        } else if (id == 0x7f0b0095) {
                            pose.invoke(owner, false);
                        } else if (id == 0x7f0b0089) {
                            enterState.invoke(owner, tuningState.get(null));
                        } else if (id == 0x7f0b00a3) {
                            Object desired = poseState.get(null);
                            if (!(Boolean) isState.invoke(owner, desired)) {
                                ((Handler) handler.get(owner)).postDelayed(
                                        (Runnable) delayed.newInstance(owner, 10), 3000L);
                                return null;
                            }
                            Object ui = poseUi.get(owner);
                            if (ui != null && poseUiState.get(ui) == poseBlocked.get(null)) return null;
                            log.invoke(null, tag.get(owner), "onPoseGuideReset", new Object[0]);
                            resetPose.invoke(owner);
                            resetGuide.invoke(owner, new Object[] {null});
                            resetUi.invoke(owner);
                            Object item = poseComponent.invoke(null);
                            Object emptyList = empty.get(null);
                            if (item != null) {
                                poses.set(item, emptyList);
                                oldIndex.setInt(item, -1);
                                newIndex.setInt(item, -1);
                                poseToken.set(item, "");
                            }
                            storedPoses.set(owner, emptyList);
                            finish.invoke(owner, "onPoseGuideReset", false);
                            Object activity = context.invoke(owner);
                            requireContext.invoke(null, activity,
                                    "null cannot be cast to non-null type com.android.camera.Camera");
                            Object module = activeModule.get(manager.invoke(camera.cast(activity)));
                            state.set(owner, desired);
                            int index = (Integer) moduleIndex.invoke(module);
                            controller.get(null); // Preserve original controller initialization.
                            resetRequest.invoke(null, index);
                        }
                        return null;
                        } catch (InvocationTargetException failure) {
                            // Preserve the original call's Throwable and its exact stop point.
                            throw failure.getCause();
                        }
                    }
                });
            }
        };
    }
}
