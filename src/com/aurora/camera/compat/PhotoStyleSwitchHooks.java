package com.aurora.camera.compat;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import com.aurora.camera.compat.ui.MiuixDialogs;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/** Photo style gestures use the stock style transaction and popup controller. */
public final class PhotoStyleSwitchHooks {
    private static final String KEY="aurora_photo_style_switch";
    private static final int POPUP_GROUP=0x7f0b0b62;
    private static final String[] TITLES={"原生","选择后关闭面板","顺序切换"};
    private PhotoStyleSwitchHooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        Options229 options=new Options229(h);
        MiuixDialogs dialogs=new MiuixDialogs(h);
        Method settings=h.method("com.android.camera.fragment.settings.CameraCapturePreferenceFragment","registerPreferenceListener");
        Method click=h.method("X9.i0","X9",View.class),refresh=h.method("X9.i0","X0",int[].class);
        Field mode=h.field("X9.i0","k");
        Class<?> styleType=h.type("r2.m");
        Method repo=h.method("g2.a","a"),get=h.method("Xh.b","x",Class.class);
        Method value=h.method(styleType,"getComponentValue",int.class),nextValue=h.method(styleType,"m",int.class);
        Method disabled=h.method(styleType,"getDisableReasonString",Context.class),closed=h.method(styleType,"s",int.class),reopen=h.method(styleType,"v",int.class,boolean.class);
        Method clearMutex=h.method("com.android.camera.data.data.F","a",int.class),clearStyle=h.method("com.android.camera.data.data.n","X0");
        Method config=h.method("R6.D","a"),apply=h.method("R6.D","qd",String.class);
        Method hide=h.method("p5.u$a","a");
        Method touch=h.method("miuix.visual.check.VisualCheckBox","onTouchEvent",MotionEvent.class),perform=h.method("miuix.visual.check.VisualCheckBox","performClick");
        return new HookPlan(){
            public String id(){return "photo-style-switch";}
            int choice(){return options.prefs().getInt(KEY,0);}
            void choose(Object fragment){
                Context context=(Context)XposedHelpers.callMethod(fragment,"requireActivity");
                dialogs.builder(context).setTitle("摄影风格切换优化").setSingleChoiceItems(TITLES,choice(),(dialog,selected)->{
                    if(!options.prefs().edit().putInt(KEY,selected).commit())throw new IllegalStateException("Cannot save photography style setting");
                    Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
                    Object row=XposedHelpers.callMethod(root,"k0",KEY);
                    XposedHelpers.callMethod(row,"c0",TITLES[selected]);dialog.dismiss();
                }).setNegativeButton("取消",null).show();
            }
            void close(View item)throws ReflectiveOperationException {
                if(choice()!=1||!item.isEnabled())return;
                ViewParent parent=item.getParent();
                while(parent instanceof View&&((View)parent).getId()!=POPUP_GROUP)parent=parent.getParent();
                if(!(parent instanceof View)||!((View)parent).isShown())return;
                hide.invoke(null);
            }
            public void install(){
                XposedBridge.hookMethod(settings,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(!p.hasThrowable())options.addAction(p.thisObject,KEY,"摄影风格切换优化",TITLES[choice()],"pref_aurora_watermark_device_name",()->choose(p.thisObject));
                }});
                XposedBridge.hookMethod(click,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable {
                    View item=(View)p.args[0];if(choice()!=2||item==null)return;
                    Optional<?> owner=(Optional<?>)config.invoke(null);if(!owner.isPresent())return;
                    int currentMode=mode.getInt(p.thisObject);
                    Object style=get.invoke(repo.invoke(null),styleType);
                    if(disabled.invoke(style,item.getContext())!=null)return;
                    String current=(String)value.invoke(style,currentMode),next=(String)nextValue.invoke(style,currentMode);
                    if(next==null||next.equals(current))return;
                    if((Boolean)closed.invoke(style,currentMode)){clearMutex.invoke(null,currentMode);clearStyle.invoke(null);reopen.invoke(style,currentMode,false);}
                    apply.invoke(owner.get(),next);refresh.invoke(p.thisObject,new int[]{190});p.setResult(null);
                }});
                XposedBridge.hookMethod(touch,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {
                    if(!p.hasThrowable()&&((MotionEvent)p.args[0]).getActionMasked()==MotionEvent.ACTION_UP)close((View)p.thisObject);
                }});
                XposedBridge.hookMethod(perform,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable {if(!p.hasThrowable())close((View)p.thisObject);}});
            }
        };
    }
}
