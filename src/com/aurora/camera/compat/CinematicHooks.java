package com.aurora.camera.compat;

import android.view.View;
import com.aurora.camera.compat.cinematic.CinematicRuntime;
import com.aurora.camera.compat.cinematic.SmartDollyAsd;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Cinematic automatic dolly and complete smart request/result/UI lifecycle. */
public final class CinematicHooks {
    private static final String ASD="aurora.compat.cinematic.asd";
    private static final String APPEND="aurora.compat.cinematic.append";
    private static final String SESSION="aurora.compat.cinematic.session";
    private static final String START="aurora.compat.cinematic.start";
    private static final String STOP="aurora.compat.cinematic.stop";
    private CinematicHooks(){}

    private static final class Scope {
        final Object owner,argument;
        final boolean enabled;
        final Scope previous;
        Scope(Object owner,Object argument,boolean enabled,Scope previous){
            this.owner=owner;this.argument=argument;this.enabled=enabled;this.previous=previous;
        }
    }
    public static HookPlan resolve(Host host)throws ReflectiveOperationException{
        final CinematicRuntime runtime=new CinematicRuntime(host);
        return new HookPlan(){
            @Override public String id(){return "cinematic";}
            @Override public void install(){
                // Legacy invokeOriginalMethod falls back to virtual Method.invoke
                // for an unhooked parent. Establish its backup before replacing
                // Cinematic.onActive, otherwise the parent call re-enters this
                // replacement on a CinematicModule receiver. This callback never
                // changes any receiver's arguments, result or throwable.
                if(XposedBridge.hookMethod(runtime.baseOnActive,new StrictHook(){})==null)
                    throw new IllegalStateException("Cinematic parent onActive backup was not installed");
                XposedBridge.hookMethod(runtime.options,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        if(param.hasThrowable()||runtime.smart.supported(runtime.capabilitiesOfOption(param.thisObject)))return;
                        @SuppressWarnings("unchecked") List<Object> original=(List<Object>)param.getResult();
                        ArrayList<Object> items=new ArrayList<>(original);
                        items.remove(1); // Exact stock [off, smart, automatic] table.
                        List<Object> filtered=Collections.unmodifiableList(items);
                        CinematicRuntime.write(runtime.componentItems,param.thisObject,filtered);
                        param.setResult(filtered);
                    }
                });
                XposedBridge.hookMethod(runtime.menuEntries,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        if(!param.hasThrowable()){
                            @SuppressWarnings("unchecked") List<Object> entries=(List<Object>)param.getResult();
                            runtime.addMenuEntry(entries);
                        }
                    }
                });
                XposedBridge.hookMethod(runtime.panelInitialize,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        if(param.hasThrowable())return;
                        int index=(Integer)CinematicRuntime.read(runtime.panelIndex,param.thisObject);
                        Object option=CinematicRuntime.read(runtime.panelChoice,param.thisObject);
                        int automatic=(Integer)CinematicRuntime.call(runtime.findIndex,option,"2");
                        // Stock already handles index 2. When smart is unsupported,
                        // automatic moves to index 1; apply the missed native bind.
                        if(index!=2&&index==automatic){
                            ((View)CinematicRuntime.read(runtime.panelAutoLayout,param.thisObject)).setVisibility(View.VISIBLE);
                            CinematicRuntime.call(runtime.panelRefresh,param.thisObject);
                        }
                    }
                });
                XposedBridge.hookMethod(runtime.sessionMode,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        param.setResult(runtime.smart.selected()?0x8034:0x8032);
                    }
                });

                final ThreadLocal<Scope> session=new ThreadLocal<>();
                XposedBridge.hookMethod(runtime.sessionSettings,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope scope=new Scope(param.thisObject,param.args[0],true,session.get());
                        param.setObjectExtra(SESSION,scope);session.set(scope);
                    }
                    @Override protected void after(MethodHookParam param){
                        session.set(((Scope)param.getObjectExtra(SESSION)).previous);
                    }
                });
                XposedBridge.hookMethod(runtime.baseSessionSettings,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        Scope scope=session.get();
                        if(!param.hasThrowable()&&scope!=null&&scope.owner==param.thisObject&&scope.argument==param.args[0])
                            runtime.smart.session(param.args[0]);
                    }
                });
                XC_MethodHook preview=new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        runtime.smart.preview(param.args[0],CinematicRuntime.read(runtime.requestCapabilities,param.thisObject));
                    }
                };
                XposedBridge.hookMethod(runtime.previewSettings,preview);
                XposedBridge.hookMethod(runtime.recordSettings,preview);
                XposedBridge.hookMethod(runtime.requestSettings,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        runtime.smart.apply((Integer)param.args[0],param.args[1],
                                CinematicRuntime.read(runtime.requestCapabilities,param.thisObject));
                    }
                });

                final ThreadLocal<Scope> append=new ThreadLocal<>();
                XposedBridge.hookMethod(runtime.append,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope scope=new Scope(param.thisObject,param.args[0],runtime.smart.selected(),append.get());
                        param.setObjectExtra(APPEND,scope);append.set(scope);
                    }
                    @Override protected void after(MethodHookParam param){append.set(((Scope)param.getObjectExtra(APPEND)).previous);}
                });
                XposedBridge.hookMethod(runtime.registerInterceptor,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope scope=append.get();
                        if(scope!=null&&scope.enabled&&scope.argument==param.thisObject&&runtime.asdType.isInstance(param.args[0])){
                            XposedHelpers.setAdditionalInstanceField(param.args[0],ASD,new SmartDollyAsd(runtime,runtime.smart));
                        }
                    }
                });
                XposedBridge.hookMethod(runtime.declareTags,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        SmartDollyAsd state=state(param.thisObject);
                        if(state!=null){state.declare(param.thisObject);param.setResult(null);}
                    }
                });
                XposedBridge.hookMethod(runtime.initializeAsd,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        SmartDollyAsd state=state(param.thisObject);if(state!=null)param.setResult(state.initialize(param.thisObject));
                    }
                });
                XposedBridge.hookMethod(runtime.inTimeCondition,new StrictHook(){
                    @Override protected void before(MethodHookParam param){if(state(param.thisObject)!=null)param.setResult(true);}
                });
                XposedBridge.hookMethod(runtime.sampleTime,new StrictHook(){
                    @Override protected void before(MethodHookParam param){if(state(param.thisObject)!=null)param.setResult(30);}
                });
                XposedBridge.hookMethod(runtime.asdTag,new StrictHook(){
                    @Override protected void before(MethodHookParam param){if(state(param.thisObject)!=null)param.setResult("CinematicDollyMultipleASD");}
                });
                XposedBridge.hookMethod(runtime.parseResult,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        SmartDollyAsd state=state(param.thisObject);if(state!=null){state.parse(param.args[0]);param.setResult(null);}
                    }
                });
                XposedBridge.hookMethod(runtime.consumeResult,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        SmartDollyAsd state=state(param.thisObject);if(state!=null){state.consume(param.thisObject);param.setResult(null);}
                    }
                });
                XposedBridge.hookMethod(runtime.ignoreTap,new StrictHook(){
                    @Override protected void before(MethodHookParam param){param.setResult(runtime.smart.selected());}
                });
                XposedBridge.hookMethod(runtime.singleTap,new StrictHook(){
                    @Override protected void before(MethodHookParam param){if(runtime.smart.selected())param.setResult(null);}
                });
                XposedBridge.hookMethod(runtime.doubleTap,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        if(runtime.smart.selected()){
                            CinematicRuntime.call(runtime.focusRoi,param.args[1],
                                    CinematicRuntime.read(runtime.moduleIndexField,param.thisObject),param.args[0]);
                            param.setResult(null);
                        }
                    }
                });
                XposedBridge.hookMethod(runtime.onActive,new StrictHook(){
                    @Override protected void before(MethodHookParam param)throws Throwable{
                        runtime.initializeModule(param.thisObject);param.setResult(null);
                    }
                });
                XposedBridge.hookMethod(runtime.onInactive,new StrictHook(){
                    @Override protected void before(MethodHookParam param){runtime.smart.reset();runtime.dolly.stop();}
                });

                final ThreadLocal<Scope> starting=new ThreadLocal<>();
                XposedBridge.hookMethod(runtime.recordStarted,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope scope=new Scope(param.thisObject,null,true,starting.get());param.setObjectExtra(START,scope);starting.set(scope);
                    }
                    @Override protected void after(MethodHookParam param){starting.set(((Scope)param.getObjectExtra(START)).previous);}
                });
                XposedBridge.hookMethod(runtime.recorderStarted,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        Scope scope=starting.get();
                        if(!param.hasThrowable()&&scope!=null&&scope.owner==param.thisObject)
                            runtime.dolly.start((Integer)CinematicRuntime.read(runtime.moduleIndexField,param.thisObject));
                    }
                });
                final ThreadLocal<Scope> stopping=new ThreadLocal<>();
                XposedBridge.hookMethod(runtime.stopRecording,new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope scope=new Scope(param.thisObject,null,true,stopping.get());param.setObjectExtra(STOP,scope);stopping.set(scope);
                    }
                    @Override protected void after(MethodHookParam param){stopping.set(((Scope)param.getObjectExtra(STOP)).previous);}
                });
                XposedBridge.hookMethod(runtime.baseStopRecording,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        Scope scope=stopping.get();
                        if(!param.hasThrowable()&&scope!=null&&scope.owner==param.thisObject&&Boolean.TRUE.equals(param.getResult())){
                            runtime.smart.reset();runtime.dolly.stop();
                        }
                    }
                });
            }
        };
    }
    private static SmartDollyAsd state(Object interceptor){
        return(SmartDollyAsd)XposedHelpers.getAdditionalInstanceField(interceptor,ASD);
    }
}
