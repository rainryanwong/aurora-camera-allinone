package com.aurora.camera.compat.filter;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.Options229;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Imported PNG LUTs in the stock video component and HAL filter protocol. */
public final class ImportedVideoLut256 {
    private static final int VALUE_BASE=0x400000fd, SLOT_MASK=0x1ff00;
    private final LutRuntime229 runtime;
    private final Options229 options;
    private final Class<?> videoType,configType,cinematicType,cinematicConfigType,strengthType,strengthConfigType;
    private final Method init,cloudInit,nativeCatalog,configMode,configRepository,runningRepository,repositoryItem;
    private final Method getValue,setValue,defaultValue,mapperGet,map,logEnabled,videoId,degree,degreeConfigMode,cameraRepository,cameraConfig,strengthValue,workspaceValue,switchOn;
    private final NativeVideoLutWriter256 writer;
    private final Method panelInit,panelDestroy,panelIndex,notifyAdapter;
    private final Field currentMode,items,capabilities,mapperProvider,infoName,infoIcon,infoVideoId,itemValue,itemName,itemLabel;
    private final Field panelMode,panelComponent,panelRows,panelAdapter,panelList,panelSelected,panelClick,panelPending,adapterRows,listRows,listIndex;
    private final Constructor<?> infoConstructor;
    private final List<WeakReference<Object>> loaded=new ArrayList<>();
    private final Map<Object,List<Object>> sources=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private final Field itemData,cloudName;
    private final Constructor<?> managementComponent;
    private final ThreadLocal<Boolean> managementLoading=new ThreadLocal<>();
    private final ThreadLocal<Integer> rebuilding=ThreadLocal.withInitial(()->0);
    private int activeLut,activeNativeSlot=254;
    private String activeRevision;
    private volatile WeakReference<Object> visiblePanel=new WeakReference<>(null);

    public ImportedVideoLut256(Host h,LutRuntime229 runtime)throws ReflectiveOperationException{
        this.runtime=runtime;options=new Options229(h);
        videoType=h.type("v2.e0");configType=h.type("r2.F");cinematicType=h.type("v2.u");cinematicConfigType=h.type("r2.l");
        strengthType=h.type("v2.W");strengthConfigType=h.type("r2.u");
        init=h.method("v2.e0","e",int.class);cloudInit=h.method("v2.e0","d",int.class,Map.class);
        nativeCatalog=h.method("v2.e0","o",int.class);configMode=h.method("r2.F","q",int.class);
        configRepository=h.method("g2.a","a");runningRepository=h.method("g2.a","j");repositoryItem=h.method("Xh.b","x",Class.class);
        getValue=h.method("com.android.camera.data.data.c","getComponentValue",int.class);
        setValue=h.method("com.android.camera.data.data.c","setComponentValue",int.class,String.class);
        defaultValue=h.method("v2.e0","getDefaultValue",int.class);
        items=h.field("com.android.camera.data.data.c","mItems");capabilities=h.field("com.android.camera.data.data.c","mCapabilities");
        currentMode=h.field("com.android.camera.module.c0","a");
        mapperProvider=h.field("i2.a","a");mapperGet=h.method("i2.b","f");map=h.method("j2.d","j",int.class,ArrayList.class,h.type("l9.g"));
        infoName=h.field("i3.b","c");infoIcon=h.field("i3.b","e");infoVideoId=h.field("i3.b","n");
        infoConstructor=h.type("i3.b").getDeclaredConstructor(int.class,int.class,int.class,int.class);infoConstructor.setAccessible(true);
        itemValue=h.field("com.android.camera.data.data.d","r");itemName=h.field("com.android.camera.data.data.d","l");itemLabel=h.field("com.android.camera.data.data.d","o");
        itemData=h.field("com.android.camera.data.data.d","a");cloudName=h.field("com.android.camera.data.data.b","g");
        managementComponent=h.type("r2.F").getDeclaredConstructor(h.type("r2.n1"));managementComponent.setAccessible(true);
        logEnabled=h.method("com.android.camera.data.data.x","m0",int.class);
        videoId=h.method("com.android.camera.data.data.i","a0");degree=h.method("com.android.camera.data.data.i","Z",int.class,int.class);
        degreeConfigMode=h.method("r2.u","p",int.class);cameraRepository=h.method("g2.a","i");cameraConfig=h.method("A2.a$a","b",int.class);
        strengthValue=h.method("v2.W","m",int.class,int.class);
        workspaceValue=h.method("r2.a","checkValueValidByWorkspace",int.class,String.class);
        switchOn=h.method("v2.e0","isSwitchOn",int.class);writer=new NativeVideoLutWriter256(h);
        panelInit=h.method("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","c",h.type("com.android.camera.data.data.c"),int.class,h.type("P9.i"));
        panelDestroy=h.method("P9.j","onDestroyView");panelIndex=h.method("P9.i","Qs");notifyAdapter=h.method("androidx.recyclerview.widget.RecyclerView$g","notifyDataSetChanged");
        panelMode=h.field("com.android.camera.fragment.h","mCurrentMode");panelComponent=h.field("P9.i","P");panelRows=h.field("P9.i","R");
        panelAdapter=h.field("P9.i","O");panelList=h.field("P9.i","M");panelSelected=h.field("P9.i","V");panelClick=h.field("P9.i","c0");panelPending=h.field("P9.j","e0");
        adapterRows=h.field("com.android.camera.fragment.d","f");listRows=h.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","a");listIndex=h.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","b");
    }

    public static boolean supportsMode(int mode){return mode==162||mode==180;}
    public static int value(int slot){if(slot<1||slot>LutStore229.SLOTS)throw new IllegalArgumentException("LUT slot "+slot);return VALUE_BASE|((slot-1)<<8);}
    public static int slot(int value){return (value&~SLOT_MASK)==VALUE_BASE?((value>>>8)&511)+1:0;}
    private int mode()throws IllegalAccessException{return currentMode.getInt(null);}
    private boolean eligible(int mode)throws ReflectiveOperationException{return supportsMode(mode)&&!(Boolean)logEnabled.invoke(null,mode);}
    private Object component(int mode,boolean cinematic)throws ReflectiveOperationException{
        boolean config=(Boolean)configMode.invoke(null,mode);
        Object repository=(config?configRepository:runningRepository).invoke(null);
        return repositoryItem.invoke(repository,cinematic?(config?cinematicConfigType:cinematicType):(config?configType:videoType));
    }
    private int selection(int mode)throws ReflectiveOperationException{return Integer.parseInt((String)getValue.invoke(component(mode,false),mode));}
    private int selectedValue(int mode)throws ReflectiveOperationException{
        if(!eligible(mode))return 0;
        // Cinematic selection takes precedence in the stock a0() reader.
        int cinematic=Integer.parseInt((String)getValue.invoke(component(mode,true),mode));
        return (cinematic>>8)==18?0:selection(mode);
    }
    private List<Object> withLooks(List<?> source,Object owner,int mode)throws ReflectiveOperationException{
        ArrayList<Object> result=new ArrayList<>(source),missing=new ArrayList<>();
        Map<Integer,Object> present=new java.util.HashMap<>();for(Object row:result)present.put(Integer.parseInt((String)itemValue.get(row)),row);
        for(Object info:(List<?>)nativeCatalog.invoke(null,mode)){
            int value=infoVideoId.getInt(info);if(LeicaLooks273.videoRank(value)==Integer.MAX_VALUE)continue;
            Object row=present.get(value);if(row==null)missing.add(info);else{
                itemName.setInt(row,infoName.getInt(info));
                de.robv.android.xposed.XposedHelpers.setIntField(row,"c",infoIcon.getInt(info));
                de.robv.android.xposed.XposedHelpers.setIntField(row,"g",infoIcon.getInt(info));
            }
        }
        if(!missing.isEmpty())result.addAll((List<?>)map.invoke(mapperGet.invoke(mapperProvider.get(null)),mode,missing,capabilities.get(owner)));
        return result;
    }

    @SuppressWarnings("unchecked") private void project(Object owner,int mode)throws ReflectiveOperationException{
        if(!supportsMode(mode))return;
        List<?> current=sources.get(owner);if(current==null)current=(List<?>)items.get(owner);if(current==null)return;
        current=withLooks(current,owner,mode);
        LutStore229 store=runtime.store();Set<Integer> enabled=new HashSet<>();if(eligible(mode)){Set<String> hidden=new VideoFilterWorkspace270(runtime,options.context()).hidden();for(int slot:store.configuredSlots())if(FilterWorkspace215.shown(hidden,LutStore229.token(slot),store.enabled(slot)))enabled.add(slot);}
        ArrayList<Object> next=new ArrayList<>();Set<Integer> present=new HashSet<>();
        for(Object row:current){
            int id=Integer.parseInt((String)itemValue.get(row)),slot=slot(id);
            if(slot==0)next.add(row);
            else if(enabled.contains(slot)){
                itemName.setInt(row,-1);itemLabel.set(row,store.label(LutStore229.token(slot),store.name(slot)));next.add(row);present.add(slot);
            }
        }
        ArrayList<Object> missing=new ArrayList<>();
        if(!present.containsAll(enabled)){
            List<?> catalog=(List<?>)nativeCatalog.invoke(null,mode);
            // e0.p() returns an empty list while the native catalog protocol is absent.
            if(catalog.isEmpty())return;
            Object template=catalog.get(0);int order=next.size();
            for(int slot:store.configuredSlots())if(!present.contains(slot)&&enabled.contains(slot)){
                Object row=infoConstructor.newInstance(runtime.id(slot),infoName.getInt(template),infoIcon.getInt(template),order++);
                // The video mapper reads n; a() is the still-render identifier.
                infoVideoId.setInt(row,value(slot));missing.add(row);
            }
        }
        if(!missing.isEmpty()){
            Object mapper=mapperGet.invoke(mapperProvider.get(null));
            List<?> mapped=(List<?>)map.invoke(mapper,mode,missing,capabilities.get(owner));
            for(Object row:mapped){int slot=slot(Integer.parseInt((String)itemValue.get(row)));itemName.setInt(row,-1);itemLabel.set(row,store.label(LutStore229.token(slot),store.name(slot)));next.add(row);}
        }
        VideoFilterWorkspace270 workspace=new VideoFilterWorkspace270(runtime,options.context());
        Set<String> hidden=workspace.hidden();String original=(String)defaultValue.invoke(owner,mode);
        List<FilterWorkspace215.Entry> ordered=new ArrayList<>();Map<String,Object> byId=new java.util.HashMap<>();
        for(Object row:next){int value=Integer.parseInt((String)itemValue.get(row));String id=VideoFilterWorkspace270.identity(value);byId.put(id,row);ordered.add(new FilterWorkspace215.Entry(value,id,"",original.equals(String.valueOf(value)),true));}
        try{workspace.arrange(ordered,false);}catch(IOException failure){throw new IllegalStateException("Cannot read video filter order",failure);}
        next.clear();for(FilterWorkspace215.Entry entry:ordered)if(entry.fixed||!hidden.contains(entry.id))next.add(byId.get(entry.id));
        items.set(owner,next);
        int selected=Integer.parseInt((String)getValue.invoke(owner,mode));
        if(slot(selected)>0&&!enabled.contains(slot(selected))||hidden.contains(VideoFilterWorkspace270.identity(selected)))setValue.invoke(owner,mode,defaultValue.invoke(owner,mode));
    }

    public List<FilterWorkspace215.Entry> managementEntries(android.content.Context context)throws ReflectiveOperationException{
        Object owner=component(162,false);String original=(String)defaultValue.invoke(owner,162);
        List<?> nativeRows=sources.get(owner);
        if(nativeRows==null||nativeRows.isEmpty()){
            List<?> current=(List<?>)items.get(owner);
            if(current!=null&&!current.isEmpty())nativeRows=current;
            else{
                // Native e/d only replace this detached component's mItems;
                // preserve cloud filtering without rebinding the active camera.
                Object detached=managementComponent.newInstance(configRepository.invoke(null));
                capabilities.set(detached,capabilities.get(owner));managementLoading.set(true);
                try{init.invoke(detached,162);nativeRows=(List<?>)items.get(detached);}finally{managementLoading.remove();}
            }
        }
        nativeRows=withLooks(nativeRows,owner,162);
        java.util.LinkedHashMap<String,FilterWorkspace215.Entry> rows=new java.util.LinkedHashMap<>();
        int originalValue=Integer.parseInt(original);String originalId=VideoFilterWorkspace270.identity(originalValue);
        rows.put(originalId,new FilterWorkspace215.Entry(originalValue,originalId,"原图",true,true));
        for(Object row:nativeRows){int value=Integer.parseInt((String)itemValue.get(row));if(slot(value)>0)continue;
            int resource=itemName.getInt(row);String name=resource>0?context.getString(resource):(String)itemLabel.get(row);
            if(name==null){Object cloud=itemData.get(row);if(cloud!=null)name=(String)cloudName.get(cloud);}
            if(value==originalValue)name="原图";
            if(name==null)throw new IllegalStateException("Video filter label missing: "+value);
            String id=VideoFilterWorkspace270.identity(value);rows.put(id,new FilterWorkspace215.Entry(value,id,name,value==originalValue,true));
        }
        return new ArrayList<>(rows.values());
    }

    private boolean videoEnabled(int slot){return FilterWorkspace215.shown(new VideoFilterWorkspace270(runtime,options.context()).hidden(),LutStore229.token(slot),runtime.store().enabled(slot));}
    private void remember(Object owner){
        synchronized(loaded){for(Iterator<WeakReference<Object>> it=loaded.iterator();it.hasNext();){Object old=it.next().get();if(old==null)it.remove();else if(old==owner)return;}loaded.add(new WeakReference<>(owner));}
    }
    public void refreshLoaded()throws ReflectiveOperationException{
        int mode=mode();if(!supportsMode(mode))return;
        Object current=component(mode,false);
        synchronized(loaded){for(Iterator<WeakReference<Object>> it=loaded.iterator();it.hasNext();){Object owner=it.next().get();if(owner==null)it.remove();else if(owner==current)project(owner,mode);}}
        synchronizePanel(current,mode);
    }

    private void synchronizePanel(Object owner,int mode)throws ReflectiveOperationException{
        Object panel=visiblePanel.get();if(panel==null||mode()!=mode||panelMode.getInt(panel)!=mode||panelComponent.get(panel)!=owner)return;
        ArrayList<?> rows=new ArrayList<>((List<?>)items.get(owner));panelRows.set(panel,rows);
        int index=(Integer)panelIndex.invoke(panel);panelSelected.setInt(panel,index);((AtomicInteger)panelClick.get(panel)).set(index);panelPending.set(panel,null);
        Object list=panelList.get(panel);if(list!=null){listRows.set(list,rows);listIndex.setInt(list,index);}
        Object adapter=panelAdapter.get(panel);if(adapter!=null){adapterRows.set(adapter,rows);notifyAdapter.invoke(adapter);}
    }

    private synchronized int publish(int slot)throws ReflectiveOperationException,IOException{
        LutStore229 store=runtime.store();String revision=store.resourceToken(slot);
        if(activeLut==slot&&revision.equals(activeRevision))return activeNativeSlot;
        int target=activeNativeSlot==253?254:253;
        byte[] png=Files.readAllBytes(store.file(slot).toPath());
        writer.write("/data/vendor/camera/",target+".png",png);
        activeLut=slot;activeRevision=revision;activeNativeSlot=target;
        Log.i("AuroraVideoLut","published lut="+slot+" vendorId="+target+" revision="+revision);
        return target;
    }
    private synchronized int publishLook(int ordinal)throws ReflectiveOperationException,IOException{
        String resource=LeicaLooks273.resource(ordinal),revision="native:"+resource;
        if(activeLut==0&&revision.equals(activeRevision))return activeNativeSlot;
        int id=options.context().getResources().getIdentifier(resource,"raw",options.context().getPackageName());
        if(id==0)throw new IOException("Leica Look resource missing: "+resource);
        byte[] png;try(java.io.InputStream input=options.context().getResources().openRawResource(id);java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1)output.write(buffer,0,count);png=output.toByteArray();
        }
        int target=activeNativeSlot==253?254:253;writer.write("/data/vendor/camera/",target+".png",png);
        activeLut=0;activeRevision=revision;activeNativeSlot=target;
        Log.i("AuroraVideoLut","published look="+resource+" vendorId="+target);return target;
    }
    private void publicationFailed(int mode,int slot,IOException failure)throws ReflectiveOperationException{
        Object owner=component(mode,false);setValue.invoke(owner,mode,defaultValue.invoke(owner,mode));
        Log.e("AuroraVideoLut","Cannot publish imported video LUT slot="+slot,failure);
        new Handler(Looper.getMainLooper()).post(()->{
            try{synchronizePanel(owner,mode);}catch(ReflectiveOperationException invalid){throw new IllegalStateException("Cannot restore video filter selection",invalid);}
            Toast.makeText(options.context(),"录像滤镜应用失败",Toast.LENGTH_LONG).show();
        });
    }
    public void install()throws ReflectiveOperationException{
        StrictHook rebuild=new StrictHook(){
          @Override protected void before(MethodHookParam p){rebuilding.set(rebuilding.get()+1);}
          @Override protected void after(MethodHookParam p)throws Throwable{
            int depth=rebuilding.get()-1;if(depth==0)rebuilding.remove();else rebuilding.set(depth);
            if(depth>0||p.hasThrowable()||Boolean.TRUE.equals(managementLoading.get())||options.context()==null||!supportsMode((Integer)p.args[0]))return;
            @SuppressWarnings("unchecked") List<Object> source=(List<Object>)items.get(p.thisObject);
            sources.put(p.thisObject,new ArrayList<>(source));remember(p.thisObject);project(p.thisObject,(Integer)p.args[0]);
        }};
        XposedBridge.hookMethod(init,rebuild);XposedBridge.hookMethod(cloudInit,rebuild);
        XposedBridge.hookMethod(panelInit,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(!p.hasThrowable()&&videoType.isInstance(p.args[0])&&supportsMode(panelMode.getInt(p.args[2])))visiblePanel=new WeakReference<>(p.args[2]);
        }});
        XposedBridge.hookMethod(panelDestroy,new StrictHook(){@Override protected void after(MethodHookParam p){if(visiblePanel.get()==p.thisObject)visiblePanel.clear();}});
        XposedBridge.hookMethod(workspaceValue,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int mode=(Integer)p.args[0];if(!supportsMode(mode)||!videoType.isInstance(p.thisObject))return;
            String value=(String)p.args[1];int slot=slot(Integer.parseInt(value));
            if(new VideoFilterWorkspace270(runtime,options.context()).hidden().contains(VideoFilterWorkspace270.identity(Integer.parseInt(value)))){p.setResult(defaultValue.invoke(p.thisObject,mode));return;}
            int look=LeicaLooks273.videoOrdinal(Integer.parseInt(value));
            if(slot==0&&look==0)return;
            p.setResult(eligible(mode)&&(look>0||videoEnabled(slot))?value:defaultValue.invoke(p.thisObject,mode));
        }});
        XposedBridge.hookMethod(videoId,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int mode=mode(),value=selectedValue(mode),slot=slot(value),look=LeicaLooks273.videoOrdinal(value);if(slot==0&&look==0)return;
            if(slot>0&&!videoEnabled(slot)){p.setResult(0);return;}
            try{p.setResult(look>0?publishLook(look):publish(slot));}catch(IOException failure){publicationFailed(mode,look>0?value:slot,failure);p.setResult(0);}
        }});
        XposedBridge.hookMethod(switchOn,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int mode=(Integer)p.args[0];if(!supportsMode(mode))return;
            int value=Integer.parseInt((String)getValue.invoke(p.thisObject,mode)),slot=slot(value);
            if(LeicaLooks273.videoOrdinal(value)>0)p.setResult(eligible(mode));else if(slot>0)p.setResult(eligible(mode)&&videoEnabled(slot));
        }});
        XposedBridge.hookMethod(degree,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int mode=mode(),value=selectedValue(mode),slot=slot(value),look=LeicaLooks273.videoOrdinal(value);
            if(look==0&&(slot==0||!videoEnabled(slot)))return;
            boolean config=(Boolean)degreeConfigMode.invoke(null,mode);
            Object repository=config?cameraConfig.invoke(cameraRepository.invoke(null),p.args[1]):runningRepository.invoke(null);
            Object strength=repositoryItem.invoke(repository,config?strengthConfigType:strengthType);
            p.setResult(Integer.parseInt((String)strengthValue.invoke(strength,mode,value)));
        }});
    }
}
