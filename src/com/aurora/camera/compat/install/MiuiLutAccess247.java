package com.aurora.camera.compat.install;

import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AtomicFile;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One-shot installation configuration, never a service or a host-process Hook. */
public final class MiuiLutAccess247 {
    public static final String PACKAGE="com.aurora.camera.compat";
    private static final String AUTHORITY="com.lbe.security.miui.permmgr";
    private static final File BACKUP=new File("/data/adb/camera68_aurora_lsp/lutshare-autostart-policy.before");
    public interface Access { Snapshot read() throws Exception; void set(int action) throws Exception; long permission(); }
    public interface Transport { Bundle call(String method,Bundle extras) throws Exception; }
    /** Same external-provider route as this ROM's built-in root `content` command. */
    public static final class ExternalTransport implements Transport,AutoCloseable {
        private final Object manager,provider,attribution;
        private final IBinder token=new Binder();
        private final Method call,release;
        private boolean held;
        public ExternalTransport() throws Exception {
            Class<?> managerType=Class.forName("android.app.IActivityManager");
            manager=invoke(Class.forName("android.app.ActivityManager").getMethod("getService"),null);
            release=managerType.getMethod("removeContentProviderExternalAsUser",String.class,IBinder.class,int.class);
            Object holder=invoke(managerType.getMethod("getContentProviderExternal",String.class,int.class,IBinder.class,String.class),manager,AUTHORITY,0,token,"*cmd*");
            if(holder==null)throw new IOException("MIUI permission provider is unavailable");
            held=true;
            try {
                provider=Class.forName("android.app.ContentProviderHolder").getField("provider").get(holder);
                if(provider==null)throw new IOException("MIUI external permission provider is missing");
                Class<?> source=Class.forName("android.content.AttributionSource");
                attribution=source.getConstructor(int.class,String.class,String.class).newInstance(Binder.getCallingUid(),"root",null);
                call=Class.forName("android.content.IContentProvider").getMethod("call",source,String.class,String.class,String.class,Bundle.class);
            } catch(Exception failure) {
                try { close(); } catch(Exception cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
        }
        @Override public Bundle call(String method,Bundle extras) throws Exception {
            if(!held)throw new IllegalStateException("External permission provider is closed");
            return (Bundle)invoke(call,provider,attribution,AUTHORITY,method,null,extras);
        }
        @Override public void close() throws Exception {
            if(held) { invoke(release,manager,AUTHORITY,token,0);held=false; }
        }
    }
    private static Object invoke(Method method,Object receiver,Object...args) throws Exception {
        try { return method.invoke(receiver,args); }
        catch(InvocationTargetException failure) {
            Throwable cause=failure.getCause();
            if(cause instanceof Exception)throw (Exception)cause;
            if(cause instanceof Error)throw (Error)cause;
            throw new IllegalStateException(cause);
        }
    }
    public static final class Snapshot {
        public final int action; public final boolean configured;
        public Snapshot(int action,boolean configured) { this.action=action;this.configured=configured; }
    }
    public static final class ProviderAccess implements Access {
        private final Transport transport; private final long permission;
        public ProviderAccess(Transport transport,boolean newArchitecture) {
            this.transport=transport;this.permission=newArchitecture?14L:16384L;
        }
        public long permission(){return permission;}
        private Bundle identity() {
            Bundle b=new Bundle();b.putString("extra_package",PACKAGE);b.putInt("extra_user_id",0);return b;
        }
        public Snapshot read() throws Exception {
            Bundle args=identity();ArrayList<Long> ids=new ArrayList<>();ids.add(permission);
            args.putSerializable("extra_permission",ids);
            Bundle response=transport.call("22",args);
            Object data=response==null?null:response.getSerializable("extra_data");
            if(!(data instanceof Map))throw new IOException("MIUI permission action response is missing");
            Object value=((Map<?,?>)data).get(permission);
            if(!(value instanceof Integer))throw new IOException("MIUI auto-start action is missing");
            int action=(Integer)value;
            if(action!=0&&action!=1&&action!=2&&action!=3&&action!=6)throw new IOException("Unsupported auto-start action="+action);
            Bundle changedArgs=identity();changedArgs.putLong("extra_permissionId",permission);
            Bundle changed=transport.call("23",changedArgs);
            Object configured=changed==null?null:changed.getSerializable("extra_data");
            if(!(configured instanceof Boolean))throw new IOException("MIUI user-policy response is missing");
            return new Snapshot(action,(Boolean)configured);
        }
        public void set(int action) throws Exception {
            Bundle args=new Bundle();args.putLong("extra_permission",permission);args.putInt("extra_action",action);
            args.putStringArray("extra_package",new String[]{PACKAGE});args.putInt("extra_flags",0);args.putInt("extra_user_id",0);
            // Native case 6 queues AppOpsAdapter work and returns null. Await policy state separately.
            transport.call("6",args);
        }
    }
    public static String apply(Access access,File backup) throws Exception {
        Snapshot before=access.read();
        if(!backup.isFile())save(backup,access.permission(),before);
        load(backup,access.permission());
        if(before.action!=3||!before.configured)access.set(3);
        awaitState(access,3,true,true);
        return "MIUI auto-start policy: action="+before.action+", userConfigured="+before.configured+" -> action=3, userConfigured=true; package="+PACKAGE+"; user=0";
    }
    public static String restore(Access access,File backup) throws Exception {
        if(!backup.isFile())return "No saved LUT auto-start policy";
        Snapshot original=load(backup,access.permission());Snapshot current=access.read();
        if(current.action!=3||!current.configured)return "LUT auto-start policy changed since apply; preserve current user choice";
        // This ROM records every case-6 write in PermissionChangeStatus. Action0 does not clear it.
        // Restore the saved effective choice without claiming the native "never configured" marker reset.
        access.set(original.action);
        Snapshot after=awaitState(access,original.action,true,true);
        return "MIUI auto-start effective action restored; action="+after.action+"; userConfigured="+after.configured
                +(original.configured?"":"; originalUserConfigured=false; native configured marker retained (no supported reset); backup preserved");
    }
    /** One setter request, followed by bounded completion observation on this installation CLI thread. */
    private static Snapshot awaitState(Access access,int action,boolean configured,boolean matchAction) throws Exception {
        long deadline=System.nanoTime()+5000000000L;
        for(;;) {
            Snapshot state=access.read();
            if(state.configured==configured&&(!matchAction||state.action==action))return state;
            long remaining=deadline-System.nanoTime();
            if(remaining<=0)throw new IOException("MIUI auto-start policy completion timed out; lastAction="+state.action
                    +" lastUserConfigured="+state.configured+" expectedAction="+(matchAction?Integer.toString(action):"default")
                    +" expectedUserConfigured="+configured+"; setter was not resent");
            try {Thread.sleep(Math.min(100L,Math.max(1L,remaining/1000000L)));}
            catch(InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted awaiting MIUI auto-start policy; lastAction="+state.action+" lastUserConfigured="+state.configured,interrupted);
            }
        }
    }
    private static void save(File backup,long permission,Snapshot state) throws IOException {
        File parent=backup.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("Cannot create policy backup directory");
        Properties p=new Properties();p.setProperty("format","1");p.setProperty("package",PACKAGE);p.setProperty("user","0");
        p.setProperty("permission",Long.toString(permission));p.setProperty("action",Integer.toString(state.action));p.setProperty("configured",Boolean.toString(state.configured));
        AtomicFile atomic=new AtomicFile(backup);FileOutputStream out=atomic.startWrite();
        try {p.store(new OutputStreamWriter(out,StandardCharsets.UTF_8),"Original MIUI LUT-sharing auto-start user policy");out.getFD().sync();atomic.finishWrite(out);}
        catch(IOException error){atomic.failWrite(out);throw error;}
    }
    private static Snapshot load(File backup,long permission) throws IOException {
        Properties p=new Properties();try(InputStream input=new FileInputStream(backup)){p.load(new InputStreamReader(input,StandardCharsets.UTF_8));}
        if(!"1".equals(p.getProperty("format"))||!PACKAGE.equals(p.getProperty("package"))||!"0".equals(p.getProperty("user"))||!Long.toString(permission).equals(p.getProperty("permission")))
            throw new IOException("Saved auto-start policy identity differs");
        int action;try{action=Integer.parseInt(p.getProperty("action"));}catch(RuntimeException error){throw new IOException("Invalid saved action",error);}
        if(action!=0&&action!=1&&action!=2&&action!=3&&action!=6)throw new IOException("Unsupported saved action");
        String configured=p.getProperty("configured");if(!"true".equals(configured)&&!"false".equals(configured))throw new IOException("Invalid saved user-policy flag");
        return new Snapshot(action,Boolean.parseBoolean(configured));
    }
    public static void main(String[] args) throws Exception {
        if(android.os.Process.myUid()!=0)throw new SecurityException("Installation configuration requires root UID");
        if(args.length!=1||!("apply".equals(args[0])||"restore".equals(args[0])||"check".equals(args[0])))throw new IllegalArgumentException("Expected apply, restore or check");
        Object pm=invoke(Class.forName("android.app.AppGlobals").getMethod("getPackageManager"),null);
        android.content.pm.ApplicationInfo info=(android.content.pm.ApplicationInfo)invoke(Class.forName("android.content.pm.IPackageManager")
                .getMethod("getApplicationInfo",String.class,long.class,int.class),pm,"com.lbe.security.miui",(long)PackageManager.GET_META_DATA,0);
        if(info==null)throw new IOException("MIUI permission manager metadata is unavailable");
        boolean modern=info.metaData!=null&&info.metaData.getBoolean("supportNewArchitectureVersion",false);
        try(ExternalTransport transport=new ExternalTransport()) {
            Access access=new ProviderAccess(transport,modern);
            if("check".equals(args[0])){Snapshot s=access.read();System.out.println("MIUI auto-start policy: permission="+access.permission()+" action="+s.action+" userConfigured="+s.configured+"; package="+PACKAGE+"; user=0");}
            else System.out.println("apply".equals(args[0])?apply(access,BACKUP):restore(access,BACKUP));
        }
    }
}
