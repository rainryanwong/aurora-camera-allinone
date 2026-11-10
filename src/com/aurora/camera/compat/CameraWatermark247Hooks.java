package com.aurora.camera.compat;

import android.content.Context;
import android.util.Log;
import com.aurora.camera.compat.editor.Editor245Assets;
import com.aurora.camera.compat.editor.Editor245TemplateData;
import com.aurora.camera.compat.watermark.CameraLeica248;
import de.robv.android.xposed.XposedBridge;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;

/** Publish the same seven verified templates to Camera's own native photo-watermark parser. */
public final class CameraWatermark247Hooks implements HookPlan {
    private static final String TAG="AuroraCameraWm247", GUARD="247.camera.watermark.guard", ROOT="247.camera.watermark.root";
    private final Host host;
    private final Method context, parse, support;
    private final Field groupPath, groupItems, itemPath;
    private final Class<?> photoManager;
    private final CameraLeica248 leica;
    private volatile Set<String> available=Collections.emptySet();
    private String lastParsed;

    private CameraWatermark247Hooks(Host host) throws ReflectiveOperationException {
        this.host=host;
        context=host.method("com.xiaomi.camera.basic.Global","getContext");
        parse=host.method("Gg.r","g",File.class,ArrayList.class,boolean.class);
        support=host.method("Gg.O","f"); photoManager=host.type("Gg.T");
        groupPath=host.field("Gg.G","a"); groupItems=host.field("Gg.G","b");
        itemPath=host.field("com.xiaomi.cam.watermark.a","a");
        leica=new CameraLeica248(host);
        if (parse.getReturnType()!=ArrayList.class || support.getReturnType()!=List.class
                || groupPath.getType()!=Path.class || groupItems.getType()!=ArrayList.class
                || itemPath.getType()!=Path.class || !host.type("Gg.O").isAssignableFrom(photoManager))
            throw new NoSuchMethodException("CameraWatermark247 pinned photo watermark ABI differs");
    }
    public static HookPlan resolve(Host host) throws ReflectiveOperationException { return new CameraWatermark247Hooks(host); }
    @Override public String id() { return "247.camera-color-signature-watermarks"; }

    private File ownRoot(File input) throws Exception {
        if (input==null || !input.isDirectory() || !new File(input,"assets_copied").isFile()) return null;
        Context app=(Context)context.invoke(null);
        if (app==null) throw new IllegalStateException("Camera watermark context is missing");
        File actual=input.getCanonicalFile();
        if (actual.equals(new File(app.getFilesDir(),"watermarks").getCanonicalFile())) return actual;
        File external=app.getExternalFilesDir(null);
        return external!=null && actual.equals(new File(external,"watermarks").getCanonicalFile()) ? actual : null;
    }

    @Override public void install() {
        XposedBridge.hookMethod(parse,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                File root=ownRoot((File)p.args[0]);
                if (root==null) return;
                p.setObjectExtra(ROOT,root);
                // Native m()/w() reaches this parser only after its own copy/init waits.
                // Keep our publication and that parse together; do not alter native copying or readiness.
                Editor245Assets.Guard guard=Editor245Assets.lock(root.getParentFile());
                p.setObjectExtra(GUARD,guard); available=Collections.emptySet();
                Editor245Assets.Publication result=Editor245Assets.publishLocked(root.getParentFile(),host.moduleApkPath);
                available=Collections.unmodifiableSet(new LinkedHashSet<>(result.groups));
                Log.i(TAG,"verified native photo-watermark resources; created="+result.created+"; groups="+available);
                if (!result.conflicts.isEmpty()) Log.e(TAG,"Existing camera watermark conflicts preserved; excluded groups="+result.conflicts);
                leica.prepare(root);
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                try {
                    if (p.getObjectExtra(ROOT)!=null && !p.hasThrowable()) report((List<?>)p.getResult());
                } finally {
                    Editor245Assets.Guard guard=(Editor245Assets.Guard)p.getObjectExtra(GUARD);
                    if (guard!=null) guard.close();
                }
            }
        });
        XposedBridge.hookMethod(support,new StrictHook() {
            @Override protected void after(MethodHookParam p) {
                if (p.hasThrowable() || !photoManager.isInstance(p.thisObject)) return;
                List<?> original=(List<?>)p.getResult();
                // Empty is the OEM's unrestricted state. Turning it into seven IDs would remove stock templates.
                if (original==null || original.isEmpty() || (available.isEmpty()&&leica.preparedIds().isEmpty())) return;
                LinkedHashSet<Object> ids=new LinkedHashSet<>(original);
                for (Editor245TemplateData.Group group:Editor245TemplateData.GROUPS) if (available.contains(group.id))
                    for (Editor245TemplateData.Template template:group.entries) ids.add(template.id);
                ids.addAll(leica.preparedIds());
                p.setResult(new ArrayList<>(ids));
            }
        });
        leica.install();
    }

    private synchronized void report(List<?> groups) throws IllegalAccessException {
        Map<String,List<String>> found=new LinkedHashMap<>();
        for (Object group:groups) {
            String name=((Path)groupPath.get(group)).getFileName().toString();
            if (!available.contains(name)) continue;
            List<String> ids=new ArrayList<>();
            for (Object item:(List<?>)groupItems.get(group)) ids.add(((Path)itemPath.get(item)).getFileName().toString());
            found.put(name,ids);
        }
        String value=found.toString();
        if (!value.equals(lastParsed)) {
            lastParsed=value;
            Log.i(TAG,"native parsed local photo-watermarks="+value+"; original selection/preview/Mivi save pipeline retained");
        }
    }
}
