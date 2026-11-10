package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.lang.ref.WeakReference;

/** One native CamStyle workspace. Order/CRUD stay in the camera's own storage. */
public final class FilterWorkspace215 {
    private static final String CACHE="215.camstyle.visible";
    private static final ThreadLocal<Boolean> photoLoading=new ThreadLocal<>();
    private final Host host;
    private final LutRuntime229 runtime;
    private final Field parameters;
    final Class<?> camStyle,rear,front,rowClass,styleRow;
    final Method global,provider,obtain,getList,load,kind,label,isOriginal,setOrder,select,markSelected;
    final Field rawList,identity,order,official,selected,mode;
    private final List<Method> maintenance=new ArrayList<>();
    private final List<Method> readers=new ArrayList<>();
    private final List<Method> gridMaintenance=new ArrayList<>();
    private final ThreadLocal<Integer> depth=ThreadLocal.withInitial(()->0);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners=new ArrayList<>();
    private final List<WeakReference<Object>> displayed=new ArrayList<>();
    private final List<WeakReference<Object>> fragments=new ArrayList<>();
    private final Method resume,notifyAdapter;
    private final Field fragmentWorkspace,fragmentAdapter;
    private final Method globalData,cameraFacing,currentMode;
    private final Field intentType;
    private final Class<?> nativeManager;
    private final Field gridAdapter,gridRows,gridSelected;
    private final Method closeGridMenu;
    private SharedPreferences prefs;
    private boolean posted;
    private final Runnable dispatch=this::dispatchChanges;
    private void dispatchChanges(){
        synchronized(this){posted=false;}
        displayed.removeIf(ref->ref.get()==null);
        for(WeakReference<Object> ref:displayed){Object ws=ref.get();if(ws!=null)project(ws,true);}
        fragments.removeIf(ref->ref.get()==null);
        for(WeakReference<Object> ref:fragments){Object fragment=ref.get();if(fragment==null)continue;Object ws=read(fragmentWorkspace,fragment);if(!camStyle.isInstance(ws))continue;
            Object adapter=read(fragmentAdapter,fragment);if(adapter!=null)call(notifyAdapter,adapter);
            // The native management grid also retains a separate p list (k is
            // its master). Refresh both consumers after edits in this dialog.
            if(nativeManager.isInstance(fragment)){
                Object grid=read(gridAdapter,fragment);if(grid==null)continue;
                @SuppressWarnings("unchecked") List<Object> rows=(List<Object>)read(gridRows,grid);
                List<Object> next=ordered(ws);next.removeIf(row->(Boolean)call(isOriginal,row));
                rows.clear();rows.addAll(next);
                Object active=read(gridSelected,grid);if(active!=null&&!next.contains(active)){
                    try{gridSelected.set(grid,null);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}call(closeGridMenu,grid);
                }
                call(notifyAdapter,grid);
            }
        }
        for(Runnable listener:new ArrayList<>(listeners))listener.run();
        PhotoFilterVisibility257.changed();
    }
    static final class BackgroundRows {final long revision;final List<Object> rows;BackgroundRows(long revision,List<Object> rows){this.revision=revision;this.rows=rows;}}
    static final class Cache {final ArrayList<Object> visible=new ArrayList<>();}
    public static final class Entry {
        public final Object nativeRow;public final String id,name;public final boolean fixed,builtIn;
        Entry(Object row,String id,String name,boolean fixed,boolean builtIn){nativeRow=row;this.id=id;this.name=name;this.fixed=fixed;this.builtIn=builtIn;}
    }
    public FilterWorkspace215(Host h)throws ReflectiveOperationException {
        this(h,null);
    }
    public FilterWorkspace215(Host h,LutRuntime229 runtime)throws ReflectiveOperationException {
        this.runtime=runtime;parameters=h.field("U9.S","e");
        host=h;camStyle=h.type("com.android.camera.features.mode.capture.d");rear=h.type("com.android.camera.features.mode.capture.k");front=h.type("com.android.camera.features.mode.capture.i");rowClass=h.type("U9.S");styleRow=h.type("com.android.camera.features.mode.capture.e");
        global=h.method("com.xiaomi.camera.basic.Global","getContext");provider=h.method("g2.a","e");obtain=h.method("y2.a","a",Class.class);
        getList=h.method("com.xiaomi.microfilm.vlog.vv.v","getList");rawList=h.field("com.xiaomi.microfilm.vlog.vv.v","mItemList");
        kind=h.method("U9.a","l");load=h.method(camStyle,"G",int.class);label=h.method("com.android.camera.features.mode.capture.e","z",Context.class);
        isOriginal=h.method("com.android.camera.features.mode.capture.e","b0");setOrder=h.method(rowClass,"X",int.class);
        select=h.method(camStyle,"E",Context.class,int.class,rowClass);
        identity=h.field(rowClass,"a");order=h.field(rowClass,"K");official=h.field(rowClass,"p");selected=h.field(rowClass,"n");
        mode=h.field("U9.a","a");markSelected=h.method(rowClass,"W",boolean.class);
        resume=h.method("U9.M","onResume");fragmentWorkspace=h.field("U9.M","V");fragmentAdapter=h.field("U9.M","Q");notifyAdapter=h.method("androidx.recyclerview.widget.RecyclerView$g","notifyDataSetChanged");
        globalData=h.method("g2.a","g");cameraFacing=h.method("u2.Y","B");currentMode=h.method("u2.Y","D",int.class);intentType=h.field("u2.Y","u");
        nativeManager=h.type("com.android.camera.features.mode.capture.a0");gridAdapter=h.field(nativeManager,"n0");gridRows=h.field("com.android.camera.fragment.S","p");gridSelected=h.field("com.android.camera.fragment.S","l");closeGridMenu=h.method("com.android.camera.fragment.S","H");
        // Internal loading/binding and native management must see every preset,
        // including hidden ones. No projection can become the master catalog.
        for(String owner:new String[]{"U9.a",camStyle.getName()})
            for(Method method:h.type(owner).getDeclaredMethods())
                if(!java.lang.reflect.Modifier.isAbstract(method.getModifiers())&&
                   java.util.Arrays.asList("G","C","f","Q","H","I","a","b","add","remove","addUserItem","restoreWorkspace","rollbackData","onCleared").contains(method.getName()))maintenance.add(method);
        for(String owner:new String[]{"U9.a",camStyle.getName()})
            for(Method method:h.type(owner).getDeclaredMethods())
                if(!java.lang.reflect.Modifier.isAbstract(method.getModifiers())&&
                   java.util.Arrays.asList("g","h","p","s","t","u","c","o","B","E","O").contains(method.getName()))readers.add(method);
        gridMaintenance.add(h.method(nativeManager,"It",h.type("android.view.View")));
        gridMaintenance.add(h.method(nativeManager,"Ks",String.class,boolean.class));
        gridMaintenance.add(h.method(nativeManager,"onPause"));
        gridMaintenance.add(h.method(nativeManager,"onResume"));
    }
    static Object call(Method m,Object receiver,Object... args) {
        try{return m.invoke(receiver,args);}catch(IllegalAccessException e){throw new IllegalStateException(m.toString(),e);}
        catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;if(cause instanceof Error)throw (Error)cause;throw new IllegalStateException(m.toString(),cause);}
    }
    static Object read(Field f,Object receiver){try{return f.get(receiver);}catch(IllegalAccessException e){throw new IllegalStateException(f.toString(),e);}}
    private SharedPreferences prefs(){if(prefs==null)prefs=((Context)call(global,null)).getSharedPreferences("aurora_filter_workspace_v2",Context.MODE_PRIVATE);return prefs;}
    public boolean frontFacing(){return (Integer)call(cameraFacing,call(globalData,null))==1;}
    public int currentMode(){Object data=call(globalData,null);return (Integer)call(currentMode,data,(Integer)read(intentType,data));}
    public Object workspace(boolean frontFacing)throws IOException {
        CamStyleAccess260.lock();try{
        Object result=call(obtain,call(provider,null),frontFacing?front:rear);
        if(raw(result).isEmpty()){
            // Native f binds from the CURRENT camera's component. Loading the
            // opposite facing here could prune its official folders as unsupported.
            if(frontFacing!=frontFacing())throw new IOException("请先在相机中打开一次"+(frontFacing?"前置":"后置")+"滤镜面板，再管理该工作区");
            // These are Photo workspaces. G also reads the last shooting mode
            // internally, so its restore and binding must share the same mode.
            Boolean previous=photoLoading.get();photoLoading.set(true);
            try{call(load,result,163);}finally{if(previous==null)photoLoading.remove();else photoLoading.set(previous);}
        }
        return result;
        }finally{CamStyleAccess260.unlock();}
    }
    @SuppressWarnings("unchecked") public List<Object> raw(Object workspace){return (List<Object>)read(rawList,workspace);}
    /** Read only the already-loaded current workspace; never reload another facing. */
    public String selectedEffectsKey(){
        CamStyleAccess260.lock();try{
        Object workspace=call(obtain,call(provider,null),frontFacing()?front:rear);
        for(Object row:raw(workspace))if((Boolean)read(selected,row)&&!(Boolean)call(isOriginal,row))
            return LutStore229.recipeKey(kind(workspace),(String)read(identity,row));
        return null;
        }finally{CamStyleAccess260.unlock();}
    }
    public String kind(Object workspace){return (String)call(kind,workspace);}
    public void enter(){depth.set(depth.get()+1);}
    public void leave(){int n=depth.get()-1;if(n<0)throw new IllegalStateException("CamStyle maintenance depth");if(n==0)depth.remove();else depth.set(n);}
    public boolean full(){return depth.get()>0;}
    public void listen(Runnable r){listeners.add(r);}
    public void unlisten(Runnable r){listeners.remove(r);}
    public void changed(){WorkspaceFilterOrder270.invalidate();synchronized(this){if(posted)return;posted=true;}main.post(dispatch);}
    public List<Entry> snapshot(Object workspace,Context context){
        CamStyleAccess260.lock();try{
        ArrayList<Object> rows=new ArrayList<>(raw(workspace));rows.sort(Comparator.comparingInt(r->(Integer)read(order,r)));
        ArrayList<Entry> result=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(Object row:rows){String id=(String)read(identity,row);if(id==null||!ids.add(id))throw new IllegalStateException("CamStyle duplicate/missing identity: "+id);
            result.add(new Entry(row,id,(String)call(label,row,context),(Boolean)call(isOriginal,row),(Boolean)read(official,row)));}
        return result;
        }finally{CamStyleAccess260.unlock();}
    }
    public Set<String> hidden(Object workspace){return new HashSet<>(prefs().getStringSet("hidden:"+kind(workspace),Collections.emptySet()));}
    private int slot(Object row){
        if(runtime==null||!(Boolean)read(official,row))return 0;
        Object filter=((java.util.Map<?,?>)read(parameters,row)).get("pref_camera_shader_coloreffect_key");
        return filter instanceof String?runtime.slot(Integer.parseInt((String)filter)):0;
    }
    public boolean visible(Object row,Set<String> hidden){
        if((Boolean)call(isOriginal,row))return true;
        int slot=slot(row);return shown(hidden,(String)read(identity,row),slot==0||runtime.store().enabled(slot));
    }
    public boolean enabled(Entry entry,Set<String> hidden){
        CamStyleAccess260.lock();try{return visible(entry.nativeRow,hidden);}finally{CamStyleAccess260.unlock();}
    }
    public boolean enabled(Object workspace,Entry entry){return enabled(entry,hidden(workspace));}
    // Existing sets contain disabled identities. An explicit enable overrides the shared import default only here.
    static void setVisibility(Set<String> settings,String id,boolean enabled){
        if(enabled){settings.remove(id);settings.add("+"+id);}
        else{settings.remove("+"+id);settings.add(id);}
    }
    static boolean shown(Set<String> settings,String id,boolean defaultValue){
        return !settings.contains(id)&&(defaultValue||settings.contains("+"+id));
    }
    public List<String> initial(Object workspace,List<Entry> entries)throws IOException {
        String key="initial:"+kind(workspace);String value=prefs().getString(key,null);
        if(value!=null)return java.util.Arrays.asList(value.split("\n",-1));
        ArrayList<String> ids=new ArrayList<>();for(Entry e:entries)ids.add(e.id);
        if(!prefs().edit().putString(key,String.join("\n",ids)).commit())throw new IOException("无法记录初始滤镜顺序");return ids;
    }
    public void apply(Object workspace,List<Entry> rows,Set<String> hidden)throws IOException {
        CamStyleAccess260.lock();try{
        List<Object> master=raw(workspace);if(rows.size()!=master.size()||!new HashSet<>(master).equals(nativeRows(rows)))throw new IOException("相机滤镜列表已变化，请刷新后重试");
        for(Entry e:rows)if(e.fixed&&hidden.contains(e.id))throw new IOException("原图不能停用");
        Entry selectedRow=null,original=null;for(Entry e:rows){if((Boolean)read(selected,e.nativeRow)&&!visible(e.nativeRow,hidden))selectedRow=e;if(e.fixed)original=e;}
        if(selectedRow!=null&&original==null)throw new IOException("当前工作区没有原图条目，无法停用正在使用的滤镜");
        ArrayList<Object> oldMaster=new ArrayList<>(master);Set<String> oldHidden=hidden(workspace);int[] ranks=new int[rows.size()];for(int i=0;i<rows.size();i++)ranks[i]=(Integer)read(order,rows.get(i).nativeRow);
        int touched=0;boolean visibilityTouched=false;
        try{
            for(int i=0;i<rows.size();i++){touched=i+1;call(setOrder,rows.get(i).nativeRow,i);}
            visibilityTouched=true;if(!prefs().edit().putStringSet("hidden:"+kind(workspace),new HashSet<>(hidden)).commit())throw new IOException("滤镜显示状态未能保存");
            master.clear();for(Entry e:rows)master.add(e.nativeRow);
        }catch(RuntimeException|IOException failure){
            IOException report=new IOException("滤镜修改未完成",failure);
            // SharedPreferences updates its memory map even when disk commit
            // returns false. Restore both the memory value and persistent value.
            if(visibilityTouched&&!prefs().edit().putStringSet("hidden:"+kind(workspace),oldHidden).commit())report.addSuppressed(new IOException("显示状态回滚未能写入磁盘"));
            // X sets K before creating its marker; reset K to force native repair.
            for(int i=0;i<touched;i++)try{order.setInt(rows.get(i).nativeRow,Integer.MIN_VALUE);call(setOrder,rows.get(i).nativeRow,ranks[i]);}catch(ReflectiveOperationException|RuntimeException rollback){report.addSuppressed(rollback);}
            master.clear();master.addAll(oldMaster);throw report;
        }finally{changed();}
        if(selectedRow!=null)selectOriginal(workspace,selectedRow,original);
        }finally{CamStyleAccess260.unlock();}
    }
    private void selectOriginal(Object workspace,Entry old,Entry original){
        call(markSelected,old.nativeRow,false);call(markSelected,original.nativeRow,true);
        call(select,workspace,call(global,null),(Integer)read(mode,workspace),original.nativeRow);
    }
    public void beforeDelete(Object workspace,Entry entry,Context context)throws IOException {
        CamStyleAccess260.lock();try{
        if(!(Boolean)read(selected,entry.nativeRow))return;
        for(Entry e:snapshot(workspace,context))if(e.fixed){selectOriginal(workspace,entry,e);return;}
        throw new IOException("当前工作区没有原图条目，无法删除正在使用的滤镜");
        }finally{CamStyleAccess260.unlock();}
    }
    private Set<Object> nativeRows(List<Entry> rows){Set<Object> result=new HashSet<>();for(Entry e:rows)result.add(e.nativeRow);return result;}
    private ArrayList<Object> ordered(Object workspace){
        CamStyleAccess260.lock();try{
            ArrayList<Object> next=new ArrayList<>(raw(workspace));
            next.sort(Comparator.comparingInt(r->(Integer)read(order,r)));return next;
        }finally{CamStyleAccess260.unlock();}
    }
    private ArrayList<Object> visible(Object workspace){
        CamStyleAccess260.lock();try{
        Set<String> hidden=hidden(workspace);
        ArrayList<Object> next=ordered(workspace);
        next.removeIf(row->!visible(row,hidden));
        return next;
        }finally{CamStyleAccess260.unlock();}
    }
    private List<Object> project(Object workspace,boolean rebuild){
        // Native adapters retain this list and read it without our lock. Only
        // the main thread may own it; background callers get a separate view.
        if(Looper.myLooper()!=Looper.getMainLooper()){
            CamStyleAccess260.lock();try{
                long revision=WorkspaceFilterOrder270.revision();
                BackgroundRows snapshot=(BackgroundRows)XposedHelpers.getAdditionalInstanceField(workspace,CACHE+".worker");
                if(snapshot==null||snapshot.revision!=revision){
                    snapshot=new BackgroundRows(revision,visible(workspace));
                    XposedHelpers.setAdditionalInstanceField(workspace,CACHE+".worker",snapshot);
                }
                return new ArrayList<>(snapshot.rows);
            }finally{CamStyleAccess260.unlock();}
        }
        Cache cache=(Cache)XposedHelpers.getAdditionalInstanceField(workspace,CACHE);
        // Even an all-enabled adapter must retain this view: a later toggle
        // updates the SAME list, rather than leaving it attached to the master.
        if(cache==null){cache=new Cache();XposedHelpers.setAdditionalInstanceField(workspace,CACHE,cache);displayed.add(new WeakReference<>(workspace));rebuild=true;}
        // A native drag may mutate this view before persisting each row's K.
        // Rebuilding in the middle of that loop would undo its move or throw.
        if(!rebuild)return cache.visible;
        ArrayList<Object> next=visible(workspace);
        if(!cache.visible.equals(next)){cache.visible.clear();cache.visible.addAll(next);}
        return cache.visible;
    }
    public void install(){
        XposedBridge.hookMethod(currentMode,new StrictHook(){@Override protected void before(MethodHookParam p){
            if(Boolean.TRUE.equals(photoLoading.get()))p.setResult(163);
        }});
        // Hold the catalog transaction across the complete native call and
        // all its projection callbacks. UI publication is queued on main.
        for(Method method:maintenance)XposedBridge.hookMethod(method,new StrictHook(100){
            @Override protected void before(MethodHookParam p){if(camStyle.isInstance(p.thisObject))enter();}
            @Override protected void after(MethodHookParam p){if(camStyle.isInstance(p.thisObject)){leave();changed();}}
        });
        for(Method method:maintenance)CamStyleAccess260.guard(method,camStyle);
        for(Method method:readers)CamStyleAccess260.guard(method,camStyle);
        CamStyleAccess260.guard(setOrder,styleRow);
        CamStyleAccess260.guard(markSelected,styleRow);
        for(Method method:gridMaintenance)CamStyleAccess260.guard(method,nativeManager);
        Method pause=gridMaintenance.get(2);
        XposedBridge.hookMethod(pause,new StrictHook(){
            @Override protected void before(MethodHookParam p){
                Object grid=read(gridAdapter,p.thisObject);if(grid==null)return;
                Object ws=read(fragmentWorkspace,p.thisObject);
                @SuppressWarnings("unchecked") List<Object> rows=(List<Object>)read(gridRows,grid);
                // Keep the user's grid order, discard removed members, and
                // retain entries added by a load since the grid was opened.
                List<Object> current=raw(ws);rows.removeIf(row->!current.contains(row));
                for(Object row:current)if(!(Boolean)call(isOriginal,row)&&!rows.contains(row))rows.add(row);
            }
            @Override protected void after(MethodHookParam p){changed();}
        });
        XposedBridge.hookMethod(setOrder,new StrictHook(){@Override protected void after(MethodHookParam p){if(styleRow.isInstance(p.thisObject))changed();}});
        XposedBridge.hookMethod(resume,new StrictHook(){@Override protected void after(MethodHookParam p){if(p.hasThrowable())return;for(WeakReference<Object> ref:fragments)if(ref.get()==p.thisObject)return;fragments.add(new WeakReference<>(p.thisObject));}});
        XposedBridge.hookMethod(getList,new StrictHook(){
            @Override protected void after(MethodHookParam p){
                if(p.hasThrowable()||full()||!camStyle.isInstance(p.thisObject))return;
                p.setResult(project(p.thisObject,false));
            }
        });
    }
}
