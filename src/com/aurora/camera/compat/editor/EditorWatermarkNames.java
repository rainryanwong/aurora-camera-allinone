package com.aurora.camera.compat.editor;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.features.watermark.WatermarkLabels;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/** Supply display names to the native maker without changing saved photo metadata. */
public final class EditorWatermarkNames {
    private final Method configure;
    private final Constructor<?> auxiliary;
    private final Field[] fields = new Field[12];

    public EditorWatermarkNames(Host host) throws ReflectiveOperationException {
        WatermarkLabels.initialize(host.moduleApkPath);
        Class<?> data = host.type("kh.e");
        auxiliary = data.getDeclaredConstructor();
        auxiliary.setAccessible(true);
        for (int i = 0; i < fields.length; i++) fields[i] = host.field(data, String.valueOf((char) ('a' + i)));
        configure = host.method("com.miui.mediaeditor.photo.watermask.CloudWatermarkMaker", "a",
                host.type("com.xiaomi.cam.watermark.b"),
                String.class, String.class, String.class, String.class, float.class, float.class,
                String.class, String.class, String.class, String.class, String.class,
                Map.class, Map.class, String.class,
                boolean.class, boolean.class, boolean.class, boolean.class,
                boolean.class, boolean.class, boolean.class, boolean.class,
                String.class, boolean.class, String.class, boolean.class, String.class,
                boolean.class, boolean.class, String.class, String.class, String.class,
                host.type("S3.b"), data);
    }

    private Object displayData(Object source) throws ReflectiveOperationException {
        if (source == null) return null;
        String filter = (String) fields[5].get(source), lens = (String) fields[6].get(source);
        String shownFilter = WatermarkLabels.displayFilter(filter), shownLens = WatermarkLabels.lens(lens);
        if (Objects.equals(filter, shownFilter) && Objects.equals(lens, shownLens)) return source;
        Object copy = auxiliary.newInstance();
        for (Field field : fields) field.set(copy, field.get(source));
        fields[5].set(copy, shownFilter);
        fields[6].set(copy, shownLens);
        return copy;
    }

    public void install() {
        XposedBridge.hookMethod(configure, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                // The maker reads both names while building text and artwork nodes.
                // Its auxiliary argument is shared with edit/save operations, so
                // replace this call's value instead of mutating that shared object.
                p.args[34] = displayData(p.args[34]);
            }
        });
    }
}
