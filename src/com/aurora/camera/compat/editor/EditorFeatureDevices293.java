package com.aurora.camera.compat.editor;

import android.os.Build;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Extend the device lists for palette, photo-agent, image-to-video, deblur and ID photo; their other eligibility checks remain native. */
public final class EditorFeatureDevices293 {
    private EditorFeatureDevices293() {}

    public static void install(Host host) {
        HookDiagnostics.tryInstall("EditorPalette", () -> {
            Field field = host.field("bt.a", "d");
            if (field.getType() != String[].class) throw new NoSuchFieldException(field.toString());
            Set<String> devices = new LinkedHashSet<>(Arrays.asList((String[]) field.get(null)));
            devices.add(Build.DEVICE);
            field.set(null, devices.toArray(new String[0]));
        });
        HookDiagnostics.tryInstall("EditorImageToVideo", () -> extend(host, "at.a", "a"));
        HookDiagnostics.tryInstall("EditorPhotoAgent", () -> extend(host, "at.b", "a"));
        HookDiagnostics.tryInstall("EditorDeblur", () -> extend(host, "at.k", "a", "b"));
        HookDiagnostics.tryInstall("EditorIdPhoto", () -> extend(host, "at.j", "a"));
    }

    @SuppressWarnings("unchecked")
    private static void extend(Host host, String owner, String... names) throws ReflectiveOperationException {
        Field[] fields = new Field[names.length];
        java.util.List<Set<String>> previous = new java.util.ArrayList<>();
        java.util.List<Set<String>> updated = new java.util.ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Field field = fields[i] = host.field(owner, names[i]);
            if (field.getType() != Set.class) throw new NoSuchFieldException(field.toString());
            Set<String> original = (Set<String>) field.get(null);
            previous.add(original);
            Set<String> copy = new LinkedHashSet<>(original);
            copy.add(Build.DEVICE);
            updated.add(copy);
        }
        int written = 0;
        try {
            for (; written < fields.length; written++) fields[written].set(null, updated.get(written));
        } catch (IllegalAccessException | RuntimeException failure) {
            for (int i = written - 1; i >= 0; i--) fields[i].set(null, previous.get(i));
            throw failure;
        }
    }
}
