package com.aurora.camera.compat.softlight;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.Options229;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

/** Reuses 1770's XML, TopAlertSlideSwitchButton and the current native theme. */
public final class SoftLightPanel245 {
    private static final int TYPE=0x41555346;
    private static final String CONTROL="com.android.camera.ui.TopAlertSlideSwitchButton";
    private static final String[] LABELS={"关闭","黑柔","白柔"};
    private final Host host;
    private final SoftLightRuntime245 runtime;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Constructor<?> row;
    private final Method add,theme,themeStyle,style,clickable;
    private final Field parent,fragmentMode;
    private WeakReference<Object> fragment=new WeakReference<>(null);
    private LinearLayout panel;
    private int panelMode=-1;
    private int selectedTextColor,normalTextColor;
    private boolean binding,posted;
    public SoftLightPanel245(Host h,SoftLightRuntime245 r)throws ReflectiveOperationException{
        host=h;runtime=r;parent=h.field("p5.q","b0");fragmentMode=h.field("com.android.camera.fragment.h","mCurrentMode");
        add=h.method("p5.q","ys",View.class,boolean.class,int.class,int.class,LinearLayout.LayoutParams.class,int.class);
        row=h.type(CONTROL+"$b").getDeclaredConstructor();row.setAccessible(true);
        h.field(CONTROL+"$b","b");h.field(CONTROL+"$b","c");h.field(CONTROL+"$b","h");
        h.field(CONTROL,"m");h.field(CONTROL,"P");h.field(CONTROL,"r");h.field(CONTROL,"j");h.field(CONTROL,"l");
        h.method(CONTROL,"c",String.class,int.class,h.type(CONTROL+"$b"));h.method(CONTROL,"setIndex",int.class);
        h.method(CONTROL,"setSlideSwitchListener",h.type("a5.c"));h.method("a5.c","a",int.class,String.class);h.method("a5.c","b");
        theme=h.method("q9.a","a");themeStyle=h.method("q9.b","f");style=h.method("r9.D","e",Context.class,int.class);clickable=h.method("V6.b","b");
        for(String name:new String[]{"a","b","c","d","e","f","g","h","i","j"})h.field("r9.D$a",name);
        for(String name:new String[]{"setChildWidth","setChildHeight","setmChildMargin","setIndicatorColor","setSelectColor","setBackgroundColor","setNormalColor","setType"})h.method(CONTROL,name,int.class);
        h.method(CONTROL,"setSemicircleRectStyle",boolean.class);h.method(CONTROL,"getBackgroundPaint");h.method("f2.e","a",int.class,boolean.class);h.field("f2.e","c");
    }
    private static Object call(Object owner,String name,Object...args){return XposedHelpers.callMethod(owner,name,args);}
    private int applyStyle(){
        // Phoenix uses the dedicated three-option text style (186), independent of
        // the active capture mode. Stock's other style variants leave child width 0.
        Object s=SoftLightRuntime245.invoke(style,SoftLightRuntime245.invoke(themeStyle,SoftLightRuntime245.invoke(theme,null)),panel.getContext(),186);
        call(panel,"setChildWidth",((Integer)XposedHelpers.getObjectField(s,"f")));call(panel,"setChildHeight",((Integer)XposedHelpers.getObjectField(s,"g")));call(panel,"setmChildMargin",((Integer)XposedHelpers.getObjectField(s,"h")));
        selectedTextColor=(Integer)XposedHelpers.getObjectField(s,"i");
        call(panel,"setSemicircleRectStyle",((Boolean)XposedHelpers.getObjectField(s,"a")));call(panel,"setIndicatorColor",((Integer)XposedHelpers.getObjectField(s,"e")));call(panel,"setSelectColor",selectedTextColor);call(panel,"setBackgroundColor",((Integer)XposedHelpers.getObjectField(s,"c")));
        Paint paint=(Paint)call(panel,"getBackgroundPaint");paint.setStyle((Paint.Style)XposedHelpers.getObjectField(s,"b"));paint.setStrokeWidth(((Integer)XposedHelpers.getObjectField(s,"j")));
        try{Object color=host.field("f2.e","c").get(null);int id=panel.getResources().getIdentifier("top_alert_slide_switch_normal_color","color","com.android.camera");normalTextColor=(Integer)call(color,"a",id,true);call(panel,"setNormalColor",normalTextColor);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        return ((Integer)XposedHelpers.getObjectField(s,"d"));
    }
    private void create(Context context){
        int layout=context.getResources().getIdentifier("fgt","layout","com.android.camera");
        panel=(LinearLayout)LayoutInflater.from(context).inflate(layout,null);
        ArrayList<Object> rows=new ArrayList<>();
        try{for(int i=0;i<LABELS.length;i++){Object item=row.newInstance();XposedHelpers.setObjectField(item,"b",Integer.toString(i));XposedHelpers.setIntField(item,"c",android.R.string.cancel);XposedHelpers.setObjectField(item,"h","柔光效果，"+LABELS[i]);rows.add(item);}}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        XposedHelpers.setObjectField(panel,"m",rows);XposedHelpers.setObjectField(panel,"P",rows);XposedHelpers.setBooleanField(panel,"r",true);
        panel.setTag(TYPE);call(panel,"setType",TYPE);applyStyle();
        for(int i=0;i<rows.size();i++){call(panel,"c","0",i,rows.get(i));((TextView)panel.getChildAt(i)).setText(LABELS[i]);}
        try{call(panel,"setSlideSwitchListener",Proxy.newProxyInstance(host.loader,new Class<?>[]{host.type("a5.c")},(proxy,m,args)->{
            if(m.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,m.getName(),args);
            if(m.getName().equals("b"))return !(Boolean)SoftLightRuntime245.invoke(clickable,null);
            if(m.getName().equals("a")){runtime.select(Integer.parseInt((String)args[1]));render();return null;}
            throw new IllegalStateException("Unknown soft-light callback: "+m);
        }));}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}
    }
    public void detach(){if(panel!=null&&panel.getParent()!=null)((ViewGroup)panel.getParent()).removeView(panel);}
    public void destroy(Object owner){if(fragment.get()!=owner)return;detach();fragment.clear();panel=null;panelMode=-1;}
    public void stop(Object owner){if(fragment.get()==owner)detach();}
    public void refresh(){
        if(Looper.myLooper()!=Looper.getMainLooper()){
            synchronized(this){if(posted)return;posted=true;}
            main.post(()->{synchronized(this){posted=false;}render();});return;
        }
        render();
    }
    private void render(){
        if(panel==null||panel.getParent()==null||!runtime.state.enabled()||!SoftLightState245.supports(panelMode))return;
        int selected=runtime.state.effective(panelMode);panel.setVisibility(selected==3?View.GONE:View.VISIBLE);if(selected==3)return;
        if(((Integer)XposedHelpers.getObjectField(panel,"j"))!=selected){
            ValueAnimator animator=(ValueAnimator)XposedHelpers.getObjectField(panel,"l");if(animator!=null)animator.cancel();
            panel.setEnabled(true);
            // Native cancellation can already select the destination. Selecting
            // that same index again restores its text to the normal (white) color.
            if(((Integer)XposedHelpers.getObjectField(panel,"j"))!=selected)call(panel,"setIndex",selected);
        }
        // Native color setters update stored colors, not existing TextViews.
        // Rebind this panel after a theme change even when selection is unchanged.
        for(int i=0;i<panel.getChildCount();i++)((TextView)panel.getChildAt(i)).setTextColor(i==selected?selectedTextColor:normalTextColor);
    }
    public void update(Object owner){
        if(Looper.myLooper()!=Looper.getMainLooper())throw new IllegalStateException("Native top-alert lifecycle must run on main thread");
        if(binding)return;
        try{
            int mode=fragmentMode.getInt(owner);
            if(!runtime.state.enabled()||!SoftLightState245.supports(mode)){if(fragment.get()==owner)detach();panelMode=-1;return;}
            LinearLayout target=(LinearLayout)parent.get(owner);if(target==null)return;
            binding=true;
            if(fragment.get()!=owner){detach();panel=null;fragment=new WeakReference<>(owner);}
            runtime.sync(runtime.controller());if(panel==null)create(target.getContext());
            int height=applyStyle();panelMode=mode;
            if(panel.getParent()!=target){detach();SoftLightRuntime245.invoke(add,owner,panel,true,1000,0,new LinearLayout.LayoutParams(-2,height),-1);}
            render();
        }catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        finally{binding=false;}
    }
}
