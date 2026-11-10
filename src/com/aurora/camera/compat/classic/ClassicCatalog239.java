package com.aurora.camera.compat.classic;

/** Retained Phoenix profiles, shared foldable scene LUTs and stock-1770 native. */
public final class ClassicCatalog239 {
    public static final String[] IDS={"passthrough","m9","neutral","12su","fuxi","13u","zizhan","bixi","native"};
    public static final String[] LABELS={"透传","M9","中性","小米 12S Ultra","小米 13","小米 13 Ultra","小米 MIX Fold 2","小米 MIX Flip 2","跟随原生"};
    public static final String[] SCENES={"common","food","human","night","plants","sunrise_sunset"};
    private ClassicCatalog239(){}
    public static int index(String model){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(model))return i;throw new IllegalArgumentException("Unknown classic style: "+model);}
    public static String savedModel(String model){
        if("14u".equals(model)||"17u".equals(model)||"18pm".equals(model)||"blackwhite".equals(model))return "native";
        index(model);return model;
    }
    /** Native bixi scene aliases, shared by preview decoding and MIVI publication. */
    public static String assetScene(String model,String scene){
        if("bixi".equals(model))switch(scene){
            case "food":case "human":case "plants":case "sunrise_sunset":return "common";
        }
        return scene;
    }
    public static String label(String model){return LABELS[index(model)];}
    public static String scene(String token){
        if(token==null||!token.startsWith("lut_cvstyle_"))return null;
        String value=token.substring(12);
        for(String prefix:new String[]{"fuxi_","bixi_","zizhan_"})if(value.startsWith(prefix)){value=value.substring(prefix.length());break;}
        for(String scene:SCENES)if(scene.equals(value))return scene;
        return null;
    }
    public static Boolean asd(Boolean nativeValue,Boolean override,int mode){return mode==163&&nativeValue!=null&&override!=null?override:nativeValue;}
}
