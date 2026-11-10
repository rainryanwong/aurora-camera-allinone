package com.aurora.camera.compat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.aurora.camera.compat.ambilight.LocalInstallFailure;
import com.aurora.camera.compat.ambilight.ModuleAssets;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** F21: exact module-owned split/config + stock Qigsaw validation and lifecycle. */
public final class AmbilightHooks {
    private static final String TAG = "AuroraAmbilight";
    private static final String Q = "com.iqiyi.android.qigsaw.core.";
    private static final String P = "com.google.android.play.core.";
    private static final String ACTIVE_EXTRA = "aurora.ambilight.previousInstall";

    private AmbilightHooks() {}

    private static final class InstallCall {
        final Object owner;
        final Object manager;
        InstallCall(Object owner, Object manager) { this.owner = owner; this.manager = manager; }
    }

    public static HookPlan resolve(final Host host) throws ReflectiveOperationException {
        final ModuleAssets assets = ModuleAssets.load(host.moduleApkPath);
        final Class<?> info = host.type(Q + "splitrequest.splitinfo.SplitInfo");
        final Class<?> managerInterface = host.type(Q + "splitrequest.splitinfo.SplitInfoManager");
        final Class<?> splitTask = host.type(P + "tasks.Task");
        final Class<?> failureListener = host.type(P + "tasks.OnFailureListener");
        final Class<?> splitException = host.type(P + "splitinstall.SplitInstallException");
        final Class<?> sessionState = host.type(P + "splitinstall.SplitInstallSessionState");
        final Class<?> installer = host.type("r6.f1");
        final Class<?> installerListener = host.type("r6.f1$a");

        final Method configStream = exact(host, Q + "splitrequest.splitinfo.SplitInfoManagerImpl",
                "createInputStreamFromAssets", InputStream.class, true, Context.class, String.class);
        final Method copyBuiltIn = exact(host, Q + "splitinstall.SplitDownloadPreprocessor",
                "copyBuiltInSplit", void.class, false, Context.class, info);
        final Field splitApk = host.field(Q + "splitinstall.SplitDownloadPreprocessor", "splitApk");
        if (splitApk.getType() != File.class) throw new NoSuchFieldException("Ambilight splitApk:File");
        final Method pathManager = exact(host, Q + "splitrequest.splitinfo.SplitPathManager",
                "require", host.type(Q + "splitrequest.splitinfo.SplitPathManager"), true);
        final Method tempDirectory = exact(host, Q + "splitrequest.splitinfo.SplitPathManager",
                "getSplitTmpDir", File.class, false);
        final Method copy = exact(host, Q + "common.FileUtil", "copyFile", void.class,
                true, InputStream.class, OutputStream.class);
        final Method delete = exact(host, Q + "common.FileUtil", "deleteFileSafely",
                boolean.class, true, File.class);
        final Method splitName = exact(host, info, "getSplitName", String.class, false);
        final Method builtIn = exact(host, info, "isBuiltIn", boolean.class, false);
        final Method url = exact(host, info, "getUrl", String.class, false);
        final Method dependencies = exact(host, info, "getDependencies", List.class, false);
        final Method md5 = exact(host, info, "getMd5", String.class, false);
        final Method size = exact(host, info, "getSize", long.class, false);
        final Method init = exact(host, installer, "init", void.class, false);
        final Method getManager = exact(host, Q + "splitrequest.splitinfo.SplitInfoManagerService",
                "getInstance", managerInterface, true);
        final Method getInfo = exact(host, managerInterface, "getSplitInfo", info, false,
                Context.class, String.class);
        final Field context = host.field(installer, "b");
        final Field installManager = host.field(installer, "e");
        final Field pending = host.field(installer, "f");
        final Field listener = host.field(installer, "l");
        if (context.getType() != Context.class || pending.getType() != ConcurrentHashMap.class
                || listener.getType() != installerListener
                || installManager.getType() != host.type(P + "splitinstall.SplitInstallManager")) {
            throw new NoSuchFieldException("Ambilight r6.f1 owner/context/task/listener contract");
        }
        final Method enter = exact(host, installer, "eh", host.type("miuix.appcompat.app.l"),
                false, String.class, Context.class, boolean.class, Runnable.class);
        final Method install = exact(host, installer, "o0", boolean.class, false, String.class);
        final Method start = exact(host, P + "splitinstall.SplitInstallManagerImpl", "startInstall",
                splitTask, false, host.type(P + "splitinstall.SplitInstallRequest"));
        final Method addFailure = exact(host, splitTask, "addOnFailureListener", splitTask,
                false, failureListener);
        final Method errorCode = exact(host, splitException, "getErrorCode", int.class, false);
        final Method createState = exact(host, sessionState, "createFrom", sessionState,
                true, Bundle.class);
        final Method stateUpdate = exact(host, installerListener, "onStateUpdate", void.class,
                false, Object.class);
        final ThreadLocal<InstallCall> active = new ThreadLocal<>();

        return new HookPlan() {
            @Override public String id() { return "ambilight-local-split"; }

            private boolean local(Object split) throws Throwable {
                if (split == null || !"ambilight".equals(call(splitName, split))) return false;
                if (!(Boolean) call(builtIn, split)
                        || !"assets://ambilight.zip".equals(call(url, split))) return false;
                List<?> required = (List<?>) call(dependencies, split);
                return required == null || required.isEmpty();
            }

            private boolean localOwner(Object owner, String name) throws Throwable {
                if (!"ambilight".equals(name)) return false;
                call(init, owner);
                Object manager = call(getManager, null);
                if (manager == null) return false;
                return local(call(getInfo, manager, context.get(owner), name));
            }

            @Override public void install() {
                XposedBridge.hookMethod(configStream, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        Context owner = (Context) param.args[0];
                        if ("com.android.camera".equals(owner.getPackageName())
                                && ModuleAssets.CONFIG_NAME.equals(param.args[1])) {
                            param.setResult(assets.config());
                        }
                    }
                });
                XposedBridge.hookMethod(copyBuiltIn, new StrictHook() {
                    @Override protected void before(MethodHookParam param)
                            throws Throwable {
                        try {
                            Context owner = (Context) param.args[0];
                            Object split = param.args[1];
                            if (!"com.android.camera".equals(owner.getPackageName()) || !local(split)) return;
                            if (!ModuleAssets.SPLIT_MD5.equals(call(md5, split))
                                    || (Long) call(size, split) != ModuleAssets.SPLIT_BYTES) {
                                throw new IOException("Ambilight local split identity mismatch");
                            }
                            copyExact((File) call(tempDirectory, call(pathManager, null)),
                                    (File) splitApk.get(param.thisObject), assets, copy, delete);
                            param.setResult(null);
                        } catch (Throwable failure) {
                            // Propagate through the original Qigsaw load/Task failure chain.
                            param.setThrowable(failure);
                        }
                    }
                });
                XposedBridge.hookMethod(install, new StrictHook() {
                    @Override protected void before(MethodHookParam param)
                            throws Throwable {
                        try {
                            if (!localOwner(param.thisObject, (String) param.args[0])) return;
                            param.setObjectExtra(ACTIVE_EXTRA, active.get());
                            InstallCall current = new InstallCall(param.thisObject, installManager.get(param.thisObject));
                            param.setObjectExtra(ACTIVE_EXTRA + ".entered", current);
                            active.set(current);
                        } catch (Throwable failure) {
                            param.setThrowable(failure);
                        }
                    }
                    @Override protected void after(MethodHookParam param) {
                        InstallCall current = active.get();
                        if (current != null && current == param.getObjectExtra(ACTIVE_EXTRA + ".entered")) {
                            InstallCall previous = (InstallCall) param.getObjectExtra(ACTIVE_EXTRA);
                            if (previous == null) active.remove(); else active.set(previous);
                        }
                    }
                });
                XposedBridge.hookMethod(start, new StrictHook() {
                    @Override protected void after(MethodHookParam param)
                            throws Throwable {
                        try {
                            InstallCall current = active.get();
                            if (current == null || current.manager != param.thisObject || param.hasThrowable()) return;
                            Object task = param.getResult();
                            Object callback = Proxy.newProxyInstance(host.loader, new Class<?>[]{failureListener},
                                    new LocalInstallFailure(current.owner, pending, listener,
                                            splitException, errorCode, createState, stateUpdate));
                            call(addFailure, task, callback);
                        } catch (Throwable failure) {
                            param.setThrowable(failure);
                        }
                    }
                });
                XposedBridge.hookMethod(enter, new StrictHook() {
                    @Override protected void before(MethodHookParam param)
                            throws Throwable {
                        try {
                            if (!localOwner(param.thisObject, (String) param.args[0])) return;
                            boolean ready = (Boolean) call(install, param.thisObject, param.args[0]);
                            if (ready && param.args[3] != null) ((Runnable) param.args[3]).run();
                            param.setResult(null);
                        } catch (Throwable failure) {
                            param.setThrowable(failure);
                        }
                    }
                });
            }
        };
    }

    /** Original 3 attempts/rename/error contract, with output FD opened before read-only. */
    private static void copyExact(File temporaryDirectory, File target, ModuleAssets assets,
            Method copier, Method delete) throws Throwable {
        File temporary = File.createTempFile("tmp-ambilight", ".apk", temporaryDirectory);
        boolean copied = false;
        int attempts = 0;
        IOException lastFailure = null;
        while (!copied && attempts < 3) {
            attempts++;
            try {
                InputStream input = assets.split();
                call(delete, null, temporary);
                FileOutputStream output = new FileOutputStream(temporary);
                temporary.setReadOnly();
                // The exact original FileUtil copier owns/closes both streams.
                call(copier, null, input, output);
                copied = temporary.renameTo(target);
                if (!copied) Log.w(TAG, "Failed to rename " + temporary + " to " + target);
            } catch (IOException failure) {
                lastFailure = failure;
                Log.w(TAG, "Failed to copy built-in split, attempt " + attempts, failure);
            }
            Log.i(TAG, "Copy built-in split " + (copied ? "succeeded" : "failed")
                    + " '" + target + "': length " + target.length());
            if (!copied) {
                call(delete, null, target);
                if (target.exists()) Log.w(TAG, "Failed to delete corrupted split " + target);
            }
        }
        call(delete, null, temporary);
        if (!copied) throw new IOException("Failed to copy built-in split " + target, lastFailure);
    }

    private static Method exact(Host host, String owner, String name, Class<?> result,
            boolean isStatic, Class<?>... args) throws ReflectiveOperationException {
        return exact(host, host.type(owner), name, result, isStatic, args);
    }

    private static Method exact(Host host, Class<?> owner, String name, Class<?> result,
            boolean isStatic, Class<?>... args) throws ReflectiveOperationException {
        Method method = host.method(owner, name, args);
        if (method.getReturnType() != result || Modifier.isStatic(method.getModifiers()) != isStatic) {
            throw new NoSuchMethodException("Ambilight contract mismatch: " + method);
        }
        return method;
    }

    private static Object call(Method method, Object receiver, Object... args) throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException wrapper) {
            throw wrapper.getCause();
        }
    }
}
