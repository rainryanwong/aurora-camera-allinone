package com.aurora.camera.compat.ev;

import com.aurora.camera.compat.Host;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Four accepted AuroraBottomEv helpers, calling the original component/factory. */
public final class BottomEvPort {
    public final Class<?> modeUi,modeProtocol,entry;
    public final Method apply,menu,click,factory,peY;
    private final Method family,evX,evU,prefs,L,R,repo,get,blocked,module,J1,Z,y;
    private final Field lambdaCase,currentMode,context,kind,tip;

    public BottomEvPort(Host host)throws ReflectiveOperationException {
        modeUi=host.type("y3.d");modeProtocol=host.type("y3.t");entry=host.type("Z4.a");
        apply=host.method("M9.f","apply",Object.class);menu=host.method(modeUi,"k");
        click=host.method("com.android.camera.features.mode.capture.c1","onClick",host.type("android.view.View"));
        factory=host.method("com.android.camera.features.mode.capture.e1","a",host.type("android.content.Context"),int.class);
        peY=host.method("Pe.b","Y");family=host.method("com.android.camera.module.c0","l",int.class);
        evX=host.method("r2.F0","x",int.class);evU=host.method("r2.F0","u",int.class);
        prefs=host.method("g2.a","g");L=host.method("u2.Y","L");R=host.method("u2.Y","R");
        repo=host.method("g2.a","a");get=host.method("Xh.b","x",Class.class);
        blocked=host.method("r2.F0","t",int.class);module=host.method("y3.s","getModuleId");
        J1=host.method("com.android.camera.data.data.i","J1");Z=host.method("com.android.camera.data.data.x","Z");y=host.method("K2.g","y");
        lambdaCase=host.field("M9.f","a");currentMode=host.field("com.android.camera.module.c0","a");
        context=host.field(modeUi,"a");kind=host.field(entry,"e");tip=host.field("b5.i","c");
        if(menu.getReturnType()!=ArrayList.class||factory.getReturnType()!=host.type("Z4.c")
                ||lambdaCase.getType()!=int.class||currentMode.getType()!=int.class
                ||kind.getType()!=int.class||tip.getType()!=int.class)
            throw new NoSuchMethodException("BottomEV table/factory/item contract");
        for(Method value:new Method[]{peY,family,evX,evU,L,R,blocked,J1,Z,y})if(value.getReturnType()!=boolean.class)
            throw new NoSuchMethodException("BottomEV boolean contract "+value);
    }

    public boolean owns(int mode)throws Throwable {
        if(!(Boolean)call(family,null,mode)&&mode!=163&&mode!=162&&mode!=175&&mode!=171&&mode!=173
                &&mode!=256&&mode!=232&&mode!=227&&mode!=228)return false;
        return (Boolean)call((Boolean)call(peY,null)?evX:evU,null,mode);
    }
    public boolean available(int mode)throws Throwable {
        if(!owns(mode))return false;
        Object data=call(prefs,null);if(!(Boolean)call(L,data)||!(Boolean)call(R,data))return false;
        Object ev=call(get,call(repo,null),blocked.getDeclaringClass());
        return ev!=null&&!(Boolean)call(blocked,ev,mode);
    }
    public boolean modeUiLambda(Object lambda,Object argument)throws IllegalAccessException {
        // The actual packed-switch has exactly seven alternatives, values0..6.
        int branch=lambdaCase.getInt(lambda);
        return (branch<0||branch>=7)&&modeProtocol.isInstance(argument);
    }
    public boolean clickAllowed()throws Throwable {
        if((Boolean)call(peY,null))return true;
        return available(currentMode.getInt(null))&&(Boolean)call(J1,null)
                &&!(Boolean)call(Z,null)&&!(Boolean)call(y,null);
    }
    public Object ensure(Object value,Object owner)throws Throwable {
        if(!modeUi.isInstance(owner))return value;
        int mode=(Integer)call(module,owner);if(!available(mode))return value;
        List<?> original=(List<?>)value;
        if(original!=null)for(Object item:original)if(entry.isInstance(item)&&kind.getInt(item)==5)return value;
        Object ev=call(factory,null,context.get(owner),mode);if(ev==null)return value;
        ArrayList<Object> result=new ArrayList<>();result.add(ev);if(original!=null)result.addAll(original);return result;
    }
    public void withoutOldTip(Object value,Object owner)throws Throwable {
        if(!owns((Integer)call(module,owner)))return;
        Iterator<?> iterator=((ArrayList<?>)value).iterator();
        while(iterator.hasNext())if(tip.getInt(iterator.next())==0xb26)iterator.remove();
    }
    public static Object call(Method method,Object receiver,Object...args)throws Throwable {
        try{return method.invoke(receiver,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
}
