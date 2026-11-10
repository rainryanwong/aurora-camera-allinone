package com.aurora.camera.compat.editor;

import android.os.Build;
import android.util.Log;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/** Extend the device lists for palette, photo-agent, image-to-video, deblur and ID photo; their other eligibility checks remain native. */
public final class EditorFeatureDevices293 {
    private final Field palette;
    private final Field[] features;

    public EditorFeatureDevices293(Host host) throws ReflectiveOperationException {
        palette=host.field("bt.a","d");
        features=new Field[]{host.field("at.a","a"),host.field("at.b","a"),
                host.field("at.k","a"),host.field("at.k","b"),host.field("at.j","a")};
        if(palette.getType()!=String[].class)throw new NoSuchFieldException("Editor palette device list differs");
        for(Field field:features)if(field.getType()!=Set.class)
            throw new NoSuchFieldException("Editor AI device list differs: "+field);
    }

    public void install() throws IllegalAccessException {
        Set<String> devices=new LinkedHashSet<>(Arrays.asList((String[])palette.get(null)));
        devices.add(Build.DEVICE);
        palette.set(null,devices.toArray(new String[0]));
        for(Field field:features) {
            @SuppressWarnings("unchecked") Set<String> original=(Set<String>)field.get(null);
            Set<String> allowed=new LinkedHashSet<>(original);
            allowed.add(Build.DEVICE);
            field.set(null,allowed);
        }
        Log.i("AuroraEditor245","feature device lists extended for "+Build.DEVICE
                +": filter palette, photo agent, image-to-video, motion deblur, AI ID photo");
    }
}
