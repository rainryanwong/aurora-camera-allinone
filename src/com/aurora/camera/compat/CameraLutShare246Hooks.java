package com.aurora.camera.compat;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import com.aurora.camera.compat.filter.CameraLutCatalog246;
import com.aurora.camera.compat.filter.CameraFilterCatalog248;
import com.aurora.camera.compat.filter.LutSync246;
import com.aurora.camera.compat.filter.LutStore229;
import com.aurora.camera.compat.sharedlut.LutShare246;
import com.aurora.camera.compat.sharedlut.LutShare246Client;
import com.aurora.camera.compat.sharedlut.LutShare257Bootstrap;
import de.robv.android.xposed.XposedBridge;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;

/** One-way event-driven export of camera-owned imported PNGs to the module provider. */
public final class CameraLutShare246Hooks implements HookPlan {
    private static volatile CameraLutShare246Hooks instance;
    public static final String BOOTSTRAP="content://com.android.camera.settings.CameraSettingsSearchProvider/aurora_lutshare246";
    private static final String MODULE="com.aurora.camera.compat",TAG="AuroraLutShare246";
    private static final byte[] MODULE_CERT=certificate();
    private final Method applicationCreate,bootstrapQuery;
    private final String moduleApkPath;
    private volatile Context context;
    private SharedPreferences preferences,visibility;
    private final CameraFilterCatalog248 nativeCatalog;
    private final Map<String,FileObserver> observers=new LinkedHashMap<>();
    private final LutSync246 sync=new LutSync246(Executors.newSingleThreadExecutor(r->{
        Thread thread=new Thread(r,"AuroraLutShare246");thread.setDaemon(true);return thread;
    }),this::publish,(revision,error)->{
        Log.e(TAG,"Imported LUT publication failed; previous complete catalog is retained",error);
    });
    private final SharedPreferences.OnSharedPreferenceChangeListener changed=(prefs,key)->{
        if(prefs==visibility||CameraLutCatalog246.changedKey(key)||key!=null&&(key.startsWith("label:")||key.startsWith("effect:")))sync.request();
    };
    private CameraLutShare246Hooks(Host host)throws ReflectiveOperationException {
        moduleApkPath=host.moduleApkPath;
        applicationCreate=host.method("com.android.camera.CameraAppImpl","onCreate");
        bootstrapQuery=host.method("com.android.camera.settings.CameraSettingsSearchProvider","query",Uri.class,String[].class,String.class,String[].class,String.class);
        nativeCatalog=new CameraFilterCatalog248(host);
    }
    public static HookPlan resolve(Host host)throws ReflectiveOperationException{instance=new CameraLutShare246Hooks(host);return instance;}
    public static void requestSnapshot(){CameraLutShare246Hooks hooks=instance;if(hooks!=null)hooks.sync.request();}
    public String id(){return "246.camera-imported-lut-publication";}
    private synchronized void initialize(Context value){
        if(context!=null)return;
        context=value.getApplicationContext();
        preferences=context.getSharedPreferences("aurora_luts229",Context.MODE_PRIVATE);
        // Retain both the preferences and listener: SharedPreferences keeps listeners weakly.
        preferences.registerOnSharedPreferenceChangeListener(changed);
        visibility=context.getSharedPreferences("aurora_filter_workspace_v2",Context.MODE_PRIVATE);
        visibility.registerOnSharedPreferenceChangeListener(changed);
    }
    private void publish(long revision)throws Exception {
        Context owner=context;
        if(owner==null)throw new IllegalStateException("Camera LUT context is not initialized");
        refreshObservers(owner);
        List<LutShare246.ExportItem> items=nativeCatalog.snapshot(owner);
        List<LutShare246.Row> rows=new ArrayList<>();for(LutShare246.ExportItem item:items)rows.add(item.row);
        if(!sync.current(revision))return;
        if(fingerprint(rows).equals(fingerprint(LutShare246Client.read(owner).rows)))return;
        LutShare246Client.publish(owner,items);
        Log.i(TAG,"Published imported LUT catalog count="+items.size());
    }
    private static String fingerprint(List<LutShare246.Row> rows){
        StringBuilder value=new StringBuilder();
        for(LutShare246.Row row:rows)value.append(row.id.length()).append(':').append(row.id).append(row.name.length()).append(':').append(row.name).append(row.revision).append(':').append(row.order).append(':').append(row.effectIdentity()).append(';');
        return value.toString();
    }
    private void refreshObservers(Context owner)throws java.io.IOException{
        Set<String> wanted=new LinkedHashSet<>();String files=owner.getFilesDir().getCanonicalPath();wanted.add(files);File workspace=new File(owner.getFilesDir(),"Workspace");
        if(workspace.isDirectory())wanted.add(workspace.getCanonicalPath());
        for(String kind:new String[]{"MiCamStyleRear","MiCamStyleFront"}){
            File root=new File(workspace,kind);if(!root.isDirectory())continue;wanted.add(root.getCanonicalPath());File[] entries=root.listFiles();
            if(entries==null)throw new java.io.IOException("Cannot watch native recipe workspace "+kind);
            for(File entry:entries)if(entry.isDirectory()&&entry.getName().startsWith(kind+"_"))wanted.add(entry.getCanonicalPath());
        }
        Iterator<Map.Entry<String,FileObserver>> old=observers.entrySet().iterator();while(old.hasNext()){Map.Entry<String,FileObserver> entry=old.next();if(!wanted.contains(entry.getKey())){entry.getValue().stopWatching();old.remove();}}
        for(String path:wanted)if(!observers.containsKey(path)){
            FileObserver observer=new FileObserver(path,FileObserver.CREATE|FileObserver.DELETE|FileObserver.MOVED_FROM|FileObserver.MOVED_TO|FileObserver.CLOSE_WRITE|FileObserver.DELETE_SELF|FileObserver.MOVE_SELF){
                @Override public void onEvent(int event,String name){if(!path.equals(files)||"Workspace".equals(name))sync.request();}
            };observer.startWatching();observers.put(path,observer);
        }
    }
    private static byte[] certificate(){
        String hex="08e04cdba0d0374cee03318fb4e886fb6b4b4a0365d16ecc5be8fa617721c2dc";
        byte[] result=new byte[hex.length()/2];for(int i=0;i<result.length;i++)result[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);return result;
    }
    static void authorizeBootstrap(Context context,int uid)throws PackageManager.NameNotFoundException {
        PackageManager manager=context.getPackageManager();String[] packages=manager.getPackagesForUid(uid);
        if(packages==null||!Arrays.asList(packages).contains(MODULE)||manager.getApplicationInfo(MODULE,0).uid!=uid||
                !manager.hasSigningCertificate(MODULE,MODULE_CERT,PackageManager.CERT_INPUT_SHA256))
            throw new SecurityException("Imported LUT bootstrap caller is not the signed module UID");
    }
    public void install(){
        XposedBridge.hookMethod(applicationCreate,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable())return;
                Application application=(Application)p.thisObject;initialize(application);
                new LutStore229(application).installBundled(moduleApkPath);
                application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks(){
                    public void onActivityResumed(Activity activity){sync.request();}
                    public void onActivityCreated(Activity activity,Bundle state){}
                    public void onActivityStarted(Activity activity){}
                    public void onActivityPaused(Activity activity){}
                    public void onActivityStopped(Activity activity){}
                    public void onActivitySaveInstanceState(Activity activity,Bundle state){}
                    public void onActivityDestroyed(Activity activity){}
                });
                // Providers are created before Application.onCreate; pending bootstrap requests wait for this real event.
                LutShare257Bootstrap.connect(application,sync::ready);
            }
        });
        XposedBridge.hookMethod(bootstrapQuery,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                if(p.args[0]==null||!BOOTSTRAP.equals(p.args[0].toString()))return;
                ContentProvider provider=(ContentProvider)p.thisObject;Context owner=provider.getContext();
                if(owner==null)throw new IllegalStateException("Camera bootstrap provider has no context");
                authorizeBootstrap(owner,Binder.getCallingUid());
                if(p.args[1]!=null||p.args[2]!=null||p.args[3]!=null||p.args[4]!=null)
                    throw new IllegalArgumentException("Imported LUT bootstrap accepts only the fixed empty query");
                initialize(owner);sync.request();
                MatrixCursor result=new MatrixCursor(new String[]{"status"});result.addRow(new Object[]{"scheduled"});p.setResult(result);
            }
        });
    }
}
