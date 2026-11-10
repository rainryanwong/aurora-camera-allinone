package com.aurora.camera.compat.cinematic;

/** Accepted automatic-dolly start/stop policy, using the existing native UI protocol. */
public final class DollyRepair {
    public interface Runtime {
        String choice();
        Object zoomController();
        String range(int mode);
        String speed(int mode);
        void start(Object controller,float from,float to,float speed,boolean stopShutter);
        void stopAnimator();
        void recordingPreference(boolean value);
        void invalidNumber(NumberFormatException failure);
        boolean busy();
        Object bottomProtocol();
        void openPanel(Object protocol);
    }
    private final Runtime runtime;
    public DollyRepair(Runtime runtime){this.runtime=runtime;}
    public boolean selected(){return "2".equals(runtime.choice());}
    public void start(int mode){
        if(mode!=227||!selected())return;
        Object controller=runtime.zoomController();
        if(controller==null)return;
        String[] range=runtime.range(mode).split(":");
        if(range.length!=2)return;
        try{
            float from=Float.parseFloat(range[0]),to=Float.parseFloat(range[1]);
            float speed=Float.parseFloat(runtime.speed(mode));
            runtime.start(controller,from,to,speed,false);
        }catch(NumberFormatException failure){runtime.invalidNumber(failure);}
    }
    public void stop(){runtime.stopAnimator();runtime.recordingPreference(false);}
    public void click(){
        if(runtime.busy())return;
        Object protocol=runtime.bottomProtocol();
        if(protocol!=null)runtime.openPanel(protocol);
    }
}
