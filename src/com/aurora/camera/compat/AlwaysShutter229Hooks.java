package com.aurora.camera.compat;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Phoenix shutter-only mute bypass, with an explicit headphone-only route. */
public final class AlwaysShutter229Hooks {
    private AlwaysShutter229Hooks(){}
    public static boolean shutter(int event){return event==0||(event>=2&&event<=5);}
    public static boolean headphone(int type){return type==AudioDeviceInfo.TYPE_WIRED_HEADSET||type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||type==AudioDeviceInfo.TYPE_USB_HEADSET||type==AudioDeviceInfo.TYPE_BLE_HEADSET||type==AudioDeviceInfo.TYPE_HEARING_AID;}
    public static int priority(int type){if(type==AudioDeviceInfo.TYPE_WIRED_HEADSET||type==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||type==AudioDeviceInfo.TYPE_USB_HEADSET)return 100;if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLE_HEADSET)return 90;if(type==AudioDeviceInfo.TYPE_HEARING_AID)return 80;return type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO?10:0;}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        Options229 options=new Options229(h);
        Method setting=h.method("com.android.camera.fragment.settings.CameraCommonPreferenceFragment","registerPreferenceListener");
        Method moduleSound=h.method("com.android.camera.module.u","playCameraSound",int.class);
        Method gate=h.method("E1.g4","c"),load=h.method("E1.g4","g",int.class),play=h.method("E1.g4","p",float.class,int.class),release=h.method("E1.g4","n",int.class);
        Method accept=h.method("E1.c4","accept",Object.class);Field variant=h.field("E1.c4","a"),event=h.field("E1.g4$d","a");Class<?> soundEvent=h.type("E1.g4$d");
        Method legacy=AudioAttributes.Builder.class.getDeclaredMethod("setLegacyStreamType",int.class),build=AudioTrack.Builder.class.getDeclaredMethod("build"),ringer=AudioManager.class.getDeclaredMethod("getRingerMode");
        ThreadLocal<Boolean> dispatch=new ThreadLocal<>(),module=new ThreadLocal<>();ThreadLocal<AudioDeviceInfo> route=new ThreadLocal<>();ThreadLocal<Boolean> loading=new ThreadLocal<>();
        Map<Object,Map<Integer,Integer>> routes=Collections.synchronizedMap(new WeakHashMap<>());
        return new HookPlan(){public String id(){return "229.always-shutter-headphone-route";}
            private AudioDeviceInfo output(){
                AudioManager manager=options.context().getSystemService(AudioManager.class);
                AudioDeviceInfo chosen=null;for(AudioDeviceInfo device:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(headphone(device.getType())&&(chosen==null||priority(device.getType())>priority(chosen.getType())))chosen=device;
                return chosen;
            }
            public void install(){
                XposedBridge.hookMethod(setting,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())options.addSwitch(p.thisObject,Options229.SOUND,"始终开启快门声","忽略系统静音；连接蓝牙或有线耳机时仅在耳机播放","pref_camerasound_key",null);}});
                XposedBridge.hookMethod(moduleSound,new StrictHook(){@Override protected void before(MethodHookParam p){p.setObjectExtra("229.sound.module",module.get());module.set(options.enabled(Options229.SOUND)&&shutter((Integer)p.args[0]));}@Override protected void after(MethodHookParam p){Boolean prior=(Boolean)p.getObjectExtra("229.sound.module");if(prior==null)module.remove();else module.set(prior);}});
                XposedBridge.hookMethod(gate,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&Boolean.TRUE.equals(module.get()))p.setResult(true);}});
                XposedBridge.hookMethod(accept,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    p.setObjectExtra("229.sound.dispatch",dispatch.get());
                    if(variant.getInt(p.thisObject)==0&&soundEvent.isInstance(p.args[0])&&shutter(event.getInt(p.args[0]))&&options.enabled(Options229.SOUND))dispatch.set(true);
                }@Override protected void after(MethodHookParam p){Boolean prior=(Boolean)p.getObjectExtra("229.sound.dispatch");if(prior==null)dispatch.remove();else dispatch.set(prior);}});
                XposedBridge.hookMethod(ringer,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(dispatch.get()))p.setResult(AudioManager.RINGER_MODE_NORMAL);}});
                XposedBridge.hookMethod(load,new StrictHook(){@Override protected void before(MethodHookParam p){
                    p.setObjectExtra("229.sound.loading",loading.get());p.setObjectExtra("229.sound.route",route.get());
                    if(options.enabled(Options229.SOUND)&&shutter((Integer)p.args[0])){loading.set(true);route.set(output());}
                }@Override protected void after(MethodHookParam p){Boolean prior=(Boolean)p.getObjectExtra("229.sound.loading");if(prior==null)loading.remove();else loading.set(prior);AudioDeviceInfo device=(AudioDeviceInfo)p.getObjectExtra("229.sound.route");if(device==null)route.remove();else route.set(device);}});
                XposedBridge.hookMethod(legacy,new StrictHook(){@Override protected void before(MethodHookParam p){if(Boolean.TRUE.equals(loading.get()))p.args[0]=route.get()==null?7:AudioManager.STREAM_MUSIC;}});
                XposedBridge.hookMethod(build,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(p.hasThrowable()||!Boolean.TRUE.equals(loading.get())||route.get()==null)return;
                    AudioTrack track=(AudioTrack)p.getResult();if(!track.setPreferredDevice(route.get())){track.release();throw new IllegalStateException("Cannot route shutter to connected headphones");}
                    Log.i("AuroraCameraCompat","shutter229 headphone route="+route.get().getId()+" type="+route.get().getType());
                }});
                XposedBridge.hookMethod(play,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    int id=(Integer)p.args[1];if(!shutter(id))return;
                    boolean on=options.enabled(Options229.SOUND);AudioDeviceInfo device=on?output():null;int next=on?(device==null?-2:device.getId()):-1;
                    Map<Integer,Integer> known; synchronized(routes){known=routes.computeIfAbsent(p.thisObject,k->new java.util.HashMap<>());}
                    synchronized(known){Integer previous=known.get(id);if(previous==null||previous!=next){release.invoke(p.thisObject,id);known.put(id,next);}}
                }});
            }
        };
    }
}
