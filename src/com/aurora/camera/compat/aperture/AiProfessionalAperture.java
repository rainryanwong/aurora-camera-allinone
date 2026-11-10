package com.aurora.camera.compat.aperture;

import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/** Professional continuous-aperture contract for the independent AI component. */
final class AiProfessionalAperture {
    private static final String STATE="aurora.compat.aperture.ai.preview";
    private static final String BINDING="aurora.compat.aperture.ai.asd";
    private final Class<?> aperture, professional, asd, panel;
    private final Method initialize, tableLoad, available, validate, discrete, items, automatic, effectiveValue;
    private final Method running, runningMode, runningRear, repository, getComponent;
    private final Method parse, tagValue, consume, append, manager, resolveManager, addInterceptor, panelProvider, render;
    private final Constructor<?> asdConstructor;
    private final Field mode, initMode, profile, table, adjustable, supported, runningIndex;
    private final Field target, managerMode, managerComponent, interceptors, panelMode, selectors, selectorComponent;

    private static final class State {
        volatile int generation;
        volatile Float preview;
        volatile long revision;
        volatile long renderedRevision;
    }
    private static final class Binding {
        final Object component;
        final State state;
        final int generation;
        Binding(Object component,State state) {
            this.component=component;this.state=state;generation=state.generation;
        }
    }

    AiProfessionalAperture(Host h) throws ReflectiveOperationException {
        aperture=h.type("v2.k");professional=h.type("r2.o0");asd=h.type("s6.p");panel=h.type("Q4.D");
        initialize=h.method("v2.k","N",h.type("com.android.camera.data.data.C"));
        tableLoad=h.method("v2.k","D",h.type("l9.g"));available=h.method("l9.h","e",h.type("l9.g"));
        validate=h.method("v2.k","checkValueValid",int.class,String.class);
        discrete=h.method("v2.k","I");items=h.method("v2.k","getItems");
        automatic=h.method("v2.k","b");effectiveValue=h.method("v2.k","A",int.class);
        mode=h.field("com.android.camera.data.data.c","mCurrentMode");
        initMode=h.field("com.android.camera.data.data.C","a");
        profile=h.field("v2.k","c0");table=h.field("v2.k","c");
        adjustable=h.field("v2.k","d0");supported=h.field("v2.k","W");
        running=h.method("g2.a","g");runningMode=h.method("u2.Y","D",int.class);
        runningRear=h.method("u2.Y","L");runningIndex=h.field("u2.Y","u");
        repository=h.method("g2.a","j");getComponent=h.method("Xh.b","x",Class.class);
        asdConstructor=asd.getDeclaredConstructor(h.type("U1.e"));asdConstructor.setAccessible(true);
        target=h.field("s6.p","e");managerMode=h.field("U1.b","b");
        managerComponent=h.field("U1.b","e");resolveManager=h.method("U1.b","d0");
        parse=h.method("s6.p","tagValueAutomaticParsed");
        tagValue=h.method("com.android.camera.module.interceptor.base.i","getTagValue",int.class,Object.class);
        consume=h.method("s6.p","consumeResultOnMainThreadIfDataChanged");
        append=h.method("com.android.camera.features.mode.ai.AiModule","appendModuleExternalASD",
                h.type("com.android.camera.module.interceptor.base.a"));
        manager=h.method("com.android.camera.module.Camera2Module","getApertureManager");
        interceptors=h.field("com.android.camera.module.interceptor.base.a","a");
        addInterceptor=h.method("com.android.camera.module.interceptor.base.a","a",
                h.type("com.android.camera.module.interceptor.base.i"));
        panelProvider=h.method("R6.B0","a");panelMode=h.field("com.android.camera.fragment.h","mCurrentMode");
        selectors=h.field("Q4.D","t");selectorComponent=h.field("Q4.x0","b");
        render=h.method("Q4.D","Ss",int.class);
    }

    void install() {
        XposedBridge.hookMethod(initialize,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (professional.isInstance(p.thisObject)) return;
                if (mode.getInt(p.thisObject)==168 || initMode.getInt(p.args[0])==168) {
                    State state=state(p.thisObject);
                    synchronized (state) {
                        state.generation++;state.preview=null;
                        state.revision++;state.renderedRevision=state.revision;
                    }
                }
            }
        });
        XposedBridge.hookMethod(tableLoad,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (!ai(p.thisObject) || profile.getInt(p.thisObject)==1) return;
                int kind=profile.getInt(p.thisObject);
                // Exactly r2.o0.D: use full availableApertures, not lowResAvailableApertures.
                if (kind==2 || kind==3) {
                    table.set(p.thisObject,available.invoke(null,p.args[0]));p.setResult(null);
                }
            }
        });
        XposedBridge.hookMethod(validate,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if ((Integer)p.args[0]!=168 || !ai(p.thisObject)
                        || (Boolean)discrete.invoke(p.thisObject)) return;
                // Same unsupported/items/bounds/-1 contract as r2.o0.checkValueValid.
                if (!adjustable.getBoolean(p.thisObject)) {p.setResult(true);return;}
                if (((List<?>)items.invoke(p.thisObject)).isEmpty()) {p.setResult(false);return;}
                float value=Float.parseFloat((String)p.args[1]);
                float[] values=(float[])table.get(p.thisObject);
                p.setResult(value==-1.0f || value>=values[0] && value<=values[values.length-1]);
            }
        });
        XposedBridge.hookMethod(effectiveValue,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if ((Integer)p.args[0]!=168 || !current(p.thisObject)
                        || !(Boolean)automatic.invoke(p.thisObject)) return;
                Float actual=state(p.thisObject).preview;
                // No nominal aperture or cached default is presented as a CaptureResult value.
                // Native c() formats F text; native a()/AUTO button consume the same actual value.
                // The preference and HAL request remain the negative AUTO sentinel.
                if (actual!=null) p.setResult(actual);
            }
        });
        XposedBridge.hookMethod(resolveManager,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable() || managerMode.getInt(p.thisObject)!=168) return;
                Object component=component();
                if (current(component)) managerComponent.set(p.thisObject,component);
            }
        });
        XposedBridge.hookMethod(asdConstructor,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable() && managerMode.getInt(p.args[0])==168) bind(p.thisObject);
            }
        });
        XposedBridge.hookMethod(append,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable()) return;
                Object component=component();
                if (!current(component)) return;
                // Stock gates this AI registration through professional component flags.
                // Keep its interceptor when present; close only the evidenced missing-registration case.
                for (Object value:(List<?>)interceptors.get(p.args[0])) if (asd.isInstance(value)) {
                    bind(value);return;
                }
                Object value=asdConstructor.newInstance(manager.invoke(p.thisObject));
                bind(value);addInterceptor.invoke(p.args[0],value);
            }
        });
        XposedBridge.hookMethod(parse,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable()) return;
                Binding binding=binding(p.thisObject);
                if (!live(binding) || !(Boolean)automatic.invoke(binding.component)) return;
                // Re-read the declared LENS_APERTURE tag with null, never s6.p's nominal/default c.
                Object value=tagValue.invoke(p.thisObject,0,null);
                if (value instanceof Float) {
                    float actual=(Float)value;
                    if (Float.isFinite(actual) && actual>0.0f) {
                        synchronized (binding.state) {
                            if (live(binding) && !value.equals(binding.state.preview)) {
                                binding.state.preview=(Float)value;binding.state.revision++;
                            }
                        }
                    }
                }
            }
        });
        XposedBridge.hookMethod(consume,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable()) return;
                Binding binding=binding(p.thisObject);
                if (!live(binding)
                        || !(Boolean)automatic.invoke(binding.component)) return;
                final long revision=binding.state.revision;
                if (revision==binding.state.renderedRevision) return;
                Object value=((Optional<?>)panelProvider.invoke(null)).orElse(null);
                if (!panel.isInstance(value) || panelMode.getInt(value)!=168) return;
                List<?> rows=(List<?>)selectors.get(value);
                if (rows==null) return;
                for (int i=0;i<rows.size();i++) if (selectorComponent.get(rows.get(i))==binding.component) {
                    render.invoke(value,i);
                    // A parse during render belongs to a later revision and remains pending.
                    binding.state.renderedRevision=revision;return;
                }
            }
        });
    }

    private boolean ai(Object component) throws IllegalAccessException {
        return aperture.isInstance(component) && !professional.isInstance(component) && mode.getInt(component)==168;
    }
    private Object component() throws ReflectiveOperationException {
        return getComponent.invoke(repository.invoke(null),aperture);
    }
    private boolean current(Object component) throws ReflectiveOperationException {
        if (!ai(component) || !supported.getBoolean(component)) return false;
        Object data=running.invoke(null);
        return (Integer)runningMode.invoke(data,runningIndex.getInt(data))==168 && (Boolean)runningRear.invoke(data);
    }
    private void bind(Object interceptor) throws ReflectiveOperationException {
        Object component=component();
        if (!current(component)) return;
        target.set(interceptor,component);
        XposedHelpers.setAdditionalInstanceField(interceptor,BINDING,new Binding(component,state(component)));
    }
    private Binding binding(Object interceptor) {
        return (Binding)XposedHelpers.getAdditionalInstanceField(interceptor,BINDING);
    }
    private boolean live(Binding binding) throws ReflectiveOperationException {
        return binding!=null && binding.generation==binding.state.generation && current(binding.component);
    }
    private static State state(Object component) {
        State state=(State)XposedHelpers.getAdditionalInstanceField(component,STATE);
        if (state==null) {
            state=new State();XposedHelpers.setAdditionalInstanceField(component,STATE,state);
        }
        return state;
    }
}
