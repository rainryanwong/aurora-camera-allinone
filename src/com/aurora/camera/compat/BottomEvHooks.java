package com.aurora.camera.compat;

import com.aurora.camera.compat.ev.BottomEvPort;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** F08 bottom EV entry/click policy, scoped to the real original factory. */
public final class BottomEvHooks {
    private static final String PREVIOUS="aurora.bottomEv.factoryPrevious";
    private BottomEvHooks(){}
    private abstract static class Hook extends XC_MethodHook {
        void before(MethodHookParam p)throws Throwable{}void after(MethodHookParam p)throws Throwable{}
        @Override protected final void beforeHookedMethod(MethodHookParam p){try{before(p);}catch(Throwable t){p.setThrowable(t);}}
        @Override protected final void afterHookedMethod(MethodHookParam p){try{after(p);}catch(Throwable t){p.setThrowable(t);}}
    }
    public static HookPlan resolve(Host host)throws ReflectiveOperationException {
        final BottomEvPort port=new BottomEvPort(host);
        final ThreadLocal<Boolean> factoryScope=new ThreadLocal<>();
        return new HookPlan(){
            public String id(){return "bottom-ev";}
            public void install(){
                XposedBridge.hookMethod(port.apply,new Hook(){@Override void after(MethodHookParam p)throws Throwable{
                    if(!p.hasThrowable()&&port.modeUiLambda(p.thisObject,p.args[0]))p.setResult(port.ensure(p.getResult(),p.args[0]));
                }});
                XposedBridge.hookMethod(port.menu,new Hook(){@Override void after(MethodHookParam p)throws Throwable{
                    if(!p.hasThrowable())port.withoutOldTip(p.getResult(),p.thisObject);
                }});
                XposedBridge.hookMethod(port.click,new Hook(){@Override void before(MethodHookParam p)throws Throwable{
                    if(!port.clickAllowed())p.setResult(null);
                }});
                XposedBridge.hookMethod(port.factory,new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        boolean extend=!(Boolean)BottomEvPort.call(port.peY,null)&&port.available((Integer)p.args[1]);
                        p.setObjectExtra(PREVIOUS,factoryScope.get());p.setObjectExtra(PREVIOUS+".entered",true);factoryScope.set(extend);
                    }
                    @Override void after(MethodHookParam p){
                        if(!Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))return;
                        Boolean previous=(Boolean)p.getObjectExtra(PREVIOUS);if(previous==null)factoryScope.remove();else factoryScope.set(previous);
                    }
                });
                XposedBridge.hookMethod(port.peY,new Hook(){@Override void after(MethodHookParam p){
                    if(!p.hasThrowable()&&Boolean.TRUE.equals(factoryScope.get()))p.setResult(true);
                }});
            }
        };
    }
}
