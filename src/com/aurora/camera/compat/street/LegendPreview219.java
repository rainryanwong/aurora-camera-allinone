package com.aurora.camera.compat.street;

import android.graphics.Rect;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.Map;

/** Observe the real native View; q4.h also casts its H field back to View. */
public final class LegendPreview219 {
    public interface Listener {void preview(int height,Rect rect);}
    private static final Map<Object,WeakReference<Listener>> owners=new WeakHashMap<>();
    private LegendPreview219(){}
    public static void attach(Object nativeView,Listener listener){owners.put(nativeView,new WeakReference<>(listener));}
    public static void detach(Object nativeView,Listener listener){WeakReference<Listener> r=owners.get(nativeView);if(r!=null&&r.get()==listener)owners.remove(nativeView);}
    public static void install(Host h)throws ReflectiveOperationException {
        XposedBridge.hookMethod(h.method("com.android.camera.ui.EquipStreetBgView","a",int.class,Rect.class),new StrictHook(){
            @Override protected void after(MethodHookParam p){if(p.hasThrowable())return;WeakReference<Listener> r=owners.get(p.thisObject);Listener listener=r==null?null:r.get();if(listener!=null)listener.preview((Integer)p.args[0],(Rect)p.args[1]);}
        });
    }
}
