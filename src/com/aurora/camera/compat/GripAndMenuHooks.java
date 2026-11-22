package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;
import com.aurora.camera.diagnostics.ObservedHook;

import android.hardware.camera2.CaptureRequest;
import android.view.KeyEvent;
import com.aurora.camera.compat.gripmenu.GripMenuRuntime;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Last accepted grip, menu upgrade, street and Legend request deltas. */
public final class GripAndMenuHooks {
    private GripAndMenuHooks(){}
    private static final class Scope {
        final Object owner,argument;final int mode;final Scope previous;boolean busy;
        Scope(Object owner,Object argument,int mode,Scope previous){this.owner=owner;this.argument=argument;this.mode=mode;this.previous=previous;}
    }
    private static ObservedHook scope(final ThreadLocal<Scope> local,final String key,final int argument,final int modeArgument){
        return new StrictHook(){
            @Override protected void before(MethodHookParam param){
                Scope current=new Scope(param.thisObject,argument<0?null:param.args[argument],modeArgument<0?0:(Integer)param.args[modeArgument],local.get());
                param.setObjectExtra(key,current);local.set(current);
            }
            @Override protected void after(MethodHookParam param){local.set(((Scope)param.getObjectExtra(key)).previous);}
        };
    }
    public static HookPlan resolve(Host host)throws ReflectiveOperationException{
        final GripMenuRuntime runtime=new GripMenuRuntime(host);
        final Method migrate=GripMenuRuntime.m(host,"u2.y","T",void.class,false,int.class,ArrayList.class);
        final Method segmented=GripMenuRuntime.m(host,"com.android.camera.data.data.i","r1",boolean.class,true,int.class);
        return new HookPlan(){
            @Override public String id(){return "grip-and-menu";}
            @Override public void install(){
                final ThreadLocal<Scope> down=new ThreadLocal<>(),up=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.keyDown,scope(down,"grip.down",1,0));
                HookDiagnostics.hookMethod(runtime.keyUp,scope(up,"grip.up",1,0));
                HookDiagnostics.hookMethod(runtime.lock,new StrictHook(){
                    @Override protected void before(MethodHookParam param)throws Throwable{
                        Scope current=down.get();if(current==null||current.owner!=param.thisObject||current.mode!=80||current.busy)return;
                        current.busy=true;try{runtime.grip.down(param.thisObject,(KeyEvent)current.argument);param.setResult(null);}finally{current.busy=false;}
                    }
                });
                ObservedHook release=new StrictHook(){
                    @Override protected void before(MethodHookParam param){
                        Scope current=up.get();if(current==null||current.owner!=param.thisObject||current.mode!=80||current.busy)return;
                        current.busy=true;try{if(runtime.grip.up(param.thisObject,(KeyEvent)current.argument))param.setResult(null);}finally{current.busy=false;}
                    }
                };
                HookDiagnostics.hookMethod(runtime.cancel,release);HookDiagnostics.hookMethod(runtime.cancelVideo,release);HookDiagnostics.hookMethod(runtime.cancelStreet,release);
                HookDiagnostics.hookMethod(runtime.cancelPhoto,release);HookDiagnostics.hookMethod(runtime.cancelDual,release);
                final ThreadLocal<Scope> initializing=new ThreadLocal<>(),slider=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.stops,new StrictHook(){
                    @Override protected void before(MethodHookParam param){Scope current=new Scope(param.thisObject,null,(Integer)param.args[0],initializing.get());param.setObjectExtra("grip.stops",current);initializing.set(current);}
                    @Override protected void after(MethodHookParam param){
                        Scope current=(Scope)param.getObjectExtra("grip.stops");initializing.set(current.previous);
                        if(!param.hasThrowable())runtime.addStops(param.thisObject,current.mode);
                    }
                });
                HookDiagnostics.hookMethod(runtime.slider,scope(slider,"grip.slider",-1,0));
                HookDiagnostics.hookMethod(runtime.sanitize,new StrictHook(){
                    @Override protected void after(MethodHookParam param)throws Throwable{
                        Scope current=slider.get();if(!param.hasThrowable()&&current!=null&&initializing.get()==null&&runtime.matchingSlider(current.owner,param.args[0])){
                            @SuppressWarnings("unchecked") ArrayList<Float> values=(ArrayList<Float>)param.args[0];LensRouteHooks.completeProStops(values,current.mode);
                        }
                    }
                });
                HookDiagnostics.hookMethod(runtime.accept,new StrictHook(){
                    @Override protected void before(MethodHookParam param){runtime.accept(param.thisObject,param.args[0]);param.setResult(null);}
                });
                final ThreadLocal<Scope> keyZoom=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.zoomKey,scope(keyZoom,"grip.zoom",1,-1));
                HookDiagnostics.hookMethod(segmented,new StrictHook(){
                    @Override protected void before(MethodHookParam param){Scope current=keyZoom.get();if(current!=null&&!current.busy){current.busy=true;if((Integer)param.args[0]==175)param.setResult(false);}}
                });
                HookDiagnostics.hookMethod(migrate,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void before(MethodHookParam param){runtime.migration.migrate(param.thisObject,(Integer)param.args[0],(ArrayList<Integer>)param.args[1]);}
                });
                final ThreadLocal<Scope> wheel=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.wheel,scope(wheel,"grip.wheel",1,-1));
                HookDiagnostics.hookMethod(runtime.component,new StrictHook(){
                    @Override protected void after(MethodHookParam param){
                        Scope current=wheel.get();if(!param.hasThrowable()&&current!=null&&!current.busy&&runtime.apertureMenu(param.args[0])&&runtime.legendWheel(current.owner)){
                            current.busy=true;try{runtime.apertureWheel(current.owner,(Boolean)current.argument);}finally{current.busy=false;}
                        }
                    }
                });
                final ThreadLocal<Scope> street=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.streetItems,scope(street,"grip.street",-1,-1));
                ObservedHook support=new StrictHook(){@Override protected void before(MethodHookParam param){if(street.get()!=null)param.setResult(runtime.shine());}};
                HookDiagnostics.hookMethod(runtime.streetSupport,support);HookDiagnostics.hookMethod(runtime.streetSupportAlt,support);
                HookDiagnostics.hookMethod(runtime.legendItems,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam param){if(!param.hasThrowable())runtime.legendRow((ArrayList<Object>)param.getResult());}
                });
                final ThreadLocal<Scope> night=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.nightItems,new StrictHook(){
                    @Override protected void before(MethodHookParam param){Scope current=new Scope(param.thisObject,new ArrayList<Object>(),0,night.get());param.setObjectExtra("grip.night",current);night.set(current);}
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam param){
                        Scope current=(Scope)param.getObjectExtra("grip.night");night.set(current.previous);
                        if(!param.hasThrowable())((ArrayList<Object>)param.getResult()).addAll(0,(ArrayList<Object>)current.argument);
                    }
                });
                HookDiagnostics.hookMethod(runtime.apertureTouch,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam param){Scope current=night.get();if(current!=null&&!param.hasThrowable())((ArrayList<Object>)current.argument).addAll(runtime.nightRow());}
                });
                HookDiagnostics.hookMethod(runtime.streetInit,new StrictHook(){@Override protected void before(MethodHookParam param){runtime.streetInitialize(param.thisObject);}});
                HookDiagnostics.hookMethod(runtime.beautyInit,new StrictHook(){@Override protected void after(MethodHookParam param){if(!param.hasThrowable())runtime.beautyInitialize(param.thisObject);}});
                final ThreadLocal<Scope> recording=new ThreadLocal<>();
                HookDiagnostics.hookMethod(runtime.recordSetup,scope(recording,"grip.recording",-1,0));
                HookDiagnostics.hookMethod(runtime.request,new StrictHook(){
                    @Override protected void after(MethodHookParam param){Scope current=recording.get();if(!param.hasThrowable()&&current!=null&&current.owner==param.thisObject&&current.mode==(Integer)param.args[0])runtime.eis(param.thisObject,current.mode,(CaptureRequest.Builder)param.getResult());}
                });
            }
        };
    }
}
