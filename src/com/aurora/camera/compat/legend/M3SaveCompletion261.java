package com.aurora.camera.compat.legend;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.diagnostics.ObservedHook;
import de.robv.android.xposed.XposedHelpers;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiPredicate;

/** Prepares the native quick thumbnail and observes completion of the same capture. */
public final class M3SaveCompletion261 {
    public interface Listener { void saved(Uri uri); }
    private static final String TAG = "AuroraM3Save";
    private static final String PREVIOUS = "aurora.m3.save.previous";
    private static final String CAPTURE = "aurora.m3.thumbnail.capture";
    private static final String EARLY_PREVIOUS = "aurora.m3.thumbnail.previous";
    private static final String PUBLISHED = "aurora.m3.thumbnail.published";
    private static final String RAN = "aurora.m3.thumbnail.ran";
    private static final ColorMatrixColorFilter MONO = new ColorMatrixColorFilter(new float[]{
            .299f, .587f, .114f, 0, 0,
            .299f, .587f, .114f, 0, 0,
            .299f, .587f, .114f, 0, 0,
            0, 0, 0, 1, 0
    });
    private final ThreadLocal<Save> current = new ThreadLocal<>();
    private final ThreadLocal<Capture> early = new ThreadLocal<>();
    private final Method[] storage;
    private final Method update, insert, repository, component, value, context, makeThumbnail,
            publishThumbnail, thumbnailSize;
    private final Field taskMetadata, taskSave, taskImage, mode, name, callback, orientation,
            writerTask, updateWriter, insertWriter;
    private final Method captureStart, previewStorage, postThumbnail;
    private final Constructor<?> copyTask;
    private final Field thumbnailBitmap, thumbnailUri;
    private final Class<?> legendary;
    private final Listener listener;
    private final BiPredicate<Uri, Uri> sameImage;

    public M3SaveCompletion261(Host h, Listener listener, BiPredicate<Uri, Uri> sameImage)
            throws ReflectiveOperationException {
        this.listener = listener;
        this.sameImage = sameImage;
        Class<?> task = h.type("Sh.s"), thumbnail = h.type("E1.a5");
        storage = new Method[]{h.method("t7.d", "a", task), h.method("t7.e", "a", task)};
        update = h.method("l7.J", "invoke");
        insert = h.method("l7.H", "invoke");
        updateWriter = h.field("l7.J", "b");
        insertWriter = h.field("l7.H", "f");
        writerTask = h.field("l1.a", "b");
        taskMetadata = h.field(task, "b");
        taskSave = h.field(task, "k");
        taskImage = h.field(task, "a");
        mode = h.field("Sh.a", "g");
        name = h.field("Sh.C", "b");
        callback = h.field("Sh.C", "l");
        orientation = h.field("Sh.B", "c");
        repository = h.method("g2.a", "a");
        component = h.method("Xh.b", "x", Class.class);
        legendary = h.type("r2.B");
        value = h.method("com.android.camera.data.data.c", "getComponentValue", int.class);
        context = h.method("com.xiaomi.camera.basic.Global", "getContext");
        makeThumbnail = h.method(thumbnail, "d", Uri.class, Bitmap.class, int.class, boolean.class);
        publishThumbnail = h.method("l7.C", "e", thumbnail, boolean.class);
        thumbnailSize = h.method(thumbnail, "s", long.class);
        captureStart = h.method("com.android.camera.module.Camera2Module", "onCaptureStart",
                task, h.type("l9.r0"));
        copyTask = task.getDeclaredConstructor(task);
        previewStorage = h.method("t7.h", "h", task);
        postThumbnail = h.method("b7.a", "e", thumbnail, boolean.class);
        thumbnailBitmap = h.field(thumbnail, "b");
        thumbnailUri = h.field(thumbnail, "a");
    }

    public boolean selected() throws ReflectiveOperationException {
        return "M3".equals(value.invoke(component.invoke(repository.invoke(null), legendary), 256));
    }

    public void install() {
        installEarlyThumbnail();
        for (Method method : storage) HookDiagnostics.hookMethod(method, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS, current.get());
                Object task = p.args[0], save = taskSave.get(task);
                String file = (String) name.get(save);
                if (mode.getInt(taskMetadata.get(task)) == 256 && file != null
                        && file.endsWith(".jpg") && selectedForSave(task)) {
                    current.set(new Save(task, capture(task), callback.get(save),
                            orientation.getInt(taskImage.get(task))));
                } else current.remove();
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                Save save = current.get();
                Save previous = (Save) p.getObjectExtra(PREVIOUS);
                if (previous == null) current.remove(); else current.set(previous);
                if (!p.hasThrowable() && save != null && save.uri != null) {
                    boolean alreadyPosted = save.nativeThumbnail
                            || (save.capture != null && sameImage.test(save.uri, save.capture.earlyUri));
                    if (save.callback != null && !alreadyPosted) publish(save);
                    listener.saved(save.uri);
                }
            }
        });
        observe(update, updateWriter);
        observe(insert, insertWriter);
    }

    private static Capture capture(Object task) {
        Object selection = XposedHelpers.getAdditionalInstanceField(task, CAPTURE);
        return selection instanceof Capture ? (Capture) selection : null;
    }

    private boolean selectedForSave(Object task) throws ReflectiveOperationException {
        Object selection = XposedHelpers.getAdditionalInstanceField(task, CAPTURE);
        return selection == null ? selected() : selection instanceof Capture;
    }

    private void installEarlyThumbnail() {
        HookDiagnostics.hookMethod(captureStart, new ObservedHook() {
            @Override protected void runAfter(MethodHookParam p) throws ReflectiveOperationException {
                Object task = p.getResult();
                if (task == null || mode.getInt(taskMetadata.get(task)) != 256) return;
                XposedHelpers.setAdditionalInstanceField(task, CAPTURE, selected() ? new Capture() : Boolean.FALSE);
                p.setObjectExtra(RAN, Boolean.TRUE);
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(RAN));
            }
        });
        // MIVI clones the capture task before filling the quick-image buffer.
        HookDiagnostics.hookMethod(copyTask, new ObservedHook() {
            @Override protected void runAfter(MethodHookParam p) {
                if (p.hasThrowable()) return;
                Object selection = XposedHelpers.getAdditionalInstanceField(p.args[0], CAPTURE);
                if (selection != null) {
                    XposedHelpers.setAdditionalInstanceField(p.thisObject, CAPTURE, selection);
                    p.setObjectExtra(RAN, Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(RAN));
            }
        });
        HookDiagnostics.hookMethod(previewStorage, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam p) {
                Capture capture = capture(p.args[0]);
                p.setObjectExtra(EARLY_PREVIOUS, early.get());
                if (capture == null) early.remove(); else early.set(capture);
                if (capture != null) p.setObjectExtra(RAN, Boolean.TRUE);
            }

            @Override protected void runAfter(MethodHookParam p) {
                Capture previous = (Capture) p.getObjectExtra(EARLY_PREVIOUS);
                if (previous == null) early.remove(); else early.set(previous);
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return Boolean.TRUE.equals(p.getObjectExtra(RAN));
            }
        });
        // At the native UI callback, the saver has already prepared the preview file.
        HookDiagnostics.hookMethod(postThumbnail, new ObservedHook() {
            @Override protected void runBefore(MethodHookParam p) throws IllegalAccessException {
                if (early.get() == null) return;
                Object thumbnail = p.args[0];
                Uri uri = (Uri) thumbnailUri.get(thumbnail);
                Bitmap source = (Bitmap) thumbnailBitmap.get(thumbnail);
                if (uri == null || source == null) return;
                Bitmap gray = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
                gray.setDensity(source.getDensity());
                Paint paint = new Paint();
                paint.setColorFilter(MONO);
                new Canvas(gray).drawBitmap(source, 0f, 0f, paint);
                thumbnailBitmap.set(thumbnail, gray);
                p.setObjectExtra(PUBLISHED, uri);
            }

            @Override protected void runAfter(MethodHookParam p) throws IllegalAccessException {
                if (p.hasThrowable()) return;
                Uri published = (Uri) p.getObjectExtra(PUBLISHED);
                if (published != null) {
                    early.get().earlyUri = published;
                    return;
                }
                Save save = current.get();
                if (save != null && save.uri != null
                        && sameImage.test(save.uri, (Uri) thumbnailUri.get(p.args[0]))) {
                    save.nativeThumbnail = true;
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam p) {
                return p.getObjectExtra(PUBLISHED) != null;
            }
        });
    }

    private void observe(Method method, Field writer) {
        HookDiagnostics.hookMethod(method, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                Save save = current.get();
                if (!p.hasThrowable() && save != null && p.getResult() != null
                        && writerTask.get(writer.get(p.thisObject)) == save.task) {
                    save.uri = (Uri) p.getResult();
                    Log.i(TAG, "file committed " + save.uri);
                }
            }
        });
    }

    private void publish(Save save) throws Throwable {
        Context app = (Context) context.invoke(null);
        // Read the final file directly: MediaStore's early thumbnail can still be the color preview.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = app.getContentResolver().openInputStream(save.uri)) {
            BitmapFactory.decodeStream(input, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0)
            throw new IOException("Cannot decode committed M3 image " + save.uri);
        options.inJustDecodeBounds = false;
        options.inSampleSize = Math.max(1,
                Integer.highestOneBit(Math.max(options.outWidth, options.outHeight) / 1080));
        Bitmap bitmap;
        try (InputStream input = app.getContentResolver().openInputStream(save.uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IOException("Cannot decode M3 thumbnail " + save.uri);
        Object thumbnail = makeThumbnail.invoke(null, save.uri, bitmap, save.orientation, false);
        try (android.content.res.AssetFileDescriptor file =
                     app.getContentResolver().openAssetFileDescriptor(save.uri, "r")) {
            thumbnailSize.invoke(thumbnail, file.getLength());
        }
        publishThumbnail.invoke(save.callback, thumbnail, true);
        Log.i(TAG, "saved-image thumbnail published " + save.uri);
    }

    private static final class Save {
        final Object task, callback;
        final int orientation;
        final Capture capture;
        Uri uri;
        boolean nativeThumbnail;
        Save(Object task, Capture capture, Object callback, int orientation) {
            this.task = task; this.callback = callback; this.orientation = orientation;
            this.capture = capture;
        }
    }

    private static final class Capture {
        volatile Uri earlyUri;
    }
}
