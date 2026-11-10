package com.aurora.camera.compat.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.*;
import java.util.*;

/** Native XmlElement model consumers, including car_*; no global TextView hooks. */
public final class Editor245Xml {
    private final Method create, update, model, brand, product, format, uppercase, setFormat;
    private final Field view, exif, override, series, number, version, dark, logoWidth, logoHeight;
    private final EditorWatermark266 special;
    private final boolean nativeCloud;
    private final Map<ImageView,LogoState> logos=Collections.synchronizedMap(new WeakHashMap<ImageView,LogoState>());
    private final Map<View,Integer> hidden=Collections.synchronizedMap(new WeakHashMap<View,Integer>());
    private static final class LogoState {
        final Drawable drawable;final float width,height;
        LogoState(ImageView view,float width,float height){this.drawable=view.getDrawable();this.width=width;this.height=height;}
    }
    private final Constructor<?> formattedModel;
    private final Class<?> textClass, formatClass;
    private final Map<TextView,Typeface> originalFonts = Collections.synchronizedMap(new WeakHashMap<TextView,Typeface>());
    public Editor245Xml(Host h, EditorWatermark266 special) throws ReflectiveOperationException {
        this.special=special;
        nativeCloud=h.usesNativeCloudFilter();
        String prefix = "com.miui.mediaeditor.photo.watermark.";
        Class<?> render = h.type(prefix + "model.render.WatermarkRenderData");
        create = h.method(prefix + "element.k", "d", ViewGroup.class, RectF.class, Bitmap.class, render, float.class, boolean.class, boolean.class);
        update = h.method(prefix + "element.k", "e", RectF.class, Bitmap.class, render, float.class);
        view = h.field(prefix + "element.d", "i");
        exif = h.field(render, "n"); override = h.field(render, "o");
        dark=h.field(render,"e");
        logoWidth=h.field(prefix+"widgets.elementview.LogoElementView","m");
        logoHeight=h.field(prefix+"widgets.elementview.LogoElementView","n");
        model = h.method("kh.g", "o", h.type("S3.b"), h.type("kh.e"), boolean.class);
        brand = h.method("kh.g", "e", h.type("S3.b"), h.type("kh.e"), boolean.class);
        product = nativeCloud ? h.method("Qn.a$a", "c", String.class, boolean.class, boolean.class)
                : h.method("Qn.a$a", "c", String.class, String.class, String.class, boolean.class);
        series = h.field("In.a", "a"); number = nativeCloud ? null : h.field("In.a", "b"); version = h.field("In.a", "d");
        textClass = h.type(prefix + "widgets.elementview.TextElementView");
        formatClass = h.type(prefix + "widgets.elementview.FormatModelElementView");
        format = h.method(textClass, "getFormat"); uppercase = h.method(textClass, "getUppercase");
        setFormat = h.method(formatClass, "setFormatModelData", h.type("kh.g$a"));
        formattedModel = nativeCloud ? h.type("kh.g$a").getConstructor(String.class,String.class)
                : h.type("kh.g$a").getConstructor(String.class, String.class, String.class);
        if (view.getType() != View.class || exif.getType() != h.type("S3.b") || override.getType() != h.type("kh.e")
                || !TextView.class.isAssignableFrom(textClass) || model.getReturnType() != String.class
                || brand.getReturnType() != String.class || product.getReturnType() != h.type("In.a")) {
            throw new NoSuchMethodException("Editor245 XML model contract differs");
        }
    }
    public void install() {
        XposedBridge.hookMethod(create, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable { if (!p.hasThrowable()) apply(p.thisObject, p.args[3]); }
        });
        XposedBridge.hookMethod(update, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable { restore((View)view.get(p.thisObject)); }
            @Override protected void after(MethodHookParam p) throws Throwable { if (!p.hasThrowable()) apply(p.thisObject, p.args[2]); }
        });
    }
    private void apply(Object element, Object data) throws Exception {
        View root = (View) view.get(element);
        if (root == null) return; // Native XML element has not constructed a view.
        Context context = root.getContext();
        String selected = Editor245State.read(context);
        int textId = context.getResources().getIdentifier("frame_model_view", "id", Editor245Policy.PACKAGE);
        View candidate = root.findViewById(textId);
        Object photo = exif.get(data), nativeOverride = override.get(data);
        if (textClass.isInstance(candidate)) {
            TextView target = (TextView) candidate;
            String pattern = (String) format.invoke(target);
            if (pattern == null) pattern = context.getString(context.getResources().getIdentifier("gallery_frame_default_model_format", "string", Editor245Policy.PACKAGE));
            String value = selected.isEmpty() ? (String) model.invoke(null, photo, nativeOverride, uppercase.invoke(target)) : selected;
            target.setText(String.format(pattern, value));
            if (selected.isEmpty()) {
                Typeface original = originalFonts.remove(target);
                if (original != null) target.setTypeface(original);
            } else if (!hasGlyphs(target, value)) {
                if (!originalFonts.containsKey(target)) originalFonts.put(target, target.getTypeface());
                target.setTypeface(Typeface.create("sans-serif", target.getTypeface() == null ? Typeface.NORMAL : target.getTypeface().getStyle()));
            }
        }
        int formatId = context.getResources().getIdentifier("frame_format_model_view", "id", Editor245Policy.PACKAGE);
        View structured = root.findViewById(formatId);
        if (formatClass.isInstance(structured)) {
            Object value;
            Object shown = selected.isEmpty() ? model.invoke(null, photo, nativeOverride, false) : selected;
            Object parsed = nativeCloud ? product.invoke(null,shown,false,false)
                    : product.invoke(null, brand.invoke(null, photo, nativeOverride, false),shown,"",false);
            value = nativeCloud ? formattedModel.newInstance(series.get(parsed),version.get(parsed))
                    : formattedModel.newInstance(series.get(parsed), number.get(parsed), version.get(parsed));
            setFormat.invoke(structured, value);
        }
        String name=EditorWatermark266.model(selected.isEmpty() ? (String)model.invoke(null,photo,nativeOverride,false) : selected);
        View brandView=find(root,"frame_image_brand_view");
        if(EditorWatermark266.special(name) && brandView instanceof ImageView) {
            ImageView logo=(ImageView)brandView;
            Bitmap bitmap=special.bitmap(context,name,dark.getBoolean(data)?"xiaomi_white.webp":"xiaomi_black.webp");
            float height=logoHeight.getFloat(logo);
            logos.put(logo,new LogoState(logo,logoWidth.getFloat(logo),height));
            logo.setImageBitmap(bitmap);
            logoWidth.setFloat(logo,height*bitmap.getWidth()/bitmap.getHeight());
            logo.requestLayout();
            hide(candidate);hide(structured);
        }
    }
    private static View find(View root,String name) {
        Context context=root.getContext();
        return root.findViewById(context.getResources().getIdentifier(name,"id",Editor245Policy.PACKAGE));
    }
    private void hide(View target) {
        if(target==null)return;
        hidden.put(target,target.getVisibility());target.setVisibility(View.GONE);
    }
    private void restore(View root) throws IllegalAccessException {
        if(root==null)return;
        View brand=find(root,"frame_image_brand_view");
        if(brand instanceof ImageView) {
            LogoState state=logos.remove(brand);
            if(state!=null) {
                ((ImageView)brand).setImageDrawable(state.drawable);
                logoWidth.setFloat(brand,state.width);logoHeight.setFloat(brand,state.height);
                brand.requestLayout();
            }
        }
        for(String id:new String[]{"frame_model_view","frame_format_model_view"}) {
            View target=find(root,id);Integer visibility=hidden.remove(target);
            if(visibility!=null)target.setVisibility(visibility);
        }
    }
    private static boolean hasGlyphs(TextView view, String value) {
        for (int i = 0; i < value.length();) {
            int point = value.codePointAt(i), count = Character.charCount(point);
            if (point != 0x200d && point != 0xfe0e && point != 0xfe0f && !Character.isWhitespace(point)
                    && !view.getPaint().hasGlyph(value.substring(i, i + count))) return false;
            i += count;
        }
        return true;
    }
}
