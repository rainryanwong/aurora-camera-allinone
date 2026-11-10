package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.IOException;
import java.util.*;
import com.aurora.camera.compat.filter.FilterWorkspace215.Entry;

/** Independent video visibility and order, stored alongside the photo workspaces. */
public final class VideoFilterWorkspace270 {
    public static final String KIND="Video";
    private final LutRuntime229 runtime;
    private final Context context;
    public VideoFilterWorkspace270(LutRuntime229 runtime,Context context){this.runtime=runtime;this.context=context;}
    private SharedPreferences prefs(){return context.getSharedPreferences("aurora_filter_workspace_v2",0);}
    public static String identity(int value){int slot=ImportedVideoLut256.slot(value);return slot>0?LutStore229.token(slot):"video_"+value;}
    public Set<String> hidden(){return new HashSet<>(prefs().getStringSet("hidden:"+KIND,Collections.emptySet()));}
    public boolean enabled(Entry entry,Set<String> hidden){int slot=ImportedVideoLut256.slot((Integer)entry.nativeRow);return entry.fixed||FilterWorkspace215.shown(hidden,entry.id,slot==0||runtime.store().enabled(slot));}
    public List<Entry> snapshot()throws IOException{
        try{
            List<Entry> rows=runtime.video().managementEntries(context);
            LutStore229 store=runtime.store();
            for(int slot:store.configuredSlots())rows.add(new Entry(ImportedVideoLut256.value(slot),LutStore229.token(slot),store.label(LutStore229.token(slot),store.name(slot)),false,true));
            arrange(rows,false);return rows;
        }catch(ReflectiveOperationException failure){throw new IOException("无法读取视频滤镜目录",failure);}
    }
    public void arrange(List<Entry> rows,boolean defaults)throws IOException{
        synchronized(VideoFilterWorkspace270.class){
            String saved=defaults?null:prefs().getString("order:"+KIND,null);
            LinkedHashSet<String> modules=new LinkedHashSet<>();
            Set<Integer> configured=new LinkedHashSet<>(runtime.store().configuredSlots());
            for(WorkspaceFilterOrder270.Row row:WorkspaceFilterOrder270.cached(context,"MiCamStyleRear")){
                int slot=runtime.slot(row.filter);if(row.official&&configured.remove(slot))modules.add(LutStore229.token(slot));
            }
            for(int slot:configured)modules.add(LutStore229.token(slot));
            for(Entry row:rows)if(!row.fixed)modules.add(row.id);
            boolean migrate=saved!=null&&!prefs().getBoolean("looks-first:"+KIND,false)&&sameExistingOrder(saved,modules);
            if(saved==null||migrate){
                LinkedHashSet<String> order=new LinkedHashSet<>();List<Entry> looks=new ArrayList<>();
                for(Entry row:rows)if(!row.fixed&&LeicaLooks273.videoRank((Integer)row.nativeRow)<Integer.MAX_VALUE)looks.add(row);
                looks.sort(Comparator.comparingInt(row->LeicaLooks273.videoRank((Integer)row.nativeRow)));for(Entry row:looks)order.add(row.id);
                order.addAll(modules);saved=String.join("\n",order);
                SharedPreferences.Editor change=prefs().edit().putString("order:"+KIND,saved);
                if(defaults)change.remove("hidden:"+KIND);
                if(!change.commit())throw new IOException("视频滤镜独立顺序未能保存");
            }
            if(!prefs().getBoolean("looks-first:"+KIND,false)&&!prefs().edit().putBoolean("looks-first:"+KIND,true).commit())throw new IOException("Leica Looks 视频顺序未能保存");
            Map<String,Integer> ranks=new HashMap<>();
            for(String id:saved.split("\n"))ranks.putIfAbsent(id,ranks.size());
            rows.sort(Comparator.comparingInt((Entry row)->row.fixed?-1:ranks.getOrDefault(row.id,Integer.MAX_VALUE)));
        }
    }
    private static boolean sameExistingOrder(String saved,Set<String> defaults){
        List<String> previous=new ArrayList<>();for(String id:saved.split("\n"))if(defaults.contains(id))previous.add(id);
        Set<String> present=new HashSet<>(previous);List<String> expected=new ArrayList<>();for(String id:defaults)if(present.contains(id))expected.add(id);
        return previous.equals(expected);
    }
    public void apply(List<Entry> rows,Set<String> hidden)throws IOException{
        List<String> ids=new ArrayList<>();for(Entry row:rows){if(row.fixed&&hidden.contains(row.id))throw new IOException("原图不能停用");ids.add(row.id);}
        if(!prefs().edit().putString("order:"+KIND,String.join("\n",ids)).putStringSet("hidden:"+KIND,new HashSet<>(hidden)).commit())throw new IOException("视频滤镜设置未能保存");
        try{runtime.video().refreshLoaded();}catch(ReflectiveOperationException failure){throw new IOException("视频滤镜列表刷新失败",failure);}
    }
    public void reset()throws IOException{
        try{arrange(runtime.video().managementEntries(context),true);runtime.video().refreshLoaded();}catch(ReflectiveOperationException failure){throw new IOException("视频滤镜列表刷新失败",failure);}
    }
}
