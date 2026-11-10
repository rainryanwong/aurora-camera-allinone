package com.aurora.camera.compat.video;

import com.aurora.camera.compat.Host;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Exact stock-1770 UI boundaries. No camera data, preference or layout writes. */
public final class VideoWatermarkUiPort {
    public final Method topItems,menuItems,editorItems,editorVisibility,editorAuxiliary,videoSetting;
    private final Field itemId,currentMode,editorBar,editorMenu;
    private final Map<Object,Integer> editorModes=Collections.synchronizedMap(new WeakHashMap<Object,Integer>());
    private final ThreadLocal<EditorScope> editorScope=new ThreadLocal<>();

    public static final class EditorScope {
        final Object owner;
        final int mode;
        final EditorScope previous;
        EditorScope(Object owner,int mode,EditorScope previous){this.owner=owner;this.mode=mode;this.previous=previous;}
    }

    public VideoWatermarkUiPort(Host host)throws ReflectiveOperationException {
        Class<?> adapter=host.type("Y9.A"),holder=host.type("Y9.T");
        topItems=host.method("X9.i0","s0",int.class,Optional.class,boolean.class);
        menuItems=host.method("p5.W","zs");
        editorItems=host.method("Y9.q","ns",ArrayList.class);
        editorVisibility=host.method(adapter,"u",holder,String.class);
        editorAuxiliary=host.method(adapter,"v",int.class,String.class);
        videoSetting=host.method("com.android.camera.fragment.settings.CameraCamcorderPreferenceFragment","addWatermarkPreference");
        itemId=host.field("b5.i","c");
        currentMode=host.field("com.android.camera.fragment.h","mCurrentMode");
        editorBar=host.field("Y9.q","g");editorMenu=host.field("Y9.q","h");
        if(topItems.getReturnType()!=List.class||menuItems.getReturnType()!=List.class
                ||editorItems.getReturnType()!=void.class||editorVisibility.getReturnType()!=void.class
                ||editorAuxiliary.getReturnType()!=void.class||videoSetting.getReturnType()!=void.class
                ||itemId.getType()!=int.class||currentMode.getType()!=int.class
                ||editorBar.getType()!=adapter||editorMenu.getType()!=adapter
                ||!currentMode.getDeclaringClass().isAssignableFrom(editorItems.getDeclaringClass())
                ||!currentMode.getDeclaringClass().isAssignableFrom(menuItems.getDeclaringClass()))
            throw new NoSuchMethodException("Video watermark final UI/owner contract");
    }

    private static boolean hiddenMode(int mode){return mode==162||mode==180;}

    /** The source list was already built from defaults or the complete saved layout. */
    public Object visibleItems(Object value,int mode)throws IllegalAccessException {
        if(!hiddenMode(mode)||value==null)return value;
        List<?> items=(List<?>)value;
        ArrayList<Object> result=null;
        for(int n=0;n<items.size();n++){
            Object item=items.get(n);
            if(itemId.getInt(item)==223){
                if(result==null){result=new ArrayList<>(items.size()-1);result.addAll(items.subList(0,n));}
            }else if(result!=null)result.add(item);
        }
        return result==null?value:result;
    }

    public int fragmentMode(Object fragment)throws IllegalAccessException {return currentMode.getInt(fragment);}

    public EditorScope enterEditor(Object owner)throws IllegalAccessException {
        EditorScope next=new EditorScope(owner,fragmentMode(owner),editorScope.get());
        editorScope.set(next);return next;
    }

    public void leaveEditor(EditorScope scope,boolean successful)throws IllegalAccessException {
        try{
            if(successful){
                Object bar=editorBar.get(scope.owner),menu=editorMenu.get(scope.owner);
                if(bar!=null)editorModes.put(bar,scope.mode);
                if(menu!=null)editorModes.put(menu,scope.mode);
            }
        }finally{
            if(scope.previous==null)editorScope.remove();else editorScope.set(scope.previous);
        }
    }

    public boolean hideEditorItem(Object adapter,Object token)throws IllegalAccessException {
        if(!"223".equals(token))return false;
        // During ns(), setAdapter can bind synchronously before its after hook.
        // Match the concrete owner's fields before using a pending nested scope.
        for(EditorScope scope=editorScope.get();scope!=null;scope=scope.previous){
            if(editorBar.get(scope.owner)==adapter||editorMenu.get(scope.owner)==adapter)
                return hiddenMode(scope.mode);
        }
        Integer mode=editorModes.get(adapter);
        return mode!=null&&hiddenMode(mode);
    }
}
