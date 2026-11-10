package com.aurora.camera.compat.watermark;

import android.content.Context;
import com.android.camera.port.AuroraWatermarkName;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XposedBridge;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipFile;

/** Replace only a template's native layout, then reapply its live user config. */
public final class WatermarkVariants219 {
    private final Method context,logo,model,parser,translator,applyUser,mix,preview,publish,read,parse;
    private final Field identity,root,itemLazy,lazyValue,lazyMonitor,userApplied,parseOwner,parserPath,layout,counters;
    private final Field[] state;
    private final Constructor<?> parserConstructor;
    private final Map<Object,String> loaded=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object,ReentrantLock> locks=Collections.synchronizedMap(new WeakHashMap<>());
    private final ThreadLocal<Object> parsing=new ThreadLocal<>();
    private final ThreadLocal<Boolean> rebuilding=new ThreadLocal<>();
    private final Host host;
    private volatile WatermarkVariantPolicy219 policy;
    public WatermarkVariants219(Host h)throws ReflectiveOperationException {
        host=h;Class<?> item=h.type("com.xiaomi.cam.watermark.a"),config=h.type("ps.a");
        context=h.method("com.xiaomi.camera.basic.Global","getContext");identity=h.field("Pe.b$b","a");logo=h.method("Pe.b","z");model=h.method("Pe.b","A");
        parser=h.method(item,"q");translator=h.method(item,"L");root=h.field(item,"a");itemLazy=h.field(item,"f");userApplied=h.field(item,"j");
        lazyValue=h.field("gv.n","b");lazyMonitor=h.field("gv.n","c");
        parserConstructor=config.getDeclaredConstructor(Path.class,String.class,h.type("Gg.W"));parserConstructor.setAccessible(true);
        applyUser=h.method(item,"l0",item,Context.class);mix=h.method(item,"J0",String.class,String.class,boolean.class);
        preview=h.method(item,"c",android.app.Application.class,android.graphics.Bitmap.class,h.type("qs.b"),int.class);publish=h.method(item,"n0");
        parse=h.method("ps.a$a","invoke");parseOwner=h.field("ps.a$a","a");parserPath=h.field(config,"b");
        read=h.method("rv.j","x",File.class,Charset.class);layout=h.field(config,"j");counters=h.field(config,"l");
        state=new Field[]{h.field(config,"n"),h.field(config,"o"),h.field(config,"p"),h.field(config,"q")};
        if(itemLazy.getType()!=h.type("gv.n")||lazyValue.getType()!=Object.class||userApplied.getType()!=boolean.class||parse.getReturnType()!=Object.class)throw new NoSuchFieldException("Watermark variant host contract");
    }
    private WatermarkVariantPolicy219 policy()throws Exception {
        if(policy==null){try(ZipFile zip=new ZipFile(host.moduleApkPath);InputStream in=zip.getInputStream(zip.getEntry("assets/watermark-variants219.json"))){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] bytes=new byte[8192];for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);policy=new WatermarkVariantPolicy219(new String(out.toByteArray(),StandardCharsets.UTF_8));}}
        return policy;
    }
    private Context context()throws Exception{return (Context)context.invoke(null);}
    private String folder(String configPath)throws Exception {
        Path files=context().getFilesDir().toPath().toAbsolutePath().normalize(),file=new File(configPath).toPath().toAbsolutePath().normalize();
        if(!file.startsWith(files)||!"config.json".equals(file.getFileName().toString()))return null;
        String relative=files.relativize(file.getParent()).toString().replace(File.separatorChar,'/');
        return relative.startsWith("watermarks/")||relative.startsWith("video_watermarks/")?relative:null;
    }
    private String choice(String folder)throws Exception {
        Context c=context();String selected=c.getSharedPreferences("aurora_watermark_names",0).getString("selected","").trim();
        Object value=identity.get(null);return policy().choice(folder,(String)logo.invoke(value),AuroraWatermarkName.apply((String)model.invoke(value)),!selected.isEmpty());
    }
    private ReentrantLock lock(Object item){synchronized(locks){return locks.computeIfAbsent(item,k->new ReentrantLock());}}
    private void begin(MethodHookParam p)throws Exception {
        ReentrantLock guard=lock(p.thisObject);guard.lock();p.setObjectExtra("219.variant.lock",guard);
        ensure(p.thisObject);
    }
    private void end(MethodHookParam p){ReentrantLock guard=(ReentrantLock)p.getObjectExtra("219.variant.lock");if(guard!=null)guard.unlock();}
    private void ensure(Object item)throws Exception {
        if(Boolean.TRUE.equals(rebuilding.get()))return;
        Object before=parser.invoke(item);String used=loaded.get(before);if(used==null)return;
        String folder=folder((String)parserPath.get(before));if(folder==null||used.equals(choice(folder)))return;
        Object lazy=itemLazy.get(item);boolean applied=userApplied.getBoolean(item);String position=(String)layout.get(before);
        Object next=parserConstructor.newInstance(root.get(item),parserPath.get(before),translator.invoke(item));
        for(Field field:state)field.set(next,field.get(before));
        @SuppressWarnings("unchecked") Map<Object,Object> oldCounters=(Map<Object,Object>)counters.get(before);
        @SuppressWarnings("unchecked") Map<Object,Object> nextCounters=(Map<Object,Object>)counters.get(next);nextCounters.putAll(oldCounters);
        synchronized(lazyMonitor.get(lazy)){
            rebuilding.set(true);lazyValue.set(lazy,next);userApplied.setBoolean(item,false);
            try{
                applyUser.invoke(null,item,context());
                // Both OEM variants were checked to expose the same layout keys.
                layout.set(next,position);
            }catch(Exception failure){lazyValue.set(lazy,before);userApplied.setBoolean(item,applied);throw failure;}
            finally{rebuilding.remove();}
        }
    }
    public void install(){
        XposedBridge.hookMethod(parse,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{p.setObjectExtra("219.parser.previous",parsing.get());parsing.set(parseOwner.get(p.thisObject));}
            @Override protected void after(MethodHookParam p){Object previous=p.getObjectExtra("219.parser.previous");if(previous==null)parsing.remove();else parsing.set(previous);}
        });
        XposedBridge.hookMethod(read,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            Object owner=parsing.get();if(owner==null||p.hasThrowable()||!(p.getResult() instanceof String))return;
            String path=(String)parserPath.get(owner);if(!new File(path).equals(p.args[0]))return;
            String folder=folder(path);if(folder==null)return;String selected=choice(folder);
            p.setResult(policy().rewrite(folder,(String)p.getResult(),selected));loaded.put(owner,selected);
        }});
        XposedBridge.hookMethod(mix,new StrictHook(100){
            @Override protected void before(MethodHookParam p)throws Throwable{p.args[1]=AuroraWatermarkName.apply((String)p.args[1]);begin(p);}
            @Override protected void after(MethodHookParam p){end(p);}
        });
        for(Method method:new Method[]{preview,publish})XposedBridge.hookMethod(method,new StrictHook(100){
            @Override protected void before(MethodHookParam p)throws Throwable{begin(p);}
            @Override protected void after(MethodHookParam p){end(p);}
        });
    }
}
