package com.aurora.camera.compat.night;

import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/** Night uses the same native CamStyle workspace/palette entry as rear photo. */
public final class NightCamStyle213 {
    private final Method support,eligible,softLight,initialize,vibrance,global,front,flip;
    private final Method legacyButton,camStyleButton,paletteReady,getList,loadWorkspace;
    private final Method external,externalKey,excludedInitialization;
    private final Method clearAllPalette,remove;
    private final Method[] keys,general;
    private final Field mode,inputMode,inputFront,inputCap,customVibrance,workspaceMode;
    private final Class<?> palette;
    private final AtomicInteger workspaceLogs=new AtomicInteger();
    private final AtomicInteger routeLogs=new AtomicInteger();
    public NightCamStyle213(Host h)throws ReflectiveOperationException {
        palette=h.type("v2.j0");
        support=h.method(palette,"isSupportMode",int.class);
        eligible=h.method(palette,"r",int.class);softLight=h.method(palette,"s",int.class);
        initialize=h.method(palette,"R",Object.class);
        customVibrance=h.field(palette,"b");
        mode=h.field("com.android.camera.data.data.c","mCurrentMode");
        inputMode=h.field("com.android.camera.data.data.C","a");inputFront=h.field("com.android.camera.data.data.C","b");inputCap=h.field("com.android.camera.data.data.C","c");
        vibrance=h.method("l9.h","A2",h.type("l9.g"));
        global=h.method("g2.a","g");front=h.method("u2.Y","N");flip=h.method("K2.b","a0");
        paletteReady=h.method("com.android.camera.data.data.F","R0");
        legacyButton=h.method("Z4.m","a");camStyleButton=h.method("Z4.m","g",boolean.class);
        external=h.method("com.android.camera.data.data.i","x0");
        excludedInitialization=h.method("com.android.camera.data.data.x","Z");
        externalKey=h.method("com.android.camera.data.data.c","getKey4ExternalScreen",String.class);
        clearAllPalette=h.method("r2.Y0","q",h.type("bi.a$a"));remove=h.method("Xh.a","s",String.class);
        String[] owners={"r2.Z0","r2.f1","r2.t0","r2.r0","r2.d1","r2.h1","r2.j1","r2.R0","r2.X0"};
        keys=new Method[owners.length];general=new Method[owners.length];
        for(int i=0;i<owners.length;i++){keys[i]=h.method(owners[i],"getKey",int.class);general[i]=h.method(owners[i],"getGeneralKey",int.class);}
        loadWorkspace=h.method("com.android.camera.features.mode.capture.d","G",int.class);
        workspaceMode=h.field("U9.a","a");getList=h.method("com.xiaomi.microfilm.vlog.vv.v","getList");
    }
    public void install(){
        // Only these pure eligibility functions receive photo's mode. The actual
        // initialization input, component identity and request mode remain 173.
        StrictHook qualify=new StrictHook(){
            @Override protected void before(MethodHookParam p){if((Integer)p.args[0]==173)p.args[0]=163;}
        };
        XposedBridge.hookMethod(support,qualify);XposedBridge.hookMethod(eligible,qualify);XposedBridge.hookMethod(softLight,qualify);
        XposedBridge.hookMethod(initialize,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable {
                if(p.hasThrowable()||(Boolean)excludedInitialization.invoke(null)||mode.getInt(p.thisObject)!=173||inputMode.getInt(p.args[0])!=173)return;
                // Same physical rear/capability requirement as photo's R(), whose
                // inline mode whitelist otherwise omits Night's vibrance flag.
                customVibrance.setBoolean(p.thisObject,inputFront.getInt(p.args[0])==0&&(Boolean)vibrance.invoke(null,inputCap.get(p.args[0])));
            }
        });
        for(int i=0;i<keys.length;i++){
            final Method prefix=general[i];
            XposedBridge.hookMethod(keys[i],new StrictHook(){
                @Override protected void before(MethodHookParam p)throws Throwable{
                    if((Integer)p.args[0]!=173)return;
                    p.setResult(nightKey(prefix,p.thisObject));
                }
            });
        }
        XposedBridge.hookMethod(clearAllPalette,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable())return;
                // Native q is the all-mode cleanup and keeps its original mode set.
                // Add only this component's new Night key to the same editor transaction.
                for(int i=1;i<keys.length;i++)if(keys[i].getDeclaringClass().isInstance(p.thisObject)){
                    remove.invoke(p.args[0],nightKey(general[i],p.thisObject));return;
                }
            }
        });
        XposedBridge.hookMethod(loadWorkspace,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable {
                if(p.hasThrowable()||workspaceMode.getInt(p.thisObject)!=173||workspaceLogs.getAndIncrement()>=8)return;
                List<?> rows=(List<?>)getList.invoke(p.thisObject);
                Log.i("AuroraCameraCompat","night213 workspace mode=173 class="+p.thisObject.getClass().getName()+" rows="+rows.size()+" palette="+paletteReady.invoke(null));
            }
        });
    }
    private String nightKey(Method prefix,Object component)throws ReflectiveOperationException{
        String key=(String)prefix.invoke(component,173)+"_173";
        return (Boolean)external.invoke(null)?(String)externalKey.invoke(null,key):key;
    }
    public Object bottom(Object factory)throws ReflectiveOperationException {
        boolean enabled=(Boolean)paletteReady.invoke(null);
        Object previous=XposedHelpers.getAdditionalInstanceField(factory,"night213.route");
        if(!Boolean.valueOf(enabled).equals(previous)){
            XposedHelpers.setAdditionalInstanceField(factory,"night213.route",enabled);
            if(routeLogs.getAndIncrement()<8)Log.i("AuroraCameraCompat","night213 bottom mode=173 panel="+(enabled?"native-CamStyle":"native-basic-filter")+" palette="+enabled);
        }
        if(!enabled)return legacyButton.invoke(factory);
        boolean frontOrFlip=(Boolean)front.invoke(global.invoke(null))||(Boolean)flip.invoke(null);
        return camStyleButton.invoke(factory,frontOrFlip);
    }
}
