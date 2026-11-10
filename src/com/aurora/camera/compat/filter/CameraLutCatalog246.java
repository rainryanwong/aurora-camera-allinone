package com.aurora.camera.compat.filter;

import android.content.Context;
import com.aurora.camera.compat.sharedlut.LutShare246;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** Publishes only configured cube imports; names come from the imported slot record. */
public final class CameraLutCatalog246 {
    private CameraLutCatalog246(){}
    private static Map<String,String> slots(Context context)throws IOException {
        Map<String,?> all=context.getSharedPreferences("aurora_luts229",Context.MODE_PRIVATE).getAll();
        LinkedHashMap<String,String> slots=new LinkedHashMap<>();
        for(int slot=1;slot<=LutStore229.SLOTS;slot++){
            String key="slot:"+slot;Object value=all.get(key);if(value==null)continue;
            if(!(value instanceof String))throw new IOException("Invalid imported LUT record "+key);
            slots.put(key,(String)value);
        }
        return slots;
    }
    public static boolean changedKey(String key) {
        if(key==null)return true; // SharedPreferences.clear() on Android 11+.
        if(!key.startsWith("slot:"))return false;
        try {int slot=Integer.parseInt(key.substring(5));return slot>=1&&slot<=LutStore229.SLOTS;}
        catch(NumberFormatException invalid){return false;}
    }
    public static List<LutShare246.ExportItem> snapshot(Context context)throws IOException {
        return snapshot(context,new java.util.HashMap<>());
    }
    static List<LutShare246.ExportItem> snapshot(Context context,Map<File,String> revisions)throws IOException {
        Map<String,String> slots=slots(context);
        LutStore229 store=new LutStore229(context);
        ArrayList<LutShare246.ExportItem> out=new ArrayList<>();
        File root=new File(context.getFilesDir(),"aurora_luts229").getCanonicalFile();
        try{
            for(int slot=1;slot<=LutStore229.SLOTS;slot++){
                String raw=slots.get("slot:"+slot);if(raw==null)continue;
                if(!store.publishToEditor(slot))continue;
                JSONObject item=new JSONObject(raw);String revision=item.getString("revision");
                File file=new File(root,item.getString("file")).getCanonicalFile();
                if(!root.equals(file.getParentFile())||!file.isFile())throw new IOException("Imported LUT file missing or outside its private directory: slot="+slot);
                String actual=revision(file,revisions);
                if(!actual.equals(revision))throw new IOException("Imported LUT revision does not match PNG: slot="+slot);
                // Stable slot IDs, content-addressed revisions, and ascending slot order match the camera.
                out.add(new LutShare246.ExportItem(new LutShare246.Row("import_"+slot,item.getString("name"),actual,out.size()),file));
            }
        }catch(org.json.JSONException|IllegalArgumentException invalid){throw new IOException("Invalid imported LUT metadata",invalid);}
        if(!slots.equals(slots(context)))throw new IOException("Imported LUT metadata changed during export");
        return out;
    }
    static String revision(File file,Map<File,String> revisions)throws IOException {
        String value=revisions.get(file);
        if(value==null){value=LutShare246.sha256(file);revisions.put(file,value);}
        return value;
    }
}
