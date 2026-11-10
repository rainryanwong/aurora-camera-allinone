package com.aurora.camera.compat.editor;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Keeps cloud Leica filters on their native path while admitting LCC device entries. */
public final class EditorLeicaFilters292 {
    private final Constructor<?> cloudItem;

    public EditorLeicaFilters292(Host host) throws ReflectiveOperationException {
        List<Class<?>> parameters=new ArrayList<>(Arrays.asList(int.class,int.class,String.class,int.class,
                String.class,String.class,String.class,String.class,String.class,String.class,
                List.class,List.class,List.class,long.class,long.class));
        if(host.usesNativeCloudFilter())parameters.add(int.class);
        cloudItem=host.type(host.usesNativeCloudFilter()?"nz.d":"ud.c")
                .getDeclaredConstructor(parameters.toArray(new Class<?>[0]));
        cloudItem.setAccessible(true);
    }

    public void install() {
        XposedBridge.hookMethod(cloudItem,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                if((Integer)p.args[0]!=4)return;
                @SuppressWarnings("unchecked") List<String> devices=(List<String>)p.args[11];
                if(!devices.contains("lcc_support_devices"))return;
                List<String> supported=new ArrayList<>(devices);
                for(int i=0;i<supported.size();i++)
                    if("lcc_support_devices".equals(supported.get(i)))supported.set(i,"*");
                p.args[11]=supported;
            }
        });
    }
}
