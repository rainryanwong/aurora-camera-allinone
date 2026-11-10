package com.aurora.camera.compat.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the OEM filename-encoded workspace without loading or rebinding it. */
public final class RecipeCatalog219 {
    public static final String[] COLORS={"tone","color_tone","color_temp","texture","vibrance","noise","soft_light","vignette"};
    public static final int ORIGINAL=66048;
    public static final class Recipe {
        public final File directory;public final String kind,id,name;public final int filter,order;public final int[] colors;
        Recipe(File directory,String kind,String id,String name,int filter,int order,int[] colors){this.directory=directory;this.kind=kind;this.id=id;this.name=name;this.filter=filter;this.order=order;this.colors=colors.clone();}
        public String identity(){return kind+"/"+id;}
    }
    public static Recipe read(File directory,String kind)throws IOException {
        String id=directory.getName();String[] parts=id.split("_",4);
        if(parts.length!=4||!parts[0].equals(kind)||parts[1].equals("official")||parts[3].isEmpty())return null;
        File canonical=directory.getCanonicalFile();
        // /data/user/0 and /data/data can name the same app directory. Compare
        // resolved parents instead of rejecting every legitimate parent alias.
        if(!canonical.getParentFile().equals(directory.getParentFile().getCanonicalFile())||!directory.isDirectory())return null;
        String[] files=directory.list();if(files==null)throw new IOException("Cannot read recipe "+id);
        int filter=ORIGINAL,order=Integer.MAX_VALUE;int[] values=new int[COLORS.length];boolean[] seen=new boolean[COLORS.length];boolean hasFilter=false;
        for(String file:files){
            if(file.startsWith("o_"))order=number(file.substring(2),0,Integer.MAX_VALUE,id);
            String prefix="p_pref_camera_shader_coloreffect_key_";
            if(file.startsWith(prefix)){if(hasFilter)throw new IOException("Ambiguous base filter: "+id);hasFilter=true;filter=number(file.substring(prefix.length()),0,Integer.MAX_VALUE,id);}
            for(int i=0;i<COLORS.length;i++){
                prefix="p_pref_qc_camera_style_"+COLORS[i]+"_key_";
                if(!file.startsWith(prefix))continue;
                if(seen[i])throw new IOException("Ambiguous color parameter: "+id+"/"+COLORS[i]);
                seen[i]=true;values[i]=number(file.substring(prefix.length()),-100,100,id);
            }
        }
        return new Recipe(directory,kind,id,parts[3],filter,order,values);
    }
    private static int number(String text,int min,int max,String id)throws IOException {
        try{int value=Integer.parseInt(text);if(value<min||value>max)throw new NumberFormatException();return value;}
        catch(NumberFormatException failure){throw new IOException("Invalid recipe parameter: "+id+"/"+text,failure);}
    }
    public static List<Recipe> scan(File workspace,Map<String,Set<String>> hidden)throws IOException {
        ArrayList<Recipe> result=new ArrayList<>();
        for(String kind:Arrays.asList("MiCamStyleRear","MiCamStyleFront")){
            File root=new File(workspace,kind);if(!root.exists())continue;File[] entries=root.listFiles();if(entries==null)throw new IOException("Cannot list workspace "+kind);
            Set<String> hiddenIds=hidden.getOrDefault(kind,Collections.emptySet());
            for(File entry:entries){
                if(hiddenIds.contains(entry.getName()))continue;
                try{Recipe recipe=read(entry,kind);if(recipe!=null)result.add(recipe);}
                catch(IOException invalid){System.err.println("AuroraCameraCompat invalid shared recipe: "+invalid.getMessage());}
            }
        }
        result.sort(Comparator.comparingInt((Recipe r)->r.order).thenComparing(Recipe::identity));return result;
    }
    private RecipeCatalog219(){}
}
