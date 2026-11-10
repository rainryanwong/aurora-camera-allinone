package com.aurora.camera.compat;
import com.aurora.camera.compat.filter.FilterManager215;
import com.aurora.camera.compat.filter.LutRuntime229;
public final class FilterManager211Hooks {
    public static HookPlan resolve(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        FilterManager215 manager=new FilterManager215(host,runtime);
        return new HookPlan(){public String id(){return "215.camstyle-filter-management";}public void install(){manager.install();}};
    }
}
