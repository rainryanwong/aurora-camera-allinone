package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** Project imported LUTs into Photo's native CamStyle workspace, without C()'s full rebuild. */
public final class LutWorkspace237 {
    private static final String FILTER="pref_camera_shader_coloreffect_key",SCOPE="237.lut.workspace";
    private final Host host;private final LutRuntime229 runtime;private final FilterWorkspace215 w;
    private final Method bind,reload,initialize,cloudInitialize,repository,component,componentItems,root,rowType,create,parse,delete;
    private final Method controller,catalog,infoId,mapperGet,map;
    private final Method componentKey,parameter,createNative,unsupported,retainUnsupported;
    private final Field parameters,binding,displayName,items,category,capabilities,mapperProvider,itemValue,itemName,itemLabel;
    private final Class<?> filterType;
    private final ThreadLocal<Boolean> synchronizing=new ThreadLocal<>();
    private PhoenixDefaults257 defaults;
    public LutWorkspace237(Host h,LutRuntime229 runtime)throws ReflectiveOperationException{
        host=h;this.runtime=runtime;w=new FilterWorkspace215(h,runtime);
        bind=h.method("com.android.camera.features.mode.capture.d","f",int.class);
        reload=h.method("com.android.camera.features.mode.capture.d","Q",int.class,Context.class,java.util.concurrent.ConcurrentHashMap.class);
        initialize=h.method("v2.U","e",int.class);cloudInitialize=h.method("v2.U","d",int.class,Map.class);
        repository=h.method("g2.a","a");component=h.method("Xh.b","x",Class.class);filterType=h.type("r2.t");
        componentItems=h.method("r2.a","getItems");items=h.field("com.android.camera.data.data.c","mItems");category=h.field("r2.a","a");capabilities=h.field("v2.U","b");
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");catalog=h.method("com.xiaomi.camera.effect.EffectController","q",int.class);infoId=h.method("i3.b","a");
        mapperProvider=h.field("i2.a","a");mapperGet=h.method("i2.b","c");map=h.method("j2.d","j",int.class,ArrayList.class,h.type("l9.g"));
        itemValue=h.field("com.android.camera.data.data.d","r");itemName=h.field("com.android.camera.data.data.d","l");itemLabel=h.field("com.android.camera.data.data.d","o");
        root=h.method("U9.a","getWorkspaceDir");rowType=h.method("U9.a","n");
        create=h.method("U9.S","f",String.class,Class.class,String.class,boolean.class,String.class,String.class,int.class,boolean.class,String[].class);
        parse=h.method("U9.S","N",int.class,String.class,Class.class,String.class);delete=h.method("U9.S","L",boolean.class);
        parameters=h.field("U9.S","e");binding=h.field("com.android.camera.features.mode.capture.e","M");displayName=h.field("U9.S","l");
        componentKey=h.method("v2.U","getKey",int.class);parameter=h.method("U9.S","H",String.class);
        createNative=h.method("com.android.camera.features.mode.capture.d","M",int.class,Context.class,String.class,int.class,h.type("com.android.camera.data.data.d"));
        unsupported=h.method("si.f","h",int.class,String.class);retainUnsupported=h.method("F6.a","v");
    }
    private boolean eligible(Object workspace,int mode){return mode==163&&w.camStyle.isInstance(workspace)&&w.front.isInstance(workspace)==w.frontFacing()&&!w.raw(workspace).isEmpty();}
    public int ownedSlot(Object workspace,Object row){
        return ownedSlot(row);
    }
    public int ownedSlot(Object row){
        // Q() can create the native official row before our mirror is needed.
        // Its directory uses the display name, so recognize its actual filter
        // parameter rather than requiring our stable token in the identity.
        if(!(Boolean)read(w.official,row))return 0;
        Object value=((Map<?,?>)read(parameters,row)).get(FILTER);if(!(value instanceof String))return 0;
        int id=Integer.parseInt((String)value),category=id>>>16;
        return category==2||category==7||category==10?runtime.slot(id):0;
    }
    public int ownedSlot(String kind,String identity){
        String prefix=kind+"_official_";if(identity==null||!identity.startsWith(prefix))return 0;
        int split=identity.indexOf('_',prefix.length());if(split<0)return 0;
        int slot=LutStore229.slot(identity.substring(split+1)),id;try{id=Integer.parseInt(identity.substring(prefix.length(),split));}catch(NumberFormatException other){return 0;}
        int category=id>>>16;return slot>0&&runtime.slot(id)==slot&&(category==2||category==7||category==10)&&identity.equals(identity(kind,slot,id))?slot:0;
    }
    String identity(String kind,int slot,int id){return kind+"_official_"+id+"_"+LutStore229.token(slot);}
    private Object filter(){return call(component,call(repository,null),filterType);}
    private PhoenixDefaults257 defaults()throws IOException{
        if(defaults==null)try{defaults=new PhoenixDefaults257(host,runtime,w,this);}
        catch(ReflectiveOperationException failure){throw new IOException("原生默认滤镜接口未能解析",failure);}
        return defaults;
    }
    @SuppressWarnings("unchecked") private void projectComponent(Object owner,int mode)throws IOException{
        int cat=(Integer)read(category,owner);if(cat!=2&&cat!=7&&cat!=10)return;
        if(mode==163)defaults().projectComponent(owner,mode);
        List<Object> current=(List<Object>)read(items,owner);if(current==null||current.isEmpty())return; // native f() initializes an empty component itself
        LutStore229 store=runtime.store();Set<Integer> configured=new HashSet<>(mode==163?store.configuredSlots():store.slots());Set<Integer> present=new HashSet<>();ArrayList<Object> next=new ArrayList<>();
        for(Object row:current){int id=Integer.parseInt((String)read(itemValue,row)),slot=runtime.slot(id);if(slot==0||configured.contains(slot)){next.add(row);present.add(id);}}
        if(configured.isEmpty()){if(next.size()!=current.size())try{items.set(owner,next);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}return;}
        ArrayList<Object> missing=new ArrayList<>();
        for(Object info:mode==163?runtime.workspaceCatalog(cat):(List<?>)call(catalog,call(controller,null),cat)){int id=(Integer)call(infoId,info);if(configured.contains(runtime.slot(id))&&!present.contains(id))missing.add(info);}
        if(!missing.isEmpty()){
            Object mapper=call(mapperGet,read(mapperProvider,null));List<?> added=(List<?>)call(map,mapper,mode,missing,read(capabilities,owner));
            if(added.size()!=missing.size())throw new IOException("原生映射器未返回全部 LUT 条目");next.addAll(added);
        }
        Set<Integer> bound=new HashSet<>();for(Object row:next){int id=Integer.parseInt((String)read(itemValue,row)),slot=runtime.slot(id);if(slot>0){bound.add(slot);try{itemName.setInt(row,-1);itemLabel.set(row,store.label(LutStore229.token(slot),store.name(slot)));}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}}}
        if(!bound.containsAll(configured))throw new IOException("原生拍照目录未映射全部导入 LUT");
        try{items.set(owner,next);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
    }
    private List<FilterWorkspace215.Entry> prepare(Object workspace,int mode)throws IOException{
        projectComponent(filter(),mode);
        List<FilterWorkspace215.Entry> removedSelections=new ArrayList<>();
        Context context=(Context)call(w.global,null);LutStore229 store=runtime.store();Set<Integer> slots=new HashSet<>(store.configuredSlots());
        for(Object row:new ArrayList<>(w.raw(workspace))){int slot=ownedSlot(workspace,row);if(slot==0)continue;
            FilterWorkspace215.Entry entry=new FilterWorkspace215.Entry(row,(String)read(w.identity,row),(String)call(w.label,row,context),false,true);
            if(!slots.contains(slot)){if((Boolean)read(w.selected,row))removedSelections.add(entry);call(delete,row,true);w.raw(workspace).remove(row);}
        }
        // Preserve cloud rows and their order. Only the imported rows are mapped
        // with the same Photo mapper as v2.U.r; do not replace the full component.
        return removedSelections;
    }
    @SuppressWarnings("unchecked") private void append(Object workspace,int mode)throws IOException{
        LutStore229 store=runtime.store();List<Object> rows=w.raw(workspace);String kind=w.kind(workspace);
        projectComponent(filter(),mode);Map<Integer,Object> items=new HashMap<>();
        for(Object item:(List<?>)call(componentItems,filter())){int id=Integer.parseInt((String)read(itemValue,item));int slot=runtime.slot(id);if(slot>0)items.put(slot,item);}
        Map<Integer,Object> owned=new HashMap<>();
        int rank=-1;for(Object row:rows){rank=Math.max(rank,(Integer)read(w.order,row));int slot=ownedSlot(workspace,row);if(slot>0)owned.putIfAbsent(slot,row);}
        for(int slot:store.configuredSlots()){
            Object item=items.get(slot);if(item==null)throw new IOException("原生拍照滤镜目录缺少 LUT 槽位 "+slot);String id=(String)read(itemValue,item);
            Object found=owned.get(slot);
            if(found==null){
                String base=(String)call(root,workspace);Class<?> type=(Class<?>)call(rowType,workspace);File folder=new File(base,identity(kind,slot,Integer.parseInt(id)));
                // Native creation deletes an existing directory. Parse our exact
                // stable mirror if it exists; never invoke creation on that path.
                if(folder.exists()){
                    found=call(parse,null,mode,kind,type,folder.getAbsolutePath());if(found==null)throw new IOException("无法恢复 LUT 工作区条目："+folder.getName());
                }else{
                    found=call(create,null,kind,type,base,false,id,LutStore229.token(slot),mode,false,new String[]{"p_"+FILTER+"_"+id});
                    call(w.setOrder,found,++rank);
                }
                rows.add(found);
            }
            Object actual=((Map<?,?>)read(parameters,found)).get(FILTER);
            if(!id.equals(actual))throw new IOException("原生 LUT 工作区参数未绑定到当前拍照类别，槽位 "+slot);
            try{binding.set(found,item);displayName.set(found,store.label(LutStore229.token(slot),store.name(slot)));}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        }
        Context context=(Context)call(w.global,null);
        // Native f must bind M first: b0() caches whether a row is Original,
        // and querying it while M is null permanently caches a false result.
        Set<String> groupHidden=w.hidden(workspace);
        for(Object row:rows){int slot=ownedSlot(workspace,row);if(slot>0&&(Boolean)read(w.selected,row)&&!w.visible(row,groupHidden)){
            w.beforeDelete(workspace,new FilterWorkspace215.Entry(row,(String)read(w.identity,row),(String)call(w.label,row,context),false,true),context);
        }}
        restoreVisibility(workspace,context,kind,store);
        defaults().projected(workspace,context,kind);
        initializeFrontOrder(workspace,context,kind);
    }
    private void initializeFrontOrder(Object owner,Context context,String kind)throws IOException{
        if(!w.front.isInstance(owner))return;
        SharedPreferences prefs=context.getSharedPreferences("aurora_filter_workspace_v2",0);
        String key="module-first:"+kind;
        if(prefs.getBoolean(key,false))return;
        List<FilterWorkspace215.Entry> rows=w.snapshot(owner,context);
        List<String> current=new ArrayList<>();for(FilterWorkspace215.Entry row:rows)current.add(row.id);
        String initial=prefs.getString("initial:"+kind,null);
        boolean customized=initial!=null&&!WorkspaceOrder215.restore(current,Arrays.asList(initial.split("\n",-1))).equals(current);
        if(!customized){
            frontOrder(rows,context);
            w.apply(owner,rows,w.hidden(owner));
        }
        if(!prefs.edit().putBoolean(key,true).commit())throw new IOException("前置滤镜顺序未能保存");
    }
    void frontOrder(List<FilterWorkspace215.Entry> rows,Context context)throws IOException{
        Map<Integer,Integer> rearRanks=new HashMap<>();
            for(WorkspaceFilterOrder270.Row row:WorkspaceFilterOrder270.read(context,"MiCamStyleRear"))if(row.official){int slot=runtime.slot(row.filter);if(slot>0)rearRanks.putIfAbsent(slot,rearRanks.size());}
            rows.sort(Comparator.comparingInt((FilterWorkspace215.Entry row)->row.fixed?0:looksRank(row)<Integer.MAX_VALUE?1:ownedSlot(row.nativeRow)>0?2:3)
                    .thenComparingInt(row->{int look=looksRank(row);if(look<Integer.MAX_VALUE)return look;int slot=ownedSlot(row.nativeRow);return slot>0?rearRanks.getOrDefault(slot,Integer.MAX_VALUE):0;}));
    }
    private int looksRank(FilterWorkspace215.Entry row){
        if(!row.builtIn)return Integer.MAX_VALUE;Object value=((Map<?,?>)read(parameters,row.nativeRow)).get(FILTER);
        return value instanceof String?LeicaLooks273.rank(Integer.parseInt((String)value)):Integer.MAX_VALUE;
    }
    /** Explicit restoration also removes old workspace hiding for bundled LUTs. */
    public void restoreBundledVisibility()throws IOException{
        Context context=(Context)call(w.global,null);
        if(!context.getSharedPreferences("aurora_filter_workspace_v2",0).edit()
                .putBoolean("restore-bundled:MiCamStyleRear",true).putBoolean("restore-bundled:MiCamStyleFront",true).commit())
            throw new IOException("内置滤镜显示状态未能保存");
    }
    private void restoreVisibility(Object workspace,Context context,String kind,LutStore229 store)throws IOException{
        SharedPreferences prefs=context.getSharedPreferences("aurora_filter_workspace_v2",0);String pending="restore-bundled:"+kind;
        if(!prefs.getBoolean(pending,false))return;
        Set<String> hidden=w.hidden(workspace);
        for(Object row:w.raw(workspace)){int slot=ownedSlot(workspace,row);if(slot>0&&store.bundled(slot))hidden.remove((String)read(w.identity,row));}
        if(!prefs.edit().putStringSet("hidden:"+kind,hidden).remove(pending).commit())throw new IOException("内置滤镜显示状态未能刷新");
        w.changed();
    }
    public void refreshLoaded(){
        CamStyleAccess260.lock();try{
        Object workspace=call(w.obtain,call(w.provider,null),w.frontFacing()?w.front:w.rear);
        int mode=w.currentMode();
        if(eligible(workspace,mode))call(bind,workspace,mode);
            }finally{CamStyleAccess260.unlock();}
    }
    private Map<String,Object> firstItems(List<?> values){
        Map<String,Object> index=new HashMap<>();
        for(Object value:values)index.putIfAbsent((String)read(itemValue,value),value);
        return index;
    }
    @SuppressWarnings("unchecked") private void bindIndexed(Object owner,int mode)throws IllegalAccessException{
        Object component=filter();String key=(String)call(componentKey,component,mode);
        List<?> values=(List<?>)call(componentItems,component);
        if(values.isEmpty()){Log.e("BaseCamStyleWorkspace","filterList cloud empty");call(initialize,component,mode);values=(List<?>)call(componentItems,component);}
        Log.d("BaseCamStyleWorkspace","fillLocalFilterComponentDataItem: "+values.size());
        List<Object> rows=(List<Object>)call(w.getList,owner);Object first=values.get(0);
        boolean retain=(Boolean)call(retainUnsupported,null);Map<String,Object> index=firstItems(values);
        List<Object> removed=new ArrayList<>();
        for(Object row:rows){
            if(read(binding,row)!=null)continue;
            String value=(String)call(parameter,row,key);Object item=index.get(value);
            if(item!=null)binding.set(row,item);
            else if((Boolean)read(w.official,row)&&(Boolean)call(unsupported,null,mode,value)&&!retain)removed.add(row);
            else binding.set(row,first);
        }
        for(Object row:removed)call(delete,row,true);
        rows.removeAll(removed);
    }
    @SuppressWarnings("unchecked") private boolean reloadIndexed(Object owner,int mode,Context context,Object cloud)throws ReflectiveOperationException{
        Object component=filter();call(cloudInitialize,component,mode,cloud);
        List<?> values=(List<?>)call(componentItems,component);
        if(values.isEmpty()){Log.e("BaseCamStyleWorkspace","filterList reloadFromFilterData empty");call(initialize,component,mode);values=(List<?>)call(componentItems,component);}
        Log.d("BaseCamStyleWorkspace","reloadFromFilterData: "+values.size());
        List<Object> rows=(List<Object>)call(w.getList,owner);Set<String> present=new HashSet<>();
        for(Object row:rows)if((Boolean)read(w.official,row))present.add((String)call(parameter,row,FILTER));
        List<Object> added=new ArrayList<>();
        // Native Q computes every missing item before creating any rows.
        for(Object item:values)if(!present.contains((String)read(itemValue,item)))added.add(item);
        int order=-1;for(Object row:rows)order=Math.max(order,(Integer)read(w.order,row));
        for(Object item:added){
            Object row=call(createNative,owner,Integer.parseInt((String)read(itemValue,item)),context,"p_"+FILTER+"_",mode,item);
            call(w.setOrder,row,++order);w.raw(owner).add(row);
        }
        rows=(List<Object>)call(w.getList,owner);Map<String,Object> index=firstItems(values);
        List<Object> removed=new ArrayList<>();
        for(Object row:rows)if((Boolean)read(w.official,row)){
            Object item=index.get((String)call(parameter,row,FILTER));
            if(item==null)removed.add(row);else binding.set(row,item);
        }
        for(Object row:removed)call(delete,row,true);
        rows.removeAll(removed);
        if(added.isEmpty()&&removed.isEmpty())return false;
        for(int i=0;i<rows.size();i++)call(w.setOrder,rows.get(i),i);
        // Same K-order comparison used by the existing R2 comparator hooks.
        rows.sort(Comparator.comparingInt(row->(Integer)read(w.order,row)));
        return true;
    }
    public void install(){
        // Both local initialization and the cloud d() branch replace mItems.
        // Q() consumes that replacement immediately, without calling f().
        for(Method method:new Method[]{initialize,cloudInitialize})CamStyleAccess260.guard(method,filterType);
        for(Method method:new Method[]{initialize,cloudInitialize})XposedBridge.hookMethod(method,new StrictHook(75){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(!p.hasThrowable()&&filterType.isInstance(p.thisObject))projectComponent(p.thisObject,(Integer)p.args[0]);
            }
        });
        // Between R2's default-priority normalization and workspace visibility's
        // priority-100 after callback, which requests a main-thread refresh.
        for(Method method:new Method[]{bind,reload})XposedBridge.hookMethod(method,new StrictHook(75){
            private boolean observed;
            @Override protected void before(MethodHookParam p)throws Throwable{
                int mode=(Integer)p.args[0];if(Boolean.TRUE.equals(synchronizing.get()))return;
                p.setObjectExtra(SCOPE+".rows",w.raw(p.thisObject).size());
                if(!eligible(p.thisObject,mode))return;
                p.setObjectExtra(SCOPE,true);synchronizing.set(true);p.setObjectExtra(SCOPE+".removed",prepare(p.thisObject,mode));
            }
            @Override protected void after(MethodHookParam p)throws Throwable{
                boolean eligible=p.getObjectExtra(SCOPE)!=null;
                try{
                    if(eligible&&!p.hasThrowable()){
                        append(p.thisObject,(Integer)p.args[0]);
                        @SuppressWarnings("unchecked") List<FilterWorkspace215.Entry> removed=(List<FilterWorkspace215.Entry>)p.getObjectExtra(SCOPE+".removed");
                        for(FilterWorkspace215.Entry entry:removed)w.beforeDelete(p.thisObject,entry,(Context)call(w.global,null));
                        if(method.equals(reload)&&(Integer)p.getObjectExtra(SCOPE+".rows")!=w.raw(p.thisObject).size())p.setResult(true);
                    }
                }finally{
                    if(eligible)synchronizing.remove();
                    if(!observed&&p.getObjectExtra(SCOPE+".rows")!=null){
                        observed=true;Log.i("AuroraCameraCompat","lut237 "+method.getName()+" mode="+p.args[0]+" workspace="+p.thisObject.getClass().getName()+" eligible="+eligible+" rows="+p.getObjectExtra(SCOPE+".rows")+"->"+w.raw(p.thisObject).size()+" slots="+runtime.store().slots().size());
                    }
                }
            }
        });
        // Replace only the body, after all existing projection/maintenance before hooks.
        XposedBridge.hookMethod(bind,new StrictHook(-1000){
            @Override protected void before(MethodHookParam p)throws Throwable{
                if((Integer)p.args[0]!=163||!w.camStyle.isInstance(p.thisObject))return;
                bindIndexed(p.thisObject,(Integer)p.args[0]);p.setResult(null);
            }
        });
        XposedBridge.hookMethod(reload,new StrictHook(-1000){
            @Override protected void before(MethodHookParam p)throws Throwable{
                if((Integer)p.args[0]!=163||!w.camStyle.isInstance(p.thisObject))return;
                p.setResult(reloadIndexed(p.thisObject,(Integer)p.args[0],(Context)p.args[1],p.args[2]));
            }
        });
    }
}
