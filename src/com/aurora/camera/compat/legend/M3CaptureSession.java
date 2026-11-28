package com.aurora.camera.compat.legend;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.diagnostics.HookDiagnostics;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/** Let the stock open manager replace a realtime device previously used for Photo. */
public final class M3CaptureSession {
    private static final String PHOTO_CAPTURED = "aurora.compat.m3.photoCaptured";

    private final Method open;
    private final Method cameraDevices;
    private final Field cameraEntries;
    private final Field cachedDevice;

    public M3CaptureSession(Host host) throws ReflectiveOperationException {
        open = host.method("v6.i", "d", int.class, int.class, host.type("io.reactivex.u"), String[].class);
        cameraDevices = host.method("ii.e", "e");
        cameraEntries = host.field("ii.a$b", "b");
        cachedDevice = host.field("ii.a$a", "g");
    }

    public void markPhotoCapture(Object device) {
        XposedHelpers.setAdditionalInstanceField(device, PHOTO_CAPTURED, Boolean.TRUE);
    }

    public void install(M3SaveCompletion261 m3) {
        HookDiagnostics.Operation reopen = HookDiagnostics.operation("拍照后切入 M3 重新打开实时相机");
        if (HookDiagnostics.hookMethod(open, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                String[] exclusions = (String[]) p.args[3];
                if ((Integer) p.args[1] != 256 || exclusions == null || exclusions.length == 0) return;

                String target = String.valueOf(p.args[0]);
                if (!Arrays.asList(exclusions).contains(target)) return;

                Map<?, ?> entries = (Map<?, ?>) cameraEntries.get(cameraDevices.invoke(null));
                Object entry = entries.get(target);
                if (entry == null) return;
                Object device = cachedDevice.get(entry);
                if (device == null || XposedHelpers.getAdditionalInstanceField(device, PHOTO_CAPTURED) == null
                        || !m3.selected()) return;

                // The host queues the close before the open and coordinates the device callbacks.
                // Let it close this device while preserving the other exclusions.
                p.args[3] = Arrays.stream(exclusions).filter(id -> !target.equals(id)).toArray(String[]::new);
                reopen.ran();
            }
        }) != null) reopen.registered();
    }
}
