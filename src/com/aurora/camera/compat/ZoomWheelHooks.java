package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Add the 20x label through the native wheel stop-point model. */
public final class ZoomWheelHooks {
    private ZoomWheelHooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        Method prepare=h.method("J9.w","U");
        Field mode=h.field("J9.w","W0"),stops=h.field("J9.w","T0");
        return new HookPlan(){
            public String id(){return "photo-zoom-wheel-20x";}
            public void install(){
                XposedBridge.hookMethod(prepare,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void before(MethodHookParam p)throws Throwable {
                        if(mode.getInt(p.thisObject)!=163)return;
                        List<Float> original=(List<Float>)stops.get(p.thisObject);
                        int next=original.indexOf(30f);
                        if(next<0||!original.contains(10f)||original.contains(20f))return;
                        ArrayList<Float> updated=new ArrayList<>(original);updated.add(next,20f);stops.set(p.thisObject,updated);
                    }
                });
            }
        };
    }
}
