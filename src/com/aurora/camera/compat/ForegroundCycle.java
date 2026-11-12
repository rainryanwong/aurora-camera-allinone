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
    private boolean nativeOpenObserved;
    private boolean fallbackQueued;
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
        nativeOpenObserved = false;
        fallbackQueued = false;
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
        if (!pending()) return 0;
        nativeOpenObserved = true;
        return epoch;
    }

    synchronized boolean mayNeedFallback(long expectedEpoch) {
        return active && owner.get() != null && !consumed && !nativeOpenObserved
            && !fallbackQueued && expectedEpoch == epoch;
    }

    synchronized boolean claimFallback(long expectedEpoch, Object activity) {
        if (!mayNeedFallback(expectedEpoch) || !resumed || owner.get() != activity) return false;
        fallbackQueued = true;
        return true;
    }

    synchronized boolean canRunFallback(long expectedEpoch, Object activity) {
        return active && resumed && owner.get() == activity && !consumed
            && !nativeOpenObserved && fallbackQueued && expectedEpoch == epoch;
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
