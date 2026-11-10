package com.aurora.camera.compat.legend;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Publishes the committed M3 image after the native saver has finished its callbacks. */
public final class M3SaveCompletion261 {
    public interface Listener { void saved(Uri uri); }
    private static final String TAG = "AuroraM3Save";
    private static final String PREVIOUS = "aurora.m3.save.previous";
    private final ThreadLocal<Save> current = new ThreadLocal<>();
    private final Method[] storage;
    private final Method update, insert, repository, component, value, context, makeThumbnail,
            publishThumbnail, thumbnailSize;
    private final Field taskMetadata, taskSave, taskImage, mode, name, callback, orientation,
            writerTask, updateWriter, insertWriter;
    private final Class<?> legendary;
    private final Listener listener;

    public M3SaveCompletion261(Host h, Listener listener) throws ReflectiveOperationException {
        this.listener = listener;
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
    }

    public boolean selected() throws ReflectiveOperationException {
        return "M3".equals(value.invoke(component.invoke(repository.invoke(null), legendary), 256));
    }

    public void install() {
        for (Method method : storage) XposedBridge.hookMethod(method, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS, current.get());
                Object task = p.args[0], save = taskSave.get(task);
                String file = (String) name.get(save);
                if (mode.getInt(taskMetadata.get(task)) == 256 && file != null
                        && file.endsWith(".jpg") && selected()) {
                    current.set(new Save(task, callback.get(save),
                            orientation.getInt(taskImage.get(task))));
                } else current.remove();
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                Save save = current.get();
                Save previous = (Save) p.getObjectExtra(PREVIOUS);
                if (previous == null) current.remove(); else current.set(previous);
                if (!p.hasThrowable() && save != null && save.uri != null) {
                    if (save.callback != null) publish(save);
                    listener.saved(save.uri);
                }
            }
        });
        observe(update, updateWriter);
        observe(insert, insertWriter);
    }

    private void observe(Method method, Field writer) {
        XposedBridge.hookMethod(method, new StrictHook() {
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
        Uri uri;
        Save(Object task, Object callback, int orientation) {
            this.task = task; this.callback = callback; this.orientation = orientation;
        }
    }
}
