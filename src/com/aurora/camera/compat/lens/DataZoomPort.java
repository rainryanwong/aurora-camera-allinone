package com.aurora.camera.compat.lens;

import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** data.i.T retains native processing except the accepted high-pixel exclusion. */
public final class DataZoomPort {
    private final Method outer, source, repository, component, running, rear, front;
    private final Class<?> cropType, restrictionType;
    private final Method cropOn, cropFactor, cropValue, hardware, feature, restricted, featureProvider;
    private final Method roles, standaloneSupported, teleSupported, standaloneZoom, teleZoom;
    private final Field roleProvider, cameraGrip, gripController;
    private final Method restrictionOn, normalize, toArray, firstExcluded, secondExcluded;
    private final NativeCaller caller;
    private final ThreadLocal<Integer> active=new ThreadLocal<>();
    private static final String PREVIOUS="aurora.compat.lens.zoom.previous";

    public DataZoomPort(Host h) throws ReflectiveOperationException {
        outer=h.method("com.android.camera.data.data.i","T",int.class,boolean.class);
        source=h.method("com.android.camera.data.data.i","S",int.class);
        repository=h.method("g2.a","j"); component=h.method("Xh.b","x",Class.class);
        running=h.method("g2.a","g"); rear=h.method("u2.Y","L"); front=h.method("u2.Y","N");
        cropType=h.type("v2.x0"); restrictionType=h.type("v2.D0");
        cropOn=h.method(cropType,"y",int.class); cropFactor=h.method(cropType,"t",String.class);
        cropValue=h.method("com.android.camera.data.data.x","m");
        hardware=h.method("l9.h","r3"); featureProvider=h.method("Pe.b","D"); feature=h.method("Pe.b","I2");
        restricted=h.method("com.android.camera.data.data.i","M0",int.class);
        roles=h.method("v6.f","R"); roleProvider=h.field("v6.f","a");
        standaloneSupported=h.method("v6.a","v"); teleSupported=h.method("v6.a","f");
        standaloneZoom=h.method("Kr.i","i"); teleZoom=h.method("Kr.i","h");
        restrictionOn=h.method(restrictionType,"r");
        normalize=h.method("com.android.camera.data.data.i","k",List.class);
        toArray=h.method("Lr.d","k",List.class);
        firstExcluded=h.method("com.android.camera.data.data.i","f",int.class,boolean.class);
        secondExcluded=h.method("com.android.camera.data.data.i","e",int.class,boolean.class);
        cameraGrip=h.field("com.android.camera.Camera","X1"); gripController=h.field("X5.d","b");
        caller=new NativeCaller(h.loader);
    }

    public Object cameraGrip(Object camera) throws ReflectiveOperationException {
        return gripController.get(cameraGrip.get(camera));
    }

    public void install() {
        XposedBridge.hookMethod(outer,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS,active.get()); active.set((int)p.args[0]);
                if ((int)p.args[0]==175) p.setResult(highPixel((boolean)p.args[1]));
            }
            @Override protected void after(MethodHookParam p) {
                Integer previous=(Integer)p.getObjectExtra(PREVIOUS);
                if (previous==null) active.remove(); else active.set(previous);
            }
        });
        XposedBridge.hookMethod(source,new LensHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable() && Integer.valueOf(173).equals(active.get()) && (int)p.args[0]==173 && caller.matches(source,outer))
                    p.setResult(LensRules.nightTwo(173,(float[])p.getResult(),(boolean)front.invoke(running.invoke(null))));
            }
        });
    }

    private float[] singleton(float value) throws ReflectiveOperationException {
        ArrayList<Float> values=new ArrayList<>(); values.add(value);
        return (float[])toArray.invoke(null,normalize.invoke(null,values));
    }

    private float[] highPixel(boolean inner) throws ReflectiveOperationException {
        float[] values=(float[])source.invoke(null,175);
        Object crop=component.invoke(repository.invoke(null),cropType);
        if ((boolean)cropOn.invoke(crop,175) && (boolean)rear.invoke(running.invoke(null))) {
            float factor=(float)cropFactor.invoke(crop,cropValue.invoke(null));
            int index=0;
            for (int i=0;i<values.length;i++) if (values[i]==1.0f) { index=i; break; }
            values[index]=factor;
        }
        // The accepted 175 branch jumps past the original high-pixel filtering.
        if ((boolean)hardware.invoke(null) && (boolean)feature.invoke(featureProvider.invoke(null))
                && (boolean)restricted.invoke(null,175)) {
            Object provider=roleProvider.get(roles.invoke(null));
            if ((boolean)standaloneSupported.invoke(provider)) values=singleton((float)standaloneZoom.invoke(null));
            else if ((boolean)teleSupported.invoke(provider)) values=singleton((float)teleZoom.invoke(null));
        }
        if (inner && (boolean)hardware.invoke(null)
                && (boolean)restrictionOn.invoke(component.invoke(repository.invoke(null),restrictionType)))
            values=singleton((float)teleZoom.invoke(null));
        if (((boolean)firstExcluded.invoke(null,175,false) || (boolean)secondExcluded.invoke(null,175,false)) && inner) {
            float[] result=new float[values.length-1];
            System.arraycopy(values,1,result,0,result.length); return result;
        }
        return values;
    }
}
