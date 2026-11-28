package com.aurora.camera.videowm;

import android.hardware.camera2.CaptureRequest;
import android.util.Size;
import android.view.Surface;
import com.aurora.camera.compat.HookPlan;
import com.aurora.camera.compat.Host;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedHook;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;

public final class VideoWatermarkHooks {
    private VideoWatermarkHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        OfficialTemplate.host = host.loader;
        Class<?> video = host.type("com.android.camera.module.VideoModule");
        Class<?> controller = host.type("com.android.camera.module.video.B");
        Method session = host.method(video, "startRecordSession", Surface.class, Surface.class,
                host.type("l9.a$g"), Size.class, int.class, boolean.class, boolean.class, host.type("l9.a$d"));
        Method start = host.method(controller, "x", int.class, host.type("com.android.camera.module.video.I"));
        Method release = host.method(controller, "s", host.type("com.android.camera.module.video.x"));
        Method close = host.method(video, "closeCamera");
        Method applyWatermark = host.method("l9.y0", "K1", CaptureRequest.Builder.class);
        Class<?> requestCache = host.type("s5.a");
        Method videoWatermarkMode = host.method("r2.m0", "m", int.class);
        Method logEnabled = host.method("com.android.camera.data.data.x", "m0", int.class);
        return new HookPlan() {
            public String id() { return "video-watermark"; }

            public void install() {
                AuroraVideoWatermarkBridge.registerDiagnostics();
                HookDiagnostics.hookMethod(videoWatermarkMode, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) {
                        if ((Integer) p.args[0] == 164) {
                            p.args[0] = 180;
                            p.setObjectExtra("directorWatermark", Boolean.TRUE);
                        }
                    }
                    @Override protected boolean countsAsRun(MethodHookParam p) {
                        return Boolean.TRUE.equals(p.getObjectExtra("directorWatermark"));
                    }
                });
                HookDiagnostics.hookMethod(start, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) {
                        int angle = (Integer) XposedHelpers.getObjectField(p.args[1], "t");
                        AuroraVideoWatermarkBridge.setOrientationHint(angle);
                    }
                    @Override protected void runAfter(MethodHookParam p) {
                        AuroraVideoWatermarkBridge.setRecording(!p.hasThrowable() && Boolean.TRUE.equals(p.getResult()));
                    }
                });
                HookDiagnostics.hookMethod(release, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) { AuroraVideoWatermarkBridge.setRecording(false); }
                });
                HookDiagnostics.hookMethod(applyWatermark, new ObservedHook() {
                    @Override protected void runAfter(MethodHookParam p) {
                        if (!p.hasThrowable()) AuroraVideoWatermarkBridge.useCameraRequest(requestCache);
                    }
                });
                HookDiagnostics.hookMethod(close, new ObservedHook() {
                    @Override protected void runAfter(MethodHookParam p) { AuroraVideoWatermarkBridge.close(); }
                });
                HookDiagnostics.hookMethod(session, new ObservedHook() {
                    @Override protected void runBefore(MethodHookParam p) throws Throwable {
                        if (!(Boolean) XposedHelpers.callMethod(OfficialTemplate.manager(), "g")) {
                            AuroraVideoWatermarkBridge.useOriginalOutput();
                            return;
                        }
                        Object record = XposedHelpers.getObjectField(p.thisObject, "mUserRecordSetting");
                        int fps = (Integer) XposedHelpers.getObjectField(record, "B");
                        int mode = (Integer) XposedHelpers.getObjectField(p.thisObject, "mModuleIndex");
                        int angle = (Integer) XposedHelpers.getObjectField(record, "t");
                        Object global = XposedHelpers.callStaticMethod(OfficialTemplate.type("g2.a"), "g");
                        boolean rear = !((Boolean) XposedHelpers.callMethod(global, "N"));
                        boolean dolby = (Boolean) XposedHelpers.callMethod(p.thisObject, "isDolbyVisionPreview");
                        if (dolby || (Boolean) logEnabled.invoke(null, mode)) {
                            AuroraVideoWatermarkBridge.useOriginalOutput();
                            return;
                        }
                        p.args[1] = AuroraVideoWatermarkBridge.prepare((Surface) p.args[1], (Size) p.args[3],
                                (Integer) p.args[4], mode, rear, fps, angle);
                    }
                });
            }
        };
    }
}
