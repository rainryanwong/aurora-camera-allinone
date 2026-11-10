package com.aurora.camera.compat;

import android.app.Activity;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.Window;
import android.view.WindowManager;
import java.lang.ref.WeakReference;
import static de.robv.android.xposed.XposedHelpers.*;

/** A window-scoped override keeps automatic brightness out of the root hold. */
final class BrightnessWindow274 {
    private final Host host;
    private WeakReference<Activity> activity=new WeakReference<>(null);
    private Window pinned;
    private float previous,fixed;
    BrightnessWindow274(Host host){this.host=host;}
    void activity(Activity value){activity=new WeakReference<>(value);}
    void pin(float brightness){
        if(pinned!=null)return;
        Activity owner=activity.get();
        if(owner==null||!owner.hasWindowFocus())throw new IllegalStateException("Camera window is not focused");
        Window window=owner.getWindow();
        WindowManager.LayoutParams attributes=window.getAttributes();
        previous=attributes.screenBrightness;fixed=brightness;
        rate(true);
        pinned=window;
        attributes.screenBrightness=fixed;
        window.setAttributes(attributes);
    }
    void attributes(Window window,WindowManager.LayoutParams attributes){
        if(window==pinned&&attributes.screenBrightness!=fixed){previous=attributes.screenBrightness;attributes.screenBrightness=fixed;}
    }
    void release(){
        Window window=pinned;if(window==null)return;
        pinned=null;
        WindowManager.LayoutParams attributes=window.getAttributes();attributes.screenBrightness=previous;
        window.setAttributes(attributes);
        rate(false);
    }
    private void rate(boolean immediate){
        IBinder service=(IBinder)callStaticMethod(new Options229(host).type("Vp.d"),"a","display");
        if(service==null)throw new IllegalStateException("Display service unavailable");
        Parcel data=Parcel.obtain(),reply=Parcel.obtain();
        try{
            data.writeInterfaceToken("android.view.android.hardware.display.IDisplayManager");data.writeBoolean(immediate);
            if(!service.transact(16777213,data,reply,0))throw new IllegalStateException("Display brightness rate request rejected");
        }catch(RemoteException failure){throw new IllegalStateException("Cannot change display brightness rate",failure);}
        finally{reply.recycle();data.recycle();}
    }
}
