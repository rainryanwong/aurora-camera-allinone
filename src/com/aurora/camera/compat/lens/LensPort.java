package com.aurora.camera.compat.lens;

import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Exact 1770 declarations and the accepted physical-role call chain. */
public final class LensPort implements PhysicalLensRoute.Camera {
    private final Class<?> rolesType, zoomType, professionalZoom, professionalLens;
    private final Method roles, roleK, roleQ, roleE, roleJ, roleM, roleN, roleInitialized, roleAux;
    private final Field roleProvider, capabilitiesId;
    private final Method teleZoom, standaloneZoom, photoTarget, selectedZoom;
    private final Method moduleManager, managerRear, managerActual, managerCaps, moduleMode, reopen;
    private final Method crop, selectedToken, clearCrop, roleUltra, roleWide, roleTele, supportsCrop;
    private final Method featureProvider, featureInit, ultraSupport, repository, component, setValue;
    private final Field featureConfig;
    private final Method B2resolve, moduleAux, auxSetting, auxO, auxN, auxFeature, dataF, dummy;
    private final Method dataC, tripartite, lightTripartite;
    private final Method normalizeCamera, transientData, recordCamera;
    private final Method highPixelRoute, satG, satF, satE, satH;
    private final Method professionalSwitch, baseSwitch, childSwitch, baseBounds, childBounds;
    private final Field zoomModule;
    private final Method inheritedZoom, specialCapture, specialTest, videoGuard;
    private final Constructor<?> specialMapper;
    private final Method lensMap, validLens, running, front, zoomStops;
    private final Field lensEntries, entryToken, entryId, entryLabel, standaloneLabel;
    private final Constructor<?> entryConstructor;
    private final Method cameraSegments, segmentsNative, highPixel, cameraProtocols, registerProtocols;
    private final Field gripStops;
    private final Class<?>[] baseProtocols;
    private final Class<?> extraProtocol;
    private final DataZoomPort dataZoom;
    private final UltraPixelPort ultraPixel;
    private final PhysicalLensRoute route;
    private final NativeCaller caller;
    private final ThreadLocal<Object> segmentModule=new ThreadLocal<>();
    private final ThreadLocal<Boolean> registering=new ThreadLocal<>();
    private final ThreadLocal<BoundsCall> bounding=new ThreadLocal<>();
    private static final String PREVIOUS="aurora.compat.lens.previous";

    private static final class BoundsCall {
        final BoundsCall previous;
        Object childResult;
        BoundsCall(BoundsCall previous) { this.previous=previous; }
    }

    public LensPort(Host h) throws ReflectiveOperationException {
        rolesType=h.type("v6.f"); zoomType=h.type("i9.e"); professionalZoom=h.type("j9.w");
        professionalLens=h.type("r2.C0");
        roles=h.method(rolesType,"R"); roleK=h.method(rolesType,"K");
        roleQ=h.method(rolesType,"q"); roleE=h.method(rolesType,"e"); roleJ=h.method(rolesType,"j");
        roleM=h.method(rolesType,"M",int.class); roleN=h.method("v6.b","N");
        roleInitialized=h.method(rolesType,"isInitialized"); roleAux=h.method(rolesType,"k");
        roleProvider=h.field(rolesType,"a");
        teleZoom=h.method("Kr.i","h"); standaloneZoom=h.method("Kr.i","i");
        photoTarget=h.method("com.android.camera.data.data.n","m0",int.class);
        selectedZoom=h.method("com.android.camera.data.data.i","O",int.class);
        moduleManager=h.method("com.android.camera.module.a0","getCameraManager");
        moduleMode=h.method("com.android.camera.module.a0","getModuleIndex");
        Class<?> manager=moduleManager.getReturnType();
        managerRear=h.method(manager,"n0"); managerActual=h.method(manager,"getActualCameraId");
        managerCaps=h.method(manager,"T"); capabilitiesId=h.field(managerCaps.getReturnType(),"a");
        reopen=h.method(zoomType,"w9",h.type("com.android.camera.module.a0"),boolean.class);
        crop=h.method("com.android.camera.data.data.i","n1",int.class,boolean.class);
        selectedToken=h.method("com.android.camera.data.data.n","h",int.class);
        clearCrop=h.method("com.android.camera.data.data.n","S0",int.class);
        roleUltra=h.method(rolesType,"f0",int.class); roleWide=h.method(rolesType,"c0",int.class);
        roleTele=h.method(rolesType,"Z",int.class); supportsCrop=h.method("l9.h","T0",h.type("l9.g"));
        featureProvider=h.method("Pe.b","D"); featureInit=h.method("Pe.b","M1");
        featureConfig=h.field("Pe.b","e"); ultraSupport=h.method(featureConfig.getType(),"X3");
        repository=h.method("g2.a","a"); component=h.method("Xh.b","x",Class.class);
        setValue=h.method("com.android.camera.data.data.c","setComponentValue",int.class,String.class);
        B2resolve=h.method("B2.c","c",int.class,int.class,boolean.class);
        moduleAux=h.method("com.android.camera.module.c0","i");
        auxSetting=h.method("com.android.camera.data.data.x","S");
        auxO=h.method("com.android.camera.data.data.s","o"); auxN=h.method("com.android.camera.data.data.s","n");
        auxFeature=h.method("Pe.b","T1"); dataF=h.method("g2.a","f"); dummy=h.method("v2.F0","G");
        dataC=h.method("g2.a","c"); tripartite=h.method(dataC.getReturnType(),"R");
        lightTripartite=h.method("l9.h","d3",h.type("l9.g"));
        normalizeCamera=h.method("B2.c","a",int.class,int.class,int.class);
        transientData=h.method("g2.a","d"); recordCamera=h.method("t2.j","G",int.class);
        highPixelRoute=h.method("Pe.b","x1"); satG=h.method("l9.s0","g"); satF=h.method("l9.s0","f");
        satE=h.method("l9.s0","e"); satH=h.method("l9.s0","h");
        Class<?> module=h.type("com.android.camera.module.a0");
        professionalSwitch=h.method(zoomType,"J9",float.class,float.class,int.class,int.class,module);
        baseSwitch=h.method(zoomType,"K7",float.class,float.class,module);
        childSwitch=h.method("j9.z","K7",float.class,float.class,module);
        baseBounds=h.method(zoomType,"V0"); childBounds=h.method("j9.z","V0");
        zoomModule=h.field(zoomType,"b"); inheritedZoom=h.method("j9.r","L8",int.class,float.class,float.class);
        specialCapture=h.method("W6.c","a"); specialMapper=h.type("G6.g").getDeclaredConstructor(int.class);
        specialMapper.setAccessible(true); specialTest=h.method("java.util.Optional","map",java.util.function.Function.class);
        videoGuard=h.method("V6.b","j");
        lensMap=h.method(professionalLens,"q",int.class);
        validLens=h.method(professionalLens,"checkValueValid",int.class,String.class);
        lensEntries=h.field("r2.C0$c","a"); entryConstructor=h.type("r2.C0$a").getDeclaredConstructor();
        entryConstructor.setAccessible(true); entryToken=h.field("r2.C0$a","a");
        entryId=h.field("r2.C0$a","c"); entryLabel=h.field("r2.C0$a","b");
        standaloneLabel=h.field("Rh.e","pref_camera_zoom_mode_entry_tele_5X_abbr");
        running=h.method("g2.a","g"); front=h.method("u2.Y","N");
        zoomStops=h.method("com.android.camera.data.data.i","T",int.class,boolean.class);
        cameraSegments=h.method("com.android.camera.Camera","Ls",h.type("com.android.camera.Camera"),h.type("u6.g"));
        segmentsNative=h.method("com.android.camera.data.data.i","h0",List.class,int.class,float.class,float.class,List.class);
        highPixel=h.method("com.android.camera.data.data.i","r1",int.class); gripStops=h.field("X5.f","a");
        cameraProtocols=h.method("com.android.camera.Camera","registerProtocol");
        registerProtocols=h.method("q6.b","e",Class[].class);
        baseProtocols=new Class<?>[]{h.type("R6.h"),h.type("R6.M0"),h.type("R6.F0"),h.type("R6.F"),h.type("v8.b"),h.type("R6.S0")};
        extraProtocol=h.type("R6.e0");
        route=new PhysicalLensRoute(this); caller=new NativeCaller(h.loader);
        dataZoom=new DataZoomPort(h); ultraPixel=new UltraPixelPort(h);
    }

    private Object roleProvider() throws ReflectiveOperationException { return roles.invoke(null); }
    @Override public int standalone() throws ReflectiveOperationException { return (int)roleK.invoke(roleProvider()); }
    @Override public int tele() throws ReflectiveOperationException { return (int)roleQ.invoke(roleProvider()); }
    @Override public int wide() throws ReflectiveOperationException { return (int)roleE.invoke(roleProvider()); }
    @Override public int ultra() throws ReflectiveOperationException { return (int)roleJ.invoke(roleProvider()); }
    @Override public float teleZoom() throws ReflectiveOperationException { return (float)teleZoom.invoke(null); }
    @Override public boolean photoTarget(int mode) throws ReflectiveOperationException { return (boolean)photoTarget.invoke(null,mode); }
    @Override public float selectedZoom(int mode) throws ReflectiveOperationException { return (float)selectedZoom.invoke(null,mode); }
    @Override public boolean rear(Object module) throws ReflectiveOperationException {
        Object manager=moduleManager.invoke(module);
        return manager!=null && (int)managerRear.invoke(manager)==0;
    }
    @Override public int mode(Object module) throws ReflectiveOperationException { return (int)moduleMode.invoke(module); }
    @Override public int actual(Object module) throws ReflectiveOperationException { return (int)managerActual.invoke(moduleManager.invoke(module)); }
    @Override public void reopen(Object module,int mode,float from,float to,int actual,int target) throws ReflectiveOperationException {
        XposedBridge.log("AuroraCameraCompat physical route mode="+mode+" zoom="+from+"->"+to+" id="+actual+"->"+target);
        reopen.invoke(null,module,false);
    }

    public void install() {
        XposedBridge.hookMethod(B2resolve,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if ((int)p.args[0]!=0) return;
                synchronized(B2resolve.getDeclaringClass()) {
                    Object roles=roleProvider();
                    if (!(boolean)roleInitialized.invoke(roles) || (boolean)dummy.invoke(dataF.invoke(null))) return;
                    boolean aux=(boolean)moduleAux.invoke(null);
                    if (!aux && (boolean)auxSetting.invoke(null))
                        aux=(boolean)auxO.invoke(null)
                                || (boolean)auxFeature.invoke(featureProvider.invoke(null)) && (boolean)roleAux.invoke(roles)
                                || (boolean)auxN.invoke(null);
                    if (!aux) return;
                    if (!(boolean)tripartite.invoke(dataC.invoke(null))) {
                        android.util.SparseArray<?> caps=(android.util.SparseArray<?>)roleN.invoke(roleProvider.get(roles));
                        if ((boolean)lightTripartite.invoke(null,caps.get(wide()))) return;
                    }
                    int mode=(int)p.args[1];
                    int target=route.capture(0,mode);
                    if (target<0 && mode==173 && "Standalone".equals(selectedToken.invoke(null,mode))
                            && selectedZoom(mode)>=(float)standaloneZoom.invoke(null)) target=standalone();
                    if (target>=0) { resolvedCamera(p,mode,target); return; }
                    // The independent accepted high-pixel hunk also returns the live
                    // ultra role for cmpg-below-one (including NaN), even a missing role.
                    if (mode==175 && (boolean)highPixelRoute.invoke(featureProvider.invoke(null))
                            && ((boolean)satG.invoke(null) && (boolean)satF.invoke(null)
                            || (boolean)satG.invoke(null) && (boolean)satE.invoke(null)
                            || (boolean)satG.invoke(null) && (boolean)satH.invoke(null)
                            || (boolean)satH.invoke(null) && (boolean)satE.invoke(null))
                            && !(selectedZoom(mode)>=1.0f)) resolvedCamera(p,mode,ultra());
                }
            }
        });
        XC_MethodHook switching=new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                int result=route.switchLens((float)p.args[0],(float)p.args[1],p.args[2]);
                if (result>=0) p.setResult(result!=0);
            }
        };
        XposedBridge.hookMethod(baseSwitch,switching); XposedBridge.hookMethod(childSwitch,switching);
        XC_MethodHook boundHook=new LensHook() {
            @Override protected void before(MethodHookParam p) {
                BoundsCall call=new BoundsCall(bounding.get()); bounding.set(call); p.setObjectExtra(PREVIOUS,call);
            }
            @Override @SuppressWarnings("unchecked") protected void after(MethodHookParam p) throws Throwable {
                BoundsCall call=(BoundsCall)p.getObjectExtra(PREVIOUS);
                try {
                    if (!p.hasThrowable() && (call.childResult==null || call.childResult!=p.getResult())) {
                        Object module=((WeakReference<?>)zoomModule.get(p.thisObject)).get();
                        p.setResult(route.bounds(module,(List<Float>)p.getResult()));
                    }
                    if (!p.hasThrowable() && call.previous!=null) call.previous.childResult=p.getResult();
                } finally { if (call.previous==null) bounding.remove(); else bounding.set(call.previous); }
            }
        };
        XposedBridge.hookMethod(baseBounds,boundHook); XposedBridge.hookMethod(childBounds,boundHook);
        XposedBridge.hookMethod(professionalSwitch,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setResult(switchProfessional((float)p.args[0],(float)p.args[1],(int)p.args[2],(int)p.args[3],p.args[4]));
            }
        });
        XposedBridge.hookMethod(inheritedZoom,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (!professionalZoom.isInstance(p.thisObject)) return;
                Optional<?> capture=(Optional<?>)specialCapture.invoke(null);
                Object mapper=specialMapper.newInstance(6);
                Optional<?> mapped=(Optional<?>)specialTest.invoke(capture,mapper);
                if ((mapped.isPresent() && (boolean)mapped.get())
                        || (boolean)videoGuard.invoke(null) || (int)p.args[0]==2 || (int)p.args[0]==1) return;
                Object module=((WeakReference<?>)zoomModule.get(p.thisObject)).get();
                Object caps=managerCaps.invoke(moduleManager.invoke(module));
                if (switchProfessional((float)p.args[1],(float)p.args[2],(int)p.args[0],capabilitiesId.getInt(caps),module)) p.setResult(true);
            }
        });
        XposedBridge.hookMethod(lensMap,new LensHook() {
            @Override @SuppressWarnings("unchecked") protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) completeProLens((LinkedHashMap<String,Object>)lensEntries.get(p.getResult()),(int)p.args[0]);
            }
        });
        XposedBridge.hookMethod(validLens,new LensHook() {
            @Override protected void before(MethodHookParam p) { if ("Standalone".equals(p.args[1])) p.setResult(true); }
        });
        XposedBridge.hookMethod(cameraSegments,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS,segmentModule.get());
                segmentModule.set(p.args[0]);
            }
            @Override protected void after(MethodHookParam p) {
                Object previous=p.getObjectExtra(PREVIOUS); if (previous==null) segmentModule.remove(); else segmentModule.set(previous);
            }
        });
        XposedBridge.hookMethod(highPixel,new LensHook() {
            @Override protected void before(MethodHookParam p) {
                if (segmentModule.get()!=null && (int)p.args[0]==175 && caller.matches(highPixel,cameraSegments)) p.setResult(false);
            }
        });
        XposedBridge.hookMethod(segmentsNative,new LensHook() {
            @Override @SuppressWarnings("unchecked") protected void after(MethodHookParam p) throws Throwable {
                Object camera=segmentModule.get();
                if (camera!=null && !p.hasThrowable() && caller.matches(segmentsNative,cameraSegments))
                    completeProStops((ArrayList<Float>)gripStops.get(dataZoom.cameraGrip(camera)),(int)p.args[1]);
            }
        });
        XposedBridge.hookMethod(cameraProtocols,new LensHook() {
            @Override protected void before(MethodHookParam p) { p.setObjectExtra(PREVIOUS,registering.get()); registering.set(true); }
            @Override protected void after(MethodHookParam p) {
                Boolean old=(Boolean)p.getObjectExtra(PREVIOUS); if (old==null) registering.remove(); else registering.set(old);
            }
        });
        XposedBridge.hookMethod(registerProtocols,new LensHook() {
            @Override protected void before(MethodHookParam p) {
                if (!Boolean.TRUE.equals(registering.get()) || !Arrays.equals((Class<?>[])p.args[0],baseProtocols)) return;
                Class<?>[] next=Arrays.copyOf(baseProtocols,7); next[6]=extraProtocol; p.args[0]=next;
            }
        });
        dataZoom.install(); ultraPixel.install();
    }

    private void resolvedCamera(XC_MethodHook.MethodHookParam p,int mode,int target) throws ReflectiveOperationException {
        // Every inserted rear-route jump reaches the original common goto_10.
        // Keep its ID normalization and p2-controlled transient-data store.
        int actual=(int)normalizeCamera.invoke(null,0,target,mode);
        if ((boolean)p.args[2]) recordCamera.invoke(transientData.invoke(null),actual);
        XposedBridge.log("AuroraCameraCompat getActualOpenCameraId mode="+mode+" id=0->"+actual);
        p.setResult(actual);
    }

    private boolean switchProfessional(float from,float to,int action,int actual,Object module) throws ReflectiveOperationException {
        int mode=mode(module);
        if (action!=0 && action!=25 && action!=24 && action!=18) return false;
        boolean hasTele=tele()>0, changed=false;
        Object feature=featureProvider.invoke(null); featureInit.invoke(feature);
        String want="wide";
        // Dalvik cmpg below 1 includes NaN; retain the accepted branch comparison.
        if ((boolean)ultraSupport.invoke(featureConfig.get(feature)) && !(to>=1.0f) && !(boolean)roleUltra.invoke(null,actual)) { want="ultra"; changed=true; }
        if (to>=1.0f && !(boolean)roleWide.invoke(null,actual) && (!hasTele || to<teleZoom())) { want="wide"; changed=true; }
        if (standalone()>=0 && to>=(float)standaloneZoom.invoke(null)) { want="Standalone"; changed=true; }
        else if (hasTele && to>=teleZoom() && !(boolean)roleTele.invoke(null,actual)) { want="tele"; changed=true; }
        boolean cropEnabled=(boolean)crop.invoke(null,mode,false);
        int role="Standalone".equals(want)?standalone():"ultra".equals(want)?ultra():"wide".equals(want)?wide():tele();
        if (!(boolean)supportsCrop.invoke(null,roleM.invoke(roleProvider(),role))) clearCrop.invoke(null,mode);
        if (!changed) return false;
        Object config=component.invoke(repository.invoke(null),professionalLens);
        if (config!=null) setValue.invoke(config,mode,want);
        if (cropEnabled) return false;
        reopen.invoke(null,module,false); return true;
    }

    public void completeProLens(LinkedHashMap<String,Object> values,int mode) throws ReflectiveOperationException {
        if ((mode!=167 && mode!=180) || (boolean)front.invoke(running.invoke(null)) || values.containsKey("Standalone")) return;
        int id=standalone(); if (id<0) return;
        float[] stops=(float[])zoomStops.invoke(null,mode,false); if (stops==null) return;
        float target=(float)standaloneZoom.invoke(null); if (!(target>0.0f)) return;
        for (float stop:stops) if (stop==target) {
            Object entry=entryConstructor.newInstance(); entryToken.set(entry,"Standalone"); entryId.setInt(entry,id);
            entryLabel.setInt(entry,standaloneLabel.getInt(null)); values.put("Standalone",entry); return;
        }
    }

    public void completeProStops(ArrayList<Float> values,int mode) throws ReflectiveOperationException {
        if ((mode!=167 && mode!=180) || values==null || (boolean)front.invoke(running.invoke(null))
                || !(boolean)crop.invoke(null,mode,false)) return;
        float[] stops=(float[])zoomStops.invoke(null,mode,false); if (stops==null) return;
        for (float stop:stops) if (!(stop<=0.0f) && !values.contains(stop)) values.add(stop);
        Collections.sort(values);
    }
}
