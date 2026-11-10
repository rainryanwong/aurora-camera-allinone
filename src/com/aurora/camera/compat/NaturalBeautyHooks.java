package com.aurora.camera.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Rear photo and AI capture share the native capture beauty scene schema. */
public final class NaturalBeautyHooks {
    private NaturalBeautyHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Class<?> component = host.type("v2.m0");
        Class<?> componentBase = host.type("com.android.camera.data.data.c");
        Class<?> profile = host.type("o9.b");
        final Method parser = host.method(component, "P", String.class);
        final Field currentMode = host.field(componentBase, "mCurrentMode");
        final Field frontCamera = host.field(component, "a");

        if (!componentBase.isAssignableFrom(component)
                || parser.getDeclaringClass() != component
                || parser.getReturnType() != profile
                || Modifier.isStatic(parser.getModifiers())) {
            throw new NoSuchMethodException("Beauty profile parser contract: v2.m0.P(String):o9.b");
        }
        if (currentMode.getType() != int.class
                || Modifier.isStatic(currentMode.getModifiers())) {
            throw new NoSuchFieldException("Beauty mode contract: data.data.c.mCurrentMode:int instance");
        }
        if (frontCamera.getType() != boolean.class
                || Modifier.isStatic(frontCamera.getModifiers())) {
            throw new NoSuchFieldException("Beauty facing contract: v2.m0.a:boolean instance");
        }

        return new HookPlan() {
            @Override public String id() { return "natural-beauty"; }

            @Override public void install() {
                XposedBridge.hookMethod(parser, new StrictHook() {
                    @Override protected void before(MethodHookParam param)
                            throws Throwable {
                        int mode = currentMode.getInt(param.thisObject);
                        if ((mode != 163 && mode != 168)
                                || frontCamera.getBoolean(param.thisObject)) {
                            return;
                        }
                        if ("RearCaptureOriginal".equals(param.args[0])) {
                            param.args[0] = "RearCapture";
                        } else if ("RearCaptureSecondModeOriginal".equals(param.args[0])) {
                            param.args[0] = "RearCaptureSecondMode";
                        }
                    }
                });
            }
        };
    }
}
