package com.aurora.camera.videowm;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.lang.reflect.Method;

final class OfficialTemplate {
    static ClassLoader host;

    static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, host);
    }

    static Object manager() throws ReflectiveOperationException {
        return type("U8.d").getMethod("b", boolean.class).invoke(null, true);
    }

    static Bitmap render(Object item, int angle) {
        Bitmap layer = null;
        try {
            Application app = (Application) type("com.xiaomi.camera.basic.Global").getMethod("getApplication").invoke(null);
            Class<?> modeClass = type("qs.b");
            Object mode = modeClass.getField("b").get(null);
            Method draw = item.getClass().getMethod("c", Application.class, Bitmap.class, modeClass, int.class);
            boolean portrait = angle == 90 || angle == 270;
            layer = drawLayer(app, item, draw, mode, portrait ? 1080 : 1920, portrait ? 1920 : 1080);
            Bitmap output = angle == 0 ? layer : rotate(layer, -angle);
            if (output == layer) layer = null;
            return output;
        } catch (Exception e) {
            throw new IllegalStateException("Official template preparation failed", e);
        } finally {
            if (layer != null) layer.recycle();
        }
    }

    private static Bitmap drawLayer(Application app, Object item, Method draw, Object mode, int width, int height) throws Exception {
        Bitmap input = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap result = null;
        try {
            result = (Bitmap) draw.invoke(item, app, input, mode, 0);
            if (result == null || result.getWidth() != width || result.getHeight() != height) {
                throw new IllegalStateException("Unexpected overlay dimensions for " + width + "x" + height);
            }
            Bitmap ready = result;
            result = null;
            if (ready == input) input = null;
            return ready;
        } finally {
            if (result != null && result != input) result.recycle();
            if (input != null) input.recycle();
        }
    }

    private static Bitmap rotate(Bitmap source, int degrees) {
        Matrix rotation = new Matrix();
        rotation.setRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), rotation, false);
    }
}
