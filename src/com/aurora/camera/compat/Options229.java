package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import de.robv.android.xposed.XposedHelpers;

/** Small settings adapter for the pinned 1770 Preference API. */
public final class Options229 {
    public static final String SOUND="aurora_always_shutter_229", VIDEO="aurora_video_lut_quality_229";
    private final Host host;
    public Options229(Host host){this.host=host;}
    public Context context(){return (Context)XposedHelpers.callStaticMethod(type("com.xiaomi.camera.basic.Global"),"getContext");}
    public SharedPreferences prefs(){return context().getSharedPreferences("aurora_features229",Context.MODE_PRIVATE);}
    public boolean enabled(String key){Context c=context();return c!=null&&c.getSharedPreferences("aurora_features229",0).getBoolean(key,false);}
    public Class<?> type(String name){try{return host.type(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
    public interface Change {boolean set(boolean value)throws Throwable;}
    public Object addAction(Object fragment,String key,String title,String summary,Runnable action){
        return addAction(fragment,key,title,summary,null,action);
    }
    public Object addAction(Object fragment,String key,String title,String summary,String after,Runnable action){
        Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");Object old=XposedHelpers.callMethod(root,"k0",key);if(old!=null)return old;
        Object target=after==null?root:parent(root,after);
        if(target==null)throw new IllegalStateException("Required preference anchor missing: "+after);
        Context context=(Context)XposedHelpers.callMethod(fragment,"requireContext");Object row=XposedHelpers.newInstance(type("miuix.preference.TextPreference"),context,null);
        XposedHelpers.callMethod(row,"a0",key);XposedHelpers.callMethod(row,"e0",title);XposedHelpers.callMethod(row,"c0",summary);XposedHelpers.setBooleanField(row,"t",false);
        if(after!=null){Object anchor=XposedHelpers.callMethod(target,"k0",after);XposedHelpers.setIntField(row,"g",(Integer)XposedHelpers.getObjectField(anchor,"g")+1);}
        Class<?> listener=type("androidx.preference.Preference$d");XposedHelpers.setObjectField(row,"f",Proxy.newProxyInstance(host.loader,new Class<?>[]{listener},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return objectMethod(proxy,method.getName(),args);
            if(!"onPreferenceClick".equals(method.getName()))throw new IllegalStateException(method.toString());action.run();return true;
        }));XposedHelpers.callMethod(target,"j0",row);return row;
    }
    public Object addSwitch(Object fragment,String key,String title,String summary,String after,Change change){
        Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
        Object old=XposedHelpers.callMethod(root,"k0",key);
        if(old!=null){XposedHelpers.callMethod(old,"setChecked",enabled(key));return old;}
        Context context=(Context)XposedHelpers.callMethod(fragment,"requireContext");
        Object row=XposedHelpers.newInstance(type("androidx.preference.SwitchPreference"),context,null);
        XposedHelpers.callMethod(row,"a0",key);XposedHelpers.callMethod(row,"e0",title);XposedHelpers.callMethod(row,"c0",summary);
        XposedHelpers.setBooleanField(row,"t",false);XposedHelpers.callMethod(row,"setChecked",enabled(key));
        Class<?> listener=type("androidx.preference.Preference$c");
        XposedHelpers.setObjectField(row,"e",Proxy.newProxyInstance(host.loader,new Class<?>[]{listener},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class)return objectMethod(proxy,method.getName(),args);
            if(!"onPreferenceChange".equals(method.getName()))throw new IllegalStateException(method.toString());
            boolean value=Boolean.TRUE.equals(args[1]);
            if(change!=null)return change.set(value);
            return prefs().edit().putBoolean(key,value).commit();
        }));
        Object parent=after==null?root:parent(root,after);
        if(parent==null)throw new IllegalStateException("Required preference anchor missing: "+after);
        if(after==null)XposedHelpers.callMethod(parent,"j0",row);
        else{
            ArrayList<?> children=new ArrayList<>((ArrayList<?>)XposedHelpers.getObjectField(parent,"g0"));
            for(Object child:children)XposedHelpers.callMethod(parent,"n0",child);
            int order=0;for(Object child:children){
                XposedHelpers.setIntField(child,"g",order++);XposedHelpers.callMethod(parent,"j0",child);
                if(after.equals(XposedHelpers.getObjectField(child,"m"))){XposedHelpers.setIntField(row,"g",order++);XposedHelpers.callMethod(parent,"j0",row);}
            }
        }
        return row;
    }
    private Object parent(Object group,String key){
        // Stock 1770: g0 is the child list; f0 is the group's Handler.
        for(Object row:new ArrayList<>((ArrayList<?>)XposedHelpers.getObjectField(group,"g0"))){
            if(key.equals(XposedHelpers.getObjectField(row,"m")))return group;
            if(type("androidx.preference.PreferenceGroup").isInstance(row)){Object found=parent(row,key);if(found!=null)return found;}
        }
        return null;
    }
    public static Object objectMethod(Object proxy,String name,Object[] args){
        if("hashCode".equals(name))return System.identityHashCode(proxy);
        if("equals".equals(name))return proxy==args[0];
        if("toString".equals(name))return "AuroraPreference229";
        throw new IllegalStateException(name);
    }
}
