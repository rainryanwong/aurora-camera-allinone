package com.aurora.camera.compat.legend;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/** Lux/zoom shading tables shared with the M3 capture parameter asset. */
final class M3Parameters260 {
    private final ByteBuffer data;
    private final int shadingData;
    private final int[] zoomTables;
    private int u16(int offset){return data.getShort(offset)&65535;}
    private static void require(boolean value){if(!value)throw new IllegalArgumentException("Invalid M3 parameter asset");}
    M3Parameters260(byte[] bytes){
        data=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN);
        int scenes=u16(0),header=2+scenes*2,offset=u16(header),lutData=u16(header+2),zooms=u16(header+6),indexes=header+8;
        int marker=indexes+zooms*2+832;
        require(zooms>0&&u16(header+4)==17&&marker+8<=bytes.length);
        require(u16(marker)==0&&u16(marker+2)==0&&u16(marker+4)==1&&u16(marker+6)==1);
        int lutCount=0;
        for(int i=0;i<scenes;i++){
            int size=u16(2+i*2),record=offset+64;
            require(size==78&&u16(record)==1&&u16(record+6)==1);
            lutCount=Math.max(lutCount,u16(record+12)+1);offset+=size;
        }
        zoomTables=new int[zooms];float previousZoom=0;int shadingCount=0;
        for(int i=0;i<zooms;i++){
            zoomTables[i]=offset;int size=u16(indexes+i*2),ranges=u16(offset+4);float zoom=data.getFloat(offset);
            require(zoom>previousZoom&&ranges>0&&size==6+ranges*12);
            int previousEnd=0;
            for(int j=0;j<ranges;j++){
                int range=offset+6+j*12,start=u16(range),end=u16(range+2);
                require(start<=end&&(j==0||start>previousEnd)&&u16(range+4)==1&&u16(range+6)==1&&u16(range+8)==10000);
                shadingCount=Math.max(shadingCount,u16(range+10)+1);previousEnd=end;
            }
            offset+=size;previousZoom=zoom;
        }
        shadingData=lutData+lutCount*17*17*17*3;
        require(offset<=lutData&&shadingData+shadingCount*32==bytes.length);
    }
    float[] shading(int lux,float zoom){
        require(lux>=0&&lux<=65535&&Float.isFinite(zoom)&&zoom>0);
        int table=zoomTables[0];for(int i=1;i<zoomTables.length&&zoom>=data.getFloat(zoomTables[i]);i++)table=zoomTables[i];
        int count=u16(table+4),ranges=table+6,index=0;
        while(index+1<count&&lux>u16(ranges+index*12+2))index++;
        int next=ranges+index*12,previous=next;float blend=0;
        if(index>0&&lux<u16(next)){
            previous=next-12;int end=u16(previous+2);blend=(float)(lux-end)/(u16(next)-end);
        }
        int a=shadingData+u16(previous+10)*32,b=shadingData+u16(next+10)*32;float[] result=new float[8];
        for(int i=0;i<8;i++)result[i]=data.getFloat(a+i*4)*(1-blend)+data.getFloat(b+i*4)*blend;
        return result;
    }
    String configuration(int width,int height,int lux,float zoom){
        float[] p=shading(lux,zoom);
        return String.format(Locale.ROOT,"CvStyleEffect;Width=%d;Height=%d;SmoothStartValue=%.9g;SmoothEndValue=%.9g;SmoothCoordScale=%.9g;SmoothValueScale=%.9g;LightDarkPreserveK=%.9g;LightDarkPreserveB=%.9g;LightDarkPreserveV=%.9g;LightDarkPreserveT=%.9g;",width,height,p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7]);
    }
}
