package com.aurora.camera.compat.ui;

import android.app.Activity;
import de.robv.android.xposed.XposedHelpers;

/** Update the explicit miuix title as well as the Activity window title. */
public final class NativePageTitle257 {
    private NativePageTitle257(){}
    public static void set(Activity activity,String title){
        activity.setTitle(title);
        Object bar=XposedHelpers.callMethod(activity,"getAppCompatActionBar");
        Object view=XposedHelpers.callMethod(bar,"m");
        XposedHelpers.callMethod(view,"setTitle",title);
    }
}
