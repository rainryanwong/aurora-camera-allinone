package com.aurora.camera.compat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.aurora.camera.compat.ui.MiuixDialogs;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static de.robv.android.xposed.XposedHelpers.*;

/** Brightness boost in the host's saved top-bar/menu/editor layout. */
public final class Brightness273Hooks implements HookPlan {
    private static final int TOKEN=3273, TITLE=0x7e140273, ICON=0x7f0809ab;
    private final Host host;
    private final Options229 options;
    private final MiuixDialogs dialogs;
    private final BrightnessBoost273 boost;
    private final Method factory,available,editor,defaults,candidates,dispatch,settings,text,pause,resume,focus,streetMenu,windowAttributes,modeChange,componentWrite,masterEnabled,dolbyEnabled;
    private final java.lang.reflect.Field currentMode;
    private final Object unsafe;
    private final Method allocate;
    private final Class<?> rowType,cameraType,masterType,dolbyType;

    public static HookPlan resolve(Host host)throws ReflectiveOperationException{return new Brightness273Hooks(host);}
    private Brightness273Hooks(Host h)throws ReflectiveOperationException{
        host=h;options=new Options229(h);dialogs=new MiuixDialogs(h);
        h.method("E1.g5","d",Context.class,String.class);
        boost=new BrightnessBoost273(h,this::refresh,message->callStaticMethod(type("E1.g5"),"d",options.context(),message));
        factory=h.method("X9.w5","E",int.class);available=h.method("u2.y","T",int.class,ArrayList.class);
        editor=h.method("u2.y","Y",ArrayList.class,boolean.class);
        defaults=h.method("Y9.O","f",int.class,Optional.class);candidates=h.method("Y9.O","b",int.class,Optional.class);
        dispatch=h.method("r6.W","fk",int.class);
        settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        text=Resources.class.getDeclaredMethod("getText",int.class);pause=Activity.class.getDeclaredMethod("onPause");
        cameraType=h.type("com.android.camera.Camera");rowType=h.type("com.android.camera.data.data.d");
        focus=h.method(cameraType,"onWindowFocusChanged",boolean.class);
        resume=Activity.class.getDeclaredMethod("onResume");
        windowAttributes=Activity.class.getDeclaredMethod("onWindowAttributesChanged",android.view.WindowManager.LayoutParams.class);
        streetMenu=h.method("com.android.camera.features.mode.equipstreet.a","l");
        masterType=h.type("r2.i");dolbyType=h.type("s2.c");
        masterEnabled=h.method(masterType,"p");dolbyEnabled=h.method(dolbyType,"p");
        componentWrite=h.method("com.android.camera.data.data.c","setComponentValue",int.class,String.class);
        Class<?> start=h.type("com.android.camera.module.loader.base.StartControl");h.method(start,"getTargetMode");
        modeChange=h.method(cameraType,"i8",start);
        currentMode=h.field("com.android.camera.module.c0","a");
        h.method("g2.a","a");h.method("Xh.b","x",Class.class);
        h.method(masterType,"isSwitchOn",int.class);h.method(dolbyType,"isSwitchOn",int.class);
        Class<?> type=Class.forName("sun.misc.Unsafe");java.lang.reflect.Field singleton=type.getDeclaredField("theUnsafe");singleton.setAccessible(true);unsafe=singleton.get(null);
        allocate=type.getDeclaredMethod("allocateInstance",Class.class);allocate.setAccessible(true);
        h.type("b5.i$a").getDeclaredConstructor();h.type("b5.j$a").getDeclaredConstructor();h.method("b5.j$a","a");
        h.type("b5.a$a").getDeclaredConstructor();h.method("b5.a$a","a");
        h.method("R6.o1","X0",int[].class);h.type("M1.c").getDeclaredConstructor(int.class);
        h.method("miuix.appcompat.app.l","l",int.class);
    }
    public String id(){return "273.brightness-boost";}
    private static boolean supports(int mode){
        return mode==167||mode==180||mode==225||mode==229||mode==256||mode==162||mode==163
            ||mode==168||mode==173||mode==171||mode==175;
    }
    private Class<?> type(String name){return options.type(name);}
    private Object call(Object target,String name,Object...args){return callMethod(target,name,args);}
    private void refresh(){
        @SuppressWarnings("unchecked") Optional<Object> top=(Optional<Object>)callStaticMethod(type("R6.o1"),"a");
        top.ifPresent(target->call(target,"X0",new int[]{TOKEN}));
        @SuppressWarnings("unchecked") Optional<Object> tray=(Optional<Object>)callStaticMethod(type("R6.s1"),"a");
        @SuppressWarnings("unchecked") java.util.function.Consumer<Object> update=(java.util.function.Consumer<Object>)newInstance(type("M1.c"),1);
        tray.ifPresent(update);
    }
    private int mode(){try{return currentMode.getInt(null);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    private boolean videoRestricted(int mode){
        if(mode!=162&&mode!=180)return false;
        Object repository=callStaticMethod(type("g2.a"),"a");
        return (Boolean)call(call(repository,"x",masterType),"isSwitchOn",mode)
            ||(Boolean)call(call(repository,"x",dolbyType),"isSwitchOn",mode);
    }
    private void toggle(){if(boost.active()||!videoRestricted(mode()))boost.toggle();}
    private Object item(){
        Object builder=newInstance(type("b5.i$a"));setIntField(builder,"a",TOKEN);setIntField(builder,"b",8388613);
        setObjectField(builder,"c",Proxy.newProxyInstance(host.loader,new Class<?>[]{type("b5.i$c")},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,method.getName(),args);
            if(!method.getName().equals("c"))throw new IllegalStateException(method.toString());
            Object state=newInstance(type("b5.j$a"));setIntField(state,"a",ICON);setIntField(state,"e",TITLE);
            setBooleanField(state,"g",boost.active());return call(state,"a");
        }));
        setObjectField(builder,"e",(View.OnClickListener)view->toggle());
        setObjectField(builder,"d",Proxy.newProxyInstance(host.loader,new Class<?>[]{type("b5.i$b")},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,method.getName(),args);
            if(!method.getName().equals("c"))throw new IllegalStateException(method.toString());
            Object state=newInstance(type("b5.a$a"));setIntField(state,"a",ICON);setIntField(state,"c",TITLE);
            setBooleanField(state,"f",boost.active());return call(state,"a");
        }));
        return newInstance(type("b5.i"),builder);
    }
    private void settings(Object fragment){
        Object row=options.addAction(fragment,BrightnessBoost273.KEY,"亮度提升数值",String.valueOf(boost.value()),()->edit(fragment));
        Object root=getObjectField(fragment,"mPreferenceGroup");
        Object group=call(root,"k0",com.aurora.camera.compat.ui.NativePreferences240.CLASSIC_GROUP);
        call(root,"n0",row);setIntField(row,"g",3);call(group,"j0",row);
        Object asd=call(group,"k0","aurora_xiaomi_asd239");if(asd!=null){call(group,"n0",asd);setIntField(asd,"g",4);call(group,"j0",asd);}
    }
    private void edit(Object fragment){
        Context context=(Context)call(fragment,"requireActivity");
        EditText input=(EditText)newInstance(type("miuix.androidbasewidget.widget.StateEditText"),context,null);
        input.setSingleLine(true);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setText(String.valueOf(boost.value()));input.selectAll();
        LinearLayout content=new LinearLayout(context);int padding=Math.round(24*context.getResources().getDisplayMetrics().density);
        content.setPadding(padding,0,padding,0);content.addView(input,new LinearLayout.LayoutParams(-1,-2));
        Dialog dialog=dialogs.builder(context).setTitle("亮度提升数值").setMessage("输入 2065–3600，默认 2065")
            .setView(content).setNegativeButton("取消",null).setPositiveButton("保存",null).show();
        View save=(View)call(dialog,"l",android.content.DialogInterface.BUTTON_POSITIVE);
        save.setOnClickListener(view->{
            String entered=input.getText().toString().trim();int value;
            try{value=Integer.parseInt(entered);}catch(NumberFormatException failure){input.setError("请输入 2065–3600 的整数");return;}
            if(value<2065||value>3600){input.setError("数值范围为 2065–3600");return;}
            if(!options.prefs().edit().putInt(BrightnessBoost273.KEY,value).commit()){Toast.makeText(context,"亮度数值保存失败",Toast.LENGTH_LONG).show();return;}
            Object root=getObjectField(fragment,"mPreferenceGroup");call(call(root,"k0",BrightnessBoost273.KEY),"c0",String.valueOf(value));dialog.dismiss();
        });
    }
    @Override public void install(){
        XposedBridge.hookMethod(text,new StrictHook(){@Override protected void before(MethodHookParam p){if((Integer)p.args[0]==TITLE)p.setResult("亮度提升");}});
        XposedBridge.hookMethod(factory,new StrictHook(){@Override protected void before(MethodHookParam p){if((Integer)p.args[0]==TOKEN)p.setResult(item());}});
        XposedBridge.hookMethod(dispatch,new StrictHook(){@Override protected void before(MethodHookParam p){if((Integer)p.args[0]==TOKEN){toggle();p.setResult(null);}}});
        XposedBridge.hookMethod(streetMenu,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable())return;
            @SuppressWarnings("unchecked") List<Object> previous=(List<Object>)p.getResult();
            for(Object row:previous)if((Integer)getObjectField(row,"c")==TOKEN)return;
            ArrayList<Object> result=new ArrayList<>(previous);result.add(item());p.setResult(result);
        }});
        StrictHook append=new StrictHook(){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable()||!supports((Integer)p.args[0]))return;
            @SuppressWarnings("unchecked") List<Integer> previous=(List<Integer>)p.getResult();
            if(!previous.contains(TOKEN)){ArrayList<Integer> result=new ArrayList<>(previous);result.add(TOKEN);p.setResult(result);}
        }};
        XposedBridge.hookMethod(defaults,append);XposedBridge.hookMethod(candidates,append);
        XposedBridge.hookMethod(available,new StrictHook(){@Override protected void before(MethodHookParam p){
            int mode=(Integer)p.args[0];
            if(mode==182){
                @SuppressWarnings("unchecked") ArrayList<Integer> values=(ArrayList<Integer>)p.args[1];values.remove(Integer.valueOf(TOKEN));
                @SuppressWarnings("unchecked") List<Integer> saved=(List<Integer>)call(p.thisObject,"U",mode);
                if(saved.contains(TOKEN)){ArrayList<Integer> next=new ArrayList<>(saved);next.remove(Integer.valueOf(TOKEN));call(p.thisObject,"Y",next,false);}return;
            }
            if(!supports(mode))return;
            @SuppressWarnings("unchecked") ArrayList<Integer> values=(ArrayList<Integer>)p.args[1];if(!values.contains(TOKEN))values.add(TOKEN);
            @SuppressWarnings("unchecked") List<Integer> saved=(List<Integer>)call(p.thisObject,"U",mode);
            if(!saved.isEmpty()&&!saved.contains(TOKEN)){ArrayList<Integer> next=new ArrayList<>(saved);next.add(TOKEN);call(p.thisObject,"Y",next,false);}
        }});
        XposedBridge.hookMethod(editor,new StrictHook(){
            @Override protected void before(MethodHookParam p){
                int mode=(Integer)getObjectField(p.thisObject,"mCurrentMode");
                if(mode==182){@SuppressWarnings("unchecked") ArrayList<Integer> previous=(ArrayList<Integer>)p.args[0];ArrayList<Integer> values=new ArrayList<>(previous);values.remove(Integer.valueOf(TOKEN));p.args[0]=values;return;}
                if(!supports(mode))return;
                @SuppressWarnings("unchecked") ArrayList<Integer> values=(ArrayList<Integer>)p.args[0];
                if(!values.contains(TOKEN)){values=new ArrayList<>(values);values.add(TOKEN);p.args[0]=values;}
            }
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable()||!supports((Integer)getObjectField(p.thisObject,"mCurrentMode")))return;
                @SuppressWarnings("unchecked") List<Object> items=(List<Object>)getObjectField(p.thisObject,"mItems");
                for(Object row:items)if(String.valueOf(TOKEN).equals(getObjectField(row,"r")))return;
                Object row=allocate.invoke(unsafe,rowType);
                for(String name:new String[]{"c","d","e","f","g","i","k","l"})setIntField(row,name,-1);
                setObjectField(row,"r",String.valueOf(TOKEN));setIntField(row,"c",ICON);setIntField(row,"l",TITLE);setIntField(row,"j",0);
                @SuppressWarnings("unchecked") List<Integer> normalized=(List<Integer>)callStaticMethod(type("u2.y"),"c0",p.args[0]);
                items.add(Math.min(normalized.indexOf(TOKEN),items.size()),row);
            }
        });
        XposedBridge.hookMethod(componentWrite,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(!p.hasThrowable()&&boost.active()&&(masterType.isInstance(p.thisObject)||dolbyType.isInstance(p.thisObject))&&videoRestricted(mode()))boost.stop();
        }});
        StrictHook videoState=new StrictHook(){@Override protected void after(MethodHookParam p){
            if(!p.hasThrowable()&&Boolean.TRUE.equals(p.getResult())&&boost.active()&&(mode()==162||mode()==180))boost.stop();
        }};
        XposedBridge.hookMethod(masterEnabled,videoState);XposedBridge.hookMethod(dolbyEnabled,videoState);
        XposedBridge.hookMethod(modeChange,new StrictHook(){
            @Override protected void before(MethodHookParam p){
                int target=(Integer)call(p.args[0],"getTargetMode");
                if(boost.active()&&videoRestricted(target))boost.stop();
            }
            @Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&boost.active()&&videoRestricted(mode()))boost.stop();}
        });
        XposedBridge.hookMethod(settings,new StrictHook(1500){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())settings(p.thisObject);}});
        XposedBridge.hookMethod(resume,new StrictHook(){@Override protected void after(MethodHookParam p){if(cameraType.isInstance(p.thisObject))boost.activity((Activity)p.thisObject);}});
        XposedBridge.hookMethod(windowAttributes,new StrictHook(){@Override protected void before(MethodHookParam p){boost.attributes(((Activity)p.thisObject).getWindow(),(android.view.WindowManager.LayoutParams)p.args[0]);}});
        XposedBridge.hookMethod(pause,new StrictHook(){@Override protected void before(MethodHookParam p){if(cameraType.isInstance(p.thisObject))boost.stop();}});
        XposedBridge.hookMethod(focus,new StrictHook(){@Override protected void before(MethodHookParam p){boost.focus((Boolean)p.args[0]);}});
    }
}
