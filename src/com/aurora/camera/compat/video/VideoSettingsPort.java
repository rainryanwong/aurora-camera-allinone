package com.aurora.camera.compat.video;

import android.content.res.Resources;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Optional;

/** Exact F17 front-audio menu and F23 video-setting data/factory contracts. */
public final class VideoSettingsPort {
    public final Method frontMenu,videoState,videoPreferences,category,trueColour,flagLog,tagView;
    public final Field cameraSettings;
    private final Method prefs,repo,manual,getOptional,facing,audioAllowed,getComponent;
    private final Field audioPresent,stateNeeded,stateMutex,profileSingleton,profile,preferenceGroup;
    private final Method P,I0,G0,E0,F0,y1,N,S,E,e4,stateLogMessage,stateLog;
    private final Constructor<?> stateConstructor;
    private final Method item,resources,summarySetting,checkSummary,checkResource,mutex;

    public VideoSettingsPort(Host host)throws ReflectiveOperationException {
        frontMenu=host.method("r4.b","l");videoState=host.method("com.android.camera.data.data.i","c0",int.class,boolean.class);
        videoPreferences=host.method("com.android.camera.fragment.settings.CameraCamcorderPreferenceFragment","addVideoPreferences");
        category=host.method("com.android.camera.fragment.settings.b","addCategory",String.class,int.class);
        trueColour=host.method("com.android.camera.fragment.settings.f","g");
        flagLog=host.method("E1.P4","a",String.class);tagView=host.method("p5.q","initView",host.type("android.view.View"));
        cameraSettings=host.field("com.android.camera.fragment.settings.b","mCameraSettings");
        preferenceGroup=host.field("com.android.camera.fragment.settings.b","mPreferenceGroup");
        prefs=host.method("g2.a","g");repo=host.method("g2.a","a");manual=host.method("g2.a","j");
        getOptional=host.method("Xh.b","u",Class.class);facing=host.method("u2.Y","B");
        audioAllowed=host.method("H1.a","h");audioPresent=host.field("v2.c","a");
        getComponent=host.method("r2.h0","getComponentValue",int.class);
        item=host.method("X9.w5","E",int.class);
        P=host.method("u2.Y","P");I0=host.method("com.android.camera.data.data.i","I0",int.class);
        G0=host.method("com.android.camera.data.data.i","G0");E0=host.method("com.android.camera.data.data.i","E0");
        F0=host.method("com.android.camera.data.data.i","F0");y1=host.method("com.android.camera.data.data.i","y1");
        N=host.method("com.android.camera.data.data.n","N");S=host.method("com.android.camera.data.data.F","S",int.class);
        E=host.method("com.android.camera.data.data.n","E",int.class);
        profileSingleton=host.field("Pe.b$b","a");profile=host.field("Pe.b","e");e4=host.method(profile.getType(),"e4");
        stateLogMessage=host.method("E1.k0","b",int.class,String.class,String.class,boolean.class);
        stateLog=host.method("com.android.camera.log.Log","i",String.class,String.class,Object[].class);
        stateConstructor=host.type("E1.P4").getDeclaredConstructor();stateConstructor.setAccessible(true);
        stateNeeded=host.field("E1.P4","a");stateMutex=host.field("E1.P4","b");
        resources=host.method("androidx.fragment.app.Fragment","getResources");summarySetting=host.method("com.android.camera.fragment.settings.f","f",String.class);
        Class<?> group=host.type("androidx.preference.PreferenceGroup"),state=host.type("E1.P4");
        checkSummary=host.method("com.android.camera.fragment.settings.b","addCheckBoxPreference",group,String.class,boolean.class,int.class,String.class);
        checkResource=host.method("com.android.camera.fragment.settings.b","addCheckBoxPreference",group,String.class,boolean.class,int.class,int.class);
        mutex=host.method("com.android.camera.fragment.settings.b","dealPreferenceMutexEnable",group,String.class,state);
        if(frontMenu.getReturnType()!=ArrayList.class||videoState.getReturnType()!=state||trueColour.getReturnType()!=state
                ||stateNeeded.getType()!=boolean.class||stateMutex.getType()!=boolean.class||audioPresent.getType()!=boolean.class
                ||cameraSettings.getType()!=host.type("com.android.camera.fragment.settings.f")
                ||category.getReturnType()!=host.type("androidx.preference.PreferenceCategory"))
            throw new NoSuchMethodException("Video setting original state/category/component contract");
    }

    @SuppressWarnings("unchecked") public void appendFrontAudio(Object value)throws Throwable {
        if((Integer)call(facing,call(prefs,null))!=1||!(Boolean)call(audioAllowed,null))return;
        Optional<?> component=(Optional<?>)call(getOptional,call(manual,null),audioPresent.getDeclaringClass());
        if(component.isPresent()&&audioPresent.getBoolean(component.get()))((ArrayList<Object>)value).add(call(item,null,0xb6));
    }

    public Object videoTag(int mode,boolean front)throws Throwable {
        Object state;
        try{state=stateConstructor.newInstance();}catch(InvocationTargetException e){throw e.getCause();}
        call(stateLog,null,"SettingUiState",call(stateLogMessage,null,mode,"FucName videoTag  mode: "," isFront:",front),new Object[0]);
        if((Boolean)call(P,call(prefs,null))||(Boolean)call(I0,null,mode)
                ||(mode!=180&&mode!=164&&mode!=162&&mode!=214&&mode!=227)){
            stateNeeded.setBoolean(state,false);return state;
        }
        // The accepted patch still performs this exact query; only its gate was removed.
        call(e4,profile.get(profileSingleton.get(null)));
        boolean disabled=(Boolean)call(G0,null)||(Boolean)call(E0,null)||(Boolean)call(F0,null)
                ||(Boolean)call(y1,null)||(Boolean)call(N,null);
        if(!disabled)disabled=(Boolean)call(S,null,mode);
        if(!disabled){
            Optional<?> component=(Optional<?>)call(getOptional,call(repo,null),getComponent.getDeclaringClass());
            String value=component.isPresent()?(String)call(getComponent,component.get(),mode):null;
            if(value==null)value=""; // The exact Optional.map(...).orElse("") contract.
            boolean high="3001".equals(value),high24="3001,24".equals(value);
            boolean finalGate=(Boolean)call(E,null,mode);
            disabled=high||high24||finalGate;
        }
        stateMutex.setBoolean(state,disabled);return state;
    }

    public boolean needed(Object state)throws IllegalAccessException{return stateNeeded.getBoolean(state);}
    public void insertTrueColour(Object fragment,Object group,Object state)throws Throwable {
        int format=(Integer)call(summarySetting,null,"pref_true_colour_video_mode_setting_key");
        String summary=((Resources)call(resources,fragment)).getString(0x7f141043,format&255);
        call(checkSummary,fragment,group,"pref_true_colour_video_mode_setting_key",false,0x7f141154,summary);
        call(mutex,fragment,preferenceGroup.get(fragment),"pref_true_colour_video_mode_setting_key",state);
    }
    public void insertVideoTag(Object fragment,Object group,Object state)throws Throwable {
        call(checkResource,fragment,group,"pref_camera_video_tag_key",true,0x7f140fae,0x7f140faf);
        call(mutex,fragment,preferenceGroup.get(fragment),"pref_camera_video_tag_key",state);
    }
    public static Object call(Method method,Object receiver,Object...args)throws Throwable {
        try{return method.invoke(receiver,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
}
