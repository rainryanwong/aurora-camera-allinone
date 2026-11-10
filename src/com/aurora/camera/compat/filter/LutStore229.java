package com.aurora.camera.compat.filter;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import org.json.*;

/** Private PNGs and stable slots; deleting a row never renumbers other LUTs. */
public final class LutStore229 {
    public static final int SLOTS=512;
    public static final String PREFIX="aurora_user_lut229_";
    private final Context context;private final SharedPreferences prefs;
    public LutStore229(Context context){this.context=context.getApplicationContext();prefs=this.context.getSharedPreferences("aurora_luts229",0);}
    public static String token(int slot){return PREFIX+String.format(Locale.ROOT,"%02d",slot);}
    public static int slot(String token){if(token==null||!token.startsWith(PREFIX))return 0;try{int n=Integer.parseInt(token.substring(PREFIX.length()).split("_",2)[0]);return n>=1&&n<=SLOTS?n:0;}catch(NumberFormatException e){return 0;}}
    public File file(int slot){JSONObject item=record(slot);return new File(new File(context.getFilesDir(),"aurora_luts229"),item==null?"slot_"+slot+".png":item.optString("file"));}
    public String resourceToken(int slot){JSONObject item=record(slot);return item==null?token(slot):token(slot)+"_"+item.optString("revision");}
    public void installBundled(String moduleApkPath)throws IOException{BundledFilterCatalog256.install(context,prefs,moduleApkPath);}
    public int restoreBundled(String moduleApkPath,CustomFilterStore253.Progress progress)throws IOException{return BundledFilterCatalog256.restore(context,prefs,moduleApkPath,progress);}
    public synchronized List<Integer> configuredSlots(){List<Integer> result=new ArrayList<>();for(int i=1;i<=SLOTS;i++)if(prefs.contains("slot:"+i))result.add(i);return result;}
    public synchronized List<Integer> slots(){List<Integer> result=new ArrayList<>();for(int slot:configuredSlots())if(enabled(slot))result.add(slot);return result;}
    public boolean enabled(int slot){JSONObject item=record(slot);return item!=null&&(!item.has("enabled")||item.optBoolean("enabled"));}
    public boolean bundled(int slot){JSONObject item=record(slot);return item!=null&&item.has("bundled");}
    public String sourceId(int slot){JSONObject item=record(slot);return item==null?null:item.optString("sourceId",token(slot));}
    public String projection(int slot){JSONObject item=record(slot);return item==null?null:item.optString("projection","imported");}
    public boolean publishToEditor(int slot){return enabled(slot)&&!"native-gallery".equals(projection(slot));}
    public synchronized void enabled(int slot,boolean enabled)throws IOException{
        JSONObject item=record(slot);if(item==null)throw new IOException("LUT 已不存在");
        try{item.put("enabled",enabled);if(!prefs.edit().putString("slot:"+slot,item.toString()).commit())throw new IOException("滤镜启用状态未能保存");}catch(JSONException invalid){throw new IOException(invalid);}
    }
    private JSONObject record(int slot){String value=prefs.getString("slot:"+slot,null);if(value==null)return null;try{return new JSONObject(value);}catch(JSONException e){throw new IllegalStateException("Invalid private LUT metadata slot="+slot,e);}}
    public String name(int slot){JSONObject item=record(slot);return item==null?null:item.optString("name");}
    public String label(String identity,String nativeName){return prefs.getString("label:"+identity,nativeName);}
    public static String recipeKey(String kind,String identity){return "recipe:"+kind+"/"+identity;}
    public static int styleNoise(int grain){return new int[]{0,20,25,30}[grain];}
    public static int styleSoftLight(int diffusion){return new int[]{0,3,5}[diffusion];}
    public synchronized void saveEdits(String identity,int slot,String name,int grain,int diffusion)throws IOException{
        saveEdits(identity,slot,name,grain,diffusion,true);
    }
    public synchronized void saveEdits(String identity,int slot,String name,int grain,int diffusion,boolean saveEffects)throws IOException{
        if(name.trim().isEmpty())throw new IOException("名称不能为空");
        if(grain<0||grain>3||diffusion<0||diffusion>2)throw new IllegalArgumentException("effect range");
        String label=cleanName(name);SharedPreferences.Editor edit=prefs.edit().putString("label:"+identity,label);
        if(saveEffects)edit.putString("effect:"+identity,grain+":"+diffusion);
        if(slot>0){JSONObject item=record(slot);if(item==null)throw new IOException("LUT 已不存在");try{item.put("name",label);}catch(JSONException e){throw new IOException(e);}edit.putString("slot:"+slot,item.toString());}
        if(!edit.commit())throw new IOException("滤镜名称与效果未能保存");
    }
    public int[] effects(String identity){String value=prefs.getString("effect:"+identity,null);if(value==null)return null;String[] parts=value.split(":");if(parts.length!=2)throw new IllegalStateException("Invalid LUT effects "+identity);return new int[]{Integer.parseInt(parts[0]),Integer.parseInt(parts[1])};}
    public void effects(String identity,int grain,int diffusion)throws IOException{if(grain<0||grain>3||diffusion<0||diffusion>2)throw new IllegalArgumentException("effect range");if(!prefs.edit().putString("effect:"+identity,grain+":"+diffusion).commit())throw new IOException("滤镜效果未能保存");}
    public synchronized int importCube(Uri uri)throws IOException{
        try(InputStream input=context.getContentResolver().openInputStream(uri)){
            if(input==null)throw new IOException("无法读取所选 LUT");return importCube(input,displayName(uri));
        }
    }
    public synchronized int importCube(InputStream input,String fileName)throws IOException{
        CubeLut229 cube=CubeLut229.parse(input);
        return saveBitmap(Bitmap.createBitmap(cube.render(),512,512,Bitmap.Config.ARGB_8888),cube.size,cube.title.isEmpty()?fileName:cube.title);
    }
    public synchronized int importPng(InputStream input,String fileName)throws IOException{
        ByteArrayOutputStream encoded=new ByteArrayOutputStream();byte[] bytes=new byte[8192];for(int n;(n=input.read(bytes))!=-1;){if(encoded.size()+n>4*1024*1024)throw new IOException("LUT PNG 超过 4 MiB");encoded.write(bytes,0,n);}byte[] png=encoded.toByteArray();
        android.graphics.BitmapFactory.Options options=new android.graphics.BitmapFactory.Options();options.inJustDecodeBounds=true;
        android.graphics.BitmapFactory.decodeByteArray(png,0,png.length,options);
        if(options.outWidth!=512||options.outHeight!=512)throw new IOException("原生 LUT PNG 必须为 512 × 512 的 64 阶分块布局");
        options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bitmap=android.graphics.BitmapFactory.decodeByteArray(png,0,png.length,options);
        if(bitmap==null)throw new IOException("无法解码 LUT PNG");
        if(bitmap.getWidth()!=512||bitmap.getHeight()!=512){bitmap.recycle();throw new IOException("原生 LUT PNG 必须为 512 × 512 的 64 阶分块布局");}
        return saveBitmap(bitmap,64,fileName);
    }
    private int saveBitmap(Bitmap bitmap,int size,String name)throws IOException{
        try{
        int slot=0;for(int i=1;i<=SLOTS;i++)if(!prefs.contains("slot:"+i)){slot=i;break;}
        if(slot==0)throw new IOException("自定义 LUT 槽位已满（"+SLOTS+" 项）");
        name=cleanName(name);
        File output=file(slot);Files.createDirectories(output.getParentFile().toPath());File staged=new File(output.getParentFile(),"import_"+slot+".png");
        try(FileOutputStream out=new FileOutputStream(staged)){if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("无法保存原生 LUT");out.getFD().sync();}
        String revision;try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(staged.toPath()));StringBuilder hash=new StringBuilder();for(byte b:digest)hash.append(String.format(Locale.ROOT,"%02x",b&255));revision=hash.toString();}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
        output=new File(output.getParentFile(),revision+".png");Files.move(staged.toPath(),output.toPath(),StandardCopyOption.REPLACE_EXISTING);
        try{JSONObject item=new JSONObject().put("name",name).put("size",size).put("created",System.currentTimeMillis()).put("revision",revision).put("file",output.getName());if(!prefs.edit().putString("slot:"+slot,item.toString()).commit())throw new IOException("LUT 元数据未能保存");}catch(JSONException e){throw new IOException(e);}
        return slot;
        }finally{bitmap.recycle();}
    }
    private String displayName(Uri uri){try(Cursor cursor=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cursor!=null&&cursor.moveToFirst())return cursor.getString(0);}return "自定义 LUT";}
    private static String cleanName(String value){String name=value.trim().replaceAll("[\\p{Cntrl}/\\\\]"," ");if(name.toLowerCase(Locale.ROOT).endsWith(".cube"))name=name.substring(0,name.length()-5);else if(name.toLowerCase(Locale.ROOT).endsWith(".png"))name=name.substring(0,name.length()-4);if(name.isEmpty())name="自定义 LUT";return name.length()>48?name.substring(0,48):name;}
    public synchronized void rename(int slot,String name)throws IOException{JSONObject item=record(slot);if(item==null)throw new IOException("LUT 已不存在");try{String label=cleanName(name);item.put("name",label);if(!prefs.edit().putString("slot:"+slot,item.toString()).putString("label:"+token(slot),label).commit())throw new IOException("名称未能保存");}catch(JSONException e){throw new IOException(e);}}
    public synchronized void remove(int slot)throws IOException{File removed=file(slot);if(!prefs.edit().remove("slot:"+slot).remove("effect:"+token(slot)).remove("label:"+token(slot)).commit())throw new IOException("LUT 元数据删除失败");boolean referenced=false;for(int other:configuredSlots())if(file(other).equals(removed))referenced=true;if(!referenced)Files.deleteIfExists(removed.toPath());}
    public static String summary(int[] effects){int grain=effects==null?0:effects[0],soft=effects==null?0:effects[1];return "颗粒："+new String[]{"无","轻","中","强"}[grain]+" · 柔光："+new String[]{"无","黑柔","白柔"}[soft];}
}
