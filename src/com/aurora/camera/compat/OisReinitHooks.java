package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.ObservedHook;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Requests a fresh rear device open through the stock camera lifecycle. */
final class OisReinitHooks implements HookPlan {
    static final String KEY = "aurora_ois_reinit";
    private static final String TAG = "AuroraOisReinit";
    private static final String TICKET = "aurora.oisreinit.ticket";
    private static final String RELEASE_EPOCH = "aurora.oisreinit.releaseEpoch";

    private final Host host;
    private final Method attach;
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (preferences, key) -> {
                if (KEY.equals(key) || key == null) update(preferences.getBoolean(KEY, false));
            };
    private Application application;
    private Controller controller;

    OisReinitHooks(Host host) throws ReflectiveOperationException {
        this.host = host;
        attach = host.method("com.android.camera.CameraAppImpl", "attachBaseContext", Context.class);
    }

    @Override public String id() { return "ois-reinit"; }

    @Override public void install() {
        HookDiagnostics.hookMethod(attach, new StrictHook() {
            @Override protected void after(MethodHookParam p) {
                if (p.hasThrowable()) return;
                application = (Application) p.thisObject;
                SharedPreferences options = application.getSharedPreferences(
                        Options229.PREFERENCES, Context.MODE_PRIVATE);
                options.registerOnSharedPreferenceChangeListener(preferenceListener);
                update(options.getBoolean(KEY, false));
            }
        });
    }

    private void update(boolean enabled) {
        if (!enabled) {
            if (controller != null) {
                controller.stop();
                controller = null;
                event("disabled");
            }
            return;
        }
        if (controller != null) return;
        try {
            Controller next = new Controller(host, application);
            next.start();
            controller = next;
            event("enabled");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            error("cannot install OIS hooks", failure);
        }
    }

    private static void event(String message) { Log.i(TAG, message); }

    private static void error(String message, Throwable failure) { Log.e(TAG, message, failure); }

    private static final class Controller implements Application.ActivityLifecycleCallbacks {
        final ForegroundCycle cycle = new ForegroundCycle();
        final Application app;
        final Class<?> activityClass, photoClass;
        final Method open, release, metadata, restart, currentModule, activityPaused, switchingModule;
        final Method moduleAlive, moduleBusy, moduleMode, moduleCamera, moduleRecording;
        final Field moduleOwner;
        final CameraManager cameras;
        final ConcurrentHashMap<Integer, Boolean> rearById = new ConcurrentHashMap<>();
        final Handler main = new Handler(Looper.getMainLooper());
        final List<XC_MethodHook.Unhook> hooks = new ArrayList<>(2);
        final Object previewLock = new Object();
        XC_MethodHook.Unhook previewHook;
        Runnable pendingPreview;
        long previewEpoch;

        Controller(Host host, Application app) throws ReflectiveOperationException {
            this.app = app;
            activityClass = host.type("com.android.camera.Camera");
            photoClass = host.type("com.xiaomi.camera.module.PhotoBase");
            Class<?> manager = host.type("v6.i");
            Class<?> module = host.type("com.android.camera.module.u");
            Class<?> activityBase = host.type("com.android.camera.a");
            Class<?> moduleInterface = host.type("com.android.camera.module.a0");
            open = host.method(manager, "d", int.class, int.class, host.type("io.reactivex.u"), String[].class);
            release = host.method(manager, "e", host.type("E1.s2"));
            metadata = host.method(module, "onPreviewMetaDataUpdate", CaptureResult.class);
            restart = host.method(activityBase, "S8", int.class);
            currentModule = host.method(activityBase, "b2");
            activityPaused = host.method(activityBase, "isActivityPaused");
            switchingModule = host.method(activityBase, "Cj");
            moduleAlive = moduleInterface.getMethod("isDeviceAndModuleAlive");
            moduleBusy = moduleInterface.getMethod("isDoingAction");
            moduleMode = host.method(module, "getModuleIndex");
            moduleCamera = host.method(module, "getActualCameraId");
            moduleRecording = host.method(module, "isRecording");
            moduleOwner = host.field(module, "mCallback");
            cameras = app.getSystemService(CameraManager.class);
            if (cameras == null) throw new IllegalStateException("CameraManager unavailable");
        }

        void start() {
            try {
                hooks.add(HookDiagnostics.hookMethod(release, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) {
                        long epoch = cycle.activeEpoch();
                        if (epoch != 0) p.setObjectExtra(RELEASE_EPOCH, epoch);
                    }

                    @Override protected void runAfter(MethodHookParam p) {
                        Long epoch = (Long) p.getObjectExtra(RELEASE_EPOCH);
                        if (epoch == null) return;
                        if (p.hasThrowable()) {
                            cycle.releaseFailed(epoch);
                            error("native release failed", p.getThrowable());
                        } else {
                            cycle.released(p.thisObject, epoch);
                        }
                    }
                }));
                hooks.add(HookDiagnostics.hookMethod(open, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) { beforeOpen(p); }

                    @Override protected void runAfter(MethodHookParam p) {
                        ForegroundCycle.Ticket ticket = (ForegroundCycle.Ticket) p.getObjectExtra(TICKET);
                        if (ticket == null) return;
                        if (p.hasThrowable()) {
                            cycle.openFailed(ticket.epoch);
                            error("epoch=" + ticket.epoch + " native open failed", p.getThrowable());
                        } else {
                            event("epoch=" + ticket.epoch + " native_open_dispatched");
                        }
                    }
                }));
                app.registerActivityLifecycleCallbacks(this);
            } catch (RuntimeException | LinkageError failure) {
                stop();
                throw failure;
            }
        }

        void stop() {
            cycle.clear();
            stopObservingPreview(0);
            app.unregisterActivityLifecycleCallbacks(this);
            for (XC_MethodHook.Unhook hook : hooks) HookDiagnostics.unhook(hook);
            hooks.clear();
        }

        boolean isRearCamera(int cameraId) throws CameraAccessException {
            Boolean cached = rearById.get(cameraId);
            if (cached != null) return cached;
            Integer facing = cameras.getCameraCharacteristics(Integer.toString(cameraId))
                    .get(CameraCharacteristics.LENS_FACING);
            if (facing == null) throw new IllegalStateException("camera facing unavailable");
            boolean rear = facing == CameraCharacteristics.LENS_FACING_BACK;
            rearById.put(cameraId, rear);
            return rear;
        }

        void enter(Activity activity, String origin) {
            if (!activityClass.isInstance(activity)) return;
            long epoch = cycle.enter(activity, origin);
            if (epoch != 0) {
                event("epoch=" + epoch + " armed origin=" + origin);
                try {
                    observePreview(epoch);
                } catch (RuntimeException | LinkageError failure) {
                    stopObservingPreview(epoch);
                    error("epoch=" + epoch + " cannot observe preview for extra reopen", failure);
                }
            }
        }

        void leave(Activity activity) {
            if (cycle.leave(activity)) {
                stopObservingPreview(0);
                event("foreground_left; pending module work cancelled");
            }
        }

        void beforeOpen(XC_MethodHook.MethodHookParam p) {
            int cameraId = (Integer) p.args[0];
            // A negative ID is not a device open and must not consume the cycle.
            if (cameraId < 0) return;
            int mode = (Integer) p.args[1];
            long reopenEpoch = cycle.takeReopen(cameraId, mode);
            if (reopenEpoch != 0) {
                // The host queues CloseCameraCallable before OpenCameraCallable.
                // Include the target device in that close so the open cannot reuse it.
                p.args[3] = new String[0];
                event("epoch=" + reopenEpoch + " extra_reopen_force_close camera=" + cameraId
                    + " mode=" + mode);
                return;
            }
            long expectedEpoch = cycle.nativeOpenSeen();
            if (expectedEpoch == 0) return;
            // This exact host branch returns a synthetic result without opening hardware.
            if (mode == 226) {
                cycle.skip(expectedEpoch);
                stopObservingPreview(expectedEpoch);
                event("candidate_skipped: host mode=226 does not open a camera device");
                return;
            }
            boolean rear;
            try {
                rear = isRearCamera(cameraId);
            } catch (CameraAccessException | IllegalArgumentException | IllegalStateException
                     | SecurityException failure) {
                // Scope cannot be established: leave the original app operation untouched,
                // but make the missing candidate action explicit instead of faking success.
                cycle.skip(expectedEpoch);
                stopObservingPreview(expectedEpoch);
                error("scope_unavailable camera=" + cameraId + "; candidate skipped this epoch", failure);
                return;
            }
            ForegroundCycle.Ticket ticket = cycle.open(p.thisObject, rear, expectedEpoch);
            if (ticket == null) return;
            p.setObjectExtra(TICKET, ticket);
            event("epoch=" + ticket.epoch + " camera=" + cameraId + " mode=" + mode
                + " origin=" + ticket.origin + " action="
                + (ticket.releaseRequired ? "ensure_native_release" : "reuse_completed_native_release")
                + " release_scope=all_cached_devices; reinit_camera_id=" + cameraId
                + "; physical_init_set=host_defined");
            if (!ticket.releaseRequired) return;
            long start = SystemClock.elapsedRealtime();
            try {
                if (!cycle.isCurrent(ticket)) {
                    p.setObjectExtra(TICKET, null);
                    event("epoch=" + ticket.epoch + " cancelled_before_release; native loader unchanged");
                    return;
                }
                // The existing loader owns this open. Wait through its own normal release
                // primitive on its setup thread; preserve every original d(...) argument.
                // A pause after this point is handled by the native loader's cancellation;
                // do not block its UI thread or start a replacement open ourselves.
                release.invoke(p.thisObject, new Object[] {null});
                event("epoch=" + ticket.epoch + " native_release_returned elapsed_ms="
                    + (SystemClock.elapsedRealtime() - start));
            } catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause == null) cause = failure;
                p.setThrowable(cause);
                error("epoch=" + ticket.epoch + " release_failed; original open not continued", cause);
            } catch (IllegalAccessException | IllegalArgumentException failure) {
                p.setThrowable(failure);
                error("epoch=" + ticket.epoch + " release_contract_failed", failure);
            }
        }

        void observePreview(long epoch) {
            synchronized (previewLock) {
                stopObservingPreview(0);
                previewEpoch = epoch;
                previewHook = HookDiagnostics.hookMethod(metadata, new ObservedHook() {
                    @Override protected void runAfter(MethodHookParam p) {
                        if (p.hasThrowable() || !cycle.awaitingPreview(epoch)) return;
                        CaptureResult result = (CaptureResult) p.args[0];
                        if (result == null || result.get(CaptureResult.SENSOR_TIMESTAMP) == null) return;
                        Integer intent = result.get(CaptureResult.CONTROL_CAPTURE_INTENT);
                        if (intent == null || intent != CaptureResult.CONTROL_CAPTURE_INTENT_PREVIEW) return;
                        queueReopen(epoch, p.thisObject);
                    }
                });
            }
        }

        void stopObservingPreview(long epoch) {
            synchronized (previewLock) {
                if (epoch != 0 && previewEpoch != epoch) return;
                previewEpoch = 0;
                if (previewHook != null) HookDiagnostics.unhook(previewHook);
                previewHook = null;
                if (pendingPreview != null) main.removeCallbacks(pendingPreview);
                pendingPreview = null;
            }
        }

        void queueReopen(long epoch, Object module) {
            synchronized (previewLock) {
                if (previewEpoch != epoch || pendingPreview != null) return;
                pendingPreview = () -> reopenFromPreview(epoch, module);
                if (!main.post(pendingPreview)) {
                    stopObservingPreview(epoch);
                    error("epoch=" + epoch + " cannot schedule extra reopen", null);
                }
            }
        }

        void reopenFromPreview(long epoch, Object module) {
            synchronized (previewLock) {
                if (previewEpoch != epoch) return;
                pendingPreview = null;
            }
            if (!cycle.awaitingPreview(epoch)) return;
            try {
                Object owner = moduleOwner.get(module);
                if (!(owner instanceof Activity)) return;
                Activity activity = (Activity) owner;
                if (activity.isFinishing() || activity.isDestroyed()
                        || (Boolean) activityPaused.invoke(activity)
                        || (Boolean) switchingModule.invoke(activity)
                        || currentModule.invoke(activity) != module
                        || !(Boolean) moduleAlive.invoke(module)) return;
                if (!photoClass.isInstance(module) || captureIntent(activity.getIntent())) {
                    stopObservingPreview(epoch);
                    return;
                }
                if ((Boolean) moduleBusy.invoke(module) || (Boolean) moduleRecording.invoke(module)) return;
                int cameraId = (Integer) moduleCamera.invoke(module);
                if (cameraId < 0 || !isRearCamera(cameraId)) {
                    stopObservingPreview(epoch);
                    return;
                }
                int mode = (Integer) moduleMode.invoke(module);
                if (!cycle.requestReopen(epoch, activity, cameraId, mode)) return;
                stopObservingPreview(epoch);
                event("epoch=" + epoch + " extra_reopen_requested camera=" + cameraId + " mode=" + mode);
                restart.invoke(activity, mode);
            } catch (InvocationTargetException failure) {
                stopObservingPreview(epoch);
                cycle.cancelReopen(epoch);
                error("epoch=" + epoch + " extra reopen failed", failure.getCause());
            } catch (IllegalAccessException | CameraAccessException | IllegalArgumentException
                     | IllegalStateException | SecurityException failure) {
                stopObservingPreview(epoch);
                cycle.cancelReopen(epoch);
                error("epoch=" + epoch + " extra reopen contract failed", failure);
            }
        }

        static boolean captureIntent(Intent intent) {
            if (intent == null) return false;
            String action = intent.getAction();
            return MediaStore.ACTION_IMAGE_CAPTURE.equals(action)
                || MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action)
                || MediaStore.ACTION_VIDEO_CAPTURE.equals(action)
                || MediaStore.INTENT_ACTION_VIDEO_CAMERA.equals(action)
                // Stock VideoModule consumes this hidden platform extra; it is
                // not exposed as a public constant in the compile SDK.
                || intent.getBooleanExtra("android.intent.extra.quickCapture", false);
        }

        @Override public void onActivityPreCreated(Activity a, Bundle b) { enter(a, "activity_create"); }
        @Override public void onActivityPreStarted(Activity a) { enter(a, "start_return"); }
        @Override public void onActivityPreResumed(Activity a) { enter(a, "resume_return"); }
        @Override public void onActivityPostResumed(Activity a) { cycle.postResumed(a); }
        @Override public void onActivityPrePaused(Activity a) { leave(a); }
        @Override public void onActivityPreStopped(Activity a) { leave(a); }
        @Override public void onActivityPreDestroyed(Activity a) { leave(a); }
        @Override public void onActivityCreated(Activity a, Bundle b) { }
        @Override public void onActivityStarted(Activity a) { }
        @Override public void onActivityResumed(Activity a) { }
        @Override public void onActivityPaused(Activity a) { }
        @Override public void onActivityStopped(Activity a) { }
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
        @Override public void onActivityDestroyed(Activity a) { }
    }
}
