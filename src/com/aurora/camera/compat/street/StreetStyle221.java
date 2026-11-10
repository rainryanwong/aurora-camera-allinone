package com.aurora.camera.compat.street;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/** Equipment buttons cycle native photography styles instead of opening a hidden top popup. */
public final class StreetStyle221 {
    private static final int BUTTON=0x7f0b03b4,BRAND=0x7f0b03b5;
    private static final String ICON="221.street.natural.icon";
    private final Method click,next,value,closed,reopen,clearMutex,clearStyle,config,apply,icon,refreshTop;
    private final Method repo,get;
    private final Class<?> styleType;
    private final Field topMode,fragmentMode,componentMode,cv,button,label;
    public StreetStyle221(Host h)throws ReflectiveOperationException {
        click=h.method("X9.i0","X9",View.class);topMode=h.field("X9.i0","k");
        styleType=h.type("r2.m");next=h.method(styleType,"m",int.class);value=h.method(styleType,"getComponentValue",int.class);
        closed=h.method(styleType,"s",int.class);reopen=h.method(styleType,"v",int.class,boolean.class);
        clearMutex=h.method("com.android.camera.data.data.F","a",int.class);clearStyle=h.method("com.android.camera.data.data.n","X0");
        config=h.method("R6.D","a");apply=h.method("R6.D","qd",String.class);refreshTop=h.method("X9.i0","X0",int[].class);
        repo=h.method("g2.a","a");get=h.method("Xh.b","x",Class.class);icon=h.method(styleType,"o");
        fragmentMode=h.field("com.android.camera.fragment.h","mCurrentMode");componentMode=h.field("com.android.camera.data.data.c","mCurrentMode");
        cv=h.field("q4.h","t");button=h.field("q4.u","T");label=h.field("q4.u","U");
    }
    public void install(){
        XposedBridge.hookMethod(click,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            View view=(View)p.args[0];if(view==null||view.getId()!=BUTTON||topMode.getInt(p.thisObject)!=229)return;
            Object style=get.invoke(repo.invoke(null),styleType);if(componentMode.getInt(style)!=229)return;
            Optional<?> owner=(Optional<?>)config.invoke(null);if(!owner.isPresent())return;
            String before=(String)value.invoke(style,229),after;boolean wasClosed=(Boolean)closed.invoke(style,229);
            if(wasClosed){
                // Same native mutex reopening transaction as X9.i0.X9.
                clearMutex.invoke(null,229);clearStyle.invoke(null);reopen.invoke(style,229,false);after="1";
            }else after=(String)next.invoke(style,229);
            if(after!=null&&(wasClosed||!after.equals(before))){
                // Let qd own persistence, CamStyle reset and the module rebuild.
                apply.invoke(owner.get(),after);refreshTop.invoke(p.thisObject,new int[]{190});
                android.util.Log.i("AuroraCameraCompat","street221 style "+before+" -> "+after+" via native qd");
            }
            p.setResult(null);
        }});
    }
    public void update(Object fragment)throws ReflectiveOperationException {
        if(fragmentMode.getInt(fragment)!=229)return;
        LinearLayout root=(LinearLayout)button.get(fragment);TextView text=(TextView)label.get(fragment);
        if(root==null||text==null)return;
        Object style=cv.get(fragment);String selected=(String)value.invoke(style,229);
        View brand=root.findViewById(BRAND);ImageView natural=(ImageView)XposedHelpers.getAdditionalInstanceField(root,ICON);
        if("2".equals(selected)){
            if(natural==null){natural=new ImageView(root.getContext());natural.setScaleType(ImageView.ScaleType.CENTER_INSIDE);root.addView(natural,0,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));XposedHelpers.setAdditionalInstanceField(root,ICON,natural);}
            natural.setImageResource((Integer)icon.invoke(style));natural.setVisibility(View.VISIBLE);
            if(brand!=null)brand.setVisibility(View.GONE);text.setText("原色");text.setVisibility(View.GONE);
            root.setContentDescription("摄影风格 原色");
        }else{
            if(natural!=null)natural.setVisibility(View.GONE);if(brand!=null)brand.setVisibility(View.VISIBLE);text.setVisibility(View.VISIBLE);
            text.setText("0".equals(selected)?0x7f1406ce:0x7f1406cd);
            root.setContentDescription("摄影风格 "+text.getText());
        }
    }
}
