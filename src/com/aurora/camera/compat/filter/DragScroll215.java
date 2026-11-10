package com.aurora.camera.compat.filter;
/** Signed edge velocity shared by row and list drag callbacks. */
public final class DragScroll215 {
    public interface Frames {void post(Runnable r);void remove(Runnable r);void scroll(int pixels);boolean attached();}
    private final int edge,speed;private final Frames frames;private int delta;private boolean posted;
    public DragScroll215(int edge,int speed,Frames frames){this.edge=edge;this.speed=speed;this.frames=frames;}
    private final Runnable tick=this::tick;
    private void tick(){posted=false;if(delta==0||!frames.attached())return;frames.scroll(delta);posted=true;frames.post(tick);}
    public void location(float y,int height){delta=step(y,height,edge,speed);if(delta==0){stop();return;}if(!posted){posted=true;frames.post(tick);}}
    public void stop(){delta=0;posted=false;frames.remove(tick);}
    public static int step(float y,int height,int edge,int speed){
        if(height<=0||edge<=0)return 0;
        int band=Math.min(edge,height/2);
        if(band==0)return 0;
        if(y<band)return -Math.max(1,Math.round(speed*Math.min(1f,(band-y)/band)));
        if(y>height-band)return Math.max(1,Math.round(speed*Math.min(1f,(y-height+band)/band)));
        return 0;
    }
}
