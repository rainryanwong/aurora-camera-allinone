package com.aurora.camera.livemotion;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.camera2.CaptureRequest;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class MasterAntibanding {
    private static final String KEY = "master_antibanding_50hz";
    private static final String HZ_50 = Integer.toString(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE_50HZ);

    private final ClassLoader loader;
    private final Class<?> master, listener;
    private final SharedPreferences preferences;
    private final Method type, update, register, activity, find, add, key, title, summary, check;
    private final Field group, persistent, change, order;
    private final Constructor<?> toggle;

    MasterAntibanding(ClassLoader loader, Context context, Class<?> master, Method type)
            throws ReflectiveOperationException {
        this.loader = loader;
        this.master = master;
        this.type = type;
        preferences = context.getSharedPreferences("aurora_livemotion", Context.MODE_PRIVATE);
        update = loader.loadClass("com.android.camera.module.u")
                .getDeclaredMethod("updateAntiBanding", String.class);
        Class<?> lab = loader.loadClass("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment");
        Class<?> preference = loader.loadClass("androidx.preference.Preference");
        Class<?> category = loader.loadClass("androidx.preference.PreferenceGroup");
        Class<?> checkbox = loader.loadClass("com.android.camera.preferences.AccessibleCheckBoxPreference");
        listener = loader.loadClass("androidx.preference.Preference$c");
        register = lab.getDeclaredMethod("registerPreferenceListener");
        group = LiveMotionModule.field(lab, "mPreferenceGroup");
        activity = lab.getMethod("requireActivity");
        find = category.getMethod("k0", CharSequence.class);
        add = category.getMethod("j0", preference);
        toggle = checkbox.getConstructor(loader.loadClass("androidx.fragment.app.m"));
        key = preference.getMethod("a0", String.class);
        title = preference.getMethod("e0", String.class);
        summary = preference.getMethod("c0", CharSequence.class);
        check = checkbox.getMethod("setChecked", boolean.class);
        persistent = LiveMotionModule.field(preference, "t");
        change = LiveMotionModule.field(preference, "e");
        order = LiveMotionModule.field(preference, "g");
    }

    void install() {
        XposedBridge.hookMethod(update, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                if (master.isInstance(p.thisObject) && preferences.getBoolean(KEY, false)
                        && "2".equals(type.invoke(null, 231))) {
                    p.args[0] = HZ_50;
                }
            }
        });
        XposedBridge.hookMethod(register, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) addPreference(p.thisObject);
            }
        });
    }

    private void addPreference(Object fragment) throws ReflectiveOperationException {
        Object root = group.get(fragment);
        Object row = find.invoke(root, KEY);
        if (row != null) {
            check.invoke(row, preferences.getBoolean(KEY, false));
            return;
        }
        Object parent = find.invoke(root, "category_advance_setting");
        row = toggle.newInstance(activity.invoke(fragment));
        key.invoke(row, KEY);
        title.invoke(row, "主角运镜固定 50 Hz");
        summary.invoke(row, "开启后仅主角运镜使用 50 Hz 防闪烁，关闭时跟随相机设置。部分场景可提升变焦流畅度");
        // Compat places viewfinder parameters and layout debugging at -2 and -1.
        order.setInt(row, -3);
        persistent.setBoolean(row, false);
        check.invoke(row, preferences.getBoolean(KEY, false));
        change.set(row, Proxy.newProxyInstance(loader, new Class<?>[] { listener }, (proxy, method, args) -> {
            switch (method.getName()) {
                case "onPreferenceChange":
                    preferences.edit().putBoolean(KEY, (Boolean) args[1]).apply();
                    return true;
                case "equals": return proxy == args[0];
                case "hashCode": return System.identityHashCode(proxy);
                case "toString": return KEY;
                default: throw new IllegalStateException(method.toString());
            }
        }));
        add.invoke(parent, row);
    }
}
