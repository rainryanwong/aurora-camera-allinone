package com.aurora.camera.compat.nightvideo;

/** Accepted bridge from Night photo's video action to ordinary video's night option. */
public final class NightVideoBridge {
    public interface Runtime {
        boolean cameraContext(Object context);
        long uptimeMillis();
        void openVideo(Object context);
        boolean current(Object module);
        boolean alive(Object module);
        void post(Runnable task);
        int activeMode();
        boolean recording(Object module);
        Object nightOption();
        boolean empty(Object component);
        boolean blockedA0();
        boolean blockedXZ();
        boolean alreadyNight();
        Object controller();
        boolean moduleController(Object controller);
        int controllerMode(Object controller);
        void enableNight(Object controller);
        Object activity(Object module);
        void unsupported(Object activity);
    }

    private final Runtime runtime;
    private volatile long token;

    public NightVideoBridge(Runtime runtime) { this.runtime = runtime; }

    public void open(Object context) {
        if (runtime.cameraContext(context)) {
            token = runtime.uptimeMillis();
            runtime.openVideo(context);
        }
    }

    public void target(int mode) { if (mode != 162) cancel(); }
    public void cancel() { token = 0L; }
    public void failed(Object module) { if (runtime.current(module)) cancel(); }

    public void ready(final Object module) {
        if (runtime.current(module) && runtime.alive(module)) {
            final long pending = token;
            if (pending != 0L) runtime.post(new Runnable() {
                @Override public void run() { apply(module, pending); }
            });
        }
    }

    public void apply(Object module, long pending) {
        if (token != pending || !runtime.current(module)) return;
        cancel();
        if (runtime.uptimeMillis() - pending > 10000L || !runtime.alive(module)
                || runtime.activeMode() != 162 || runtime.recording(module)) return;
        Object option = runtime.nightOption();
        if (option != null && !runtime.empty(option) && !runtime.blockedA0() && !runtime.blockedXZ()) {
            if (runtime.alreadyNight()) return;
            Object controller = runtime.controller();
            if (runtime.moduleController(controller)) {
                if (runtime.controllerMode(controller) != 162) return;
                runtime.enableNight(controller);
                return;
            }
        }
        Object activity = runtime.activity(module);
        if (activity != null) runtime.unsupported(activity);
    }
}
