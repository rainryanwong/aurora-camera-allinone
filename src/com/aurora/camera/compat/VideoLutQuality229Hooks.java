package com.aurora.camera.compat;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Execute native quality rules with only their LUT predicate scoped off. */
public final class VideoLutQuality229Hooks {
    private VideoLutQuality229Hooks(){}
    public static boolean supportsMode(int mode){return mode==162||mode==164||mode==180;}
    private static final class Input{final WeakReference<Object> owner;final Object[] args;Input(Object owner,Object[] args){this.owner=new WeakReference<>(owner);this.args=args.clone();}}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        Options229 options=new Options229(h);ThreadLocal<Boolean> scope=new ThreadLocal<>();Input[] latest={null};
        Method setting=h.method("com.android.camera.fragment.settings.CameraCamcorderPreferenceFragment","registerPreferenceListener");
        Method init=h.method("r2.h0","O",int.class,int.class,int.class,h.type("l9.g"));
        Method limit=h.method("r2.h0","C",h.type("r2.r1$a"),ArrayList.class,int.class,h.type("l9.g"),int.class);
        Method preferred=h.method("r2.h0","y",int.class,String.class,boolean.class);
        Method filter=h.method("com.android.camera.data.data.F","L",int.class);
        return new HookPlan(){public String id(){return "229.video-lut-quality";}
            private StrictHook enter(int modeAt){return new StrictHook(){
                @Override protected void before(MethodHookParam p){p.setObjectExtra("229.quality.scope",scope.get());if(options.enabled(Options229.VIDEO)&&supportsMode((Integer)p.args[modeAt]))scope.set(true);}
                @Override protected void after(MethodHookParam p){Boolean old=(Boolean)p.getObjectExtra("229.quality.scope");if(old==null)scope.remove();else scope.set(old);}
            };}
            public void install(){
                XposedBridge.hookMethod(init,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())latest[0]=supportsMode((Integer)p.args[0])?new Input(p.thisObject,p.args):null;}});
                XposedBridge.hookMethod(setting,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;
                    options.addSwitch(p.thisObject,Options229.VIDEO,"解锁滤镜视频规格限制","将极大地增加发热，谨慎启用。保留相机和编码器的其他规格限制。","pref_video_encoder_key",value->{
                        if(!options.prefs().edit().putBoolean(Options229.VIDEO,value).commit())return false;
                        Input input=latest[0];Object owner=input==null?null:input.owner.get();
                        if(owner!=null){init.invoke(owner,input.args);if(!value){int mode=(Integer)input.args[0];String permitted=(String)XposedHelpers.callMethod(owner,"getComponentValue",mode);XposedHelpers.callMethod(owner,"setComponentValue",mode,permitted);}}
                        return true;
                    });
                }});
                XposedBridge.hookMethod(limit,enter(4));XposedBridge.hookMethod(preferred,enter(0));
                XposedBridge.hookMethod(filter,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(scope.get())&&supportsMode((Integer)p.args[0]))p.setResult(false);}});
            }
        };
    }
}
