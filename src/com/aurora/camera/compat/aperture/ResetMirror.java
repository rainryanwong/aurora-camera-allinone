package com.aurora.camera.compat.aperture;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import de.robv.android.xposed.XposedHelpers;

/** 6.8.001770.0 only. A mirror of the native reset action, scoped to AI's six-item row. */
public final class ResetMirror {
    private static final String BINDING = "aurora.compat.aperture.resetMirror";
    private ResetMirror() {}

    public static void update(View root, View reset, int mode, boolean sixItems, Rect preview) {
        if (root == null || reset == null) return;
        if (mode != 168 || !sixItems) { release(root); return; }
        Binding binding = (Binding) XposedHelpers.getAdditionalInstanceField(root, BINDING);
        if (binding == null || binding.reset != reset) {
            release(root);
            Activity activity = activity(root.getContext());
            if (activity == null) return;
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof FrameLayout)) return;
            binding = new Binding(root, reset, (FrameLayout) content);
            XposedHelpers.setAdditionalInstanceField(root, BINDING, binding);
        }
        binding.preview.set(preview == null ? new Rect() : preview);
        binding.sync();
    }

    public static void release(View root) {
        if (root == null) return;
        Binding binding = (Binding) XposedHelpers.removeAdditionalInstanceField(root, BINDING);
        if (binding != null) binding.dispose();
    }

    private static Activity activity(Context context) {
        for (int i = 0; i < 16; i++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) break;
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        return null;
    }

    private static final class Binding implements View.OnLayoutChangeListener, View.OnAttachStateChangeListener {
        final View root;
        final View reset;
        final FrameLayout host;
        final ImageButton button;
        final Rect preview = new Rect();
        final Rect viewport = new Rect();
        boolean disposed;

        Binding(View root, View reset, FrameLayout host) {
            this.root = root;
            this.reset = reset;
            this.host = host;
            button = new ImageButton(root.getContext());
            // Existing professional-panel icon. No APK resource table or image replacement.
            button.setImageResource(0x7f080761);
            button.setImageTintList(ColorStateList.valueOf(Color.WHITE));
            button.setScaleType(ImageView.ScaleType.FIT_CENTER);
            button.setBackgroundColor(Color.TRANSPARENT);
            int padding = dp(8);
            button.setPadding(padding, padding, padding, padding);
            CharSequence description = reset.getContentDescription();
            button.setContentDescription(description == null ? "重置参数" : description);
            button.setVisibility(View.GONE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    if (!disposed && root.isShown() && reset.isEnabled()) {
                        // The user's click executes the existing listener and reset dialog.
                        reset.performClick();
                        sync();
                    }
                }
            });
            host.addView(button, new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP | Gravity.LEFT));
            root.addOnLayoutChangeListener(this);
            host.addOnLayoutChangeListener(this);
            root.addOnAttachStateChangeListener(this);
        }

        int dp(int value) { return Math.round(value * root.getResources().getDisplayMetrics().density); }

        void sync() {
            if (disposed) return;
            // The native button stays in its original layout and owns reset state/animation.
            reset.setVisibility(View.GONE);
            if (!root.isShown()) { button.setVisibility(View.GONE); return; }
            int[] location = new int[2];
            host.getLocationOnScreen(location);
            int size = dp(44);
            // v19: keep the whole hit target inside the actual displayed preview.
            // F.f() supplies screen bounds; convert them to this host's coordinates.
            viewport.set(preview);
            viewport.offset(-location[0], -location[1]);
            if (!viewport.intersect(0, 0, host.getWidth(), host.getHeight())
                    || viewport.width() < size || viewport.height() < size) {
                button.setVisibility(View.GONE);
                return;
            }
            int top = Math.min(viewport.top + dp(12), viewport.bottom - size);
            int left = Math.min(viewport.left + dp(12), viewport.right - size);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) button.getLayoutParams();
            if (params.topMargin != top || params.leftMargin != left || params.width != size || params.height != size) {
                params.topMargin = top;
                params.leftMargin = left;
                params.width = params.height = size;
                button.setLayoutParams(params);
            }
            button.setRotation(reset.getRotation());
            button.setEnabled(reset.isEnabled());
            button.setAlpha(reset.isEnabled() ? 1.0f : 0.4f);
            button.setVisibility(View.VISIBLE);
        }

        void dispose() {
            if (disposed) return;
            disposed = true;
            root.removeOnLayoutChangeListener(this);
            host.removeOnLayoutChangeListener(this);
            root.removeOnAttachStateChangeListener(this);
            button.setOnClickListener(null);
            ViewGroup parent = (ViewGroup) button.getParent();
            if (parent != null) parent.removeView(button);
        }

        @Override public void onLayoutChange(View view, int l, int t, int r, int b, int ol, int ot, int or, int ob) { sync(); }
        @Override public void onViewAttachedToWindow(View view) { sync(); }
        @Override public void onViewDetachedFromWindow(View view) { release(root); }
    }
}
