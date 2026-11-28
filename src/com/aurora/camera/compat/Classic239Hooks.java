package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CaptureRequest;
import android.os.Looper;
import android.util.Log;
import com.aurora.camera.compat.classic.ClassicCatalog239;
import com.aurora.camera.compat.classic.ClassicCvHooks240;
import com.aurora.camera.compat.classic.ClassicPreview241;
import com.aurora.camera.compat.classic.AsdSummary241;
import com.aurora.camera.compat.ui.NativePreferences240;
import com.aurora.camera.compat.ui.NativeNotes249;
import de.robv.android.xposed.XposedHelpers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipFile;
import org.json.JSONArray;
import org.json.JSONObject;

/** Phoenix classic scene LUT selection and the separate Photo-only Xiaomi ASD override. */
public final class Classic239Hooks {
    private static final String PREFS="aurora_classic239",MODEL="selected_model",ASD="aurora_xiaomi_asd239";
    private static final String PAGE="aurora_classic_page239",ENTRY="aurora_classic_style239";
    private static final String CHOICES="aurora_classic_choices239";
    private static final String HANDLEFIX_BRIDGE = "com.aurora.camera.compat.handlefix.classic.v1";
    private static final String STREET_CAPTURE_MODEL = "aurora.classic.streetCaptureModel";
    private static final int FILTER_RENDER_BIT=1,CV_RENDER_BIT=32;
    private static final String NOTE_TEXT="经过对比，14 Ultra、17 Ultra 与 13 Ultra 的经典风格LUT调色相同，因此只保留 13 Ultra 选项。";
    private Classic239Hooks(){}
    public static HookPlan resolve(final Host h)throws ReflectiveOperationException{
        final NativePreferences240 nativePreferences=new NativePreferences240(h);
        final NativeNotes249 notes=new NativeNotes249(h,nativePreferences);
        final ClassicCvHooks240 cvHooks=new ClassicCvHooks240(h);
        final ClassicPreview241 preview=new ClassicPreview241(h);
        final Method settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        final Method populate=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","addCurrentPreferences");
        final Method register=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","registerPreferenceListener");
        final Method initialize=h.method("com.android.camera.fragment.settings.b","initializeActivity");
        final Method resume=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","onResume");
        final Method rawBitmap=h.method("Nu.g","b",Context.class,String.class);
        final Method bitmap=h.method("com.xiaomi.utils.OpenGl3dUtils","a",Context.class,String.class,boolean.class,boolean.class);
        final Method publisher=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","lambda$saveLutToCameraFileDir$1",boolean.class,String.class,String.class,String.class);
        final Method write=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","writeLutToDir",String.class,String.class,byte[].class);
        final Method save=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","saveLutToCameraFileDir",String.class);
        final Method draw=h.method("o3.b","c",int.class,FloatBuffer.class,FloatBuffer.class,Context.class);
        final Method nativeDraw=h.method("Tu.h","g",h.type("Iu.w"));
        final Method nativeAttach=h.method("Tu.c","d",h.type("Iu.s"));
        final Method cameraResume=h.method("com.android.camera.a","onResume");
        final Class<?> nativeClassicRenderer=h.type("Tu.c");
        final java.lang.reflect.Field rendererToken=h.field("Tu.h","D"),rendererBitmap=h.field("Tu.h","J"),rendererAttribute=h.field("Tu.h","G");
        final java.lang.reflect.Field attributeToken=h.field("Mu.d","b");
        final Method renderParams=h.method("com.xiaomi.camera.mivi.filter.MIVIRenderTag","initParams");
        final Method requestParams=h.method("com.xiaomi.camera.mivi.common.MIVITag","getParams");
        final Method captureBitmaps=h.method("com.xiaomi.camera.mivi.filter.MIVIRenderTag","getLutBitmaps");
        final java.lang.reflect.Field renderType=h.field("com.xiaomi.camera.mivi.common.MIVITag","mType");
        final java.lang.reflect.Field renderCvId=h.field("com.xiaomi.camera.mivi.filter.MIVIRenderTag","mCvFilterEffectId");
        final java.lang.reflect.Field renderCvDegree=h.field("com.xiaomi.camera.mivi.filter.MIVIRenderTag","mCvFilterDegree");
        final Method asd=h.method("p9.b","b",CaptureRequest.Builder.class,boolean.class);
        final Method asdRequest=h.method("l9.o0","l",CaptureRequest.Builder.class,int.class,h.type("l9.j0"));
        final java.lang.reflect.Field requestMode=h.field("l9.j0","K3");
        final java.lang.reflect.Field asdTag=h.field("ia.A0","G1");
        final Method vendorKey=h.method("ia.D0","a");
        return new HookPlan(){
            private volatile Boolean latestNativeAsd;
            private final ThreadLocal<Integer> asdMode=new ThreadLocal<>();
            private final ThreadLocal<String> captureModel=new ThreadLocal<>();
            private final Set<String> publishedStreetLuts=new HashSet<>();
            private final Set<String> diagnostics=Collections.synchronizedSet(new HashSet<>());
            private final Map<String,String> lutDigests=new java.util.concurrent.ConcurrentHashMap<>();
            private volatile SharedPreferences preferences;
            private String lastAsd;
            private final java.util.concurrent.atomic.AtomicLong previewRevision=new java.util.concurrent.atomic.AtomicLong();
            private volatile long appliedPreviewRevision;
            private ClassicCvHooks240.Runtime cvRuntime;
            public String id(){return "239.phoenix-classic-scene-luts-and-xiaomi-asd";}
            Class<?> type(String name){try{return h.type(name);}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}}
            Object call(Object target,String name,Object...args){return XposedHelpers.callMethod(target,name,args);}
            Context context(){return(Context)XposedHelpers.callStaticMethod(type("com.xiaomi.camera.basic.Global"),"getContext");}
            SharedPreferences prefs(){
                SharedPreferences current=preferences;
                if(current==null){current=context().getSharedPreferences(PREFS,0);preferences=current;}
                return current;
            }
            String model(){return ClassicCatalog239.savedModel(prefs().getString(MODEL,"native"));}
            void diagnostic(String event,String value){if(diagnostics.add(event+" "+value))Log.i("AuroraClassic241",event+" "+value);}
            synchronized void asdDiagnostic(String state,Throwable failure){
                if(state.equals(lastAsd))return;lastAsd=state;
                if(failure==null)Log.i("AuroraASD241","request_written "+state);else Log.e("AuroraASD241",state,failure);
            }
            String sha256(byte[] bytes){
                try{byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder out=new StringBuilder(64);for(byte value:hash){out.append(Character.forDigit((value>>>4)&15,16));out.append(Character.forDigit(value&15,16));}return out.toString();}
                catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
            }
            int mode(){Object global=XposedHelpers.callStaticMethod(type("g2.a"),"g");return(Integer)call(global,"D",XposedHelpers.getObjectField(global,"u"));}
            Activity activity(Object fragment){return(Activity)call(fragment,"requireActivity");}
            boolean own(Object fragment){Activity owner=activity(fragment);return owner.getClass().getName().equals("com.android.camera.fragment.settings.PreferenceExtraActivity")&&owner.getIntent().getBooleanExtra(PAGE,false);}
            void title(Object fragment){com.aurora.camera.compat.ui.NativePageTitle257.set(activity(fragment),"徕卡经典风格");}
            Object listener(Runnable action){return Proxy.newProxyInstance(h.loader,new Class<?>[]{type("androidx.preference.Preference$d")},(proxy,method,args)->{
                if(method.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,method.getName(),args);
                if(!"onPreferenceClick".equals(method.getName()))throw new IllegalStateException(method.toString());action.run();return true;
            });}
            void open(Object fragment){
                Activity owner=activity(fragment);Intent intent=new Intent(owner,type("com.android.camera.fragment.settings.PreferenceExtraActivity"));
                intent.putExtra("target_tag","com.android.camera.fragment.settings.CameraPreferenceFragment");
                try{intent.putExtra("from_where",h.field("com.android.camera.fragment.settings.b","mFromWhere").getInt(null));}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                intent.putExtra(PAGE,true);XposedHelpers.setBooleanField(fragment,"mGoToActivity",true);owner.startActivity(intent);
            }
            Object preference(Object fragment,Object group,String key,String label,String summary){
                Object row=nativePreferences.text(fragment);
                call(row,"a0",key);call(row,"e0",label);call(row,"c0",summary);XposedHelpers.setBooleanField(row,"t",false);call(group,"j0",row);return row;
            }
            String asdSummary(Boolean override){
                boolean topAi=(Boolean)XposedHelpers.callStaticMethod(type("com.android.camera.data.data.i"),"i",163);
                return AsdSummary241.text(latestNativeAsd,topAi,override);
            }
            void refreshPreview(String reason){
                long revision=previewRevision.get();
                if(revision==appliedPreviewRevision)return;
                preview.request(reason,true,true,()->{if(previewRevision.get()==revision)appliedPreviewRevision=revision;});
            }
            void addSettings(Object fragment){
                Object screen=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
                Object group=call(screen,"k0","category_aurora_classic_controls239");
                if(group==null)group=nativePreferences.category(fragment,screen,NativePreferences240.CLASSIC_GROUP,null,-1999);
                Object style=call(group,"k0",ENTRY);
                if(style==null){style=preference(fragment,group,ENTRY,"徕卡经典风格",ClassicCatalog239.label(model()));XposedHelpers.setObjectField(style,"f",listener(()->open(fragment)));}
                else call(style,"c0",ClassicCatalog239.label(model()));
                Object toggle=call(group,"k0",ASD);
                if(toggle==null){
                    toggle=nativePreferences.toggle(fragment);
                    call(toggle,"a0",ASD);call(toggle,"e0","小米ASD");XposedHelpers.setBooleanField(toggle,"t",false);
                    XposedHelpers.setObjectField(toggle,"e",Proxy.newProxyInstance(h.loader,new Class<?>[]{type("androidx.preference.Preference$c")},(proxy,method,args)->{
                        if(method.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,method.getName(),args);
                        if(!"onPreferenceChange".equals(method.getName()))throw new IllegalStateException(method.toString());
                        Boolean selected=(Boolean)args[1];boolean saved=prefs().edit().putBoolean(ASD,selected).commit();
                        if(saved)call(args[0],"c0",asdSummary(selected));return saved;
                    }));call(group,"j0",toggle);
                }
                boolean nativeDefault=latestNativeAsd!=null?latestNativeAsd:(Boolean)XposedHelpers.callStaticMethod(type("com.android.camera.data.data.i"),"i",163);
                call(toggle,"setChecked",prefs().getBoolean(ASD,nativeDefault));
                call(toggle,"c0",asdSummary(prefs().contains(ASD)?prefs().getBoolean(ASD,false):null));
            }
            void populate(Object fragment){
                Object screen=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
                notes.addClassic(fragment,screen,NOTE_TEXT);
                Object choices=call(fragment,"addSingleChoicePreferenceCategory",screen,CHOICES,ClassicCatalog239.LABELS.clone(),ClassicCatalog239.IDS.clone(),true);
                XposedHelpers.setObjectField(choices,"p0",model());
                XposedHelpers.setObjectField(choices,"f",listener(()->{
                    selectModel((String)XposedHelpers.getObjectField(choices,"p0"));
                }));title(fragment);
            }
            void selectModel(String selected) {
                ClassicCatalog239.index(selected);
                if (!prefs().edit().putString(MODEL, selected).commit()) {
                    throw new IllegalStateException("无法保存徕卡经典风格");
                }
                Log.i("AuroraClassic241", "selection_committed model=" + selected);
                refresh();
            }

            boolean classicActive() {
                return Looper.myLooper() == Looper.getMainLooper() && cvRuntime.classicSelected();
            }

            Map<String, String> activeClassic() {
                if (!classicActive()) return null;
                String selected = model();
                Map<String, String> result = new HashMap<>();
                result.put("id", selected);
                result.put("label", ClassicCatalog239.label(selected));
                return Collections.unmodifiableMap(result);
            }

            String cycleClassic(List<String> allowed, HookDiagnostics.Operation operation) {
                try {
                    if (!classicActive()) return null;
                    String current = model();
                    String next = ClassicCatalog239.next(current, allowed);
                    if (next == null) return null;
                    if (!current.equals(next)) selectModel(next);
                    String actual = model();
                    if (!next.equals(actual)) return null;
                    operation.ran();
                    return ClassicCatalog239.label(actual);
                } catch (RuntimeException | LinkageError failure) {
                    operation.failed(failure);
                    throw failure;
                }
            }

            void publishHandleFix() {
                if (!HookDiagnostics.allHooksRegistered()) return;
                HookDiagnostics.Operation cycle = HookDiagnostics.operation("HandleFix 徕卡经典配方切换");
                Map<String, Object> bridge = new HashMap<>();
                bridge.put("ids", ClassicCatalog239.IDS.clone());
                bridge.put("labels", ClassicCatalog239.LABELS.clone());
                bridge.put("version", BuildInfo.VERSION_CODE);
                bridge.put("modes", ClassicCatalog239.modes());
                bridge.put("current", (Supplier<Map<String, String>>) this::activeClassic);
                bridge.put("cycle", (Function<List<String>, String>) allowed -> cycleClassic(allowed, cycle));
                XposedHelpers.setAdditionalInstanceField(h.loader, HANDLEFIX_BRIDGE, Collections.unmodifiableMap(bridge));
                cycle.registered();
            }
            byte[] asset(String selected,String scene)throws IOException{
                String name="assets/classic239/"+selected+"/lut_cvstyle_"+ClassicCatalog239.assetScene(selected,scene)+".png";
                try(ZipFile zip=new ZipFile(h.moduleApkPath)){
                    if(zip.getEntry(name)==null)throw new IOException("Missing classic LUT "+name);
                    try(InputStream in=zip.getInputStream(zip.getEntry(name));ByteArrayOutputStream out=new ByteArrayOutputStream()){
                        byte[] buffer=new byte[8192];int length;while((length=in.read(buffer))!=-1)out.write(buffer,0,length);return out.toByteArray();
                    }
                }
            }
            byte[] lutBytes(String selected,String scene)throws IOException{return asset(selected,scene);}
            String lutDigest(String selected,String scene,byte[] bytes){
                return lutDigests.computeIfAbsent(selected+"/"+scene,key->sha256(bytes));
            }
            Bitmap decode(String selected,String scene,String token)throws Throwable{
                byte[] bytes=lutBytes(selected,scene);Bitmap result=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                if(result==null||result.getWidth()!=512||result.getHeight()!=512){if(result!=null)result.recycle();throw new IOException("Invalid classic LUT "+selected+"/"+scene);}
                diagnostic(captureModel.get()==null?"preview_decode":"capture_bitmap_decode",
                        "model="+selected+" token="+token+" scene="+scene+" size=512x512 sha256="+lutDigest(selected,scene,bytes));return result;
            }
            JSONObject streetRequestParams(JSONObject params,int mask,String selected)throws Exception{
                if((mask&CV_RENDER_BIT)==0)return params;
                // initParams appends the ordinary filter (if present) before the CV LUT.
                int index=(mask&FILTER_RENDER_BIT)==0?0:1;
                JSONArray names=params.getJSONArray("lutNames");
                String token=names.getString(index),scene=ClassicCatalog239.scene(token);
                if(scene==null)return params;
                String directory=params.getString("lutPath");
                String alias;
                synchronized(publishedStreetLuts){
                    byte[] bytes=null;
                    String digest=lutDigests.get(selected+"/"+scene);
                    if(digest==null){bytes=lutBytes(selected,scene);digest=lutDigest(selected,scene,bytes);}
                    // MIVI consumes a filename, not the camera process's LUT cache entry.
                    alias="lut_cvstyle_aurora_"+digest.substring(0,24);
                    String key=directory+alias;
                    if(!publishedStreetLuts.contains(key)){
                        if(bytes==null)bytes=lutBytes(selected,scene);
                        if(!(Boolean)write.invoke(null,directory,alias+".png",bytes))
                            throw new IOException("Street CV LUT write rejected: "+directory+alias);
                        publishedStreetLuts.add(key);
                    }
                }
                JSONObject result=new JSONObject(params.toString());
                result.getJSONArray("lutNames").put(index,alias);
                for(String name:new String[]{"cvFilterLutName","cvFilterLookupTableName"})
                    if(token.equals(result.optString(name)))result.put(name,alias);
                diagnostic("street_capture_request","model="+selected+" token="+token+" lutPath="+directory+" name="+alias);
                return result;
            }
            boolean migrateRetiredModel(){
                SharedPreferences stored=prefs();String previous=stored.getString(MODEL,"native"),selected=ClassicCatalog239.savedModel(previous);
                if(previous.equals(selected))return false;
                if(!stored.edit().putString(MODEL,selected).commit())throw new IllegalStateException("无法迁移已移除的徕卡经典风格");
                Log.i("AuroraClassic242","retired_model_migrated from="+previous+" to="+selected);
                // Reuse selection publication to restore all 24 native LUT variants.
                refresh();return true;
            }
            void refresh(){
                previewRevision.incrementAndGet();
                cvRuntime.sync("model_selection");
                Object cache=XposedHelpers.callStaticMethod(type("com.xiaomi.camera.mivi.filter.MIVILutCache"),"getInstance");
                for(String scene:ClassicCatalog239.SCENES)for(String prefix:new String[]{"","fuxi_","bixi_","zizhan_"}){
                    String token="lut_cvstyle_"+prefix+scene;call(cache,"removeLutName",token);
                    try{save.invoke(null,token);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot schedule classic LUT publication",failure);}
                }
                Object effect=XposedHelpers.callStaticMethod(type("com.xiaomi.camera.effect.EffectController"),"u");
                refreshPreview("classic_model_selection");
                Log.i("AuroraClassic241","refresh_scheduled model="+model()+" publications=24 cvEffectId="+call(effect,"j")+" cvType="+XposedHelpers.callStaticMethod(type("com.android.camera.data.data.i"),"o"));
            }
            public void install(){
                notes.install();
                final java.util.function.Supplier<String> selected=this::model;
                final java.util.function.IntSupplier currentMode=this::mode;
                cvRuntime=cvHooks.install(new ClassicCvHooks240.Selection(){public String model(){return selected.get();}public int mode(){return currentMode.getAsInt();}});
                HookDiagnostics.hookMethod(settings,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())addSettings(p.thisObject);}});
                HookDiagnostics.hookMethod(populate,new StrictHook(){@Override protected void before(MethodHookParam p){if(own(p.thisObject)){populate(p.thisObject);p.setResult(null);}}});
                HookDiagnostics.hookMethod(register,new StrictHook(){@Override protected void before(MethodHookParam p){if(own(p.thisObject))p.setResult(null);}});
                StrictHook titled=new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&own(p.thisObject))title(p.thisObject);}};
                HookDiagnostics.hookMethod(initialize,titled);HookDiagnostics.hookMethod(resume,titled);
                HookDiagnostics.hookMethod(resume,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(!p.hasThrowable()&&type("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment").isInstance(p.thisObject)){addSettings(p.thisObject);nativePreferences.regroupLaboratory(p.thisObject);}
                }});
                HookDiagnostics.hookMethod(cameraResume,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(!p.hasThrowable()&&p.thisObject.getClass().getName().equals("com.android.camera.Camera")){
                        // Lifecycle runs on the UI thread, including launches into video.
                        if(migrateRetiredModel())return;
                        cvRuntime.sync("camera_resume");
                        if(cvRuntime.classicSelected())refreshPreview("classic_camera_resume");
                    }
                }});
                StrictHook lut=new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    String token=(String)p.args[1],scene=ClassicCatalog239.scene(token);if(scene==null)return;
                    String selected=captureModel.get();
                    if(selected==null)selected=model();
                    if(!"native".equals(selected))p.setResult(decode(selected,scene,token));
                }};
                HookDiagnostics.hookMethod(rawBitmap,lut);HookDiagnostics.hookMethod(bitmap,lut);
                HookDiagnostics.hookMethod(publisher,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    String token=(String)p.args[1],scene=ClassicCatalog239.scene(token);if(scene==null)return;
                    String selected=model();if("native".equals(selected))return;
                    byte[] bytes=lutBytes(selected,scene);
                    if(!(Boolean)write.invoke(null,p.args[2],p.args[3],bytes)){
                        call(XposedHelpers.callStaticMethod(type("com.xiaomi.camera.mivi.filter.MIVILutCache"),"getInstance"),"removeLutName",token);
                        throw new IOException("Native MIVI classic LUT publication rejected "+token);
                    }
                    diagnostic("mivi_publish","model="+selected+" token="+token+" directory="+p.args[2]+" file="+p.args[3]+" bytes="+bytes.length+" sha256="+lutDigest(selected,scene,bytes)+" accepted=true");
                    p.setResult(null);
                }});
                // RenderEngineV2's Tu.c inherits a token/Bitmap/CandySDK cache
                // from Tu.h. Its token is unchanged when only the model changes.
                // Nulling D/J on its GL draw thread makes both native branches
                // reload through OpenGl3dUtils and recreate CandySDK as needed.
                HookDiagnostics.hookMethod(nativeAttach,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;
                    // Reattaching Tu.h preserves J/D. Preserve their old model identity as well.
                    diagnostic("renderer_attached","model="+model()+" renderer="+p.thisObject.getClass().getName()+" token="+rendererToken.get(p.thisObject));
                }});
                HookDiagnostics.hookMethod(nativeDraw,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    if(!nativeClassicRenderer.isInstance(p.thisObject))return;
                    Object attribute=rendererAttribute.get(p.thisObject);if(attribute==null)return;
                    String token=(String)attributeToken.get(attribute);if(ClassicCatalog239.scene(token)==null)return;
                    String selected=model();Object previous=preview.cachedModel(p.thisObject);
                    if(!preview.prepareModel(p.thisObject,selected))return;
                    Log.i("AuroraClassic241","renderer_cache_invalidated previous="+previous+" model="+selected+" token="+token+" thread="+Thread.currentThread().getName());
                    p.setObjectExtra("classic241.drawModel",selected);
                    diagnostic("renderer_active","model="+selected+" renderer="+p.thisObject.getClass().getName()+" token="+token);
                }
                    @Override protected void after(MethodHookParam p){
                        Object selected=p.getObjectExtra("classic241.drawModel");
                        if(selected!=null)preview.completeModel(p.thisObject,(String)selected,!p.hasThrowable());
                    }
                });
                HookDiagnostics.hookMethod(renderParams,new StrictHook(){
                    @Override protected void before(MethodHookParam p){
                        p.setObjectExtra(STREET_CAPTURE_MODEL,captureModel.get());
                        if(!ClassicCatalog239.isStreetMode(mode()) || !cvRuntime.classicSelected())return;
                        String selected=model();
                        if("native".equals(selected))return;
                        captureModel.set(selected);
                        XposedHelpers.setAdditionalInstanceField(p.thisObject,STREET_CAPTURE_MODEL,selected);
                    }
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        try{
                            if(!p.hasThrowable())diagnostic("capture_effect","model="+model()+" cvEffectId="+renderCvId.getInt(p.thisObject)+" degree="+renderCvDegree.getInt(p.thisObject));
                        }finally{
                            String previous=(String)p.getObjectExtra(STREET_CAPTURE_MODEL);
                            if(previous==null)captureModel.remove();else captureModel.set(previous);
                        }
                    }
                });
                HookDiagnostics.Operation streetDelivery=HookDiagnostics.operation("街拍经典 LUT 成片请求");
                HookDiagnostics.hookMethod(requestParams,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(p.hasThrowable() || !(p.getResult() instanceof JSONObject))return;
                    String selected=(String)XposedHelpers.getAdditionalInstanceField(p.thisObject,STREET_CAPTURE_MODEL);
                    if(selected==null)return;
                    try{
                        JSONObject params=(JSONObject)p.getResult();
                        JSONObject routed=streetRequestParams(params,renderType.getInt(p.thisObject),selected);
                        if(routed!=params){p.setResult(routed);streetDelivery.ran();}
                    }catch(Exception failure){
                        streetDelivery.failed(failure);
                        Log.e("AuroraClassic241","street_capture_request_failed model="+selected,failure);
                    }
                }});
                streetDelivery.registered();
                HookDiagnostics.hookMethod(captureBitmaps,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable() || !(p.getResult() instanceof List<?>))return;
                    String selected=(String)XposedHelpers.getAdditionalInstanceField(p.thisObject,STREET_CAPTURE_MODEL);
                    int mask=renderType.getInt(p.thisObject);
                    if(selected==null || (mask&CV_RENDER_BIT)==0)return;
                    int index=(mask&FILTER_RENDER_BIT)==0?0:1;
                    List<?> bitmaps=(List<?>)p.getResult();
                    if(index<bitmaps.size() && bitmaps.get(index) instanceof Bitmap)
                        diagnostic("street_capture_bitmaps","model="+selected+" cvIndex="+index);
                }});
                HookDiagnostics.hookMethod(draw,new StrictHook(){@Override protected void before(MethodHookParam p){
                    if(ClassicCatalog239.scene((String)XposedHelpers.getObjectField(p.thisObject,"j"))==null)return;
                    String selected=model();Object previous=XposedHelpers.getAdditionalInstanceField(p.thisObject,"aurora.classic239.model");
                    if(selected.equals(previous))return;
                    int texture=(Integer)XposedHelpers.getObjectField(p.thisObject,"p");
                    // Native c() reloads a nonnegative texture after glIsTexture becomes false.
                    if(previous!=null&&!selected.equals(previous)&&texture>=0)XposedHelpers.callStaticMethod(type("com.xiaomi.gl.MIGL"),"glDeleteTexture",texture,"ClassicStyle239");
                    XposedHelpers.setAdditionalInstanceField(p.thisObject,"aurora.classic239.model",selected);
                }});
                HookDiagnostics.hookMethod(asd,new StrictHook(){
                    @Override protected void before(MethodHookParam p){
                        Integer request=asdMode.get();if(request==null)return;
                        p.setObjectExtra("asd240.mode",request);p.setObjectExtra("asd240.input",p.args[1]);
                        SharedPreferences preferences=prefs();Boolean override=preferences.contains(ASD)?preferences.getBoolean(ASD,false):null;
                        p.setObjectExtra("asd240.override",override);
                        if(request==163){latestNativeAsd=(Boolean)p.args[1];if(override!=null)p.args[1]=override;}
                    }
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        Object request=p.getObjectExtra("asd240.mode");if(request==null)return;
                        if(p.hasThrowable()){asdDiagnostic("request_failed mode="+request,p.getThrowable());return;}
                        CaptureRequest.Key<?> key=(CaptureRequest.Key<?>)vendorKey.invoke(asdTag.get(null));
                        Object value;try{value=((CaptureRequest.Builder)p.args[0]).get(key);}
                        catch(IllegalArgumentException failure){asdDiagnostic("request_read_failed mode="+request+" key="+key.getName(),failure);return;}
                        String state="mode="+request+" input="+p.getObjectExtra("asd240.input")+" override="+p.getObjectExtra("asd240.override")+" effective="+p.args[1]+" key="+key.getName()+" builder="+value;
                        asdDiagnostic(state,null);
                    }
                });
                HookDiagnostics.hookMethod(asdRequest,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{p.setObjectExtra("asd239.mode",asdMode.get());asdMode.set(requestMode.getInt(p.args[2]));}
                    @Override protected void after(MethodHookParam p){Integer previous=(Integer)p.getObjectExtra("asd239.mode");if(previous==null)asdMode.remove();else asdMode.set(previous);}
                });
                publishHandleFix();
            }
        };
    }
}
