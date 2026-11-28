package com.aurora.camera.videowm;

import android.graphics.Bitmap;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import com.aurora.camera.diagnostics.HookDiagnostics;
import de.robv.android.xposed.XposedHelpers;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Keep the camera's YUV samples and blend the native template into its recording output. */
final class AuroraVideoWatermarkBridge implements ImageReader.OnImageAvailableListener {
    private static final String TAG = "AuroraVideoWm";
    private static final int WIDTH = 1920, HEIGHT = 1080;
    private static AuroraVideoWatermarkBridge active;

    private static HookDiagnostics.Operation originalOutput, markedOutput;

    static void registerDiagnostics() {
        originalOutput = HookDiagnostics.operation("关闭水印或当前规格使用相机原输出");
        markedOutput = HookDiagnostics.operation("录像画面写入视频水印");
        originalOutput.registered();
        markedOutput.registered();
    }

    private final Surface recorder;
    private final HandlerThread thread = new HandlerThread("VideoWatermarkYuv");
    private Handler handler;
    private ImageReader reader;
    private ImageWriter writer;
    private volatile boolean closing;
    private volatile Recording recording;
    private int orientationHint;

    private static native ByteBuffer makeStamp(Bitmap bitmap);
    private static native int stampBuffer(HardwareBuffer buffer, ByteBuffer stamp);

    private static final class Recording {
        final int angle;
        boolean claimed;
        boolean bufferEditFailed;
        boolean outputFailed;
        volatile ByteBuffer stamp;
        boolean reportedStamp;
        Recording(int angle) { this.angle = angle; }
    }

    private AuroraVideoWatermarkBridge(Surface recorder, int angle) {
        this.recorder = recorder;
        orientationHint = angle;
    }

    static synchronized Surface prepare(Surface recorder, Size size, int operatingMode, int moduleIndex,
            boolean rear, int fps, int angle) {
        boolean videoMode = moduleIndex == 162 || moduleIndex == 164 || moduleIndex == 180;
        // Normal SDR with EIS, 60 fps without EIS, or the regular FOV-corrected session.
        boolean sdrSession = operatingMode == 0x8004 || operatingMode == 0x803c || operatingMode == 0xf010;
        if (recorder == null || size == null || !videoMode || !sdrSession || !rear
                || size.getWidth() != WIDTH || size.getHeight() != HEIGHT || fps <= 0 || fps >= 120) {
            useOriginalOutput();
            return recorder;
        }
        if (active != null && active.recorder == recorder) return active.reader.getSurface();
        close();
        AuroraVideoWatermarkBridge bridge = new AuroraVideoWatermarkBridge(recorder, angle);
        try {
            System.loadLibrary("videowmyuv");
            bridge.thread.start();
            bridge.handler = new Handler(bridge.thread.getLooper());
            bridge.reader = new ImageReader.Builder(WIDTH, HEIGHT)
                    .setDefaultHardwareBufferFormat(HardwareBuffer.YCBCR_420_888)
                    .setDefaultDataSpace(DataSpace.DATASPACE_BT709)
                    .setUsage(HardwareBuffer.USAGE_CPU_READ_OFTEN | HardwareBuffer.USAGE_CPU_WRITE_OFTEN | HardwareBuffer.USAGE_VIDEO_ENCODE)
                    .setMaxImages(3).build();
            bridge.writer = new ImageWriter.Builder(recorder).setWidthAndHeight(WIDTH, HEIGHT)
                    .setHardwareBufferFormat(HardwareBuffer.YCBCR_420_888).setDataSpace(DataSpace.DATASPACE_BT709)
                    .setUsage(HardwareBuffer.USAGE_CPU_WRITE_OFTEN | HardwareBuffer.USAGE_VIDEO_ENCODE).build();
            bridge.reader.setOnImageAvailableListener(bridge, bridge.handler);
            active = bridge;
            return bridge.reader.getSurface();
        } catch (RuntimeException | LinkageError e) {
            bridge.release();
            markedOutput.failed(e);
            Log.e(TAG, "Cannot open YUV recording surfaces", e);
            return recorder;
        }
    }

    static synchronized void setRecording(boolean value) {
        if (active == null) return;
        active.recording = value ? new Recording(active.orientationHint) : null;
    }

    static synchronized void useOriginalOutput() {
        close();
        originalOutput.ran();
    }

    static synchronized void setOrientationHint(int value) {
        if (active != null) active.orientationHint = (value % 360 + 360) % 360;
    }

    // The camera caches its ordinary video request separately from video snapshots.
    // At this point it has already written this clip's time into the current item.
    static void useCameraRequest(Class<?> requestCache) {
        final AuroraVideoWatermarkBridge bridge;
        final Recording clip;
        final Object tag;
        synchronized (AuroraVideoWatermarkBridge.class) {
            bridge = active;
            if (bridge == null || bridge.closing) return;
            clip = bridge.recording;
            if (clip == null || clip.claimed) return;
            Object cache = XposedHelpers.callStaticMethod(requestCache, "a");
            tag = XposedHelpers.getObjectField(cache, "a");
            if (tag == null || (((Integer) XposedHelpers.callMethod(tag, "getType")) & 0x40000) == 0) return;
            clip.claimed = true;
        }
        try {
            Object manager = XposedHelpers.getObjectField(tag, "q");
            Object item = XposedHelpers.callMethod(manager, "a");
            Bitmap layer = OfficialTemplate.render(item, clip.angle);
            try {
                clip.stamp = makeStamp(layer);
                if (clip.stamp == null) throw new IllegalStateException("Invalid official watermark bitmap");
            } finally {
                layer.recycle();
            }
        } catch (RuntimeException error) {
            markedOutput.failed(error);
            Log.e(TAG, "Unable to render native video request", error);
        }
    }


    @Override public void onImageAvailable(ImageReader source) {
        if (closing) return;
        try (Image input = source.acquireNextImage()) {
            Recording clip = recording;
            if (input == null || clip == null) return;
            ByteBuffer stamp = clip.stamp;
            if (!clip.bufferEditFailed) {
                try (SyncFence ready = input.getFence(); HardwareBuffer buffer = input.getHardwareBuffer()) {
                    if (ready.isValid() && !ready.awaitForever()) throw new IllegalStateException("Camera fence");
                    int status = stampBuffer(buffer, stamp);
                    if (status != 0) throw new IllegalStateException("Writable camera YUV buffer: " + status);
                } catch (RuntimeException | IOException error) {
                    clip.bufferEditFailed = true;
                    markedOutput.failed(error);
                    Log.e(TAG, "Cannot edit this recording; forwarding camera buffers", error);
                }
            }
            writer.queueInputImage(input);
            if (!clip.reportedStamp && stamp != null && !clip.bufferEditFailed) {
                clip.reportedStamp = true;
                markedOutput.ran();
            }
        } catch (RuntimeException error) {
            Recording clip = recording;
            if (clip != null && !clip.outputFailed) {
                clip.outputFailed = true;
                markedOutput.failed(error);
                Log.e(TAG, "Could not submit camera frame", error);
            }
        }
    }

    static synchronized void close() {
        if (active == null) return;
        AuroraVideoWatermarkBridge bridge = active;
        active = null;
        bridge.closing = true;
        bridge.recording = null;
        CountDownLatch released = new CountDownLatch(1);
        bridge.handler.post(() -> {
            try { bridge.release(); } finally { released.countDown(); }
        });
        try {
            if (!released.await(2, TimeUnit.SECONDS)) Log.w(TAG, "YUV output still closing");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void release() {
        if (reader != null) reader.close();
        if (writer != null) writer.close();
        if (thread.isAlive()) thread.quitSafely();
    }
}
