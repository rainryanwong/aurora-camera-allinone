package com.aurora.camera.compat;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;
import android.content.Context;
import com.aurora.camera.compat.nightvideo.NightVideoBridge;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

/** Exact 1770 night-video routing and session/cancellation ownership. */
public final class NightVideoHooks {
    private static final String RESTORE_MARKER = "pref_camera_from_super_nigtht_video_module";
    private NightVideoHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Access access = new Access(host);
        final NightVideoBridge bridge = new NightVideoBridge(access);
        return new HookPlan() {
            @Override public String id() { return "night-video"; }
            @Override public void install() {
                XposedBridge.hookMethod(access.switchVideo, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if ((Integer) read(access.fragmentMode, param.thisObject) == 173) {
                            bridge.open(call(access.context, param.thisObject));
                            param.setResult(null);
                        }
                    }
                });
                XposedBridge.hookMethod(access.createStart, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        bridge.target((Integer) param.args[0]);
                    }
                });
                XposedBridge.hookMethod(access.sessionSuccess, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (!param.hasThrowable()) bridge.ready(param.thisObject);
                    }
                });
                XposedBridge.hookMethod(access.sessionFailed, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (!param.hasThrowable()) bridge.failed(param.thisObject);
                    }
                });
                XposedBridge.hookMethod(access.inactive, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        bridge.failed(param.thisObject);
                    }
                });
                XposedBridge.hookMethod(access.restoreMode, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        if ((Integer) param.args[0] != 173) return;
                        Object prefs = call(access.globalPreferences, null);
                        if (!(Boolean) call(access.readBoolean, prefs, RESTORE_MARKER, false)
                                || (Boolean) call(access.keepNightPhoto, null)) return;
                        if (call(access.registeredEntry, null, 214) == null) {
                            // The physical NightVideoModuleEntry class exists. The
                            // target's actual registry decides whether 214 is usable.
                            call(access.edit, prefs);
                            call(access.writeBoolean, prefs, RESTORE_MARKER, false);
                            call(access.commit, prefs);
                        }
                    }
                });
            }
        };
    }

    private static final class Access implements NightVideoBridge.Runtime {
        final Class<?> camera, moduleControllerType, nightOptionType;
        final Field fragmentMode, modeSourceIndex;
        final Method switchVideo, context, createStart, resetType, blur, viewConfig, switchMode;
        final Method moduleIndex, alive, recording, activity, controllerInstance, controllerModule;
        final Method controllerMode, enableNight, modeSource, modeLookup, selectMode;
        final Method runningPreferences, globalPreferences, component, empty, blockedA0, blockedXZ, alreadyNight;
        final Method sessionSuccess, sessionFailed, inactive, restoreMode, readBoolean, edit, writeBoolean, commit;
        final Method keepNightPhoto, registeredEntry;
        final Handler main = new Handler(Looper.getMainLooper());

        Access(Host host) throws ReflectiveOperationException {
            camera = host.type("com.android.camera.Camera");
            moduleControllerType = host.type("r6.W");
            nightOptionType = host.type("v2.v0");
            Class<?> start = host.type("com.android.camera.module.loader.base.StartControl");
            Class<?> cameraSession = host.type("android.hardware.camera2.CameraCaptureSession");
            Class<?> manager = host.type("R6.D");
            Class<?> preferences = host.type("Xh.a");
            fragmentMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
            modeSourceIndex = host.field("u2.Y", "u");
            if (fragmentMode.getType() != int.class || modeSourceIndex.getType() != int.class
                    || Modifier.isStatic(fragmentMode.getModifiers()) || Modifier.isStatic(modeSourceIndex.getModifiers())) {
                throw new NoSuchFieldException("Night-video mode fields must be instance int");
            }
            switchVideo = method(host,"z4.E","Sh",void.class,false,View.class);
            context = method(host,"androidx.fragment.app.Fragment","getContext",Context.class,false);
            createStart = method(host,start.getName(),"create",start,true,int.class);
            resetType = method(host,start.getName(),"setResetType",start,false,int.class);
            blur = method(host,start.getName(),"setNeedBlurAnimation",start,false,boolean.class);
            viewConfig = method(host,start.getName(),"setViewConfigType",start,false,int.class);
            switchMode = method(host,camera.getName(),"i8",void.class,false,start);
            moduleIndex = method(host,"com.android.camera.module.u","getModuleIndex",int.class,false);
            alive = method(host,"com.android.camera.module.u","isDeviceAndModuleAlive",boolean.class,false);
            recording = method(host,"com.android.camera.module.VideoBase","isRecording",boolean.class,false);
            activity = method(host,"com.android.camera.module.u","getActivity",host.type("androidx.fragment.app.m"),false);
            controllerInstance = method(host,manager.getName(),"b",manager,true);
            controllerModule = method(host,moduleControllerType.getName(),"ja",Optional.class,false);
            controllerMode = method(host,moduleControllerType.getName(),"Jc",int.class,false);
            enableNight = method(host,moduleControllerType.getName(),"q",void.class,false,int.class,int.class);
            modeSource = method(host,"g2.a","g",host.type("u2.Y"),true);
            modeLookup = method(host,"u2.Y","D",int.class,false,int.class);
            selectMode = method(host,"u2.Y","b0",void.class,false,int.class);
            runningPreferences = method(host,"g2.a","j",host.type("v2.F0"),true);
            globalPreferences = method(host,"g2.a","a",host.type("r2.n1"),true);
            component = method(host,"Xh.b","x",Object.class,false,Class.class);
            empty = method(host,"com.android.camera.data.data.c","isEmpty",boolean.class,false);
            blockedA0 = method(host,"K2.b","a0",boolean.class,true);
            blockedXZ = method(host,"com.android.camera.data.data.x","Z",boolean.class,true);
            alreadyNight = method(host,"com.android.camera.data.data.F","X",boolean.class,true);
            sessionSuccess = method(host,"com.android.camera.module.VideoModule","onPreviewSessionSuccess",void.class,false,cameraSession);
            sessionFailed = method(host,"com.android.camera.module.VideoModule","onPreviewSessionFailed",void.class,false,cameraSession);
            inactive = method(host,"com.android.camera.module.VideoModule","onInactive",void.class,false);
            restoreMode = method(host,"V4.f","rs",boolean.class,false,int.class,String.class,boolean.class);
            readBoolean = method(host,preferences.getName(),"h",boolean.class,false,String.class,boolean.class);
            edit = method(host,preferences.getName(),"g",preferences,false);
            writeBoolean = method(host,preferences.getName(),"n",preferences,false,String.class,boolean.class);
            commit = method(host,preferences.getName(),"c",void.class,false);
            keepNightPhoto = method(host,"K2.m","b",boolean.class,true);
            registeredEntry = method(host,"t3.a","c",host.type("com.android.camera.module.entry.a"),true,int.class);
        }

        @Override public boolean cameraContext(Object context) { return camera.isInstance(context); }
        @Override public long uptimeMillis() { return SystemClock.uptimeMillis(); }
        @Override public void openVideo(Object context) {
            call(selectMode,call(modeSource,null),162);
            Object control = call(createStart,null,162);
            call(resetType,control,8);
            call(blur,control,true);
            call(viewConfig,control,2);
            call(switchMode,context,control);
        }
        @Override public boolean current(Object module) {
            if ((Integer) call(moduleIndex,module) != 162) return false;
            Object controller = call(controllerInstance,null);
            if (!moduleControllerType.isInstance(controller)) return false;
            Optional<?> active = (Optional<?>) call(controllerModule,controller);
            return active.isPresent() && active.get() == module;
        }
        @Override public boolean alive(Object module) { return (Boolean) call(alive,module); }
        @Override public void post(Runnable task) { main.post(task); }
        @Override public int activeMode() {
            Object source = call(modeSource,null);
            return (Integer) call(modeLookup,source,(Integer) read(modeSourceIndex,source));
        }
        @Override public boolean recording(Object module) { return (Boolean) call(recording,module); }
        @Override public Object nightOption() { return call(component,call(runningPreferences,null),nightOptionType); }
        @Override public boolean empty(Object option) { return (Boolean) call(empty,option); }
        @Override public boolean blockedA0() { return (Boolean) call(blockedA0,null); }
        @Override public boolean blockedXZ() { return (Boolean) call(blockedXZ,null); }
        @Override public boolean alreadyNight() { return (Boolean) call(alreadyNight,null); }
        @Override public Object controller() { return call(controllerInstance,null); }
        @Override public boolean moduleController(Object controller) { return moduleControllerType.isInstance(controller); }
        @Override public int controllerMode(Object controller) { return (Integer) call(controllerMode,controller); }
        @Override public void enableNight(Object controller) { call(enableNight,controller,0xd41,1); }
        @Override public Object activity(Object module) { return call(activity,module); }
        @Override public void unsupported(Object activity) {
            Toast.makeText((Context) activity,"当前镜头或录像配置不支持夜景视频，请使用后置主摄",Toast.LENGTH_LONG).show();
        }
    }

    private static Method method(Host host,String owner,String name,Class<?> result,boolean isStatic,
            Class<?>... parameters) throws ReflectiveOperationException {
        Method method=host.method(owner,name,parameters);
        if(method.getReturnType()!=result || Modifier.isStatic(method.getModifiers())!=isStatic)
            throw new NoSuchMethodException(owner+"."+name+" return/static contract");
        return method;
    }
    private static Object read(Field field,Object receiver) {
        try{return field.get(receiver);}
        catch(IllegalAccessException failure){throw new IllegalStateException("Resolved night-video field inaccessible",failure);}
    }
    private static Object call(Method method,Object receiver,Object... arguments) {
        try{return method.invoke(receiver,arguments);}
        catch(IllegalAccessException failure){throw new IllegalStateException("Resolved night-video method inaccessible: "+method,failure);}
        catch(InvocationTargetException failure){
            Throwable cause=failure.getCause();
            if(cause instanceof RuntimeException)throw(RuntimeException)cause;
            if(cause instanceof Error)throw(Error)cause;
            throw new IllegalStateException("Night-video host invocation failed: "+method,cause);
        }
    }
}
