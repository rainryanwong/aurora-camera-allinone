package com.aurora.camera.compat;

import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import com.aurora.camera.compat.video.VideoSettingsPort;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;

/** F17/F23: exact original setting factories at their existing insertion points. */
public final class VideoSettingsHooks {
    private static final String PREVIOUS="aurora.video.settingsPrevious";
    private VideoSettingsHooks(){}
    private static final class Scope {
        final Object fragment,settings;
        Object category;
        boolean inserting;
        Scope(Object fragment,Object settings){this.fragment=fragment;this.settings=settings;}
        Object category(){if(category==null)throw new IllegalStateException("Original video category was not created before its setting state");return category;}
    }
    private static final class TagScope {
        final View root;boolean repairing;
        TagScope(View root){this.root=root;}
    }
    private abstract static class Hook extends XC_MethodHook {
        void before(MethodHookParam p)throws Throwable{}void after(MethodHookParam p)throws Throwable{}
        @Override protected final void beforeHookedMethod(MethodHookParam p){try{before(p);}catch(Throwable t){p.setThrowable(t);}}
        @Override protected final void afterHookedMethod(MethodHookParam p){try{after(p);}catch(Throwable t){p.setThrowable(t);}}
    }
    public static HookPlan resolve(Host host)throws ReflectiveOperationException {
        final VideoSettingsPort port=new VideoSettingsPort(host);
        final ThreadLocal<Scope> scope=new ThreadLocal<>();
        final ThreadLocal<TagScope> tagScope=new ThreadLocal<>();
        final Method find=host.method("android.view.View","findViewById",int.class);
        return new HookPlan(){
            public String id(){return "front-audio-and-video-settings";}
            public void install(){
                XposedBridge.hookMethod(port.frontMenu,new Hook(){@Override void after(MethodHookParam p)throws Throwable{
                    if(!p.hasThrowable())port.appendFrontAudio(p.getResult());
                }});
                XposedBridge.hookMethod(port.videoState,new Hook(){@Override void before(MethodHookParam p)throws Throwable{
                    p.setResult(port.videoTag((Integer)p.args[0],(Boolean)p.args[1]));
                }});
                XposedBridge.hookMethod(port.videoPreferences,new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        Object settings=port.cameraSettings.get(p.thisObject);
                        p.setObjectExtra(PREVIOUS,scope.get());p.setObjectExtra(PREVIOUS+".entered",true);
                        scope.set(new Scope(p.thisObject,settings));
                    }
                    @Override void after(MethodHookParam p){
                        if(!Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))return;
                        Scope previous=(Scope)p.getObjectExtra(PREVIOUS);if(previous==null)scope.remove();else scope.set(previous);
                    }
                });
                XposedBridge.hookMethod(port.category,new Hook(){@Override void after(MethodHookParam p){
                    Scope current=scope.get();
                    if(!p.hasThrowable()&&current!=null&&current.fragment==p.thisObject&&"category_video_setting".equals(p.args[0]))current.category=p.getResult();
                }});
                XposedBridge.hookMethod(port.trueColour,new Hook(){@Override void after(MethodHookParam p)throws Throwable{
                    Scope current=scope.get();
                    if(p.hasThrowable()||current==null||current.inserting||current.settings!=p.thisObject||port.needed(p.getResult()))return;
                    current.inserting=true;
                    try{port.insertTrueColour(current.fragment,current.category(),p.getResult());}finally{current.inserting=false;}
                }});
                XposedBridge.hookMethod(port.flagLog,new Hook(){@Override void after(MethodHookParam p)throws Throwable{
                    Scope current=scope.get();
                    if(p.hasThrowable()||current==null||current.inserting||!"pref_camera_video_tag_key".equals(p.args[0])||port.needed(p.thisObject))return;
                    current.inserting=true;
                    try{port.insertVideoTag(current.fragment,current.category(),p.thisObject);}finally{current.inserting=false;}
                }});
                XposedBridge.hookMethod(port.tagView,new Hook(){
                    @Override void before(MethodHookParam p){p.setObjectExtra(PREVIOUS,tagScope.get());tagScope.set(new TagScope((View)p.args[0]));}
                    @Override void after(MethodHookParam p){TagScope previous=(TagScope)p.getObjectExtra(PREVIOUS);if(previous==null)tagScope.remove();else tagScope.set(previous);}
                });
                XposedBridge.hookMethod(find,new Hook(){@Override void before(MethodHookParam p){
                    TagScope current=tagScope.get();
                    if(current==null||current.repairing||current.root!=p.thisObject||(Integer)p.args[0]!=0x7f0b0c45)return;
                    current.repairing=true;try{repairVideoTag(current.root);}finally{current.repairing=false;}
                }});
            }
        };
    }

    private static void repairVideoTag(View root){
        if(root==null||root.findViewById(0x7f0b0c45)!=null)return;
        View inner=root.findViewById(0x7f0b0c44);if(inner==null)return;
        ViewParent parent=inner.getParent();
        if(parent instanceof FrameLayout&&((FrameLayout)parent).getId()==View.NO_ID){
            ((FrameLayout)parent).setId(0x7f0b0c45);
            Log.i("AuroraCompat","video tag: restored missing outer FrameLayout id");
        }
    }
}
