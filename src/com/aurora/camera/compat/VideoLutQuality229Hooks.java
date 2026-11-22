package com.aurora.camera.compat;

import com.aurora.camera.diagnostics.HookDiagnostics;

import java.lang.reflect.Method;
import java.util.ArrayList;

/** Execute native quality rules with only their LUT predicate scoped off. */
public final class VideoLutQuality229Hooks {
    private VideoLutQuality229Hooks(){}
    public static boolean supportsMode(int mode){return mode==162||mode==164||mode==180;}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        ThreadLocal<Boolean> scope=new ThreadLocal<>();
        Method limit=h.method("r2.h0","C",h.type("r2.r1$a"),ArrayList.class,int.class,h.type("l9.g"),int.class);
        Method preferred=h.method("r2.h0","y",int.class,String.class,boolean.class);
        Method filter=h.method("com.android.camera.data.data.F","L",int.class);
        return new HookPlan(){public String id(){return "229.video-lut-quality";}
            private StrictHook enter(int modeAt){return new StrictHook(){
                @Override protected void before(MethodHookParam p){p.setObjectExtra("229.quality.scope",scope.get());if(supportsMode((Integer)p.args[modeAt]))scope.set(true);}
                @Override protected void after(MethodHookParam p){Boolean old=(Boolean)p.getObjectExtra("229.quality.scope");if(old==null)scope.remove();else scope.set(old);}
            };}
            public void install(){
                HookDiagnostics.hookMethod(limit,enter(4));HookDiagnostics.hookMethod(preferred,enter(0));
                HookDiagnostics.hookMethod(filter,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(scope.get())&&supportsMode((Integer)p.args[0]))p.setResult(false);}});
            }
        };
    }
}
