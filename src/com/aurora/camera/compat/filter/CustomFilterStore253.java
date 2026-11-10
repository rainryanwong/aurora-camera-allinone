package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.Options229;
import de.robv.android.xposed.XposedHelpers;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** User presets and LUT imports retain their native storage and stable identities. */
public final class CustomFilterStore253 {
    public interface Progress { void update(int completed,int total,String name); }
    public static final class Entry {
        public final String id,name,description,sourceId,projection;
        public final int slot,filter;
        public final boolean enabled,bundled;
        public final RecipeCatalog219.Recipe recipe;
        Entry(LutRuntime229.Entry row,RecipeCatalog219.Recipe recipe,LutStore229 store){
            id=row.key;name=row.name;slot=row.slot;filter=row.id;this.recipe=recipe;
            enabled=slot<=0||store.enabled(slot);bundled=slot>0&&store.bundled(slot);sourceId=slot>0?store.sourceId(slot):id;projection=slot>0?store.projection(slot):"camera-builtin";
            description=slot>0?"native-gallery".equals(projection)?"导入 LUT · 相册自带":bundled?"导入 LUT · Phoenix":"导入 LUT":recipe==null?"自定义原生滤镜":recipe.kind.equals("MiCamStyleFront")?"前置自建配方":"后置自建配方";
        }
    }
    public static final class ImportResult {
        public int total,success;
        public final List<String> failures=new ArrayList<>();
        public final List<String> unreadable=new ArrayList<>();
        public String summary(){return "共计滤镜："+total+"\n导入成功："+success+"\n导入失败："+(total-success)+(failures.isEmpty()?"":"\n\n失败滤镜：\n"+String.join("\n",failures))+(unreadable.isEmpty()?"":"\n\n文件解析失败（滤镜数量未知）：\n"+String.join("\n",unreadable));}
    }
    public static final class ScanResult {
        public int total;
        public final List<List<Entry>> groups=new ArrayList<>();
        public final List<String> failures=new ArrayList<>();
        public int duplicates(){int count=0;for(List<Entry> group:groups)count+=group.size()-1;return count;}
    }
    private static final String FILTER="pref_camera_shader_coloreffect_key";
    private final Host host;
    private final LutRuntime229 runtime;
    private final Context context;
    private final CameraFilterCatalog248 catalog;
    private final FilterWorkspace215 workspace;
    private final java.lang.reflect.Field workspaceModes,filterModes;
    public CustomFilterStore253(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        this.host=host;this.runtime=runtime;context=new Options229(host).context();catalog=new CameraFilterCatalog248(host);workspace=new FilterWorkspace215(host,runtime);workspaceModes=host.field("r2.Z0","a");filterModes=host.field("r2.t","e");
    }
    private Object call(Object owner,String name,Object...args){return XposedHelpers.callMethod(owner,name,args);}
    private Class<?> type(String name){try{return host.type(name);}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}}
    private File workspaceRoot(){return new File(context.getFilesDir(),"Workspace");}
    public List<Entry> entries()throws Exception{
        Map<String,RecipeCatalog219.Recipe> recipes=new HashMap<>();
        for(RecipeCatalog219.Recipe recipe:RecipeCatalog219.scan(workspaceRoot(),Collections.emptyMap()))recipes.put(LutStore229.recipeKey(recipe.kind,recipe.id),recipe);
        Map<String,?> edits=context.getSharedPreferences("aurora_luts229",0).getAll();List<Entry> result=new ArrayList<>();LutStore229 store=runtime.store();Set<Integer> represented=new HashSet<>();
        for(LutRuntime229.Entry row:runtime.entries()){
            RecipeCatalog219.Recipe recipe=recipes.get(row.key);
            if(row.slot>0||recipe!=null||edits.containsKey("label:"+row.key)||edits.containsKey("effect:"+row.key)){result.add(new Entry(row,recipe,store));if(row.slot>0)represented.add(row.slot);}
        }
        for(int slot:store.configuredSlots())if(!represented.contains(slot)){
            String key=LutStore229.token(slot);result.add(new Entry(new LutRuntime229.Entry(runtime.id(slot),slot,key,store.label(key,store.name(slot))),null,store));
        }
        return result;
    }
    public void enabled(Entry entry,boolean enabled)throws IOException{if(entry.slot<=0)throw new IOException("该滤镜不属于导入 LUT");runtime.store().enabled(entry.slot,enabled);}
    public ScanResult duplicates(List<Entry> rows,Progress progress)throws Exception{
        CameraFilterCatalog248.Comparison comparison=catalog.comparison(context);Map<String,List<Entry>> grouped=new LinkedHashMap<>();ScanResult result=new ScanResult();result.total=rows.size();
        int done=0;for(Entry entry:rows){
            try{
                int[] style=entry.recipe==null?new int[8]:entry.recipe.colors.clone();int[] edited=runtime.store().effects(entry.id);
                if(edited!=null){style[5]=LutStore229.styleNoise(edited[0]);style[6]=LutStore229.styleSoftLight(edited[1]);}
                int slot=entry.slot>0?entry.slot:runtime.slot(entry.filter);
                String key;
                if(entry.recipe==null&&edited!=null)key=slot>0?comparison.importedEditedKey(runtime.store().file(slot),edited):comparison.nativeEditedKey(entry.filter,edited);
                else key=comparison.nativeKey(entry.filter,style,edited!=null);
                grouped.computeIfAbsent(key,ignored->new ArrayList<>()).add(entry);
            }catch(IOException invalid){result.failures.add(entry.name+"："+invalid.getMessage());}
            progress.update(++done,rows.size(),entry.name);
        }
        for(List<Entry> group:grouped.values())if(group.size()>1){group.sort(Comparator.comparingInt(entry->entry.slot>0?0:entry.recipe==null?1:2));result.groups.add(group);}
        return result;
    }
    public Set<String> referencedLuts(Set<String> removing)throws IOException{
        Set<String> referenced=new HashSet<>();
        for(RecipeCatalog219.Recipe recipe:RecipeCatalog219.scan(workspaceRoot(),Collections.emptyMap())){
            int slot=runtime.slot(recipe.filter);
            if(slot>0&&!removing.contains(LutStore229.recipeKey(recipe.kind,recipe.id)))referenced.add(LutStore229.token(slot));
        }
        return referenced;
    }
    public String documentName(Uri uri)throws IOException{
        try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(cursor!=null&&cursor.moveToFirst()&&cursor.getString(0)!=null)return cursor.getString(0);
        }
        throw new IOException("无法读取所选滤镜的文件名");
    }
    private static final class Candidate {
        final File file;final String name;final boolean preset;
        Candidate(File file,String name,boolean preset){this.file=file;this.name=name;this.preset=preset;}
    }
    public static final class ImportItem {
        public final int index;public final String name;public final boolean preset;
        private final File file;
        ImportItem(int index,File file,String name,boolean preset){this.index=index;this.file=file;this.name=name;this.preset=preset;}
    }
    public static final class ImportBatch implements AutoCloseable {
        public final String token;
        public final List<ImportItem> items=new ArrayList<>();
        public final List<String> unreadable=new ArrayList<>();
        public boolean needsSelection;
        private final File directory;
        ImportBatch(File directory){this.directory=directory;token=directory.getName();}
        public void close()throws IOException{deleteTree(directory);}
    }
    public ImportBatch prepareFiles(List<Uri> uris,Progress progress)throws IOException{
        Path staging=Files.createTempDirectory(context.getCacheDir().toPath(),"filter-import-");ImportBatch batch=new ImportBatch(staging.toFile());
        try{
            int index=0;for(Uri uri:uris){
                String name=uri.toString();index++;
                try{
                    name=documentName(uri);progress.update(index,uris.size(),"解析 "+name);
                    File source=staging.resolve("document-"+index).toFile();
                    try(InputStream input=context.getContentResolver().openInputStream(uri);OutputStream output=new FileOutputStream(source)){
                        if(input==null)throw new IOException("无法读取所选文件");copy(input,output,512L*1024*1024);
                    }
                    List<Candidate> document=new ArrayList<>();discover(source,name,staging.resolve("unpack-"+index).toFile(),document,progress,new long[]{1024L*1024*1024},0,batch.unreadable);
                    String lower=name.toLowerCase(Locale.ROOT);if(document.size()>1&&!lower.endsWith(".cube")&&!lower.endsWith(".png"))batch.needsSelection=true;
                    for(Candidate candidate:document){String label=candidate.name;if(candidate.preset&&label.startsWith("MiCamStyle")){String[] parts=label.split("_",4);if(parts.length==4)label=parts[3];}batch.items.add(new ImportItem(batch.items.size(),candidate.file,label,candidate.preset));}
                }catch(IOException|SecurityException invalid){batch.unreadable.add(name+"："+invalid.getMessage());}
            }
            JSONObject manifest=new JSONObject().put("selection",batch.needsSelection).put("unreadable",new JSONArray(batch.unreadable));JSONArray items=new JSONArray();
            for(ImportItem item:batch.items)items.put(new JSONObject().put("file",staging.relativize(item.file.toPath()).toString()).put("name",item.name).put("preset",item.preset));
            manifest.put("items",items);Files.write(staging.resolve("pending.json"),manifest.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));return batch;
        }catch(IOException|JSONException failure){try{batch.close();}catch(IOException cleanup){failure.addSuppressed(cleanup);}throw failure instanceof IOException?(IOException)failure:new IOException("无法保存待导入滤镜列表",failure);}
    }
    public ImportBatch prepared(String token)throws IOException{
        File directory=stagedDirectory(token);ImportBatch batch=new ImportBatch(directory);
        try{
            JSONObject manifest=new JSONObject(new String(Files.readAllBytes(new File(directory,"pending.json").toPath()),java.nio.charset.StandardCharsets.UTF_8));batch.needsSelection=manifest.getBoolean("selection");
            JSONArray errors=manifest.getJSONArray("unreadable");for(int i=0;i<errors.length();i++)batch.unreadable.add(errors.getString(i));
            JSONArray items=manifest.getJSONArray("items");for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);File file=new File(directory,item.getString("file")).getCanonicalFile();
                if(!file.toPath().startsWith(directory.toPath())||!file.exists())throw new IOException("待导入滤镜暂存文件缺失，请重新选择文件");
                batch.items.add(new ImportItem(i,file,item.getString("name"),item.getBoolean("preset")));
            }return batch;
        }catch(JSONException failure){throw new IOException("待导入滤镜列表无法读取",failure);}
    }
    private File stagedDirectory(String token)throws IOException{
        if(token==null||!token.startsWith("filter-import-")||!new File(token).getName().equals(token))throw new IOException("待导入列表无效，请重新选择滤镜文件");
        File directory=new File(context.getCacheDir(),token).getCanonicalFile();if(!directory.getParentFile().equals(context.getCacheDir().getCanonicalFile()))throw new IOException("待导入列表目录无效");return directory;
    }
    public void discard(String token)throws IOException{deleteTree(stagedDirectory(token));}
    public ImportResult importSelected(ImportBatch batch,Set<Integer> selected,Progress progress)throws IOException{
        ImportResult result=new ImportResult();result.total=selected.size();result.unreadable.addAll(batch.unreadable);int done=0;
        try{
            for(ImportItem candidate:batch.items){
                if(!selected.contains(candidate.index))continue;
                try{
                    if(candidate.preset)importPreset(candidate.file,candidate.name);
                    else try(InputStream input=new FileInputStream(candidate.file)){
                        if(candidate.name.toLowerCase(Locale.ROOT).endsWith(".png"))runtime.store().importPng(input,candidate.name);else runtime.store().importCube(input,candidate.name);
                    }
                    result.success++;
                }catch(IOException invalid){result.failures.add(candidate.name+"："+invalid.getMessage());}
                progress.update(++done,result.total,candidate.name);
            }
            return result;
        }finally{batch.close();}
    }
    private void discover(File source,String name,File unpack,List<Candidate> out,Progress progress,long[] remaining,int depth,List<String> errors)throws IOException{
        String lower=name.toLowerCase(Locale.ROOT);
        if(lower.endsWith(".cube")||lower.endsWith(".png")){out.add(new Candidate(source,name,false));return;}
        try(InputStream input=new FileInputStream(source)){
            if(input.read()!='P'||input.read()!='K')throw new IOException("支持 .cube、512 × 512 原生 LUT PNG 和相机配方 ZIP");
        }
        if(depth>2)throw new IOException("支持最多三层嵌套的滤镜 ZIP 包");
        int total;try(ZipFile archive=new ZipFile(source)){total=archive.size();}
        Files.createDirectories(unpack.toPath());int count=0;
        try(ZipInputStream zip=new ZipInputStream(new FileInputStream(source))){
            for(ZipEntry entry;(entry=zip.getNextEntry())!=null;){
                if(++count>65536)throw new IOException("ZIP 内的文件数量过多");
                String path=entry.getName().replace('\\','/');File file=new File(unpack,path).getCanonicalFile();
                if(!file.toPath().startsWith(unpack.getCanonicalFile().toPath())||path.startsWith("/")||path.indexOf(':')>=0)throw new IOException("ZIP 包含无效路径："+entry.getName());
                if(entry.isDirectory()){Files.createDirectories(file.toPath());continue;}
                Files.createDirectories(file.getParentFile().toPath());
                try(OutputStream output=Files.newOutputStream(file.toPath(),StandardOpenOption.CREATE_NEW)){remaining[0]-=copy(zip,output,remaining[0]);}
                if(count==1||count==total||count%Math.max(1,total/100)==0)progress.update(count,total,"解压 "+entry.getName());
            }
        }
        int before=out.size(),failed=errors.size();discoverDirectory(unpack,name,out,progress,remaining,depth,errors);
        if(before==out.size()&&failed==errors.size())throw new IOException("ZIP 中没有可导入的滤镜");
    }
    private void discoverDirectory(File folder,String archiveName,List<Candidate> out,Progress progress,long[] remaining,int depth,List<String> errors)throws IOException{
        File[] children=folder.listFiles();if(children==null)throw new IOException("无法读取解压后的滤镜");
        boolean preset=false;for(File file:children)if(file.isFile()&&file.getName().startsWith("p_"+FILTER+"_"))preset=true;
        if(preset){out.add(new Candidate(folder,folder.getName().startsWith("MiCamStyle")?folder.getName():archiveName,true));return;}
        Arrays.sort(children,Comparator.comparing(File::getName));
        for(File file:children){
            String name=file.getName(),lower=name.toLowerCase(Locale.ROOT);
            if(file.isDirectory()){if(!name.equals("__MACOSX"))discoverDirectory(file,archiveName,out,progress,remaining,depth,errors);}
            else if(lower.endsWith(".cube")||lower.endsWith(".png"))out.add(new Candidate(file,name,false));
            else if(lower.endsWith(".zip")){
                List<Candidate> child=new ArrayList<>();
                try{discover(file,name,new File(file.getParentFile(),"unpacked-"+UUID.randomUUID()),child,progress,remaining,depth+1,errors);out.addAll(child);}
                catch(IOException invalid){errors.add(name+"："+invalid.getMessage());}
            }
        }
    }
    private void importPreset(File source,String suppliedName)throws IOException{
        String kind="MiCamStyleRear",name=suppliedName;Map<String,String> parameters=new LinkedHashMap<>();
        File[] files=source.listFiles();if(files==null)throw new IOException("无法读取配方");
        Set<String> allowed=new HashSet<>();allowed.add(FILTER);for(String color:RecipeCatalog219.COLORS)allowed.add("pref_qc_camera_style_"+color+"_key");
        for(File file:files){String marker=file.getName();
            if(marker.equals("s_MiCamStyleFront"))kind="MiCamStyleFront";
            if(marker.startsWith("p_")){
                int split=marker.lastIndexOf('_');if(split<=2)throw new IOException("无效配方参数："+marker);
                String key=marker.substring(2,split),value=marker.substring(split+1);
                if(!allowed.contains(key))throw new IOException("配方包含当前滤镜不支持的参数："+key);
                if(parameters.put(key,value)!=null)throw new IOException("配方参数重复："+key);
            }
        }
        if(source.getName().startsWith("MiCamStyleRear_")||source.getName().startsWith("MiCamStyleFront_")){
            String[] identity=source.getName().split("_",4);if(identity.length==4){kind=identity[0];name=identity[3];}
        }
        if(!parameters.containsKey(FILTER))throw new IOException("配方缺少基础滤镜");
        int filter;
        try{filter=Integer.parseInt(parameters.get(FILTER));}catch(NumberFormatException invalid){throw new IOException("配方基础滤镜无效",invalid);}
        Object[] enums=type("o3.d").getEnumConstants();int ordinal=filter&65535;
        if(ordinal>=enums.length||runtime.slot(filter)>0&&!runtime.store().configuredSlots().contains(runtime.slot(filter)))throw new IOException("配方引用的基础滤镜未安装");
        for(String key:allowed)if(!key.equals(FILTER)&&parameters.containsKey(key)){
            int value;try{value=Integer.parseInt(parameters.get(key));}catch(NumberFormatException invalid){throw new IOException("配方参数无效："+key,invalid);}
            if(value<-100||value>100||key.contains("soft_light")&&(value<0||value>5))throw new IOException("配方参数超出原生范围："+key);
        }
        name=name.replaceFirst("(?i)\\.zip$","").replaceAll("[\\p{Cntrl}/\\\\]"," ").trim();if(name.isEmpty())throw new IOException("配方名称为空");if(name.length()>48)name=name.substring(0,48);
        File parent=new File(workspaceRoot(),kind);Files.createDirectories(parent.toPath());long stamp=System.currentTimeMillis();File target;
        do{target=new File(parent,kind+"_imported_"+(stamp++)+"_"+name);}while(target.exists());
        Path temporary=Files.createTempDirectory(context.getCacheDir().toPath(),"preset-");
        try{
            for(Map.Entry<String,String> item:parameters.entrySet())Files.createFile(temporary.resolve("p_"+item.getKey()+"_"+item.getValue()));
            Files.createFile(temporary.resolve("s_"+kind));Files.createFile(temporary.resolve("v_4"));
            int order=0;for(RecipeCatalog219.Recipe existing:RecipeCatalog219.scan(workspaceRoot(),Collections.emptyMap()))if(existing.kind.equals(kind)&&existing.order<Integer.MAX_VALUE)order=Math.max(order,existing.order+1);
            Files.createFile(temporary.resolve("o_"+order));Files.move(temporary,target.toPath(),StandardCopyOption.ATOMIC_MOVE);
        }finally{deleteTree(temporary.toFile());}
    }
    /** Runs on the settings thread so selected native rows are reset before removal. */
    public void remove(Entry entry)throws IOException{
        CamStyleAccess260.lock();try{
        if(entry.recipe!=null){
            Object ws=call(XposedHelpers.callStaticMethod(type("g2.a"),"e"),"a",entry.recipe.kind.equals("MiCamStyleFront")?type("com.android.camera.features.mode.capture.i"):type("com.android.camera.features.mode.capture.k"));
            boolean removed=false;for(FilterWorkspace215.Entry row:workspace.snapshot(ws,context))if(row.id.equals(entry.recipe.id)){
                prepareRemoval(ws,row);call(row.nativeRow,"L",true);workspace.raw(ws).remove(row.nativeRow);removed=true;break;
            }
            if(!removed)deleteTree(entry.recipe.directory);
            clearWorkspaceReference(entry.recipe.kind.equals("MiCamStyleFront"),entry.recipe.directory.getAbsolutePath(),null);
            SharedPreferences visibility=context.getSharedPreferences("aurora_filter_workspace_v2",0);Set<String> hidden=new HashSet<>(visibility.getStringSet("hidden:"+entry.recipe.kind,Collections.emptySet()));hidden.remove(entry.recipe.id);
            if(!visibility.edit().putStringSet("hidden:"+entry.recipe.kind,hidden).commit())throw new IOException("滤镜已删除，但显示状态清理失败");
        }else if(entry.slot>0){
            if(referencedLuts(Collections.emptySet()).contains(entry.id))throw new IOException("该 LUT 仍被保留的自建配方使用，请先删除引用它的配方");
            for(boolean front:new boolean[]{false,true}){
                Object ws=call(XposedHelpers.callStaticMethod(type("g2.a"),"e"),"a",type(front?"com.android.camera.features.mode.capture.i":"com.android.camera.features.mode.capture.k"));
                for(FilterWorkspace215.Entry row:workspace.snapshot(ws,context))if(row.id.endsWith("_"+entry.id))prepareRemoval(ws,row);
                clearWorkspaceReference(front,null,entry.id);
                Object own=call(ownConfig(front),"x",type("r2.t"));
                for(int mode:modes(filterModes))if(String.valueOf(entry.filter).equals(call(own,"getComponentValue",mode)))call(own,"setComponentValue",mode,call(own,"getDefaultValue",mode));
            }
            runtime.store().remove(entry.slot);
        }
        SharedPreferences.Editor edit=context.getSharedPreferences("aurora_luts229",0).edit().remove("label:"+entry.id).remove("effect:"+entry.id);
        if(!edit.commit())throw new IOException("滤镜自定义设置清理失败");
        }finally{CamStyleAccess260.unlock();}
    }
    private void prepareRemoval(Object ws,FilterWorkspace215.Entry entry)throws IOException{
        boolean front=workspace.kind(ws).equals("MiCamStyleFront");
        if(front==workspace.frontFacing())workspace.beforeDelete(ws,entry,context);
        else if((Boolean)XposedHelpers.getObjectField(entry.nativeRow,"n")){
            call(entry.nativeRow,"W",false);
            for(FilterWorkspace215.Entry candidate:workspace.snapshot(ws,context))if(candidate.fixed){call(candidate.nativeRow,"W",true);break;}
        }
        String path=(String)XposedHelpers.getObjectField(entry.nativeRow,"g");
        clearWorkspaceReference(front,path,null);
    }
    private Object ownConfig(boolean front){return call(XposedHelpers.callStaticMethod(type("g2.a"),"i"),"b",front?1:0);}
    @SuppressWarnings("unchecked") private List<Integer> modes(java.lang.reflect.Field field)throws IOException{
        try{return(List<Integer>)field.get(null);}catch(IllegalAccessException failure){throw new IOException("无法读取相机工作区模式",failure);}
    }
    private void clearWorkspaceReference(boolean front,String removed,String token)throws IOException{
        Object own=call(ownConfig(front),"x",type("r2.Z0"));
        for(int mode:modes(workspaceModes)){
            String path=(String)call(own,"getComponentValue",mode);
            if(removed!=null&&removed.equals(path)||token!=null&&path.endsWith("_"+token))call(own,"setComponentValue",mode,call(own,"getDefaultValue",mode));
        }
    }
    public void refresh()throws ReflectiveOperationException{
        runtime.refresh();
        CamStyleAccess260.lock();try{
        Object ws=call(XposedHelpers.callStaticMethod(type("g2.a"),"e"),"a",type(workspace.frontFacing()?"com.android.camera.features.mode.capture.i":"com.android.camera.features.mode.capture.k"));
        if(workspace.raw(ws).isEmpty())return;
        int mode=workspace.currentMode();Set<String> known=new HashSet<>();for(FilterWorkspace215.Entry row:workspace.snapshot(ws,context))known.add(row.id);
        try{
            for(RecipeCatalog219.Recipe recipe:RecipeCatalog219.scan(workspaceRoot(),Collections.emptyMap())){
                if(!recipe.kind.equals(workspace.kind(ws))||known.contains(recipe.id))continue;
                Object parsed=XposedHelpers.callStaticMethod(type("U9.S"),"N",mode,recipe.kind,call(ws,"n"),recipe.directory.getAbsolutePath());
                if(parsed==null)throw new IOException("原生相机无法加载已导入的配方："+recipe.name);
                workspace.raw(ws).add(parsed);
            }
        }catch(IOException failure){throw new java.lang.reflect.InvocationTargetException(failure);}
        call(ws,"f",mode);
        workspace.changed();
        }finally{CamStyleAccess260.unlock();}
    }
    private static long copy(InputStream input,OutputStream output,long limit)throws IOException{
        long size=0;byte[] buffer=new byte[16384];for(int n;(n=input.read(buffer))!=-1;){size+=n;if(size>limit)throw new IOException("滤镜文件或解压内容超过 512 MiB");output.write(buffer,0,n);}return size;
    }
    private static void deleteTree(File file)throws IOException{
        if(!file.exists())return;File[] children=file.listFiles();if(children!=null)for(File child:children)deleteTree(child);Files.delete(file.toPath());
    }
}
