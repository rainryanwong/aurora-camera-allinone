package com.aurora.camera.compat.ui;

import android.os.Bundle;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Preserve the stock FragmentManager back stack inside singleTop settings. */
public final class AuthNavigation241 {
    public static final String PAGE="aurora_auth_page239",EXPERIMENTAL="aurora_auth_experimental239";
    public static final String TAG="aurora_auth_experimental241";
    private final Constructor<?> fragment,transaction;
    private final Method arguments,setArguments,manager,replace,commit;
    private final Field addToBackStack,backStackName;
    public AuthNavigation241(Host host)throws ReflectiveOperationException{
        Class<?> fragmentType=host.type("androidx.fragment.app.Fragment");
        Class<?> managerType=host.type("androidx.fragment.app.FragmentManager");
        fragment=host.type("com.android.camera.fragment.settings.CameraPreferenceFragment").getDeclaredConstructor();fragment.setAccessible(true);
        transaction=host.type("androidx.fragment.app.a").getDeclaredConstructor(managerType);transaction.setAccessible(true);
        arguments=host.method(fragmentType,"getArguments");setArguments=host.method(fragmentType,"setArguments",Bundle.class);
        manager=host.method(fragmentType,"getParentFragmentManager");
        replace=host.method("androidx.fragment.app.F","i",int.class,fragmentType,String.class);
        commit=host.method("androidx.fragment.app.a","n",boolean.class);
        // R8 removed addToBackStack(String); these are its retained native fields.
        addToBackStack=host.field("androidx.fragment.app.F","g");backStackName=host.field("androidx.fragment.app.F","i");
        if(addToBackStack.getType()!=boolean.class||backStackName.getType()!=String.class)throw new NoSuchFieldException("Host fragment back stack contract");
    }
    private static RuntimeException failed(String operation,ReflectiveOperationException failure){
        Throwable cause=failure instanceof InvocationTargetException?((InvocationTargetException)failure).getCause():failure;
        return cause instanceof RuntimeException?(RuntimeException)cause:new IllegalStateException(operation,cause);
    }
    public boolean experimental(Object owner,boolean intentValue){
        try{Bundle state=(Bundle)arguments.invoke(owner);return state!=null&&state.containsKey(EXPERIMENTAL)?state.getBoolean(EXPERIMENTAL):intentValue;}
        catch(ReflectiveOperationException failure){throw failed("AUTH page arguments",failure);}
    }
    public void preparePage(Object owner,boolean experimental){
        try{
            Bundle state=(Bundle)arguments.invoke(owner);
            if(state==null){state=new Bundle();setArguments.invoke(owner,state);}
            state.putBoolean(PAGE,true);state.putBoolean(EXPERIMENTAL,experimental);
        }catch(ReflectiveOperationException failure){throw failed("AUTH page state",failure);}
    }
    public void openExperimental(Object owner){
        try{
            Object next=fragment.newInstance();Bundle state=new Bundle();state.putBoolean(PAGE,true);state.putBoolean(EXPERIMENTAL,true);setArguments.invoke(next,state);
            Object edit=transaction.newInstance(manager.invoke(owner));
            replace.invoke(edit,android.R.id.content,next,TAG);
            addToBackStack.setBoolean(edit,true);backStackName.set(edit,TAG);
            commit.invoke(edit,false);
        }catch(ReflectiveOperationException failure){throw failed("AUTH experimental navigation",failure);}
    }
}
