package com.aurora.camera.compat.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.PorterDuff;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.features.watermark.WatermarkFonts217;
import de.robv.android.xposed.XposedBridge;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

/** Special model artwork is resolved in the editor, before native image sizing. */
public final class EditorWatermark266 {
    private final String module;
    private final boolean nativeCloud;
    private final Field device, position, series, separator, number, version;
    private final Method image, load, product, compose, nodeId, compositionPaint, configureModel, renderTree;
    private final java.util.Map<Object,String> modelNames=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<Object,String>());
    private final ThreadLocal<String> originalName=new ThreadLocal<>();
    private final ThreadLocal<Artwork> artwork = new ThreadLocal<>();
    private static final String PREVIOUS="aurora.editor266.artwork";
    private static final class Artwork {
        final Context context;
        final String name;
        final boolean shadow;
        Artwork(Context context,String name,boolean shadow) { this.context=context;this.name=name;this.shadow=shadow; }
    }

    public EditorWatermark266(Host h) throws ReflectiveOperationException {
        module=h.moduleApkPath;
        nativeCloud=h.usesNativeCloudFilter();
        configureModel=nativeCloud ? h.method("hn.a","F",String.class,String.class,boolean.class)
                : h.method("hn.a","F",String.class,String.class);
        renderTree=h.method("com.xiaomi.cam.watermark.c","b",h.type("com.xiaomi.cam.watermark.c"),Context.class,
                Bitmap.class,h.type("in.b"),int.class,h.type("hn.a"),String.class,h.type("mn.h"),boolean.class,
                PorterDuff.Mode.class,h.type("Jm.X"),boolean.class,int.class);
        nodeId=h.method("mn.a","getId");
        device=h.field("com.xiaomi.cam.watermark.c","d");
        position=h.field("com.xiaomi.cam.watermark.c","a");
        image=h.method("com.xiaomi.cam.watermark.c","e",Context.class,h.type("mn.g"),Bitmap.class,
                h.type("in.b"),h.type("kn.d"),String.class,float.class,String.class,boolean.class,
                PorterDuff.Mode.class,h.type("Jm.X"),boolean.class);
        load=nativeCloud ? h.method("on.a","a",String.class,boolean.class,ColorSpace.class)
                : h.method("on.a","a",String.class,boolean.class);
        product=nativeCloud ? h.method("Qn.a$a","c",String.class,boolean.class,boolean.class)
                : h.method("Qn.a$a","c",String.class,String.class,String.class,boolean.class);
        series=h.field("In.a","a");separator=nativeCloud?null:h.field("In.a","b");
        number=nativeCloud?null:h.field("In.a","c");version=h.field("In.a","d");
        compose=Class.forName("com.aurora.camera.features.watermark.OnlineLeicaRenderer")
                .getMethod("compose",Context.class,Bitmap.class,String.class,String.class);
        compositionPaint=compose.getDeclaringClass().getDeclaredMethod("createTextPaint",android.content.res.AssetManager.class,String.class,float.class);
        compositionPaint.setAccessible(true);
    }

    public static String model(String value) {
        if(value==null)return "";
        String name=value.trim();
        if(name.regionMatches(true,0,"Xiaomi ",0,7))name=name.substring(7).trim();
        return name;
    }
    public static boolean titanium(String name) { return name.toLowerCase(Locale.ROOT).endsWith(" ti"); }
    public static boolean leitz(String name) {
        return name.equalsIgnoreCase("leitzphone powered by xiaomi") || name.equalsIgnoreCase("LEITZ Phone by xiaomi");
    }
    public static boolean leica(String name) { return name.toLowerCase(Locale.ROOT).endsWith(" by leica"); }
    public static boolean special(String name) { return leica(name)||leitz(name); }
    public String deviceName(Object renderer) throws IllegalAccessException {
        String original=originalName.get();return model(original!=null?original:(String)device.get(renderer));
    }

    public void install(java.util.function.BooleanSupplier sampleCover) {
        XposedBridge.hookMethod(configureModel,new StrictHook() {
            @Override protected void after(MethodHookParam p) {
                if(!p.hasThrowable())modelNames.put(p.thisObject,(String)p.args[1]);
            }
        });
        XposedBridge.hookMethod(renderTree,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                p.setObjectExtra(PREVIOUS+".name",originalName.get());
                originalName.set(modelNames.get(p.args[5]));
            }
            @Override protected void after(MethodHookParam p) {
                String previous=(String)p.getObjectExtra(PREVIOUS+".name");
                if(previous==null)originalName.remove();else originalName.set(previous);
            }
        });
        XposedBridge.hookMethod(product,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if(p.hasThrowable()||p.getResult()==null)return;
                String name=model((String)p.args[nativeCloud?0:1]);
                if(!titanium(name))return;
                Object parsed=p.getResult();
                series.set(parsed,name.substring(0,name.length()-3).trim());
                if(!nativeCloud){separator.set(parsed," ");number.set(parsed,"");}
                version.set(parsed,"Ti");
            }
        });
        XposedBridge.hookMethod(image,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS,artwork.get());artwork.remove();
                if(sampleCover.getAsBoolean() || !"logo".equals(nodeId.invoke(p.args[1])))return;
                Context context=(Context)p.args[0];
                String selected=Editor245State.read(context);
                String name=selected.isEmpty()?deviceName(p.thisObject):model(selected);
                if(!special(name))return;
                artwork.set(new Artwork(context,name,"in".equals(position.get(p.thisObject))));
                // Use the normal logo loader, then supply the full artwork before OEM sizing.
                p.setObjectExtra(PREVIOUS+".device",device.get(p.thisObject));
                device.set(p.thisObject,leitz(name)?"leitzphone":name.substring(0,name.length()-9).trim());
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                if(p.getObjectExtra(PREVIOUS+".device")!=null)device.set(p.thisObject,p.getObjectExtra(PREVIOUS+".device"));
                Artwork previous=(Artwork)p.getObjectExtra(PREVIOUS);
                if(previous==null)artwork.remove();else artwork.set(previous);
            }
        });
        XposedBridge.hookMethod(load,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Artwork current=artwork.get();if(current==null)return;
                String path=(String)p.args[0];
                String file=path.substring(path.lastIndexOf('/')+1);
                if(!file.startsWith("xiaomi_")||!file.endsWith(".webp"))return;
                if(current.shadow&&!file.endsWith("_shadow.webp"))file=file.substring(0,file.length()-5)+"_shadow.webp";
                p.setResult((Boolean)p.args[1] ? dimensions(current.context,current.name,file)
                        : bitmap(current.context,current.name,file,nativeCloud?(ColorSpace)p.args[2]:null));
            }
        });
    }

    private Bitmap dimensions(Context context,String name,String file) throws Exception {
        String directory=leitz(name)?"leitzphone_powered_by_xiaomi":"17_ultra_by_leica";
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        try(InputStream in=context.getAssets().open("cloud_watermark_material/"+directory+"/"+file)) {
            BitmapFactory.decodeStream(in,null,options);
        }
        int width=options.outWidth;
        if(leica(name)&&!name.equalsIgnoreCase("17 Ultra by Leica")) {
            WatermarkFonts217.configureEditor(context,module);
            android.graphics.Paint paint=(android.graphics.Paint)compositionPaint.invoke(null,context.getAssets(),file,options.outHeight/160f);
            float delta=(paint.measureText(name.substring(0,name.length()-9).trim())-paint.measureText("17 Ultra"))*Float.intBitsToFloat(0x3f7fdd98);
            if(file.equals("xiaomi_white_shadow.webp"))delta*=Float.intBitsToFloat(0x3f7968bc);
            width+=Math.round(delta);
        }
        return Bitmap.createBitmap(width,options.outHeight,Bitmap.Config.ARGB_8888);
    }

    public Bitmap bitmap(Context context,String name,String file) throws Exception {
        return bitmap(context,name,file,null);
    }
    private Bitmap bitmap(Context context,String name,String file,ColorSpace colorSpace) throws Exception {
        String directory=leitz(name)?"leitzphone_powered_by_xiaomi":"17_ultra_by_leica";
        Bitmap source;
        try(InputStream in=context.getAssets().open("cloud_watermark_material/"+directory+"/"+file)) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredColorSpace=colorSpace;
            source=BitmapFactory.decodeStream(in,null,options);
        }
        if(source==null)throw new java.io.IOException("Cannot decode watermark artwork: "+directory+"/"+file);
        if(leitz(name)||name.equalsIgnoreCase("17 Ultra by Leica"))return source;
        WatermarkFonts217.configureEditor(context,module);
        try { return (Bitmap)compose.invoke(null,context,source,name.substring(0,name.length()-9).trim(),file); }
        finally { source.recycle(); }
    }
}
