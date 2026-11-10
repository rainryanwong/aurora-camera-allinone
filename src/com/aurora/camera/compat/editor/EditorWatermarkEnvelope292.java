package com.aurora.camera.compat.editor;

import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;

/** Cloudconfig Content protobuf: retain its seed and all unrelated wire fields. */
public final class EditorWatermarkEnvelope292 {
    private EditorWatermarkEnvelope292() {}

    private static final class Wire {
        final byte[] data;
        int at;
        Wire(byte[] data){this.data=data;}
        int number() throws IOException {
            int value=0;
            for(int shift=0;shift<32;shift+=7) {
                if(at>=data.length)throw new IOException("Truncated cloudconfig field");
                int b=data[at++]&255;value|=(b&127)<<shift;
                if((b&128)==0)return value;
            }
            throw new IOException("Cloudconfig field exceeds its wire range");
        }
        void advance(int size) throws IOException {
            if(size<0||size>data.length-at)throw new IOException("Truncated cloudconfig payload");
            at+=size;
        }
    }

    public static String unlock(String source) throws Exception {
        byte[] bytes=Base64.decode(source,Base64.DEFAULT);
        Wire wire=new Wire(bytes);
        int contentStart=-1,contentEnd=-1,payloadStart=-1,payloadEnd=-1,seed=-1;
        while(wire.at<bytes.length) {
            int start=wire.at,tag=wire.number();
            switch(tag&7) {
                case 0:wire.number();break;
                case 1:wire.advance(8);break;
                case 2:
                    int size=wire.number(),payload=wire.at;
                    wire.advance(size);
                    if(tag==10){contentStart=start;contentEnd=wire.at;payloadStart=payload;payloadEnd=wire.at;}
                    if(tag==18&&size>0)seed=bytes[payload]&63;
                    break;
                case 5:wire.advance(4);break;
                default:throw new IOException("Unsupported cloudconfig wire field: "+tag);
            }
        }
        if(contentStart<0||seed<0)throw new IOException("Cloudconfig content or seed is missing");
        byte[] clear=Arrays.copyOfRange(bytes,payloadStart,payloadEnd);
        transform(clear,seed);
        JSONObject item=new JSONObject(new String(clear,StandardCharsets.UTF_8));
        JSONObject config=new JSONObject(item.getString("content"));
        JSONArray groups=config.getJSONArray("groupList");
        boolean changed=false;
        for(int i=0;i<groups.length();i++) {
            JSONObject group=groups.getJSONObject(i);
            if(!"leica".equals(group.getString("groupConfig")))continue;
            group.put("groupSupportProduct",new JSONArray().put("*"));
            JSONArray entries=group.getJSONArray("watermarkList");
            for(int j=0;j<entries.length();j++) {
                JSONObject entry=entries.getJSONObject(j);
                entry.put("supportDeviceList",new JSONArray().put("*"));
                entry.put("unSupportDeviceList",new JSONArray());
                entry.put("restrictPhotoList",new JSONArray());
            }
            changed=true;
        }
        if(!changed)return source;
        item.put("content",config.toString());
        byte[] encrypted=item.toString().getBytes(StandardCharsets.UTF_8);
        transform(encrypted,seed);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        out.write(bytes,0,contentStart);out.write(10);
        int length=encrypted.length;
        while((length&~127)!=0){out.write((length&127)|128);length>>>=7;}
        out.write(length);out.write(encrypted);
        out.write(bytes,contentEnd,bytes.length-contentEnd);
        return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
    }

    /** The native loader XORs the final byte of each UTF-8 code point with seed & 63. */
    private static void transform(byte[] text,int seed) throws IOException {
        for(int at=0;at<text.length;at++) {
            int b=text[at]&255;
            at+=(b&248)==240?3:(b&240)==224?2:(b&224)==192?1:0;
            if(at>=text.length)throw new IOException("Truncated cloudconfig UTF-8 text");
            text[at]^=seed;
        }
    }
}
