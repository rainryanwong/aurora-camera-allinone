package com.aurora.camera.features.watermark;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.Locale;
import org.json.JSONObject;
import de.robv.android.xposed.*;

/** Phoenix metadata semantics mapped to v16. Only selected fields are changed. */
final class MetadataHooks {
    private static final String TAG="AuroraPhoenixMeta";
    static void install(ClassLoader loader) {
        try {
            Class<?> exif=Class.forName("vf.b",false,loader);
            Method set=exif.getMethod("U",String.class,String.class);
            Method get=exif.getMethod("f",String.class);
            Method image=Class.forName("l7.d",false,loader).getDeclaredMethod("d",exif);
            Method water=Class.forName("l7.d$a",false,loader).getDeclaredMethod("d");
            if(set.getReturnType()!=void.class || get.getReturnType()!=String.class || image.getReturnType()!=void.class || water.getReturnType()!=void.class)
                throw new NoSuchMethodException("metadata signatures");
            XposedBridge.hookMethod(set,new com.aurora.camera.compat.StrictHook() {
                @Override protected void before(MethodHookParam p) throws Throwable {
                    if("XiaomiAuxiliaryInfo".equals(p.args[0]) && p.args[1]!=null)
                        p.args[1]=WatermarkLabels.auxiliary((String)p.args[1]);
                }
            });
            XposedBridge.hookMethod(image,new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    if(p.hasThrowable()) return;
                    try {
                        SharedPreferences prefs=prefs(loader);
                        String name=selected(prefs);
                        if(name.isEmpty() || !prefs.getBoolean("metadata_image_enabled",true)) return;
                        String model=name.equalsIgnoreCase("leitzphone powered by xiaomi") ? "LEITZ Phone by xiaomi" :
                            name.toLowerCase(Locale.ROOT).startsWith("xiaomi ") ? name : "Xiaomi "+name;
                        set.invoke(p.args[0],"Model",model); set.invoke(p.args[0],"XiaomiProduct",model);
                        Log.i(TAG,"IMAGE_METADATA_APPLIED; Model and XiaomiProduct; final file pending");
                    } catch(Throwable e) { Log.e(TAG,"IMAGE_METADATA_FAILED",e); }
                }
            });
            XposedBridge.hookMethod(water,new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    if(p.hasThrowable()) return;
                    try {
                        SharedPreferences prefs=prefs(loader); String name=selected(prefs);
                        if(name.isEmpty() || !prefs.getBoolean("metadata_watermark_enabled",true)) return;
                        Object value=p.thisObject.getClass().getDeclaredField("b").get(p.thisObject);
                        String raw=(String)get.invoke(value,"XiaomiAuxiliaryInfo");
                        if(raw==null || raw.trim().isEmpty()) { Log.i(TAG,"WATER_METADATA_SKIP no OEM auxiliary JSON"); return; }
                        JSONObject json=new JSONObject(raw); json.put("waterName",name);
                        set.invoke(value,"XiaomiAuxiliaryInfo",json.toString());
                        Log.i(TAG,"WATER_METADATA_APPLIED; only waterName; remaining auxiliary keys retained; final file pending");
                    } catch(Throwable e) { Log.e(TAG,"WATER_METADATA_FAILED; no auxiliary replacement on parse failure",e); }
                }
            });
            Log.i(TAG,"METADATA_HOOK_READY v16; capture execution pending");
            XposedBridge.log(TAG+": METADATA_HOOK_READY");
        } catch(Throwable e) { throw new IllegalStateException("Required MetadataHooks could not be installed",e); }
    }
    private static SharedPreferences prefs(ClassLoader loader) throws Exception {
        Context c=(Context)Class.forName("ud.A",false,loader).getDeclaredField("b").get(null);
        return c.getSharedPreferences("aurora_watermark_names",0);
    }
    private static String selected(SharedPreferences prefs) {
        String n=prefs.getString("selected","").trim();
        return n.equals("工程型号") ? android.os.Build.MODEL : n;
    }
}
