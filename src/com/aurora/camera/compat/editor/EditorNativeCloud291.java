package com.aurora.camera.compat.editor;

import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** Keep the native watermark engine usable inside the editor's LSPosed process. */
public final class EditorNativeCloud291 {
    private final String module;
    private final Method query, filter, unwrap;
    private boolean ready;

    public EditorNativeCloud291(Host host) throws ReflectiveOperationException {
        module = host.moduleApkPath;
        // These names are the JNI registration contract, outside R8 symbol aliases.
        Class<?> access = Class.forName("com.miui.mediaeditor.photo.watermark.jni.WatermarkAccessBridge", false, host.loader);
        Class<?> cloud = Class.forName("com.miui.mediaeditor.photo.watermark.jni.CloudWatermarkFilterBridge", false, host.loader);
        query = access.getDeclaredMethod("nativeQuery", int.class, String[].class);
        filter = cloud.getDeclaredMethod("nativeFilter", String.class, String[].class, long.class, String.class);
        unwrap = cloud.getDeclaredMethod("nativeUnwrapFiltered", String.class, long.class, String.class);
    }

    private static native void restoreEnvironment();

    private synchronized void prepare() {
        if (ready) return;
        System.load(module + "!/lib/arm64-v8a/libaurora_editor.so");
        restoreEnvironment();
        ready = true;
        Log.i("AuroraEditor245", "native watermark environment restored; original filtering retained");
    }

    public void install() {
        StrictHook beforeNative = new StrictHook() {
            @Override protected void before(MethodHookParam p) { prepare(); }
        };
        XposedBridge.hookMethod(query, beforeNative);
        XposedBridge.hookMethod(filter, beforeNative);
        XposedBridge.hookMethod(unwrap, beforeNative);
    }
}
