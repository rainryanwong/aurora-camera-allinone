package com.aurora.camera.compat.sharedlut;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/** Runs under the module UID and shares that identity with the requesting camera or editor. */
public final class LutShare257Receiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(Build.VERSION.SDK_INT<34 || !LutShare257Bootstrap.REQUEST.equals(intent.getAction())) return;
        String caller=getSentFromPackage();
        try {
            if(!LutShare246Access.CAMERA.equals(caller) && !LutShare246Access.EDITOR.equals(caller))
                throw new SecurityException("Unexpected LUT visibility requester");
            LutShare257Bootstrap.requireCaller(context,getSentFromUid(),caller,caller,LutShare246Access.CERT_SHA256);
        } catch(SecurityException rejected) {
            Log.w(LutShare257Bootstrap.TAG,"Rejected LUT provider visibility request",rejected);
            return;
        }
        LutShare257Bootstrap.send(context,new Intent(LutShare257Bootstrap.READY).setPackage(caller));
    }
}
