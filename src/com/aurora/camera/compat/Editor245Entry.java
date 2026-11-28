package com.aurora.camera.compat;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import com.aurora.camera.compat.editor.Editor245Policy;
import com.aurora.camera.compat.editor.EditorFeatureDevices293;
import com.aurora.camera.compat.editor.EditorLegendaryGallery;
import com.aurora.camera.compat.editor.EditorLeicaFilters292;
import com.aurora.camera.compat.editor.EditorNativeCloud291;
import com.aurora.camera.compat.editor.abi250.EditorAbi250;
import com.aurora.camera.diagnostics.HookDiagnostics;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.File;
import java.io.IOException;

/** Editor-only entry. Its identity/resolution failure never enters the camera path. */
public final class Editor245Entry {
    private static final String TAG = "AuroraEditor245";
    private enum State { NEW, RESOLVING, INSTALLING, INSTALLED, FAILED }
    private static State state = State.NEW;
    private static ParcelFileDescriptor moduleArchive;
    private Editor245Entry() {}

    public static synchronized void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp, String moduleApkPath) {
        if (!Editor245Policy.process(lp.packageName, lp.processName)) return;
        if (state != State.NEW) {
            Log.i(TAG, "editor callback already handled; state=" + state);
            return;
        }
        boolean mainProcess = Editor245Policy.PACKAGE.equals(lp.processName);
        if (lp.appInfo != null && lp.appInfo.dataDir != null) {
            HookDiagnostics.start(lp.appInfo.dataDir, mainProcess ? "editor" : "photo-editor", BuildInfo.VERSION);
        }
        if (!"aurora".equals(Build.DEVICE)) {
            state = State.FAILED;
            HookDiagnostics.resolutionFailed(new IllegalStateException("Unsupported editor device " + Build.DEVICE));
            Log.e(TAG, "unsupported editor device=" + Build.DEVICE + "; no editor hooks installed");
            return;
        }
        state = State.RESOLVING;
        if (mainProcess) {
            HookDiagnostics.resolving("EditorLegendaryGallery");
            try {
                boolean supported = EditorLegendaryGallery.supportedRegion();
                HookDiagnostics.resolved();
                if (supported) HookDiagnostics.tryInstall("EditorLegendaryGallery", () -> EditorLegendaryGallery.install(lp.classLoader));
                else HookDiagnostics.skipped("EditorLegendaryGallery");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                HookDiagnostics.resolutionFailed(failure);
            }
        } else HookDiagnostics.skipped("EditorLegendaryGallery");
        try {
            HookDiagnostics.resolving("EditorAbi250");
            if (lp.appInfo == null || lp.appInfo.sourceDir == null) {
                throw new IllegalStateException("editor source APK path is missing");
            }
            if (moduleApkPath == null || moduleApkPath.isEmpty()) {
                throw new IllegalStateException("Xposed module APK path is missing");
            }
            // Keep the original inode readable by delayed editor asset lookups after an APK update.
            moduleArchive = ParcelFileDescriptor.open(new File(moduleApkPath), ParcelFileDescriptor.MODE_READ_ONLY);
            EditorAbi250 symbols = EditorAbi250.resolveCached(lp.appInfo.sourceDir, moduleApkPath,
                    new File(lp.appInfo.dataDir,"code_cache"),lp.classLoader);
            if(symbols.cacheIssue!=null)Log.e(TAG,symbols.cacheIssue);
            HookDiagnostics.resolved();
            HookDiagnostics.completed("EditorAbi250");
            Host editor = new Host(lp.classLoader, "/proc/self/fd/" + moduleArchive.getFd(), symbols);
            state = State.INSTALLING;
            if (symbols.usesNativeCloudFilter()) EditorFeatureDevices293.install(editor);
            HookDiagnostics.tryInstall("Editor245Hooks", () -> {
                Editor245Hooks watermarks = new Editor245Hooks(editor);
                if (symbols.usesNativeCloudFilter()) EditorNativeCloud291.loadBridge(moduleApkPath);
                watermarks.install();
            });
            HookDiagnostics.tryInstall("EditorLeicaFilters292", () -> new EditorLeicaFilters292(editor).install());
            HookDiagnostics.tryInstall("EditorMore246Hooks", () -> new EditorMore246Hooks(editor).install());
            state = State.INSTALLED;
        } catch (IOException
                | RuntimeException | LinkageError failure) {
            State previous = state;
            state = State.FAILED;
            if (previous == State.RESOLVING) HookDiagnostics.resolutionFailed(failure);
            if (previous == State.RESOLVING && moduleArchive != null) {
                try { moduleArchive.close(); }
                catch (IOException closeFailure) { failure.addSuppressed(closeFailure); }
                moduleArchive = null;
            }
            Log.e(TAG, "Editor asset/ABI preparation failed; see individual group results", failure);
        }
    }
}
