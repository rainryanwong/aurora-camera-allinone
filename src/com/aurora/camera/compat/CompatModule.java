package com.aurora.camera.compat;

import android.os.Build;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/** Feature hooks for the verified stock Aurora 1770 APK. */
public final class CompatModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
    private static final String TAG = "AuroraCameraCompat";
    private static final String PACKAGE = "com.android.camera";
    private static final String STOCK_SHA256 =
            "e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07";

    private enum State { NEW, RESOLVING, INSTALLING, INSTALLED, FAILED }
    private State state = State.NEW;
    private String moduleApkPath;

    @Override public void initZygote(StartupParam startupParam) {
        moduleApkPath = startupParam.modulePath;
    }

    @Override public synchronized void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lp) {
        if ("com.miui.mediaeditor".equals(lp.packageName)) {
            Editor245Entry.handleLoadPackage(lp, moduleApkPath);
            return;
        }
        if (!PACKAGE.equals(lp.packageName) || !PACKAGE.equals(lp.processName)) return;
        if (state != State.NEW) {
            Log.i(TAG, "load callback already handled; state=" + state);
            return;
        }
        if (!"aurora".equals(Build.DEVICE)) {
            state = State.FAILED;
            Log.e(TAG, "unsupported device=" + Build.DEVICE + "; no hooks installed");
            return;
        }

        List<HookPlan> plans;
        try {
            state = State.RESOLVING;
            if (lp.appInfo == null || lp.appInfo.sourceDir == null) {
                throw new IllegalStateException("camera source APK path is missing");
            }
            String actualSha = HostApkIdentity.verify(lp.appInfo.sourceDir, lp.appInfo.dataDir, STOCK_SHA256);
            if (!STOCK_SHA256.equals(actualSha)) {
                state = State.FAILED;
                Log.e(TAG, "unsupported source APK sha256=" + actualSha
                        + "; expected stock 1770; no hooks installed");
                return;
            }
            if (moduleApkPath == null) {
                throw new IllegalStateException("Xposed module APK path is missing");
            }
            Host host = new Host(lp.classLoader, moduleApkPath);
            plans = FeatureRegistry.resolve(host);
        } catch (IOException | NoSuchAlgorithmException | ReflectiveOperationException
                | RuntimeException | LinkageError failure) {
            state = State.FAILED;
            Log.e(TAG, "host/feature resolution failed; no feature hooks installed", failure);
            return;
        }

        state = State.INSTALLING;
        int installedGroups = 0;
        for (HookPlan plan : plans) {
            String group = plan.id();
            try {
                plan.install();
                installedGroups++;
                Log.i(TAG, "group installed id=" + group);
            } catch (RuntimeException | LinkageError failure) {
                state = State.FAILED;
                Log.e(TAG, "group install failed id=" + group
                        + " completedGroups=" + installedGroups
                        + "; partial hooks may remain; no retry in this process", failure);
                return;
            }
        }
        state = State.INSTALLED;
        Log.i(TAG, "Compat 0.1.110 installed groups=" + installedGroups
                + " stock_sha256=" + STOCK_SHA256);
    }

}
