package com.aurora.camera.compat;

import android.hardware.camera2.CaptureResult;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** F12: apply the request model/motion policy before its original ASD consumer. */
public final class AiRequestHooks {
    private static final String SCOPE = "aurora.compat.ai.requestParse";
    private AiRequestHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> modelType = host.type("rn.a");
        final Class<?> debugType = host.type("un.d");
        final Class<?> payloadType = host.type("s6.l");
        final Constructor<?> constructor = modelType.getDeclaredConstructor();
        constructor.setAccessible(true);
        final Constructor<?> snapshot = host.type("wn.c").getDeclaredConstructor(
                String.class, byte[].class, String.class, host.type("A9.o"));
        snapshot.setAccessible(true);
        final Field staticTag = host.field(modelType, "o");
        final Field modelTag = host.field(modelType, "l");
        final Field motion = host.field(modelType, "g");
        final Field model = host.field("s6.m", "b");
        final Field payloadModel = host.field(payloadType, "a");
        final Method parse = host.method("s6.m", "parseComplexValueManually", CaptureResult.class);
        final Method consume = host.type("com.android.camera.features.mode.ai.AiModule").getMethod(
                "onAsdChanged", host.type("com.android.camera.module.interceptor.base.d"));
        consume.setAccessible(true);
        if (modelTag.getType() != String.class || staticTag.getType() != String.class
                || motion.getType() != Float.class || model.getType() != modelType
                || payloadModel.getType() != modelType || parse.getReturnType() != void.class
                || consume.getReturnType() != void.class || Modifier.isAbstract(consume.getModifiers()))
            throw new NoSuchMethodException("F12 AI request producer/consumer contract");
        return new HookPlan() {
            public String id() { return "F12.ai-request-profile-motion"; }
            public void install() {
                final Policy policy = new Policy(staticTag, modelTag, motion);
                final ThreadLocal<Frame> frames = new ThreadLocal<>();
                XposedBridge.hookMethod(constructor, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (!p.hasThrowable()) policy.tag(p.thisObject);
                    }
                });
                XposedBridge.hookMethod(snapshot, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || !"cameraAiParam".equals(p.args[2])) return;
                        Object request = p.args[3];
                        if (request != null && (request.getClass() == modelType
                                || request.getClass() == debugType)) policy.snapshot(request);
                    }
                });
                XposedBridge.hookMethod(parse, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        Frame frame = new Frame(model.get(p.thisObject), frames.get());
                        p.setObjectExtra(SCOPE, frame); frames.set(frame);
                    }
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        Frame frame = (Frame) p.getObjectExtra(SCOPE);
                        if (frame.previous == null) frames.remove(); else frames.set(frame.previous);
                        // Exceptions retain the original parser failure. Successful retained state
                        // and the synchronous consumer both observe the accepted request policy.
                        if (!p.hasThrowable()) policy.apply(frame.model);
                    }
                });
                XposedBridge.hookMethod(consume, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        Frame frame = frames.get();
                        Object payload = p.args[0];
                        if (frame != null && payloadType.isInstance(payload)
                                && payloadModel.get(payload) == frame.model) policy.apply(frame.model);
                    }
                });
            }
        };
    }

    private static final class Frame {
        final Object model;
        final Frame previous;
        Frame(Object model, Frame previous) { this.model = model; this.previous = previous; }
    }
    private static final class Policy {
        final Field staticTag, tag, motion;
        String profile;
        Policy(Field staticTag, Field tag, Field motion) {
            this.staticTag = staticTag; this.tag = tag; this.motion = motion;
        }
        synchronized void tag(Object request) throws IllegalAccessException {
            if (profile == null) {
                // The original <clinit> already evaluated the immutable _beta suffix.
                String original = (String) staticTag.get(null);
                profile = "madrid" + (original.endsWith("_beta") ? "_beta" : "");
            }
            tag.set(request, profile);
        }
        void apply(Object request) throws IllegalAccessException {
            motion.set(request, Float.valueOf(0.0f));
            tag(request);
        }
        void snapshot(Object request) throws IllegalAccessException {
            // ASD reuses its producer object on a different thread. The original
            // request lambdas have now copied it into this independent snapshot.
            // Their absent-source null fields must survive unchanged.
            if (motion.get(request) != null) motion.set(request, Float.valueOf(0.0f));
            if (tag.get(request) != null) tag(request);
        }
    }
}
