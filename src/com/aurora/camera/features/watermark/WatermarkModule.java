package com.aurora.camera.features.watermark;

import android.content.Context;
import android.util.Log;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Camera 6.8 v16 only. No camera identity, filter, gallery or system hooks. */
public final class WatermarkModule {
    private static final String TAG="AuroraPhoenixWm";
    private static final java.util.concurrent.locks.ReentrantLock LOCK=new java.util.concurrent.locks.ReentrantLock();
    private static final ThreadLocal<Boolean> publishing=new ThreadLocal<>();
    private static final ThreadLocal<Transaction> transaction=new ThreadLocal<>();
    private static final class Transaction {
        Path source,target; byte[] original; boolean accepted;
        Transaction(Path s,Path t,byte[] b) { source=s;target=t;original=b; }
    }
    private static final Pattern ASSET=Pattern.compile("prometheus_(?:leica_suffix|leitz_full)_[A-Za-z0-9_.-]+\\.webp");
    private static void log(String s) { Log.i(TAG,s); XposedBridge.log(TAG+": "+s); }
    public static void validate(com.aurora.camera.compat.Host h)throws ReflectiveOperationException {
        h.method("on.a","f",String.class,String.class,String.class,String.class);h.method("Gg.r","d",Path.class,Path.class);h.field("Gg.r","a");h.field("ud.A","b");
        h.method("vf.b","U",String.class,String.class);h.method("vf.b","f",String.class);h.method("l7.d","d",h.type("vf.b"));h.method("l7.d$a","d");h.field("l7.d$a","b");
        h.method("Du.a$a","a",String.class,boolean.class,boolean.class);h.field("zu.a","a");h.field("zu.a","b");h.method("vs.m","p",String.class,String.class,boolean.class,boolean.class);h.field("vs.o","p");h.method("vs.a","i");
        h.method("xs.a","a",Context.class,String.class);h.field("com.xiaomi.cam.watermark.b","c");
        h.method("com.xiaomi.cam.watermark.b","e",Context.class,h.type("vs.g"),android.graphics.Bitmap.class,h.type("qs.b"),h.type("ss.e"),String.class,float.class,String.class,boolean.class,android.graphics.PorterDuff.Mode.class,h.type("Gg.Z"));
        h.method("com.xiaomi.cam.watermark.b","f",Context.class,android.graphics.Bitmap.class,h.type("qs.b"),h.type("ss.e"),h.type("ws.a"),String.class,float.class,h.type("yu.e"),String.class,boolean.class,android.graphics.PorterDuff.Mode.class,h.type("Gg.Z"),int.class);
    }
    public static void install(com.aurora.camera.compat.Host host) {
        MetadataHooks.install(host.loader);
        RenderingHooks.install(host.loader);
        try {
            final Method publish=Class.forName("on.a",false,host.loader).getDeclaredMethod("f",String.class,String.class,String.class,String.class);
            final Method entry=Class.forName("Gg.r",false,host.loader).getDeclaredMethod("d",Path.class,Path.class);
            if (publish.getReturnType()!=boolean.class || entry.getReturnType()!=void.class ||
                !Modifier.isStatic(publish.getModifiers()) || !Modifier.isStatic(entry.getModifiers())) throw new NoSuchMethodException("publisher signature");
            publish.setAccessible(true); entry.setAccessible(true);
            XposedBridge.hookMethod(entry,new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam p) {
                    if (Boolean.TRUE.equals(publishing.get())) return;
                    Path candidate=(Path)p.args[0];if(candidate==null||candidate.getFileName()==null||!candidate.getFileName().toString().matches("mivi_\\d+\\.json"))return;
                    try {
                        Context context=(Context)Class.forName("ud.A",false,host.loader).getDeclaredField("b").get(null);
                        String selected=context.getSharedPreferences("aurora_watermark_names",0).getString("selected","");
                        if(!selected.toLowerCase(java.util.Locale.ROOT).endsWith(" by leica")&&!selected.equalsIgnoreCase("leitzphone powered by xiaomi"))return;
                        if(!Class.forName("Gg.r",false,host.loader).getDeclaredField("a").getBoolean(null)) {
                            log("SKIP native publication disabled"); return;
                        }
                    } catch(Throwable e) { Log.e(TAG,"Publisher gate unavailable",e); return; }
                    LOCK.lock();p.setObjectExtra("217.watermark.lock",Boolean.TRUE);
                    transform(host,publish,(Path)p.args[0],(Path)p.args[1]);
                }
                protected void afterHookedMethod(MethodHookParam p) {
                    if(!Boolean.TRUE.equals(p.getObjectExtra("217.watermark.lock")))return;
                    Transaction t=transaction.get(); transaction.remove();
                    try{
                    if(t==null || t.accepted) return;
                    try {
                        Files.write(t.source,t.original); publishing.set(true);
                        boolean ok=(Boolean)publish.invoke(null,t.source.getParent().toString(),t.source.getFileName().toString(),
                            t.target.getParent().toString(),t.target.getFileName().toString());
                        log("FALLBACK JSON publication failed; original restored; original_publish_accepted="+ok);
                    } catch(Throwable e) { Log.e(TAG,"PUBLICATION_ROLLBACK_FAILED",e); }
                    finally { publishing.remove(); }
                    }finally{LOCK.unlock();}
                }
            });
            XposedBridge.hookMethod(publish,new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    String name=String.valueOf(p.args[1]);
                    if (!Boolean.TRUE.equals(publishing.get()) && name.matches("mivi_\\d+\\.json"))
                        log("JSON_PUBLISH name="+name+" accepted="+(!p.hasThrowable() && Boolean.TRUE.equals(p.getResult())));
                    Transaction t=transaction.get();
                    if(t!=null && !Boolean.TRUE.equals(publishing.get()) && t.source.getFileName().toString().equals(name))
                        t.accepted=!p.hasThrowable() && Boolean.TRUE.equals(p.getResult());
                }
            });
            log("HOOK_READY Gg.r.d(Path,Path) and on.a.f(String,String,String,String); runtime transform pending");
        } catch(Throwable e) { throw new IllegalStateException("Required watermark publisher hook could not be installed",e); }
    }
    private static void supplySuffixAssets(String moduleApkPath,Path folder) throws java.io.IOException {
        String prefix="assets/prometheus/watermark-suffix/";
        try(java.util.zip.ZipFile apk=new java.util.zip.ZipFile(moduleApkPath)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=apk.entries();
            while(entries.hasMoreElements()) {
                java.util.zip.ZipEntry entry=entries.nextElement();
                if(!entry.getName().startsWith(prefix))continue;
                String name=entry.getName().substring(prefix.length());
                if(!name.matches("prometheus_leica_suffix_[A-Za-z0-9_.-]+\\.webp"))continue;
                Path target=folder.resolve(name);
                if(Files.isRegularFile(target) && Files.size(target)==entry.getSize())continue;
                try(java.io.InputStream input=apk.getInputStream(entry)) {
                    Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
    private static void transform(com.aurora.camera.compat.Host host,Method publish,Path source,Path target) {
        ClassLoader loader=host.loader;
        byte[] original=null;
        try {
            Context context=(Context)Class.forName("ud.A",false,loader).getDeclaredField("b").get(null);
            if(context==null || source==null || target==null || source.getFileName()==null || !source.getFileName().toString().matches("mivi_\\d+\\.json")) return;
            String name=context.getSharedPreferences("aurora_watermark_names",0).getString("selected","");
            if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(" by leica") && !name.equalsIgnoreCase("leitzphone powered by xiaomi")) return;
            Path root=context.getFilesDir().toPath().toRealPath();
            Path actual=source.toRealPath();
            if (!actual.startsWith(root) || !actual.toString().contains("watermark")) { log("SKIP source outside watermark files"); return; }
            original=Files.readAllBytes(actual);
            Path folder=actual.getParent();
            log("TRANSFORM_BEGIN name="+name+" template="+actual.getFileName());
            supplySuffixAssets(host.moduleApkPath,folder);
            boolean changed=(Boolean)Class.forName("com.aurora.camera.features.watermark.WatermarkAssetRouter")
                .getMethod("postProcessMivi",Context.class,Path.class,Path.class).invoke(null,context,actual,folder);
            if(!changed) throw new IllegalStateException("router did not transform any model nodes");
            String json=new String(Files.readAllBytes(actual),StandardCharsets.UTF_8);
            new org.json.JSONObject(json);
            Set<String> assets=new LinkedHashSet<>(); Matcher m=ASSET.matcher(json); while(m.find()) assets.add(m.group());
            if(assets.isEmpty()) throw new IllegalStateException("no generated assets referenced");
            publishing.set(true);
            for(String file:assets) {
                Path asset=folder.resolve(file).toRealPath();
                if(!asset.getParent().equals(folder.toRealPath()) || Files.size(asset)==0) throw new IllegalStateException("invalid generated asset");
                boolean ok=(Boolean)publish.invoke(null,folder.toString(),file,target.getParent().toString(),file);
                log("ASSET_PUBLISH file="+file+" accepted="+ok);
                if(!ok) throw new IllegalStateException("native resource publication refused");
            }
            log("TRANSFORM_READY assets="+assets.size()+"; OEM JSON publication follows; capture acceptance pending");
            transaction.set(new Transaction(actual,target,original));
        } catch(Throwable e) {
            if(original!=null) try { Files.write(source,original); log("FALLBACK original JSON restored"); }
            catch(Throwable restore) { Log.e(TAG,"RESTORE_FAILED",restore); }
            Log.e(TAG,"TRANSFORM_FAILED; original publication retained when restoration succeeds",e);
        } finally { publishing.remove(); }
    }
}
