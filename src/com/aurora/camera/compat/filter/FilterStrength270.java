package com.aurora.camera.compat.filter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.view.ViewTreeObserver;
import android.app.Activity;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.Options229;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import static de.robv.android.xposed.XposedHelpers.*;

/** Use the host's filter scale, tick adapter and intensity consumer in both Photo panel families. */
public final class FilterStrength270 {
    public static final String KEY="aurora_photo_filter_strength";
    private final Options229 options;
    private final Class<?> photo,bottom,repository,running,config,effects,listener,theme;
    private final Constructor<?> createSlide,createAdapter,createReceiver;
    private final Method supported,init,select,destroy,degreeChanged;
    public FilterStrength270(Host h)throws ReflectiveOperationException{
        options=new Options229(h);photo=h.type("com.android.camera.fragment.j0");supported=h.method("P9.i","Vs");
        bottom=h.type("com.android.camera.features.mode.capture.T");init=h.method(bottom,"initView",View.class);
        select=h.method("U9.M","Yt",h.type("U9.S"),boolean.class,int.class);destroy=h.method(bottom,"onDestroyView");
        repository=h.type("g2.a");running=h.type("v2.W");config=h.type("r2.u");effects=h.type("com.xiaomi.camera.effect.EffectController");
        listener=h.type("com.android.camera.ui.a$e");theme=h.type("f2.b");
        createSlide=h.type("com.android.camera.ui.CombineSlideView").getConstructor(Context.class,AttributeSet.class,int.class);
        createAdapter=h.type("L9.d").getConstructor(Context.class,String.class,h.type("Q4.C0"));
        createReceiver=photo.getConstructor();
        degreeChanged=h.method("P9.i","S8",int.class,String.class);
    }
    private final class Scale implements ViewTreeObserver.OnGlobalLayoutListener {
        final View view,cards;
        final FrameLayout content;
        final int[] cardsAt=new int[2],contentAt=new int[2];
        final Object panel,receiver,adapter;
        Object component; int mode,filter;
        Scale(Object panel,LinearLayout root)throws ReflectiveOperationException{
            this.panel=panel;
            Context context=root.getContext();
            cards=(View)getObjectField(panel,"O");
            Activity camera=(Activity)callMethod(panel,"requireActivity");
            content=camera.findViewById(android.R.id.content);
            receiver=createReceiver.newInstance();
            setAdditionalInstanceField(receiver,KEY,this);
            view=(View)createSlide.newInstance(context,null,0);
            adapter=createAdapter.newInstance(context,"",receiver);
            setObjectField(receiver,"r",view);
            setObjectField(receiver,"s",adapter);
            setObjectField(receiver,"R",new java.util.ArrayList<>());
            Object callbacks=Proxy.newProxyInstance(listener.getClassLoader(),new Class[]{listener},(proxy,method,args)->{
                switch(method.getName()){
                    case "Ab": if(component!=null)callMethod(panel,"ue",component,mode);return null;
                    case "s5":return false;
                    case "H8":case "sl":return null;
                    case "toString":return "CamStyle filter strength";
                    case "hashCode":return System.identityHashCode(proxy);
                    case "equals":return proxy==args[0];
                    default:throw new IllegalStateException(method.toString());
                }
            });
            callMethod(view,"setListener",callbacks);callMethod(view,"b",adapter,-2f,null);
            callMethod(view,"m",callStaticMethod(theme,"e"));
            view.setContentDescription("滤镜强度");
            int size=context.getResources().getIdentifier("e2i","dimen",context.getPackageName());
            view.setVisibility(View.GONE);
            content.addView(view,new FrameLayout.LayoutParams(-1,context.getResources().getDimensionPixelSize(size)));
            content.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        public void onGlobalLayout(){
            if(component==null||!cards.isAttachedToWindow()||!cards.isShown()){
                view.setVisibility(View.GONE);return;
            }
            cards.getLocationOnScreen(cardsAt);content.getLocationOnScreen(contentAt);
            view.setTranslationY(cardsAt[1]-contentAt[1]+cards.getHeight());
            view.setVisibility(View.VISIBLE);
        }
        void release(){
            content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            content.removeView(view);
            removeAdditionalInstanceField(receiver,KEY);
        }
        void bind(Object row){
            Object value=row==null?null:((Map<?,?>)getObjectField(row,"e")).get("pref_camera_shader_coloreffect_key");
            if(value==null||Integer.parseInt((String)value)==RecipeCatalog219.ORIGINAL){
                component=null;view.setVisibility(View.GONE);return;
            }
            filter=Integer.parseInt((String)value);mode=(Integer)getObjectField(panel,"mCurrentMode");
            boolean perCamera=(Boolean)callStaticMethod(config,"p",mode);
            Object provider=perCamera?callMethod(callStaticMethod(repository,"i"),"b",callMethod(callStaticMethod(repository,"g"),"B")):callStaticMethod(repository,"j");
            component=callMethod(provider,"x",perCamera?config:running);
            // Camera clears its live slide keys on startup. CamStyle retains the
            // edited value in the selected row, under the component's native key.
            String live=(String)callMethod(component,"m",mode,filter);
            String key=(String)callMethod(component,"getGeneralKey",mode);
            String stored=restoreDegree(row,key);
            if(stored!=null&&!stored.equals(live))update(stored);
            setIntField(receiver,"mCurrentMode",mode);
            setObjectField(receiver,"T",component);
            // mt builds the stock scale specification and restores this filter's value.
            callMethod(receiver,"mt",1,String.valueOf(filter));
            onGlobalLayout();
        }
        @SuppressWarnings("unchecked")
        String restoreDegree(Object row,String key){
            Map<String,String> values=(Map<String,String>)getObjectField(row,"e");
            String stored=values.get(key);
            Map<?,?> edits=(Map<?,?>)getObjectField(row,"f");
            if(stored!=null||edits==null||!edits.containsKey(key))return stored;
            // U9.S.O restores only registered CamStyle components into e, but its
            // original-value map f still records this edited intensity key.
            File directory=new File((String)getObjectField(row,"g"));
            String prefix="p_"+key+"_";
            File[] markers=directory.listFiles((dir,name)->name.startsWith(prefix));
            if(markers==null)throw new IllegalStateException("无法读取滤镜强度："+directory);
            if(markers.length==0)return null;
            Arrays.sort(markers,Comparator.comparingLong(File::lastModified).thenComparing(File::getName));
            File latest=markers[markers.length-1];
            stored=latest.getName().substring(prefix.length());
            values.put(key,stored);
            // Older builds could append a second value after cold start because e
            // lacked the old value. Stock R can now rename the correct single marker.
            for(int i=0;i<markers.length-1;i++){
                if(!markers[i].delete())throw new IllegalStateException("无法合并旧滤镜强度："+markers[i]);
            }
            return stored;
        }
        void update(String degree){
            if(component==null)return;
            callMethod(component,"setComponentValue",mode,degree);
            callMethod(callStaticMethod(effects,"u"),"e0",filter,Integer.parseInt(degree));
        }
    }
    public void install(){
        XposedBridge.hookMethod(supported,new StrictHook(){@Override protected void before(MethodHookParam p){
            if(photo.isInstance(p.thisObject)&&options.enabled(KEY))p.setResult(true);
        }});
        // L9.d's native listener requires a Photo fragment. Route only our private
        // receiver's S8 callback to the CamStyle selection that owns this scale.
        XposedBridge.hookMethod(degreeChanged,new StrictHook(){@Override protected void before(MethodHookParam p){
            Scale scale=(Scale)getAdditionalInstanceField(p.thisObject,KEY);
            if(scale!=null){scale.update((String)p.args[1]);p.setResult(null);}
        }});
        XposedBridge.hookMethod(init,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||!options.enabled(KEY))return;
            Scale scale=new Scale(p.thisObject,(LinearLayout)p.args[0]);
            setAdditionalInstanceField(p.thisObject,KEY,scale);
            Object ws=getObjectField(p.thisObject,"V");scale.bind(ws==null?null:callMethod(ws,"g"));
        }});
        XposedBridge.hookMethod(select,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(!p.hasThrowable()&&bottom.isInstance(p.thisObject)){
                Scale scale=(Scale)getAdditionalInstanceField(p.thisObject,KEY);
                if(scale!=null)scale.bind(p.args[0]);
            }
        }});
        XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void after(MethodHookParam p){
            Scale scale=(Scale)removeAdditionalInstanceField(p.thisObject,KEY);
            if(scale!=null)scale.release();
        }});
    }
}
