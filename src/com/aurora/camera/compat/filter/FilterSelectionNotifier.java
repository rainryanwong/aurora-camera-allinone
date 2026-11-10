package com.aurora.camera.compat.filter;

import android.view.View;
import java.util.BitSet;

/** Accepted R2 next-frame selection redraw batch; camera selection stays synchronous. */
public final class FilterSelectionNotifier implements Runnable {
    public interface Owner {
        View view();
        Object adapter();
        int count(Object adapter);
        void notifyItemChanged(Object adapter, int position);
    }

    private final Owner owner;
    private View view;
    private Object adapter;
    private final BitSet dirty = new BitSet();
    private boolean posted;

    public FilterSelectionNotifier(Owner owner) { this.owner = owner; }

    public void enqueue(int previous, int selected) {
        if (posted && (view != owner.view() || adapter != owner.adapter())) {
            view.removeCallbacks(this);
            dirty.clear();
            posted = false;
        }
        view = owner.view();
        adapter = owner.adapter();
        if (previous >= 0) dirty.set(previous);
        if (selected >= 0) dirty.set(selected);
        if (!posted && !dirty.isEmpty()) {
            posted = true;
            view.postOnAnimation(this);
        }
    }

    @Override public void run() {
        posted = false;
        View targetView = view;
        Object target = adapter;
        if (targetView != owner.view() || target != owner.adapter()
                || !targetView.isAttachedToWindow()) {
            dirty.clear();
            return;
        }
        BitSet batch = (BitSet) dirty.clone();
        dirty.clear();
        int count = owner.count(target);
        for (int position = batch.nextSetBit(0); position >= 0;
                position = batch.nextSetBit(position + 1)) {
            if (position < count) owner.notifyItemChanged(target, position);
        }
    }
}
