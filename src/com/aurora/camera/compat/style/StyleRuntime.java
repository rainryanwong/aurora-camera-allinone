package com.aurora.camera.compat.style;

import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Fixed, eagerly resolved stock-1770 links for F25. No discovery/fallback. */
public final class StyleRuntime {
    public final Host host;
    private final Map<String, Method> methods = new HashMap<>();
    private final Map<String, Field> fields = new HashMap<>();
    private final Map<String, Constructor<?>> constructors = new HashMap<>();
    private final Object unsafe;
    private final Method allocate;

    public StyleRuntime(Host host) throws ReflectiveOperationException {
        this.host = host;
        link("style.init","r2.m","u","void","com.android.camera.data.data.C");
        link("style.default","r2.m","getDefaultValue","java.lang.String","int");
        link("style.value","r2.m","getComponentValue","java.lang.String","int");
        link("style.set","r2.m","setComponentValue","void","int","java.lang.String");
        link("style.empty","com.android.camera.data.data.c","isEmpty","boolean");
        link("style.supported","r2.m","w","boolean");
        link("style.active","r2.m","t","boolean","int");
        link("style.o","r2.m","o","int");
        link("style.p","r2.m","p","int","java.lang.String");
        link("style.r","r2.m","r","int","java.lang.String");
        link("style.icon","r2.m","q","int","int","java.lang.String");
        link("cap.resources","l9.h","E2","boolean","l9.g");
        link("raw","com.android.camera.data.data.n","a0","boolean","int");
        link("ultraRaw","com.android.camera.data.data.n","p0","boolean","int","l9.g");
        link("repo.a","g2.a","a","r2.n1");
        link("repo.g","g2.a","g","u2.Y");
        link("repo.j","g2.a","j","v2.F0");
        link("repo.get","Xh.b","x","java.lang.Object","java.lang.Class");
        link("prefs.bool","Xh.a","h","boolean","java.lang.String","boolean");
        link("property.bool","miuix.core.util.SystemProperties","getBoolean","boolean","java.lang.String","boolean");
        link("settings.add","com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","addCurrentPreferences","void");
        link("fragment.activity","androidx.fragment.app.Fragment","requireActivity","androidx.fragment.app.m");
        link("fragment.resources","androidx.fragment.app.Fragment","getResources","android.content.res.Resources");
        link("fragment.string","androidx.fragment.app.Fragment","getString","java.lang.String","int");
        link("context.string","android.content.Context","getString","java.lang.String","int");
        link("dropdown.bind","Xl.a","invoke","java.lang.Object","java.lang.Object");
        link("editor.item","u2.y","G","gv.A","u2.y","java.util.List","r2.m");
        link("pref.find","androidx.preference.PreferenceGroup","k0","androidx.preference.Preference","java.lang.CharSequence");
        link("pref.key","androidx.preference.Preference","a0","void","java.lang.String");
        link("pref.title","androidx.preference.Preference","e0","void","java.lang.String");
        link("pref.summary","androidx.preference.Preference","c0","void","java.lang.CharSequence");
        link("pref.checked","androidx.preference.TwoStatePreference","setChecked","void","boolean");
        link("pref.add","androidx.preference.PreferenceGroup","j0","boolean","androidx.preference.Preference");
        field("pref.order","androidx.preference.Preference","g","int");
        ctor("preference","com.android.camera.preferences.AccessibleCheckBoxPreference","androidx.fragment.app.m");
        link("menu.enabled","H4.p0","us","boolean","int");
        for(String name:new String[]{"W","R","S","U","P"})link("screen."+name,"K2.b",name,"boolean");
        link("screen.z","K2.b","z","int");
        link("device.special","Pe.c","d","boolean");
        link("ai.ks","com.android.camera.features.mode.ai.FragmentAi","Ks","boolean");
        link("ai.rt","com.android.camera.features.mode.ai.FragmentAi","rt","void","boolean");
        link("ai.se","com.android.camera.features.mode.ai.FragmentAi","Se","void","boolean");
        link("ai.st","com.android.camera.features.mode.ai.FragmentAi","st","void","boolean","boolean");
        link("ai.view","com.android.camera.features.mode.ai.FragmentAi","updateView","void","android.view.View","android.os.Bundle");
        link("ai.panel","com.android.camera.features.mode.ai.FragmentAi","pt","boolean","com.android.camera.features.mode.ai.FragmentAi","boolean");
        link("style.view","p5.u","initView","void","android.view.View");
        link("view.layout","android.view.View","setLayoutParams","void","android.view.ViewGroup$LayoutParams");
        link("preview","m6.n","m","int","boolean");
        link("debug.b","z3.d","b","java.util.ArrayList");
        link("debug.e","z3.d","e","java.util.ArrayList");
        link("debug.f","z3.d","f","java.util.List");
        link("debug.h","z3.d","h","android.util.SparseArray");
        link("debug.a","z3.d","a","java.util.List");
        link("debug.l","y3.d","l","java.util.ArrayList");
        link("menu.item","X9.w5","E","b5.i","int");
        link("menu.n","y3.d","n","void","int","int[]");
        link("camera.repo","v6.f","R","v6.f");
        link("camera.cap","v6.f","N","l9.g");
        link("camera.id","v6.f","e","int");
        link("camera.forId","v6.f","M","l9.g","int");
        link("request.d0","com.android.camera.data.data.F","d0","boolean","int","l9.g");
        link("prefs.R","u2.Y","R","boolean");
        link("prefs.L","u2.Y","L","boolean");
        link("request.z0","com.android.camera.data.data.x","z0","boolean");
        link("raw.support","r2.U","p","boolean","int");
        link("manual.q","v2.j0","q","boolean","int");
        link("manual.n","v2.i0","n","boolean");
        link("beauty.f0","v2.m0","f0","boolean");
        link("beauty.e0","v2.m0","e0","boolean");
        link("cap.m5","l9.h","m5","boolean","l9.g");
        link("pe.j1","Pe.b","j1","void");
        for(String n:new String[]{"F0","a2","Z1"})link("pe."+n,"Pe.b",n,"boolean");
        link("ui.a","Z4.m","a","Z4.g");
        link("ui.i","Z4.m","i","Z4.g","int");
        link("builder.g","Z4.g$a","f","Z4.g");
        link("builder.f","Z4.f$a","f","Z4.c");
        link("icon.provider","q9.b","n","r9.y");
        link("icon.resource","r9.y","a","int","int");
        link("beauty.app","i2.b","a","j2.k");
        link("beauty.external","j2.k","a","boolean");
        link("orientation.rect","com.android.camera.data.data.F","i","android.graphics.Rect");
        ctor("click.raw","X9.V2","int");
        ctor("click.normal","X9.o1","int");

        field("mode","com.android.camera.data.data.c","mCurrentMode","int");
        field("fragment.mode","com.android.camera.fragment.h","mCurrentMode","int");
        field("items","com.android.camera.data.data.c","mItems","java.util.List");
        field("cap","r2.m","b","l9.g");
        field("style.portrait","r2.m","c","boolean");
        for(String n:new String[]{"c","g","h","j"})field("item."+n,"com.android.camera.data.data.d",n,"int");
        field("input.mode","com.android.camera.data.data.C","a","int");
        field("input.front","com.android.camera.data.data.C","b","int");
        field("input.cap","com.android.camera.data.data.C","c","l9.g");
        field("item.value","com.android.camera.data.data.d","r","java.lang.String");
        field("item.title","com.android.camera.data.data.d","l","int");
        field("item.text","com.android.camera.data.data.d","o","java.lang.String");
        field("settings.group","com.android.camera.fragment.settings.b","mPreferenceGroup","androidx.preference.PreferenceScreen");
        field("pref.t","androidx.preference.Preference","t","boolean");
        field("pref.K","androidx.preference.Preference","K","java.lang.Object");
        field("pref.e","androidx.preference.Preference","e","androidx.preference.Preference$c");
        field("ai.g","com.android.camera.features.mode.ai.FragmentAi","g","android.widget.LinearLayout");
        field("ai.H","com.android.camera.features.mode.ai.FragmentAi","H","android.view.View");
        field("style.component","p5.u","b","com.android.camera.data.data.c");
        field("dropdown.kind","Xl.a","a","int");
        field("dropdown.owner","Xl.a","b","java.lang.Object");
        field("editor.cap","com.android.camera.data.data.c","mCapabilities","l9.g");
        field("preview.state","m6.n","E","m6.n$a");
        field("preview.mode","m6.n$a","d","int");
        field("preview.front","m6.n$a","l","int");
        field("debug.original","z3.d","i","boolean");
        field("menu.map","y3.d","b","android.util.SparseArray");
        field("menu.factory","y3.d","f","Z4.m");
        field("manual.M","v2.F0","M","boolean");
        field("pe.instance","Pe.b$b","a","Pe.b");
        field("pe.device","Pe.b","e",null);
        Class<?> device = fields.get("pe.device").getType();
        Method k3 = host.method(device,"k3");
        if(k3.getReturnType()!=boolean.class)throw new NoSuchMethodException("device.k3 boolean");
        methods.put("device.k3",k3);
        field("icon.instance","q9.a","a","q9.b");
        field("external.instance","i2.a","a","i2.b");
        for(String n:new String[]{"c","d","g","o"})field("builder."+n,"Z4.a$a",n,"int");
        for(String n:new String[]{"j","k","l"})field("builder."+n,"Z4.a$a",n,"boolean");
        field("builder.listener","Z4.a$a","a","android.view.View$OnClickListener");
        field("builder.layout","Z4.c$a","t","int");
        field("builder.binding","Z4.c$a","u","Z4.c$b");

        // Three R8 concrete classes have no constructor: two builders invoke
        // only their parent constructor; the A.b callback invokes Object.<init>.
        for(String n:new String[]{"Z4.g$a","Z4.f$a","A.b"}) {
            if(host.type(n).getDeclaredConstructors().length!=0)
                throw new NoSuchMethodException("Unexpected R8 constructor shape for "+n);
        }
        if(host.type("Z4.g$a").getSuperclass()!=host.type("Z4.a$a")
                ||host.type("Z4.f$a").getSuperclass()!=host.type("Z4.c$a")
                ||host.type("Z4.c$a").getSuperclass()!=host.type("Z4.a$a")
                ||host.type("A.b").getSuperclass()!=Object.class
                ||host.type("A.b").getDeclaredFields().length!=0)
            throw new NoSuchFieldException("Style R8 allocator hierarchy/zero-field callback contract changed");
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");
        Field singleton=unsafeClass.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        unsafe=singleton.get(null);
        allocate=unsafeClass.getDeclaredMethod("allocateInstance",Class.class);
        allocate.setAccessible(true);
    }

    public Method method(String key){return methods.get(key);}
    public Field field(String key){return fields.get(key);}
    public Object get(String key,Object receiver)throws IllegalAccessException{return fields.get(key).get(receiver);}
    public int integer(String key,Object receiver)throws IllegalAccessException{return fields.get(key).getInt(receiver);}
    public boolean boolField(String key,Object receiver)throws IllegalAccessException{return fields.get(key).getBoolean(receiver);}
    public void set(String key,Object receiver,Object value)throws IllegalAccessException{fields.get(key).set(receiver,value);}
    public Object call(String key,Object receiver,Object... args)throws Throwable{return invoke(methods.get(key),receiver,args);}
    public boolean bool(String key,Object receiver,Object...args)throws Throwable{return (Boolean)call(key,receiver,args);}
    public Object component(String repo,String name)throws Throwable{return call("repo.get",call(repo,null),host.type(name));}
    public Object construct(String key,Object... args)throws Throwable{
        try{return constructors.get(key).newInstance(args);}catch(InvocationTargetException e){throw e.getCause();}
    }
    public Object builder(boolean floating,int id)throws Throwable{
        Object object=invoke(allocate,unsafe,host.type(floating?"Z4.f$a":"Z4.g$a"));
        // Exact entirety of Z4/a$a.<init>(int): Object ctor + k/l=true + c=id.
        set("builder.k",object,true);set("builder.l",object,true);set("builder.c",object,id);
        return object;
    }
    public Object statelessBinding()throws Throwable{return invoke(allocate,unsafe,host.type("A.b"));}
    public boolean debug()throws Throwable{
        boolean fallback=bool("property.bool",null,"camera.debug.ai_mode",false);
        return bool("prefs.bool",call("repo.g",null),"pref_aurora_ai_debug_controls",fallback);
    }
    public Object style()throws Throwable{return component("repo.a","r2.m");}
    public Class<?> type(String name)throws ClassNotFoundException{return host.type(name);}

    private void link(String key,String owner,String name,String result,String... args)throws ReflectiveOperationException{
        Class<?>[] parameters=new Class<?>[args.length];for(int i=0;i<args.length;i++)parameters[i]=resolveType(args[i]);
        Method method=host.method(owner,name,parameters);
        if(method.getReturnType()!=resolveType(result))throw new NoSuchMethodException("Style return contract "+key+": "+method);
        methods.put(key,method);
    }
    private void field(String key,String owner,String name,String expected)throws ReflectiveOperationException{
        Field f=host.field(owner,name);
        if(expected!=null&&f.getType()!=resolveType(expected))throw new NoSuchFieldException("Style field contract "+key);
        fields.put(key,f);
    }
    private void ctor(String key,String owner,String... args)throws ReflectiveOperationException{
        Class<?>[] parameters=new Class<?>[args.length];for(int i=0;i<args.length;i++)parameters[i]=resolveType(args[i]);
        Constructor<?> c=host.type(owner).getDeclaredConstructor(parameters);c.setAccessible(true);constructors.put(key,c);
    }
    private Class<?> resolveType(String name)throws ClassNotFoundException{
        if("int".equals(name))return int.class;if("boolean".equals(name))return boolean.class;
        if("void".equals(name))return void.class;if("int[]".equals(name))return int[].class;
        return host.type(name);
    }
    private static Object invoke(Method method,Object receiver,Object... args)throws Throwable{
        try{return method.invoke(receiver,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
}
