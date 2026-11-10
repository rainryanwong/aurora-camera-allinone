package com.aurora.camera.features.watermark;

import android.content.Context;
import android.graphics.Typeface;
import com.aurora.camera.compat.Host;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.zip.ZipFile;

/** The composition font belongs to the LSP APK; stock camera assets stay intact. */
public final class WatermarkFonts217 {
    private static String module;private static Method context;private static Context editorContext;private static File font;
    private WatermarkFonts217(){}
    public static void configure(Host h)throws ReflectiveOperationException {module=h.moduleApkPath;context=h.method("com.xiaomi.camera.basic.Global","getContext");}
    public static synchronized void configureEditor(Context app,String apk) { module=apk;editorContext=app.getApplicationContext(); }
    public static synchronized Typeface.Builder builder()throws Exception {
        if(font==null){
            Context app=editorContext!=null?editorContext:(Context)context.invoke(null);
            File folder=new File(app.getCodeCacheDir(),"aurora-watermark217");
            if(!folder.isDirectory()&&!folder.mkdirs())throw new java.io.IOException("Cannot create watermark font cache");
            File target=new File(folder,"MiSansLatinVF-217.ttf"),temporary=new File(folder,"MiSansLatinVF-217.tmp");
            // Recreate once per process from the selected module APK, never a camera asset fallback.
            try(ZipFile apk=new ZipFile(module);InputStream in=apk.getInputStream(apk.getEntry("assets/prometheus_runtime/MiSans Latin VF.ttf"));FileOutputStream out=new FileOutputStream(temporary)){
                byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);out.getFD().sync();
            }
            java.nio.file.Files.move(temporary.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            font=target;
        }
        return new Typeface.Builder(font);
    }
}
