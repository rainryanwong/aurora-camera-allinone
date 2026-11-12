package com.aurora.camera.compat.editor;

import android.content.ContentProvider;
import android.os.Bundle;
import android.util.Log;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** Advertise the local Legendary editor to Gallery through its public capability protocol. */
public final class EditorLegendaryGallery {
    private static final String PROVIDER = "com.miui.mediaeditor.provider.MediaEditorProviderForGallery";
    private static final String LEGEND = "method_is_legend_available";
    private static final String CAPABILITIES = "method_is_device_support_capabilities";
    private static final String AVAILABLE = "key_common_is_available";

    private EditorLegendaryGallery() {}

    public static void install(ClassLoader loader) throws ReflectiveOperationException {
        Method getProperty = Class.forName("android.os.SystemProperties")
                .getMethod("get", String.class, String.class);
        if (!"cn".equalsIgnoreCase((String) getProperty.invoke(null, "ro.miui.build.region", "cn"))) return;

        Method call = Class.forName(PROVIDER, false, loader)
                .getDeclaredMethod("call", String.class, String.class, Bundle.class);
        XposedBridge.hookMethod(call, new StrictHook() {
            @Override protected void after(MethodHookParam param) {
                String method = (String) param.args[0];
                String key;
                if (LEGEND.equals(method)) key = AVAILABLE;
                else if (CAPABILITIES.equals(method)) key = LEGEND;
                else return;

                // The original provider must accept the caller and return the recognized key.
                if (param.getResult() instanceof Bundle
                        && "com.miui.gallery".equals(((ContentProvider) param.thisObject).getCallingPackage())) {
                    Bundle result = (Bundle) param.getResult();
                    if (result.containsKey(key) && !result.getBoolean(key)) {
                        result.putBoolean(key, true);
                        Log.i("AuroraEditor245", "Legendary available for Gallery: " + method);
                    }
                }
            }
        });
    }
}
