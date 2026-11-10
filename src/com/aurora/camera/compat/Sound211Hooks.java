package com.aurora.camera.compat;

import android.content.Context;
import de.robv.android.xposed.XposedBridge;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Phoenix sound bytes, stock picker/persistence/player. No host resources are replaced. */
public final class Sound211Hooks {
    private static final String[] TOKENS={"hasselblad","laoda","xpan"};
    private static final String[] TITLES={"哈苏","老大","XPAN"};
    private Sound211Hooks() {}
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{
        final Method catalog=h.method("f2.c","b"),selected=h.method("f2.c","a"),defaults=h.method("f2.c","c");
        final Constructor<?> row=h.type("f2.c").getDeclaredConstructor(int.class,String.class);
        final Field token=h.field("f2.c","b"),label=h.field("f2.c","a");
        final Method init=h.method("com.android.camera.fragment.settings.FragmentCustomShutterSound","init");
        final Field keys=h.field("com.android.camera.fragment.settings.FragmentCustomShutterSound","mEntryKeys");
        final Field values=h.field("com.android.camera.fragment.settings.FragmentCustomShutterSound","mEntryValues");
        final Method resume=h.method("com.android.camera.fragment.settings.CameraCommonPreferenceFragment","onResume");
        final Field group=h.field("com.android.camera.fragment.settings.b","mPreferenceGroup");
        final Method find=h.method("androidx.preference.PreferenceGroup","k0",CharSequence.class);
        final Method text=h.method("miuix.preference.TextPreference","k0",String.class);
        final Method asset=h.method("E1.g4","e",String.class),decode=h.method("E1.g4","j",InputStream.class);
        final Method resource=h.method("E1.g4","f",int.class);
        final Field filenames=h.field("E1.g4","n");
        final Field[] nativeSets={h.field("E1.g4","j"),h.field("E1.g4","k"),h.field("E1.g4","l"),h.field("E1.g4","m")};
        final Method global=h.method("com.xiaomi.camera.basic.Global","getContext");
        final Field profile=h.field("Pe.b","e"),singleton=h.field("Pe.b$b","a");
        final Method resourcePlayer=h.method(profile.getType(),"r2");
        final Method play=h.method("com.android.camera.module.u","playCameraSound",int.class);
        final Method moduleIndex=h.method("com.android.camera.module.u","getModuleIndex");
        row.setAccessible(true);
        return new HookPlan(){
            public String id(){return "211.phoenix-shutter-sounds";}
            public void install(){
                XposedBridge.hookMethod(play,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        if((Integer)p.args[0]!=6)return;
                        int mode=(Integer)moduleIndex.invoke(p.thisObject);if(mode!=163&&mode!=173)return;
                        List<?> rows=(List<?>)catalog.invoke(null);int position=(Integer)selected.invoke(null);
                        // Event 6 loops throughout long exposure; event 0 is the
                        // stock completion shutter and retains the selected sound.
                        if(index((String)token.get(rows.get(position)))>=0)p.setResult(null);
                    }
                });
                XposedBridge.hookMethod(catalog,new StrictHook(){
                    @SuppressWarnings("unchecked") @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable())return;
                        ArrayList<Object> rows=new ArrayList<>((List<Object>)p.getResult());
                        int safeLabel=label.getInt(rows.get(0));
                        for(String id:TOKENS){boolean exists=false;for(Object item:rows)if(id.equals(token.get(item))){exists=true;break;}
                            if(!exists)rows.add(row.newInstance(safeLabel,id));}
                        p.setResult(rows);
                    }
                });
                XposedBridge.hookMethod(init,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable())return;CharSequence[] names=(CharSequence[])keys.get(p.thisObject),ids=(CharSequence[])values.get(p.thisObject);
                        for(int i=0;i<ids.length;i++){int index=index(ids[i].toString());if(index>=0)names[i]=TITLES[index];}
                    }
                });
                XposedBridge.hookMethod(resume,new StrictHook(){
                    @Override protected void after(MethodHookParam p)throws Throwable{
                        if(p.hasThrowable())return;List<?> rows=(List<?>)catalog.invoke(null);int pos=(Integer)selected.invoke(null);
                        if(pos<0||pos>=rows.size())return;int index=index((String)token.get(rows.get(pos)));if(index<0)return;
                        Object preference=find.invoke(group.get(p.thisObject),"custom_shutter_sound_key");if(preference!=null)text.invoke(preference,TITLES[index]);
                    }
                });
                XposedBridge.hookMethod(asset,new StrictHook(){
                    @Override protected void before(MethodHookParam p)throws Throwable{
                        String path=(String)p.args[0];String style=null,suffix=null;
                        for(String id:TOKENS){String prefix="sounds/"+id+"/";if(path.startsWith(prefix)){style=id;suffix=path.substring(prefix.length());break;}}
                        if(style==null)return;
                        String source=suffix;
                        if("camera_click_motor_sound.wav".equals(source))source="camera_click.wav";
                        boolean supplied="camera_click.wav".equals(source)||("hasselblad".equals(style)&&("camera_focus.wav".equals(source)||"camera_fast_burst.wav".equals(source)||"video_record_start.wav".equals(source)||"video_record_end.wav".equals(source)));
                        if(supplied){
                            try(ZipFile apk=new ZipFile(h.moduleApkPath)){
                                ZipEntry entry=apk.getEntry("assets/aurora-sounds/"+style+"/"+source);
                                if(entry==null)throw new IllegalStateException("Packaged sound missing: "+path);
                                try(InputStream input=apk.getInputStream(entry)){p.setResult(decode.invoke(null,input));}
                            }
                            return;
                        }
                        // Phoenix supplies no replacement for these non-shutter events.
                        // Preserve the original default event instead of returning missing audio.
                        String[] names=(String[])filenames.get(null);int event=-1;for(int i=0;i<names.length;i++)if(names[i].equals(suffix)){event=i;break;}
                        if(event<0)throw new IllegalArgumentException("Unknown stock sound event: "+suffix);
                        int base=(Integer)defaults.invoke(null);
                        if(base<0||base>=((List<?>)catalog.invoke(null)).size())throw new IllegalStateException("Stock default sound index");
                        if(base<4&&(Boolean)resourcePlayer.invoke(profile.get(singleton.get(null)))){
                            p.setResult(resource.invoke(null,((int[])nativeSets[base].get(null))[event]));
                        }else{
                            Object baseline=((List<?>)catalog.invoke(null)).get(base);
                            String original=(String)token.get(baseline);
                            if(index(original)>=0)throw new IllegalStateException("Custom sound cannot be stock default");
                            p.args[0]="sounds/"+original+"/"+suffix;
                        }
                    }
                });
            }
        };
    }
    private static int index(String token){for(int i=0;i<TOKENS.length;i++)if(TOKENS[i].equals(token))return i;return -1;}
}
