package com.aurora.camera.compat.softlight;

import android.util.Log;
import com.aurora.camera.compat.Host;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact 1770 preview and MIVI boundaries; no library or camera APK replacement. */
public final class SoftLightRuntime245 {
    public final Method soft,filter,picture,attach,graph,tagType,tagParams,lookup,save;
    private final Method controller,params,write,toggle,request,pictureRefresh;
    private final Field mode,filterId,degree,version,color,view,kind;
    private final Field renderParams,softParam,rebuild,tagSoft,tagFilter,tagDegree,tagMask,paramName,paramSize,paramVignette,pictureSoft;
    private final Object[] types;
    private final int noFilter,carrier;
    private final Map<Object,Boolean> graphOwners=new WeakHashMap<>();
    private final Map<Object,Boolean> captureOwners=new WeakHashMap<>();
    private final Map<Object,Integer> captureOverrides=new WeakHashMap<>();
    private final Map<Object,Boolean> pictureOwners=new WeakHashMap<>();
    private final ThreadLocal<Integer> capturePicture=new ThreadLocal<>();
    private final ThreadLocal<Boolean> reading=new ThreadLocal<>();
    public final SoftLightState245 state=new SoftLightState245();
    public SoftLightRuntime245(Host h)throws ReflectiveOperationException{
        String effect="com.xiaomi.camera.effect.EffectController",tag="com.xiaomi.camera.mivi.filter.MIVIRenderTag";
        controller=h.method(effect,"u");soft=h.method(effect,"y",int.class);filter=h.method(effect,"e0",int.class,int.class);
        picture=h.method(effect,"c");pictureRefresh=h.method(effect,"g0");pictureSoft=h.field("Mu.g","j");
        attach=h.method(effect,"s0",h.type("Iu.v"));
        mode=h.field("com.android.camera.module.c0","a");filterId=h.field(effect,"h");degree=h.field(effect,"v");
        version=h.field(effect,"u");color=h.field(effect,"t");view=h.field(effect,"V");kind=h.field("Ku.d","f");
        params=h.method("si.f","e",int.class,int.class,int.class,boolean.class);
        paramName=h.field("Mu.d","b");paramSize=h.field("Mu.d","d");paramVignette=h.field("Mu.d","i");
        write=h.method("Iu.v","W0",h.type("Ku.d"),Object[].class);toggle=h.method("Iu.v","X0",h.type("Ku.d"),boolean.class);request=h.method("Iu.v","requestRender");
        graph=h.method("Tu.h","e",h.type("Mu.h"));renderParams=h.field("Tu.h","G");softParam=h.field("Mu.d","o");rebuild=h.field("Tu.h","N");
        tagType=h.method(tag,"initType");tagParams=h.method(tag,"initParams");tagSoft=h.field(tag,"mSoftFocusType");tagFilter=h.field(tag,"mFilterId");tagDegree=h.field(tag,"mFilterDegree");tagMask=h.field("com.xiaomi.camera.mivi.common.MIVITag","mType");
        lookup=h.method("si.f","d",int.class,int.class);save=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","saveLutByFilterId",int.class);
        types=h.type(effect+"$c").getEnumConstants();
        String[] expected={"NONE","BLACK_SOFT","WHITE_SOFT","SOFT_FOCUS"};
        if(types==null||types.length!=expected.length)throw new IllegalStateException("1770 soft-light enum ABI changed");
        for(int i=0;i<types.length;i++)if(!((Enum<?>)types[i]).name().equals(expected[i]))throw new IllegalStateException("1770 soft-light enum "+i);
        noFilter=h.field("i3.b","P").getInt(null);
        carrier=((Enum<?>)invoke(h.method("o3.d","valueOf",String.class),null,"N_ORIGINAL")).ordinal()|65536;
    }
    public static Object invoke(Method method,Object owner,Object...args){
        try{return method.invoke(owner,args);}
        catch(IllegalAccessException failure){throw new IllegalStateException(method.toString(),failure);}
        catch(InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof RuntimeException)throw(RuntimeException)cause;if(cause instanceof Error)throw(Error)cause;throw new IllegalStateException(method.toString(),cause);}
    }
    private static Object get(Field field,Object owner){try{return field.get(owner);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    private static int integer(Field field,Object owner){return(Integer)get(field,owner);}
    public Object controller(){return invoke(controller,null);}
    public int mode(){return integer(mode,null);}
    public boolean active(){return state.enabled()&&SoftLightState245.supports(mode());}
    public boolean noFilter(int id){return id==noFilter||id==0;}
    public int carrier(){return carrier;}
    public boolean reading(){return Boolean.TRUE.equals(reading.get());}
    public int sync(Object effect){
        int id=integer(filterId,effect);reading.set(true);
        try{state.accept(mode(),id,((Enum<?>)invoke(soft,effect,id)).ordinal());}
        finally{reading.remove();}
        return state.effective(mode());
    }
    /** Native calls may query another id while creating a capture tag. */
    public Object effective(Object effect,int queried,Object nativeValue){
        if(!active()||reading())return nativeValue;
        if(queried==integer(filterId,effect))state.accept(mode(),queried,((Enum<?>)nativeValue).ordinal());else sync(effect);
        return types[state.effective(mode())];
    }
    private int override(Object effect,int queried){
        if(!active()||queried!=integer(filterId,effect))return -1;
        sync(effect);return state.override(mode(),queried);
    }
    public void select(int soft){Object effect=controller();sync(effect);state.select(mode(),soft);boolean submitted=refresh(effect);Log.i("AuroraSoft245","manual mode="+mode()+" filter="+integer(filterId,effect)+" soft="+soft+" previewSubmitted="+submitted);}
    /** PictureStyle has its own recipe mist. Keep every native colour parameter and only mask that stage. */
    public void picture(Object effect,Object attribute){
        Integer captured=capturePicture.get();
        if(captured!=null){if(captured>=0)setPictureSoft(attribute);return;}
        boolean manual=override(effect,integer(filterId,effect))>=0,wasOwned;
        synchronized(pictureOwners){wasOwned=pictureOwners.containsKey(effect);if(manual)pictureOwners.put(effect,true);else pictureOwners.remove(effect);}
        if(manual){
            int nativeSoft=integer(pictureSoft,attribute);
            setPictureSoft(attribute);
            if(!wasOwned)Log.i("AuroraSoft245","PictureStyle recipe mist masked; native="+nativeSoft+"; seven colour floats and Algos retained");
        }else if(wasOwned)Log.i("AuroraSoft245","PictureStyle native recipe mist restored");
    }
    private void setPictureSoft(Object attribute){try{pictureSoft.setInt(attribute,0);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}
    /** Called after e0: first reconcile filter identity, then republish the native PictureStyle stage if owned. */
    public void pictureRefresh(Object effect){
        boolean owned;synchronized(pictureOwners){owned=pictureOwners.containsKey(effect);}
        if(owned||override(effect,integer(filterId,effect))>=0)invoke(pictureRefresh,effect);
    }
    /** e0 performs the host's normal parameter publication; its after hook handles no-filter. */
    public boolean refresh(Object effect){
        WeakReference<?> ref=(WeakReference<?>)get(view,effect);Object target=ref==null?null:ref.get();
        if(target==null)return false;
        invoke(filter,effect,integer(filterId,effect),integer(degree,effect));invoke(request,target);return true;
    }
    /** Called after native e0. A zero-strength original LUT carries mist without adding a colour filter. */
    public boolean standalonePreview(Object effect){
        if(!active()||!noFilter(integer(filterId,effect)))return false;
        int selected=sync(effect);WeakReference<?> ref=(WeakReference<?>)get(view,effect);Object target=ref==null?null:ref.get();if(target==null)return false;
        Object filterKind=get(kind,null);
        if(SoftLightState245.mist(selected)){
            Object data=invoke(params,null,carrier,integer(version,effect),0,get(color,effect));
            Object[] values={get(paramName,data),false,get(paramSize,data),0,false,false,false,get(paramVignette,data),false,selected};
            invoke(write,target,filterKind,values);
        }
        invoke(toggle,target,filterKind,SoftLightState245.mist(selected));return true;
    }
    /** Renderer receives parameters on its own engine path. Keep LUT/cache ownership intact. */
    public void graph(Object renderer,Object incoming){
        if(incoming==null)return;boolean active=active(),owned;
        synchronized(graphOwners){owned=graphOwners.containsKey(renderer);if(active)graphOwners.put(renderer,true);else graphOwners.remove(renderer);}
        Object old=get(renderParams,renderer);if(old==null)return;
        if((active||owned)&&integer(softParam,old)!=integer(softParam,incoming))try{rebuild.setBoolean(renderer,true);Log.i("AuroraSoft245","legacy graph mist delta "+integer(softParam,old)+"->"+integer(softParam,incoming)+"; native Candy rebuild requested");}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    public boolean standaloneTag(Object tag){synchronized(captureOwners){return captureOwners.containsKey(tag);}}
    public void prepareTag(Object tag){
        int manual=override(controller(),integer(tagFilter,tag));
        if(manual>=0)synchronized(captureOverrides){captureOverrides.put(tag,manual);}
        if(!active()||!noFilter(integer(tagFilter,tag))||!SoftLightState245.mist(((Enum<?>)get(tagSoft,tag)).ordinal()))return;
        try{tagDegree.setInt(tag,0);tagMask.setInt(tag,integer(tagMask,tag)|1);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        synchronized(captureOwners){captureOwners.put(tag,true);}
    }
    /** Priority25 before initParams runs after LutRuntime229's priority50 before serialization. */
    public void reassertTag(Object tag){
        Integer selected;synchronized(captureOverrides){selected=captureOverrides.get(tag);}
        if(selected==null)return;
        int previous=((Enum<?>)get(tagSoft,tag)).ordinal();
        try{tagSoft.set(tag,types[selected]);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        Log.i("AuroraSoft245","capture manual snapshot filter="+integer(tagFilter,tag)+" soft="+previous+"->"+selected+"; before native serialization");
    }
    /** Native initParams also calls c() for the same capture; keep its two mist stages consistent. */
    public Integer enterTag(Object tag){
        Integer previous=capturePicture.get(),selected;synchronized(captureOverrides){selected=captureOverrides.get(tag);}
        capturePicture.set(selected==null?-1:selected);return previous;
    }
    public void leaveTag(Integer previous){if(previous==null)capturePicture.remove();else capturePicture.set(previous);}
}
