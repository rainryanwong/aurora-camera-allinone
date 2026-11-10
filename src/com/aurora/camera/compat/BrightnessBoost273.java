package com.aurora.camera.compat;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** One root-owned hold and restore operation per user activation. */
public final class BrightnessBoost273 {
    public static final String KEY="aurora_brightness_boost_value";
    private static final String TAG="AuroraBrightness";
    private static final int MAX_BATTERY_TEMPERATURE_C=48;
    private static final int MAX_SURFACE_TEMPERATURE_C=48;
    private final Options229 options;
    private final Runnable changed;
    private final Consumer<String> toast;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->new Thread(r,"CameraBrightness"));
    private final BrightnessWindow274 window;
    private volatile Session current;
    private static final class Session { Process process; boolean stop,suspended; }

    public BrightnessBoost273(Host host,Runnable changed,Consumer<String> toast){options=new Options229(host);window=new BrightnessWindow274(host);this.changed=changed;this.toast=toast;}
    public int value(){
        int value=options.prefs().getInt(KEY,2065);
        if(value>3600){value=3600;options.prefs().edit().putInt(KEY,value).apply();}
        return value;
    }
    public void activity(android.app.Activity activity){window.activity(activity);}
    public void attributes(android.view.Window target,android.view.WindowManager.LayoutParams attributes){window.attributes(target,attributes);}
    public boolean active(){return current!=null;}
    public void toggle(){
        if(current!=null){stop();return;}
        final int value=value();
        if(value<2065||value>3600)throw new IllegalStateException("亮度数值须为 2065–3600");
        final Context context=options.context();
        Intent battery=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery==null||!battery.hasExtra(BatteryManager.EXTRA_TEMPERATURE)){toast.accept("无法读取电池温度，请稍后再试");return;}
        if(battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0)>MAX_BATTERY_TEMPERATURE_C*10){hot();return;}
        final boolean unlimited=AuthorOptions273.enabled(context,"aurora_author_brightness_unlimited");
        Session session=new Session();current=session;changed.run();
        io.execute(()->run(session,context,value,unlimited));
    }
    private void hot(){toast.accept("当前手机温度过高，请稍后再试");}
    public void focus(boolean focused){
        Session session=current;if(session==null)return;
        synchronized(session){
            session.suspended=!focused;
            if(session.process!=null&&!session.stop)try{
                session.process.getOutputStream().write((focused?"prepare\n":"pause\n").getBytes(StandardCharsets.UTF_8));
                session.process.getOutputStream().flush();
            }catch(IOException failure){Log.e(TAG,"Cannot update brightness focus",failure);}
        }
    }
    public void stop(){
        Session session=current;if(session==null)return;
        synchronized(session){
            session.stop=true;
            if(session.process!=null)try{session.process.getOutputStream().close();}
            catch(IOException failure){Log.e(TAG,"Cannot signal brightness restore",failure);}
        }
    }
    private void run(Session session,Context context,int value,boolean unlimited){
        // stdin EOF is also the camera process-death signal. The shell alone owns
        // both timeout and restore, so a delayed UI callback cannot extend a hold.
        String script="check_temp() { IFS= read -r value < \"$1\" || return 1; case $value in ''|*[!0-9]*) echo \"Invalid temperature: $1=$value\" >&2; return 1;; esac; if [ \"$value\" -gt \"$2\" ]; then echo AURORA_TEMPERATURE_HIGH; return 2; fi; }\n"
            +"battery_zone=\nquiet_zone=\ndisplay_zone=\n"
            +"for zone in /sys/class/thermal/thermal_zone*; do IFS= read -r kind < \"$zone/type\" || exit 1; case $kind in battery) battery_zone=$zone/temp;; quiet_therm) quiet_zone=$zone/temp;; display_therm) display_zone=$zone/temp;; esac; done\n"
            +"if [ -z \"$battery_zone\" ] || [ -z \"$quiet_zone\" ] || [ -z \"$display_zone\" ]; then echo 'Required temperature sensor missing' >&2; exit 1; fi\n"
            +"check_temp /sys/class/power_supply/battery/temp "+MAX_BATTERY_TEMPERATURE_C*10+" || exit $?\n"
            +"check_temp \"$battery_zone\" "+MAX_BATTERY_TEMPERATURE_C*1000+" || exit $?\n"
            +"for path in \"$quiet_zone\" \"$display_zone\"; do check_temp \"$path\" "+MAX_SURFACE_TEMPERATURE_C*1000+" || exit $?; done\n"
            +"node=/sys/class/backlight/panel0-backlight/brightness\n"
            +"held=0\npaused=1\n"
            +"release() { if [ \"$held\" -eq 1 ]; then echo \"$previous\" >\"$node\" || return 1; held=0; fi; }\n"
            +"restore() { result=$?; trap - EXIT HUP INT TERM; if ! release; then echo 'Panel brightness restore failed' >&2; result=1; fi; exit $result; }\n"
            +"trap restore EXIT\ntrap 'exit 0' HUP INT TERM\n"
            +"SECONDS=0\n"
            +"while "+(unlimited?":":"[ \"$SECONDS\" -lt 30 ]")+"; do\n"
            +"IFS= read -r -t 0.2 command\nstatus=$?\n"
            +"if [ \"$status\" -eq 1 ]; then break; fi\n"
            +"if [ \"$status\" -eq 0 ]; then case $command in prepare) IFS= read -r previous < \"$node\" || exit 1; base=$(dumpsys display | awk '/^  mDisplayId=/{main=($0==\"  mDisplayId=0\")} main && !found && /^  mSdrScreenBrightness=/{sub(/^.*=/,\"\");print;found=1}'); [ -n \"$base\" ] || { echo 'Display brightness unavailable' >&2; exit 1; }; echo \"Brightness prepare:$base\";; pause) paused=1; release || exit 1; echo 'Brightness suspended';; resume) paused=0; preparing=$SECONDS;; *) echo 'Invalid brightness command' >&2; exit 1;; esac; fi\n"
            +"if [ \"$paused\" -eq 0 ]; then\n"
            +"if [ \"$held\" -eq 0 ]; then\n"
            +"if ! dumpsys display | awk '/^  mDisplayId=/{main=($0==\"  mDisplayId=0\")} main && /^  mBrightnessReason=override\\(com.android.camera/{override=1} main && /mScreenBrightnessRampAnimator.isAnimating\\(\\)=false/{stopped=1} main && /mScreenReady=true/{ready=1} main && /mBacklightChangeInProgress=false/{done=1} END{exit !(override && stopped && ready && done)}'; then if [ $((SECONDS-preparing)) -ge 3 ]; then echo 'Camera brightness override did not settle' >&2; exit 1; fi; continue; fi\n"
            +"case $previous in ''|*[!0-9]*) echo 'Invalid panel brightness' >&2; exit 1;; esac; held=1; echo 'Brightness holding'; fi\n"
            +"echo "+value+" >\"$node\" || exit 1\nfi\n"
            +"done\nexit 0\n";
        Throwable error=null;boolean tooHot=false;
        try{
            synchronized(session){if(session.stop){main.post(()->{if(current==session)current=null;changed.run();});return;}}
            Process process=new ProcessBuilder("su","-c",script).redirectErrorStream(true).start();
            synchronized(session){
                session.process=process;
                if(session.stop)process.getOutputStream().close();
                else{process.getOutputStream().write((session.suspended?"pause\n":"prepare\n").getBytes(StandardCharsets.UTF_8));process.getOutputStream().flush();}
            }
            StringBuilder output=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(process.getInputStream(),StandardCharsets.UTF_8))){
                String line;while((line=reader.readLine())!=null){if(line.equals("AURORA_TEMPERATURE_HIGH"))tooHot=true;
                    if(line.startsWith("Brightness prepare:")){
                        final float baseline=Float.parseFloat(line.substring("Brightness prepare:".length()));
                        if(!Float.isFinite(baseline)||baseline<0f||baseline>1f)throw new IOException("Invalid display brightness: "+baseline);
                        main.post(()->{
                        synchronized(session){
                            if(current!=session||session.stop||session.suspended)return;
                            window.pin(baseline);
                            try{session.process.getOutputStream().write("resume\n".getBytes(StandardCharsets.UTF_8));session.process.getOutputStream().flush();}
                            catch(IOException failure){Log.e(TAG,"Cannot start brightness hold",failure);stop();}
                        }
                    });}
                    if(line.equals("Brightness suspended"))main.post(()->{if(current==session&&session.suspended)window.release();});Log.i(TAG,line);if(output.length()<2048)output.append(line).append('\n');}
            }
            int code=process.waitFor();
            if(code!=0&&!tooHot)throw new IOException("root brightness exit "+code+": "+output);
        }catch(IOException|InterruptedException failure){error=failure;if(failure instanceof InterruptedException)Thread.currentThread().interrupt();}
        finally{
            synchronized(session){if(session.process!=null)try{session.process.getOutputStream().close();}catch(IOException failure){Log.e(TAG,"Brightness pipe close failed",failure);}}
        }
        final Throwable failure=error;
        final boolean overTemperature=tooHot;
        main.post(()->{
            if(current==session){window.release();current=null;}
            changed.run();
            if(overTemperature)hot();
            if(failure!=null){Log.e(TAG,"Brightness operation failed",failure);toast.accept("亮度提升失败，请检查 Root 授权与温度读取");}
        });
    }
}
