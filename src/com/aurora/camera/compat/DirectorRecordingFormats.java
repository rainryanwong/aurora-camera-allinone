package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/** Director entries for the stock 10-bit recording formats. */
final class DirectorRecordingFormats {
    static final int MASTER_SHOT = 215;
    static final int DOLBY_VISION = 2850;
    static final int DOLBY_EDITOR_ICON = 0x7f080108;
    private static final int MODE = 164;

    private final Class<?> dolby;
    private final Class<?> master;
    private final Field dolbySupported;
    private final Field masterSupported;
    private final Method dataConfig;
    private final Method component;
    private final Method buildItem;
    private final Method dolbyKey;
    private final Method masterKey;
    private final Method masterMode;
    private final Constructor<?> item;
    private final Constructor<?> itemBuilder;
    private final Constructor<?> masterResource;
    private final Field builderId;
    private final Field builderResource;

    DirectorRecordingFormats(Host host) throws ReflectiveOperationException {
        dolby = host.type("s2.c");
        master = host.type("r2.i");
        dolbySupported = host.field(dolby, "f");
        masterSupported = host.field(master, "b");
        dataConfig = host.method("g2.a", "a");
        component = host.method("Xh.b", "x", Class.class);
        buildItem = host.method("X9.w5", "E", int.class);
        dolbyKey = host.method(dolby, "getKey", int.class);
        masterKey = host.method(master, "getKey", int.class);
        masterMode = host.method(master, "isSupportMode", int.class);

        Class<?> builder = host.type("b5.i$a");
        item = host.type("b5.i").getConstructor(builder);
        itemBuilder = builder.getConstructor();
        builderId = host.field(builder, "a");
        builderResource = host.field(builder, "d");
        masterResource = host.type("L4.a").getConstructor(Object.class, int.class);
    }

    void install() {
        HookDiagnostics.hookMethod(dolbyKey, new FormatHook() {
            @Override protected void after(MethodHookParam param) {
                if (!isDirector(param) || !"".equals(param.getResult())) return;
                apply(param, "pref_cinemaster_true_colour_video_key");
            }
        });
        HookDiagnostics.hookMethod(masterKey, new FormatHook() {
            @Override protected void after(MethodHookParam param) {
                if (isDirector(param)) apply(param, "pref_cinemaster_video_cclock");
            }
        });
        HookDiagnostics.hookMethod(masterMode, new FormatHook() {
            @Override protected void after(MethodHookParam param) {
                if (isDirector(param)) apply(param, true);
            }
        });
    }

    void addCards(Map<Integer, Object> cards) throws ReflectiveOperationException {
        Object config = dataConfig.invoke(null);
        Object dolbyModel = component.invoke(config, dolby);
        if (dolbySupported.getBoolean(dolbyModel)) {
            cards.put(DOLBY_VISION, buildItem.invoke(null, DOLBY_VISION));
        }
        Object masterModel = component.invoke(config, master);
        if (masterSupported.getBoolean(masterModel)) {
            Object builder = itemBuilder.newInstance();
            builderId.setInt(builder, MASTER_SHOT);
            builderResource.set(builder, masterResource.newInstance(masterModel, 3));
            cards.put(MASTER_SHOT, item.newInstance(builder));
        }
    }

    private abstract static class FormatHook extends StrictHook {
        final boolean isDirector(MethodHookParam param) {
            return !param.hasThrowable() && (int) param.args[0] == MODE;
        }

        final void apply(MethodHookParam param, Object value) {
            param.setResult(value);
            param.setObjectExtra("director-recording-format", Boolean.TRUE);
        }

        @Override protected boolean countsAsRun(MethodHookParam param) {
            return Boolean.TRUE.equals(param.getObjectExtra("director-recording-format"));
        }
    }
}
