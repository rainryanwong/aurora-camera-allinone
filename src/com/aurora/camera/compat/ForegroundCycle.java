package com.aurora.camera.compat;

import java.lang.ref.WeakReference;

/** Main lifecycle and setup-thread open calls share this small synchronized gate. */
final class ForegroundCycle {
    static final class Ticket {
        final long epoch;
        final String origin;
        final boolean releaseRequired;
        Ticket(long epoch, String origin, boolean releaseRequired) {
            this.epoch = epoch;
            this.origin = origin;
            this.releaseRequired = releaseRequired;
        }
    }

    private WeakReference<Object> owner = new WeakReference<>(null);
    private long epoch;
    private boolean active;
    private boolean consumed;
    private boolean cleanRelease;
    private Object releasedManager;
    private boolean issued;
    private boolean resumed;
    private boolean reopenRequested;
    private int reopenCamera;
    private int reopenMode;
    private String origin;

    synchronized long enter(Object activity, String source) {
        if (owner.get() == activity && active) return 0;
        owner = new WeakReference<>(activity);
        epoch++;
        active = true;
        consumed = false;
        cleanRelease = false;
        releasedManager = null;
        issued = false;
        resumed = false;
        reopenRequested = false;
        reopenCamera = -1;
        origin = source;
        return epoch;
    }

    synchronized boolean leave(Object activity) {
        if (owner.get() != activity || !active) return false;
        clear();
        return true;
    }

    synchronized void clear() {
        active = false;
        resumed = false;
        cleanRelease = false;
        releasedManager = null;
    }

    private boolean pending() {
        return active && owner.get() != null && !consumed;
    }

    synchronized boolean isCurrent(Ticket ticket) {
        return active && issued && owner.get() != null && ticket.epoch == epoch;
    }

    synchronized void postResumed(Object activity) {
        if (active && owner.get() == activity) resumed = true;
    }

    synchronized long nativeOpenSeen() {
        return pending() ? epoch : 0;
    }

    synchronized boolean awaitingPreview(long expectedEpoch) {
        return active && resumed && owner.get() != null && !reopenRequested && expectedEpoch == epoch;
    }

    synchronized boolean requestReopen(long expectedEpoch, Object activity, int camera, int mode) {
        if (!awaitingPreview(expectedEpoch) || owner.get() != activity) return false;
        reopenRequested = true;
        reopenCamera = camera;
        reopenMode = mode;
        return true;
    }

    synchronized long takeReopen(int camera, int mode) {
        if (!active || !reopenRequested || reopenCamera < 0) return 0;
        boolean matches = reopenCamera == camera && reopenMode == mode;
        reopenCamera = -1;
        if (!matches) return 0;
        consumed = true;
        return epoch;
    }

    synchronized void cancelReopen(long expectedEpoch) {
        if (expectedEpoch == epoch) reopenCamera = -1;
    }

    synchronized long activeEpoch() {
        return active && owner.get() != null ? epoch : 0;
    }

    synchronized void released(Object manager, long releaseEpoch) {
        if (releaseEpoch == 0 || releaseEpoch != activeEpoch()) return;
        cleanRelease = true;
        releasedManager = manager;
    }

    synchronized void releaseFailed(long releaseEpoch) {
        if (releaseEpoch == 0 || releaseEpoch != activeEpoch()) return;
        consumed = true;
        issued = false;
        cleanRelease = false;
        releasedManager = null;
    }

    synchronized void skip(long expectedEpoch) {
        if (expectedEpoch != epoch) return;
        consumed = true;
        issued = false;
        cleanRelease = false;
        releasedManager = null;
        reopenRequested = true;
        reopenCamera = -1;
    }

    synchronized Ticket open(Object manager, boolean rear, long expectedEpoch) {
        Ticket ticket = null;
        if (active && owner.get() != null && !consumed && rear && expectedEpoch == epoch) {
            // Consume before invoking native code: synchronous reentry must not retry.
            consumed = true;
            issued = true;
            ticket = new Ticket(epoch, origin, !(cleanRelease && releasedManager == manager));
        }
        // A front open invalidates a release in its own cycle as well.
        if (expectedEpoch == epoch) {
            cleanRelease = false;
            releasedManager = null;
        }
        return ticket;
    }

    synchronized void openFailed(long expectedEpoch) {
        if (epoch == expectedEpoch) issued = false;
    }
}
