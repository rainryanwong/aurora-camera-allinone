package com.aurora.camera.compat.filter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Configuration-only backup. External IDs are matched, never used as paths. */
public final class WorkspaceBackup215 {
    public static final int LIMIT=1024*1024;
    private static final String STOCK="e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07";
    private WorkspaceBackup215(){}
    public static JSONObject encode(String kind,List<String> ids,Set<String> hidden)throws JSONException {
        JSONArray rows=new JSONArray();for(String id:ids)rows.put(new JSONObject().put("id",id).put("enabled",!hidden.contains(id)));
        return new JSONObject().put("format","aurora-camstyle-workspace").put("version",2).put("camera_sha256",STOCK).put("workspace",kind).put("entries",rows);
    }
    public static LinkedHashMap<String,Boolean> parse(JSONObject root,String kind)throws JSONException {
        if(!"aurora-camstyle-workspace".equals(root.getString("format"))||root.getInt("version")!=2||!STOCK.equals(root.getString("camera_sha256"))||!kind.equals(root.getString("workspace")))throw new JSONException("备份格式、相机版本或滤镜分组不匹配");
        JSONArray rows=root.getJSONArray("entries");if(rows.length()>8192)throw new JSONException("滤镜条目数量超出范围");
        LinkedHashMap<String,Boolean> result=new LinkedHashMap<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);Object id=row.get("id"),enabled=row.get("enabled");
            if(!(id instanceof String)||!(enabled instanceof Boolean))throw new JSONException("备份条目类型错误");
            String value=(String)id;
            if(value.isEmpty()||value.length()>1024||value.indexOf('/')>=0||value.indexOf('\\')>=0||value.indexOf('\n')>=0||value.indexOf('\0')>=0||result.containsKey(value))throw new JSONException("备份包含重复或无效的工作区标识");
            result.put(value,(Boolean)enabled);
        }
        return result;
    }
}
