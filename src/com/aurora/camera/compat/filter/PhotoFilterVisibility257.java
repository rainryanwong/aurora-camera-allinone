package com.aurora.camera.compat.filter;

import android.content.Context;
import android.view.View;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** CamStyle visibility and order for the stock Photo filter list and thumbnails. */
public final class PhotoFilterVisibility257 {
    private static final String FILTER="pref_camera_shader_coloreffect_key",SOURCE="257.photo.source",SCOPE="257.photo.scope",RECIPE="219.recipe";
    private static PhotoFilterVisibility257 instance;
    private final FilterWorkspace215 workspace;
    private final Class<?> photo,filter;
    private final Field component,rows,adapter,list,selected,click,mode,items,value,parameters,adapterRows,listRows,listIndex,cloudPanel,taskKind,taskPanel;
    private final Method getItems,restoreItems,index,currentValue,config,configure,closed,present,viewInit,notifyAdapter;
    private final List<Method> readers=new ArrayList<>(),maintenance=new ArrayList<>();
    private final Method cloudRefresh,cloudResult,thumbnail,initialize,resume,destroy;
    private final ThreadLocal<Scope> scope=new ThreadLocal<>();
    private final ThreadLocal<Integer> full=ThreadLocal.withInitial(()->0);
    private final List<WeakReference<Object>> panels=new ArrayList<>();
    private final String original;
    private final Map<Object,Projection> projections=new java.util.WeakHashMap<>();
    private static final class Projection {
        final List<Object> master,visible;final Object workspace;final long revision;final Source source;
        Projection(Object owner,List<Object> master,List<Object> visible,Object workspace,long revision){this.master=master;this.visible=visible;this.workspace=workspace;this.revision=revision;source=new Source(owner,master);}
        ArrayList<Object> copy(){ArrayList<Object> result=new ArrayList<>(visible);XposedHelpers.setAdditionalInstanceField(result,SOURCE,source);return result;}
    }
    private static final class Scope {
        final Object panel;final boolean thumbnail;
        Scope(Object panel,boolean thumbnail){this.panel=panel;this.thumbnail=thumbnail;}
    }
    private static final class Source {
        final Object component;final ArrayList<Object> full;
        Source(Object component,List<?> full){this.component=component;this.full=new ArrayList<>(full);}
    }
    public PhotoFilterVisibility257(Host host,LutRuntime229 runtime)throws ReflectiveOperationException {
        workspace=new FilterWorkspace215(host,runtime);photo=host.type("com.android.camera.fragment.j0");filter=host.type("r2.t");
        component=host.field("P9.i","P");rows=host.field("P9.i","R");adapter=host.field("P9.i","O");list=host.field("P9.i","M");selected=host.field("P9.i","V");click=host.field("P9.i","c0");mode=host.field("com.android.camera.fragment.h","mCurrentMode");
        items=host.field("com.android.camera.data.data.c","mItems");value=host.field("com.android.camera.data.data.d","r");parameters=host.field("U9.S","e");
        adapterRows=host.field("com.android.camera.fragment.d","f");listRows=host.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","a");listIndex=host.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","b");
        getItems=host.method("r2.a","getItems");restoreItems=host.method("r2.a","n",int.class,ArrayList.class);index=host.method("P9.i","Qs");currentValue=host.method("P9.i","Ps");
        config=host.method("R6.D","b");configure=host.method("R6.D","Un",int.class);closed=host.method("v2.U","s",int.class,boolean.class);present=host.method("P9.i","mt",int.class,String.class);
        viewInit=host.method("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","c",host.type("com.android.camera.data.data.c"),int.class,host.type("P9.i"));notifyAdapter=host.method("androidx.recyclerview.widget.RecyclerView$g","notifyDataSetChanged");
        readers.add(host.method("P9.i","Ts"));readers.add(host.method(photo,"Ns"));readers.add(index);
        cloudRefresh=host.method("P9.i","Ks",host.type("P9.i"),ConcurrentHashMap.class);cloudResult=host.method("P9.h","a",Object.class);
        // The synthetic observer's P9.i field is its captured panel; bind by the
        // exact field descriptor because JADX may rename the one-letter symbol.
        Field captured=null;for(Field field:host.type("P9.h").getDeclaredFields())if(field.getType()==host.type("P9.i")){field.setAccessible(true);captured=field;}
        if(captured==null)throw new NoSuchFieldException("P9.h: LP9/i;");cloudPanel=captured;
        thumbnail=host.method("Is.a","run");taskKind=host.field("Is.a","a");taskPanel=host.field("Is.a","b");
        initialize=host.method(photo,"initView",View.class);resume=host.method(photo,"Cs",boolean.class);destroy=host.method("P9.i","Bs");
        for(Method method:host.type("com.android.camera.features.mode.capture.d").getDeclaredMethods())
            if(java.util.Arrays.asList("C","f","Q","G").contains(method.getName()))maintenance.add(method);
        original=String.valueOf(host.field("i3.b","P").getInt(null));
    }
    private boolean eligible(Object panel){return photo.isInstance(panel)&&(Integer)read(mode,panel)==163&&filter.isInstance(read(component,panel));}
    private void remember(Object panel){
        synchronized(panels){for(Iterator<WeakReference<Object>> it=panels.iterator();it.hasNext();){Object old=it.next().get();if(old==null)it.remove();else if(old==panel)return;}panels.add(new WeakReference<>(panel));}
    }
    private void capture(Object panel){
        if(!eligible(panel))return;remember(panel);Object owner=read(component,panel),visible=read(rows,panel);
        if(visible!=null)XposedHelpers.setAdditionalInstanceField(visible,SOURCE,new Source(owner,(List<?>)read(items,owner)));
    }
    @SuppressWarnings("unchecked") private ArrayList<Object> project(Object owner)throws IOException {
        CamStyleAccess260.lock();try{
        Object current;
        full.set(full.get()+1);
        try{current=workspace.workspace(workspace.frontFacing());}
        finally{int depth=full.get()-1;if(depth==0)full.remove();else full.set(depth);}
        List<Object> master=(List<Object>)read(items,owner);long revision=WorkspaceFilterOrder270.revision();
        Projection cached=projections.get(owner);
        if(cached!=null&&cached.master==master&&cached.workspace==current&&cached.revision==revision)return cached.copy();
        Set<String> hidden=workspace.hidden(current);Map<String,Object> official=new HashMap<>();
        for(Object row:workspace.raw(current))if((Boolean)read(workspace.official,row)){
            Object id=((Map<?,?>)read(parameters,row)).get(FILTER);if(id instanceof String)official.put((String)id,row);
        }
        ArrayList<Object> visible=new ArrayList<>();
        for(Object item:master){String id=(String)read(value,item);Object row=official.get(id);
            if(original.equals(id)||row==null||workspace.visible(row,hidden))visible.add(item);
        }
        visible.sort(Comparator.comparingInt(item->{String id=(String)read(value,item);Object row=official.get(id);return original.equals(id)?Integer.MIN_VALUE:row==null?Integer.MAX_VALUE:(Integer)read(workspace.order,row);}));
        Projection projection=new Projection(owner,master,visible,current,revision);projections.put(owner,projection);return projection.copy();
        }finally{CamStyleAccess260.unlock();}
    }
    private void enter(Object panel,boolean thumbnail,StrictHook.MethodHookParam param){
        if(!eligible(panel))return;param.setObjectExtra(SCOPE,new Object[]{scope.get()});scope.set(new Scope(panel,thumbnail));
    }
    private void leave(StrictHook.MethodHookParam param){
        Object[] previous=(Object[])param.getObjectExtra(SCOPE);if(previous==null)return;
        if(previous[0]==null)scope.remove();else scope.set((Scope)previous[0]);
    }
    private void synchronize(Object panel)throws IOException,IllegalAccessException {
        if(!eligible(panel)||read(adapter,panel)==null||read(list,panel)==null)return;
        Object owner=read(component,panel);List<?> previous=(List<?>)read(rows,panel);int old=(Integer)read(selected,panel);
        boolean recipe=old>=0&&old<previous.size()&&XposedHelpers.getAdditionalInstanceField(previous.get(old),RECIPE)!=null;
        String active=(String)call(currentValue,panel);ArrayList<Object> visible=project(owner);
        rows.set(panel,visible);adapterRows.set(read(adapter,panel),new ArrayList<>(visible));
        // This native binding also runs the existing recipe presenter, retaining
        // each user preset's own identity instead of treating its base as official.
        call(viewInit,read(list,panel),owner,0,panel);
        List<?> finalRows=(List<?>)read(rows,panel);boolean retained=recipe;
        for(Object row:finalRows)if(active.equals(read(value,row))&&XposedHelpers.getAdditionalInstanceField(row,RECIPE)==null){retained=true;break;}
        // j0.et delegates to this configuration path. Its UI click gate is not
        // part of a management change, so apply the configuration directly.
        if(!retained){call(closed,owner,163,false);call(configure,call(config,null),Integer.parseInt(original));}
        int position=(Integer)call(index,panel);selected.setInt(panel,position);((AtomicInteger)read(click,panel)).set(position);call(present,panel,position,call(currentValue,panel));
        listRows.set(read(list,panel),new ArrayList<>(finalRows));listIndex.setInt(read(list,panel),position);
        adapterRows.set(read(adapter,panel),new ArrayList<>(finalRows));call(notifyAdapter,read(adapter,panel));capture(panel);
    }
    /** Called by the shared workspace's existing main-thread change dispatch. */
    public static void changed(){
        if(instance==null)return;
        try{List<WeakReference<Object>> current;synchronized(instance.panels){current=new ArrayList<>(instance.panels);}
            for(WeakReference<Object> ref:current){Object panel=ref.get();if(panel!=null)instance.synchronize(panel);}
        }catch(IOException|IllegalAccessException failure){throw new IllegalStateException("Photo filter list refresh failed",failure);}
    }
    public void install(){
        instance=this;
        for(Method method:maintenance)XposedBridge.hookMethod(method,new StrictHook(2000){
            @Override protected void before(MethodHookParam p){full.set(full.get()+1);}
            @Override protected void after(MethodHookParam p){int depth=full.get()-1;if(depth==0)full.remove();else full.set(depth);}
        });
        for(Method method:readers)XposedBridge.hookMethod(method,new StrictHook(1000){
            @Override protected void before(MethodHookParam p){if(photo.isInstance(p.thisObject)){
                // Ts assigns its component and mode inside the native method.
                p.setObjectExtra(SCOPE,new Object[]{scope.get()});scope.set(new Scope(p.thisObject,false));}}
            @Override protected void after(MethodHookParam p){try{if(!p.hasThrowable())capture(p.thisObject);}finally{leave(p);}}
        });
        XposedBridge.hookMethod(cloudRefresh,new StrictHook(1000){
            @Override protected void before(MethodHookParam p){enter(p.args[0],false,p);}
            @Override protected void after(MethodHookParam p)throws Throwable{try{if(!p.hasThrowable()){capture(p.args[0]);synchronize(p.args[0]);}}finally{leave(p);}}
        });
        XposedBridge.hookMethod(cloudResult,new StrictHook(1000){
            @Override protected void before(MethodHookParam p){enter(read(cloudPanel,p.thisObject),false,p);}
            @Override protected void after(MethodHookParam p){leave(p);}
        });
        XposedBridge.hookMethod(viewInit,new StrictHook(1000){
            @Override protected void before(MethodHookParam p){enter(p.args[2],false,p);}
            @Override protected void after(MethodHookParam p){try{if(!p.hasThrowable())capture(p.args[2]);}finally{leave(p);}}
        });
        XposedBridge.hookMethod(thumbnail,new StrictHook(1000){
            @Override protected void before(MethodHookParam p){if((Integer)read(taskKind,p.thisObject)==3)enter(read(taskPanel,p.thisObject),true,p);}
            @Override protected void after(MethodHookParam p){leave(p);}
        });
        XposedBridge.hookMethod(getItems,new StrictHook(1000){@Override protected void after(MethodHookParam p)throws Throwable{
            Scope reader=scope.get();if(p.hasThrowable()||reader==null||full.get()>0||!eligible(reader.panel)||read(component,reader.panel)!=p.thisObject)return;
            p.setResult(reader.thumbnail?read(rows,reader.panel):project(p.thisObject));
        }});
        XposedBridge.hookMethod(restoreItems,new StrictHook(1000){@Override protected void before(MethodHookParam p){
            Object source=XposedHelpers.getAdditionalInstanceField(p.args[1],SOURCE);
            if(source instanceof Source&&((Source)source).component==p.thisObject)p.args[1]=new ArrayList<>(((Source)source).full);
        }});
        StrictHook refresh=new StrictHook(1000){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())synchronize(p.thisObject);}};
        XposedBridge.hookMethod(initialize,refresh);XposedBridge.hookMethod(resume,refresh);
        XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void after(MethodHookParam p){synchronized(panels){panels.removeIf(ref->ref.get()==null||ref.get()==p.thisObject);}}});
    }
}
