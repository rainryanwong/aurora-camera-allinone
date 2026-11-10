package com.aurora.camera.compat;

import android.view.View;
import android.graphics.drawable.Drawable;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Repair the native HDR presentation consumers; keep values and request routing native. */
public final class HdrPresentation214Hooks {
    private HdrPresentation214Hooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        final Method normalIcons=h.method("r2.A","o");
        final int normalTop=h.field("Rh.b","ic_new_config_hdr_normal_top_mm").getInt(null);
        final int offMenu=h.field("Rh.b","ic_new_config_hdr_off").getInt(null);
        final int autoMenu=h.field("Rh.b","ic_top_config_hdr_auto").getInt(null);
        final int normalMenu=h.field("Rh.b","ic_new_config_hdr_normal").getInt(null);
        final Method bind=h.method("com.android.camera2.compat.theme.custom.mm.top.LabelItemViewMM","a",boolean.class,h.type("com.android.camera.data.data.d"),int.class,Drawable.class,h.type("b5.i"),int.class);
        final Field configId=h.field("b5.i","c"),rowValue=h.field("com.android.camera.data.data.d","r");
        final Field rowImage=h.field("com.android.camera.data.data.d","c");
        final Method click=h.method("X9.i0","z3",View.class),notify=h.method("X9.N","accept",Object.class);
        final Field clickMode=h.field("X9.i0","k"),tip=h.field("X9.N","a");
        // tip_hdr_on survives in the generated R class but was removed from
        // resources.arsc. accessibility_hdr_on is an existing localized string
        // in the actual 1770 APK (zh_CN: HDR，开启状态).
        final int tipAuto=h.field("Rh.e","tip_hdr_auto").getInt(null),tipOn=h.field("Rh.e","accessibility_hdr_on").getInt(null);
        final Method repository=h.method("g2.a","a"),component=h.method("Xh.b","x",Class.class);
        final Class<?> hdr=h.type("r2.A");final Method value=h.method(hdr,"getComponentValue",int.class);
        final Method topFactory=h.method("X9.i3","invoke",Object.class),menuFactory=h.method("X9.D2","invoke",Object.class);
        final Field topBuilder=h.field("X9.i3","b"),topMode=h.field("X9.i3","c"),topImage=h.field("b5.j$a","a"),topAnimation=h.field("b5.j$a","d");
        final Field menuBuilder=h.field("X9.D2","c"),menuMode=h.field("X9.D2","b"),menuImage=h.field("b5.a$a","a"),menuAnimation=h.field("b5.a$a","b");
        final ThreadLocal<Integer> clicks=new ThreadLocal<>();
        return new HookPlan(){
            public String id(){return "214.native-hdr-icons-and-tip";}
            public void install(){
                // Stock o() calls J("auto") twice, including for normal/on.
                XposedBridge.hookMethod(normalIcons,new StrictHook(){
                    @Override protected void before(MethodHookParam p){p.setResult(new int[]{normalTop,normalTop});}
                });
                XposedBridge.hookMethod(bind,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        Object config=p.args[4];if(config==null||configId.getInt(config)!=194)return;
                        Object row=p.args[1];String selected=(String)rowValue.get(row);
                        int icon="off".equals(selected)?offMenu:"auto".equals(selected)?autoMenu:isOn(selected)?normalMenu:-1;
                        if(icon<0)return;
                        // This consumer uses the explicit icon parameter, then h;
                        // c is the image-presence gate. Both must describe HDR.
                        rowImage.setInt(row,icon);p.args[5]=icon;
                    }
                });
                XposedBridge.hookMethod(topFactory,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||!hdr.isInstance(p.args[0])||!isOn((String)value.invoke(p.args[0],topMode.getInt(p.thisObject))))return;
                        Object builder=topBuilder.get(p.thisObject);topImage.setInt(builder,normalTop);topAnimation.setInt(builder,0);
                    }
                });
                XposedBridge.hookMethod(menuFactory,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||!hdr.isInstance(p.args[0])||!isOn((String)value.invoke(p.args[0],menuMode.getInt(p.thisObject))))return;
                        Object builder=menuBuilder.get(p.thisObject);menuImage.setInt(builder,normalTop);menuAnimation.setInt(builder,0);
                    }
                });
                XposedBridge.hookMethod(click,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        p.setObjectExtra("214.hdr.click",clicks.get());clicks.set(clickMode.getInt(p.thisObject));
                    }
                    @Override protected void after(MethodHookParam p){restore(clicks,(Integer)p.getObjectExtra("214.hdr.click"));}
                });
                XposedBridge.hookMethod(notify,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        Integer mode=clicks.get();if(mode==null||tip.getInt(p.thisObject)!=tipAuto)return;
                        Object owner=component.invoke(repository.invoke(null),hdr);
                        if(!isOn((String)value.invoke(owner,mode)))return;
                        p.setObjectExtra("214.hdr.tip",tipAuto);tip.setInt(p.thisObject,tipOn);
                    }
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        Object previous=p.getObjectExtra("214.hdr.tip");if(previous!=null)tip.setInt(p.thisObject,(Integer)previous);
                    }
                });
            }
        };
    }
    private static boolean isOn(String value){return "normal".equals(value)||"on".equals(value);}
    private static <T> void restore(ThreadLocal<T> local,T previous){if(previous==null)local.remove();else local.set(previous);}
}
