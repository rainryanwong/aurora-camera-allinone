package com.aurora.camera.compat;

import android.os.Build;
import android.util.Log;
import com.aurora.camera.compat.editor.Editor245Policy;
import com.aurora.camera.compat.editor.EditorFeatureDevices293;
import com.aurora.camera.compat.editor.abi250.EditorAbi250;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;
import java.io.IOException;

/** Editor-only entry. Its identity/resolution failure never enters the camera path. */
public final class Editor245Entry {
    private static final String TAG = "AuroraEditor245";
    private enum State { NEW, RESOLVING, INSTALLING, INSTALLED, FAILED }
    private static State state = State.NEW;
    private Editor245Entry() {}

    public static synchronized void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp, String moduleApkPath) {
        if (!Editor245Policy.process(lp.packageName, lp.processName)) return;
        if (state != State.NEW) {
            Log.i(TAG, "editor callback already handled; state=" + state);
            return;
        }
        if (!"aurora".equals(Build.DEVICE)) {
            state = State.FAILED;
            Log.e(TAG, "unsupported editor device=" + Build.DEVICE + "; no editor hooks installed");
            return;
        }
        try {
            state = State.RESOLVING;
            if (lp.appInfo == null || lp.appInfo.sourceDir == null) {
                throw new IllegalStateException("editor source APK path is missing");
            }
            if (moduleApkPath == null || moduleApkPath.isEmpty()) {
                throw new IllegalStateException("Xposed module APK path is missing");
            }
            long started=android.os.SystemClock.elapsedRealtime();
            EditorAbi250 symbols = EditorAbi250.resolveCached(lp.appInfo.sourceDir, moduleApkPath,
                    new File(lp.appInfo.dataDir,"code_cache"),lp.classLoader);
            if(symbols.cacheIssue!=null)Log.e(TAG,symbols.cacheIssue);
            Host editor = new Host(lp.classLoader, moduleApkPath, symbols);
            EditorFeatureDevices293 features = symbols.usesNativeCloudFilter() ? new EditorFeatureDevices293(editor) : null;
            Editor245Hooks hooks = new Editor245Hooks(editor);
            EditorMore246Hooks more = new EditorMore246Hooks(editor);
            state = State.INSTALLING;
            if(features!=null)features.install();
            hooks.install();
            more.install();
            state = State.INSTALLED;
            Log.i(TAG, "editor watermark hooks installed; process=" + lp.processName
                    + "; contract=dynamic250; cache="+(symbols.cacheHit?"hit":"miss")
                    + "; initialize_ms="+(android.os.SystemClock.elapsedRealtime()-started)
                    + "; native_cloud_filter="+symbols.usesNativeCloudFilter()
                    + "; imported_lut_more_group=installed");
        } catch (IOException | ReflectiveOperationException
                | RuntimeException | LinkageError failure) {
            State previous = state;
            state = State.FAILED;
            Log.e(TAG, "editor hooks failed at " + previous + "; "
                    + (previous == State.INSTALLING ? "partial editor hooks may remain; " : "no editor hooks installed; ")
                    + "no retry in this process", failure);
        }
    }
}
