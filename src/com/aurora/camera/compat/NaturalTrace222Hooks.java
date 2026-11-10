package com.aurora.camera.compat;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.RggbChannelVector;
import android.os.SystemClock;
import android.util.Log;
import com.aurora.camera.compat.trace.TraceWindow222;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Read-only Natural preview diagnostics. No parameter, return-value or buffer writes. */
public final class NaturalTrace222Hooks implements HookPlan {
    public static final String PROPERTY="debug.aurora.natural222",TAG="AuroraNatural222";
    private final Method data,current,facing,repo,get,value,key,filter,hdr,controller,keyName,config;
    private final Method setup,sessionWrite,completed,progressed,built,effectSet,effectAttrs,rendererAttrs,rendererEnable;
    private final Field intent,componentMode,sessionValues,configValues,configStyle,configModule;
    private final Class<?> styleType;
    private final TraceWindow222 trace;
    private final Map<Object,Integer> sessions=new WeakHashMap<>(),requests=new WeakHashMap<>();
    private int nextSession,nextRequest;
    private volatile boolean diagnosticFailed;

    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        Method property=h.method("miuix.core.util.SystemProperties","getBoolean",String.class,boolean.class);
        TraceWindow222 window=new TraceWindow222(()->{
            try{return (Boolean)property.invoke(null,PROPERTY,false);}catch(ReflectiveOperationException e){throw new IllegalStateException("222 diagnostic control",e);}
        },SystemClock::elapsedRealtimeNanos,row->{row.put("pid",android.os.Process.myPid());String line=new JSONObject(row).toString();if(line==null)throw new IllegalArgumentException("222 invalid diagnostic JSON");int bytes=line.getBytes(StandardCharsets.UTF_8).length;if(bytes>3500)throw new IllegalArgumentException("222 diagnostic row too long: "+bytes);Log.i(TAG,line);});
        return new NaturalTrace222Hooks(h,window);
    }
    NaturalTrace222Hooks(Host h,TraceWindow222 window)throws ReflectiveOperationException {
        trace=window;data=h.method("g2.a","g");current=h.method("u2.Y","D",int.class);facing=h.method("u2.Y","B");intent=h.field("u2.Y","u");
        repo=h.method("g2.a","a");get=h.method("Xh.b","x",Class.class);styleType=h.type("r2.m");componentMode=h.field("com.android.camera.data.data.c","mCurrentMode");
        value=h.method(styleType,"getComponentValue",int.class);key=h.method(styleType,"getKey",int.class);
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");filter=h.method("com.xiaomi.camera.effect.EffectController","m");hdr=h.method("com.xiaomi.camera.effect.EffectController","t");
        setup=h.method("com.android.camera.module.Camera2Module","setupCameraConfigForSessionIfNeed",h.type("k6.k"));
        config=h.method("k6.k","K0");configValues=h.field("l9.i0","a");configStyle=h.field("l9.j0","F3");configModule=h.field("l9.j0","K3");
        sessionWrite=h.method("l9.z1","a",h.type("ia.D0"),Object.class);sessionValues=h.field("l9.z1","b");keyName=h.method("ia.D0","b");
        completed=h.method("l9.y0$k","onCaptureCompleted",CameraCaptureSession.class,CaptureRequest.class,TotalCaptureResult.class);
        progressed=h.method("l9.y0$k","onCaptureProgressed",CameraCaptureSession.class,CaptureRequest.class,CaptureResult.class);
        built=CaptureRequest.Builder.class.getDeclaredMethod("build");built.setAccessible(true);
        effectSet=h.method("com.xiaomi.camera.effect.EffectController","d0",int.class);effectAttrs=h.method("com.xiaomi.camera.effect.EffectController","e0",int.class,int.class);
        rendererAttrs=h.method("F8.j","W0",h.type("Ku.d"),Object[].class);rendererEnable=h.method("F8.j","X0",h.type("Ku.d"),boolean.class);
    }
    public String id(){return "222.natural-preview-observer-default-off";}
    private interface Read {void run(XC_MethodHook.MethodHookParam p)throws Exception;}
    private void hook(Method method,Read read){XposedBridge.hookMethod(method,new XC_MethodHook(){@Override protected void afterHookedMethod(MethodHookParam p){
        // A diagnostic failure must not replace the original camera result or exception.
        if(p.hasThrowable()||diagnosticFailed)return;
        try{read.run(p);}catch(Exception failure){diagnosticFailed=true;Log.e(TAG,"222 observer failed; observation stopped, camera result retained",failure);}
    }});}
    private Map<String,Object> context()throws Exception {
        if(!trace.requested())return null;
        Object state=data.invoke(null);int mode=(Integer)current.invoke(state,intent.getInt(state)),front=(Integer)facing.invoke(state);
        if(!trace.poll((mode==163||mode==225||mode==229)&&front==0))return null;
        Object style=get.invoke(repo.invoke(null),styleType),effect=controller.invoke(null);int cm=componentMode.getInt(style);
        // UI mode is deliberately separate from vendor module data in each request/result.
        return TraceWindow222.fields("ui_mode_now",mode,"front_now",front,"component_mode_now",cm,"component_key_now",key.invoke(style,cm),"style_now",value.invoke(style,cm),"filter_now",filter.invoke(effect),"hdr_effect_now",hdr.invoke(effect));
    }
    private synchronized int identity(Map<Object,Integer> map,Object object,boolean session){Integer id=map.get(object);if(id==null){id=session?++nextSession:++nextRequest;map.put(object,id);}return id;}
    static boolean vendorAllowed(String name){String n=name.toLowerCase(java.util.Locale.ROOT);return n.equals("xiaomi.app.module")||n.equals("com.xiaomi.sessionparams.stylizationtype")||n.equals("org.quic.camera2.tuning.mode.tuningmode")||n.equals("org.quic.camera2.tuning.mode")||n.equals("org.codeaurora.qcamera3.privatedata.effectmode")||n.equals("org.quic.camera2.effectmode.value");}
    static Object safe(Object value){
        if(value==null||value instanceof Number||value instanceof Boolean)return value;
        if(value instanceof String){String s=(String)value;return s.length()<=256?s:TraceWindow222.fields("prefix",s.substring(0,256),"length",s.length(),"truncated",true);}
        if(value instanceof Enum)return ((Enum<?>)value).name();
        if(value.getClass().isArray()){int count=Array.getLength(value);List<Object> entries=new ArrayList<>();for(int i=0;i<Math.min(count,64);i++)entries.add(safe(Array.get(value,i)));return TraceWindow222.fields("values",entries,"length",count,"array_type",value.getClass().getComponentType().getName(),"truncated",count>64);}
        return TraceWindow222.fields("class",value.getClass().getName());
    }
    private Object typed(Object raw){return TraceWindow222.fields("java_type",raw==null?null:raw.getClass().getName(),"raw",safe(raw));}
    private Map<String,Object> requestVendors(CaptureRequest request){Map<String,Object> row=TraceWindow222.fields();for(CaptureRequest.Key<?> k:request.getKeys())if(vendorAllowed(k.getName()))row.put(k.getName(),typed(request.get(k)));return row;}
    private Map<String,Object> resultVendors(CaptureResult result){Map<String,Object> row=TraceWindow222.fields();for(CaptureResult.Key<?> k:result.getKeys())if(vendorAllowed(k.getName()))row.put(k.getName(),typed(result.get(k)));return row;}
    private void frame(XC_MethodHook.MethodHookParam p,boolean partial)throws Exception {
        Map<String,Object> row=context();if(row==null)return;
        CameraCaptureSession session=(CameraCaptureSession)p.args[0];CaptureRequest request=(CaptureRequest)p.args[1];CaptureResult result=(CaptureResult)p.args[2];
        Integer captureIntent=request.get(CaptureRequest.CONTROL_CAPTURE_INTENT);
        if(captureIntent==null||captureIntent!=CaptureRequest.CONTROL_CAPTURE_INTENT_PREVIEW)return;
        Map<String,Object> vendors=resultVendors(result);
        if(partial&&vendors.isEmpty())return;
        row.put("session",identity(sessions,session,true));row.put("request",identity(requests,request,false));row.put("logical_camera",session.getDevice().getId());row.put("frame",result.getFrameNumber());
        row.put("sensor_timestamp",result.get(CaptureResult.SENSOR_TIMESTAMP));row.put("capture_intent",captureIntent);row.put("active_physical_id",result.get(CaptureResult.LOGICAL_MULTI_CAMERA_ACTIVE_PHYSICAL_ID));
        row.put("zoom_ratio",result.get(CaptureResult.CONTROL_ZOOM_RATIO));row.put("lens_focal_length_mm",result.get(CaptureResult.LENS_FOCAL_LENGTH));
        row.put("ae_mode",result.get(CaptureResult.CONTROL_AE_MODE));row.put("ae_state",result.get(CaptureResult.CONTROL_AE_STATE));row.put("exposure_ns",result.get(CaptureResult.SENSOR_EXPOSURE_TIME));row.put("iso",result.get(CaptureResult.SENSOR_SENSITIVITY));
        row.put("ae_lock",result.get(CaptureResult.CONTROL_AE_LOCK));row.put("cc_mode",result.get(CaptureResult.COLOR_CORRECTION_MODE));
        row.put("awb_mode",result.get(CaptureResult.CONTROL_AWB_MODE));row.put("awb_state",result.get(CaptureResult.CONTROL_AWB_STATE));row.put("awb_lock",result.get(CaptureResult.CONTROL_AWB_LOCK));
        RggbChannelVector gains=result.get(CaptureResult.COLOR_CORRECTION_GAINS);row.put("awb_gains",gains==null?null:Arrays.asList(gains.getRed(),gains.getGreenEven(),gains.getGreenOdd(),gains.getBlue()));
        ColorSpaceTransform matrix=result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM);List<Object> ccm=null;if(matrix!=null){ccm=new ArrayList<>();for(int r=0;r<3;r++)for(int c=0;c<3;c++){android.util.Rational v=matrix.getElement(c,r);ccm.add(Arrays.asList(v.getNumerator(),v.getDenominator()));}}row.put("ccm_rationals",ccm);
        row.put("request_vendor",requestVendors(request));row.put("result_vendor",vendors);row.put("missing_vendor_is_not_provider_stability",true);
        trace.record(partial?"partial":"frame",row);
    }
    public void install(){
        hook(setup,p->{Map<String,Object> row=context();if(row!=null){Object values=configValues.get(config.invoke(p.args[0]));row.put("config_owner",System.identityHashCode(p.args[0]));row.put("app_config_style_F3",configStyle.getInt(values));row.put("app_config_K3_raw",configModule.getInt(values));row.put("stage","after_app_session_setup_not_provider_ack");trace.record("session_setup",row);}});
        hook(sessionWrite,p->{Map<String,Object> row=context();if(row==null)return;String name=(String)keyName.invoke(p.args[0]);if(!vendorAllowed(name))return;Map<?,?> stored=(Map<?,?>)sessionValues.get(p.thisObject);row.put("key",name);row.put("input",safe(p.args[1]));synchronized(p.thisObject){row.put("stored",stored.containsKey(p.args[0]));row.put("stored_value",safe(stored.get(p.args[0])));}row.put("stage","app_session_map_not_provider_ack");trace.record("session_tag",row);});
        hook(built,p->{Map<String,Object> row=context();if(row==null)return;CaptureRequest request=(CaptureRequest)p.getResult();row.put("request",identity(requests,request,false));row.put("request_vendor",requestVendors(request));row.put("stage","built_not_submission_ack");trace.record("request_built",row);});
        hook(completed,p->frame(p,false));hook(progressed,p->frame(p,true));
        for(Method method:new Method[]{effectSet,effectAttrs})hook(method,p->{Map<String,Object> row=context();if(row!=null){row.put("method",method.getName());row.put("args",safe(p.args));trace.record("effect_state",row);}});
        for(Method method:new Method[]{rendererAttrs,rendererEnable})hook(method,p->{Map<String,Object> row=context();if(row!=null){row.put("engine",System.identityHashCode(p.thisObject));row.put("method",method.getName());row.put("renderer",safe(p.args[0]));row.put("value",safe(p.args[1]));row.put("stage","app_renderer_call_not_present_ack");trace.record("renderer_state",row);}});
        Log.i(TAG,"222 observer ready; disabled unless "+PROPERTY+"=true; read-only; no provider-query claim");
    }
}
