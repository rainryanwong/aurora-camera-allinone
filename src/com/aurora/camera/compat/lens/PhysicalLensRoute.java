package com.aurora.camera.compat.lens;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Accepted physical-role policy. Camera IDs always come from the live role provider. */
public final class PhysicalLensRoute {
    public interface Camera {
        int standalone() throws ReflectiveOperationException;
        int tele() throws ReflectiveOperationException;
        int wide() throws ReflectiveOperationException;
        int ultra() throws ReflectiveOperationException;
        float teleZoom() throws ReflectiveOperationException;
        boolean photoTarget(int mode) throws ReflectiveOperationException;
        float selectedZoom(int mode) throws ReflectiveOperationException;
        boolean rear(Object module) throws ReflectiveOperationException;
        int mode(Object module) throws ReflectiveOperationException;
        int actual(Object module) throws ReflectiveOperationException;
        void reopen(Object module,int mode,float from,float to,int actual,int target)
                throws ReflectiveOperationException;
    }

    private final Camera camera;
    public PhysicalLensRoute(Camera camera) { this.camera=camera; }

    public boolean applies(int mode) throws ReflectiveOperationException {
        return mode==175 || mode==169 || mode==163 && camera.photoTarget(mode);
    }

    public int lens(int mode,float zoom) throws ReflectiveOperationException {
        if (!applies(mode) || Float.isNaN(zoom)) return -1;
        if (zoom>=5.0f) return camera.standalone();
        if (zoom>=camera.teleZoom()) return camera.tele();
        if (zoom>=1.0f) return camera.wide();
        return camera.ultra();
    }

    public int capture(int logicalCamera,int mode) throws ReflectiveOperationException {
        return logicalCamera==0 ? lens(mode,camera.selectedZoom(mode)) : -1;
    }

    public boolean rearTarget(Object module) throws ReflectiveOperationException {
        return module!=null && camera.rear(module) && applies(camera.mode(module));
    }

    public List<Float> bounds(Object module,List<Float> original) throws ReflectiveOperationException {
        if (!rearTarget(module) || original==null || camera.standalone()<0) return original;
        TreeSet<Float> result=new TreeSet<>(original);
        result.add(5.0f);
        return new ArrayList<>(result);
    }

    /** -1: original method continues; 0: role already matches; 1: native reopen requested. */
    public int switchLens(float from,float to,Object module) throws ReflectiveOperationException {
        if (!rearTarget(module) || from<5.0f && to<5.0f) return -1;
        int mode=camera.mode(module), target=lens(mode,to);
        if (target<0) return -1;
        int actual=camera.actual(module);
        if (target==actual) return 0;
        camera.reopen(module,mode,from,to,actual,target);
        return 1;
    }
}
