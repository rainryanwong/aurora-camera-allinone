package com.aurora.camera.compat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.View;
import com.aurora.camera.compat.street.LegendStreet217;
import com.aurora.camera.compat.street.StreetSelection217;
import com.aurora.camera.compat.ui.MiuixDialogs;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;

/** Phoenix three-way street UI selection, mapped to exact stock-1770 consumers. */
public final class Street217Hooks {
    private static final String KEY="pref_aurora_street_ui217";
    private static final String[] LABELS={"原生","经典街拍","传奇街拍"};
    private Street217Hooks(){}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException {
        final Method global=h.method("com.xiaomi.camera.basic.Global","getContext");
        final Method settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","addCurrentPreferences");
        final Method activity=h.method("androidx.fragment.app.Fragment","requireActivity");
        final Field group=h.field("com.android.camera.fragment.settings.b","mPreferenceGroup");
        final Method find=h.method("androidx.preference.PreferenceGroup","k0",CharSequence.class),add=h.method("androidx.preference.PreferenceGroup","j0",h.type("androidx.preference.Preference"));
        final Method title=h.method("androidx.preference.Preference","e0",String.class),summary=h.method("androidx.preference.Preference","c0",CharSequence.class),key=h.method("androidx.preference.Preference","a0",String.class);
        final Field click=h.field("androidx.preference.Preference","f"),persistent=h.field("androidx.preference.Preference","t"),order=h.field("androidx.preference.Preference","g");
        final Class<?> clickType=h.type("androidx.preference.Preference$d");
        final Constructor<?> preference=h.type("miuix.preference.TextPreference").getDeclaredConstructor(Context.class,h.type("android.util.AttributeSet"));preference.setAccessible(true);
        final MiuixDialogs dialogs=new MiuixDialogs(h);
        final com.aurora.camera.compat.street.StreetMode218 modeRouting=new com.aurora.camera.compat.street.StreetMode218(h);
        final com.aurora.camera.compat.street.StreetControls220 controls=new com.aurora.camera.compat.street.StreetControls220(h);
        final com.aurora.camera.compat.street.StreetStyle221 streetStyle=new com.aurora.camera.compat.street.StreetStyle221(h);
        final Method layout=h.method("com.android.camera.features.mode.equipstreet.a$a","e",Activity.class);
        final Class<?> rectProtocol=h.type("M6.a");
        final Field width=h.field("K2.g","g"),height=h.field("K2.g","f");final Method supportedRect=h.method("K2.b","U");
        final Method init=h.method("q4.u","initView",View.class),animate=h.method("q4.h","provideAnimateElement",int.class,List.class,int.class);
        final List<Method> updates=Arrays.asList(h.method("q4.u","mo"),h.method("q4.u","Ds"),h.method("q4.u","i7"),h.method("q4.u","rq"),h.method("q4.h","notifyDataChanged",int.class,int.class),h.method("q4.u","onClick",View.class));
        // Resolve the Legend adapter's complete host dependency group before installing hooks.
        h.method("g2.a","a");h.method("Xh.b","x",Class.class);h.method("com.android.camera.data.data.c","getComponentValue",int.class);h.method("r2.b0","isSwitchOn",int.class);h.type("r2.F0");h.method("r2.m","o");h.field("q4.h","t");h.field("q4.h","r");h.field("com.android.camera.ui.EquipStreetBgView","n");
        for(String field:new String[]{"Q","R","S","T","U"})h.field("q4.u",field);
        final Map<Object,WeakReference<View>> roots=new WeakHashMap<>();final Map<Object,WeakReference<LegendStreet217>> legends=new WeakHashMap<>();
        return new HookPlan(){
            private SharedPreferences prefs()throws ReflectiveOperationException {Context c=(Context)global.invoke(null);return c==null?null:c.getSharedPreferences("aurora_street_ui217",0);}
            private int selection()throws ReflectiveOperationException {SharedPreferences p=prefs();return p==null?0:p.getInt(KEY,0);}
            private void refresh(Object fragment,boolean entering)throws Exception {
                streetStyle.update(fragment);
                WeakReference<LegendStreet217> ref=legends.get(fragment);LegendStreet217 legend=ref==null?null:ref.get();
                if(selection()!=2){if(legend!=null){legend.restore();legends.remove(fragment);}return;}
                if(legend==null&&entering){WeakReference<View> r=roots.get(fragment);View root=r==null?null:r.get();if(root!=null){legend=new LegendStreet217(h,fragment,root);legends.put(fragment,new WeakReference<>(legend));}}
                if(legend!=null)legend.update();
            }
            public String id(){return "217.classic-and-legend-street-ui";}
            public void install(){
                XposedBridge.hookMethod(settings,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;Object screen=group.get(p.thisObject);if(find.invoke(screen,KEY)!=null)return;Object category=find.invoke(screen,"category_advance_setting");
                    Activity owner=(Activity)activity.invoke(p.thisObject);Object row=preference.newInstance(owner,null);persistent.setBoolean(row,false);order.setInt(row,-250);key.invoke(row,KEY);title.invoke(row,"街拍模式UI");summary.invoke(row,LABELS[selection()]);
                    click.set(row,Proxy.newProxyInstance(h.loader,new Class<?>[]{clickType},(proxy,m,args)->{
                        if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraStreetUi217";}
                        dialogs.builder(owner).setTitle("街拍模式UI").setSingleChoiceItems(LABELS,selection(),(dialog,selected)->{
                            try{SharedPreferences preferences=prefs();int previous=preferences.getInt(KEY,0);boolean existed=preferences.contains(KEY);if(!preferences.edit().putInt(KEY,selected).commit()){SharedPreferences.Editor restore=preferences.edit();if(existed)restore.putInt(KEY,previous);else restore.remove(KEY);if(!restore.commit())throw new IllegalStateException("Street selection rollback failed");throw new IllegalStateException("街拍样式保存失败");}summary.invoke(row,LABELS[selected]);dialog.dismiss();}
                            catch(ReflectiveOperationException e){throw new IllegalStateException("Street setting",e);}
                        }).setNegativeButton("取消",null).show();return true;
                    }));add.invoke(category,row);
                }});
                modeRouting.install();
                controls.install();
                streetStyle.install();
                try{com.aurora.camera.compat.street.LegendPreview219.install(h);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                XposedBridge.hookMethod(layout,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(selection()!=2)return;Activity owner=(Activity)p.args[0];int inset=Math.round(owner.getResources().getDisplayMetrics().density*24);p.setResult(Proxy.newProxyInstance(h.loader,new Class<?>[]{rectProtocol},(proxy,m,args)->{
                    if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraLegendRect217";}
                    int[] rect=StreetSelection217.legendRect((Integer)args[0],width.getInt(null),height.getInt(null),inset,(Boolean)supportedRect.invoke(null));return rect==null?args[1]:new Rect(rect[0],rect[1],rect[2],rect[3]);
                }));}});
                XposedBridge.hookMethod(init,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{WeakReference<LegendStreet217> ref=legends.remove(p.thisObject);LegendStreet217 previous=ref==null?null:ref.get();if(previous!=null)previous.restore();roots.remove(p.thisObject);}
                    @Override protected void after(MethodHookParam p)throws Throwable{if(p.hasThrowable())return;roots.put(p.thisObject,new WeakReference<>((View)p.args[0]));refresh(p.thisObject,true);}
                });
                XposedBridge.hookMethod(animate,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())refresh(p.thisObject,(Integer)p.args[0]==229);}});
                for(Method method:updates)XposedBridge.hookMethod(method,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())refresh(p.thisObject,false);}});
            }
        };
    }
}
