package com.aurora.camera.compat;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.aurora.camera.compat.style.StyleDebugUi;
import com.aurora.camera.compat.style.StyleRuntime;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.List;

/** F25: real style construction, request-only RAW isolation, and live AI debug UI. */
public final class StyleHooks {
    private static final String NATURAL = "原色·测试";
    private static final String PREVIOUS = "aurora.style.previousScope";
    private StyleHooks() {}

    private static final class ResourceScope {
        final Object cap;
        final boolean force;
        final boolean fallback;
        ResourceScope(Object cap,boolean force,boolean fallback){this.cap=cap;this.force=force;this.fallback=fallback;}
    }
    private static final class TitleScope {
        final Object fragment;
        String pendingTitle;
        TitleScope(Object fragment){this.fragment=fragment;}
    }
    private static final class DropdownScope {
        final Object component;
        final android.content.Context context;
        String pendingTitle;
        DropdownScope(Object component,android.content.Context context){this.component=component;this.context=context;}
    }
    private static final class AiViewScope {
        final Object owner;
        boolean consumed;
        AiViewScope(Object owner){this.owner=owner;}
    }
    /** Preserve callback failures as call failures, instead of Xposed logging and resuming. */
    private abstract static class Hook extends XC_MethodHook {
        void before(MethodHookParam p)throws Throwable {}
        void after(MethodHookParam p)throws Throwable {}
        @Override protected final void beforeHookedMethod(MethodHookParam p){
            try{before(p);}catch(Throwable failure){p.setThrowable(failure);}
        }
        @Override protected final void afterHookedMethod(MethodHookParam p){
            try{after(p);}catch(Throwable failure){p.setThrowable(failure);}
        }
    }

    public static HookPlan resolve(Host host)throws ReflectiveOperationException {
        final StyleRuntime r=new StyleRuntime(host);
        final StyleDebugUi debugUi=new StyleDebugUi(r);
        final ThreadLocal<ResourceScope> resources=new ThreadLocal<>();
        final ThreadLocal<TitleScope> title=new ThreadLocal<>();
        final ThreadLocal<DropdownScope> dropdown=new ThreadLocal<>();
        final ThreadLocal<AiViewScope> aiView=new ThreadLocal<>();
        final ThreadLocal<Object> aiLayout=new ThreadLocal<>();
        return new HookPlan(){
            @Override public String id(){return "natural-style-and-ai-debug";}
            @Override public void install(){
                XposedBridge.hookMethod(r.method("cap.resources"),new Hook(){
                    @Override void after(MethodHookParam p)throws Throwable{
                        ResourceScope scope=resources.get();
                        if(p.hasThrowable()||scope==null||scope.cap!=p.args[0])return;
                        if(scope.force||(scope.fallback&&!(Boolean)p.getResult()&&r.bool("style.supported",r.style())))p.setResult(true);
                    }
                });
                XposedBridge.hookMethod(r.method("style.init"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        Object component=p.thisObject,input=p.args[0];
                        int old=r.integer("mode",component),next=r.integer("input.mode",input);
                        // This executes before stock u overwrites mCurrentMode.
                        if(next==168&&old!=168&&!r.bool("style.empty",component)){
                            String value=(String)r.call("style.value",component,old);
                            if("0".equals(value)||"1".equals(value)||"2".equals(value))r.call("style.set",component,168,value);
                        }
                        p.setObjectExtra(PREVIOUS,resources.get());
                        p.setObjectExtra(PREVIOUS+".entered",true);
                        boolean force=r.integer("input.front",input)==0&&naturalMode(next);
                        resources.set(new ResourceScope(r.get("input.cap",input),force,false));
                    }
                    @Override void after(MethodHookParam p)throws Throwable{
                        try{
                            if(!p.hasThrowable()){
                                Object component=p.thisObject;int mode=r.integer("input.mode",p.args[0]);
                                List<?> items=(List<?>)r.get("items",component);
                                if(mode==171){
                                    // Old portrait value 2 may still be selected on upgrade.
                                    // Normalize through the value hook before removing its row.
                                    r.call("style.value",component,171);
                                    java.util.ArrayList<Object> visible=new java.util.ArrayList<>();
                                    for(Object item:items)if(!"2".equals(r.get("item.value",item)))visible.add(item);
                                    r.set("items",component,visible);
                                }else if(r.integer("input.front",p.args[0])==0){
                                    for(Object item:items)if("2".equals(r.get("item.value",item))){
                                        r.set("item.title",item,-1);r.set("item.text",item,NATURAL);
                                    }
                                }
                            }
                        }finally{if(Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))restore(resources,(ResourceScope)p.getObjectExtra(PREVIOUS));}
                    }
                });
                for(String key:new String[]{"style.o","style.p","style.r"}){
                    XposedBridge.hookMethod(r.method(key),new Hook(){
                        @Override void before(MethodHookParam p)throws Throwable{
                            p.setObjectExtra(PREVIOUS,resources.get());p.setObjectExtra(PREVIOUS+".entered",true);
                            resources.set(new ResourceScope(r.get("cap",p.thisObject),false,true));
                        }
                        @Override void after(MethodHookParam p){if(Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))restore(resources,(ResourceScope)p.getObjectExtra(PREVIOUS));}
                    });
                }
                // This original value-190 factory chooses animation j or drawable c.
                // Natural has no animation; its outer branch must agree with style.o.
                XposedBridge.hookMethod(r.method("editor.item"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        Object owner=p.args[0],component=p.args[2];
                        if(owner==null||component==null)return;
                        int mode=r.integer("mode",owner);
                        if(!naturalMode(mode)||naturalTitle(r,component,mode)==null)return;
                        Object cap=r.get("editor.cap",owner);if(cap==null)return;
                        p.setObjectExtra(PREVIOUS,resources.get());p.setObjectExtra(PREVIOUS+".entered",true);
                        resources.set(new ResourceScope(cap,true,false));
                    }
                    @Override void after(MethodHookParam p){if(Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))restore(resources,(ResourceScope)p.getObjectExtra(PREVIOUS));}
                });
                XposedBridge.hookMethod(r.method("style.value"),new Hook(){
                    @Override void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable()||!"2".equals(p.getResult()))return;
                        int mode=r.integer("mode",p.thisObject);
                        if(mode==171){
                            String value=(String)r.call("style.default",p.thisObject,171);
                            // Stock getKey(171) falls back to the ordinary Photo key
                            // when c=false. Never overwrite Photo's accepted style 2.
                            if(r.boolField("style.portrait",p.thisObject))r.call("style.set",p.thisObject,171,value);
                            p.setResult(value);
                        }else if(mode==167&&(r.bool("raw",null,mode)||r.bool("ultraRaw",null,mode,r.get("cap",p.thisObject))))p.setResult("0");
                    }
                });
                XposedBridge.hookMethod(r.method("preview"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        Object state=r.get("preview.state",p.thisObject);
                        if(state==null||r.integer("preview.front",state)!=0)return;
                        int mode=r.integer("preview.mode",state);
                        if((mode==163||mode==167||mode==225||mode==229||mode==168)&&r.bool("style.active",r.style(),mode))p.setResult(0);
                    }
                });
                XposedBridge.hookMethod(r.method("settings.add"),new Hook(){
                    @Override void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())addSetting(r,p.thisObject);}
                });
                XposedBridge.hookMethod(r.method("menu.enabled"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        boolean eligible=r.debug()||r.bool("screen.S",null)||r.bool("screen.U",null);
                        p.setResult(eligible&&!r.bool("device.special",null)&&(Integer)p.args[0]==168);
                    }
                });
                XposedBridge.hookMethod(r.method("debug.b"),new Hook(){@Override void before(MethodHookParam p)throws Throwable{p.setResult(debugUi.tokens());}});
                XposedBridge.hookMethod(r.method("debug.e"),new Hook(){@Override void before(MethodHookParam p)throws Throwable{p.setResult(debugUi.quickMenu(p.thisObject));}});
                XposedBridge.hookMethod(r.method("debug.f"),new Hook(){@Override void before(MethodHookParam p)throws Throwable{p.setResult(debugUi.topMenu(p.thisObject));}});
                XposedBridge.hookMethod(r.method("debug.h"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        android.util.SparseArray<?> map=(android.util.SparseArray<?>)r.get("menu.map",p.thisObject);
                        p.setObjectExtra(PREVIOUS+".protocol6",map.indexOfKey(6)>=0);
                    }
                    @Override void after(MethodHookParam p)throws Throwable{
                        if(!p.hasThrowable())debugUi.adjustProtocolMap(p.thisObject,(Boolean)p.getObjectExtra(PREVIOUS+".protocol6"));
                    }
                });
                XposedBridge.hookMethod(r.method("debug.l"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{if(p.thisObject.getClass()==r.type("z3.d"))p.setResult(debugUi.extraMenu());}
                });
                XposedBridge.hookMethod(r.method("ai.se"),new Hook(){@Override void before(MethodHookParam p)throws Throwable{layoutSe(r,p.thisObject,(Boolean)p.args[0]);p.setResult(null);}});
                XposedBridge.hookMethod(r.method("ai.st"),new Hook(){@Override void before(MethodHookParam p)throws Throwable{layoutSt(r,p.thisObject,(Boolean)p.args[0],(Boolean)p.args[1]);p.setResult(null);}});
                XposedBridge.hookMethod(r.method("ai.ks"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        Object owner=aiLayout.get();
                        if(owner!=null&&r.integer("fragment.mode",owner)==168&&r.debug())p.setResult(true);
                    }
                });
                XposedBridge.hookMethod(r.method("ai.view"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        p.setObjectExtra(PREVIOUS,aiView.get());
                        p.setObjectExtra(PREVIOUS+".layout",aiLayout.get());
                        // The original normal-layout commit is after Ns/Bt and the
                        // W/R early return. A nested excluded owner must mask this frame.
                        aiView.set(r.integer("fragment.mode",p.thisObject)==168
                                &&!r.bool("screen.W",null)&&!r.bool("screen.R",null)
                                ?new AiViewScope(p.thisObject):null);
                        aiLayout.set(p.thisObject);
                    }
                    @Override void after(MethodHookParam p){
                        restore(aiView,(AiViewScope)p.getObjectExtra(PREVIOUS));
                        restore(aiLayout,p.getObjectExtra(PREVIOUS+".layout"));
                    }
                });
                XposedBridge.hookMethod(r.method("ai.panel"),new Hook(){
                    @Override void before(MethodHookParam p){p.setObjectExtra(PREVIOUS,aiLayout.get());aiLayout.set(p.args[0]);}
                    @Override void after(MethodHookParam p){restore(aiLayout,p.getObjectExtra(PREVIOUS));}
                });
                XposedBridge.hookMethod(r.method("view.layout"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        AiViewScope scope=aiView.get();
                        if(scope==null||scope.consumed||r.get("ai.g",scope.owner)!=p.thisObject
                                ||!(p.args[0] instanceof FrameLayout.LayoutParams))return;
                        // Ns/Bt do not write g. Consume its first normal-layout
                        // commit only; the later Ts->st setter retains its own margin.
                        // Framework bridge class names are not part of this contract.
                        scope.consumed=true;
                        Object owner=scope.owner;
                        FrameLayout.LayoutParams layout=(FrameLayout.LayoutParams)p.args[0];
                        int z=(Integer)r.call("screen.z",null);boolean enabled=r.debug();
                        if(r.bool("ai.ks",null))layout.topMargin=enabled?(int)(z*2.7f):
                                ((Resources)r.call("fragment.resources",owner)).getDimensionPixelSize(0x7f070079)
                                        +((Rect)r.call("orientation.rect",null)).bottom;
                        else layout.bottomMargin=enabled?(int)(z*2.7f):z;
                    }
                });
                XposedBridge.hookMethod(r.method("style.view"),new Hook(){
                    @Override void before(MethodHookParam p){
                        p.setObjectExtra(PREVIOUS,title.get());p.setObjectExtra(PREVIOUS+".entered",true);
                        // Stock initView initializes b itself. It is null on first entry.
                        title.set(new TitleScope(p.thisObject));
                    }
                    @Override void after(MethodHookParam p){if(Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))restore(title,(TitleScope)p.getObjectExtra(PREVIOUS));}
                });
                XposedBridge.hookMethod(r.method("style.icon"),new Hook(){
                    @Override void after(MethodHookParam p)throws Throwable{
                        TitleScope scope=title.get();DropdownScope menu=dropdown.get();
                        if(scope!=null)scope.pendingTitle=null;
                        if(menu!=null)menu.pendingTitle=null;
                        if(p.hasThrowable()||!"2".equals(p.args[1]))return;
                        int mode=(Integer)p.args[0];
                        if(scope!=null&&mode!=171&&r.integer("fragment.mode",scope.fragment)==mode){
                            // The original panel has assigned b before this call.
                            scope.pendingTitle=naturalTitle(r,r.get("style.component",scope.fragment),mode);
                        }
                        if(menu!=null)menu.pendingTitle=naturalTitle(r,menu.component,mode);
                    }
                });
                XposedBridge.hookMethod(r.method("fragment.string"),new Hook(){
                    @Override void before(MethodHookParam p){
                        TitleScope scope=title.get();
                        if(scope!=null&&scope.fragment==p.thisObject&&(Integer)p.args[0]==-1
                                &&scope.pendingTitle!=null){
                            String itemTitle=scope.pendingTitle;scope.pendingTitle=null;
                            p.setResult(itemTitle);
                        }
                    }
                });
                XposedBridge.hookMethod(r.method("dropdown.bind"),new Hook(){
                    @Override void before(MethodHookParam p)throws Throwable{
                        if(r.integer("dropdown.kind",p.thisObject)!=4||!r.type("r2.m").isInstance(p.args[0]))return;
                        Object owner=r.get("dropdown.owner",p.thisObject);
                        if(!r.type("com.android.camera.fragment.top.secondmenu.CvTypeSecondMenu").isInstance(owner))return;
                        p.setObjectExtra(PREVIOUS,dropdown.get());p.setObjectExtra(PREVIOUS+".entered",true);
                        dropdown.set(new DropdownScope(p.args[0],((View)owner).getContext()));
                    }
                    @Override void after(MethodHookParam p){if(Boolean.TRUE.equals(p.getObjectExtra(PREVIOUS+".entered")))restore(dropdown,(DropdownScope)p.getObjectExtra(PREVIOUS));}
                });
                XposedBridge.hookMethod(r.method("context.string"),new Hook(){
                    @Override void before(MethodHookParam p){
                        DropdownScope scope=dropdown.get();
                        if(scope!=null&&scope.context==p.thisObject&&(Integer)p.args[0]==-1&&scope.pendingTitle!=null){
                            String text=scope.pendingTitle;scope.pendingTitle=null;p.setResult(text);
                        }
                    }
                });
            }
        };
    }

    private static boolean naturalMode(int mode){return mode==163||mode==167||mode==168||mode==173||mode==225||mode==229;}
    private static String naturalTitle(StyleRuntime r,Object component,int mode)throws Throwable{
        if(!naturalMode(mode)||!r.type("r2.m").isInstance(component)||r.integer("mode",component)!=mode)return null;
        Object natural=null;
        for(Object item:(List<?>)r.get("items",component))if("2".equals(r.get("item.value",item))){if(natural!=null)return null;natural=item;}
        String expected=NATURAL;
        return natural!=null&&r.integer("item.title",natural)==-1&&expected.equals(r.get("item.text",natural))?(String)r.get("item.text",natural):null;
    }
    private static <T> void restore(ThreadLocal<T> local,T previous){if(previous==null)local.remove();else local.set(previous);}
    private static FrameLayout.LayoutParams params(View view){
        ViewGroup.LayoutParams p=view==null?null:view.getLayoutParams();
        return p instanceof FrameLayout.LayoutParams?(FrameLayout.LayoutParams)p:null;
    }
    private static int baseline(StyleRuntime r)throws Throwable{int z=(Integer)r.call("screen.z",null);return r.debug()?(int)(z*2.7f):z;}
    private static boolean topLayout(StyleRuntime r,Object owner)throws Throwable{return r.integer("fragment.mode",owner)==168&&r.debug()||r.bool("ai.ks",null);}
    private static void layoutSe(StyleRuntime r,Object owner,boolean expanded)throws Throwable{
        if(r.bool("screen.W",null)||r.bool("screen.R",null)||topLayout(r,owner))return;
        View modal=(View)r.get("ai.H",owner);if(modal!=null&&modal.getVisibility()==0)return;
        LinearLayout row=(LinearLayout)r.get("ai.g",owner);FrameLayout.LayoutParams p=params(row);if(p==null)return;
        int extra=expanded?((Resources)r.call("fragment.resources",owner)).getDimensionPixelSize(0x7f070075):0;
        int target=baseline(r)+extra;if(p.bottomMargin!=target){p.bottomMargin=target;row.setLayoutParams(p);}
    }
    private static void layoutSt(StyleRuntime r,Object owner,boolean hidden,boolean updateTop)throws Throwable{
        if(r.bool("screen.W",null)){if(updateTop)r.call("ai.rt",null,false);return;}
        if(updateTop)r.call("ai.rt",null,hidden);
        LinearLayout row=(LinearLayout)r.get("ai.g",owner);
        if(r.bool("screen.R",null)){if(row!=null)row.setVisibility(hidden?8:0);return;}
        FrameLayout.LayoutParams p=params(row);if(p==null)return;
        if(!topLayout(r,owner))p.bottomMargin=hidden?
                ((Resources)r.call("fragment.resources",owner)).getDimensionPixelSize(0x7f070073):baseline(r);
        row.setLayoutParams(p);
    }
    private static void addSetting(StyleRuntime r,Object fragment)throws Throwable{
        Object group=r.get("settings.group",fragment);if(group==null)return;
        String key="pref_aurora_ai_debug_controls";if(r.call("pref.find",group,key)!=null)return;
        Object category=r.call("pref.find",group,"category_advance_setting");
        if(r.type("androidx.preference.PreferenceGroup").isInstance(category))group=category;
        Object preference=r.construct("preference",r.call("fragment.activity",fragment));
        r.set("pref.order",preference,-200);
        r.call("pref.key",preference,key);r.call("pref.title",preference,"AI帮拍调试界面");
        r.call("pref.summary",preference,"显示顶部菜单和手动参数栏，返回相机后生效");
        boolean enabled=r.debug();r.set("pref.t",preference,false);r.set("pref.K",preference,enabled);
        r.call("pref.checked",preference,enabled);r.set("pref.e",preference,fragment);r.call("pref.add",group,preference);
        Object keep=r.construct("preference",r.call("fragment.activity",fragment));
        r.set("pref.order",keep,-199);r.call("pref.key",keep,"pref_aurora_ai_reset_keep_zoom");
        r.call("pref.title",keep,"AI帮拍重置操作优化");
        r.call("pref.summary",keep,"重置按钮只重新识别，保留当前焦段");
        boolean keepEnabled=r.bool("prefs.bool",r.call("repo.g",null),"pref_aurora_ai_reset_keep_zoom",false);
        r.set("pref.t",keep,false);r.set("pref.K",keep,keepEnabled);r.call("pref.checked",keep,keepEnabled);
        r.set("pref.e",keep,fragment);r.call("pref.add",group,keep);
    }
}
