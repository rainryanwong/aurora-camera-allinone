package com.aurora.camera.compat.street;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** Reuse native shutter entries and the three implemented equipment layouts. */
public final class StreetControls220 {
    private static final int SHUTTER=266;
    private final Class<?> equipment;
    private final Method top,extra,migrate,editor,build,ratioItems,ratioValue,ratioDefault,data,mode;
    private final Field itemId,itemValue,componentMode,intent;
    private final Map<Object,String> normalized=Collections.synchronizedMap(new WeakHashMap<>());
    public StreetControls220(Host h)throws ReflectiveOperationException {
        equipment=h.type("com.android.camera.features.mode.equipstreet.a");
        top=h.method("y3.d","e");extra=h.method(equipment,"l");build=h.method("X9.w5","E",int.class);itemId=h.field("b5.i","c");
        migrate=h.method("u2.y","T",int.class,ArrayList.class);editor=h.method("u2.y","Y",ArrayList.class,boolean.class);componentMode=h.field("com.android.camera.data.data.c","mCurrentMode");
        ratioItems=h.method("r2.T","getItems");ratioValue=h.method("r2.T","getComponentValue",int.class);itemValue=h.field("com.android.camera.data.data.d","r");
        ratioDefault=h.method("r2.T","getDefaultValue",int.class);
        data=h.method("g2.a","g");mode=h.method("u2.Y","D",int.class);intent=h.field("u2.Y","u");
    }
    public static boolean supportedRatio(String value){return "4x3".equals(value)||"3x2".equals(value)||"1x1".equals(value);}
    private int current()throws Exception {Object state=data.invoke(null);return (Integer)mode.invoke(state,intent.getInt(state));}
    private ArrayList<Object> withShutter(List<?> original)throws Exception {
        ArrayList<Object> result=new ArrayList<>(original);for(Object row:result)if(itemId.getInt(row)==SHUTTER)return result;
        result.add(build.invoke(null,SHUTTER));return result;
    }
    private ArrayList<Integer> tokens(Object original){
        @SuppressWarnings("unchecked") ArrayList<Integer> result=new ArrayList<>((List<Integer>)original);
        if(!result.contains(SHUTTER))result.add(SHUTTER);return result;
    }
    public void install(){
        XposedBridge.hookMethod(top,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable()&&equipment.isInstance(p.thisObject))p.setResult(withShutter((List<?>)p.getResult()));}});
        XposedBridge.hookMethod(extra,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())p.setResult(withShutter((List<?>)p.getResult()));}});
        XposedBridge.hookMethod(migrate,new StrictHook(){@Override protected void before(MethodHookParam p){if((Integer)p.args[0]==229)p.args[1]=tokens(p.args[1]);}});
        XposedBridge.hookMethod(editor,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(componentMode.getInt(p.thisObject)==229)p.args[0]=tokens(p.args[0]);}});
        XposedBridge.hookMethod(ratioItems,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||current()!=229)return;ArrayList<Object> safe=new ArrayList<>();
            for(Object row:(List<?>)p.getResult())if(supportedRatio((String)itemValue.get(row)))safe.add(row);
            p.setResult(safe);
        }});
        XposedBridge.hookMethod(ratioValue,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||(Integer)p.args[0]!=229)return;String value=(String)p.getResult();
            if(supportedRatio(value)){normalized.remove(p.thisObject);return;}
            String fallback=(String)ratioDefault.invoke(p.thisObject,229);
            if(!supportedRatio(fallback))throw new IllegalStateException("Native equipment default has no implemented layout: "+fallback);
            if(!value.equals(normalized.put(p.thisObject,value)))android.util.Log.i("AuroraCameraCompat","street220 ratio "+value+" uses native equipment default "+fallback+"; native225 stored value preserved");
            // Native 225 and 229 share their preference key. Normalize only the
            // 229 read; exiting to native street keeps its 9:16/Full selection.
            p.setResult(fallback);
        }});
    }
}
