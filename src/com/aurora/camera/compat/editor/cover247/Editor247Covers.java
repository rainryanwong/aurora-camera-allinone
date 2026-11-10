package com.aurora.camera.compat.editor.cover247;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.compat.editor.Editor245TemplateData;
import de.robv.android.xposed.XposedBridge;
import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipFile;

/** Seven native-rendered sample covers, isolated from the active photo and watermark manager. */
public final class Editor247Covers {
    private static final String TAG = "AuroraEditor247Covers", MARKER = "aurora-editor247-cover:";
    private static final String VERSION = "v3-" + Editor245TemplateData.MANIFEST_SHA256.substring(0, 16);
    private final Host host;
    private final boolean nativeCloud;
    private final Class<?> cloudItem;
    private final Field app, adapterItems, itemId, itemGroup, itemCover, holderSuffix, sampleFormat;
    private final Method bind, notifyChanged, initialize, config, model, render, sampleTime;
    private final Constructor<?> nativeItem;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Object> adapters = Collections.newSetFromMap(new WeakHashMap<Object,Boolean>());
    private final ThreadLocal<Boolean> drawing = new ThreadLocal<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "AuroraEditor247Covers"); thread.setDaemon(true); return thread;
    });
    private volatile Map<String,File> ready = Collections.emptyMap();
    private boolean queued, complete;

    public Editor247Covers(Host host) throws ReflectiveOperationException {
        this.host = host;
        nativeCloud = host.usesNativeCloudFilter();
        String prefix = "com.miui.mediaeditor.photo.watermark.";
        Class<?> adapter = host.type(prefix + "PhotoWatermarkItemAdapter");
        Class<?> holder = host.type(prefix + "PhotoWatermarkItemAdapter$Holder");
        cloudItem = host.type(prefix + "model.menu.CloudWatermarkItem");
        adapterItems = host.field(adapter, "f");
        itemId = host.field(cloudItem, "f"); itemGroup = host.field(cloudItem, "i"); itemCover = host.field(cloudItem, "g");
        holderSuffix = host.field(holder, "b"); app = host.field("y2.a", "c");
        bind = host.method(adapter, "onBindViewHolder", host.type("androidx.recyclerview.widget.RecyclerView$ViewHolder"), int.class);
        notifyChanged = host.usesNativeCloudFilter() ? host.method("androidx.recyclerview.widget.RecyclerView$Adapter","notifyDataSetChanged")
                : host.type("androidx.recyclerview.widget.RecyclerView$Adapter").getMethod("notifyDataSetChanged");
        Class<?> item = host.type("com.xiaomi.cam.watermark.b"), nativeConfig = host.type("hn.a"), format = host.type("in.b");
        nativeItem = item.getConstructor(Path.class, boolean.class, int.class);
        initialize = host.method(item, "M", Context.class, boolean.class, boolean.class);
        config = host.method(item, "o"); model = nativeCloud ? host.method(nativeConfig,"F",String.class,String.class,boolean.class)
                : host.method(nativeConfig, "F", String.class, String.class);
        sampleTime = host.method(item, "h0", long.class, boolean.class);
        sampleFormat = host.field(format, "a");
        render = host.method(item, "b", item, Context.class, Bitmap.class, format, PorterDuff.Mode.class, String.class, int.class);
        if (adapterItems.getType() != List.class || holderSuffix.getType() != String.class
                || itemId.getType() != String.class || itemGroup.getType() != String.class || itemCover.getType() != String.class
                || bind.getReturnType() != void.class || notifyChanged.getReturnType() != void.class
                || config.getReturnType() != nativeConfig || model.getReturnType() != void.class
                || initialize.getReturnType() != void.class || sampleTime.getReturnType() != void.class
                || render.getReturnType() != Bitmap.class || !Modifier.isStatic(render.getModifiers())) {
            throw new NoSuchMethodException("Editor247 native sample cover ABI differs");
        }
    }

    public static String marker(String group, String id) {
        return key(group, id) == null ? "" : MARKER + group + "/" + id;
    }
    private static String key(String group, String id) {
        if ("color_chart".equals(group) && ("27".equals(id) || "28".equals(id) || "29".equals(id) || "30".equals(id))) return group + "/" + id;
        if ("signature".equals(group) && ("24".equals(id) || "25".equals(id) || "26".equals(id))) return group + "/" + id;
        return null;
    }
    public boolean rendering() { return Boolean.TRUE.equals(drawing.get()); }

    public void install() {
        XposedBridge.hookMethod(bind, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Object item = ((List<?>) adapterItems.get(p.thisObject)).get((Integer) p.args[1]);
                if (!cloudItem.isInstance(item)) return;
                String group = (String) itemGroup.get(item), id = (String) itemId.get(item);
                String key = key(group, id);
                if (key == null || !marker(group, id).equals(itemCover.get(item))) return;
                // Observe only the adapter actually binding one of our seven sample covers.
                adapters.add(p.thisObject);
                File file = ready.get(key);
                if (file == null) return;
                Object holder = p.args[0];
                p.setObjectExtra("cover247.item", item); p.setObjectExtra("cover247.url", itemCover.get(item));
                p.setObjectExtra("cover247.suffix", holderSuffix.get(holder));
                itemCover.set(item, file.toURI().toString()); holderSuffix.set(holder, "");
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                Object item = p.getObjectExtra("cover247.item");
                if (item == null) return;
                try { itemCover.set(item, p.getObjectExtra("cover247.url")); }
                finally { holderSuffix.set(p.args[0], p.getObjectExtra("cover247.suffix")); }
            }
        });
    }

    /** Called only after the real l(boolean) returns true, never by a timer or catalog preload. */
    public synchronized void nativeReady(Set<String> groups) {
        if (queued || complete || groups.isEmpty()) return;
        Set<String> allowed = new HashSet<>(groups);
        queued = true;
        worker.execute(() -> {
            try {
                Context context = (Context) app.get(null);
                if (context == null) throw new IllegalStateException("Editor app context is missing");
                Map<String,File> generated = generate(context, allowed);
                ready = Collections.unmodifiableMap(generated);
                synchronized (Editor247Covers.this) { complete = generated.size() == 7; }
                main.post(() -> {
                    for (Object adapter : new ArrayList<>(adapters)) {
                        try { notifyChanged.invoke(adapter); }
                        catch (ReflectiveOperationException failure) { Log.e(TAG, "Native cover adapter refresh failed", failure); }
                    }
                });
                Log.i(TAG, "native sample covers ready=" + generated.keySet());
            } catch (Exception failure) {
                Log.e(TAG, "Native sample cover generation failed; no unrendered sample is published", failure);
            } finally {
                synchronized (Editor247Covers.this) { queued = false; }
            }
        });
    }

    private Map<String,File> generate(Context context, Set<String> allowed) throws Exception {
        Path cache = new File(context.getFilesDir(), "aurora-editor247-covers/" + VERSION).toPath();
        Files.createDirectories(cache);
        Map<String,File> result = new LinkedHashMap<>();
        // A separate cache lock protects these copied sample settings across the two editor processes.
        // It never owns the user's watermark root and never holds the native Q manager lock.
        try (FileOutputStream lockStream = new FileOutputStream(cache.resolve(".lock").toFile(), true);
             FileLock lock = lockStream.getChannel().lock(); ZipFile module = new ZipFile(host.moduleApkPath)) {
            for (Editor245TemplateData.Group group : Editor245TemplateData.GROUPS) {
                if (!allowed.contains(group.id)) continue;
                Path copies = cache.resolve("templates");
                copyGroup(module, copies, group.id);
                for (Editor245TemplateData.Template template : group.entries) {
                    String key = group.id + "/" + template.id;
                    Path cover = cache.resolve(group.id + "-" + template.id + ".png");
                    if (!Files.isRegularFile(cover)) render(context, copies.resolve(group.id).resolve(template.id),
                            copies.resolve(group.id).resolve("img_setting_base.webp"), cover);
                    result.put(key, cover.toFile());
                }
            }
        }
        return result;
    }

    private static void copyGroup(ZipFile module, Path copies, String group) throws IOException {
        String prefix = "assets/editor245/watermarks/" + group + "/";
        Enumeration<? extends java.util.zip.ZipEntry> entries = module.entries();
        while (entries.hasMoreElements()) {
            java.util.zip.ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)) continue;
            String relative = entry.getName().substring("assets/editor245/watermarks/".length());
            Path target = copies.resolve(relative);
            Files.createDirectories(target.getParent());
            if (!Files.exists(target)) try (InputStream input = module.getInputStream(entry)) { Files.copy(input, target); }
        }
    }

    private void render(Context context, Path template, Path source, Path output) throws Exception {
        Bitmap input = BitmapFactory.decodeFile(source.toString()), rendered = null, cover = null;
        if (input == null) throw new IOException("Cannot decode stock watermark sample " + source);
        Path temporary = null;
        try {
            Files.createDirectories(template.resolve("userData/current"));
            Object isolated = nativeItem.newInstance(template, false, 4);
            initialize.invoke(isolated, context, false, false);
            // These are cover-only sample labels, never a fabricated S3.b or photo EXIF.
            if(nativeCloud)model.invoke(config.invoke(isolated),"XIAOMI","Xiaomi 14 Ultra",false);
            else model.invoke(config.invoke(isolated), "XIAOMI", "Xiaomi 14 Ultra");
            sampleTime.invoke(isolated, 1704067200000L, false);
            drawing.set(true);
            // Hn.c applies this mode only to the source-photo node. CLEAR deliberately
            // makes an overlay with a transparent photo hole; SRC creates the full sample
            // at the native-computed inner rect, just like the maker's draw-photo branch.
            try { rendered = (Bitmap) render.invoke(null, isolated, context, input, sampleFormat.get(null), PorterDuff.Mode.SRC, "", 72); }
            finally { drawing.remove(); }
            // The stock helper returns its original input on renderer failure. That is not a cover.
            if (rendered == null || rendered == input) throw new IOException("Native watermark renderer did not produce a cover: " + template);
            // The native adapter uses CENTER_CROP. Match its real cover dimensions and fit the
            // entire native frame, otherwise that crop can remove the watermark from the sample.
            int width = context.getResources().getDimensionPixelSize(0x7f070fb6);
            int height = context.getResources().getDimensionPixelSize(0x7f070fb5);
            cover = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(cover);
            canvas.drawColor(0xff262626);
            float scale = Math.min((float) width / rendered.getWidth(), (float) height / rendered.getHeight());
            float left = (width - rendered.getWidth() * scale) / 2f, top = (height - rendered.getHeight() * scale) / 2f;
            canvas.drawBitmap(rendered, null, new RectF(left, top, width - left, height - top), new Paint(3));
            temporary = Files.createTempFile(output.getParent(), ".cover247-", ".png");
            try (FileOutputStream stream = new FileOutputStream(temporary.toFile())) {
                if (!cover.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException("Cannot encode native sample cover");
                stream.getFD().sync();
            }
            Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary);
            if (cover != null) cover.recycle();
            if (rendered != null && rendered != input) rendered.recycle();
            input.recycle();
        }
    }
}
