package com.aurora.camera.compat.street;

/** Phoenix UI selection semantics, scoped to the two street module IDs. */
public final class StreetSelection217 {
    private StreetSelection217(){}
    public static int route(int mode,int selection,boolean explicitlySelected){
        if(selection>0&&mode==225)return 229;
        if(selection==0&&explicitlySelected&&mode==229)return 225;
        return mode;
    }
    public static int[] legendRect(int ratio,int screenWidth,int screenHeight,int inset,boolean supported){
        int width=screenWidth-2*inset,height=0;
        if(supported){if(ratio==0)height=width*4/3;else if(ratio==2)height=width*3/2;else if(ratio==4)height=width;}
        if(width<=0||height==0)return null;
        int top=Math.round(screenHeight*0.4397f)-height/2;
        return new int[]{inset,top,inset+width,top+height};
    }
}
