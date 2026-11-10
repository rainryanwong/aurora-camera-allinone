package com.aurora.camera.compat.gripmenu;

import java.util.ArrayList;
import java.util.List;

/** Exact once-per-key upgrade of already saved menus, before original defaults. */
public final class MenuMigration {
    public interface Runtime {
        int camera(Object component); String key(Object component,int mode);
        boolean marked(Object component,String key); List<Integer> saved(Object component,int mode);
        void save(Object component,ArrayList<Integer> values); void mark(Object component,String key);
    }
    private final Runtime runtime;
    public MenuMigration(Runtime runtime){this.runtime=runtime;}
    public void migrate(Object component,int mode,ArrayList<Integer> available){
        if(mode==173){
            available.removeIf(value->value==212);
            String key=runtime.key(component,mode)+"_aurora_night_no_filter_shortcut_v1";
            if(runtime.marked(component,key))return;
            List<Integer> saved=runtime.saved(component,mode);if(saved.isEmpty())return;
            ArrayList<Integer> values=new ArrayList<>(saved);
            boolean changed=values.removeIf(value->value==212);
            if(available.contains(0xd40)&&!values.contains(0xd40)){values.add(0xd40);changed=true;}
            if(changed)runtime.save(component,values);
            runtime.mark(component,key);return;
        }
        int[] required;
        switch(mode){case 163:required=new int[]{0xc9,0xe4,0x93};break;
            case 162:required=new int[]{0xa5};break;case 225:required=new int[]{0xef};break;
            default:required=null;}
        if(required!=null)upgrade(component,mode,available,required,"_aurora_features_r6");
        if(mode==256&&runtime.camera(component)==0&&available.contains(0xd40))
            upgrade(component,mode,available,new int[]{0xd40},"_aurora_aperture_v1");
    }
    private void upgrade(Object component,int mode,ArrayList<Integer> available,int[] required,String suffix){
        String key=runtime.key(component,mode)+suffix;if(runtime.marked(component,key))return;
        List<Integer> saved=runtime.saved(component,mode);if(saved.isEmpty())return;
        ArrayList<Integer> values=new ArrayList<>(saved);boolean changed=false;
        for(int id:required)if(available.contains(id)&&!values.contains(id)){
            int legacy=id==0xa5?values.indexOf(0xda):-1;
            if(legacy>=0)values.set(legacy,id);else values.add(id);changed=true;
        }
        if(changed)runtime.save(component,values);
        runtime.mark(component,key);
    }
}
