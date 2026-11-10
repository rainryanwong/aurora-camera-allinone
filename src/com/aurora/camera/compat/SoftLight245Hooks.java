package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.aurora.camera.compat.softlight.SoftLightPanel245;
import com.aurora.camera.compat.softlight.SoftLightRuntime245;
import com.aurora.camera.compat.ui.NativePreferences240;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Phoenix 1.3.0 soft-light control, remapped to the SHA-pinned stock 1770 host. */
public final class SoftLight245Hooks {
    private static final String KEY="aurora_soft_light_enabled245",STORE="aurora_soft_light245",SCOPE="aurora.softlight245.capture";
    private SoftLight245Hooks(){}
    public static HookPlan resolve(final Host h)throws ReflectiveOperationException{
        final SoftLightRuntime245 runtime=new SoftLightRuntime245(h);
        final SoftLightPanel245 panel=new SoftLightPanel245(h,runtime);
        final NativePreferences240 preferences=new NativePreferences240(h);
        final Method global=h.method("com.xiaomi.camera.basic.Global","getContext");
        final Method settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        final Method[] updates={h.method("p5.q","initView",View.class),h.method("p5.q","provideAnimateElement",int.class,List.class,int.class),h.method("p5.q","notifyAfterFrameAvailable",int.class),h.method("p5.q","notifyThemeChanged",int.class,int.class),h.method("p5.q","updateView",View.class,Bundle.class),h.method("p5.q","onStart")};
        final Method destroy=h.method("p5.q","onDestroyView"),stop=h.method("p5.q","onStop");
        final Class<?> change=h.type("androidx.preference.Preference$c");h.method(change,"onPreferenceChange",h.type("androidx.preference.Preference"),Object.class);
        return new HookPlan(){
            final ThreadLocal<Boolean> capture=new ThreadLocal<>();
            public String id(){return "245.phoenix-soft-light-control";}
            Object call(Object owner,String name,Object...args){return XposedHelpers.callMethod(owner,name,args);}
            SharedPreferences prefs(){return((Context)SoftLightRuntime245.invoke(global,null)).getSharedPreferences(STORE,0);}
            Boolean authorVisible;
            boolean authorVisible(){
                if(authorVisible==null)authorVisible=AuthorOptions273.enabled((Context)SoftLightRuntime245.invoke(global,null),"aurora_author_soft_light");
                return authorVisible;
            }
            void enabled(){
                boolean visible=authorVisible();SharedPreferences saved=prefs();
                if(!visible&&saved.getBoolean(KEY,false)&&!saved.edit().putBoolean(KEY,false).commit())
                    throw new IllegalStateException("柔光组件设置保存失败");
                runtime.state.enabled(visible&&saved.getBoolean(KEY,false));
            }
            void settings(Object fragment){
                enabled();if(!authorVisible())return;Object screen=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
                Object group=preferences.category(fragment,screen,NativePreferences240.CLASSIC_GROUP,null,-1999);
                if(call(screen,"k0",KEY)!=null)return;
                Object row=preferences.toggle(fragment);call(row,"a0",KEY);call(row,"e0","柔光控制组件");call(row,"c0","在拍照、人像、街拍和专业模式显示柔光控制");
                XposedHelpers.setBooleanField(row,"t",false);XposedHelpers.setIntField(row,"g",5);call(row,"setChecked",runtime.state.enabled());
                XposedHelpers.setObjectField(row,"e",Proxy.newProxyInstance(h.loader,new Class<?>[]{change},(proxy,m,args)->{
                    if(m.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,m.getName(),args);
                    if(!m.getName().equals("onPreferenceChange"))throw new IllegalStateException(m.toString());
                    boolean selected=(Boolean)args[1];if(!prefs().edit().putBoolean(KEY,selected).commit())throw new IllegalStateException("柔光组件设置保存失败");
                    runtime.state.enabled(selected);if(!selected)panel.detach();
                    boolean submitted=runtime.refresh(runtime.controller());Log.i("AuroraSoft245","component enabled="+selected+" previewSubmitted="+submitted);return true;
                }));call(group,"j0",row);
            }
            public void install(){
                XposedBridge.hookMethod(settings,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())settings(p.thisObject);}});
                XposedBridge.hookMethod(runtime.soft,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;enabled();if(!runtime.active()||runtime.reading())return;Object next=runtime.effective(p.thisObject,(Integer)p.args[0],p.getResult());if(next!=p.getResult())p.setResult(next);panel.refresh();}});
                XposedBridge.hookMethod(runtime.filter,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;enabled();if(runtime.active()){runtime.sync(p.thisObject);runtime.standalonePreview(p.thisObject);panel.refresh();}runtime.pictureRefresh(p.thisObject);}});
                XposedBridge.hookMethod(runtime.picture,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()){enabled();runtime.picture(p.thisObject,p.getResult());}}});
                XposedBridge.hookMethod(runtime.attach,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable()||p.args[0]==null)return;enabled();if(runtime.active())runtime.refresh(p.thisObject);}});
                XposedBridge.hookMethod(runtime.graph,new StrictHook(){@Override protected void before(MethodHookParam p){runtime.graph(p.thisObject,p.args[0]);}});
                XposedBridge.hookMethod(runtime.tagType,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;enabled();runtime.prepareTag(p.thisObject);}});
                XposedBridge.hookMethod(runtime.tagParams,new StrictHook(25){
                    @Override protected void before(MethodHookParam p){runtime.reassertTag(p.thisObject);p.setObjectExtra(SCOPE,capture.get());p.setObjectExtra(SCOPE+".picture",runtime.enterTag(p.thisObject));p.setObjectExtra(SCOPE+".entered",true);capture.set(runtime.standaloneTag(p.thisObject));}
                    @Override protected void after(MethodHookParam p){if(!Boolean.TRUE.equals(p.getObjectExtra(SCOPE+".entered")))return;runtime.leaveTag((Integer)p.getObjectExtra(SCOPE+".picture"));Boolean previous=(Boolean)p.getObjectExtra(SCOPE);if(previous==null)capture.remove();else capture.set(previous);}
                });
                XposedBridge.hookMethod(runtime.lookup,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(capture.get())&&runtime.noFilter((Integer)p.args[0])){p.args[0]=runtime.carrier();p.args[1]=0;}}});
                XposedBridge.hookMethod(runtime.save,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(capture.get())&&runtime.noFilter((Integer)p.args[0]))p.args[0]=runtime.carrier();}});
                StrictHook update=new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;enabled();panel.update(p.thisObject);}};
                for(Method method:updates)XposedBridge.hookMethod(method,update);
                XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void before(MethodHookParam p){panel.destroy(p.thisObject);}});
                XposedBridge.hookMethod(stop,new StrictHook(){@Override protected void before(MethodHookParam p){panel.stop(p.thisObject);}});
            }
        };
    }
}
