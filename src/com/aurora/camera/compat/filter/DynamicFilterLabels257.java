package com.aurora.camera.compat.filter;

import android.view.View;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Apply the component row's native string-name contract to filter text and accessibility. */
final class DynamicFilterLabels257 {
    private final Method bind,label,accessibility,describe,selection,findHolder;
    private final Field resourceName,labelName,adapterRows,adapterOwner,panelRows,panelView,view;
    DynamicFilterLabels257(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        bind=host.method("com.android.camera.fragment.I","m",host.type("com.android.camera.data.data.d"));
        label=host.method("com.android.camera.fragment.I","h",String.class);
        accessibility=host.method("com.android.camera.fragment.d","x",View.class,int.class,boolean.class,boolean.class);
        describe=host.method("com.android.camera.fragment.d","y",View.class,String.class,host.type("com.android.camera.fragment.d$e"),boolean.class);
        selection=host.method("P9.i","Zs",int.class,int.class);
        findHolder=host.method("androidx.recyclerview.widget.RecyclerView","findViewHolderForAdapterPosition",int.class);
        resourceName=host.field("com.android.camera.data.data.d","l");labelName=host.field("com.android.camera.data.data.d","o");
        adapterRows=host.field("com.android.camera.fragment.d","f");adapterOwner=host.field("com.android.camera.fragment.d","e");
        panelRows=host.field("P9.i","R");panelView=host.field("P9.i","M");
        view=host.field("androidx.recyclerview.widget.RecyclerView$C","itemView");
    }
    private String name(Object row)throws IllegalAccessException{
        return resourceName.getInt(row)==-1?(String)labelName.get(row):null;
    }
    void install(){
        // I.m otherwise clears l == -1 rows, even though d.u reads their o name.
        XposedBridge.hookMethod(bind,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            String name=name(p.args[0]);if(name==null)return;
            label.invoke(p.thisObject,name);p.setResult(null);
        }});
        // Both normal binding and selecting a row call x. Use its native
        // string-name counterpart, including the selected announcement.
        XposedBridge.hookMethod(accessibility,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            View item=(View)p.args[0];if(item==null||!(item.getTag() instanceof Integer))return;
            Object row=((List<?>)adapterRows.get(p.thisObject)).get((Integer)item.getTag());
            String name=name(row);if(name==null)return;
            describe.invoke(null,item,name,adapterOwner.get(p.thisObject),(Boolean)p.args[2]);p.setResult(null);
        }});
        // Zs resets the previous row directly, without going through x.
        XposedBridge.hookMethod(selection,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;
            List<?> rows=(List<?>)panelRows.get(p.thisObject);int previous=(Integer)p.args[0];
            Object list=panelView.get(p.thisObject);if(rows==null||list==null||previous<0||previous>=rows.size()||previous==(Integer)p.args[1])return;
            String name=name(rows.get(previous));if(name==null)return;
            Object holder=findHolder.invoke(list,previous);if(holder!=null)((View)view.get(holder)).setContentDescription(name);
        }});
    }
}
