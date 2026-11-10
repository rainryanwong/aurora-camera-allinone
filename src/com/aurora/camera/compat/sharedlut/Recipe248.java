package com.aurora.camera.compat.sharedlut;

import java.io.*;
import java.util.Arrays;

/** Camera1770 recipe values, not a baked color LUT. Effect order follows MIVIRenderTag. */
public final class Recipe248 {
    public static final int BASE_BLACK_MIST=8,BASE_WHITE_MIST=16;
    public final boolean original;
    public final int baseFlags;
    private final int[] style;
    private final float[] base;
    public Recipe248(boolean original,int baseFlags,float[] base,int[] style) {
        if((baseFlags&~31)!=0||(baseFlags&(BASE_BLACK_MIST|BASE_WHITE_MIST))==(BASE_BLACK_MIST|BASE_WHITE_MIST)||base==null||base.length!=6||style==null||style.length!=8)
            throw new IllegalArgumentException("Invalid Camera recipe shape");
        for(float value:base)if(Float.isNaN(value)||Float.isInfinite(value)||Math.abs(value)>100)
            throw new IllegalArgumentException("Invalid base filter parameter");
        for(int i=0;i<style.length;i++)if(style[i]<-100||style[i]>100||i==6&&(style[i]<0||style[i]>5))
            throw new IllegalArgumentException("Invalid PictureStyle value at "+i);
        this.original=original;this.baseFlags=baseFlags;this.base=base.clone();this.style=style.clone();
    }
    public int[] style(){return style.clone();}
    public float[] base(){return base.clone();}
    public String identity(){return (original?"1":"0")+":"+baseFlags+":"+Arrays.toString(base)+":"+Arrays.toString(style);}
    /** Mu.g c..i: texture, tone, vibrance, temperature, tint, grain, vignette. */
    public float[] uniforms(){
        float saturation=style[4]/100f;
        return new float[]{style[3]/100f,style[0]/100f,saturation*(saturation<0?2f:1.4f),
                style[2]*2f/100f,style[1]*2f/100f,style[5]/100f,style[7]/100f};
    }
    public int algos(){float[] v=uniforms();int bits=0;int[] masks={64,8,4,1,2,16,32};
        for(int i=0;i<v.length;i++)if(i==5?v[i]>.001f:Math.abs(v[i])>.001f)bits|=masks[i];return bits;}
    public String beforeStyle(){
        if(original)return "";
        StringBuilder s=new StringBuilder();
        if((baseFlags&BASE_BLACK_MIST)!=0)s.append("BlackMistEffect;LumThresh=0.5;Raduis=4.0;MixRatio=1.0@");
        if((baseFlags&BASE_WHITE_MIST)!=0)s.append("WhiteMistEffect;LumThresh=0.1;Raduis=4.0;MixRatio=1.0@");
        s.append("CubeLutEffect;cube_strength=1.0;lut_type=0;");
        if((baseFlags&3)!=0){s.append("@CvEffect;");
            if((baseFlags&1)!=0)s.append("SmoothStartValue=").append(base[0]).append(";Falloff=").append(base[1])
                .append(";SmoothEndValue=").append(base[2]).append(";DarkStrength=").append(base[3]).append(';');
            if((baseFlags&2)!=0)s.append("NoiseStrength=").append(base[4]).append(';');
        }
        if((baseFlags&4)!=0)s.append("@SharpenEffect;SharpenIntensity=").append(base[5]);
        return s.toString();
    }
    public String afterStyle(){
        int soft=style[6];if(soft==0)return "";
        float ratio=new float[]{0,.3f,.5f,.8f,.6f,.8f}[soft];
        return (soft>3?"WhiteMistEffect;LumThresh=0.1":"BlackMistEffect;LumThresh=0.5")+";Raduis=4.0;MixRatio="+ratio;
    }
    public void write(DataOutput output)throws IOException{
        output.writeBoolean(original);output.writeInt(baseFlags);for(float value:base)output.writeFloat(value);for(int value:style)output.writeInt(value);
    }
    public static Recipe248 read(DataInput input)throws IOException{
        boolean original=input.readBoolean();int flags=input.readInt();float[] base=new float[6];int[] style=new int[8];
        for(int i=0;i<base.length;i++)base[i]=input.readFloat();for(int i=0;i<style.length;i++)style[i]=input.readInt();
        try{return new Recipe248(original,flags,base,style);}catch(IllegalArgumentException invalid){throw new IOException("Invalid Camera recipe metadata",invalid);}
    }
}
