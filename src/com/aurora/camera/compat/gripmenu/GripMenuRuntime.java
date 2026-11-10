package com.aurora.camera.compat.gripmenu;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;
import android.util.Range;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Exact 1770 accessors; no dex replacement or invented host fields. */
public final class GripMenuRuntime implements GripPolicy.Runtime,MenuMigration.Runtime {
    private static final String ACTIVE="aurora.compat.grip.active";
    public final GripPolicy grip=new GripPolicy(this);
    public final MenuMigration migration=new MenuMigration(this);
    public final Method keyDown,keyUp,lock,cancel,cancelVideo,cancelStreet,cancelPhoto,cancelDual,stops,slider,sanitize,
            accept,zoomKey,wheel,component,streetItems,streetSupport,streetSupportAlt,legendItems,nightItems,
            apertureTouch,streetInit,beautyInit,recordSetup,request;
    private final Method getRunning,getGlobal,getComponent,front,focusSupported,getFocus,getFocusMode,focusProtocol,
            focusVm,center,notify,focusLock,focusStart,focusCancel,logger,rangeProtocol,
            savedMenu,menuKey,saveMenu,parentContains,parentEdit,parentPut,parentCommit,
            protocolIndex,protocolRatio,zoomSupport,zoomAlternatives,defaultValue,callback,
            runningInit,runningRows,beautyRow,shineSupported,nightGlobal,streetMode,
            videoB,videoZ,currentModule,moduleMode,moduleState,moduleManager,managerReady,globalReady,
            apertureManager,componentAperture,apertureNext,apertureValid,apertureMessage,componentSet,
            focusBusyProtocol,menuBusyProtocol,ringProtocol,apertureProtocol,apertureUiProtocol,topProtocol,bottomProtocol,
            topHint,registeredUi;
    private final Field managerField,moduleIndex,focusRect,focusPoint,stopValues,menuCamera,parent,
            ratio,b,c,d,e,beautyMode,beautyEnabled,beautyItems,beautyPanel,beautyWide,
            requestState,requestConfig,configMode,requestCaps,rowValue,apertureAvailable;
    private final Class<?> nativeFocusType,zoomComponentType,beautyType,apertureType,apertureMenuType;
    private final Constructor<?> row,rangeMapper,initialBusy,menuBusy,ringMapper,apertureEnabled,apertureBusy,
            hideHint,apertureApply,refreshTop,apertureHint;
    private final Field defaultRange,featureSingleton;
    private final Method builderA,builderI,addRow,shineReady,stateReady;

    public GripMenuRuntime(Host host)throws ReflectiveOperationException{
        Class<?> module=host.type("com.android.camera.module.u"),moduleApi=host.type("com.android.camera.module.a0");
        Class<?> focus=host.type("v6.r");nativeFocusType=host.type("v6.q");
        Class<?> manager=host.type("k6.k"),running=host.type("v2.F0"),global=host.type("u2.Y");
        Class<?> base=host.type("com.android.camera.data.data.c"),parentType=host.type("Xh.a");
        Class<?> rowBuilder=host.type("b5.i$a"),rowType=host.type("b5.i");
        Class<?> caps=host.type("l9.g"),configuration=host.type("l9.j0"),cameraState=host.type("l9.i0");
        beautyType=host.type("v2.m0");apertureType=host.type("v2.k");apertureMenuType=host.type("r2.o0");
        zoomComponentType=host.type("v2.x0");
        keyDown=m(host,module.getName(),"onKeyDown",boolean.class,false,int.class,KeyEvent.class);
        keyUp=m(host,module.getName(),"onKeyUp",boolean.class,false,int.class,KeyEvent.class);
        lock=m(host,module.getName(),"handleLockFocus",void.class,false);
        cancel=m(host,module.getName(),"cancelFocus",void.class,false,boolean.class);
        cancelVideo=m(host,"com.android.camera.module.VideoBase","cancelFocus",void.class,false,boolean.class);
        cancelStreet=m(host,"com.android.camera.features.mode.street.StreetModule","cancelFocus",void.class,false,boolean.class);
        cancelPhoto=m(host,"com.xiaomi.camera.module.PhotoBase","cancelFocus",void.class,false,boolean.class);
        cancelDual=m(host,"com.xiaomi.microfilm.dualcam.mode.DualVideoModuleBase","cancelFocus",void.class,false,boolean.class);
        stops=m(host,"X5.f","a",void.class,false,int.class);
        slider=m(host,"X5.d","jh",void.class,false,int.class,int.class,boolean.class);
        sanitize=m(host,"com.android.camera.data.data.i","a",void.class,true,ArrayList.class);
        accept=m(host,"r6.H0","accept",void.class,false,Object.class);
        zoomKey=m(host,"r6.O0","v",void.class,false,int.class,moduleApi,boolean.class,boolean.class);
        wheel=m(host,"r6.O0","T0",void.class,false,MotionEvent.class,boolean.class,String.class);
        component=m(host,"Xh.b","x",Object.class,false,Class.class);
        streetItems=m(host,"com.android.camera.features.mode.street.a","l",ArrayList.class,false);
        streetSupport=m(host,"r9.o","c",boolean.class,false);
        streetSupportAlt=m(host,"A9.P","c",boolean.class,false);
        legendItems=m(host,"T3.b","l",ArrayList.class,false);
        nightItems=m(host,"b4.a","l",ArrayList.class,false);
        apertureTouch=m(host,"Pe.b","j2",void.class,false);
        streetInit=m(host,"r6.W","gf",void.class,false);
        beautyInit=m(host,beautyType.getName(),"X",void.class,false,host.type("v2.J0$a"));
        recordSetup=m(host,"l9.y0","m2",CaptureRequest.Builder.class,false,int.class);
        request=m(host,"l9.y0","o2",CaptureRequest.Builder.class,false,int.class);
        getRunning=m(host,"g2.a","j",running,true);getGlobal=m(host,"g2.a","g",global,true);
        getComponent=m(host,"Xh.b","x",Object.class,false,Class.class);front=m(host,global.getName(),"N",boolean.class,false);
        managerField=f(host,module.getName(),"mCameraManager",manager);moduleIndex=f(host,module.getName(),"mModuleIndex",int.class);
        focusSupported=m(host,manager.getName(),"a1",boolean.class,false);getFocus=m(host,manager.getName(),"p0",focus,false);
        getFocusMode=m(host,nativeFocusType.getName(),"getFocusMode",int.class,false);
        focusRect=f(host,nativeFocusType.getName(),"N",Rect.class);focusPoint=f(host,nativeFocusType.getName(),"P",Point.class);
        focusProtocol=m(host,"R6.u0","b",host.type("R6.u0"),true);
        focusVm=m(host,"R6.u0","Vm",void.class,false,int.class,int.class,int.class);
        center=m(host,nativeFocusType.getName(),"j0",void.class,false);
        notify=m(host,module.getName(),"notifyFocusAreaUpdate",void.class,false,boolean.class);
        focusLock=m(host,manager.getName(),"Y0",void.class,false,boolean.class);
        focusStart=m(host,nativeFocusType.getName(),"l0",void.class,false,int.class);
        focusCancel=m(host,focus.getName(),"cancelFocus",void.class,false,boolean.class);
        logger=m(host,"com.android.camera.log.Log","v",void.class,true,String.class,String.class);
        stopValues=f(host,"X5.f","a",ArrayList.class);
        rangeProtocol=m(host,"W6.d","a",Optional.class,true);rangeMapper=ctor(host,"R5.u",int.class);
        defaultRange=sf(host,"h9.b","a",Range.class);
        savedMenu=m(host,"u2.y","U",List.class,false,int.class);menuKey=m(host,"u2.y","getKey",String.class,false,int.class);
        saveMenu=m(host,"u2.y","Y",void.class,false,ArrayList.class,boolean.class);
        menuCamera=f(host,"u2.y","a",int.class);parent=f(host,base.getName(),"mParentDataItem",parentType);
        parentContains=m(host,parentType.getName(),"f",boolean.class,false,String.class);
        parentEdit=m(host,parentType.getName(),"g",parentType,false);
        parentPut=m(host,parentType.getName(),"r",parentType,false,String.class,String.class);
        parentCommit=m(host,parentType.getName(),"c",void.class,false);
        ratio=f(host,"r6.H0","a",float.class);b=f(host,"r6.H0","b",boolean.class);c=f(host,"r6.H0","c",boolean.class);
        d=f(host,"r6.H0","d",int.class);e=f(host,"r6.H0","e",host.type("r6.O0$c"));
        protocolIndex=m(host,"R6.M","I7",int.class,false,float.class,boolean.class,boolean.class);
        protocolRatio=m(host,"R6.M","mr",float.class,false,int.class);
        featureSingleton=sf(host,"Pe.b$b","a",host.type("Pe.b"));
        zoomSupport=m(host,"Pe.b","Y1",boolean.class,false,int.class);
        shineSupported=m(host,zoomComponentType.getName(),"y",boolean.class,false,int.class);
        zoomAlternatives=m(host,zoomComponentType.getName(),"v",ArrayList.class,false);
        defaultValue=m(host,zoomComponentType.getName(),"getDefaultValue",String.class,false,int.class);
        callback=m(host,"r6.O0$c","a",void.class,false,float.class);
        runningInit=m(host,beautyType.getName(),"b0",void.class,false,int.class,List.class,String.class);
        runningRows=m(host,beautyType.getName(),"Q",ArrayList.class,false);
        rowValue=f(host,"com.android.camera.data.data.d","r",String.class);
        apertureAvailable=f(host,apertureType.getName(),"W",boolean.class);
        streetMode=m(host,"r6.W","Jc",int.class,false);
        beautyRow=m(host,beautyType.getName(),"H",host.type("com.android.camera.data.data.d"),true);
        shineReady=m(host,beautyType.getName(),"e0",boolean.class,false);
        beautyMode=f(host,base.getName(),"mCurrentMode",int.class);beautyEnabled=f(host,beautyType.getName(),"b0",boolean.class);
        beautyItems=f(host,base.getName(),"mItems",List.class);beautyPanel=f(host,beautyType.getName(),"i",int.class);
        beautyWide=f(host,beautyType.getName(),"V",boolean.class);
        builderA=m(host,"X9.w5","a",rowBuilder,true);builderI=m(host,"X9.w5","i",rowBuilder,true);
        row=ctor(host,rowType.getName(),rowBuilder);addRow=m(host,"A.b","g",void.class,true,rowBuilder,ArrayList.class);
        requestState=f(host,"l9.y0","E",cameraState);requestConfig=f(host,cameraState.getName(),"a",configuration);configMode=f(host,configuration.getName(),"K3",int.class);
        requestCaps=f(host,"l9.y0","D",caps);
        videoB=m(host,"l9.o0","B",void.class,true,CaptureRequest.Builder.class,caps,configuration);
        videoZ=m(host,"l9.o0","z",void.class,true,CaptureRequest.Builder.class,caps,configuration);
        currentModule=m(host,"r6.O0","o0",Optional.class,false);
        moduleMode=m(host,moduleApi.getName(),"getModuleIndex",int.class,false);
        moduleState=m(host,moduleApi.getName(),"getModuleState",host.type("k6.g"),false);
        moduleManager=m(host,moduleApi.getName(),"getCameraManager",manager,false);
        stateReady=m(host,"k6.g","z",boolean.class,false);managerReady=m(host,manager.getName(),"p",boolean.class,false);
        globalReady=m(host,global.getName(),"R",boolean.class,false);
        apertureManager=m(host,moduleApi.getName(),"getApertureManager",host.type("U1.e"),false);
        focusBusyProtocol=m(host,"R6.L0","a",Optional.class,true);initialBusy=ctor(host,"E1.q2",int.class);
        menuBusyProtocol=m(host,"R6.j0","a",Optional.class,true);menuBusy=ctor(host,"H3.k",int.class);
        ringProtocol=m(host,"R6.M","a",Optional.class,true);ringMapper=ctor(host,"r6.d",MotionEvent.class,int.class);
        apertureProtocol=m(host,"R6.a1","a",Optional.class,true);apertureEnabled=ctor(host,"E1.t4",int.class);
        apertureUiProtocol=m(host,"R6.s1","a",Optional.class,true);apertureBusy=ctor(host,"H4.S",int.class);
        componentAperture=m(host,apertureType.getName(),"A",float.class,false,int.class);
        apertureNext=m(host,base.getName(),"getComponentNextValue",String.class,false,int.class,boolean.class);
        apertureValid=m(host,apertureType.getName(),"J",boolean.class,false);
        apertureMessage=m(host,apertureType.getName(),"C",String.class,false);
        componentSet=m(host,base.getName(),"setComponentValue",void.class,false,int.class,String.class);
        topProtocol=m(host,"R6.o1","a",Optional.class,true);bottomProtocol=m(host,"R6.m1","b",host.type("R6.m1"),true);
        registeredUi=m(host,"R6.D","a",Optional.class,true);nightGlobal=m(host,"R6.m1","a",Optional.class,true);
        topHint=m(host,"R6.m1","jm",void.class,false,int.class,String.class,long.class);
        hideHint=ctor(host,"E1.a0",int.class,byte.class);apertureApply=ctor(host,"K9.f",String.class,int.class);
        refreshTop=ctor(host,"C4.H",int.class);apertureHint=ctor(host,"Tn.y",int.class,int.class,Object.class);
    }
    public Object running(Class<?> type){return call(getComponent,call(getRunning,null),type);}
    public boolean front(){return(Boolean)call(front,call(getGlobal,null));}
    @Override public boolean active(Object owner){return Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(owner,ACTIVE));}
    @Override public void active(Object owner,boolean value){XposedHelpers.setAdditionalInstanceField(owner,ACTIVE,value);}
    @Override public Object manager(Object owner){return read(managerField,owner);}
    @Override public boolean canFocus(Object manager){return(Boolean)call(focusSupported,manager);}
    @Override public Object focus(Object manager){return call(getFocus,manager);}
    @Override public boolean nativeFocus(Object focus){return nativeFocusType.isInstance(focus);}
    @Override public int focusMode(Object focus){return(Integer)call(getFocusMode,focus);}
    @Override public Rect focusRect(Object focus){return(Rect)read(focusRect,focus);}
    @Override public Point focusPoint(Object focus){return(Point)read(focusPoint,focus);}
    @Override public Object focusUi(){return call(focusProtocol,null);}
    @Override public void focusUi(Object ui,int type,int x,int y){call(focusVm,ui,type,x,y);}
    @Override public void centerFocus(Object focus){call(center,focus);}
    @Override public void notifyFocus(Object owner,boolean value){call(notify,owner,value);}
    @Override public int mode(Object owner){return(Integer)read(moduleIndex,owner);}
    @Override public void focusLock(Object manager,boolean value){call(focusLock,manager,value);}
    @Override public void originalLock(Object owner)throws Throwable{XposedBridge.invokeOriginalMethod(lock,owner,new Object[0]);}
    @Override public void startFocus(Object focus,int type){call(focusStart,focus,type);}
    @Override public void cancelNative(Object focus,boolean value){call(focusCancel,focus,value);}
    @Override public void log(String message){call(logger,null,"AuroraN1G",message);}
    @SuppressWarnings("unchecked") public ArrayList<Float> stops(Object owner){return(ArrayList<Float>)read(stopValues,owner);}
    @SuppressWarnings("unchecked") public void addStops(Object owner,int mode){
        ArrayList<Float> values=stops(owner);if(!GripPolicy.stopMode(mode)||front()||values==null||!values.contains(1f))return;
        Optional<Object> protocol=(Optional<Object>)call(rangeProtocol,null);
        final Range<Float> range=(Range<Float>)protocol.map((Function<Object,Object>)construct(rangeMapper,2)).orElse(read(defaultRange,null));
        GripPolicy.addStops(values,mode,false,value->range.contains(value));
    }
    @Override public int camera(Object component){return(Integer)read(menuCamera,component);}
    @Override public String key(Object component,int mode){return(String)call(menuKey,component,mode);}
    @Override public boolean marked(Object component,String key){return(Boolean)call(parentContains,read(parent,component),key);}
    @SuppressWarnings("unchecked") @Override public List<Integer> saved(Object component,int mode){return(List<Integer>)call(savedMenu,component,mode);}
    @Override public void save(Object component,ArrayList<Integer> values){call(saveMenu,component,values,false);}
    @Override public void mark(Object component,String key){Object data=read(parent,component);call(parentEdit,data);call(parentPut,data,key,"done");call(parentCommit,data);}
    @SuppressWarnings("unchecked") public void accept(Object owner,Object protocol){
        float initial=(Float)read(ratio,owner);int mode=(Integer)read(d,owner);
        int index=(Integer)call(protocolIndex,protocol,initial,read(b,owner),read(c,owner));
        float selected=(Float)call(protocolRatio,protocol,index);
        if(!(GripPolicy.stopMode(mode)&&(selected==1f||selected==1.2f||selected==1.5f))){
            Object component=running(zoomComponentType);
            if((Boolean)call(zoomSupport,read(featureSingleton,null),mode)&&(Boolean)call(shineSupported,component,mode)){
                ArrayList<Float> alternatives=(ArrayList<Float>)call(zoomAlternatives,component);
                if(!alternatives.contains(initial)&&alternatives.contains(selected))selected=Float.parseFloat((String)call(defaultValue,component,mode));
            }
        }
        if(index>=0&&selected>0f)call(callback,read(e,owner),selected);
    }
    public boolean shine(){return(Boolean)call(shineReady,running(beautyType));}
    public boolean apertureMenu(Object type){return type==apertureMenuType;}
    public void streetInitialize(Object module){
        if((Integer)call(streetMode,module)!=225)return;Object component=running(beautyType);
        @SuppressWarnings("unchecked") ArrayList<Object> rows=(ArrayList<Object>)call(runningRows,component);
        call(runningInit,component,8,rows,read(rowValue,rows.get(0)));
    }
    @SuppressWarnings("unchecked") public void beautyInitialize(Object component){
        if((Integer)read(beautyMode,component)!=225||(Boolean)read(beautyEnabled,component))return;
        write(beautyEnabled,component,true);((List<Object>)read(beautyItems,component)).add(call(beautyRow,null));
        write(beautyPanel,component,(Boolean)read(beautyWide,component)?4:6);
    }
    public void legendRow(ArrayList<Object> rows){
        if((Boolean)read(apertureAvailable,running(apertureType)))rows.add(4,construct(row,call(builderA,null)));
    }
    public ArrayList<Object> nightRow(){ArrayList<Object> rows=new ArrayList<>();call(addRow,null,call(builderI,null),rows);return rows;}
    public boolean matchingSlider(Object owner,Object values){return stops(owner)==values;}
    public boolean legendWheel(Object owner){
        Optional<?> current=(Optional<?>)call(currentModule,owner);
        return current.isPresent()&&(Integer)call(moduleMode,current.get())==256;
    }
    @SuppressWarnings("unchecked") public void apertureWheel(Object owner,boolean positive){
        // Called only after original T0 has passed all input/module/busy gates,
        // resolved the temporary ring choice and entered its aperture case.
        if(!mapped(apertureProtocol,apertureEnabled,8))return;
        Object module=((Optional<?>)call(currentModule,owner)).get();if(call(apertureManager,module)==null)return;
        if(mapped(apertureUiProtocol,apertureBusy,4))return;
        Object component=running(apertureType);String previous=String.valueOf((Float)call(componentAperture,component,256));
        String next=(String)call(apertureNext,component,256,positive);
        if(!(Boolean)call(apertureValid,component)){
            String message=(String)call(apertureMessage,component);Object ui=call(bottomProtocol,null);
            if(message!=null&&!message.isEmpty()&&ui!=null){consumer(topProtocol,construct(hideHint,5,(byte)0));call(topHint,ui,0,message,3000L);}return;
        }
        if(next!=null&&!next.equals(previous)){
            call(componentSet,component,256,next);consumer(registeredUi,construct(apertureApply,next,1));
            consumer(topProtocol,construct(refreshTop,3));
        }
        if(!((Optional<?>)call(apertureUiProtocol,null)).isPresent())consumer(nightGlobal,construct(apertureHint,256,1,component));
    }
    @SuppressWarnings("unchecked") private boolean mapped(Method protocol,Constructor<?> mapper,int value){
        return(Boolean)((Optional<Object>)call(protocol,null)).map((Function<Object,Object>)construct(mapper,value)).orElse(false);
    }
    @SuppressWarnings("unchecked") private void consumer(Method protocol,Object consumer){((Optional<Object>)call(protocol,null)).ifPresent((Consumer<Object>)consumer);}
    public void eis(Object owner,int mode,CaptureRequest.Builder builder){
        if(mode!=256)return;
        Object config=read(requestConfig,read(requestState,owner));if(mode==256&&config!=null&&(Integer)read(configMode,config)==256){
            Object caps=read(requestCaps,owner);call(videoB,null,builder,caps,config);call(videoZ,null,builder,caps,config);
        }
    }
    public static Field f(Host host,String owner,String name,Class<?> type)throws ReflectiveOperationException{Field field=host.field(owner,name);if(field.getType()!=type||Modifier.isStatic(field.getModifiers()))throw new NoSuchFieldException(owner+"."+name);return field;}
    public static Field sf(Host host,String owner,String name,Class<?> type)throws ReflectiveOperationException{Field field=host.field(owner,name);if(field.getType()!=type||!Modifier.isStatic(field.getModifiers()))throw new NoSuchFieldException(owner+"."+name);return field;}
    public static Method m(Host host,String owner,String name,Class<?> result,boolean isStatic,Class<?>... parameters)throws ReflectiveOperationException{Method method=host.method(owner,name,parameters);if(method.getReturnType()!=result||Modifier.isStatic(method.getModifiers())!=isStatic)throw new NoSuchMethodException(owner+"."+name);return method;}
    private static Constructor<?> ctor(Host host,String owner,Class<?>... parameters)throws ReflectiveOperationException{Constructor<?> ctor=host.type(owner).getDeclaredConstructor(parameters);ctor.setAccessible(true);return ctor;}
    public static Object read(Field field,Object owner){try{return field.get(owner);}catch(IllegalAccessException failure){throw new IllegalStateException(field.toString(),failure);}}
    public static void write(Field field,Object owner,Object value){try{field.set(owner,value);}catch(IllegalAccessException failure){throw new IllegalStateException(field.toString(),failure);}}
    public static Object call(Method method,Object owner,Object... values){try{return method.invoke(owner,values);}catch(IllegalAccessException failure){throw new IllegalStateException(method.toString(),failure);}catch(InvocationTargetException failure){throw propagate(failure.getCause());}}
    private static Object construct(Constructor<?> ctor,Object... values){try{return ctor.newInstance(values);}catch(InstantiationException|IllegalAccessException failure){throw new IllegalStateException(ctor.toString(),failure);}catch(InvocationTargetException failure){throw propagate(failure.getCause());}}
    private static RuntimeException propagate(Throwable failure){if(failure instanceof RuntimeException)return(RuntimeException)failure;if(failure instanceof Error)throw(Error)failure;return new IllegalStateException(failure);}
}
