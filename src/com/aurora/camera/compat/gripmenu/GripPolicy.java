package com.aurora.camera.compat.gripmenu;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.InputDevice;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;

/** Accepted N1G half-press and discrete stop rules; state belongs to the module. */
public final class GripPolicy {
    public interface Runtime {
        boolean active(Object owner); void active(Object owner,boolean value);
        Object manager(Object owner); boolean canFocus(Object manager);
        Object focus(Object manager); boolean nativeFocus(Object focus); int focusMode(Object focus);
        Rect focusRect(Object focus); Point focusPoint(Object focus); Object focusUi();
        void focusUi(Object ui,int type,int x,int y); void centerFocus(Object focus);
        void notifyFocus(Object owner,boolean value); int mode(Object owner);
        void focusLock(Object manager,boolean value); void originalLock(Object owner)throws Throwable;
        void startFocus(Object focus,int type); void cancelNative(Object focus,boolean value);
        void log(String message);
    }
    private final Runtime runtime;
    public GripPolicy(Runtime runtime){this.runtime=runtime;}
    public static boolean n1g(KeyEvent event){
        if(event==null)return false;InputDevice device=event.getDevice();if(device==null)return false;
        return(device.getVendorId()==0x2717&&device.getProductId()==0x5083)
                ||"N1G".equals(device.getName())||"N1G Mouse".equals(device.getName());
    }
    public void down(Object owner,KeyEvent event)throws Throwable{
        runtime.active(owner,false);
        if(n1g(event)){
            Object manager=runtime.manager(owner);
            if(!runtime.canFocus(manager)){runtime.originalLock(owner);return;}
            Object focus=runtime.focus(manager);
            if(runtime.nativeFocus(focus)){
                int mode=runtime.focusMode(focus);
                if(mode!=0&&mode!=5){
                    Rect rect=runtime.focusRect(focus);
                    if(rect!=null&&!rect.isEmpty()){
                        Object ui=runtime.focusUi();if(ui!=null){Point point=runtime.focusPoint(focus);runtime.focusUi(ui,5,point.x,point.y);}
                    }else runtime.centerFocus(focus);
                    runtime.notifyFocus(owner,false);
                    if(runtime.mode(owner)==256)runtime.focusLock(manager,true);else runtime.originalLock(owner);
                    runtime.startFocus(focus,0);runtime.active(owner,true);
                    runtime.log("half press: native AF task started");return;
                }
            }
        }
        runtime.originalLock(owner);
    }
    /** True consumes only the original module cancel at the accepted key-up point. */
    public boolean up(Object owner,KeyEvent event){
        if(!runtime.active(owner)||!n1g(event))return false;
        runtime.active(owner,false);Object focus=runtime.focus(runtime.manager(owner));
        if(focus==null)return false;
        runtime.cancelNative(focus,true);
        runtime.log("half release: native AF state cleaned");return true;
    }
    public static boolean stopMode(int mode){return mode==163||mode==168||mode==173||mode==225||mode==229;}
    public interface StopRange { boolean contains(float value); }
    public static void addStops(ArrayList<Float> values,int mode,boolean front,StopRange range){
        if(!stopMode(mode)||front||values==null||!values.contains(1f))return;
        float maximum=Collections.max(values);
        if(maximum>=1.2f&&range.contains(1.2f)&&!values.contains(1.2f))values.add(1.2f);
        if(maximum>=1.5f&&range.contains(1.5f)&&!values.contains(1.5f))values.add(1.5f);
        Collections.sort(values);
    }
}
