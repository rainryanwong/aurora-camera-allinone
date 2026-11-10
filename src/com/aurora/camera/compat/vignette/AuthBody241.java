package com.aurora.camera.compat.vignette;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Match the Java template's line boundaries in both native shader loaders. */
public final class AuthBody241 {
    private AuthBody241(){}
    public static String normalize(String body)throws IOException{
        if(body.isEmpty()||body.indexOf(0)>=0)throw new IOException("GLSL 函数体为空或包含 NUL");
        String result=(body.startsWith("\n")?"":"\n")+body+(body.endsWith("\n")?"":"\n");
        if(result.getBytes(StandardCharsets.UTF_8).length>16384)throw new IOException("GLSL 函数体（含首尾换行）超过 16 KiB");
        return result;
    }
}
