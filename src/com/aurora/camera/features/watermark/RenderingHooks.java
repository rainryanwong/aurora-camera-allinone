package com.aurora.camera.features.watermark;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;
import de.robv.android.xposed.*;

/** Exact v16 mapping of Phoenix watermark consumers, not a global device spoof. */
final class RenderingHooks {
    private static final String TAG="AuroraPhoenixWm";
    private static final ThreadLocal<Render> activeRender=new ThreadLocal<>();
    private static final class Render {
        final String name;
        final Object owner;
        boolean composed;
        Render(String name,Object owner){this.name=name;this.owner=owner;}
    }
    private static final LruCache<String,Bitmap> brandAssets=new LruCache<String,Bitmap>(8*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
    };
    private static final Map<Object,ReentrantLock> rendererLocks=Collections.synchronizedMap(new WeakHashMap<>());
    private static ReentrantLock rendererLock(Object owner){synchronized(rendererLocks){ReentrantLock lock=rendererLocks.get(owner);if(lock==null){lock=new ReentrantLock();rendererLocks.put(owner,lock);}return lock;}}
    private static Context context(ClassLoader loader) throws Exception {
        return (Context)Class.forName("ud.A",false,loader).getDeclaredField("b").get(null);
    }
    private static String selected(ClassLoader loader) throws Exception {
        Context c=context(loader);
        if(c==null)return "";
        String value=c.getSharedPreferences("aurora_watermark_names",0).getString("selected","");
        return "工程型号".equals(value)?android.os.Build.MODEL:value;
    }
    private static boolean leica(String n) {return n.toLowerCase(Locale.ROOT).endsWith(" by leica");}
    private static boolean leitz(String n) {return n.equalsIgnoreCase("leitzphone powered by xiaomi");}
    private static Field field(Class<?> c,String n) throws Exception {
        for(Class<?> k=c;k!=null;k=k.getSuperclass()) {
            try {Field f=k.getDeclaredField(n);f.setAccessible(true);return f;}
            catch(NoSuchFieldException e) {}
        }
        throw new NoSuchFieldException(n);
    }
    static void install(final ClassLoader loader) {
        // Resolve the complete dependency group before installing any hook.
        try {
            Method product=Class.forName("Du.a$a",false,loader).getDeclaredMethod("a",String.class,boolean.class,boolean.class);
            Class<?> brand=Class.forName("zu.a",false,loader);
            Field series=field(brand,"a"),suffix=field(brand,"b");
            if(product.getReturnType()!=brand || series.getType()!=String.class || suffix.getType()!=String.class)
                throw new NoSuchMethodException("product contract");
            Class<?> model=Class.forName("vs.m",false,loader);
            Method text=model.getDeclaredMethod("p",String.class,String.class,boolean.class,boolean.class);
            Field content=field(model,"p");
            Method asset=Class.forName("xs.a",false,loader).getDeclaredMethod("a",Context.class,String.class);
            if(asset.getReturnType()!=Bitmap.class || text.getReturnType()!=void.class || content.getType()!=String.class)
                throw new NoSuchMethodException("dynamic text/asset contract");
            Class<?> renderer=Class.forName("com.xiaomi.cam.watermark.b",false,loader);
            Field rendererModel=field(renderer,"c");
            if(rendererModel.getType()!=String.class)throw new NoSuchFieldException("renderer model contract");
            Method e=renderer.getDeclaredMethod("e",Context.class,Class.forName("vs.g",false,loader),Bitmap.class,
                Class.forName("qs.b",false,loader),Class.forName("ss.e",false,loader),String.class,float.class,
                String.class,boolean.class,android.graphics.PorterDuff.Mode.class,Class.forName("Gg.Z",false,loader));
            Method f=renderer.getDeclaredMethod("f",Context.class,Bitmap.class,Class.forName("qs.b",false,loader),
                Class.forName("ss.e",false,loader),Class.forName("ws.a",false,loader),String.class,float.class,
                Class.forName("yu.e",false,loader),String.class,boolean.class,android.graphics.PorterDuff.Mode.class,
                Class.forName("Gg.Z",false,loader),int.class);
            final Method compose=Class.forName("com.aurora.camera.features.watermark.OnlineLeicaRenderer")
                .getMethod("compose",Context.class,Bitmap.class,String.class,String.class);
            final Class<?> viewNode=Class.forName("vs.a",false,loader);
            final Method nodeId=viewNode.getDeclaredMethod("i");
            if(nodeId.getReturnType()!=String.class)throw new NoSuchMethodException("Camera node identity contract");
            XposedBridge.hookMethod(product,new XC_MethodHook() {
                protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if(p.hasThrowable() || p.getResult()==null)return;
                        String n=selected(loader);
                        // Ti is a separate native rect_params node, not a literal suffix in the main text.
                        if(!n.endsWith(" Ti") || !String.valueOf(p.args[0]).endsWith(" Ti"))return;
                        series.set(p.getResult(),n.substring(0,n.length()-3).trim());suffix.set(p.getResult(),"Ti");
                    } catch(Throwable err) {Log.e(TAG,"TI_ROUTE_FAILED; OEM retained",err);}
                }
            });
            XposedBridge.hookMethod(text,new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        String n=selected(loader);if(n.isEmpty())return;
                        // MiVi publication needs the complete model text before asset conversion.
                        p.args[1]=n;
                    } catch(Throwable err) {Log.e(TAG,"TEXT_ROUTE_FAILED; OEM retained",err);}
                }
            });
            XC_MethodHook gate=new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam p) {
                    ReentrantLock lock=rendererLock(p.thisObject);lock.lock();p.setObjectExtra("217.renderer.lock",lock);
                    try {
                        String n=selected(loader);if(n.isEmpty())return;
                        if(leica(n) && p.method.equals(f)) {
                            Object node=p.args[4];
                            String id=viewNode.isInstance(node)?String.valueOf(nodeId.invoke(node)):"";
                            if("model".equalsIgnoreCase(id)||"model_versionName".equalsIgnoreCase(id)) {
                                Render current=activeRender.get();
                                // The preceding logo node already carries this model text.
                                if(current!=null&&current.owner==p.thisObject&&current.name.equals(n)&&current.composed) {
                                    p.setResult(null);return;
                                }
                            }
                        }
                        // 6.8 J0 stores this field in lower case; its value forms a case-sensitive asset path.
                        String canonical=leica(n)?"17 ultra by leica":leitz(n)?"leitzphone powered by xiaomi":n.toLowerCase(Locale.ROOT);
                        p.setObjectExtra("aurora.model",rendererModel.get(p.thisObject));
                        p.setObjectExtra("aurora.previousRender",activeRender.get());
                        rendererModel.set(p.thisObject,canonical);
                        Render current=activeRender.get();
                        p.setObjectExtra("aurora.applied",Boolean.TRUE);
                        activeRender.set(current!=null&&current.owner==p.thisObject&&current.name.equals(n)?current:new Render(n,p.thisObject));
                    } catch(Throwable err) {Log.e(TAG,"BITMAP_GATE_FAILED; OEM retained",err);}
                }
                protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                    try {
                        if(!Boolean.TRUE.equals(p.getObjectExtra("aurora.applied")))return;
                        try {rendererModel.set(p.thisObject,p.getObjectExtra("aurora.model"));}
                        finally {Render previous=(Render)p.getObjectExtra("aurora.previousRender");if(previous==null)activeRender.remove();else activeRender.set(previous);}
                    } finally {ReentrantLock lock=(ReentrantLock)p.getObjectExtra("217.renderer.lock");if(lock!=null)lock.unlock();}
                }
            };
            XposedBridge.hookMethod(e,gate);XposedBridge.hookMethod(f,gate);
            XposedBridge.hookMethod(asset,new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam p) {
                    Render current=activeRender.get();
                    if(current==null||!leica(current.name))return;
                    String path=(String)p.args[1];
                    if(!path.startsWith("cloud_watermark_material/17_ultra_by_leica/"))return;
                    Bitmap cached=brandAssets.get(current.name+"\n"+path);
                    if(cached==null)return;
                    // OEM render nodes own their bitmap and may modify or recycle it.
                    p.setResult(cached.copy(Bitmap.Config.ARGB_8888,false));
                    p.setObjectExtra("217.brand.cached",Boolean.TRUE);
                    current.composed=true;
                }
                protected void afterHookedMethod(MethodHookParam p) {
                    if(Boolean.TRUE.equals(p.getObjectExtra("217.brand.cached")))return;
                    Render current=activeRender.get();
                    if(current==null || !leica(current.name) || p.hasThrowable() || !(p.getResult() instanceof Bitmap))return;
                    String n=current.name;
                    String path=String.valueOf(p.args[1]);
                    if(!path.startsWith("cloud_watermark_material/17_ultra_by_leica/"))return;
                    try {
                        Bitmap source=(Bitmap)p.getResult();
                        String base=n.substring(0,n.length()-9).trim();
                        Bitmap result=(Bitmap)compose.invoke(null,p.args[0],source,base,path.substring(path.lastIndexOf('/')+1));
                        if(result==null || result.isRecycled())throw new IllegalStateException("invalid rendered asset");
                        brandAssets.put(n+"\n"+path,result);
                        p.setResult(result.copy(Bitmap.Config.ARGB_8888,false));
                        current.composed=true;
                        // xs.a.a decoded this source for this call; its replacement is now owned by OEM.
                        if(source!=result)source.recycle();
                    } catch(Throwable err) {Log.e(TAG,"DYNAMIC_COMPOSE_FAILED; native bitmap retained",err);}
                }
            });
            Log.i(TAG,"RENDER_HOOK_READY product/Ti, model text, scoped bitmap composition; v16 contracts mapped");
        } catch(Throwable err) {throw new IllegalStateException("Required RenderingHooks could not be installed",err);}
    }
}
