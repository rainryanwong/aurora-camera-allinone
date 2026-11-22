package com.aurora.camera.compat;

import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Aligns the native director monitoring icons with the landscape alert row. */
final class DirectorLandscape implements HookPlan {
    private static final int DIRECTOR_BUTTON_SIZE = 0x7f071856; // eae, native DISP and guide box

    private final Method createLandscapeControls;
    private final Field landscapeControls;

    private DirectorLandscape(Host host) throws ReflectiveOperationException {
        createLandscapeControls = host.method("z4.E", "Jb", LinearLayout.class, int.class);
        landscapeControls = host.field("z4.n", "d");
    }

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        return new DirectorLandscape(host);
    }

    @Override public String id() {
        return "DirectorLandscape";
    }

    @Override public void install() {
        HookDiagnostics.hookMethod(createLandscapeControls, new StrictHook() {
            @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                if (param.hasThrowable() || param.getResult() == null) return;
                LinearLayout controls = (LinearLayout) landscapeControls.get(param.getResult());
                if (controls == null || controls.getChildCount() == 0) return;
                int size = controls.getResources().getDimensionPixelSize(DIRECTOR_BUTTON_SIZE);
                for (int index = 0; index < controls.getChildCount(); index++) {
                    ImageView control = (ImageView) controls.getChildAt(index);
                    LinearLayout.LayoutParams position = (LinearLayout.LayoutParams) control.getLayoutParams();
                    // The drawable sizes vary. Fixed, centered cells keep one baseline after rotation.
                    position.width = size;
                    position.height = size;
                    position.gravity = Gravity.CENTER_VERTICAL;
                    control.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    control.setLayoutParams(position);
                }
                param.setObjectExtra("director-landscape-controls", Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-landscape-controls"));
            }
        });
    }
}
