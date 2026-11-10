package com.aurora.camera.compat.filter;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Reads the persisted native order without loading a different camera mode. */
public final class WorkspaceFilterOrder270 {
    public static final class Row {
        public final String identity;
        public final int filter,order;
        public final boolean official;
        Row(String identity,int filter,int order,boolean official){this.identity=identity;this.filter=filter;this.order=order;this.official=official;}
    }
    private WorkspaceFilterOrder270(){}
    private static final Map<String,List<Row>> cached=new HashMap<>();
    private static long revision;
    public static synchronized void invalidate(){cached.clear();revision++;}
    public static synchronized long revision(){return revision;}
    public static synchronized List<Row> cached(Context context,String kind)throws IOException{
        String key=context.getFilesDir().getAbsolutePath()+"/"+kind;
        List<Row> rows=cached.get(key);if(rows==null){rows=Collections.unmodifiableList(read(context,kind));cached.put(key,rows);}return rows;
    }
    public static List<Row> read(Context context,String kind)throws IOException{
        File root=new File(context.getFilesDir(),"Workspace/"+kind);
        if(!root.exists())return Collections.emptyList();
        File[] directories=root.listFiles();if(directories==null)throw new IOException("Cannot read filter workspace "+kind);
        List<Row> rows=new ArrayList<>();
        for(File directory:directories){
            if(!directory.isDirectory()||!directory.getName().startsWith(kind+"_"))continue;
            String[] markers=directory.list();if(markers==null)throw new IOException("Cannot read filter order "+directory.getName());
            int order=Integer.MAX_VALUE,filter=RecipeCatalog219.ORIGINAL;
            try{for(String marker:markers){
                if(marker.startsWith("o_"))order=Integer.parseInt(marker.substring(2));
                String prefix="p_pref_camera_shader_coloreffect_key_";
                if(marker.startsWith(prefix))filter=Integer.parseInt(marker.substring(prefix.length()));
            }}catch(NumberFormatException invalid){throw new IOException("Invalid filter order "+directory.getName(),invalid);}
            rows.add(new Row(directory.getName(),filter,order,directory.getName().startsWith(kind+"_official_")));
        }
        rows.sort(Comparator.comparingInt((Row row)->row.order).thenComparing(row->row.identity));
        return rows;
    }
}
