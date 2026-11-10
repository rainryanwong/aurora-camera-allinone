package com.aurora.camera.compat.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.os.SystemClock;
import android.util.SparseArray;
import com.aurora.camera.compat.filter.LutStore229;
import com.aurora.camera.compat.vignette.AuthBody241;
import com.aurora.camera.compat.vignette.AuthReadiness239;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Settings and the private user assets referenced by the stock-1770 camera. */
public final class CameraSettingsBackup253 {
    private static final String FORMAT="aurora-camera-settings", STOCK="e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07";
    private static final String[] STORES={"camera_settings_global","camera_settings_workspace","camera_settings_live",
        "com.android.camera_preferences","watermark_setting","video_watermark_setting","aurora_features229","aurora_classic239","phoenix_vignette",
        "aurora_soft_light245","aurora_street_ui217","aurora_watermark_names","aurora_filter_manager_v1",
        "aurora_filter_workspace_v2","aurora_luts229","aurora_shared_recipes219"};
    private static final String[] ROOTS={"Workspace/MiCamStyleRear","Workspace/MiCamStyleFront","aurora_luts229"};
    private static final Set<String> LOCAL_ONLY=new HashSet<>(Arrays.asList("pref_camera_first_use_permission_shown_key",
        "pref_ai_service_privacy","pref_first_ai_service_confirm_shown_key","user_agree","material_download_state","supported_watermark_list",
        "pref_version_key","pref_device_name_key","pref_camera_global_guide_shown_key","pref_camera_global_guide_count_key",
        "pref_watermark_clear_mivi_data_key","pref_video_watermark_clear_mivi_data_key"));
    private static final long MAX_BYTES=512L*1024*1024;
    private CameraSettingsBackup253(){}

    /** The caller owns the stream. Run on the settings I/O worker. */
    public static synchronized String write(Context context,OutputStream output)throws Exception{
        Context app=context.getApplicationContext();
        List<Object> items=dataItems(app);
        flush(items);
        JSONObject metadata=new JSONObject().put("format",FORMAT).put("version",1).put("camera",STOCK).put("created",System.currentTimeMillis());
        JSONObject stores=new JSONObject();int keys=0;
        for(String name:storeNames(app)){
            Map<String,Object> values=settings(app.getSharedPreferences(name,0).getAll());keys+=values.size();stores.put(name,encode(values));
        }
        metadata.put("preferences",stores);
        SortedSet<String> roots=assetRoots(app);metadata.put("roots",new JSONArray(roots));
        JSONObject auth=auth(app);metadata.put("auth",auth);
        validateLuts(app.getSharedPreferences("aurora_luts229",0).getAll(),app.getFilesDir());
        File base=app.getFilesDir();int files=0;long[] bytes={0};
        ZipOutputStream zip=new ZipOutputStream(new NonClosingOutput(output));
        try{
            put(zip,"settings.json",metadata.toString().getBytes(StandardCharsets.UTF_8));
            for(String root:roots){File directory=inside(base,root);if(directory.exists())files+=archive(zip,base,directory,bytes);}
            zip.finish();zip.flush();
        }finally{zip.close();}
        return summary(keys,files);
    }

    public static synchronized String inspect(Context context,InputStream input)throws Exception{
        try(Backup backup=read(context.getApplicationContext(),input)){return summary(backup.keys(),backup.files);}
    }

    /** On success the UI must close the camera with finishForReload. */
    public static synchronized String restore(Context context,InputStream input)throws Exception{
        Context app=context.getApplicationContext();
        try(Backup backup=read(app,input)){
            List<Object> items=dataItems(app);flush(items);
            SortedSet<String> names=storeNames(app);names.addAll(backup.stores.keySet());
            Map<String,Map<String,Object>> previous=new LinkedHashMap<>();
            for(String name:names)previous.put(name,settings(app.getSharedPreferences(name,0).getAll()));
            SortedSet<String> roots=assetRoots(app);roots.addAll(backup.roots);
            File rollback=new File(backup.directory,"previous");Files.createDirectory(rollback.toPath());
            List<String> moved=new ArrayList<>(),installed=new ArrayList<>(),written=new ArrayList<>();
            JSONObject previousAuth=auth(app);boolean authChanged=false;
            try{
                if(backup.auth.has("active")&&!backup.auth.getString("active").equals(previousAuth.optString("active",null))){
                    authChanged=true;applyAuth(app,backup.auth.getString("active"));
                }
                for(String root:roots){
                    File target=inside(app.getFilesDir(),root),old=inside(rollback,root),next=inside(backup.assets,root);
                    if(target.exists()){Files.createDirectories(old.getParentFile().toPath());Files.move(target.toPath(),old.toPath());moved.add(root);}
                    if(next.exists()){Files.createDirectories(target.getParentFile().toPath());Files.move(next.toPath(),target.toPath());installed.add(root);}
                }
                for(String name:names){written.add(name);replace(app.getSharedPreferences(name,0),backup.stores.getOrDefault(name,Collections.emptyMap()));}
                writeCustomAuth(app,backup.auth);
                clearCaches(items);
            }catch(Exception failure){
                for(int i=written.size()-1;i>=0;i--){String name=written.get(i);try{replace(app.getSharedPreferences(name,0),previous.get(name));}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}}
                for(int i=installed.size()-1;i>=0;i--)try{remove(inside(app.getFilesDir(),installed.get(i)));}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}
                for(int i=moved.size()-1;i>=0;i--){String root=moved.get(i);try{File target=inside(app.getFilesDir(),root);Files.createDirectories(target.getParentFile().toPath());Files.move(inside(rollback,root).toPath(),target.toPath());}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}}
                try{writeCustomAuth(app,previousAuth);}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}
                if(authChanged&&previousAuth.has("active"))try{applyAuth(app,previousAuth.getString("active"));}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}
                try{clearCaches(items);}catch(Exception rollbackFailure){failure.addSuppressed(rollbackFailure);}
                if(failure.getSuppressed().length>0){backup.preserve=true;failure.addSuppressed(new IOException("恢复前的数据保留于："+backup.directory));}
                throw failure;
            }
            return summary(backup.keys(),backup.files);
        }
    }

    public static void finishForReload(Activity activity){
        activity.finishAffinity();
        // The camera also caches mode lists, workspace rows, LUTs and native shaders.
        Process.killProcess(Process.myPid());
    }

    private static String summary(int keys,int files){return keys+" 项设置，"+files+" 个自定义资源文件";}
    private static boolean store(String name){return Arrays.asList(STORES).contains(name)||name.matches("camera_settings_simple_mode_local_[0-9]+");}
    private static SortedSet<String> storeNames(Context context)throws IOException{
        SortedSet<String> result=new TreeSet<>(Arrays.asList(STORES));
        File directory=new File(context.getApplicationInfo().dataDir,"shared_prefs");File[] files=directory.listFiles();
        if(files==null&&directory.exists())throw new IOException("无法读取相机设置目录");
        if(files!=null)for(File file:files){String name=file.getName();if(name.endsWith(".xml")){name=name.substring(0,name.length()-4);if(store(name))result.add(name);}}
        return result;
    }
    private static boolean userSetting(String key){
        String lower=key.toLowerCase(Locale.ROOT);
        return !LOCAL_ONLY.contains(key)&&!key.startsWith("pref_camera_open_time_")
            &&!lower.contains("permission")&&!lower.contains("access_token")&&!lower.contains("auth_token")
            &&!lower.contains("password")&&!lower.contains("credential")&&!lower.contains("account_id")
            &&!lower.endsWith("_hint_shown_key")&&!lower.endsWith("_hint_shown_key_bool");
    }
    private static Map<String,Object> settings(Map<String,?> all){
        Map<String,Object> result=new TreeMap<>();for(Map.Entry<String,?> entry:all.entrySet())if(userSetting(entry.getKey())){
            Object value=entry.getValue();result.put(entry.getKey(),value instanceof Set?new TreeSet<>((Set<String>)value):value);
        }return result;
    }
    private static JSONObject encode(Map<String,Object> values)throws JSONException,IOException{
        JSONObject result=new JSONObject();for(Map.Entry<String,Object> entry:values.entrySet()){
            Object value=entry.getValue();String type;
            if(value instanceof String)type="string";else if(value instanceof Boolean)type="boolean";
            else if(value instanceof Integer)type="int";else if(value instanceof Long)type="long";else if(value instanceof Float)type="float";
            else if(value instanceof Set)type="strings";else throw new IOException("不支持的设置值："+entry.getKey());
            result.put(entry.getKey(),new JSONObject().put("type",type).put("value",value instanceof Set?new JSONArray((Set<?>)value):value.toString()));
        }return result;
    }
    private static Map<String,Object> decode(JSONObject json)throws JSONException,IOException{
        Map<String,Object> values=new TreeMap<>();Iterator<String> keys=json.keys();
        while(keys.hasNext()){
            String key=keys.next();if(!userSetting(key))throw new IOException("备份包含非用户设置："+key);
            JSONObject item=json.getJSONObject(key);String type=item.getString("type");Object value;
            switch(type){
                case "string":value=item.getString("value");break;
                case "boolean":String bool=item.getString("value");if(!bool.equals("true")&&!bool.equals("false"))throw new IOException("布尔设置无效："+key);value=Boolean.parseBoolean(bool);break;
                case "int":value=Integer.parseInt(item.getString("value"));break;
                case "long":value=Long.parseLong(item.getString("value"));break;
                case "float":float number=Float.parseFloat(item.getString("value"));if(!Float.isFinite(number))throw new IOException("小数设置无效："+key);value=number;break;
                case "strings":Set<String> set=new TreeSet<>();JSONArray array=item.getJSONArray("value");for(int i=0;i<array.length();i++)set.add(array.getString(i));value=set;break;
                default:throw new IOException("设置类型无效："+type);
            }
            values.put(key,value);
        }return values;
    }
    private static void replace(SharedPreferences preferences,Map<String,Object> values)throws IOException{
        SharedPreferences.Editor editor=preferences.edit();
        for(String key:preferences.getAll().keySet())if(userSetting(key))editor.remove(key);
        for(Map.Entry<String,Object> entry:values.entrySet()){
            String key=entry.getKey();Object value=entry.getValue();
            if(value instanceof String)editor.putString(key,(String)value);else if(value instanceof Boolean)editor.putBoolean(key,(Boolean)value);
            else if(value instanceof Integer)editor.putInt(key,(Integer)value);else if(value instanceof Long)editor.putLong(key,(Long)value);
            else if(value instanceof Float)editor.putFloat(key,(Float)value);else if(value instanceof Set)editor.putStringSet(key,new HashSet<>((Set<String>)value));
            else throw new IOException("不支持的设置值："+key);
        }if(!editor.commit())throw new IOException("相机设置保存失败");
    }

    private static SortedSet<String> assetRoots(Context context)throws IOException{
        SortedSet<String> roots=new TreeSet<>(Arrays.asList(ROOTS));
        for(String kind:new String[]{"watermarks","video_watermarks"})findUserData(context.getFilesDir(),new File(context.getFilesDir(),kind),roots,0);
        return roots;
    }
    private static void findUserData(File base,File directory,Set<String> roots,int depth)throws IOException{
        if(!directory.exists()||depth>5)return;
        inside(base,relative(base,directory));
        if(directory.getName().equals("userData")){roots.add(relative(base,directory));return;}
        File[] children=directory.listFiles();if(children==null)throw new IOException("无法读取水印设置目录："+directory.getName());
        for(File child:children)if(child.isDirectory())findUserData(base,child,roots,depth+1);
    }
    private static boolean assetRoot(String path){
        if(Arrays.asList(ROOTS).contains(path))return true;
        return path.matches("(?:watermarks|video_watermarks)/(?:[^/]+/){0,4}userData");
    }
    private static int archive(ZipOutputStream zip,File base,File file,long[] bytes)throws IOException{
        String name=relative(base,file);inside(base,name);
        if(Files.isSymbolicLink(file.toPath()))throw new IOException("自定义资源不能包含符号链接："+name);
        if(file.isDirectory()){
            zip.putNextEntry(new ZipEntry("files/"+name+"/"));zip.closeEntry();
            File[] children=file.listFiles();if(children==null)throw new IOException("无法读取自定义资源："+name);
            Arrays.sort(children,Comparator.comparing(File::getName));int count=0;for(File child:children)count+=archive(zip,base,child,bytes);return count;
        }
        zip.putNextEntry(new ZipEntry("files/"+name));try(InputStream input=new FileInputStream(file)){copy(input,zip,bytes,MAX_BYTES);}zip.closeEntry();return 1;
    }
    private static String relative(File base,File file)throws IOException{return base.getCanonicalFile().toPath().relativize(file.getCanonicalFile().toPath()).toString().replace(File.separatorChar,'/');}
    private static File inside(File base,String path)throws IOException{
        List<String> components=Arrays.asList(path.split("/",-1));
        if(path.isEmpty()||path.startsWith("/")||path.contains("\\")||path.indexOf(0)>=0||components.contains("..")||components.contains(".")||components.contains(""))throw new IOException("备份资源路径无效："+path+"（目录："+base+"）");
        File root=base.getCanonicalFile(),target=new File(base,path).getCanonicalFile();
        if(!target.toPath().startsWith(root.toPath())||target.equals(root))throw new IOException("备份资源超出设置目录");return target;
    }
    private static void put(ZipOutputStream zip,String name,byte[] bytes)throws IOException{zip.putNextEntry(new ZipEntry(name));zip.write(bytes);zip.closeEntry();}
    private static void copy(InputStream input,OutputStream output,long[] count,long limit)throws IOException{
        byte[] buffer=new byte[32768];int read;while((read=input.read(buffer))!=-1){count[0]+=read;if(count[0]>limit)throw new IOException("备份资源超过大小限制");output.write(buffer,0,read);}
    }
    private static final class Backup implements AutoCloseable{
        final File directory,assets;final Map<String,Map<String,Object>> stores=new TreeMap<>();final SortedSet<String> roots=new TreeSet<>();JSONObject auth;int files;boolean preserve;
        Backup(Context context)throws IOException{directory=Files.createTempDirectory(context.getCacheDir().toPath(),"settings-253-").toFile();assets=new File(directory,"assets");Files.createDirectory(assets.toPath());}
        int keys(){int count=0;for(Map<String,Object> values:stores.values())count+=values.size();return count;}
        public void close()throws IOException{if(!preserve)remove(directory);}
    }
    private static Backup read(Context context,InputStream input)throws Exception{
        Backup backup=new Backup(context);
        try{
            JSONObject metadata=null;Set<String> entries=new HashSet<>();long[] bytes={0};
            ZipInputStream zip=new ZipInputStream(new NonClosingInput(input));
            try{
                ZipEntry entry;while((entry=zip.getNextEntry())!=null){
                    String name=entry.getName();if(!entries.add(name)||entries.size()>8192)throw new IOException("备份资源目录重复或过大");
                    if(name.equals("settings.json")){
                        ByteArrayOutputStream output=new ByteArrayOutputStream();copy(zip,output,new long[]{0},8L*1024*1024);
                        metadata=new JSONObject(new String(output.toByteArray(),StandardCharsets.UTF_8));
                    }else if(name.startsWith("files/")){
                        String path=name.substring(6);if(entry.isDirectory())path=path.substring(0,path.length()-1);
                        File target=inside(backup.assets,path);
                        if(entry.isDirectory())Files.createDirectories(target.toPath());else{
                            Files.createDirectories(target.getParentFile().toPath());try(OutputStream output=new FileOutputStream(target)){copy(zip,output,bytes,MAX_BYTES);}backup.files++;
                        }
                    }else throw new IOException("备份包含未知内容："+name);
                    zip.closeEntry();
                }
            }finally{zip.close();}
            if(metadata==null||!FORMAT.equals(metadata.getString("format"))||metadata.getInt("version")!=1||!STOCK.equals(metadata.getString("camera")))throw new IOException("请选择本相机导出的设置备份");
            JSONObject preferences=metadata.getJSONObject("preferences");Iterator<String> names=preferences.keys();
            while(names.hasNext()){String name=names.next();if(!store(name))throw new IOException("备份设置目录无效："+name);backup.stores.put(name,decode(preferences.getJSONObject(name)));}
            for(String name:STORES)if(!backup.stores.containsKey(name))throw new IOException("备份缺少相机设置："+name);
            JSONArray roots=metadata.getJSONArray("roots");for(int i=0;i<roots.length();i++){String root=roots.getString(i);inside(backup.assets,root);if(!assetRoot(root)||!backup.roots.add(root))throw new IOException("备份自定义资源目录无效："+root);}
            for(String root:ROOTS)if(!backup.roots.contains(root))throw new IOException("备份缺少自定义资源目录："+root);
            for(String entry:entries)if(entry.startsWith("files/")){
                String path=entry.substring(6);boolean allowed=false;for(String root:backup.roots)if(path.equals(root+"/")||path.startsWith(root+"/")){allowed=true;break;}
                if(!allowed)throw new IOException("备份包含设置范围外的资源："+path);
            }
            backup.auth=metadata.getJSONObject("auth");for(String key:new String[]{"active","custom"})if(backup.auth.has(key))AuthBody241.normalize(backup.auth.getString(key));
            validateLuts(backup.stores.get("aurora_luts229"),backup.assets);
            return backup;
        }catch(Exception failure){try{backup.close();}catch(IOException cleanup){failure.addSuppressed(cleanup);}throw failure;}
    }

    private static void validateLuts(Map<String,?> values,File files)throws IOException,JSONException{
        for(Map.Entry<String,?> entry:values.entrySet())if(entry.getKey().startsWith("slot:")){
            int slot=Integer.parseInt(entry.getKey().substring(5));if(slot<1||slot>LutStore229.SLOTS)throw new IOException("备份LUT槽位无效");
            if(!(entry.getValue() instanceof String))throw new IOException("备份LUT记录无效");
            String name=new JSONObject((String)entry.getValue()).getString("file");
            if(!name.equals(new File(name).getName())||!name.endsWith(".png"))throw new IOException("备份LUT文件名无效");
            if(!inside(new File(files,"aurora_luts229"),name).isFile())throw new IOException("备份缺少自定义LUT："+slot);
        }
    }

    private static JSONObject auth(Context context)throws IOException,JSONException{
        JSONObject result=new JSONObject();File root=new File(context.getFilesDir(),"phoenix-vignette");
        for(String[] item:new String[][]{{"active","colorDark.glsl"},{"custom","custom.glsl"}}){File file=new File(root,item[1]);if(file.isFile())result.put(item[0],AuthBody241.normalize(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8)));}
        return result;
    }
    private static void writeCustomAuth(Context context,JSONObject auth)throws IOException,JSONException{
        File target=new File(context.getFilesDir(),"phoenix-vignette/custom.glsl");
        if(auth.has("custom")){Files.createDirectories(target.getParentFile().toPath());atomic(target,AuthBody241.normalize(auth.getString("custom")));}
        else Files.deleteIfExists(target.toPath());
    }
    private static void atomic(File target,String text)throws IOException{
        File temporary=new File(target.getParentFile(),target.getName()+".settings253.tmp");
        try{try(FileOutputStream output=new FileOutputStream(temporary)){output.write(text.getBytes(StandardCharsets.UTF_8));output.getFD().sync();}Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        finally{Files.deleteIfExists(temporary.toPath());}
    }
    private static void applyAuth(Context context,String body)throws Exception{
        String issue=AuthReadiness239.check();if(issue!=null)throw new IOException(issue);
        File root=new File(context.getFilesDir(),"phoenix-vignette");String token=Long.toString(System.nanoTime());
        atomic(new File(root,"request.glsl"),AuthBody241.normalize(body));atomic(new File(root,"request"),token+" apply\n");
        long end=SystemClock.elapsedRealtime()+8000;
        while(SystemClock.elapsedRealtime()<end){File ack=new File(root,"ack");if(ack.isFile()){
            String answer=new String(Files.readAllBytes(ack.toPath()),StandardCharsets.UTF_8).trim();
            if(answer.startsWith(token+" ")){if(!answer.equals(token+" OK"))throw new IOException(answer.substring(token.length()+1));return;}
        }Thread.sleep(50);}throw new IOException("原生暗角公式服务未响应");
    }

    private static List<Object> dataItems(Context context)throws ReflectiveOperationException{
        Class<?> repository=Class.forName("g2.a",false,context.getClassLoader());Object provider=repository.getDeclaredMethod("i").invoke(null);
        List<Object> result=new ArrayList<>();
        for(String name:new String[]{"b","c","d","e"}){Field field=provider.getClass().getDeclaredField(name);field.setAccessible(true);Object item=field.get(provider);if(item!=null)result.add(item);}
        Field configs=provider.getClass().getDeclaredField("a");configs.setAccessible(true);SparseArray<?> values=(SparseArray<?>)configs.get(provider);
        for(int i=0;i<values.size();i++)result.add(values.valueAt(i));return result;
    }
    private static Field baseField(Object item,String name)throws ReflectiveOperationException{
        Class<?> type=item.getClass();while(type!=null){if(type.getName().equals("Xh.a")){Field field=type.getDeclaredField(name);field.setAccessible(true);return field;}type=type.getSuperclass();}throw new NoSuchFieldException("Xh.a."+name);
    }
    private static void flush(List<Object> items)throws Exception{
        for(Object item:items){Object lock=baseField(item,"a").get(item);synchronized(lock){
            Field editor=baseField(item,"d"),dirty=baseField(item,"e");Object pending=editor.get(item);
            if(pending!=null&&dirty.getBoolean(item)){if(!((SharedPreferences.Editor)pending).commit())throw new IOException("相机待保存设置写入失败");dirty.setBoolean(item,false);}
        }}
    }
    private static void clearCaches(List<Object> items)throws ReflectiveOperationException{
        for(Object item:items){Object lock=baseField(item,"a").get(item);synchronized(lock){
            Object cache=baseField(item,"b").get(item);Method clear=cache.getClass().getMethod("clear");clear.invoke(cache);
            baseField(item,"d").set(item,null);baseField(item,"e").setBoolean(item,false);
        }}
    }
    private static void remove(File file)throws IOException{
        if(!file.exists())return;
        if(file.isDirectory()&&!Files.isSymbolicLink(file.toPath())){File[] children=file.listFiles();if(children==null)throw new IOException("无法读取临时设置目录");for(File child:children)remove(child);}
        Files.delete(file.toPath());
    }
    private static final class NonClosingOutput extends FilterOutputStream{NonClosingOutput(OutputStream output){super(output);}public void write(byte[] bytes,int offset,int length)throws IOException{out.write(bytes,offset,length);}public void close()throws IOException{flush();}}
    private static final class NonClosingInput extends FilterInputStream{NonClosingInput(InputStream input){super(input);}public void close(){}}
}
