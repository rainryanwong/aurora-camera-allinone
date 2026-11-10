package com.aurora.camera.compat;

import com.aurora.camera.compat.video.VideoWatermarkUiPort;
import de.robv.android.xposed.XposedBridge;

/** Temporarily hide video watermark UI while preserving its preference and layout. */
public final class VideoWatermarkUiHooks {
    private static final String SCOPE="aurora.videoWatermark.editorScope";
    private VideoWatermarkUiHooks(){}

    public static HookPlan resolve(Host host)throws ReflectiveOperationException {
        final VideoWatermarkUiPort port=new VideoWatermarkUiPort(host);
        return new HookPlan(){
            public String id(){return "video-watermark-ui-hidden";}
            public void install(){
                XposedBridge.hookMethod(port.topItems,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable {
                        if(!p.hasThrowable())p.setResult(port.visibleItems(p.getResult(),(Integer)p.args[0]));
                    }
                });
                XposedBridge.hookMethod(port.menuItems,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable {
                        if(!p.hasThrowable())p.setResult(port.visibleItems(p.getResult(),port.fragmentMode(p.thisObject)));
                    }
                });
                XposedBridge.hookMethod(port.editorItems,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable {
                        p.setObjectExtra(SCOPE,port.enterEditor(p.thisObject));
                    }
                    @Override protected void after(MethodHookParam p)throws Throwable {
                        VideoWatermarkUiPort.EditorScope scope=(VideoWatermarkUiPort.EditorScope)p.getObjectExtra(SCOPE);
                        if(scope!=null)port.leaveEditor(scope,!p.hasThrowable());
                    }
                });
                XposedBridge.hookMethod(port.editorVisibility,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable {
                        // 216 is the host's existing blank-slot presentation. The adapter
                        // still contains 223, so qs()/Y() keep the user's saved layout.
                        if(port.hideEditorItem(p.thisObject,p.args[1]))p.args[1]="216";
                    }
                });
                XposedBridge.hookMethod(port.editorAuxiliary,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable {
                        if(port.hideEditorItem(p.thisObject,p.args[1]))p.args[1]="216";
                    }
                });
                XposedBridge.hookMethod(port.videoSetting,new StrictHook(){
                    @Override protected void before(MethodHookParam p){
                        // This method creates the video-watermark category specifically;
                        // it also runs when settings was opened from a photo mode.
                        p.setResult(null);
                    }
                });
            }
        };
    }
}
