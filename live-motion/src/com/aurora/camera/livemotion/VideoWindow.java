package com.aurora.camera.livemotion;

import android.media.MediaFormat;
import android.os.SystemClock;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Retains the native MasterLive post-shutter window before publishing its snapshot. */
final class VideoWindow {
    private final CaptureCompletion captures;
    private final Method record, baseSnapshot, videoSnapshot;
    private final Constructor<?> request;
    private final Class<?> videoClass;
    private final Field videoEncoder, config, master, red, main, free;
    private final Field snapshots, format, restart, head, tail, snap, maxDuration;
    private final ThreadLocal<Scope> scope = new ThreadLocal<>();

    VideoWindow(ClassLoader cl, CaptureCompletion captures) throws ReflectiveOperationException {
        this.captures = captures;
        Class<?> recorder = Class.forName("Gm.b", false, cl);
        Class<?> settings = Class.forName("Gm.c", false, cl);
        Class<?> base = Class.forName("Hm.h", false, cl);
        Class<?> window = Class.forName("Hm.p", false, cl);
        videoClass = Class.forName("Hm.k", false, cl);
        record = recorder.getDeclaredMethod("g", int.class, Class.forName("Gm.y", false, cl),
                Object.class, int.class, boolean.class, boolean.class, String.class,
                boolean.class, int.class);
        Class<?>[] params = { int.class, long.class, boolean.class, boolean.class, boolean.class };
        baseSnapshot = base.getDeclaredMethod("u", params);
        videoSnapshot = videoClass.getDeclaredMethod("u", params);
        videoEncoder = LiveMotionModule.field(recorder, "b");
        config = LiveMotionModule.field(recorder, "m");
        master = LiveMotionModule.field(settings, "l");
        red = LiveMotionModule.field(settings, "m");
        main = LiveMotionModule.field(settings, "n");
        free = LiveMotionModule.field(settings, "o");
        snapshots = LiveMotionModule.field(base, "g");
        format = LiveMotionModule.field(base, "c");
        restart = LiveMotionModule.field(videoClass, "R");
        head = LiveMotionModule.field(window, "e");
        tail = LiveMotionModule.field(window, "f");
        snap = LiveMotionModule.field(window, "g");
        maxDuration = LiveMotionModule.field(window, "t");
        request = Class.forName("Gm.b$a", false, cl).getDeclaredConstructor(window, window,
                int.class, Object.class, Class.forName("Gm.y", false, cl), boolean.class,
                Class.forName("Gm.a", false, cl), int.class, boolean.class, long.class);
    }

    void install() {
        XposedBridge.hookMethod(record, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                p.setObjectExtra("Aurora.previousWindowScope", scope.get());
                scope.remove();
                if (!captures.owns(p.args[2]) || (Boolean) p.args[7]) return;
                Object settings = config.get(p.thisObject);
                if (!master.getBoolean(settings) || red.getBoolean(settings)
                        || (!main.getBoolean(settings) && !free.getBoolean(settings))) return;
                Object encoder = videoEncoder.get(p.thisObject);
                if (videoClass.isInstance(encoder)) scope.set(new Scope(encoder, p.args[2]));
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                Scope previous = (Scope) p.getObjectExtra("Aurora.previousWindowScope");
                if (previous == null) scope.remove(); else scope.set(previous);
            }
        });
        XposedBridge.hookMethod(baseSnapshot, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                Scope call = scope.get();
                if (call == null || call.encoder != p.thisObject || p.hasThrowable()
                        || p.getResult() == null) return;
                Object result = p.getResult();
                call.window = result;
                call.head = head.getLong(result);
                call.tail = tail.getLong(result);
                call.snap = snap.getLong(result);
            }
        });
        XposedBridge.hookMethod(videoSnapshot, new XC_MethodReplacement() {
            @Override protected Object replaceHookedMethod(MethodHookParam p) throws Throwable {
                Scope call = scope.get();
                if (call == null || call.encoder != p.thisObject || (Boolean) p.args[4]) {
                    return original(p);
                }
                // Base u() publishes to g. Keep its consumer from seeing a transient early tail.
                synchronized (snapshots.get(p.thisObject)) {
                    call.window = null;
                    long snapshotNowUs = SystemClock.elapsedRealtimeNanos() / 1000L;
                    Object result = original(p);
                    if (result == null) return null; // Native codec-not-started result.
                    if (result != call.window) {
                        throw new IllegalStateException("6.8 native video window was not captured");
                    }
                    long oldHead = head.getLong(result), oldTail = tail.getLong(result);
                    MediaFormat media = (MediaFormat) format.get(p.thisObject);
                    long preroll = media == null ? 0L
                            : (long) (media.getFloat("i-frame-interval", 0f) * 1000000f);
                    long nextHead = call.head - preroll;
                    // HAL still/ZSL timestamp can precede the actual animation by ~0.3s.
                    // Preserve native post duration, anchored in the native elapsed clock.
                    long nextTail = snapshotNowUs + (call.tail - call.snap);
                    long nextSnap = snap.getLong(result);
                    long resumed = restart.getLong(p.thisObject);
                    // Same native restart guard, applied to the retained native window.
                    if (resumed > 0 && resumed > nextHead && resumed < nextTail) {
                        nextHead = resumed + 10000L;
                        if (nextHead > nextSnap) nextSnap = nextHead;
                    }
                    head.setLong(result, nextHead);
                    tail.setLong(result, nextTail);
                    snap.setLong(result, nextSnap);
                    // The writer independently caps samplePTS-firstPTS by this field.
                    maxDuration.setLong(result, nextTail - nextHead);
                    LiveMotionModule.log("video window task=" + System.identityHashCode(call.task)
                            + " old=[" + oldHead + "," + oldTail + "] native=["
                            + call.head + "," + call.snap + "," + call.tail + "] retained=["
                            + nextHead + "," + nextSnap + "," + nextTail + "] now="
                            + snapshotNowUs + " maxDuration=" + maxDuration.getLong(result));
                    return result;
                }
            }
        });
        XposedBridge.hookMethod(request, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                Scope call = scope.get();
                if (call == null || p.args[1] != call.window || p.args[3] != call.task) return;
                // Native fixSnapshot copies only head. Complete the same window contract
                // before this request is constructed and either sample writer is submitted.
                Object audio = p.args[0], video = p.args[1];
                head.setLong(audio, head.getLong(video));
                tail.setLong(audio, tail.getLong(video));
                maxDuration.setLong(audio, tail.getLong(audio) - head.getLong(audio));
                LiveMotionModule.log("audio window task=" + System.identityHashCode(call.task)
                        + " retained=[" + head.getLong(audio) + "," + snap.getLong(audio)
                        + "," + tail.getLong(audio) + "] maxDuration="
                        + maxDuration.getLong(audio));
            }
        });
    }

    private Object original(XC_MethodHook.MethodHookParam p) throws Throwable {
        try { return XposedBridge.invokeOriginalMethod(videoSnapshot, p.thisObject, p.args); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }

    private static final class Scope {
        final Object encoder, task;
        Object window;
        long head, tail, snap;
        Scope(Object encoder, Object task) { this.encoder = encoder; this.task = task; }
    }
}
