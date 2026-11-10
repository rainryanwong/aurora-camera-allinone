package com.aurora.camera.compat.watermark;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Same watermark identity and settings, with its OEM short/long layout. */
public final class WatermarkVariantPolicy219 {
    private static final class Pair {
        final JSONObject shortConfig,longConfig;
        Pair(JSONObject a,JSONObject b){shortConfig=a;longConfig=b;}
    }
    private final Map<String,Pair> pairs=new HashMap<>();
    public WatermarkVariantPolicy219(String json)throws JSONException {
        JSONArray rows=new JSONObject(json).getJSONArray("pairs");
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i),a=row.getJSONObject("short"),b=row.getJSONObject("long");
            Pair pair=new Pair(a,b);String prefix=row.getString("root")+"/"+row.getString("group")+"/";
            pairs.put(prefix+a.getJSONObject("metadata").getString("id"),pair);
            pairs.put(prefix+b.getJSONObject("metadata").getString("id"),pair);
        }
    }
    public String choice(String folder,String logo,String model,boolean custom)throws JSONException {
        if(!custom||!pairs.containsKey(folder))return "native";
        int length=(String.valueOf(logo)+" "+String.valueOf(model)).length();
        JSONObject variant=length>17?pairs.get(folder).longConfig:pairs.get(folder).shortConfig;
        String id=variant.getJSONObject("metadata").getString("id");
        return folder.endsWith("/"+id)?"native":id;
    }
    public String rewrite(String folder,String original,String choice)throws JSONException {
        Pair pair=pairs.get(folder);if(pair==null||"native".equals(choice))return original;
        JSONObject source=new JSONObject(original);
        String own=source.getJSONObject("metadata").getString("id");
        if(!folder.endsWith("/"+own))throw new JSONException("Watermark folder and metadata ID differ");
        JSONObject variant=pair.longConfig.getJSONObject("metadata").getString("id").equals(choice)?pair.longConfig:pair.shortConfig;
        source.put("layout_group",new JSONArray(variant.getJSONArray("layout_group").toString()));
        JSONObject resources=new JSONObject(variant.getJSONObject("wmRes").toString());
        // Background names are persisted user choices. Some historical long
        // layouts rename a color; retain the names of this native template.
        JSONArray before=source.getJSONObject("wmRes").getJSONArray("background"),after=resources.getJSONArray("background");
        if(before.length()!=after.length())throw new JSONException("Watermark background contract differs");
        String layout=source.getJSONArray("layout_group").toString();
        for(int i=0;i<before.length();i++){
            String oldName=before.getJSONObject(i).getString("name"),newName=after.getJSONObject(i).getString("name");
            after.getJSONObject(i).put("name",oldName);layout=layout.replace("@background="+newName,"@background="+oldName);
        }
        source.put("layout_group",new JSONArray(layout));source.put("wmRes",resources);
        source.put("version",variant.get("version"));
        return source.toString();
    }
}
