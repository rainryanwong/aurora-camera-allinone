package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** Route the two lab switches through the camera's original debug readers. */
public final class ViewfinderDebugHooks {
    public static final String PARAMETERS = "aurora_viewfinder_debug_parameters";
    public static final String LAYOUT = "aurora_viewfinder_debug_layout";

    private ViewfinderDebugHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Options229 options = new Options229(host);
        Method property = host.method("Kr.g", "a", String.class);
        return new HookPlan() {
            @Override public String id() { return "viewfinder-debug-options"; }

            @Override public void install() {
                XposedBridge.hookMethod(property, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        String key = (String) param.args[0];
                        if (key == null) return;
                        boolean enabled;
                        switch (key) {
                            case "camera.preview.enable.log":
                                enabled = options.enabled(PARAMETERS) || options.enabled(LAYOUT);
                                break;
                            case "camera.preview.enable.debug.btn":
                            case "camera.preview.enable.debug.fragment_debug":
                            case "camera.preview.debug.enable.fragment_debug_all":
                                enabled = options.enabled(LAYOUT);
                                break;
                            case "camera.preview.debug.debugInfo_view":
                            case "camera.preview.debug.show_afMode":
                            case "camera.preview.debug.show_afStatus":
                            case "camera.preview.debug.show_afLensPosition":
                            case "camera.preview.debug.show_distance":
                            case "camera.preview.debug.af_info":
                            case "camera.preview.debug.awb_cct":
                            case "camera.preview.debug.awb_gain":
                            case "camera.preview.debug.aec_lux":
                            case "camera.preview.debug.show_ISO":
                            case "camera.preview.debug.show_exposureTime":
                            case "camera.preview.debug.show_hdrTrigger":
                            case "camera.preview.debug.show_nightTrigger":
                                enabled = options.enabled(PARAMETERS);
                                break;
                            default:
                                return;
                        }
                        param.setResult(enabled ? "1" : "0");
                    }
                });
            }
        };
    }
}
