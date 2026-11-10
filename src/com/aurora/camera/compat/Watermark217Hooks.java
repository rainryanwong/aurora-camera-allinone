package com.aurora.camera.compat;

import android.os.Bundle;
import com.android.camera.port.AuroraWatermarkName;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** The existing native settings fragment is reused only in our marked internal page. */
public final class Watermark217Hooks {
    private Watermark217Hooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        final Method capture=h.method("com.android.camera.fragment.settings.CameraCapturePreferenceFragment","addWatermarkPreference");
        final Method populate=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","addCurrentPreferences");
        final Method register=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","registerPreferenceListener");
        final Method resume=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","onResume");
        final Method clicked=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","onPreferenceClick",h.type("androidx.preference.Preference"));
        final Class<?> captureType=h.type("com.android.camera.fragment.settings.CameraCapturePreferenceFragment");
        final Method back=h.method("com.android.camera.fragment.settings.a","onBackPressed");
        final Method mix=h.method("com.xiaomi.cam.watermark.a","J0",String.class,String.class,boolean.class);
        final Method serialize=h.method("com.xiaomi.cam.watermark.a","n0");
        final Method preview=h.method("com.xiaomi.cam.watermark.a","c",android.app.Application.class,android.graphics.Bitmap.class,h.type("qs.b"),int.class);
        final com.aurora.camera.compat.watermark.WatermarkVariants219 variants=new com.aurora.camera.compat.watermark.WatermarkVariants219(h);
        com.aurora.camera.features.watermark.WatermarkModule.validate(h);
        AuroraWatermarkName.hostLoader=h.loader;
        com.aurora.camera.features.watermark.WatermarkFonts217.configure(h);
        return new HookPlan(){
            public String id(){return "217.watermark-model-and-special-rendering";}
            public void install(){
                XposedBridge.hookMethod(capture,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())AuroraWatermarkName.install(p.thisObject);}});
                XposedBridge.hookMethod(populate,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(AuroraWatermarkName.own(p.thisObject)){AuroraWatermarkName.populatePage(p.thisObject);p.setResult(null);}}});
                XposedBridge.hookMethod(register,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(AuroraWatermarkName.own(p.thisObject))p.setResult(null);}});
                XposedBridge.hookMethod(resume,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(p.hasThrowable())return;if(AuroraWatermarkName.own(p.thisObject))AuroraWatermarkName.resumePage(p.thisObject);else if(captureType.isInstance(p.thisObject))AuroraWatermarkName.install(p.thisObject);}});
                XposedBridge.hookMethod(clicked,new StrictHook(){@Override protected void before(MethodHookParam p){if(captureType.isInstance(p.thisObject)&&AuroraWatermarkName.onPreferenceClick(p.thisObject,p.args[0]))p.setResult(true);}});
                XposedBridge.hookMethod(back,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(AuroraWatermarkName.back((android.app.Activity)p.thisObject))p.setResult(null);}});
                variants.install();
                StrictHook prepare=new StrictHook(){@Override protected void before(MethodHookParam p){AuroraWatermarkName.prepare(p.thisObject);}};
                XposedBridge.hookMethod(serialize,prepare);XposedBridge.hookMethod(preview,prepare);
                com.aurora.camera.features.watermark.WatermarkModule.install(h);
            }
        };
    }
}
