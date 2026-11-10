package com.aurora.camera.compat;
import com.aurora.camera.compat.filter.LutRuntime229;
import com.aurora.camera.compat.filter.LutPage229;
import com.aurora.camera.compat.filter.ImportedVideoLut256;
public final class Lut229Hooks {
    public static HookPlan resolve(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        ImportedVideoLut256 video=new ImportedVideoLut256(host,runtime);
        runtime.attachVideo(video);
        Laboratory253Hooks laboratory=new Laboratory253Hooks(host,runtime);
        LutPage229 page=new LutPage229(host,runtime,laboratory::openImport);
        com.aurora.camera.compat.filter.FilterStrength270 strength=new com.aurora.camera.compat.filter.FilterStrength270(host);
        return new HookPlan(){public String id(){return "229.lut-import-and-effects";}public void install(){try{runtime.install();video.install();page.install();laboratory.install();strength.install();}catch(ReflectiveOperationException e){throw new IllegalStateException("LUT runtime contract initialization failed",e);}}};
    }
}
