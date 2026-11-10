package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete the final native still-HDR list, including builders which bypass t(). */
public final class Hdr211Hooks {
    private Hdr211Hooks() {}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        final Method initialize=h.method("r2.A","w",h.type("com.android.camera.data.data.C"));
        final Method cycle=h.method("r2.A","m",int.class),items=h.method("r2.A","getItems");
        final Method value=h.method("r2.A","getComponentValue",int.class);
        final Method description=h.method("r2.A","s",int.class),icons=h.method("r2.A","o");
        final Field rawItems=h.field("com.android.camera.data.data.c","mItems");
        final Field inputMode=h.field("com.android.camera.data.data.C","a");
        final Field rowValue=h.field("com.android.camera.data.data.d","r");
        final Field image=h.field("com.android.camera.data.data.d","c"),selectedImage=h.field("com.android.camera.data.data.d","g");
        final Field shadow=h.field("com.android.camera.data.data.d","f"),selectedShadow=h.field("com.android.camera.data.data.d","h");
        final Field label=h.field("com.android.camera.data.data.d","l");
        final int onTitle=h.field("Rh.e","pref_simple_hdr_entry_on").getInt(null);
        final int onAccessibility=h.field("Rh.e","accessibility_hdr_on").getInt(null);
        final int onIcon=h.field("Rh.b","ic_new_config_hdr_normal").getInt(null);
        final Field provider=h.field("Y6.j","a");
        final Method themedIcon=h.method("Y6.k","J",String.class);
        final Class<?> rowType=h.type("com.android.camera.data.data.d");
        // Stock d has no constructor after R8 and inherits only Object state.
        // As in the existing style allocator, preserve its VM defaults and fields.
        if(rowType.getSuperclass()!=Object.class||rowType.getDeclaredConstructors().length!=0)
            throw new NoSuchMethodException("Stock HDR row allocation contract");
        final ArrayList<Field> rowFields=new ArrayList<>();
        for(Field field:rowType.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);rowFields.add(field);}
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");
        Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        final Object unsafe=singleton.get(null);
        final Method allocate=unsafeType.getDeclaredMethod("allocateInstance",Class.class);allocate.setAccessible(true);
        return new HookPlan(){
            public String id(){return "211.stock-hdr-on-branch";}
            public void install(){
                XposedBridge.hookMethod(initialize,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{p.setObjectExtra("213.hdr.previous",rawItems.get(p.thisObject));}
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||!still(inputMode.getInt(p.args[0])))return;
                        @SuppressWarnings("unchecked") List<Object> rows=(List<Object>)rawItems.get(p.thisObject);
                        // w returns before publishing when the native HDR capability is absent.
                        if(rows==p.getObjectExtra("213.hdr.previous")||rows==null)return;
                        Object off=null;boolean auto=false,normal=false;
                        for(Object row:rows){String entry=(String)rowValue.get(row);if("off".equals(entry))off=row;if("auto".equals(entry))auto=true;if("normal".equals(entry)||"on".equals(entry))normal=true;}
                        if(off==null||!auto)return;
                        String before=describe(rows);
                        if(!normal){
                            Object on=allocate.invoke(unsafe,rowType);
                            // Off and normal have identical native initialization defaults.
                            // Copy them, then assign exactly t()'s normal row resource fields.
                            for(Field field:rowFields)field.set(on,field.get(off));
                            int primary=((int[])icons.invoke(null))[0];
                            rowValue.set(on,"normal");image.setInt(on,primary);selectedImage.setInt(on,primary);
                            shadow.setInt(on,onIcon);selectedShadow.setInt(on,(Integer)themedIcon.invoke(provider.get(null),"normal"));label.setInt(on,onTitle);
                            ArrayList<Object> completed=new ArrayList<>(rows);completed.add(on);
                            rows=Collections.unmodifiableList(completed);rawItems.set(p.thisObject,rows);
                        }
                        Log.i("AuroraCameraCompat","hdr213 initialized mode="+inputMode.getInt(p.args[0])+" before="+before+" after="+describe(rows));
                    }
                    String describe(List<?> rows)throws IllegalAccessException{
                        ArrayList<String> result=new ArrayList<>();for(Object row:rows)result.add((String)rowValue.get(row));return result.toString();
                    }
                });
                XposedBridge.hookMethod(cycle,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        // Stock m() is a binary shortcut: off -> row 1 -> off,
                        // even when t() supplies three rows. Use the native list
                        // order only when the newly exposed normal row is present.
                        if(!still((Integer)p.args[0]))return;
                        List<?> rows=(List<?>)items.invoke(p.thisObject);
                        if(rows==null||rows.size()<3)return;
                        boolean normal=false;int current=-1;
                        String selected=(String)value.invoke(p.thisObject,p.args[0]);
                        for(int i=0;i<rows.size();i++){
                            String candidate=(String)rowValue.get(rows.get(i));
                            if("normal".equals(candidate))normal=true;
                            if(candidate.equals(selected))current=i;
                        }
                        if(normal)p.setResult(rowValue.get(rows.get((current+1)%rows.size())));
                    }
                });
                XposedBridge.hookMethod(description,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        // The native s() labels normal as AUTO when profile.U1 is true.
                        // Its value/consumer are still HDR=1; use the existing ON a11y label.
                        if(still((Integer)p.args[0])&&"normal".equals(value.invoke(p.thisObject,p.args[0])))p.setResult(onAccessibility);
                    }
                });
            }
        };
    }
    private static boolean still(int mode){
        // Native photo shutter modes (c0.a), professional photo, both street
        // modes, and the existing Legend capture port. This only selects the
        // consumer family: w() still has to publish off+auto before any addition.
        // A valid three-row list already supplied for these modes must also cycle.
        return mode==163||mode==167||mode==168||mode==171||mode==173||mode==175
                ||mode==182||mode==186||mode==205||mode==225||mode==228||mode==229
                ||mode==230||mode==231||mode==232||mode==256;
    }
}
