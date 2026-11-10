package com.aurora.camera.compat.watermark;

import android.content.Context;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.compat.editor.Editor245Assets;
import de.robv.android.xposed.XposedBridge;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Restore exact stock Leica assets; add only two missing templates, with scoped product eligibility. */
public final class CameraLeica248 {
    private static final String TAG="AuroraCameraLeica248";
    private static final String MANIFEST="6ed57b2bf9c954e42d486a0cc5f0b00ee3e8e8667e7bd40ff387b7d395cf33ef";
    private static final Set<String> NATIVE=ids("2","104","111","88","89","90","91","92");
    private static final Set<String> DONOR=ids("93","94");
    private static final Set<String> THEME=ids("111","88","89","90","91","92");
    private final Host host;
    private final Method context,filter,theme,device;
    private final Field itemPath;
    private final Class<?> photoManager;
    private final ThreadLocal<Ready> filtering=new ThreadLocal<>();
    private final Set<String> logged=Collections.synchronizedSet(new HashSet<>());
    private volatile Ready ready=new Ready(null,Collections.emptySet());
    private static final class Ready {
        final Path root;final Set<String> ids;
        Ready(Path root,Set<String> ids){this.root=root;this.ids=Collections.unmodifiableSet(new LinkedHashSet<>(ids));}
    }
    private static final class Row {
        final String source,relative,id,hash;byte[] bytes;
        Row(String source,String relative,String hash){this.source=source;this.relative=relative;this.hash=hash;this.id=relative.split("/")[1];}
    }
    private static Set<String> ids(String...values){return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(values)));}
    public CameraLeica248(Host h) throws ReflectiveOperationException {
        host=h;context=h.method("com.xiaomi.camera.basic.Global","getContext");
        photoManager=h.type("Gg.T");filter=h.method("Gg.O","d",boolean.class);
        theme=h.method("Gg.B","invoke",Object.class);device=h.method("Gg.x","invoke",Object.class);
        itemPath=h.field("com.xiaomi.cam.watermark.a","a");
        if(filter.getReturnType()!=void.class||theme.getReturnType()!=Object.class||device.getReturnType()!=Object.class||itemPath.getType()!=Path.class)
            throw new NoSuchMethodException("CameraLeica248 pinned filter ABI differs");
    }
    public Set<String> preparedIds(){return ready.ids;}
    private static byte[] bytes(InputStream input) throws IOException {
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);return out.toByteArray();
        }
    }
    /** The caller holds the existing camera publication guard through this operation and native parsing. */
    public void prepare(File directory) throws Exception {
        ready=new Ready(null,Collections.emptySet());
        Context app=(Context)context.invoke(null);
        if(app==null||app.getApplicationInfo()==null||app.getApplicationInfo().sourceDir==null)throw new IOException("Camera source APK is unavailable");
        Path root=directory.getCanonicalFile().toPath();
        Map<String,List<Row>> templates=new LinkedHashMap<>();Set<String> names=new HashSet<>();int nativeCount=0,donorCount=0;
        try(ZipFile module=new ZipFile(host.moduleApkPath);ZipFile stock=new ZipFile(app.getApplicationInfo().sourceDir)) {
            ZipEntry manifest=module.getEntry("assets/camerawm248/leica248.tsv");
            if(manifest==null)throw new IOException("Camera Leica manifest is missing");
            byte[] encoded=bytes(module.getInputStream(manifest));
            if(!MANIFEST.equals(Editor245Assets.sha(encoded)))throw new IOException("Camera Leica manifest identity differs");
            for(String line:new String(encoded,StandardCharsets.UTF_8).split("\n")) {
                String[] columns=line.split("\t",-1);
                if(columns.length!=3||!columns[1].matches("leica/(2|104|111|88|89|90|91|92|93|94)/[A-Za-z0-9_.-]+")||!columns[2].matches("[a-f0-9]{64}")||!names.add(columns[1]))
                    throw new IOException("Invalid camera Leica manifest row");
                Row row=new Row(columns[0],columns[1],columns[2]);
                if(row.source.equals("native")&&NATIVE.contains(row.id))nativeCount++;
                else if(row.source.equals("donor")&&DONOR.contains(row.id))donorCount++;
                else throw new IOException("Unexpected camera Leica source");
                templates.computeIfAbsent(row.id,k->new ArrayList<>()).add(row);
            }
            if(nativeCount!=163||donorCount!=22||templates.size()!=10)throw new IOException("Incomplete camera Leica publication manifest");
            Set<String> available=new LinkedHashSet<>();List<String> conflicts=new ArrayList<>();int created=0;
            for(Map.Entry<String,List<Row>> template:templates.entrySet()) {
                boolean conflict=false;
                for(Row row:template.getValue()) {
                    File target=new File(directory,row.relative).getCanonicalFile();
                    if(!target.toPath().startsWith(root))throw new IOException("Camera Leica destination escapes native root");
                    if(target.exists()&&(!target.isFile()||!row.hash.equals(Editor245Assets.sha(Files.readAllBytes(target.toPath()))))) {conflicts.add(row.relative);conflict=true;}
                }
                if(!conflict)available.add(template.getKey());
            }
            // Keep the native parse guard, but load only missing resources from
            // templates whose existing files matched. Validate before writing.
            for(String id:available)for(Row row:templates.get(id)) {
                if(Files.exists(new File(directory,row.relative).toPath()))continue;
                ZipFile zip=row.source.equals("native")?stock:module;
                String path=(row.source.equals("native")?"assets/watermarks/":"assets/camerawm248/")+row.relative;
                ZipEntry entry=zip.getEntry(path);if(entry==null)throw new IOException("Missing camera Leica source: "+path);
                row.bytes=bytes(zip.getInputStream(entry));
                if(!row.hash.equals(Editor245Assets.sha(row.bytes)))throw new IOException("Camera Leica source identity differs: "+row.relative);
            }
            for(String id:available)for(Row row:templates.get(id)) {
                if(row.bytes!=null) {
                    Path target=new File(directory,row.relative).toPath();
                    Files.createDirectories(target.getParent());Path temporary=Files.createTempFile(target.getParent(),".leica248-",".tmp");
                    try {
                        try(FileOutputStream stream=new FileOutputStream(temporary.toFile())){stream.write(row.bytes);stream.getFD().sync();}
                        Files.move(temporary,target);created++;
                    } finally {Files.deleteIfExists(temporary);}
                    if(!row.hash.equals(Editor245Assets.sha(Files.readAllBytes(target))))throw new IOException("Camera Leica publication verification failed: "+row.relative);
                }
            }
            ready=new Ready(root,available);
            Log.i(TAG,"verified Leica resources; readyIds="+available+"; created="+created+"; stockFiles=163; donorFiles=22");
            if(!conflicts.isEmpty())Log.e(TAG,"Existing Leica template conflicts preserved; excluded templates: "+conflicts);
        }
    }
    private String eligible(Object item,Set<String> allowed) throws Exception {
        Ready current=filtering.get();if(current==null||current.root==null)return null;
        Path path=((Path)itemPath.get(item)).toFile().getCanonicalFile().toPath();String id=path.getFileName().toString();
        return current.ids.contains(id)&&allowed.contains(id)&&path.equals(current.root.resolve("leica").resolve(id))?id:null;
    }
    public void install() {
        XposedBridge.hookMethod(filter,new StrictHook(){
            @Override protected void before(MethodHookParam p){if(photoManager.isInstance(p.thisObject)){p.setObjectExtra("248.leica.previous",filtering.get());filtering.set(ready);}}
            @Override protected void after(MethodHookParam p){if(photoManager.isInstance(p.thisObject)){Ready previous=(Ready)p.getObjectExtra("248.leica.previous");if(previous==null)filtering.remove();else filtering.set(previous);}}
        });
        XposedBridge.hookMethod(theme,new StrictHook(){
            @Override protected void after(MethodHookParam p) throws Throwable {
                if(p.hasThrowable()||!Boolean.TRUE.equals(p.getResult()))return;
                String id=eligible(p.args[0],THEME);if(id==null)return;p.setResult(Boolean.FALSE);
                if(logged.add("theme:"+id))Log.i(TAG,"photo-only native Leica theme eligibility released; id="+id);
            }
        });
        XposedBridge.hookMethod(device,new StrictHook(){
            @Override protected void after(MethodHookParam p) throws Throwable {
                if(p.hasThrowable()||!Boolean.TRUE.equals(p.getResult()))return;
                String id=eligible(p.args[0],ids("2"));if(id==null)return;p.setResult(Boolean.FALSE);
                if(logged.add("device:"+id))Log.i(TAG,"photo-only native Leica device eligibility released; id="+id);
            }
        });
    }
}
