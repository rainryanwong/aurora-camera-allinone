package com.aurora.camera.compat;

import android.util.SparseArray;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Exact configuration changes present in the accepted 2.0.1 classes3 DEX. */
public final class MadridConfigHooks {
    static final String MADRID = "횕횙횛훘횛횟훘횒횓횀횟횕횓훘횻횗횒횄횟횒";
    static final String COMMON_FLAGSHIP = "㺶㺺㺸㻻㺸㺼㻻㺱㺰㺣㺼㺶㺰㻻㺭㺼㺴㺺㺸㺼㻻㺖㺺㺸㺸㺺㺻㺓㺹㺴㺲㺦㺽㺼㺥";

    private MadridConfigHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Class<?> madrid = host.type(MADRID);
        final Method selector = host.method("Yf.c", "a", String.class);
        requireReturn(selector, Class.class);
        if (!Modifier.isStatic(selector.getModifiers())) {
            throw new NoSuchMethodException("Yf.c.a(String) must remain static");
        }
        final String[] enabledNames = {
            "M2", "R3", "S3", "T4", "W3", "e4", "p2", "w1", "z3"
        };
        final Method[] enabled = new Method[enabledNames.length];
        for (int i = 0; i < enabledNames.length; i++) {
            enabled[i] = host.method(madrid, enabledNames[i]);
            requireReturn(enabled[i], boolean.class);
        }
        final Method highResolutionName = host.method(madrid, "o3");
        final Method mimojiFamily = host.method(madrid, "A5");
        final Method shortVideoFamily = host.method(madrid, "U0");
        final Method countdown = host.method(madrid, "Z");
        requireReturn(highResolutionName, boolean.class);
        requireReturn(mimojiFamily, int.class);
        requireReturn(shortVideoFamily, int.class);
        requireReturn(countdown, int.class);
        final Method modes = host.method(madrid, "G");
        final Method legendaryModes = host.method(madrid, "i0");
        requireReturn(modes, int[].class);
        requireReturn(legendaryModes, int[].class);
        final Method focalTable = host.method(madrid, "J0");
        final Method rearStops = host.method(madrid, "X0");
        final Method frontStops = host.method(madrid, "Y0");
        final Method zoomRange = host.method(madrid, "d1");
        final Method watermark = host.method(madrid, "d");
        for (Method method : new Method[] {
                focalTable, rearStops, frontStops, zoomRange, watermark }) {
            requireReturn(method, SparseArray.class);
        }
        final Method motionProfile = host.method(madrid, "c0");
        final Method focalCycles = host.method(madrid, "c1");
        requireReturn(motionProfile, Map.class);
        requireReturn(focalCycles, Map.class);
        final Field beanRatios = host.field("Re.a", "c");
        if (beanRatios.getType() != List.class) {
            throw new NoSuchFieldException("Re.a.c must be a List");
        }
        final Constructor<?> portraitBean = host.type("Re.b")
                .getDeclaredConstructor(int.class, float.class, float.class);
        portraitBean.setAccessible(true);
        final Field modelName = host.field("Pe.c", "h");
        if (modelName.getType() != String.class || !Modifier.isStatic(modelName.getModifiers())) {
            throw new NoSuchFieldException("Pe.c.h must be the static model name String");
        }
        final Method microfilmFamily = host.method(COMMON_FLAGSHIP, "k1");
        final Method microfilmEnabled = host.method(COMMON_FLAGSHIP, "m3");
        requireReturn(microfilmFamily, int.class);
        requireReturn(microfilmEnabled, boolean.class);

        return new HookPlan() {
            @Override public String id() { return "madrid-config-1770"; }

            @Override public void install() {
                // Accepted DEX tests Aurorapro, then Aurora with an impossible AND.
                // Thus every non-null name returns Madrid; null keeps the original NPE.
                XposedBridge.hookMethod(selector, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if (param.args[0] != null) param.setResult(madrid);
                    }
                });
                for (Method method : enabled) {
                    XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(true));
                }
                XposedBridge.hookMethod(highResolutionName, XC_MethodReplacement.returnConstant(false));
                XposedBridge.hookMethod(mimojiFamily, XC_MethodReplacement.returnConstant(4));
                XposedBridge.hookMethod(shortVideoFamily, XC_MethodReplacement.returnConstant(3));
                XposedBridge.hookMethod(countdown, XC_MethodReplacement.returnConstant(-1));
                XposedBridge.hookMethod(modes, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return new int[] { 167, 256, 175, 162, 163, 171, 173, 168, 186,
                                254, 225, 231, 232, 172, 169, 206, 191, 166 };
                    }
                });
                XposedBridge.hookMethod(legendaryModes, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return new int[] { 163, 186, 162, 225, 256 };
                    }
                });
                XposedBridge.hookMethod(focalTable, new StrictHook() {
                    @SuppressWarnings("unchecked")
                    @Override protected void after(MethodHookParam param) throws Throwable {
                        if (param.hasThrowable()) return;
                        SparseArray<Object> result = (SparseArray<Object>) param.getResult();
                        // J0 also feeds street viewfinder cropping and zoom conversion.
                        // Keep 1770's 28/35/50 contracts; use Aurora's tele references.
                        result.put(75, portraitBean.newInstance(2, 75.0f, 75.0f));
                        result.put(90, portraitBean.newInstance(3, 90.0f, 75.0f));
                        result.put(135, portraitBean.newInstance(3, 135.0f, 120.0f));
                    }
                });
                XposedBridge.hookMethod(rearStops, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return rearStops();
                    }
                });
                XposedBridge.hookMethod(frontStops, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return frontStops();
                    }
                });
                XposedBridge.hookMethod(zoomRange, new XC_MethodReplacement() {
                    @Override protected Object replaceHookedMethod(MethodHookParam param) {
                        return zoomRanges();
                    }
                });
                XposedBridge.hookMethod(motionProfile, new StrictHook() {
                    @SuppressWarnings("unchecked")
                    @Override protected void after(MethodHookParam param) throws Throwable {
                        if (param.hasThrowable()) return;
                        Map<String, Object> result = (Map<String, Object>) param.getResult();
                        applyMotionProfile(result, beanRatios);
                    }
                });
                XposedBridge.hookMethod(focalCycles, new StrictHook() {
                    @SuppressWarnings("unchecked")
                    @Override protected void after(MethodHookParam param) {
                        if (param.hasThrowable()) return;
                        applyCycles((Map<Integer, SparseArray<String[]>>) param.getResult());
                    }
                });
                XposedBridge.hookMethod(watermark, new StrictHook() {
                    @SuppressWarnings("unchecked")
                    @Override protected void after(MethodHookParam param) throws Throwable {
                        if (param.hasThrowable()) return;
                        SparseArray<String[]> result = (SparseArray<String[]>) param.getResult();
                        String prefix = result.get(0)[0];
                        String deviceName = ((String) modelName.get(null))
                                .replaceFirst("(?i)^Xiaomi\\s+", "");
                        result.put(0, new String[] { prefix, deviceName });
                    }
                });
                XposedBridge.hookMethod(microfilmFamily, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if (madrid.isInstance(param.thisObject)) param.setResult(2);
                    }
                });
                XposedBridge.hookMethod(microfilmEnabled, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if (madrid.isInstance(param.thisObject)) param.setResult(true);
                    }
                });
            }
        };
    }

    static SparseArray<Float[]> rearStops() {
        SparseArray<Float[]> result = new SparseArray<>();
        for (int mode : new int[] { 163, 168, 256, 231 }) {
            result.put(mode, new Float[] { 0.5f, 1.0f, 2.0f, 3.2f, 5.0f });
        }
        for (int mode : new int[] { 175, 173, 172, 167, 180 }) {
            result.put(mode, new Float[] { 0.5f, 1.0f, 3.2f, 5.0f });
        }
        result.put(171, new Float[] { 1.0f, 2.2f, 3.2f, 5.0f });
        return result;
    }

    static SparseArray<Float[]> frontStops() {
        SparseArray<Float[]> result = new SparseArray<>();
        for (int mode : new int[] { 163, 168, 230, 162 }) {
            result.put(mode, new Float[] { 0.5f, 1.0f });
        }
        result.put(175, new Float[] { 0.5f, 1.0f, 3.2f });
        result.put(171, new Float[] { 0.5f, 1.0f, 1.2f, 1.5f, 2.0f });
        return result;
    }

    static SparseArray<SparseArray<Float[]>> zoomRanges() {
        SparseArray<SparseArray<Float[]>> result = new SparseArray<>();
        SparseArray<Float[]> capture = new SparseArray<>();
        capture.put(0, new Float[] { 0.5f, 1.0f, 2.0f, 3.2f, 5.0f, 10.0f, 30.0f, 60.0f, 120.0f });
        SparseArray<Float[]> video = new SparseArray<>();
        video.put(0, new Float[] { 0.5f, 1.0f, 2.0f, 3.2f, 5.0f, 10.0f, 15.0f });
        SparseArray<Float[]> professional = new SparseArray<>();
        professional.put(0, new Float[] { 0.5f, 1.0f, 2.0f, 3.2f, 5.0f, 10.0f, 32.0f });
        result.put(163, capture);
        result.put(168, capture);
        result.put(162, video);
        result.put(167, professional);
        return result;
    }

    static void applyCycles(Map<Integer, SparseArray<String[]>> result) {
        SparseArray<String[]> capture = result.get(163).clone();
        capture.put(4, new String[] { "5.0", "10", "30" });
        result.put(163, capture);
        result.put(168, capture.clone());
        SparseArray<String[]> legendary = result.get(256).clone();
        legendary.put(4, new String[] { "5.0", "10", "30" });
        result.put(256, legendary);
    }

    @SuppressWarnings("unchecked")
    static void applyMotionProfile(Map<String, Object> result, Field beanRatios)
            throws IllegalAccessException {
        // c0 only changes the ultra-wide lower bound in profile 2/3.
        // A LiveMotion-owned profile already has different intervals.
        for (String type : new String[] { "2", "3" }) {
            Object bean = result.get(type);
            List<Float> ratios = (List<Float>) beanRatios.get(bean);
            if (Float.compare(ratios.get(0), 0.7f) == 0) {
                List<Float> replacement = new ArrayList<>(ratios);
                replacement.set(0, 0.5f);
                beanRatios.set(bean, replacement);
            }
        }
    }

    private static void requireReturn(Method method, Class<?> expected)
            throws NoSuchMethodException {
        if (method.getReturnType() != expected) {
            throw new NoSuchMethodException(method.toString() + " return type must be " + expected);
        }
    }
}
