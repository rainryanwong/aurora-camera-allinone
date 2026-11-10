package com.aurora.camera.compat.ui;

import android.content.Context;
import android.util.Log;
import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** The same activity-themed preference factories used by stock 1770 settings. */
public final class NativePreferences240 {
    public static final String FILTER_GROUP="category_aurora_filters240";
    public static final String CLASSIC_GROUP="category_aurora_classic_controls239";
    private final Constructor<?> toggle,text;
    private final Method category;
    private final Class<?> groupType,advanceType;
    private final int multiChoiceLayout,multiChoiceWidget;

    public NativePreferences240(Host host)throws ReflectiveOperationException{
        // The native factory calls this Activity-specific constructor, not the
        // generic SwitchPreference whose widget is an Android material Switch.
        toggle=host.type("com.android.camera.preferences.AccessibleCheckBoxPreference")
            .getDeclaredConstructor(host.type("androidx.fragment.app.m"));
        toggle.setAccessible(true);
        text=host.type("miuix.preference.TextPreference")
            .getDeclaredConstructor(Context.class,host.type("android.util.AttributeSet"));
        text.setAccessible(true);
        category=host.method("com.android.camera.fragment.settings.b","addCategory",String.class,int.class);
        groupType=host.type("androidx.preference.PreferenceGroup");
        advanceType=host.type("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment");
        host.method("androidx.preference.PreferenceGroup","n0",host.type("androidx.preference.Preference"));
        host.field("androidx.preference.PreferenceGroup","g0");
        host.field("androidx.preference.Preference","g");
        multiChoiceLayout=host.field("miuix.preference.C","miuix_preference_flexible_multi_choice").getInt(null);
        multiChoiceWidget=host.field("miuix.preference.C","miuix_preference_widget_multichoice").getInt(null);
    }
    private static Object call(Object owner,String method,Object...args){return XposedHelpers.callMethod(owner,method,args);}
    public Context context(Object fragment){return (Context)call(fragment,"requireActivity");}
    public Object toggle(Object fragment){
        try{return toggle.newInstance(context(fragment));}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native camera toggle creation failed",failure);}
    }
    public Object text(Object fragment){
        try{return text.newInstance(context(fragment),null);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native camera text preference creation failed",failure);}
    }
    public Object selection(Object fragment){
        Object result=toggle(fragment);
        XposedHelpers.setIntField(result,"V",multiChoiceLayout);
        XposedHelpers.setIntField(result,"W",multiChoiceWidget);
        return result;
    }
    public Object category(Object fragment,Object screen,String key,String title,int order){
        Object result=call(screen,"k0",key);if(result!=null)return result;
        try{result=category.invoke(fragment,key,-1);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Native camera category creation failed",failure);}
        if(title!=null)call(result,"e0",title);
        XposedHelpers.setIntField(result,"g",order);call(screen,"j0",result);return result;
    }
    private Object parent(Object group,Object wanted){
        @SuppressWarnings("unchecked") ArrayList<Object> children=new ArrayList<>((ArrayList<Object>)XposedHelpers.getObjectField(group,"g0"));
        for(Object child:children){
            if(child==wanted)return group;
            if(groupType.isInstance(child)){Object found=parent(child,wanted);if(found!=null)return found;}
        }
        return null;
    }
    private void order(Object root,Object item,int order){
        if((Integer)XposedHelpers.getObjectField(item,"g")==order)return;
        Object parent=parent(root,item);
        if(parent!=null)call(parent,"n0",item);
        XposedHelpers.setIntField(item,"g",order);
        if(parent!=null)call(parent,"j0",item);
    }
    private void move(Object root,Object group,String key,int order){
        Object item=call(root,"k0",key);
        if(item==null){Log.w("AuroraPrefs240","Laboratory row unavailable: "+key);return;}
        Object previous=parent(root,item);
        if(previous==group&&(Integer)XposedHelpers.getObjectField(item,"g")==order)return;
        if(previous==null)throw new IllegalStateException("Laboratory row has no parent: "+key);
        call(previous,"n0",item);XposedHelpers.setIntField(item,"g",order);call(group,"j0",item);
    }
    public void regroupLaboratory(Object fragment){
        if(!advanceType.isInstance(fragment))return;
        Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
        Object filters=category(fragment,root,FILTER_GROUP,null,-2000);
        Object classic=category(fragment,root,CLASSIC_GROUP,null,-1999);
        order(root,filters,-2000);order(root,classic,-1999);
        move(root,filters,"pref_aurora_filter_manager",0);
        move(root,filters,"aurora_lut_import_effects229",1);
        move(root,classic,"aurora_auth_entry239",0);
        move(root,classic,"aurora_classic_style239",1);
        move(root,classic,"pref_aurora_street_ui217",2);
        move(root,classic,"aurora_xiaomi_asd239",3);
    }
    public void regroupLaboratoryTools(Object fragment){
        Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
        Object ai=category(fragment,root,"category_aurora_ai", "AI选项优化",-1998);
        Object backup=category(fragment,root,"category_aurora_backup",null,-1997);
        move(root,ai,"pref_aurora_ai_debug_controls",0);
        move(root,ai,"pref_aurora_ai_reset_keep_zoom",1);
        move(root,ai,com.aurora.camera.compat.SuperTelephotoHooks.THREE_STATES,2);
        move(root,ai,com.aurora.camera.compat.SuperTelephotoHooks.ZOOM10,3);
        move(root,ai,com.aurora.camera.compat.SuperTelephotoHooks.EXTENDED,4);
        move(root,backup,"aurora_settings_backup253",0);
        Object filters=call(root,"k0",FILTER_GROUP);
        move(root,filters,"aurora_custom_filter_manager253",2);
        move(root,filters,"aurora_photo_filter_strength",3);
    }
}
