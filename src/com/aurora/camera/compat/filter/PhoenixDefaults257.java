package com.aurora.camera.compat.filter;

import android.content.Context;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.filter.FilterWorkspace215.Entry;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** Phoenix 1.3.0's canonical order and visibility, using the host's native catalog. */
public final class PhoenixDefaults257 {
    private static final String FILTER="pref_camera_shader_coloreffect_key";
    private static final String PREFS="aurora_filter_workspace_v2",NATIVE="phoenix-native-defaults:";
    // FilterContentContract.ENTITY_IDS. DEFAULT_VISIBLE is true for indices 0..55.
    private static final String[] CANONICAL=(
            "L_BRIGHT,L_NATURAL,LC_CHR,LC_CLS,LC_CNT,LC_ETN,LC_BRS,LC_TEL,L_DARK,L_DARK_HIGH,LC_IA,L_BLUE,LUT_CC,LUT_NC,"+
            "GALLERY_9001,GALLERY_9002,GALLERY_9003,GALLERY_9004,GALLERY_9005,GALLERY_9006,GALLERY_9007,GALLERY_9008,GALLERY_9009,GALLERY_9010,GALLERY_9011,preset:rev11-012,"+
            "GALLERY_9012,preset:rev11-013,GALLERY_9013,preset:rev11-035,preset:rev11-036,preset:rev11-037,preset:rev11-004,preset:rev11-005,preset:rev11-006,preset:rev11-007,"+
            "preset:rev11-008,preset:rev11-010,preset:rev11-011,preset:rev11-001,preset:rev11-002,preset:rev11-003,preset:rev11-015,preset:rev11-016,"+
            "preset:071-01,preset:071-02,preset:071-03,preset:071-04,preset:071-05,preset:071-06,preset:071-07,preset:071-08,preset:071-09,preset:071-10,preset:071-11,preset:071-12,"+
            "N_H_400,N_KG_200,N_NATURE,N_KC_64,N_BLACKGOLD,N_KP_160,N_LIVELY,N_C_50D,N_F_50,N_FLOWER_DREAM,N_FC_400,N_V_250,"+
            "GALLERY_102,GALLERY_25,GALLERY_103,GALLERY_104,GALLERY_105,GALLERY_5,GALLERY_6,GALLERY_13,GALLERY_14,GALLERY_101,GALLERY_100,GALLERY_99,GALLERY_97,GALLERY_98,"+
            "GALLERY_18,GALLERY_19,GALLERY_20,GALLERY_21,GALLERY_22,GALLERY_23,GALLERY_24,GALLERY_29,GALLERY_9,GALLERY_96,GALLERY_26,GALLERY_27,GALLERY_28,GALLERY_32,GALLERY_33,"+
            "GALLERY_34,GALLERY_35,GALLERY_36,GALLERY_11,GALLERY_12,GALLERY_90,GALLERY_91,GALLERY_86,GALLERY_38,GALLERY_39,GALLERY_40,GALLERY_41,GALLERY_87,GALLERY_88,"+
            "GALLERY_62,GALLERY_63,GALLERY_64,GALLERY_65,GALLERY_106,GALLERY_107,GALLERY_108,GALLERY_109,GALLERY_122,GALLERY_110,GALLERY_111,GALLERY_112,GALLERY_113,GALLERY_115,"+
            "GALLERY_42,GALLERY_43,GALLERY_44,GALLERY_45,GALLERY_46,GALLERY_48,GALLERY_50,GALLERY_51,GALLERY_55,GALLERY_56,GALLERY_7,GALLERY_16,GALLERY_66,GALLERY_17,"+
            "GALLERY_68,GALLERY_69,GALLERY_71,GALLERY_72,GALLERY_73,GALLERY_74,GALLERY_75,GALLERY_76,GALLERY_83,GALLERY_80,GALLERY_82,GALLERY_78,GALLERY_85,GALLERY_84,GALLERY_79,GALLERY_8,GALLERY_81").split(",");
    private static final Map<String,Integer> RANKS=new HashMap<>();
    static{for(int i=0;i<CANONICAL.length;i++)RANKS.put(CANONICAL[i],i);}
    private final LutRuntime229 runtime;
    private final FilterWorkspace215 workspace;
    private final LutWorkspace237 imported;
    private final Field parameters;
    private final NativeCatalog nativeCatalog;

    public PhoenixDefaults257(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        this(host,runtime,new FilterWorkspace215(host,runtime),new LutWorkspace237(host,runtime));
    }
    PhoenixDefaults257(Host host,LutRuntime229 runtime,FilterWorkspace215 workspace,LutWorkspace237 imported)throws ReflectiveOperationException{
        this.runtime=runtime;this.workspace=workspace;this.imported=imported;parameters=host.field("U9.S","e");nativeCatalog=new NativeCatalog(host,runtime);
    }
    /** Keep user imports and recipes; deleted module resources use their own restore action. */
    public void reset(Object owner,Context context)throws IOException{
        String pending="phoenix-defaults:"+workspace.kind(owner);
        if(!context.getSharedPreferences(PREFS,0).edit().putBoolean(pending,true).putBoolean(NATIVE+workspace.kind(owner),true).commit())throw new IOException("默认排序请求未能保存");
        try{runtime.refresh();}catch(ReflectiveOperationException failure){throw new IOException("默认滤镜显示未能刷新",failure);}
        applyOrder(owner,context);
        if(workspace.front.isInstance(owner)==workspace.frontFacing())clearPending(context,pending);
    }
    /** Keep native-supported canonical items available across local and cloud reloads. */
    public void projectComponent(Object component,int mode)throws IOException{
        if(mode!=163)return;
        Context context=(Context)call(workspace.global,null);
        String kind=workspace.frontFacing()?"MiCamStyleFront":"MiCamStyleRear";
        nativeCatalog.project(component,mode,context.getSharedPreferences(PREFS,0).getBoolean(NATIVE+kind,false));
    }
    public void projected(Object owner,Context context,String kind)throws IOException{
        CamStyleAccess260.lock();try{
        String key="phoenix-defaults:"+kind;
        boolean pending=context.getSharedPreferences(PREFS,0).getBoolean(key,false);
        if(!pending&&!context.getSharedPreferences(PREFS,0).getBoolean(NATIVE+kind,false)){
            List<Object> added=nativeCatalog.append(owner,workspace,context,parameters,false);
            if(!added.isEmpty())workspace.apply(owner,workspace.snapshot(owner,context),workspace.hidden(owner));
            initializeLooksOrder(owner,context,kind);return;
        }
        if(pending)applyOrder(owner,context);else appendNative(owner,context);
        if(pending)clearPending(context,key);
        initializeLooksOrder(owner,context,kind);
        }finally{CamStyleAccess260.unlock();}
    }
    private void initializeLooksOrder(Object owner,Context context,String kind)throws IOException{
        android.content.SharedPreferences prefs=context.getSharedPreferences(PREFS,0);String key="looks-first:"+kind;
        if(prefs.getBoolean(key,false))return;
        List<Entry> rows=workspace.snapshot(owner,context);List<String> current=new ArrayList<>();for(Entry row:rows)current.add(row.id);
        String initial=prefs.getString("initial:"+kind,null);
        boolean customized=initial!=null&&!WorkspaceOrder215.restore(current,Arrays.asList(initial.split("\n",-1))).equals(current);
        if(!customized){
            rows.sort(Comparator.comparingInt((Entry row)->row.fixed?-1:looksRank(row)));
            workspace.apply(owner,rows,workspace.hidden(owner));
        }
        if(!prefs.edit().putBoolean(key,true).commit())throw new IOException("Leica Looks 顺序未能保存");
    }
    private int looksRank(Entry row){
        if(!row.builtIn)return Integer.MAX_VALUE;
        Object value=((Map<?,?>)read(parameters,row.nativeRow)).get(FILTER);
        return value instanceof String?LeicaLooks273.rank(Integer.parseInt((String)value)):Integer.MAX_VALUE;
    }
    private static void clearPending(Context context,String key)throws IOException{
        if(!context.getSharedPreferences(PREFS,0).edit().remove(key).commit())throw new IOException("默认排序状态未能保存");
    }
    private void applyOrder(Object owner,Context context)throws IOException{
        CamStyleAccess260.lock();try{
        appendNative(owner,context);
        LutStore229 store=runtime.store();
        List<Entry> rows=workspace.snapshot(owner,context);Set<String> hidden=workspace.hidden(owner);
        Map<String,Integer> ranks=new HashMap<>();
        for(Entry entry:rows){
            Integer rank=RANKS.get(source(owner,entry,store));
            ranks.put(entry.id,entry.fixed?-1:rank==null?CANONICAL.length:rank);
            if(rank!=null){FilterWorkspace215.setVisibility(hidden,entry.id,rank<56);}
            else if(!entry.fixed&&entry.builtIn&&imported.ownedSlot(owner,entry.nativeRow)==0)hidden.add(entry.id);
        }
        rows.sort(Comparator.comparingInt(entry->ranks.get(entry.id)));
        if(workspace.front.isInstance(owner))imported.frontOrder(rows,context);
        workspace.apply(owner,rows,hidden);
        }finally{CamStyleAccess260.unlock();}
    }
    private void appendNative(Object owner,Context context)throws IOException{
        if(workspace.front.isInstance(owner)!=workspace.frontFacing()||(Integer)read(workspace.mode,owner)!=163)return;
        List<Object> added=nativeCatalog.append(owner,workspace,context,parameters,true);
        String knownKey=NATIVE+"seen:"+workspace.kind(owner);
        Set<String> known=new HashSet<>(context.getSharedPreferences(PREFS,0).getStringSet(knownKey,Collections.emptySet()));
        Set<String> hidden=workspace.hidden(owner);
        boolean learned=false;
        for(Entry entry:workspace.snapshot(owner,context)){
            if(entry.fixed||!entry.builtIn||imported.ownedSlot(owner,entry.nativeRow)>0||!known.add(entry.id))continue;
            learned=true;Integer rank=RANKS.get(source(owner,entry,runtime.store()));
            if(rank==null||rank>=56)hidden.add(entry.id);else hidden.remove(entry.id);
        }
        if(!added.isEmpty()||learned)workspace.apply(owner,workspace.snapshot(owner,context),hidden);
        // New cloud official rows receive defaults once; later user edits survive reloads.
        if(learned&&!context.getSharedPreferences(PREFS,0).edit().putStringSet(knownKey,known).commit())throw new IOException("原生默认滤镜状态未能保存");
    }
    private String source(Object owner,Entry entry,LutStore229 store){
        if(entry.fixed)return null;
        int slot=imported.ownedSlot(owner,entry.nativeRow);
        if(slot>0)return store.bundled(slot)?store.sourceId(slot):null;
        if(!entry.builtIn)return null;
        Object value=((Map<?,?>)read(parameters,entry.nativeRow)).get(FILTER);
        return value==null?null:runtime.identity(Integer.parseInt((String)value));
    }

    private static final class NativeCatalog {
        private final LutRuntime229 runtime;
        private final Method repository,component,controller,catalog,infoId,mapperGet,map,root,rowType,create,parse;
        private final Field items,category,capabilities,mapperProvider,itemValue,itemName,itemLabel,binding;
        private final Class<?> filterType;
        NativeCatalog(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
            this.runtime=runtime;repository=host.method("g2.a","a");component=host.method("Xh.b","x",Class.class);filterType=host.type("r2.t");
            controller=host.method("com.xiaomi.camera.effect.EffectController","u");catalog=host.method("com.xiaomi.camera.effect.EffectController","q",int.class);infoId=host.method("i3.b","a");
            mapperProvider=host.field("i2.a","a");mapperGet=host.method("i2.b","c");map=host.method("j2.d","j",int.class,ArrayList.class,host.type("l9.g"));
            items=host.field("com.android.camera.data.data.c","mItems");category=host.field("r2.a","a");capabilities=host.field("v2.U","b");
            itemValue=host.field("com.android.camera.data.data.d","r");itemName=host.field("com.android.camera.data.data.d","l");itemLabel=host.field("com.android.camera.data.data.d","o");binding=host.field("com.android.camera.features.mode.capture.e","M");
            root=host.method("U9.a","getWorkspaceDir");rowType=host.method("U9.a","n");
            create=host.method("U9.S","f",String.class,Class.class,String.class,boolean.class,String.class,String.class,int.class,boolean.class,String[].class);
            parse=host.method("U9.S","N",int.class,String.class,Class.class,String.class);
        }
        private Object filter(){return call(component,call(repository,null),filterType);}
        @SuppressWarnings("unchecked") void project(Object owner,int mode,boolean full)throws IOException{
            int cat=(Integer)read(category,owner);if(cat!=2&&cat!=10)return;
            List<Object> current=(List<Object>)read(items,owner);if(current==null||current.isEmpty())return;
            Set<Integer> present=new HashSet<>();for(Object item:current)present.add(Integer.parseInt((String)read(itemValue,item)));
            ArrayList<Object> missing=new ArrayList<>();List<?> available=(List<?>)call(catalog,call(controller,null),cat);if(available==null)return;
            for(Object info:available){int id=(Integer)call(infoId,info);if(runtime.slot(id)==0&&(LeicaLooks273.contains(id)||full&&RANKS.containsKey(runtime.identity(id)))&&!present.contains(id))missing.add(info);}
            if(missing.isEmpty())return;
            Object mapper=call(mapperGet,read(mapperProvider,null));List<?> mapped=(List<?>)call(map,mapper,mode,missing,read(capabilities,owner));
            ArrayList<Object> next=new ArrayList<>(current);next.addAll(mapped);
            try{items.set(owner,next);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
        }
        @SuppressWarnings("unchecked") List<Object> append(Object owner,FilterWorkspace215 workspace,Context context,Field parameters,boolean full)throws IOException{
            int mode=(Integer)read(workspace.mode,owner);
            if(mode!=163||workspace.front.isInstance(owner)!=workspace.frontFacing())return Collections.emptyList();
            Object filter=filter();project(filter,mode,full);
            List<Object> rows=workspace.raw(owner),added=new ArrayList<>();Map<Integer,Object> existing=new HashMap<>();
            int order=-1;for(Object row:rows){order=Math.max(order,(Integer)read(workspace.order,row));if((Boolean)read(workspace.official,row)){
                Object id=((Map<?,?>)read(parameters,row)).get(FILTER);if(id instanceof String)existing.put(Integer.parseInt((String)id),row);
            }}
            for(Object item:(List<Object>)read(items,filter)){
                String value=(String)read(itemValue,item);int id=Integer.parseInt(value);
                if(runtime.slot(id)>0||!(LeicaLooks273.contains(id)||full&&RANKS.containsKey(runtime.identity(id))))continue;
                Object row=existing.get(id);
                if(row==null){
                    int resource=(Integer)read(itemName,item);String label=resource==-1?(String)read(itemLabel,item):context.getString(resource);
                    String kind=workspace.kind(owner),base=(String)call(root,owner);Class<?> type=(Class<?>)call(rowType,owner);
                    File folder=new File(base,kind+"_official_"+value+"_"+label);
                    if(folder.exists()){
                        row=call(parse,null,mode,kind,type,folder.getAbsolutePath());if(row==null)throw new IOException("无法恢复原生滤镜条目："+label);
                    }else{
                        row=call(create,null,kind,type,base,false,value,label,mode,false,new String[]{"p_"+FILTER+"_"+value});
                        call(workspace.setOrder,row,++order);
                    }
                    rows.add(row);existing.put(id,row);added.add(row);
                }
                try{binding.set(row,item);}catch(IllegalAccessException failure){throw new IllegalStateException(failure);}
            }
            return added;
        }
    }
}
