package com.aurora.camera.compat.editor.more246;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.sharedlut.Recipe248;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.zip.ZipFile;

/** One complete bitmap processor used by ordinary-photo native preview and save. */
public final class EditorRecipe248 {
    private final Constructor<?> sdk;
    private final Field handle;
    private final Method configure,update,setLut,process,release;
    public EditorRecipe248(Host host)throws ReflectiveOperationException{
        Class<?> type=host.type("com.xiaomi.milab.filtersdk.CandySDK");sdk=type.getDeclaredConstructor();handle=host.field(type,"a");
        configure=host.method(type,"configPipeline",long.class,String.class);setLut=host.method(type,"setLutBitmap",long.class,Bitmap.class,int.class);
        update=host.method(type,"updatePipeline",long.class,String.class);
        process=host.method(type,"processBitmap",long.class,Bitmap.class,float[].class);release=host.method(type,"release",long.class);
        if(handle.getType()!=long.class||configure.getReturnType()!=boolean.class||setLut.getReturnType()!=boolean.class||update.getReturnType()!=boolean.class||release.getReturnType()!=boolean.class)throw new NoSuchMethodException("Editor248 Candy raw ABI differs");
        // Read lazily on the actual render worker, not during catalog/UI initialization.
        module=host.moduleApkPath;
    }
    private final String module;private StyleGpu248 loaded;
    private ByteBuffer transfer;
    private final Stage primary=new Stage(),soft=new Stage(),paletteStage=new Stage();
    public synchronized void apply(Bitmap target,File lut,Recipe248 recipe,int degree)throws Exception{
        apply(target,lut,recipe,degree,null);
    }
    public synchronized void apply(Bitmap target,File lut,Recipe248 recipe,int degree,String palette)throws Exception{
        long started=android.os.SystemClock.elapsedRealtimeNanos();
        if(degree<0||degree>100)throw new IllegalArgumentException("Recipe degree outside 0..100");
        if(degree==0){if(palette!=null)applyPalette(target,palette);return;}
        if(Looper.myLooper()!=null&&Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Recipe rendering reached the UI thread");
        if(!target.isMutable()||target.getConfig()!=Bitmap.Config.ARGB_8888)throw new IOException("Camera recipe requires the native mutable SDR photo bitmap");
        Object gainmap=Build.VERSION.SDK_INT>=34?target.getGainmap():null;
        ColorSpace targetSpace=target.getColorSpace();
        boolean p3=ColorSpace.get(ColorSpace.Named.DISPLAY_P3).equals(targetSpace);
        if(targetSpace==null||(!targetSpace.isSrgb()&&!p3))throw new IOException("Camera recipe supports SDR sRGB / Display P3 only: "+targetSpace);
        Bitmap full=baseCopy(target);
        try{
            String before=recipe.beforeStyle(),after=recipe.afterStyle();
            String identity=recipe.identity();
            if(degree==100&&recipe.algos()==0) {
                String chain=append(append(before,after),palette);
                if(!chain.isEmpty())primary.apply(full,chain,before.isEmpty()?null:lut,identity+":combined");
            } else {
                if(!before.isEmpty())primary.apply(full,before,lut,identity+":before");
                if(recipe.algos()!=0){if(loaded==null)loaded=new StyleGpu248(shader());Bitmap output=loaded.apply(full,recipe);full.recycle();full=output;}
                if(!after.isEmpty())soft.apply(full,after,null,after);
                if(degree==100&&palette!=null)paletteStage.apply(full,palette,null,"palette");
            }
            if(degree==100)copy(target,full);else mix(target,full,degree);
            if(degree<100&&palette!=null)applyPalette(target,palette);
            boolean gainmapPreserved=gainmap==null||target.getGainmap()==gainmap;
            Log.i("AuroraRecipe248","complete bitmap recipe applied degree="+degree+" size="+target.getWidth()+"x"+target.getHeight()+" algos="+recipe.algos()+" soft="+recipe.style()[6]+" color="+targetSpace.getName()+" baseEdit=true gainmapInput="+(gainmap!=null)+" gainmapPreserved="+gainmapPreserved+" palette="+(palette!=null)+" elapsed_ns="+(android.os.SystemClock.elapsedRealtimeNanos()-started));
        }finally{full.recycle();}
    }
    /** Match the native bitmap path: update raw RGBA without rebuilding CS or gainmap. */
    public synchronized void applyPalette(Bitmap target,String script)throws Exception {
        long started=android.os.SystemClock.elapsedRealtimeNanos();
        if(Looper.myLooper()!=null&&Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("Palette rendering reached the UI thread");
        if(!target.isMutable()||target.getConfig()!=Bitmap.Config.ARGB_8888)throw new IOException("Filter palette requires the native mutable SDR photo bitmap");
        paletteStage.apply(target,script,null,"palette");
        Log.i("AuroraRecipe248","editor filter palette applied: "+script+" elapsed_ns="+(android.os.SystemClock.elapsedRealtimeNanos()-started));
    }
    private static String append(String first,String next){return next==null||next.isEmpty()?first:first.isEmpty()?next:first+"@"+next;}
    private static String structure(String script){
        StringBuilder result=new StringBuilder();
        for(String effect:script.split("@")){int end=effect.indexOf(';');result.append(end<0?effect:effect.substring(0,end)).append('@');}
        return result.toString();
    }
    private Bitmap baseCopy(Bitmap source){
        Bitmap result=Bitmap.createBitmap(source.getWidth(),source.getHeight(),Bitmap.Config.ARGB_8888,source.hasAlpha(),source.getColorSpace());
        try{result.setDensity(source.getDensity());copy(result,source);return result;}
        catch(RuntimeException|Error failure){result.recycle();throw failure;}
    }
    private void copy(Bitmap destination,Bitmap source) {
        if(destination.getRowBytes()!=source.getRowBytes()){mix(destination,source,100);return;}
        int size=source.getByteCount();
        if(transfer==null||transfer.capacity()<size)transfer=ByteBuffer.allocateDirect(size);
        transfer.clear();transfer.limit(size);source.copyPixelsToBuffer(transfer);transfer.position(0);destination.copyPixelsFromBuffer(transfer);
    }
    public synchronized void close()throws Exception {
        Exception primaryFailure=null;
        for(Stage stage:new Stage[]{primary,soft,paletteStage})try{stage.close();}catch(Exception failure){if(primaryFailure==null)primaryFailure=failure;else primaryFailure.addSuppressed(failure);}
        try{if(loaded!=null){loaded.close();loaded=null;}}finally{transfer=null;}
        if(primaryFailure!=null)throw primaryFailure;
    }
    /** Like CandyAlgorithm, retain a configured chain and LUT until their identity changes. */
    private final class Stage {
        private Object owner;
        private long value;
        private String identity;
        private Bitmap image;
        void apply(Bitmap bitmap,String script,File lut,String key)throws Exception {
            String next=key+"\n"+(lut==null?"":lut.getAbsolutePath())+"\n"+structure(script);
            if(!next.equals(identity)) {
                close();owner=sdk.newInstance();value=handle.getLong(owner);
                if(value==0)throw new IOException("Native recipe Candy initialization returned no handle");
                try {
                    if(!Boolean.TRUE.equals(configure.invoke(null,value,script)))throw new IOException("Native Candy rejected recipe stage: "+script);
                    if(lut!=null) {
                        image=BitmapFactory.decodeFile(lut.getAbsolutePath());if(image==null)throw new IOException("Cannot decode shared recipe base LUT");
                        if(!Boolean.TRUE.equals(setLut.invoke(null,value,image,-1)))throw new IOException("Native Candy rejected recipe base LUT");
                    }
                    identity=next;
                } catch(Exception failure) {try{close();}catch(Exception cleanup){failure.addSuppressed(cleanup);}throw failure;}
            } else if(!Boolean.TRUE.equals(update.invoke(null,value,script)))throw new IOException("Native Candy rejected recipe update: "+script);
            process.invoke(null,value,bitmap,new float[]{0,0,bitmap.getWidth(),bitmap.getHeight()});
        }
        void close()throws Exception {
            try {
                if(value!=0&&!Boolean.TRUE.equals(release.invoke(null,value)))throw new IOException("Native recipe Candy release failed");
                if(owner!=null)handle.setLong(owner,0);
            } finally {value=0;owner=null;identity=null;if(image!=null){image.recycle();image=null;}}
        }
    }
    private String shader()throws Exception{
        if(module==null)throw new IOException("Recipe shader module APK path is missing");byte[] bytes;
        try(ZipFile zip=new ZipFile(module);InputStream input=zip.getInputStream(zip.getEntry("assets/recipes248/style_adjust.frag"));ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] block=new byte[4096];int count,total=0;while((count=input.read(block))!=-1){total+=count;if(total>16384)throw new IOException("Recipe shader exceeds pinned size");out.write(block,0,count);}bytes=out.toByteArray();}
        StringBuilder digest=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))digest.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        if(!digest.toString().equals("5edae90d8b0ce90681d5f6958fe4cbc4e80732d3c82fc08b2a51a631a9e41e65"))throw new IOException("Recipe shader differs from the pinned Camera body binding");
        return new String(bytes,StandardCharsets.US_ASCII);
    }
    static void mix(Bitmap source,Bitmap effect,int degree){
        // getPixels/setPixels use sRGB integers even for P3. Mix native premultiplied
        // bytes in the target space instead, retaining the borrowed bitmap's CS/owner.
        byte[] a=new byte[source.getByteCount()],b=new byte[effect.getByteCount()];
        source.copyPixelsToBuffer(ByteBuffer.wrap(a));effect.copyPixelsToBuffer(ByteBuffer.wrap(b));
        int span=Math.multiplyExact(source.getWidth(),4),strideA=source.getRowBytes(),strideB=effect.getRowBytes();
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<span;x++){
            int i=y*strideA+x,j=y*strideB+x;
            a[i]=(byte)(((a[i]&255)*(100-degree)+(b[j]&255)*degree+50)/100);
        }
        source.copyPixelsFromBuffer(ByteBuffer.wrap(a));
    }
    public static int mixPixel(int source,int effect,int degree){int result=0;for(int shift=0;shift<32;shift+=8){int a=source>>>shift&255,b=effect>>>shift&255;result|=((a*(100-degree)+b*degree+50)/100)<<shift;}return result;}
}
