package com.aurora.camera.livemotion;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.os.Handler;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.IntFunction;

/** Joins real final-JPEG handoff and video-save success for the same capture. */
final class CaptureCompletion {
    private final Method snapshot, deliverJpeg, videoSaved, cancelled, failed, jpegReady, type;
    private final Field callbackOwner, imageCallback, currentTask, allImages, handler;
    private final Constructor<?> nativeCompletion;
    private volatile Object active;
    private volatile Shot current;

    CaptureCompletion(ClassLoader cl, Class<?> master, Method type)
            throws ReflectiveOperationException {
        this.type = type;
        Class<?> task = Class.forName("Sh.s", false, cl);
        Class<?> manager = Class.forName("Gm.s", false, cl);
        Class<?> callback = Class.forName(master.getName() + "$b", false, cl);
        Class<?> saver = Class.forName("l7.j", false, cl);
        snapshot = manager.getDeclaredMethod("O4", task, Class.forName("l7.Q", false, cl),
                boolean.class, int.class);
        deliverJpeg = saver.getDeclaredMethod("D", task, CaptureResult.class,
                CameraCharacteristics.class, String.class, IntFunction.class);
        videoSaved = saver.getDeclaredMethod("i", Object.class, String.class, long.class, int.class);
        cancelled = callback.getDeclaredMethod("j", Object.class);
        failed = callback.getDeclaredMethod("l", Object.class, Exception.class);
        jpegReady = task.getDeclaredMethod("o");
        callbackOwner = LiveMotionModule.field(callback, "a");
        imageCallback = LiveMotionModule.field(manager, "h0");
        currentTask = LiveMotionModule.field(master, "mParallelTaskData");
        allImages = LiveMotionModule.field(master, "mIsAllImageReceived");
        handler = LiveMotionModule.field(master, "mHandler");
        nativeCompletion = Class.forName("Eh.a", false, cl)
                .getDeclaredConstructor(Object.class, int.class);
    }

    void activate(Object module) { active = module; }

    void deactivate(Object module) {
        if (active == module) { active = null; current = null; }
    }

    void install() {
        XposedBridge.hookMethod(snapshot, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                Object cb = p.args[1];
                if (!callbackOwner.getDeclaringClass().isInstance(cb)) return;
                Object module = callbackOwner.get(cb);
                String selected = selectedType();
                if (module != active || (!"2".equals(selected) && !"3".equals(selected))
                        || currentTask.get(module) != p.args[0]) return;
                Shot old = current;
                if (old != null && old.module == module && old.task == p.args[0]) return;
                Runnable completion = (Runnable) nativeCompletion.newInstance(
                        imageCallback.get(p.thisObject), 2);
                current = new Shot(module, p.args[0], selected, completion,
                        (Handler) handler.get(module));
                LiveMotionModule.log("capture bind task=" + System.identityHashCode(p.args[0])
                        + " type=" + selected);
            }
        });
        XposedBridge.hookMethod(deliverJpeg, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                Shot shot = matching(p.args[0]);
                if (shot != null && (Boolean) jpegReady.invoke(shot.task)) {
                    p.setObjectExtra("Aurora.finalJpeg", shot);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Shot shot = (Shot) p.getObjectExtra("Aurora.finalJpeg");
                if (shot != null && !p.hasThrowable()) signal(shot, true);
            }
        });
        XposedBridge.hookMethod(videoSaved, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Shot shot = matching(p.args[0]);
                if (shot != null && !p.hasThrowable()) signal(shot, false);
            }
        });
        XC_MethodHook cancel = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                Shot shot = matching(p.args[0]);
                if (shot != null) {
                    current = null;
                    LiveMotionModule.log("capture cancelled task=" + System.identityHashCode(shot.task));
                }
            }
        };
        XposedBridge.hookMethod(cancelled, cancel);
        XposedBridge.hookMethod(failed, cancel);
    }

    private Shot matching(Object task) {
        Shot shot = current;
        return shot != null && shot.task == task && shot.module == active ? shot : null;
    }

    boolean owns(Object task) { return matching(task) != null; }

    private String selectedType() {
        try { return (String) type.invoke(null, 231); }
        catch (ReflectiveOperationException e) {
            throw new IllegalStateException("6.8 movement type contract failed", e);
        }
    }

    private void signal(final Shot shot, boolean jpeg) {
        synchronized (shot) {
            if (current != shot || active != shot.module) return;
            if (jpeg) shot.jpeg = true; else shot.video = true;
            LiveMotionModule.log("capture media task=" + System.identityHashCode(shot.task)
                    + " jpeg=" + shot.jpeg + " video=" + shot.video);
            if (!shot.jpeg || !shot.video || shot.queued) return;
            shot.queued = true;
        }
        if (!shot.handler.post(new Runnable() {
            @Override public void run() {
                if (current != shot || active != shot.module || !shot.type.equals(selectedType())) return;
                try {
                    if (currentTask.get(shot.module) != shot.task || allImages.getBoolean(shot.module)) return;
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException("6.8 capture completion contract failed", e);
                }
                LiveMotionModule.log("capture native completion task=" + System.identityHashCode(shot.task));
                shot.completion.run();
            }
        })) LiveMotionModule.log("capture completion post rejected task=" + System.identityHashCode(shot.task));
    }

    private static final class Shot {
        final Object module, task;
        final String type;
        final Runnable completion;
        final Handler handler;
        boolean jpeg, video, queued;
        Shot(Object module, Object task, String type, Runnable completion, Handler handler) {
            this.module = module; this.task = task; this.type = type;
            this.completion = completion; this.handler = handler;
        }
    }
}
