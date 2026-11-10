package com.aurora.camera.compat.cinematic;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.HashMap;

/** Module-owned SmartDollyASD state attached only to one registered stock s6.D. */
public final class SmartDollyAsd {
    public interface Runtime {
        Object module(Object interceptor);
        Rect sensor(Object interceptor);
        int[] previewSize(Object module);
        Object registerResult(Object interceptor);
        Float[] values(Object holder);
        float cropRatio(Rect sensor,Object result);
        int orientation(Object module);
        void sensorMatrix(Matrix matrix,Rect sensor,float ratio);
        void displayMatrix(Matrix matrix,int orientation,int width,int height,int sensorWidth,int sensorHeight);
        Object subject(Rect rect,int type,int index,int status);
        void previous(Object next,Object previous);
        int previousStatus(Object previous);
        void publish(ArrayList<Object> subjects);
        boolean recording(Object module);
        boolean audioTracking(Object module);
        void track(Object module,Rect rect,float audioRatio);
        boolean readPreference(String key,boolean fallback);
        void writePreference(String key,boolean value);
        Object hintReceiver();
        void hint(Object receiver,int visibility,int stringId,String key,long duration);
        void log(String message);
    }
    private final Runtime runtime;
    private final SmartDolly smart;
    private final RectF rectangle=new RectF();
    private final Matrix sensorMatrix=new Matrix();
    private final Matrix displayMatrix=new Matrix();
    private final HashMap<Integer,Object> subjects=new HashMap<>();
    private Object holder;
    private Rect sensor;
    private float ratio;
    private int previewWidth;
    private boolean pendingFaceHint;

    public SmartDollyAsd(Runtime runtime,SmartDolly smart){this.runtime=runtime;this.smart=smart;}
    public void declare(Object interceptor){holder=runtime.registerResult(interceptor);}
    public boolean initialize(Object interceptor){
        sensor=runtime.sensor(interceptor);
        previewWidth=runtime.previewSize(runtime.module(interceptor))[0];
        return true;
    }
    public void parse(Object result){ratio=runtime.cropRatio(sensor,result);}

    public void consume(Object interceptor){
        Object module=runtime.module(interceptor);
        if(!smart.active(module))return;
        Float[] values=runtime.values(holder);
        if(values==null || values.length<5)return;
        for(int index=0;index<5;index++)if(values[index]==null)return;
        ArrayList<Object> current=new ArrayList<>();
        Rect mapped=new Rect();
        Rect origin=new Rect(Math.round(values[0]),Math.round(values[1]),
                Math.round(values[0]+values[2]),Math.round(values[1]+values[3]));
        int status=Math.round(values[4]);
        runtime.log("originRect: "+origin);
        sensorMatrix.reset();displayMatrix.reset();
        int orientation=runtime.orientation(module);
        int[] preview=runtime.previewSize(module);
        runtime.sensorMatrix(sensorMatrix,sensor,ratio);
        runtime.displayMatrix(displayMatrix,orientation,preview[0],preview[1],sensor.width(),sensor.height());
        rectangle.set(origin);
        sensorMatrix.mapRect(rectangle);displayMatrix.mapRect(rectangle);
        mapped.set((int)rectangle.left,(int)rectangle.top,(int)rectangle.right,(int)rectangle.bottom);
        Object previous=subjects.get(0);
        if(previous==null)previous=runtime.subject(mapped,4,1,status);
        Object next=runtime.subject(mapped,4,1,status);
        runtime.previous(next,previous);
        current.add(next);subjects.put(0,next);
        // Preserve the accepted helper's c2.e.e previous-history field, rather
        // than guessing that c2.e.b is the intended lastStatus consumer.
        int lastStatus=runtime.previousStatus(previous);
        runtime.log("rect: "+mapped+" status: "+status+" lastStatus: "+lastStatus);
        runtime.publish(current);
        if(runtime.recording(module) && runtime.audioTracking(module)){
            float audioRatio=previewWidth*1.0f/mapped.width();
            runtime.log("getTrackResult rect = "+mapped+" audioZoomRatio = "+audioRatio);
            runtime.track(module,mapped,audioRatio);
        }
        boolean inAnime=(status&0x40)>0,lastInAnime=(lastStatus&0x40)>0;
        runtime.log("isInAnime = "+inAnime+" lastIsInAnime = "+lastInAnime);
        if(inAnime!=lastInAnime)runtime.writePreference("pref_cinematic_intell_dolly_in_anime",inAnime);
        boolean switched=runtime.readPreference("pref_cinematic_intell_dolly_switch_face_tips",false);
        boolean showNoBokeh=runtime.readPreference("pref_cinematic_intell_dolly_no_bokeh_tips",false);
        Object hint=runtime.hintReceiver();
        boolean detected=(status&8)>0;
        if(!pendingFaceHint)pendingFaceHint=!switched&&detected;
        if(hint!=null && showNoBokeh){
            runtime.hint(hint,detected?8:0,0x7f1404c3,"cinematic_intell_dolly_not_detect_desc",-1L);
            if(pendingFaceHint){
                pendingFaceHint=false;
                runtime.hint(hint,0,0x7f1404a6,"cinematic_intell_dolly_switch_face_desc",3000L);
                runtime.writePreference("pref_cinematic_intell_dolly_switch_face_tips",true);
            }
        }
    }
}
