package com.aurora.camera.compat.sharedlut;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/** Establishes provider visibility through a signed module broadcast before provider I/O. */
public final class LutShare257Bootstrap {
    static final String MODULE="com.aurora.camera.compat";
    static final String REQUEST=MODULE+".action.LUT_PROVIDER_CONNECT";
    static final String READY=MODULE+".action.LUT_PROVIDER_READY";
    static final String TAG="AuroraLutShare257";
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final List<Runnable> waiting=new ArrayList<Runnable>();
    private static Context owner;
    private static BroadcastReceiver receiver;
    private static boolean requested,connected;

    private LutShare257Bootstrap() {}

    public static void connect(Context context,Runnable ready) {
        if(Build.VERSION.SDK_INT<34) throw new UnsupportedOperationException("LUT provider visibility requires Android 14 or later");
        Context application=context.getApplicationContext();
        if(Looper.myLooper()==Looper.getMainLooper()) connectMain(application,ready);
        else MAIN.post(new Runnable(){@Override public void run(){connectMain(application,ready);}});
    }

    private static void connectMain(Context application,Runnable ready) {
        if(connected) { MAIN.post(ready); return; }
        waiting.add(ready);
        if(requested) return;
        owner=application;
        receiver=new BroadcastReceiver(){
            @Override public void onReceive(Context context,Intent intent) {
                if(!READY.equals(intent.getAction()) || !owner.getPackageName().equals(intent.getPackage())) return;
                try {
                    requireCaller(owner,getSentFromUid(),getSentFromPackage(),MODULE,LutShare246Access.MODULE_CERT_SHA256);
                } catch(SecurityException rejected) {
                    Log.w(TAG,"Rejected LUT provider visibility reply",rejected);
                    return;
                }
                connected=true;
                owner.unregisterReceiver(this);
                receiver=null;
                List<Runnable> callbacks=new ArrayList<Runnable>(waiting);
                waiting.clear();
                Log.i(TAG,"LUT provider visible to "+owner.getPackageName());
                for(Runnable callback:callbacks) MAIN.post(callback);
            }
        };
        owner.registerReceiver(receiver,new IntentFilter(READY),null,MAIN,Context.RECEIVER_EXPORTED);
        requested=true;
        Intent request=new Intent(REQUEST).setComponent(new ComponentName(MODULE,LutShare257Receiver.class.getName()))
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        send(owner,request);
    }

    static void send(Context context,Intent intent) {
        context.sendOrderedBroadcast(intent,null,BroadcastOptions.makeBasic().setShareIdentityEnabled(true).toBundle());
    }

    static void requireCaller(Context context,int uid,String actualPackage,String expectedPackage,String certificate) {
        if(uid<0 || !expectedPackage.equals(actualPackage)) throw new SecurityException("Unexpected LUT visibility sender");
        PackageManager manager=context.getPackageManager();
        try {
            if(manager.getApplicationInfo(expectedPackage,0).uid!=uid || !manager.hasSigningCertificate(expectedPackage,
                    LutShare246Access.certificateBytes(certificate),PackageManager.CERT_INPUT_SHA256))
                throw new SecurityException("Unexpected LUT visibility sender identity");
        } catch(PackageManager.NameNotFoundException missing) {
            throw new SecurityException("LUT visibility sender package is missing",missing);
        }
    }
}
