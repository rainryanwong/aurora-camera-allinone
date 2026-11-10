package com.aurora.camera.compat;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** F10: transform the original preference value object, retaining all other settings. */
public final class PhotoQualityHooks {
    private PhotoQualityHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> preference = host.type("com.android.camera.fragment.settings.h");
        final Method factory = host.method("com.android.camera.fragment.settings.g", "b", String.class);
        final Method context = host.method("com.xiaomi.camera.basic.Global", "getContext");
        final Field key = host.field(preference, "a");
        final Field entries = host.field(preference, "b");
        final Field values = host.field(preference, "c");
        final Field descriptions = host.field(preference, "d");
        final Field defaultValue = host.field(preference, "e");
        final Field enabled = host.field(preference, "f");
        final Field title = host.field(preference, "g");
        final Field summary = host.field(preference, "h");
        final Field listener = host.field(preference, "i");
        final Constructor<?> copy = preference.getDeclaredConstructor(String.class,
                String[].class, String[].class, String[].class, String.class,
                boolean.class, int.class, String.class,
                host.type("com.android.camera.fragment.settings.i"));
        copy.setAccessible(true);
        if (factory.getReturnType() != preference || context.getReturnType() != Context.class
                || entries.getType() != String[].class || values.getType() != String[].class)
            throw new NoSuchMethodException("F10 preference factory/value contract");
        return new HookPlan() {
            public String id() { return "F10.photo-quality-super"; }
            public void install() {
                XposedBridge.hookMethod(factory, new StrictHook() {
                    @Override protected void after(MethodHookParam p) throws Throwable {
                        if (p.hasThrowable() || !"pref_camera_jpegquality_key".equals(p.args[0])) return;
                        Object original = p.getResult();
                        Context app = (Context) context.invoke(null);
                        // Version-local resource used by the accepted Smali patch for value "super".
                        String superValue = app.getString(0x7f140eab);
                        p.setResult(copy.newInstance(key.get(original),
                                prepend("超高", (String[]) entries.get(original)),
                                prepend(superValue, (String[]) values.get(original)),
                                descriptions.get(original), defaultValue.get(original),
                                enabled.getBoolean(original), title.getInt(original),
                                summary.get(original), listener.get(original)));
                    }
                });
            }
        };
    }

    static String[] prepend(String first, String[] original) {
        String[] result = new String[original.length + 1];
        result[0] = first;
        System.arraycopy(original, 0, result, 1, original.length);
        return result;
    }
}
