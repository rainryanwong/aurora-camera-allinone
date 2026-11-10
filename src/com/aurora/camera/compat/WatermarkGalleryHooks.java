package com.aurora.camera.compat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Draw the native gallery layout directly at its ImageView resolution. */
public final class WatermarkGalleryHooks {
    private static final int THUMBNAIL_HEIGHT = 0x7f071b40;
    private static final String PREVIOUS = "watermark.gallery.previousHeight";
    private static final String ENTERED = "watermark.gallery.entered";
    private WatermarkGalleryHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        Class<?> preference = host.type("com.android.camera.fragment.watermark.wmSettingV2.WmGalleryPreference");
        Field context = host.field(preference, "g0");
        Field manager = host.field(preference, "L0");
        Class<?> photoManager = host.type("Gg.T");
        Method load = host.method("v5.e", "run");
        Field loadOwner = host.field("v5.e", "a");
        Method update = host.method("S5.i0", "run");
        Field updateOwner = host.field("S5.i0", "b");
        Class<?> node = host.type("yu.a");
        Class<?> coverRoot = host.type("yu.n");
        Class<?> drawing = host.type("Au.a");
        Method rasterize = host.method(node, "i", node, ColorSpace.class,
                int.class, int.class, String.class, int.class);
        Method measure = host.method(node, "h", int.class, int.class);
        Method layout = host.method(node, "f", int.class, int.class);
        Method background = host.method(node, "b", drawing, String.class);
        Method content = host.method(node, "a", drawing, String.class);
        Field width = host.field(node, "h"), measuredHeight = host.field(node, "i");
        Field canvas = host.field(drawing, "a");
        Constructor<?> drawingConstructor = drawing.getDeclaredConstructor(Bitmap.class);
        drawingConstructor.setAccessible(true);
        ThreadLocal<Integer> height = new ThreadLocal<>();
        return new HookPlan() {
            public String id() { return "watermark-gallery-thumbnails"; }
            public void install() {
                XposedBridge.hookMethod(load, scope(loadOwner, context, preference, manager, photoManager, height));
                XposedBridge.hookMethod(update, scope(updateOwner, context, preference, manager, photoManager, height));
                XposedBridge.hookMethod(rasterize, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        Integer target = height.get();
                        if (target == null || !coverRoot.isInstance(p.args[0])) return;
                        Object root = p.args[0];
                        int defaults = (Integer) p.args[5];
                        ColorSpace color = (defaults & 2) != 0 ? null : (ColorSpace) p.args[1];
                        int requestedWidth = (defaults & 4) != 0 ? 0 : (Integer) p.args[2];
                        int requestedHeight = (defaults & 8) != 0 ? 0 : (Integer) p.args[3];
                        String group = (defaults & 16) != 0 ? "" : (String) p.args[4];
                        measure.invoke(root, requestedWidth, requestedHeight);
                        int fullWidth = width.getInt(root), fullHeight = measuredHeight.getInt(root);
                        layout.invoke(root, fullWidth, fullHeight);
                        float scale = Math.min(1f, target / (float) fullHeight);
                        int outputWidth = Math.max(1, Math.round(fullWidth * scale));
                        int outputHeight = Math.min(fullHeight, target);
                        if (color == null) color = ColorSpace.get(ColorSpace.Named.SRGB);
                        Bitmap bitmap = Bitmap.createBitmap(outputWidth, outputHeight,
                                Bitmap.Config.ARGB_8888, true, color);
                        Object painter = drawingConstructor.newInstance(bitmap);
                        // Keep native layout coordinates; rasterize once into the smaller bitmap.
                        ((Canvas) canvas.get(painter)).scale(scale, scale);
                        background.invoke(root, painter, group);
                        content.invoke(root, painter, group);
                        p.setResult(bitmap);
                    }
                });
            }
        };
    }

    private static StrictHook scope(Field owner, Field context, Class<?> preference, Field manager,
                                    Class<?> photoManager, ThreadLocal<Integer> height) {
        return new StrictHook() {
            @Override protected void before(MethodHookParam p) throws IllegalAccessException {
                Object gallery = owner.get(p.thisObject);
                if (!preference.isInstance(gallery)) return;
                if (!photoManager.isInstance(manager.get(gallery))) return;
                Context app = (Context) context.get(gallery);
                int target = app.getResources().getDimensionPixelSize(THUMBNAIL_HEIGHT);
                p.setObjectExtra(PREVIOUS, height.get());
                p.setObjectExtra(ENTERED, true);
                height.set(target);
            }
            @Override protected void after(MethodHookParam p) {
                if (!Boolean.TRUE.equals(p.getObjectExtra(ENTERED))) return;
                Integer previous = (Integer) p.getObjectExtra(PREVIOUS);
                if (previous == null) height.remove(); else height.set(previous);
            }
        };
    }
}
