package com.aurora.camera.compat;

import android.app.Activity;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.aurora.camera.compat.legend.M3SaveCompletion261;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.WeakHashMap;

/** F26: bind a thumbnail click to the new accepted capture, retaining gallery gates. */
public final class LatestPhotoHooks {
    private static final String TAG = "AuroraLatestPhoto";
    private static final String SCOPE_KEY = "aurora.compat.latestPhoto.captureScope";
    private LatestPhotoHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> camera = host.type("com.android.camera.Camera");
        final Class<?> module = host.type("com.android.camera.module.Camera2Module");
        final Method start = host.method(module, "startNormalCapture", int.class);
        final Method click = host.method(module, "onThumbnailClicked", boolean.class);
        final Method pause = host.method(module, "onActionPause");
        final Method dispatch = host.method("l9.y0", "q1", host.type("l9.a$j"),
                host.type("l7.j"), host.type("rh.a"));
        final Method activity = host.method("com.android.camera.module.u", "getActivityOpt");
        final Method departed = host.method("com.android.camera.module.u", "isDeparted");
        final Method wait = module.getMethod("needWaitSaveFinish");
        final Method cannotOpen = host.method(module, "isCannotGotoGallery");
        final Method thumbnail = camera.getMethod("ds");
        final Field item = host.field("E1.f5", "a");
        final Field uri = host.field("E1.a5", "a");
        final Field moduleIndex = host.field("com.android.camera.module.u", "mModuleIndex");
        final Gate gate = new Gate(camera, activity, departed, wait,
                cannotOpen, thumbnail, item, uri, click);
        final M3SaveCompletion261 m3 = new M3SaveCompletion261(host, gate::saved);
        if (start.getReturnType() != boolean.class || click.getReturnType() != void.class
                || pause.getReturnType() != void.class || dispatch.getReturnType() != void.class
                || activity.getReturnType() != Optional.class || departed.getReturnType() != boolean.class
                || wait.getReturnType() != boolean.class || cannotOpen.getReturnType() != boolean.class
                || thumbnail.getReturnType() != host.type("E1.f5")
                || item.getType() != host.type("E1.a5") || uri.getType() != Uri.class
                || !Activity.class.isAssignableFrom(camera)) {
            throw new NoSuchMethodException("F26 capture/thumbnail/gallery contract");
        }
        wait.setAccessible(true);
        thumbnail.setAccessible(true);
        return new HookPlan() {
            public String id() { return "F26.latest-photo-identity"; }
            public void install() {
                m3.install();
                final ThreadLocal<Object> captureScope = new ThreadLocal<>();
                XposedBridge.hookMethod(start, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        p.setObjectExtra(SCOPE_KEY, captureScope.get());
                        captureScope.set(p.thisObject);
                    }
                    @Override protected void after(MethodHookParam p) {
                        Object previous = p.getObjectExtra(SCOPE_KEY);
                        if (previous == null) captureScope.remove(); else captureScope.set(previous);
                    }
                });
                XposedBridge.hookMethod(dispatch, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        // Only q1 reached from this very Camera2Module.startNormalCapture.
                        // Earlier capture guards and other modules' q1 calls remain untouched.
                        Object owner = captureScope.get();
                        if (owner != null && owner == p.args[0])
                            gate.capture(owner, moduleIndex.getInt(owner) == 256 && m3.selected());
                    }
                });
                XposedBridge.hookMethod(click, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        if (gate.defer(p.thisObject, (Boolean) p.args[0])) p.setResult(null);
                    }
                });
                XposedBridge.hookMethod(pause, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        gate.cancel(p.thisObject);
                    }
                });
            }
        };
    }

    static boolean sameImage(Uri a, Uri b) {
        if (a == null || b == null) return a == b;
        if (a.equals(b)) return true;
        if (!"media".equals(a.getAuthority()) || !"media".equals(b.getAuthority())) return false;
        // MediaStore may publish the same row using external/external_primary aliases.
        try {
            return a.getPath().contains("/images/media/")
                    && b.getPath().contains("/images/media/")
                    && ContentUris.parseId(a) == ContentUris.parseId(b);
        } catch (RuntimeException invalidExternalUri) {
            return false;
        }
    }

    static final class Gate {
        private static final long WINDOW = 20000;
        private static final long CLICK_TIMEOUT = 8000;
        private final Handler main = new Handler(Looper.getMainLooper());
        private final WeakHashMap<Object, State> states = new WeakHashMap<>();
        private final Class<?> cameraType;
        private final Method activity, departed, wait, cannotOpen, thumbnail, click;
        private final Field item, uri;

        Gate(Class<?> cameraType, Method activity, Method departed, Method wait,
                Method cannotOpen, Method thumbnail, Field item, Field uri, Method click) {
            this.cameraType = cameraType; this.activity = activity; this.departed = departed;
            this.wait = wait; this.cannotOpen = cannotOpen; this.thumbnail = thumbnail;
            this.item = item; this.uri = uri; this.click = click;
        }

        private Activity camera(Object owner) throws IllegalAccessException, InvocationTargetException {
            Object value = ((Optional<?>) activity.invoke(owner)).orElse(null);
            return cameraType.isInstance(value) ? (Activity) value : null;
        }
        private Uri thumbnail(Activity camera) throws IllegalAccessException, InvocationTargetException {
            Object updater = thumbnail.invoke(camera);
            Object current = updater == null ? null : item.get(updater);
            return current == null ? null : (Uri) uri.get(current);
        }
        private boolean ready(Object owner) throws IllegalAccessException, InvocationTargetException {
            return !(Boolean) wait.invoke(owner) && !(Boolean) cannotOpen.invoke(owner);
        }
        synchronized void capture(Object owner, boolean m3) throws ReflectiveOperationException {
            Activity camera = camera(owner);
            if (camera == null) return;
            cancel(owner);
            states.put(owner, new State(owner, thumbnail(camera), m3));
            Log.i(TAG, "capture accepted; waiting for " + (m3 ? "M3 save" : "new thumbnail identity"));
        }
        void saved(Uri saved) {
            main.post(() -> {
                synchronized (Gate.this) {
                    try {
                        for (java.util.Map.Entry<Object, State> entry : states.entrySet()) {
                            State state = entry.getValue();
                            Activity camera = camera(entry.getKey());
                            if (state.m3 && camera != null && !sameImage(saved, state.previous)
                                    && sameImage(saved, thumbnail(camera))) {
                                state.saved = saved;
                                Log.i(TAG, "M3 save ready " + saved);
                            }
                        }
                    } catch (InvocationTargetException failure) {
                        LatestPhotoHooks.<RuntimeException>raise(failure.getCause());
                    } catch (IllegalAccessException failure) {
                        throw new IllegalStateException("F26 resolved camera binding became inaccessible", failure);
                    }
                }
            });
        }
        synchronized void cancel(Object owner) {
            State old = states.remove(owner);
            if (old != null) main.removeCallbacks(old);
        }
        synchronized boolean defer(Object owner, boolean secondScreen)
                throws ReflectiveOperationException {
            State state = states.get(owner);
            if (state == null) return false;
            Activity camera = camera(owner);
            if (camera == null || (Boolean) departed.invoke(owner)
                    || SystemClock.uptimeMillis() - state.captureAt > WINDOW) {
                cancel(owner);
                return false;
            }
            Uri current = thumbnail(camera);
            if (state.ready(current) && ready(owner)) {
                cancel(owner);
                return false;
            }
            if (!state.pending) {
                state.pending = true;
                state.secondScreen = secondScreen;
                state.deadline = SystemClock.uptimeMillis() + CLICK_TIMEOUT;
                main.postDelayed(state, 80);
                Log.i(TAG, "thumbnail click queued; old image suppressed");
            }
            return true;
        }

        private final class State implements Runnable {
            final WeakReference<Object> owner;
            final Uri previous;
            final boolean m3;
            Uri saved;
            final long captureAt = SystemClock.uptimeMillis();
            boolean pending, secondScreen;
            long deadline;
            State(Object owner, Uri previous, boolean m3) {
                this.owner = new WeakReference<>(owner);
                this.previous = previous;
                this.m3 = m3;
            }
            boolean ready(Uri current) {
                return current != null && !sameImage(current, previous)
                        && (!m3 || sameImage(current, saved));
            }
            @Override public void run() {
                Object module = owner.get();
                if (module == null) return;
                synchronized (Gate.this) {
                    if (states.get(module) != this || !pending) return;
                    try {
                        Activity camera = camera(module);
                        if (camera == null || (Boolean) departed.invoke(module)
                                || camera.isFinishing() || !camera.hasWindowFocus()) {
                            cancel(module);
                            Log.i(TAG, "queued click cancelled: camera no longer active");
                            return;
                        }
                        Uri current = thumbnail(camera);
                        if (ready(current) && Gate.this.ready(module)) {
                            cancel(module);
                            Log.i(TAG, "new thumbnail ready; opening " + current);
                            click.invoke(module, secondScreen);
                            return;
                        }
                        if (SystemClock.uptimeMillis() >= deadline) {
                            pending = false;
                            Log.w(TAG, "queued click timed out; staying in camera");
                            Toast.makeText(camera, "照片仍在处理中，请稍后再试", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        main.postDelayed(this, 80);
                    } catch (InvocationTargetException failure) {
                        LatestPhotoHooks.<RuntimeException>raise(failure.getCause());
                    } catch (IllegalAccessException failure) {
                        cancel(module);
                        throw new IllegalStateException("F26 resolved camera binding became inaccessible", failure);
                    }
                }
            }
        }
    }

    /** Only bridges Runnable.run; the original failure and pending state survive. */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void raise(Throwable failure) throws T {
        throw (T) failure;
    }
}
