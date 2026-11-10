package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.zip.ZipFile;
import org.json.*;

/** Phoenix's preload and native-gallery entities share the ordinary imported LUT store. */
public final class BundledFilterCatalog256 {
    private static final String SEEDED="bundled:phoenix130:seeded";
    private static final String ROOT="assets/phoenix256/";

    static synchronized void install(Context context,SharedPreferences prefs,String moduleApkPath)throws IOException{
        if(prefs.getBoolean(SEEDED,false))return;
        restore(context,prefs,moduleApkPath,(done,total,name)->{});
    }
    static synchronized int restore(Context context,SharedPreferences prefs,String moduleApkPath,CustomFilterStore253.Progress progress)throws IOException{
        try(ZipFile module=new ZipFile(moduleApkPath)){
            JSONArray items=new JSONObject(new String(read(module,ROOT+"catalog.json"),StandardCharsets.UTF_8)).getJSONArray("items");
            Map<String,Integer> existing=new HashMap<>();
            for(int slot=1;slot<=LutStore229.SLOTS;slot++){
                String raw=prefs.getString("slot:"+slot,null);if(raw==null)continue;JSONObject item=new JSONObject(raw);
                if("phoenix130".equals(item.optString("bundled")))existing.putIfAbsent(item.getString("sourceId"),slot);
            }
            int missing=0;for(int i=0;i<items.length();i++)if(!existing.containsKey(items.getJSONObject(i).getString("sourceId")))missing++;
            List<Integer> free=new ArrayList<>();for(int i=1;i<=LutStore229.SLOTS;i++)if(!prefs.contains("slot:"+i))free.add(i);
            if(free.size()<missing)throw new IOException("恢复内置滤镜需要 "+missing+" 个空闲 LUT 槽位，当前剩余 "+free.size()+" 个");
            File directory=new File(context.getFilesDir(),"aurora_luts229");Files.createDirectories(directory.toPath());
            SharedPreferences.Editor edit=prefs.edit();long created=System.currentTimeMillis();int next=0;
            for(int i=0;i<items.length();i++){
                JSONObject source=items.getJSONObject(i);String revision=source.getString("revision"),sourceId=source.getString("sourceId");
                Integer assigned=existing.get(sourceId);int slot=assigned==null?free.get(next++):assigned;
                File output=new File(directory,revision+".png");
                java.nio.file.Path temporary=Files.createTempFile(directory.toPath(),"bundled-",".png");
                try{Files.write(temporary,read(module,ROOT+"luts/"+revision+".png"));Files.move(temporary,output.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                finally{Files.deleteIfExists(temporary);}
                JSONObject item=new JSONObject().put("name",source.getString("name")).put("size",64).put("created",created)
                        .put("revision",revision).put("file",output.getName()).put("bundled","phoenix130")
                        .put("sourceId",sourceId).put("origin",source.getString("origin")).put("projection",source.getString("projection"))
                        .put("enabled",source.getBoolean("enabled"));
                edit.putString("slot:"+slot,item.toString()).remove("label:"+LutStore229.token(slot)).putString("effect:"+LutStore229.token(slot),source.getInt("grain")+":"+source.getInt("diffusion"));
                progress.update(i+1,items.length(),source.getString("name"));
            }
            if(!edit.putBoolean(SEEDED,true).commit())throw new IOException("内置滤镜未能保存");
            return items.length();
        }catch(JSONException invalid){throw new IOException("无法读取内置滤镜目录",invalid);}
    }
    private static byte[] read(ZipFile module,String path)throws IOException{
        java.util.zip.ZipEntry entry=module.getEntry(path);if(entry==null)throw new FileNotFoundException(path);
        try(InputStream input=module.getInputStream(entry);ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[16384];for(int n;(n=input.read(buffer))!=-1;)output.write(buffer,0,n);return output.toByteArray();
        }
    }
    private BundledFilterCatalog256(){}
}
