package com.aurora.camera.compat.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.SeekBar;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;

/** The host's own day/night MIUIX slider, rather than a tinted framework slider. */
public final class MiuixSeekBar241 {
    private final Constructor<?> constructor;
    public MiuixSeekBar241(Host host)throws ReflectiveOperationException{
        Class<?> type=host.type("miuix.androidbasewidget.widget.SeekBar");
        if(!SeekBar.class.isAssignableFrom(type))throw new NoSuchMethodException("Host MIUIX slider must extend SeekBar");
        constructor=type.getDeclaredConstructor(Context.class,AttributeSet.class);
        constructor.setAccessible(true);
    }
    public SeekBar create(Context activity){
        try{return(SeekBar)constructor.newInstance(activity,null);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Host MIUIX slider creation failed",failure);}
    }
}
