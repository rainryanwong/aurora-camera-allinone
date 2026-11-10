package com.aurora.camera.compat.filter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Rec.709 3D .cube -> the native 8x8 tiled, 64^3 RGB texture. */
public final class CubeLut229 {
    public static final int LIMIT=16*1024*1024;
    public final int size;public final float[] values;public final String title;
    private CubeLut229(int size,float[] values,String title){this.size=size;this.values=values;this.title=title;}
    public static CubeLut229 parse(InputStream input)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[8192];
        for(int n;(n=input.read(buffer))!=-1;){if(bytes.size()+n>LIMIT)throw new IOException("LUT 文件超过 16 MiB");bytes.write(buffer,0,n);}
        BufferedReader reader=new BufferedReader(new StringReader(new String(bytes.toByteArray(),StandardCharsets.UTF_8)));
        int size=0,at=0,lineNo=0;float[] data=null;String title="";
        for(String line;(line=reader.readLine())!=null;){lineNo++;if(lineNo==1&&line.startsWith("\ufeff"))line=line.substring(1);int comment=line.indexOf('#');if(comment>=0)line=line.substring(0,comment);line=line.trim();if(line.isEmpty())continue;
            String[] parts=line.split("\\s+");String key=parts[0].toUpperCase(Locale.ROOT);
            if("TITLE".equals(key)){title=line.substring(parts[0].length()).trim();if(title.startsWith("\"")&&title.endsWith("\"")&&title.length()>1)title=title.substring(1,title.length()-1);continue;}
            if("LUT_1D_SIZE".equals(key))throw new IOException("不支持 1D shaper");
            if("LUT_3D_SIZE".equals(key)){
                if(parts.length!=2||size!=0)throw new IOException("LUT_3D_SIZE 重复或无效");
                try{size=Integer.parseInt(parts[1]);}catch(NumberFormatException e){throw new IOException("LUT 阶数无效",e);}
                if(size!=16&&size!=17&&size!=32&&size!=33&&size!=64&&size!=65)throw new IOException("仅支持 16、17、32、33、64、65 阶 3D LUT");data=new float[size*size*size*3];continue;
            }
            if("DOMAIN_MIN".equals(key)||"DOMAIN_MAX".equals(key)){
                if(parts.length!=4)throw new IOException("DOMAIN 维数无效");float expected="DOMAIN_MIN".equals(key)?0f:1f;
                for(int i=1;i<4;i++)if(number(parts[i],lineNo)!=expected)throw new IOException("仅支持 DOMAIN 0–1 的 Rec.709 LUT");continue;
            }
            if(parts.length!=3||data==null||at+3>data.length)throw new IOException("LUT 数据数量或格式错误，第 "+lineNo+" 行");
            for(String part:parts){float value=number(part,lineNo);if(value<0||value>1)throw new IOException("Rec.709 LUT 输出必须位于 0 到 1，第 "+lineNo+" 行");data[at++]=value;}
        }
        if(data==null||at!=data.length)throw new IOException("LUT 数据不完整："+at+" / "+(data==null?0:data.length));
        return new CubeLut229(size,data,title);
    }
    private static float number(String value,int line)throws IOException{try{float f=Float.parseFloat(value);if(!Float.isFinite(f))throw new NumberFormatException();return f;}catch(NumberFormatException e){throw new IOException("LUT 数值无效，第 "+line+" 行",e);}}
    private float at(int r,int g,int b,int component){return values[((b*size+g)*size+r)*3+component];}
    private static float mix(float a,float b,float t){return a+(b-a)*t;}
    public int sample(float r,float g,float b){
        int r0=(int)r,g0=(int)g,b0=(int)b,r1=Math.min(r0+1,size-1),g1=Math.min(g0+1,size-1),b1=Math.min(b0+1,size-1);int pixel=0xff000000;
        for(int c=0;c<3;c++){
            float low=mix(mix(at(r0,g0,b0,c),at(r1,g0,b0,c),r-r0),mix(at(r0,g1,b0,c),at(r1,g1,b0,c),r-r0),g-g0);
            float high=mix(mix(at(r0,g0,b1,c),at(r1,g0,b1,c),r-r0),mix(at(r0,g1,b1,c),at(r1,g1,b1,c),r-r0),g-g0);
            pixel|=Math.round(mix(low,high,b-b0)*255f)<<(16-8*c);
        }return pixel;
    }
    public int[] render(){int[] result=new int[512*512];float scale=(size-1)/63f;
        for(int b=0;b<64;b++)for(int g=0;g<64;g++)for(int r=0;r<64;r++)result[((b/8*64+g)*512)+b%8*64+r]=sample(r*scale,g*scale,b*scale);
        return result;
    }
}
