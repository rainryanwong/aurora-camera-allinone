package com.aurora.camera.compat.cinematic;

import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.util.Log;
import android.view.View;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Fixed 1770 declarations behind the stable cinematic policies. */
public final class CinematicRuntime implements SmartDolly.Runtime,SmartDollyAsd.Runtime,DollyRepair.Runtime {
    public final SmartDolly smart;
    public final DollyRepair dolly;
    public final Class<?> asdType,cinematicType,optionType,baseInterceptorType;
    public final Method append,registerInterceptor,sessionMode,sessionSettings,baseSessionSettings;
    public final Method previewSettings,recordSettings,requestSettings,options,menuEntries,panelInitialize,panelRefresh;
    public final Method onActive,baseOnActive,onInactive,ignoreTap,singleTap,doubleTap;
    public final Method stopRecording,baseStopRecording,recordStarted,recorderStarted;
    public final Method declareTags,initializeAsd,inTimeCondition,sampleTime,asdTag,parseResult,consumeResult;
    public final Method focusRoi,findIndex;
    public final Field requestCapabilities,moduleIndexField,componentItems,panelChoice,panelIndex,panelAutoLayout;
    private final Field optionCapabilities,capabilityCharacteristics,capabilityId,sourceIndex;
    private final Field sessionTags,interceptorModule,interceptorCapabilities,holderValue;
    private final Field callback,displayWidth,displayHeight;
    private final Field subjectRect,subjectType,subjectStatus,subjectPreviousRect,subjectPreviousType,subjectPreviousStatus;
    private final Field animator,protocolRegistry,needLockTip,needUnlockTip,hasDoubleTapped,renderFeature;
    private final Method running,component,getChoice,preferenceRead,preferenceWrite,requestKey,featureKey;
    private final Method requestFactory,featureFactory,resultFactory,tagObject,tagName,writeRequest;
    private final Method nativeMetadata,vendorKeys,globalSource,lookupMode;
    private final Method currentController,currentModule,moduleIndex,moduleAlive,isRecording,departed;
    private final Method managerCapabilities,managerData,writeSession,registerTag,sensorRect,getCallback,getPreview;
    private final Method orientation,cropVendor,cropFromRequest,cropFromResult,sensorTransform,displayTransform;
    private final Method overlayReceiver,overlaySubjects,audioTracking,trackRect,audioZoom,hintReceiver,hint;
    private final Method protocol,zoomStart,busyStart,busyAction,bottomReceiver,openBottom;
    private final Method moduleRender,addRender,defaultZoom;
    private final Constructor<?> subjectConstructor,zoomInitializer;
    private final Class<?> controllerType,rangeType,speedType,zoomProtocolType,zoomImplementation;
    private final Field cropSourceTag;
    private final MenuFactory menu;
    private volatile Tags tags;

    public CinematicRuntime(Host host)throws ReflectiveOperationException {
        Class<?> capabilities=host.type("l9.g"),vendor=host.type("ia.D0"),manager=host.type("k6.k");
        Class<?> preferences=host.type("Xh.a"),baseComponent=host.type("com.android.camera.data.data.c");
        Class<?> module=host.type("com.android.camera.module.u"),pipeline=host.type("com.android.camera.module.interceptor.base.a");
        Class<?> resultType=host.type("c2.e"),callbackType=host.type("com.android.camera.module.b0");
        Class<?> display=host.type("E1.V4"),preview=host.type("E1.z3");
        optionType=host.type("v2.p");rangeType=host.type("v2.q");speedType=host.type("v2.r");
        asdType=host.type("s6.D");cinematicType=host.type("com.android.camera.features.mode.cinematic.CinematicModule");
        baseInterceptorType=host.type("com.android.camera.module.interceptor.base.i");
        controllerType=host.type("r6.W");zoomProtocolType=host.type("R6.y");
        zoomImplementation=host.type("com.android.camera.features.mode.cinematic.j");
        running=m(host,"g2.a","j",host.type("v2.F0"),true);
        component=m(host,"Xh.b","x",Object.class,false,Class.class);
        getChoice=m(host,baseComponent.getName(),"getComponentValue",String.class,false,int.class);
        findIndex=m(host,baseComponent.getName(),"findIndexOfValue",int.class,false,String.class);
        componentItems=f(host,baseComponent.getName(),"mItems",List.class);
        preferenceRead=m(host,preferences.getName(),"h",boolean.class,false,String.class,boolean.class);
        preferenceWrite=m(host,preferences.getName(),"n",preferences,false,String.class,boolean.class);
        optionCapabilities=f(host,optionType.getName(),"a",capabilities);
        capabilityCharacteristics=f(host,capabilities.getName(),"d",host.type("android.hardware.camera2.CameraCharacteristics"));
        capabilityId=f(host,capabilities.getName(),"e",int.class);
        requestKey=m(host,capabilities.getName(),"S0",boolean.class,false,String.class);
        featureKey=m(host,capabilities.getName(),"y0",boolean.class,false,vendor);
        requestFactory=m(host,"ia.A0","a",vendor,true,Supplier.class,Class.class);
        featureFactory=m(host,"ia.y0","a",vendor,true,Supplier.class,Class.class);
        resultFactory=m(host,"ia.C0","a",vendor,true,Supplier.class,Class.class);
        tagObject=m(host,vendor.getName(),"a",Object.class,false);
        tagName=m(host,vendor.getName(),"b",String.class,false);
        writeRequest=m(host,"ia.E0","e",void.class,true,CaptureRequest.Builder.class,vendor,Object.class);
        Class<?> metadata=host.type("android.hardware.camera2.impl.CameraMetadataNative");
        nativeMetadata=m(host,"Tp.b","c",metadata,true,host.type("android.hardware.camera2.CameraCharacteristics"));
        vendorKeys=m(host,metadata.getName(),"getAllVendorKeys",ArrayList.class,false,Class.class);
        globalSource=m(host,"g2.a","g",host.type("u2.Y"),true);
        sourceIndex=f(host,"u2.Y","u",int.class);
        lookupMode=m(host,"u2.Y","D",int.class,false,int.class);
        currentController=m(host,"R6.D","a",Optional.class,true);
        currentModule=m(host,controllerType.getName(),"ja",Optional.class,false);
        moduleIndex=m(host,module.getName(),"getModuleIndex",int.class,false);
        moduleAlive=m(host,module.getName(),"isDeviceAndModuleAlive",boolean.class,false);
        isRecording=m(host,module.getName(),"isRecording",boolean.class,false);
        departed=m(host,module.getName(),"isDeparted",boolean.class,false);
        moduleIndexField=f(host,module.getName(),"mModuleIndex",int.class);
        managerCapabilities=m(host,manager.getName(),"c",capabilities,false);
        managerData=m(host,manager.getName(),"K0",host.type("l9.i0"),false);
        sessionTags=f(host,"l9.i0","b",host.type("l9.z1"));
        writeSession=m(host,"l9.z1","a",void.class,false,vendor,Object.class);
        requestCapabilities=f(host,"l9.y0","D",capabilities);

        append=m(host,cinematicType.getName(),"appendModuleExternalASD",void.class,false,pipeline);
        registerInterceptor=m(host,pipeline.getName(),"a",void.class,false,baseInterceptorType);
        sessionMode=m(host,"com.android.camera.features.mode.cinematic.h","i",int.class,false,host.type("y3.w"));
        sessionSettings=m(host,"com.android.camera.features.mode.cinematic.h","r",void.class,false,manager);
        baseSessionSettings=m(host,"y3.f","r",void.class,false,manager);
        previewSettings=m(host,"l9.y0","M1",void.class,false,CaptureRequest.Builder.class);
        recordSettings=m(host,"l9.y0","N1",void.class,false,CaptureRequest.Builder.class);
        requestSettings=m(host,"l9.y0","Q1",void.class,false,int.class,CaptureRequest.Builder.class);
        options=m(host,optionType.getName(),"initItems",List.class,false);
        menuEntries=m(host,"com.android.camera.features.mode.cinematic.d","f",List.class,false);
        panelInitialize=m(host,"com.android.camera.features.mode.cinematic.m","initView",void.class,false,View.class);
        panelRefresh=m(host,"com.android.camera.features.mode.cinematic.m","Bs",void.class,false);
        panelChoice=f(host,"com.android.camera.features.mode.cinematic.m","m",optionType);
        panelIndex=f(host,"com.android.camera.features.mode.cinematic.m","r",int.class);
        panelAutoLayout=f(host,"com.android.camera.features.mode.cinematic.m","t",host.type("android.widget.LinearLayout"));
        onActive=m(host,cinematicType.getName(),"onActive",void.class,false);
        baseOnActive=m(host,"com.android.camera.module.VideoModule","onActive",void.class,false);
        onInactive=m(host,cinematicType.getName(),"onInactive",void.class,false);
        ignoreTap=m(host,cinematicType.getName(),"isIgnoreSingleTap",boolean.class,false);
        singleTap=m(host,cinematicType.getName(),"onSingleTapUp",void.class,false,int.class,int.class,boolean.class);
        doubleTap=m(host,cinematicType.getName(),"lambda$onDoubleTap$4",void.class,false,Rect.class,host.type("l9.a"));
        focusRoi=m(host,"l9.a","X0",void.class,false,int.class,Rect.class);
        stopRecording=m(host,cinematicType.getName(),"stopVideoRecording",boolean.class,false,boolean.class);
        baseStopRecording=m(host,"com.android.camera.module.VideoModule","stopVideoRecording",boolean.class,false,boolean.class);
        recordStarted=m(host,"com.android.camera.module.VideoModule","onRecordStarted",void.class,false);
        recorderStarted=m(host,"com.android.camera.module.VideoModule","onStartRecorderSucceed",void.class,false);
        needLockTip=f(host,cinematicType.getName(),"mNeedCinematicLockTip",boolean.class);
        needUnlockTip=f(host,cinematicType.getName(),"mNeedCinematicUnlockTip",boolean.class);
        hasDoubleTapped=f(host,cinematicType.getName(),"mHasDoubleTaped",boolean.class);
        callback=f(host,module.getName(),"mCallback",callbackType);
        moduleRender=m(host,callbackType.getName(),"fi",host.type("F8.j"),false);
        renderFeature=sf(host,"Ku.d","f",host.type("Ku.d"));
        addRender=m(host,"F8.j","V0",host.type("Tu.x"),false,host.type("Ku.d"));
        defaultZoom=m(host,"com.android.camera.data.data.F","p0",String.class,true);
        zoomInitializer=host.type("E3.g").getDeclaredConstructor(float.class,int.class);
        zoomInitializer.setAccessible(true);

        Class<?> interceptorBase=host.type("com.android.camera.module.interceptor.base.c");
        interceptorModule=f(host,interceptorBase.getName(),"module",host.type("com.android.camera.module.interceptor.base.h"));
        interceptorCapabilities=f(host,interceptorBase.getName(),"capabilities",capabilities);
        holderValue=f(host,"com.android.camera.module.interceptor.base.b","a",Object.class);
        declareTags=m(host,asdType.getName(),"declareTags",void.class,false);
        initializeAsd=m(host,asdType.getName(),"initAndGetPriorCondition",boolean.class,false);
        inTimeCondition=m(host,asdType.getName(),"getInTimeCondition",boolean.class,false);
        sampleTime=m(host,asdType.getName(),"getSampleTime",int.class,false);
        asdTag=m(host,asdType.getName(),"getTAG",String.class,false);
        parseResult=m(host,asdType.getName(),"parseComplexValueManually",void.class,false,CaptureResult.class);
        consumeResult=m(host,asdType.getName(),"consumeResultOnMainThreadIfDataChanged",void.class,false);
        registerTag=m(host,baseInterceptorType.getName(),"addAndGetTag",host.type("com.android.camera.module.interceptor.base.b"),false,CaptureResult.Key.class);
        sensorRect=m(host,"l9.h","d",Rect.class,true,capabilities);
        getCallback=m(host,module.getName(),"getModuleCallback",callbackType,false);
        getPreview=m(host,callbackType.getName(),"o0",preview,false);
        displayWidth=f(host,display.getName(),"s",int.class);
        displayHeight=f(host,display.getName(),"t",int.class);
        orientation=m(host,module.getName(),"getCameraDisplayOrientation",int.class,false);
        cropSourceTag=sf(host,"r8.e","b",vendor);
        cropVendor=m(host,"ia.E0","l",Object.class,true,CaptureResult.class,vendor,int.class);
        cropFromRequest=m(host,"Cw.t","C",float.class,true,Rect.class,Rect.class);
        cropFromResult=m(host,"Kr.i","l",float.class,true,Rect.class,CaptureResult.class);
        sensorTransform=m(host,"Cw.t","w",void.class,true,Matrix.class,Rect.class,float.class);
        displayTransform=m(host,"sm.b","e",void.class,true,Matrix.class,boolean.class,boolean.class,
                int.class,int.class,int.class,int.class,int.class,int.class,int.class);
        subjectConstructor=resultType.getDeclaredConstructor(Rect.class,int.class,int.class,int.class);
        subjectConstructor.setAccessible(true);
        subjectRect=f(host,resultType.getName(),"d",Rect.class);
        subjectType=f(host,resultType.getName(),"c",int.class);
        subjectStatus=f(host,resultType.getName(),"b",int.class);
        subjectPreviousRect=f(host,resultType.getName(),"g",Rect.class);
        subjectPreviousType=f(host,resultType.getName(),"f",int.class);
        subjectPreviousStatus=f(host,resultType.getName(),"e",int.class);
        overlayReceiver=m(host,"R6.u0","a",Optional.class,true);
        overlaySubjects=m(host,"R6.u0","a2",void.class,false,ArrayList.class,boolean.class,boolean.class);
        audioTracking=m(host,"com.android.camera.data.data.n","G",boolean.class,true,int.class);
        trackRect=m(host,module.getName(),"setTrackRect",void.class,false,Rect.class,int.class);
        audioZoom=m(host,module.getName(),"setAiAudioZoomLvManually",void.class,false,float.class);
        hintReceiver=m(host,"R6.m1","a",Optional.class,true);
        hint=m(host,"R6.m1","lj",void.class,false,int.class,int.class,String.class,long.class);
        protocolRegistry=sf(host,"O6.i$a","a",host.type("O6.i"));
        protocol=m(host,"O6.i","d",Optional.class,false,Class.class);
        zoomStart=m(host,zoomProtocolType.getName(),"kd",void.class,false,float.class,float.class,float.class,boolean.class);
        animator=f(host,zoomImplementation.getName(),"a",ValueAnimator.class);
        busyStart=m(host,"V6.b","b",boolean.class,true);
        busyAction=m(host,"V6.b","f",boolean.class,true);
        bottomReceiver=m(host,"R6.j0","a",Optional.class,true);
        openBottom=m(host,"R6.j0","h",void.class,false,int.class,int.class,int.class);
        menu=new MenuFactory(host);
        smart=new SmartDolly(this);
        dolly=new DollyRepair(this);
    }

    private static final class Tags {
        final Object enable,feature,result;
        Tags(Object enable,Object feature,Object result){this.enable=enable;this.feature=feature;this.result=result;}
    }
    private Tags tags(){
        Tags value=tags;
        if(value==null)synchronized(this){
            value=tags;
            if(value==null){
                value=new Tags(call(requestFactory,null,(Supplier<String>)()->SmartDolly.ENABLE,Boolean.class),
                        call(featureFactory,null,(Supplier<String>)()->"com.xiaomi.cinematicIntellTruck.FeatureEnable",Boolean.class),
                        call(resultFactory,null,(Supplier<String>)()->"com.xiaomi.cinematicIntellTruck.Results",Float[].class));
                tags=value;
            }
        }
        return value;
    }
    private Object component(Class<?> type){return call(component,call(running,null),type);}
    public Object capabilitiesOfOption(Object option){return read(optionCapabilities,option);}
    @Override public Object componentCapabilities(){return read(optionCapabilities,component(optionType));}
    @Override public String choice(){return(String)call(getChoice,component(optionType),227);}
    @Override public boolean requestKey(Object caps,String name){return(Boolean)call(requestKey,caps,name);}
    @Override public boolean featureKey(Object caps){return(Boolean)call(featureKey,caps,tags().feature);}
    @Override public List<String> resultKeys(Object caps){
        Object metadata=call(nativeMetadata,null,read(capabilityCharacteristics,caps));
        @SuppressWarnings("unchecked") ArrayList<CaptureResult.Key<?>> keys=(ArrayList<CaptureResult.Key<?>>)call(vendorKeys,metadata,CaptureResult.Key.class);
        if(keys==null)return null;
        ArrayList<String> names=new ArrayList<>();for(CaptureResult.Key<?> key:keys)names.add(key.getName());return names;
    }
    @Override public String resultName(){return(String)call(tagName,tags().result);}
    @Override public int cameraId(Object caps){return(Integer)read(capabilityId,caps);}
    @Override public void logGate(int cameraId,boolean registered){Log.i("SmartDollyGate","cameraId="+cameraId+" results_registered="+registered);}
    @Override public boolean alive(Object module){return(Boolean)call(moduleAlive,module);}
    @Override public int moduleIndex(Object module){return(Integer)call(moduleIndex,module);}
    @Override public boolean current(Object module){
        Optional<?> controller=(Optional<?>)call(currentController,null);
        if(!controller.isPresent()||!controllerType.isInstance(controller.get()))return false;
        Optional<?> active=(Optional<?>)call(currentModule,controller.get());return active.orElse(null)==module;
    }
    @Override public int activeMode(){Object source=call(globalSource,null);return(Integer)call(lookupMode,source,read(sourceIndex,source));}
    @Override public Object capabilities(Object manager){return call(managerCapabilities,manager);}
    @Override public void enableRequest(Object builder,boolean enabled){call(writeRequest,null,builder,tags().enable,enabled);}
    @Override public void enableSession(Object manager,boolean enabled){
        Object state=call(managerData,manager);call(writeSession,read(sessionTags,state),tags().feature,enabled);
    }
    @Override public void preference(String key,boolean value){call(preferenceWrite,call(running,null),key,value);}
    @Override public Object module(Object interceptor){return read(interceptorModule,interceptor);}
    @Override public Rect sensor(Object interceptor){return(Rect)call(sensorRect,null,read(interceptorCapabilities,interceptor));}
    @Override public int[] previewSize(Object module){Object display=call(getPreview,call(getCallback,module));return new int[]{(Integer)read(displayWidth,display),(Integer)read(displayHeight,display)};}
    @Override public Object registerResult(Object interceptor){return call(registerTag,interceptor,call(tagObject,tags().result));}
    @Override public Float[] values(Object holder){return(Float[])read(holderValue,holder);}
    @Override public float cropRatio(Rect sensor,Object result){
        CaptureResult capture=(CaptureResult)result;
        Integer source=(Integer)call(cropVendor,null,capture,read(cropSourceTag,null),0xdead);
        if(source!=null&&source==1)return(Float)call(cropFromRequest,null,sensor,capture.getRequest().get(CaptureRequest.SCALER_CROP_REGION));
        return(Float)call(cropFromResult,null,sensor,capture);
    }
    @Override public int orientation(Object module){return(Integer)call(orientation,module);}
    @Override public void sensorMatrix(Matrix matrix,Rect sensor,float ratio){call(sensorTransform,null,matrix,sensor,ratio);}
    @Override public void displayMatrix(Matrix matrix,int orientation,int width,int height,int sensorWidth,int sensorHeight){
        call(displayTransform,null,matrix,false,false,orientation,width,height,width/2,height/2,sensorWidth,sensorHeight);
    }
    @Override public Object subject(Rect rect,int type,int index,int status){return construct(subjectConstructor,rect,type,index,status);}
    @Override public void previous(Object next,Object previous){
        write(subjectPreviousRect,next,read(subjectRect,previous));
        write(subjectPreviousType,next,read(subjectType,previous));
        write(subjectPreviousStatus,next,read(subjectStatus,previous));
    }
    @Override public int previousStatus(Object previous){return(Integer)read(subjectPreviousStatus,previous);}
    @Override public void publish(ArrayList<Object> subjects){Optional<?> overlay=(Optional<?>)call(overlayReceiver,null);if(overlay.isPresent())call(overlaySubjects,overlay.get(),subjects,false,false);}
    @Override public boolean recording(Object module){return(Boolean)call(isRecording,module);}
    @Override public boolean audioTracking(Object module){return(Boolean)call(audioTracking,null,moduleIndex(module));}
    @Override public void track(Object module,Rect rect,float ratio){call(trackRect,module,rect,0);call(audioZoom,module,ratio);}
    @Override public boolean readPreference(String key,boolean fallback){return(Boolean)call(preferenceRead,call(running,null),key,fallback);}
    @Override public void writePreference(String key,boolean value){preference(key,value);}
    @Override public Object hintReceiver(){return((Optional<?>)call(hintReceiver,null)).orElse(null);}
    @Override public void hint(Object receiver,int visibility,int stringId,String key,long duration){call(hint,receiver,visibility,stringId,key,duration);}
    @Override public void log(String message){Log.d("CinematicDollyMultipleASD",message);}
    @Override public Object zoomController(){return((Optional<?>)call(protocol,read(protocolRegistry,null),zoomProtocolType)).orElse(null);}
    @Override public String range(int mode){return(String)call(getChoice,component(rangeType),mode);}
    @Override public String speed(int mode){return(String)call(getChoice,component(speedType),mode);}
    @Override public void start(Object controller,float from,float to,float speed,boolean stopShutter){call(zoomStart,controller,from,to,speed,stopShutter);}
    @Override public void stopAnimator(){
        Object controller=zoomController();if(zoomImplementation.isInstance(controller)){
            ValueAnimator animation=(ValueAnimator)read(animator,controller);
            if(animation!=null){animation.cancel();write(animator,controller,null);}
        }
    }
    @Override public void recordingPreference(boolean value){preference("pref_cinematic_dolly_zoom_is_recording",value);}
    @Override public void invalidNumber(NumberFormatException failure){Log.w("DollyRepair","Invalid dolly range or speed; recording continues without auto zoom",failure);}
    @Override public boolean busy(){return(Boolean)call(busyStart,null)||(Boolean)call(busyAction,null);}
    @Override public Object bottomProtocol(){return((Optional<?>)call(bottomReceiver,null)).orElse(null);}
    @Override public void openPanel(Object protocol){call(openBottom,protocol,7,0xd4,2);}
    public void addMenuEntry(List<Object> entries){entries.add(menu.create(dolly));}
    public void initializeModule(Object module)throws Throwable{
        XposedBridgeAccess.original(baseOnActive,module);
        if((Boolean)call(departed,module)){Log.w("CinematicModule","MODE_CINEMATIC isDeparted");return;}
        if(!smart.selected()){
            // F.C() is the stock Pe.b.M() capability touch; retain its current
            // side effect by executing the unchanged stock method at this point.
            call(menu.cinematicCapabilityTouch,null);
            float zoom=Float.valueOf((String)call(defaultZoom,null));
            @SuppressWarnings("unchecked") Consumer<Object> initializer=(Consumer<Object>)construct(zoomInitializer,zoom,1);
            @SuppressWarnings("unchecked") Optional<Object> controller=(Optional<Object>)call(currentController,null);
            controller.ifPresent(initializer);
        }
        write(needLockTip,module,true);write(needUnlockTip,module,true);write(hasDoubleTapped,module,false);
        Object cb=read(callback,module);
        Object render=cb==null?null:call(moduleRender,cb);
        if(render!=null)call(addRender,render,read(renderFeature,null));
        preference("pref_cinematic_intell_dolly_in_anime",false);
    }

    /** Only the observed constructor-less Z4.g$a menu builder is allocated. */
    private static final class MenuFactory {
        final Class<?> builder;
        final Object allocator;
        final Method allocate,factory,cinematicCapabilityTouch;
        final Field enabled,visible,id,order,icon,title,selected,click;
        MenuFactory(Host host)throws ReflectiveOperationException{
            builder=host.type("Z4.g$a");Class<?> base=host.type("Z4.a$a");
            if(builder.getSuperclass()!=base||builder.getDeclaredConstructors().length!=0)
                throw new NoSuchMethodException("Exact R8 Z4.g$a without constructor required");
            enabled=f(host,base.getName(),"k",boolean.class);visible=f(host,base.getName(),"l",boolean.class);
            id=f(host,base.getName(),"c",int.class);order=f(host,base.getName(),"o",int.class);
            icon=f(host,base.getName(),"d",int.class);title=f(host,base.getName(),"g",int.class);
            selected=f(host,base.getName(),"j",boolean.class);click=f(host,base.getName(),"a",View.OnClickListener.class);
            factory=m(host,builder.getName(),"f",host.type("Z4.g"),false);
            cinematicCapabilityTouch=m(host,"com.android.camera.data.data.F","C",void.class,true);
            Class<?> unsafe=Class.forName("sun.misc.Unsafe");
            Field singleton=unsafe.getDeclaredField("theUnsafe");singleton.setAccessible(true);allocator=singleton.get(null);
            allocate=unsafe.getDeclaredMethod("allocateInstance",Class.class);allocate.setAccessible(true);
        }
        Object create(final DollyRepair policy){
            Object value=call(allocate,allocator,builder);
            // Complete body of actual Z4.a$a(int): Object init, k/l=true, c=id.
            write(enabled,value,true);write(visible,value,true);write(id,value,24);
            write(order,value,0);write(icon,value,0x7f080689);write(title,value,0x7f1404a5);
            write(selected,value,policy.selected());
            write(click,value,new View.OnClickListener(){@Override public void onClick(View view){policy.click();}});
            return call(factory,value);
        }
    }
    private static final class XposedBridgeAccess {
        static void original(Method method,Object receiver)throws Throwable{
            de.robv.android.xposed.XposedBridge.invokeOriginalMethod(method,receiver,new Object[0]);
        }
    }
    public static Field f(Host host,String owner,String name,Class<?> type)throws ReflectiveOperationException{
        Field field=host.field(owner,name);if(field.getType()!=type||Modifier.isStatic(field.getModifiers()))
            throw new NoSuchFieldException(owner+"."+name+" instance/type contract");return field;
    }
    public static Field sf(Host host,String owner,String name,Class<?> type)throws ReflectiveOperationException{
        Field field=host.field(owner,name);if(field.getType()!=type||!Modifier.isStatic(field.getModifiers()))
            throw new NoSuchFieldException(owner+"."+name+" static/type contract");return field;
    }
    public static Method m(Host host,String owner,String name,Class<?> result,boolean isStatic,Class<?>... params)throws ReflectiveOperationException{
        Method method=host.method(owner,name,params);if(method.getReturnType()!=result||Modifier.isStatic(method.getModifiers())!=isStatic)
            throw new NoSuchMethodException(owner+"."+name+" return/static contract");return method;
    }
    public static Object read(Field field,Object receiver){try{return field.get(receiver);}catch(IllegalAccessException failure){throw new IllegalStateException("Cinematic field inaccessible: "+field,failure);}}
    public static void write(Field field,Object receiver,Object value){try{field.set(receiver,value);}catch(IllegalAccessException failure){throw new IllegalStateException("Cinematic field inaccessible: "+field,failure);}}
    public static Object call(Method method,Object receiver,Object... args){try{return method.invoke(receiver,args);}catch(IllegalAccessException failure){throw new IllegalStateException("Cinematic method inaccessible: "+method,failure);}catch(InvocationTargetException failure){throw propagate(failure.getCause(),method.toString());}}
    private static Object construct(Constructor<?> constructor,Object... args){try{return constructor.newInstance(args);}catch(InstantiationException|IllegalAccessException failure){throw new IllegalStateException("Cinematic constructor failed: "+constructor,failure);}catch(InvocationTargetException failure){throw propagate(failure.getCause(),constructor.toString());}}
    private static RuntimeException propagate(Throwable failure,String operation){if(failure instanceof RuntimeException)return(RuntimeException)failure;if(failure instanceof Error)throw(Error)failure;return new IllegalStateException("Cinematic host operation failed: "+operation,failure);}
}
