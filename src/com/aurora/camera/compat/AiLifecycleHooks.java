package com.aurora.camera.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/** F13: cancel the task owned by the departing AI module, not a newer task. */
public final class AiLifecycleHooks {
    private static final String BINDING = "aurora.compat.ai.tuningBinding";
    private static final String SCOPE = "aurora.compat.ai.acceptScope";
    private AiLifecycleHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> fragmentType = host.type("com.android.camera.features.mode.ai.FragmentAi");
        final Class<?> aiType = host.type("com.android.camera.features.mode.ai.AiModule");
        final Class<?> cameraBase = host.type("com.android.camera.a");
        final Class<?> taskType = host.type("A3.q");
        final Method accept = host.method(fragmentType, "am", taskType);
        final Method inactive = host.method(aiType, "onInactive");
        final Method activity = fragmentType.getMethod("getActivity");
        final Method context = fragmentType.getMethod("getContext");
        final Method manager = host.method(cameraBase, "as");
        final Method fragmentMode = host.method(fragmentType, "Js");
        final Field activeModule = host.field("ph.b", "o");
        final Field task = host.field(fragmentType, "f");
        final Field finished = host.field(fragmentType, "y0");
        final Field state = host.field(fragmentType, "I0");
        final Field recognizing = host.field("com.android.camera.features.mode.ai.FragmentAi$c", "c");
        final Field controller = host.field("A3.f", "a");
        final Field running = host.field("A3.s", "e");
        final Field identity = host.field(taskType, "b");
        final Method cancel = host.method(taskType, "b");
        final Method title = host.method(fragmentType, "P9", String.class, String.class, boolean.class);
        final Method result = host.method(fragmentType, "j5", host.type("z3.b"));
        if (task.getType() != taskType || finished.getType() != boolean.class
                || context.getReturnType() != Context.class || cancel.getReturnType() != void.class
                || manager.getReturnType() != host.type("ph.b") || state.getType() != recognizing.getType())
            throw new NoSuchMethodException("F13 AI task/lifecycle contract");
        activity.setAccessible(true); context.setAccessible(true);
        return new HookPlan() {
            public String id() { return "F13.ai-tuning-lifecycle"; }
            public void install() {
                final Handler main = new Handler(Looper.getMainLooper());
                final ThreadLocal<AcceptFrame> frames = new ThreadLocal<>();
                XposedBridge.hookMethod(accept, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        AcceptFrame frame = new AcceptFrame(p.thisObject, p.args[0], frames.get());
                        p.setObjectExtra(SCOPE, frame); frames.set(frame);
                    }
                    @Override protected void after(MethodHookParam p) {
                        AcceptFrame frame = (AcceptFrame) p.getObjectExtra(SCOPE);
                        if (frame.previous == null) frames.remove(); else frames.set(frame.previous);
                    }
                });
                XposedBridge.hookMethod(manager, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        AcceptFrame frame = frames.get();
                        if (frame != null && !frame.bound && !p.hasThrowable()) {
                            // Capture the same manager result consumed by the original am method.
                            Object module = activeModule.get(p.getResult());
                            if (aiType.isInstance(module)) frame.module = module;
                        }
                    }
                });
                XposedBridge.hookMethod(fragmentMode, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        AcceptFrame frame = frames.get();
                        if (frame == null || frame.owner != p.thisObject
                                || frame.bound || frame.module == null) return;
                        synchronized (frame.module) {
                            XposedHelpers.setAdditionalInstanceField(frame.module, BINDING,
                                    new Binding(frame.owner, frame.task));
                        }
                        frame.bound = true;
                    }
                });
                XposedBridge.hookMethod(inactive, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        final Binding binding;
                        synchronized (p.thisObject) {
                            binding = (Binding) XposedHelpers.removeAdditionalInstanceField(p.thisObject, BINDING);
                        }
                        if (binding == null || binding.task == null) return;
                        main.post(new Runnable() {
                            @Override public void run() {
                                try {
                                    Object current = running.get(controller.get(null));
                                    if (current != identity.get(binding.task)) return;
                                    cancel.invoke(binding.task);
                                    Object fragment = binding.fragment;
                                    if (fragment == null || task.get(fragment) != binding.task
                                            || finished.getBoolean(fragment)
                                            || state.get(fragment) != recognizing.get(null)) return;
                                    Log.i("AiTuningLifecycle", "origin module inactive: finish matching tuning task");
                                    if (context.invoke(fragment) != null) {
                                        String message = "zh".equals(Locale.getDefault().getLanguage())
                                                ? "识别已中断，请重试" : "Recognition interrupted. Please retry.";
                                        title.invoke(fragment, message, null, false);
                                    }
                                    result.invoke(fragment, new Object[] {null});
                                    task.set(fragment, null);
                                } catch (InvocationTargetException failure) {
                                    AiLifecycleHooks.<RuntimeException>raise(failure.getCause());
                                } catch (IllegalAccessException failure) {
                                    throw new IllegalStateException("F13 resolved lifecycle binding became inaccessible", failure);
                                }
                            }
                        });
                    }
                });
            }
        };
    }

    /** Only bridges the Runnable throws declaration; preserves the same Throwable. */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void raise(Throwable failure) throws T {
        throw (T) failure;
    }

    private static final class Binding {
        final Object fragment, task;
        Binding(Object fragment, Object task) { this.fragment = fragment; this.task = task; }
    }
    private static final class AcceptFrame {
        final Object owner, task;
        final AcceptFrame previous;
        Object module;
        boolean bound;
        AcceptFrame(Object owner, Object task, AcceptFrame previous) {
            this.owner = owner; this.task = task; this.previous = previous;
        }
    }
}
