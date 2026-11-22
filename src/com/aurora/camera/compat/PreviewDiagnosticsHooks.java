package com.aurora.camera.compat;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.RggbChannelVector;
import android.os.SystemClock;
import android.util.Log;
import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Observe completed preview results without modifying requests or holding image buffers. */
final class PreviewDiagnosticsHooks implements HookPlan {
    private static final String PROPERTY = "debug.aurora.preview_session";
    private static final String FILE_SESSION = "file:";
    private static final String TAG = "AuroraPreviewFrame";
    private static final String RECORDED = "aurora.preview.recorded";
    private final Method completed;
    private final Method property;
    private final File frameFile;
    private FileOutputStream frames;
    private String fileSession;
    private String fileToken;
    private long fileDeadlineNanos;

    private PreviewDiagnosticsHooks(Method completed, Method property, File frameFile) {
        this.completed = completed;
        this.property = property;
        this.frameFile = frameFile;
    }

    static PreviewDiagnosticsHooks resolve(Host host, String cameraDataDir) throws ReflectiveOperationException {
        Method completed = host.method("l9.y0$k", "onCaptureCompleted", CameraCaptureSession.class,
                CaptureRequest.class, TotalCaptureResult.class);
        Method property = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
        return new PreviewDiagnosticsHooks(completed, property, new File(cameraDataDir, "cache/aurora-preview.frames"));
    }

    @Override public String id() { return "preview-diagnostics"; }

    @Override public void install() {
        HookDiagnostics.hookMethod(completed, new ObservedHook() {
            @Override protected synchronized void runAfter(MethodHookParam param) throws Exception {
                if (param.hasThrowable()) return;
                String session = (String) property.invoke(null, PROPERTY);
                if (frames != null && !session.equals(fileSession)) closeFrames();
                if (session.isEmpty()) return;
                CaptureRequest request = (CaptureRequest) param.args[1];
                if (!Integer.valueOf(CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW)
                        .equals(request.get(CaptureRequest.CONTROL_CAPTURE_INTENT))) return;
                long now = SystemClock.elapsedRealtimeNanos();
                boolean fileCapture = session.startsWith(FILE_SESSION);
                if (fileCapture) {
                    if (!session.equals(fileSession)) {
                        int separator = session.lastIndexOf(':');
                        fileToken = session.substring(FILE_SESSION.length(), separator);
                        fileDeadlineNanos = Long.parseLong(session.substring(separator + 1)) * 1_000_000_000L;
                        fileSession = session;
                    }
                    if (now >= fileDeadlineNanos) {
                        if (frames != null) closeFrames();
                        return;
                    }
                }
                CameraCaptureSession camera = (CameraCaptureSession) param.args[0];
                CaptureResult result = (CaptureResult) param.args[2];
                String row = now + " " + frame(camera, result);
                if (fileCapture) {
                    if (frames == null) frames = new FileOutputStream(frameFile, true);
                    frames.write((fileToken + " " + row + "\n").getBytes(StandardCharsets.UTF_8));
                } else {
                    // Earlier All in One versions collect the plain-token stream from logcat.
                    Log.i(TAG, session + " " + row);
                }
                param.setObjectExtra(RECORDED, Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra(RECORDED));
            }
        });
    }

    private void closeFrames() throws IOException {
        FileOutputStream stream = frames;
        frames = null;
        stream.close();
    }

    private static JSONObject frame(CameraCaptureSession camera, CaptureResult result)
            throws org.json.JSONException {
        JSONObject row = new JSONObject();
        row.put("pid", android.os.Process.myPid());
        row.put("camera_session", System.identityHashCode(camera));
        row.put("camera", camera.getDevice().getId());
        row.put("frame", result.getFrameNumber());
        row.put("sensor_ns", result.get(CaptureResult.SENSOR_TIMESTAMP));
        row.put("physical", result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID));
        row.put("zoom", result.get(CaptureResult.CONTROL_ZOOM_RATIO));
        row.put("focal_mm", result.get(CaptureResult.LENS_FOCAL_LENGTH));
        row.put("focus_diopters", result.get(CaptureResult.LENS_FOCUS_DISTANCE));
        row.put("exposure_ns", result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
        row.put("iso", result.get(CaptureResult.SENSOR_SENSITIVITY));
        row.put("post_raw_boost", result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST));
        row.put("ae", result.get(CaptureResult.CONTROL_AE_STATE));
        row.put("awb", result.get(CaptureResult.CONTROL_AWB_STATE));
        row.put("af", result.get(CaptureResult.CONTROL_AF_STATE));
        row.put("scene_mode", result.get(CaptureResult.CONTROL_SCENE_MODE));
        RggbChannelVector gains = result.get(CaptureResult.COLOR_CORRECTION_GAINS);
        if (gains != null) {
            row.put("awb_gains", new JSONArray().put(gains.getRed()).put(gains.getGreenEven())
                    .put(gains.getGreenOdd()).put(gains.getBlue()));
        }
        ColorSpaceTransform transform = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM);
        if (transform != null) {
            JSONArray matrix = new JSONArray();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) matrix.put(transform.getElement(c, r).toString());
            }
            row.put("ccm", matrix);
        }
        return row;
    }
}
