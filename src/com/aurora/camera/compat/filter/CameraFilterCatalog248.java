package com.aurora.camera.compat.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.SparseArray;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.sharedlut.LutShare246;
import com.aurora.camera.compat.sharedlut.Recipe248;
import de.robv.android.xposed.XposedBridge;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Camera1770 native recipe/catalog adapter. It never selects or mutates a camera recipe. */
public final class CameraFilterCatalog248 {
    private final Host host;
    private final Method controller,lookup,pixels,rowId,enumValues;
    private static final String DYNAMIC_PREFIX="AURORA_USER_LUT229_";
    private static final int LUT_EFFECTS=2|Recipe248.BASE_BLACK_MIST|Recipe248.BASE_WHITE_MIST;
    private final Field catalogs,parameters,rowLabel,rowGroup;
    private final Method[] flags=new Method[3];
    private final Map<Integer,File> nativeImages=new HashMap<>();
    private String nativeImageModel;
    public CameraFilterCatalog248(Host host)throws ReflectiveOperationException{
        this.host=host;enumValues=host.method("o3.d","values");controller=host.method("com.xiaomi.camera.effect.EffectController","u");
        catalogs=host.field("com.xiaomi.camera.effect.EffectController","R");
        lookup=host.method("xi.h0","f",host.type("o3.d"),boolean.class,int.class,int.class);
        pixels=host.method("o3.b","a",Context.class);parameters=host.field("o3.d","b");
        rowId=host.method("i3.b","a");rowLabel=host.field("i3.b","c");rowGroup=host.field("i3.b","o");
        for(int i=0;i<3;i++)flags[i]=host.method("com.xiaomi.camera.effect.EffectController",new String[]{"J","K","L"}[i],int.class);
    }
    private static final class Base {final File png;final boolean original;final int flags;final float[] values;
        Base(File png,boolean original,int flags,float[] values){this.png=png;this.original=original;this.flags=flags;this.values=values;}}
    public Comparison comparison(Context context){return new Comparison(context);}
    /** Compare the effective recipe and decoded LUT, independently of names and PNG metadata. */
    public final class Comparison {
        private final Context context;
        private final Object[] enums;
        private final Map<Integer,Base> bases=new HashMap<>();
        private final Map<File,String> pixels=new HashMap<>();
        private Comparison(Context context){this.context=context;try{enums=(Object[])enumValues.invoke(null);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
        public String nativeKey(int id,int[] style,boolean edited)throws Exception{
            Base value=base(context,id,enums,bases);float[] parameters=value.values.clone();int mask=value.flags;
            if(edited){parameters[4]=0;mask&=~LUT_EFFECTS;}
            return key(value.png,mask,parameters,style);
        }
        public String importedKey(File png,int[] style)throws IOException{return key(png,0,new float[6],style);}
        public String nativeEditedKey(int id,int[] effects)throws Exception{
            Base value=withEffects(base(context,id,enums,bases),effects);
            return key(value.png,value.flags,value.values,new int[8]);
        }
        public String importedEditedKey(File png,int[] effects)throws IOException{
            Base value=withEffects(new Base(png,false,0,new float[6]),effects);
            return key(value.png,value.flags,value.values,new int[8]);
        }
        private String key(File png,int mask,float[] parameters,int[] style)throws IOException{
            String hash=pixels.get(png);
            if(hash==null){
                Bitmap image=android.graphics.BitmapFactory.decodeFile(png.getAbsolutePath());
                if(image==null)throw new IOException("无法解码滤镜 LUT："+png.getName());
                try{
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");
                    int[] line=new int[image.getWidth()];
                    for(int y=0;y<image.getHeight();y++){image.getPixels(line,0,line.length,0,y,line.length,1);for(int pixel:line){digest.update((byte)(pixel>>16));digest.update((byte)(pixel>>8));digest.update((byte)pixel);}}
                    StringBuilder text=new StringBuilder();for(byte b:digest.digest())text.append(String.format(Locale.ROOT,"%02x",b&255));hash=text.toString();pixels.put(png,hash);
                }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}finally{image.recycle();}
            }
            float[] effective=parameters.clone();
            if((mask&1)==0)Arrays.fill(effective,0,4,0);
            if((mask&2)==0)effective[4]=0;
            if((mask&4)==0)effective[5]=0;
            return hash+":"+mask+":"+Arrays.toString(effective)+":"+Arrays.toString(style);
        }
    }
    public List<LutShare246.ExportItem> snapshot(Context context)throws Exception{
        Map<File,String> revisions=new HashMap<>();
        List<LutShare246.ExportItem> out=new ArrayList<>(CameraLutCatalog246.snapshot(context,revisions));
        LutStore229 store=new LutStore229(context);Map<String,?> preferences=context.getSharedPreferences("aurora_luts229",0).getAll();
        // Existing pure cube rows stay unchanged unless this user explicitly saved extra effects.
        for(int index=0;index<out.size();index++){
            LutShare246.ExportItem item=out.get(index);int slot=Integer.parseInt(item.row.id.substring(7));int[] edited=store.effects(LutStore229.token(slot));
            if(edited!=null){Base value=withEffects(new Base(item.file,false,0,new float[6]),edited);
                out.set(index,new LutShare246.ExportItem(new LutShare246.Row(item.row.id,item.row.name,item.row.revision,item.row.order,new Recipe248(false,value.flags,value.values,new int[8])),item.file));}
        }
        Object[] enums=(Object[])enumValues.invoke(null);Map<Integer,Base> bases=new HashMap<>();
        @SuppressWarnings("unchecked") SparseArray<ArrayList<?>> source=(SparseArray<ArrayList<?>>)catalogs.get(controller.invoke(null));
        Set<String> seen=new HashSet<>();
        for(int kind:new int[]{2,7,10}){
            List<?> rows=source.get(kind);if(rows==null)continue;
            for(Object row:rows){int id=(Integer)rowId.invoke(row),ordinal=id&65535;if(ordinal>=enums.length)continue;
                String key=((Enum<?>)enums[ordinal]).name();if("N_ORIGINAL".equals(key)||dynamicSlot(key)>0||"LEICA".equals(rowGroup.get(row))||!seen.add(key))continue;
                if(!preferences.containsKey("label:"+key)&&!preferences.containsKey("effect:"+key))continue;
                int resource=rowLabel.getInt(row);if(resource<=0)continue;String name=store.label(key,context.getString(resource));
                add(out,"native_"+key,name,base(context,id,enums,bases),new int[8],false,revisions);
            }
        }
        Map<String,Set<String>> hidden=new HashMap<>();
        for(String kind:new String[]{"MiCamStyleRear","MiCamStyleFront"})hidden.put(kind,context.getSharedPreferences("aurora_filter_workspace_v2",0).getStringSet("hidden:"+kind,Collections.emptySet()));
        for(RecipeCatalog219.Recipe recipe:RecipeCatalog219.scan(new File(context.getFilesDir(),"Workspace"),hidden)){
            String key=LutStore229.recipeKey(recipe.kind,recipe.id);int[] style=recipe.colors.clone(),edited=store.effects(key);if(edited!=null)applyEdits(style,edited);
            add(out,"recipe_"+hash(recipe.identity()),store.label(key,recipe.name),base(context,recipe.filter,enums,bases),style,edited!=null,revisions);
        }
        if(!preferences.equals(context.getSharedPreferences("aurora_luts229",0).getAll()))throw new IOException("Camera filter customization changed during export");
        Map<String,Integer> ranks=new HashMap<>();
        for(String kind:new String[]{"MiCamStyleRear","MiCamStyleFront"}){
            for(WorkspaceFilterOrder270.Row row:WorkspaceFilterOrder270.read(context,kind)){
                String id;
                if(!row.official)id="recipe_"+hash(kind+"/"+row.identity);
                else{
                    if(row.filter==RecipeCatalog219.ORIGINAL)continue;
                    int ordinal=row.filter&65535;if(ordinal>=enums.length)continue;
                    String name=((Enum<?>)enums[ordinal]).name();int slot=dynamicSlot(name);
                    id=slot>0?"import_"+slot:"native_"+name;
                }
                if(!ranks.containsKey(id))ranks.put(id,ranks.size());
            }
        }
        out.sort(Comparator.comparingInt(item->ranks.getOrDefault(item.row.id,Integer.MAX_VALUE)));
        for(int i=0;i<out.size();i++){
            LutShare246.ExportItem item=out.get(i);LutShare246.Row row=item.row;
            out.set(i,new LutShare246.ExportItem(new LutShare246.Row(row.id,row.name,row.revision,i,row.recipe),item.file));
        }
        return out;
    }
    private static void applyEdits(int[] style,int[] edited)throws IOException{
        if(edited.length!=2||edited[0]<0||edited[0]>3||edited[1]<0||edited[1]>2)throw new IOException("Invalid saved camera effects");
        style[5]=LutStore229.styleNoise(edited[0]);style[6]=LutStore229.styleSoftLight(edited[1]);
    }
    private static void add(List<LutShare246.ExportItem> out,String id,String name,Base base,int[] style,boolean edited,Map<File,String> revisions)throws IOException{
        float[] values=base.values.clone();int flags=base.flags;
        // A saved recipe override owns StyleAdjust grain/mist and disables the LUT noise/mist stages.
        if(edited){values[4]=0;flags&=~LUT_EFFECTS;}
        out.add(new LutShare246.ExportItem(new LutShare246.Row(id,name,CameraLutCatalog246.revision(base.png,revisions),out.size(),new Recipe248(base.original,flags,values,style)),base.png));
    }
    private static int dynamicSlot(String name){
        if(!name.startsWith(DYNAMIC_PREFIX))return 0;
        int slot=Integer.parseInt(name.substring(DYNAMIC_PREFIX.length()));
        return slot>=1&&slot<=LutStore229.SLOTS?slot:0;
    }
    private static Base withEffects(Base base,int[] edited)throws IOException{
        if(edited.length!=2||edited[0]<0||edited[0]>3||edited[1]<0||edited[1]>2)throw new IOException("Invalid saved camera effects");
        float[] values=base.values.clone();values[4]=new float[]{0,.2f,.25f,.3f}[edited[0]];
        int mask=base.flags&~LUT_EFFECTS;
        if(edited[0]>0)mask|=2;
        if(edited[1]==1)mask|=Recipe248.BASE_BLACK_MIST;
        if(edited[1]==2)mask|=Recipe248.BASE_WHITE_MIST;
        return new Base(base.png,base.original,mask,values);
    }
    private Base base(Context context,int id,Object[] enums,Map<Integer,Base> cache)throws Exception{
        Base ready=cache.get(id);if(ready!=null)return ready;
        if(id==RecipeCatalog219.ORIGINAL){ready=new Base(nativeImage(context,id,null),true,0,new float[6]);}
        else{
            int ordinal=id&65535;if(ordinal>=enums.length)throw new IOException("Recipe base filter is outside Camera LUT enum: "+id);
            Object type=enums[ordinal];String name=((Enum<?>)type).name();int slot=dynamicSlot(name);
            float[] raw=(float[])parameters.get(type);if(raw==null||raw.length<6)throw new IOException("Camera LUT parameters missing: "+name);
            LutStore229 store=new LutStore229(context);
            if(slot>0){
                File png=store.file(slot);
                if(store.name(slot)==null||!png.isFile())throw new IOException("Recipe base LUT is no longer configured: "+slot);
                // Dynamic values clone N_ORIGINAL; their J/K/L flags are false.
                // Read the configured PNG directly: the original factory cannot construct these bindings.
                ready=new Base(png,false,0,Arrays.copyOf(raw,6));
            }else{
                int mask=0;for(int i=0;i<3;i++)if((Boolean)flags[i].invoke(null,id))mask|=1<<i;
                ready=new Base(nativeImage(context,id,type),false,mask,Arrays.copyOf(raw,6));
            }
            int[] edited=store.effects(slot>0?LutStore229.token(slot):name);
            if(edited!=null)ready=withEffects(ready,edited);
        }
        cache.put(id,ready);return ready;
    }
    /** Only fixed host pixels are reused; names, effects, recipes and remote state are read anew. */
    private synchronized File nativeImage(Context context,int id,Object type)throws Exception{
        // Classic scene resource hooks can change the pixels returned by the native loader.
        String model=context.getSharedPreferences("aurora_classic239",0).getString("selected_model","native");
        if(!model.equals(nativeImageModel)){nativeImages.clear();nativeImageModel=model;}
        File cached=nativeImages.get(id);
        if(cached!=null&&cached.isFile())return cached;
        Bitmap owned;
        if(type==null)owned=identity();
        else{
            Object nativeLookup;
            try{nativeLookup=XposedBridge.invokeOriginalMethod(lookup,null,new Object[]{type,false,0,100});}
            catch(Throwable failure){if(failure instanceof Exception)throw (Exception)failure;if(failure instanceof Error)throw (Error)failure;throw new ReflectiveOperationException("Original Camera LUT factory failed",failure);}
            if(nativeLookup==null)throw new IOException("Camera native LUT factory returned null: "+type);
            Bitmap borrowed=(Bitmap)pixels.invoke(nativeLookup,context);
            if(borrowed==null)throw new IOException("Camera native LUT decode failed: "+type);
            if(borrowed.getWidth()!=512||borrowed.getHeight()!=512)throw new IOException("Camera base LUT grid is unsupported: "+type+" "+borrowed.getWidth()+"x"+borrowed.getHeight());
            owned=borrowed.copy(Bitmap.Config.ARGB_8888,false);
            if(owned==null)throw new IOException("Cannot copy Camera base LUT: "+type);
        }
        File result=write(context,owned);
        nativeImages.put(id,result);
        return result;
    }
    private static Bitmap identity(){int[] colors=new int[512*512];for(int b=0;b<64;b++)for(int g=0;g<64;g++)for(int r=0;r<64;r++)
        colors[(b/8*64+g)*512+b%8*64+r]=0xff000000|Math.round(r*255f/63)<<16|Math.round(g*255f/63)<<8|Math.round(b*255f/63);
        return Bitmap.createBitmap(colors,512,512,Bitmap.Config.ARGB_8888);}
    private static File write(Context context,Bitmap owned)throws IOException{
        Path root=new File(context.getFilesDir(),"aurora_export248").toPath();Files.createDirectories(root);Path temp=Files.createTempFile(root,"lut-",".part");
        try{try(FileOutputStream output=new FileOutputStream(temp.toFile())){if(!owned.compress(Bitmap.CompressFormat.PNG,100,output))throw new IOException("Cannot encode native Camera LUT");output.getFD().sync();}
            Path file=root.resolve(LutShare246.sha256(temp.toFile())+".png");if(Files.exists(file))Files.delete(temp);else Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE);return file.toFile();
        }finally{owned.recycle();Files.deleteIfExists(temp);}
    }
    private static String hash(String value)throws Exception{
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
}
