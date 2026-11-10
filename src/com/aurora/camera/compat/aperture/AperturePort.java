package com.aurora.camera.compat.aperture;

import com.aurora.camera.compat.StrictHook;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Size;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** 1770 aperture consumers resolved before hooks are registered. */
public final class AperturePort {
    private final Class<?> aperture, professional, panel, manualAdapter, workspaceType, firstExposure, secondExposure;
    private final Method running, runningMode, runningRear, repository, getComponent;
    private final Field runningIndex, componentMode, supported, satSupported;
    private final Method componentValue, componentDefault, componentSet, componentItems;
    private final Method apertureApply, apertureValid, apertureAuto, apertureDisplay;
    private final Method protocol, protocolMode, protocolAperture, exclusionProtocol;
    private final Method enableClick, currentPreview;
    private final Field fragmentMode, selectors, rootView, resetView, selectorComponent;
    private final Field slideView, adapterComponent;
    private final Method drawAdapter;
    private final Constructor<?> selectorConstructor;
    private final Method largeScreen;
    private final Field manualPosition, selectedIndex, selectedText, seekbar, drawText;
    private final Method manualMap, manualLabel, selectedDrawData;
    private final Method componentSetup, componentInit, componentSupport, componentCheck,activeAperture,compareManual,compareSat;
    private final Method componentKey, professionalKey, componentTitle, discreteAperture;
    private final Constructor<?> compareModeA, compareModeB, discreteAdapter;
    private final Member[] titleOnlyScopes;
    private final Method createSlider, sliderClick, panelChange, sliderChange;
    private final Method createSelectors, renderRow, autoRow, disabled, resetParameters;
    private final Method resetState, updateResetState, cancelResetAnimation, animationRunning;
    private final Field resetAnimation;
    private final Method[] refreshMethods, releaseMethods;
    private final Method workspaceProvider, workspaceGet, preferences, preferenceInt;
    private final Method workspaceDirectory, workspacePrepare, workspaceLoad;
    private final Constructor<?> workspaceCallback;
    private final Method workspaceInit, workspaceRestore;
    private final Field workspaceComponents, componentTag;
    private final Method managerUpdate, managerApply, apertureSupportsValue;
    private final Field managerMode, managerComponent, managerValue, activeKind, zoom;
    private final Field autoValue;
    private final RowRenderer renderer;
    private final AiProfessionalAperture aiProfessional;

    private final ThreadLocal<Object> initializing = new ThreadLocal<>();
    private final ThreadLocal<Scope> scope = new ThreadLocal<>();
    private final ThreadLocal<Object> resetting = new ThreadLocal<>();
    private final ThreadLocal<WorkspaceCall> restoringWorkspace = new ThreadLocal<>();
    private static final String EXTRA = "aurora.compat.aperture.previous";

    private static final class Scope {
        final boolean title, forceDiscrete, programmatic;
        Scope(boolean title, boolean forceDiscrete, boolean programmatic) {
            this.title=title; this.forceDiscrete=forceDiscrete; this.programmatic=programmatic;
        }
    }

    private static final class WorkspaceCall {
        final Object workspace;
        final int mode;
        Object previousComponents;
        WorkspaceCall(Object workspace,int mode) { this.workspace=workspace;this.mode=mode; }
    }

    public AperturePort(Host h) throws ReflectiveOperationException {
        aperture=h.type("v2.k"); professional=h.type("r2.o0"); panel=h.type("Q4.D");
        manualAdapter=h.type("R4.f"); workspaceType=h.type("U9.s0");
        firstExposure=h.type("r2.D0"); secondExposure=h.type("r2.F0");
        Class<?> component=h.type("com.android.camera.data.data.c");
        running=h.method("g2.a","g"); runningMode=h.method("u2.Y","D",int.class);
        runningRear=h.method("u2.Y","L"); runningIndex=h.field("u2.Y","u");
        repository=h.method("g2.a","j"); getComponent=h.method("Xh.b","x",Class.class);
        componentMode=h.field(component,"mCurrentMode");
        supported=h.field(aperture,"W"); satSupported=h.field(aperture,"Y");
        componentValue=h.method(component,"getComponentValue",int.class);
        componentDefault=h.method(component,"getDefaultValue",int.class);
        componentSet=h.method(component,"setComponentValue",int.class,String.class);
        componentItems=h.method(aperture,"getItems"); apertureApply=h.method(aperture,"i",int.class,String.class);
        apertureValid=h.method(aperture,"checkValueValid",int.class,String.class);
        apertureAuto=h.method(aperture,"b"); apertureDisplay=h.method(aperture,"c",int.class);
        autoValue=h.field("e2.a","b");
        protocol=h.method("R6.C0","b"); protocolMode=h.method("R6.C0","getModuleIndex");
        protocolAperture=h.method("R6.C0","dh",int.class,String.class,String.class);
        exclusionProtocol=h.method("R6.o1","b");
        enableClick=h.method("com.android.camera.fragment.a","isEnableClick");
        currentPreview=h.method("com.android.camera.data.data.F","f");
        fragmentMode=h.field("com.android.camera.fragment.h","mCurrentMode");
        selectors=h.field(panel,"t"); rootView=h.field(panel,"r"); resetView=h.field(panel,"H");
        selectorComponent=h.field("Q4.x0","b"); slideView=h.field("Q4.v","i");
        adapterComponent=h.field("Q4.v","j");
        drawAdapter=h.method("com.android.camera.ui.CombineSlideView","getDrawAdapter");
        selectorConstructor=h.type("Q4.x0").getDeclaredConstructor(View.class,component,Size.class);
        selectorConstructor.setAccessible(true);
        largeScreen=h.method("Pe.b","Y");
        manualPosition=h.field(manualAdapter,"n"); selectedIndex=h.field("R4.G","g");
        selectedText=h.field("R4.G","h"); seekbar=h.field("micamx.compat.ui.widget.seekbar.e$a","c");
        drawText=h.field("lx.a","b"); manualMap=h.method(manualAdapter,"j",String.class);
        manualLabel=h.method(manualAdapter,"t",float.class,boolean.class);
        selectedDrawData=h.method("micamx.compat.ui.widget.seekbar.e","getMSelectDrawData");
        componentSetup=h.method(aperture,"F",int.class);
        componentInit=h.method(aperture,"N",h.type("com.android.camera.data.data.C"));
        componentSupport=h.method(aperture,"u",int.class);
        activeAperture=h.method("com.android.camera.data.data.A","k",int.class);
        compareManual=h.method(aperture,"m",aperture,int.class);compareSat=h.method(aperture,"n",aperture,int.class);
        componentCheck=h.method(aperture,"checkValueValid",int.class,String.class);
        componentKey=h.method(aperture,"getKey",int.class); professionalKey=h.method(professional,"getKey",int.class);
        componentTitle=h.method(aperture,"getDisplayTitleString"); discreteAperture=h.method(aperture,"I");
        compareModeA=h.type("v2.i").getDeclaredConstructor(int.class);
        compareModeB=h.type("v2.j").getDeclaredConstructor(int.class);
        discreteAdapter=h.type("R4.v").getDeclaredConstructor(Context.class,component,int.class,h.type("Q4.A0"));
        titleOnlyScopes=new Member[] {discreteAdapter,h.method("R4.v","a",int.class),
            h.method("R4.v","o",int.class,boolean.class),h.method("R4.v","s"),h.method("R4.v","t",float.class),
            h.method("Q4.v","Bs",boolean.class),h.method("Q4.v","updateView",View.class,Bundle.class)};
        createSlider=h.method("Q4.v","As",component); sliderClick=h.method("Q4.v","onClick",View.class);
        Class<?>[] changeParameters={component,String.class,String.class,boolean.class,int.class,int.class};
        panelChange=h.method(panel,"Vh",changeParameters); sliderChange=h.method("Q4.v","Vh",changeParameters);
        createSelectors=h.method("Q4.G","b",ViewGroup.class);
        renderRow=h.method("Q4.E","c",ArrayList.class,int.class,int.class,int.class);
        autoRow=h.method("Q4.E","a",component,int.class); disabled=h.method(aperture,"disableUpdate");
        panelClickGuard=h.method(panel,"Ms",component);
        resetParameters=h.method(panel,"Ef",int.class,h.type("U9.S"));
        resetState=h.method(panel,"Ps"); updateResetState=h.method(panel,"Rs",boolean.class);
        resetModified=h.method(panel,"Js");
        resetAnimation=h.field("com.airbnb.lottie.LottieAnimationView","h");
        cancelResetAnimation=h.method("com.airbnb.lottie.LottieAnimationView","j");
        animationRunning=h.method("p1.D","l");
        refreshMethods=new Method[] {updateResetState,
            h.method(panel,"notifyAfterFrameAvailable",int.class),h.method(panel,"notifyDataChanged",int.class,int.class),
            h.method(panel,"notifyPreviewRectChange",h.type("a6.h"),Rect.class,float.class,h.type("a6.p")),
            h.method(panel,"onContainerVisibilityChange",int.class,int.class,boolean.class),
            h.method(panel,"provideAnimateElement",int.class,List.class,int.class),
            h.method(panel,"provideRotateItem",List.class,int.class),h.method(panel,"setClickEnable",boolean.class),
            h.method(panel,"updateView",View.class,Bundle.class)};
        releaseMethods=new Method[] {h.method(panel,"onPause"),h.method(panel,"onStop"),
            h.method("com.android.camera.fragment.v","onDestroyView")};
        workspaceProvider=h.method("g2.a","e"); workspaceGet=h.method("y2.a","a",Class.class);
        preferences=h.method("g2.a","k"); preferenceInt=h.method("Xh.a","j",String.class,int.class);
        workspaceDirectory=h.method("U9.a","getWorkspaceDir"); workspacePrepare=h.method(workspaceType,"Q");
        workspaceLoad=h.method(workspaceType,"P",int.class,h.type("com.android.camera.fragment.v"),h.type("io.reactivex.functions.d"));
        workspaceCallback=h.type("Q4.x").getDeclaredConstructor(int.class,Object.class,Object.class);
        workspaceCallback.setAccessible(true);
        workspaceInit=h.method("z3.c","i",int.class);
        workspaceRestore=h.method("z3.b","M",int.class,h.type("z3.c"),boolean.class);
        workspaceComponents=h.field("U9.S","q");
        componentTag=h.field(component,"TAG");
        managerUpdate=h.method("U1.b","U8"); managerApply=h.method("U1.b","Rp",float.class);
        managerMode=h.field("U1.b","b"); managerComponent=h.field("U1.b","e"); managerValue=h.field("U1.b","c");
        activeKind=h.field(aperture,"r"); zoom=h.field(aperture,"g");
        apertureSupportsValue=h.method(aperture,"L",float.class);
        renderer=new RowRenderer(h,aperture,professional);
        aiProfessional=new AiProfessionalAperture(h);
    }

    public void install() {
        aiProfessional.install();
        XC_MethodHook setupAlias=new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                p.args[0]=ApertureRules.setupMode((Integer)p.args[0]);
            }
        };
        XposedBridge.hookMethod(componentSetup,setupAlias);
        XposedBridge.hookMethod(componentSupport,setupAlias);
        XposedBridge.hookMethod(activeAperture,new StrictHook(){
            @Override protected void before(MethodHookParam p){int mode=(Integer)p.args[0];if(mode==171||mode==175)p.args[0]=163;}
        });
        StrictHook membership239=new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                Object owner=p.args[0];int mode=componentMode.getInt(owner);
                if(initializing.get()==owner&&(mode==171||mode==175)&&(Integer)p.args[1]==163)p.setResult(true);
            }
        };
        XposedBridge.hookMethod(compareManual,membership239);XposedBridge.hookMethod(compareSat,membership239);
        XposedBridge.hookMethod(componentCheck,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                p.args[0]=ApertureRules.validationMode((Integer)p.args[0]);
            }
        });
        XC_MethodHook aiKey=new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                if ((Integer)p.args[0]==168) p.setResult("pref_camera_aperture_retain_key_ai");
                if ((Integer)p.args[0]==173) p.setResult("pref_camera_aperture_retain_key_night");
            }
        };
        XposedBridge.hookMethod(componentKey,aiKey); XposedBridge.hookMethod(professionalKey,aiKey);
        XposedBridge.hookMethod(componentInit,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(EXTRA,initializing.get());
                p.setObjectExtra(EXTRA+".oldMode",componentMode.getInt(p.thisObject));
                initializing.set(p.thisObject);
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                try {
                    int mode=componentMode.getInt(p.thisObject);
                    if (!p.hasThrowable() && (mode==168||mode==173))
                        satSupported.setBoolean(p.thisObject,supported.getBoolean(p.thisObject));
                    if(!p.hasThrowable()&&mode==225&&(Integer)p.getObjectExtra(EXTRA+".oldMode")!=225
                            &&supported.getBoolean(p.thisObject)){
                        String automatic=(String)autoValue.get(null);
                        componentSet.invoke(p.thisObject,225,automatic);apertureApply.invoke(p.thisObject,225,automatic);
                    }
                } finally { set(initializing,p.getObjectExtra(EXTRA)); }
            }
        });
        XC_MethodHook initializationMode=new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Object target=initializing.get();
                if (target!=null) {
                    int mode=componentMode.getInt(target);
                    if((mode==168||mode==171||mode==173||mode==175||mode==225)&&(Integer)p.args[0]==mode)p.args[0]=163;
                }
            }
        };
        XposedBridge.hookMethod(compareModeA,initializationMode); XposedBridge.hookMethod(compareModeB,initializationMode);
        XposedBridge.hookMethod(apertureApply,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (initializing.get()==p.thisObject && (componentMode.getInt(p.thisObject)==168||componentMode.getInt(p.thisObject)==173))
                    satSupported.setBoolean(p.thisObject,supported.getBoolean(p.thisObject));
            }
        });
        XposedBridge.hookMethod(componentTitle,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Scope current=scope.get();
                if (current!=null && current.title && isAi(p.thisObject,mode())) p.setResult(0x7f140d73);
            }
        });
        XposedBridge.hookMethod(discreteAperture,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Scope current=scope.get();
                if (current!=null && current.forceDiscrete && isAi(p.thisObject,mode())) p.setResult(true);
            }
        });
        for (Member method:titleOnlyScopes) titleScope(method,false,false);
        // Native I() selects the professional R4.f continuous ruler for c0=2/3.
        // A genuinely discrete capability still keeps its original R4.v path.
        titleScope(createSlider,false,false); titleScope(sliderClick,false,true);
        changeHook(panelChange,false); changeHook(sliderChange,true);
        XposedBridge.hookMethod(autoRow,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (aperture.isInstance(p.args[0])) p.setResult(apertureAuto.invoke(p.args[0]));
            }
        });
        XposedBridge.hookMethod(createSelectors,new StrictHook() {
            @SuppressWarnings("unchecked")
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) addSelector((ArrayList<Object>)p.getResult(),(ViewGroup)p.args[0]);
            }
        });
        XposedBridge.hookMethod(renderRow,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                // Stock c consumes getDefaultValueDisplayString as a resource
                // before an after-hook can repair a disabled base-aperture row.
                if (renderer.bind((ArrayList<?>)p.args[0],(Integer)p.args[1],
                        (Integer)p.args[2],(Integer)p.args[3],mode())) p.setResult(null);
            }
        });
        XposedBridge.hookMethod(findPanelClickGuard(),new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (isAi(p.args[0],fragmentMode.getInt(p.thisObject)) && (Boolean)disabled.invoke(p.args[0]))
                    p.setResult(false);
            }
        });
        installReset(); installWorkspace();
        XposedBridge.hookMethod(managerUpdate,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (managerMode.getInt(p.thisObject)!=168) return;
                Object component=managerComponent.get(p.thisObject);
                String value=(String)componentValue.invoke(component,168);
                if (activeKind.getInt(component)==2) {
                    float target=(Boolean)apertureSupportsValue.invoke(component,zoom.getFloat(component))
                            ? Float.parseFloat(value) : -1.0f;
                    if (managerValue.getFloat(p.thisObject)!=target) {
                        managerApply.invoke(p.thisObject,target);
                        Log.d("AuroraCameraCompat","updateSatWideAperture target="+target);
                    }
                }
                p.setResult(null);
            }
        });
    }

    private Method panelClickGuard;
    private Method findPanelClickGuard() { return panelClickGuard; }

    private void titleScope(Member member,final boolean force,final boolean click) {
        XposedBridge.hookMethod(member,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                Scope previous=scope.get(); p.setObjectExtra(EXTRA,previous);
                scope.set(new Scope(true,force || previous!=null && previous.forceDiscrete,
                        click || previous!=null && previous.programmatic));
            }
            @Override protected void after(MethodHookParam p) {
                set(scope,(Scope)p.getObjectExtra(EXTRA));
            }
        });
    }

    private void changeHook(Method method,final boolean slider) {
        XposedBridge.hookMethod(method,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Scope previous=scope.get(); p.setObjectExtra(EXTRA,previous);
                scope.set(new Scope(false,false,previous!=null && previous.programmatic));
                try {
                    Object component=p.args[0]; int mode=(Integer)p.args[4];
                    if (slider && previous!=null && previous.programmatic && aperture.isInstance(component)) {
                        Object adapter=drawAdapter.invoke(slideView.get(p.thisObject));
                        if (manualAdapter.isInstance(adapter)) syncProgrammatic(adapter,(String)p.args[2]);
                    }
                    if (!isAi(component,mode) || !(Boolean)enableClick.invoke(p.thisObject)
                            || fragmentMode.getInt(p.thisObject)!=mode) return;
                    Object callback=protocol.invoke(null);
                    if (callback==null || (Integer)protocolMode.invoke(callback)!=mode
                            || exclusionProtocol.invoke(null)==null) return;
                    apply(component,mode,(String)p.args[1],(String)p.args[2],callback);
                } catch (Throwable failure) {
                    set(scope,previous); throw failure;
                }
            }
            @Override protected void after(MethodHookParam p) {
                set(scope,(Scope)p.getObjectExtra(EXTRA));
            }
        });
    }

    private int mode() throws ReflectiveOperationException {
        Object data=running.invoke(null);
        return (Integer)runningMode.invoke(data,runningIndex.getInt(data));
    }

    private Object component() throws ReflectiveOperationException {
        return getComponent.invoke(repository.invoke(null),aperture);
    }

    private boolean isAi(Object data,int mode) {
        return ApertureRules.ai(mode,aperture.isInstance(data),professional.isInstance(data));
    }

    private void apply(Object component,int mode,String oldValue,String newValue,Object callback)
            throws ReflectiveOperationException {
        if (!supported.getBoolean(component)) return;
        if (!autoValue.get(null).equals(newValue)
                && !(Boolean)apertureValid.invoke(component,mode,newValue)) return;
        componentSet.invoke(component,mode,newValue);
        apertureApply.invoke(component,mode,newValue);
        protocolAperture.invoke(callback,1,oldValue,newValue);
    }

    private void syncProgrammatic(Object adapter,String value) throws ReflectiveOperationException {
        if (!ApertureRules.programmaticValue(value)) return;
        int position=Math.round((Float)manualMap.invoke(adapter,value));
        manualPosition.setFloat(adapter,(float)position);
        selectedIndex.setInt(adapter,position); selectedText.set(adapter,value);
        Object bar=seekbar.get(adapter);
        if (bar==null) return;
        Object drawData=selectedDrawData.invoke(bar);
        if (drawData==null) return;
        drawText.set(drawData,manualLabel.invoke(adapter,(float)position,true));
        ((View)bar).invalidate();
    }

    private void addSelector(ArrayList<Object> result,ViewGroup root) throws ReflectiveOperationException {
        if (mode()!=168 || !(Boolean)runningRear.invoke(running.invoke(null))) return;
        Object component=component();
        if (component==null || !supported.getBoolean(component)
                || ((List<?>)componentItems.invoke(component)).isEmpty()) return;
        for (Object selector:result) if (aperture.isInstance(selectorComponent.get(selector))) return;
        Context context=root.getContext();
        int width=context.getResources().getDimensionPixelSize(0x7f070767);
        int height=context.getResources().getDimensionPixelSize((Boolean)largeScreen.invoke(null)
                ? 0x7f070907 : 0x7f07076c);
        View view=LayoutInflater.from(context).inflate(0x7f0e015f,root,false);
        view.setId(View.generateViewId()); view.setTag(component);
        Object selector=selectorConstructor.newInstance(view,component,new Size(width,height));
        // The accepted helper inserts after the original aperture slot, before ET/ISO/WB.
        int index=0;
        if (!result.isEmpty()) {
            Object first=selectorComponent.get(result.get(0));
            if (firstExposure.isInstance(first) || secondExposure.isInstance(first)) index=1;
        }
        result.add(index,selector);
    }

    private boolean sixItems(Object fragment) throws IllegalAccessException {
        if (fragmentMode.getInt(fragment)!=168) return false;
        List<?> list=(List<?>)selectors.get(fragment);
        if (list==null || list.size()!=6) return false;
        for (Object item:list) if (isAi(selectorComponent.get(item),168)) return true;
        return false;
    }

    private void refresh(Object fragment) throws ReflectiveOperationException {
        ResetMirror.update((View)rootView.get(fragment),(View)resetView.get(fragment),
                fragmentMode.getInt(fragment),sixItems(fragment),(Rect)currentPreview.invoke(null));
    }

    private void installReset() {
        XposedBridge.hookMethod(resetState,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (sixItems(p.thisObject)) { resetForSix(p.thisObject); p.setResult(null); }
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) refresh(p.thisObject);
            }
        });
        XC_MethodHook update=new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) refresh(p.thisObject);
            }
        };
        for (Method method:refreshMethods) XposedBridge.hookMethod(method,update);
        XC_MethodHook release=new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (panel.isInstance(p.thisObject)) ResetMirror.release((View)rootView.get(p.thisObject));
            }
        };
        for (Method method:releaseMethods) XposedBridge.hookMethod(method,release);
        XposedBridge.hookMethod(resetParameters,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                p.setObjectExtra(EXTRA,resetting.get()); resetting.set(p.thisObject);
            }
            @Override protected void after(MethodHookParam p) {
                set(resetting,p.getObjectExtra(EXTRA));
            }
        });
        XposedBridge.hookMethod(cancelResetAnimation,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                Object fragment=resetting.get();
                if (p.hasThrowable() || fragment==null || resetView.get(fragment)!=p.thisObject
                        || fragmentMode.getInt(fragment)!=168) return;
                Object component=component();
                if (component!=null) apertureApply.invoke(component,168,componentValue.invoke(component,168));
            }
        });
    }

    private Method resetModified;
    private void resetForSix(Object fragment) throws ReflectiveOperationException {
        View reset=(View)resetView.get(fragment); reset.setVisibility(View.VISIBLE);
        if ((Boolean)animationRunning.invoke(resetAnimation.get(reset))) return;
        int used=(Integer)preferenceInt.invoke(preferences.invoke(null),
                "pref_camera_manual_workspace_used_index_key",0);
        if (used==0) {
            updateResetState.invoke(fragment,resetModified.invoke(fragment));
            return;
        }
        Object workspace=workspaceGet.invoke(workspaceProvider.invoke(null),workspaceType);
        if (!new File((String)workspaceDirectory.invoke(workspace)).exists()) workspacePrepare.invoke(workspace);
        Object callback=workspaceCallback.newInstance(0,fragment,workspace);
        workspaceLoad.invoke(workspace,fragment.hashCode(),fragment,callback);
    }

    private void installWorkspace() {
        XposedBridge.hookMethod(workspaceRestore,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                p.setObjectExtra(EXTRA,restoringWorkspace.get());
                restoringWorkspace.set(new WorkspaceCall(p.args[1],(Integer)p.args[0]));
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                try {
                    WorkspaceCall call=restoringWorkspace.get();
                    if (call.previousComponents!=null) workspaceComponents.set(call.workspace,call.previousComponents);
                } finally { set(restoringWorkspace,(WorkspaceCall)p.getObjectExtra(EXTRA)); }
            }
        });
        XposedBridge.hookMethod(workspaceInit,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(EXTRA,workspaceComponents.get(p.thisObject));
            }
            @SuppressWarnings("unchecked")
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable() || (Integer)p.args[0]!=168) return;
                Object old=p.getObjectExtra(EXTRA);
                WorkspaceCall call=restoringWorkspace.get();
                if (old!=null && (call==null || call.workspace!=p.thisObject || call.mode!=168)) return;
                ArrayList<Object> values=(ArrayList<Object>)workspaceComponents.get(p.thisObject);
                ArrayList<Object> filtered=new ArrayList<>();
                Object runningAperture=old==null ? component() : null;
                for (Object value:values) {
                    boolean skip=old==null ? value==runningAperture
                            : "ComponentRunningAperture".equals(componentTag.get(value));
                    if (!skip) filtered.add(value);
                }
                if (old!=null) call.previousComponents=values;
                workspaceComponents.set(p.thisObject,filtered);
            }
        });
    }

    private static <T> void set(ThreadLocal<T> local,T previous) {
        if (previous==null) local.remove(); else local.set(previous);
    }
}
