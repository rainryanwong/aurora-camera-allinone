package com.aurora.camera.compat.workspace;

import android.util.Log;
import com.aurora.camera.compat.HookPlan;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Stock1770 Capture.T can receive a page-selection callback before its posted default-row binding. */
public final class Workspace248Hooks implements HookPlan {
    private static final String TAG="AuroraWorkspace248";
    private final Class<?> capture,workspace,row;
    private final Field ownerWorkspace,defaultRow,nativeOriginal,master;
    private final Method choose,isOriginal;

    private Workspace248Hooks(Host host) throws ReflectiveOperationException {
        capture=host.type("com.android.camera.features.mode.capture.T");
        workspace=host.type("com.android.camera.features.mode.capture.d");
        row=host.type("com.android.camera.features.mode.capture.e");
        Class<?> item=host.type("U9.S"),base=host.type("U9.a");
        ownerWorkspace=host.field("U9.M","V");defaultRow=host.field("U9.M","W");
        nativeOriginal=host.field(capture,"E0");
        master=host.field("com.xiaomi.microfilm.vlog.vv.v","mItemList");
        choose=host.method("U9.M","Yt",item,boolean.class,int.class);
        isOriginal=host.method(row,"b0");
        if(choose.getReturnType()!=void.class||isOriginal.getReturnType()!=boolean.class
                ||ownerWorkspace.getType()!=base||defaultRow.getType()!=item||nativeOriginal.getType()!=row||master.getType()!=List.class
                ||!host.type("U9.M").isAssignableFrom(capture)||!base.isAssignableFrom(workspace)||!item.isAssignableFrom(row))
            throw new NoSuchMethodException("Workspace248 pinned Capture.T selection ABI differs");
    }
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {return new Workspace248Hooks(host);}
    @Override public String id(){return "248.capture-workspace-null-default";}
    @Override public void install(){
        XposedBridge.hookMethod(choose,new StrictHook(){
            @Override protected void before(MethodHookParam p) throws Throwable {
                if(!capture.isInstance(p.thisObject)||!row.isInstance(p.args[0])||defaultRow.get(p.thisObject)!=null)return;
                Object current=ownerWorkspace.get(p.thisObject);if(!workspace.isInstance(current))return;
                // T.eu() posts this exact b0() scan. Use its existing native row, never synthesize a recipe.
                for(Object candidate:(List<?>)master.get(current))if((Boolean)isOriginal.invoke(candidate)){
                    nativeOriginal.set(p.thisObject,candidate);defaultRow.set(p.thisObject,candidate);
                    Log.i(TAG,"bound existing native original before posted workspace initialization");
                    return;
                }
                // A workspace with no native original is not a demonstrated supported input.
                // Leave its native error path intact instead of substituting an arbitrary recipe.
            }
        });
    }
}
