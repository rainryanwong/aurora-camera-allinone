package com.aurora.camera.compat;

import android.util.SparseArray;
import com.aurora.camera.compat.night.NightCamStyle213;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Native NightModule inherits Camera2Module aperture/filter consumers. */
public final class Night211Hooks {
    private Night211Hooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        final Method initialize=h.method("v2.m0","X",h.type("v2.J0$a")),filterRow=h.method("v2.m0","y");
        final Field mode=h.field("com.android.camera.data.data.c","mCurrentMode"),items=h.field("com.android.camera.data.data.c","mItems"),value=h.field("com.android.camera.data.data.d","r");
        final Field filters=h.field("v2.m0","l"),kind=h.field("v2.m0","i"),selected=h.field("v2.m0","c"),defaults=h.field("v2.m0","b");
        final Method repository=h.method("g2.a","j"),get=h.method("Xh.b","x",Class.class);
        final Class<?> aperture=h.type("v2.k"),shine=h.type("v2.m0");
        final Field supported=h.field(aperture,"W"),factory=h.field("y3.d","f"),id=h.field("b5.i","c");
        final Method build=h.method("X9.w5","E",int.class);
        final Method top=h.method("b4.a","e"),extra=h.method("b4.a","l"),floating=h.method("b4.a","f");
        final Method menu=h.method("u2.y","T",int.class,ArrayList.class);
        final Method editor=h.method("u2.y","Y",ArrayList.class,boolean.class);
        final Method cloud=h.method("v2.U","d",int.class,Map.class);
        final Class<?> captureFilter=h.type("r2.t");
        final NightCamStyle213 camStyle=new NightCamStyle213(h);
        return new HookPlan(){
            public String id(){return "211.night-aperture-and-filters";}
            public void install(){
                camStyle.install();
                XposedBridge.hookMethod(cloud,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void before(MethodHookParam p){
                        if((Integer)p.args[0]!=173||!captureFilter.isInstance(p.thisObject))return;
                        // The native lookup otherwise misses 173 and falls back to the small
                        // built-in list. Share photo's existing catalog only for this call;
                        // keep mode=173 for native camera filtering, mapping and preferences.
                        Map<Integer,?> original=(Map<Integer,?>)p.args[1];
                        Object photo=original.get(163);
                        if(original.get(173)!=null||photo==null)return;
                        HashMap<Integer,Object> catalog=new HashMap<>(original);catalog.put(173,photo);p.args[1]=catalog;
                    }
                });
                XposedBridge.hookMethod(initialize,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||mode.getInt(p.thisObject)!=173)return;
                        List<Object> previous=(List<Object>)items.get(p.thisObject);ArrayList<Object> rows=new ArrayList<>(previous);
                        boolean found=false;for(Object row:rows)if("7".equals(value.get(row))){found=true;break;}
                        if(!found)rows.add(filterRow.invoke(null));items.set(p.thisObject,rows);filters.setBoolean(p.thisObject,true);
                        if(kind.getInt(p.thisObject)==-1)kind.setInt(p.thisObject,5);
                        if(selected.get(p.thisObject)==null){selected.set(p.thisObject,"7");((SparseArray<Object>)defaults.get(p.thisObject)).put(kind.getInt(p.thisObject),"7");}
                    }
                });
                StrictHook configRows=new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable())return;ArrayList<Object> result=new ArrayList<>((List<Object>)p.getResult());
                        for(int i=result.size()-1;i>=0;i--)if(id.getInt(result.get(i))==212)result.remove(i);
                        if(supported.getBoolean(get.invoke(repository.invoke(null),aperture)))append(result,0xd40);
                        p.setResult(result);
                    }
                    void append(ArrayList<Object> rows,int target)throws Throwable{
                        for(Object row:rows)if(id.getInt(row)==target)return;rows.add(build.invoke(null,target));
                    }
                };
                XposedBridge.hookMethod(top,configRows);XposedBridge.hookMethod(extra,configRows);
                XposedBridge.hookMethod(floating,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable())return;ArrayList<Object> result=new ArrayList<>((List<Object>)p.getResult());
                        result.add(camStyle.bottom(factory.get(p.thisObject)));p.setResult(result);
                    }
                });
                XposedBridge.hookMethod(menu,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void before(MethodHookParam p)throws Throwable{
                        if((Integer)p.args[0]!=173)return;ArrayList<Integer> available=(ArrayList<Integer>)p.args[1];
                        if(supported.getBoolean(get.invoke(repository.invoke(null),aperture))&&!available.contains(0xd40))available.add(0xd40);
                    }
                });
                XposedBridge.hookMethod(editor,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void before(MethodHookParam p)throws Throwable{
                        if(mode.getInt(p.thisObject)!=173)return;
                        // Y also constructs candidates directly from native/cached tokens,
                        // without passing through the mode's available-list migration.
                        ArrayList<Integer> candidates=new ArrayList<>((List<Integer>)p.args[0]);
                        candidates.removeAll(java.util.Collections.singleton(212));p.args[0]=candidates;
                    }
                });
            }
        };
    }
}
