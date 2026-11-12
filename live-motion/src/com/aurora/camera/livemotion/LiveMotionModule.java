package com.aurora.camera.livemotion;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Independent 6.8 experiment. Original camera code owns capture completion and media. */
public final class LiveMotionModule implements IXposedHookLoadPackage {
    private static final String TAG = "AuroraLiveMotion";
    private static final String PKG = "com.android.camera";
    private static final String MASTER = "com.android.camera.features.mode.masterlive.MasterLiveModule";

    @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!PKG.equals(lp.packageName) || !PKG.equals(lp.processName)) return;
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        XposedBridge.hookMethod(attach, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (p.hasThrowable()) return;
                Context context = (Context) p.args[0];
                PackageInfo info = context.getPackageManager().getPackageInfo(PKG, 0);
                if (info.getLongVersionCode() != 680017700L) {
                    log("unsupported camera version=" + info.getLongVersionCode());
                    return;
                }
                install(lp.classLoader, context);
            }
        });
    }

    private static void install(ClassLoader cl, Context context) throws ReflectiveOperationException {
        Object feature = Class.forName("Pe.b", true, cl).getDeclaredMethod("D").invoke(null);
        final Object device = field(feature.getClass(), "e").get(feature);
        Class<?> entry = Class.forName(MASTER + "Entry", false, cl);
        Class<?> master = Class.forName(MASTER, false, cl);
        Class<?> bean = Class.forName("Re.a", false, cl);
        final Map<String, Object> profile = profile(bean);
        Method support = method(device.getClass(), "l3");
        Method config = method(device.getClass(), "c0");
        Method entrySupport = entry.getDeclaredMethod("support");
        Method active = master.getDeclaredMethod("onActive");
        Method inactive = master.getDeclaredMethod("onInactive");
        Method auto = master.getDeclaredMethod("startAutoZoom", float.class, float.class,
                float.class, int.class, boolean.class);
        Method reset = master.getDeclaredMethod("resetZoomRatioAfterRecording");
        Method finished = master.getDeclaredMethod("onPictureTakenFinished", boolean.class,
                long.class, int.class);
        final Field images = field(master, "mIsAllImageReceived");
        final Field zoom = field(master, "mIsCaptureZoomCompleted");
        final Field capture = field(master, "currentCaptureStatus");
        final Method type = Class.forName("com.android.camera.data.data.i", false, cl)
                .getDeclaredMethod("B", int.class);
        final Method lens = Class.forName("com.android.camera.data.data.n", false, cl)
                .getDeclaredMethod("h", int.class);
        final CaptureCompletion completionBridge = new CaptureCompletion(cl, master, type);
        final VideoWindow videoWindow = new VideoWindow(cl, completionBridge);
        final TargetZoom targetZoom = new TargetZoom(cl, master, type);
        final MasterAntibanding antibanding = new MasterAntibanding(cl, context, master, type);

        // Resolve the entire required contract before installing feature hooks.
        XposedBridge.hookMethod(support, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (p.thisObject == device) p.setResult(true);
            }
        });
        XposedBridge.hookMethod(config, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (p.thisObject == device) p.setResult(profile);
            }
        });
        XposedBridge.hookMethod(entrySupport, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(true); }
        });
        XposedBridge.hookMethod(active, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) {
                    completionBridge.activate(p.thisObject);
                    targetZoom.activate(p.thisObject);
                }
                log("active type=" + type.invoke(null, 231) + " lens=" + lens.invoke(null, 231)
                        + " error=" + p.hasThrowable());
            }
        });
        XposedBridge.hookMethod(inactive, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                completionBridge.deactivate(p.thisObject);
                targetZoom.deactivate(p.thisObject);
            }
            @Override protected void afterHookedMethod(MethodHookParam p) {
                log("inactive error=" + p.hasThrowable());
            }
        });
        XposedBridge.hookMethod(auto, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                log("zoom " + Arrays.toString(p.args));
            }
        });
        XC_MethodHook completion = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                log("completion images=" + images.getBoolean(p.thisObject)
                        + " zoom=" + zoom.getBoolean(p.thisObject)
                        + " capture=" + capture.getInt(p.thisObject)
                        + " error=" + p.hasThrowable());
            }
        };
        XposedBridge.hookMethod(reset, completion);
        XposedBridge.hookMethod(finished, completion);
        completionBridge.install();
        videoWindow.install();
        targetZoom.install();
        antibanding.install();
        log("installed v0.5.1 camera=680017700 device=" + device.getClass().getName()
                + " types=" + profile.keySet() + " lenses=wide,tele,Standalone");
    }

    private static Map<String, Object> profile(Class<?> bean) throws ReflectiveOperationException {
        // Experimental Aurora control intervals. Camera IDs and physical routing stay native.
        // b/c/e retain native indexing; d retains the bean layout (no direct reader found).
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("2", movement(bean, "2", true));
        map.put("3", movement(bean, "3", false));
        Object still = bean.getConstructor().newInstance();
        field(bean, "a").set(still, "0");
        map.put("0", still);
        return Collections.unmodifiableMap(map);
    }

    private static Object movement(Class<?> bean, String id, boolean main)
            throws ReflectiveOperationException {
        Object value = bean.getConstructor().newInstance();
        field(bean, "a").set(value, id);
        field(bean, "b").set(value, Arrays.asList("wide", "tele", "Standalone"));
        field(bean, "c").set(value, Arrays.asList(1.0f, 3.1f, 3.2f, 4.9f, 5.0f, 12.0f));
        field(bean, "d").set(value, main
                ? Arrays.asList(true, true, true, true, true, true)
                : Arrays.asList(false, true, false, true, false, true));
        field(bean, "e").set(value, Arrays.asList("1.0:3.1", "3.2:4.9", "5.0:12.0"));
        field(bean, "f").setBoolean(value, main);
        field(bean, "g").set(value, "wide");
        return value;
    }

    private static Method method(Class<?> owner, String name) throws NoSuchMethodException {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(name); }
            catch (NoSuchMethodException e) { /* 6.8 device profiles inherit Common methods. */ }
        }
        throw new NoSuchMethodException(owner.getName() + "." + name);
    }

    static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) { /* Fields on known camera superclass. */ }
        }
        throw new NoSuchFieldException(owner.getName() + "." + name);
    }

    static void log(String message) {
        Log.i(TAG, message);
    }
}
