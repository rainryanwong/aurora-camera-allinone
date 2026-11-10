package com.aurora.camera.compat.ui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/** One opaque page owns its complete window, including the gesture-bar area. */
public final class FullScreenPage220 {
    private FullScreenPage220(){}
    public static void apply(Window window,View content,int background,boolean dark){
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND
                |WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                |WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.setDecorFitsSystemWindows(false);
        window.setBackgroundDrawable(new ColorDrawable(background));
        window.getDecorView().setBackgroundColor(background);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setNavigationBarContrastEnforced(false);
        WindowInsetsController bars=window.getInsetsController();
        if(bars!=null){
            int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    |WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            bars.setSystemBarsAppearance(dark?0:mask,mask);
        }
        window.setLayout(-1,-1);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                |WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        content.requestApplyInsets();
    }
    public static int bottomPadding(int systemBars,int ime){return Math.max(systemBars,ime);}
}
