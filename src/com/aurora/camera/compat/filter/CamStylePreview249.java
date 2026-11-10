package com.aurora.camera.compat.filter;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Register the existing native PictureStyle attribute in the same main preview graph. */
public final class CamStylePreview249 {
    private final Method state,mode,facing,controller,provider,workspace,original,add,write;
    private final Field intent,mainEngine,rows,selected;
    private final Object picture;
    private final Class<?> rear;
    public CamStylePreview249(Host h)throws ReflectiveOperationException {
        state=h.method("g2.a","g");mode=h.method("u2.Y","D",int.class);facing=h.method("u2.Y","B");intent=h.field("u2.Y","u");
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");mainEngine=h.field("com.xiaomi.camera.effect.EffectController","V");
        provider=h.method("g2.a","e");workspace=h.method("y2.a","a",Class.class);rear=h.type("com.android.camera.features.mode.capture.k");
        rows=h.field("com.xiaomi.microfilm.vlog.vv.v","mItemList");selected=h.field("U9.S","n");original=h.method("com.android.camera.features.mode.capture.e","b0");
        picture=h.field("Ku.d","P").get(null);add=h.method("F8.j","V0",h.type("Ku.d"));write=h.method("F8.j","W0",h.type("Ku.d"),Object[].class);

    }
    private boolean eligible(Object engine)throws ReflectiveOperationException {
        Object current=state.invoke(null);if((Integer)mode.invoke(current,intent.getInt(current))!=163||(Integer)facing.invoke(current)==1)return false;
        Object reference=mainEngine.get(controller.invoke(null));
        if(!(reference instanceof WeakReference)||((WeakReference<?>)reference).get()!=engine)return false;
        Object owner=workspace.invoke(provider.invoke(null),rear);
        CamStyleAccess260.lock();try{
        Object loaded=rows.get(owner);if(!(loaded instanceof List))return false;
        for(Object row:(List<?>)loaded)if(selected.getBoolean(row)&&!(Boolean)original.invoke(row))return true;
        return false;
        }finally{CamStyleAccess260.unlock();}
    }
    public void install(){
        XposedBridge.hookMethod(write,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||p.args[0]!=picture)return;
            Object[] values=(Object[])p.args[1];if((Integer)values[0]<=0&&(Integer)values[8]<=0)return;
            if(!eligible(p.thisObject))return;
            // W0 already assigns the native attribute. V0 only adds that cached
            // renderer to H; its GL runnable has an identity contains guard.
            Object renderer=add.invoke(p.thisObject,picture);
            if(renderer==null)throw new IllegalStateException("249 native PictureStyle renderer factory returned null");
        }});
    }
}
