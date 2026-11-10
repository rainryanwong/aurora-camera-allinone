package com.aurora.camera.compat.vignette;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** Paired root service proof; absence never substitutes a native success. */
public final class AuthReadiness239 {
    public static final File DIRECTORY=new File("/data/user/0/com.android.camera/files/phoenix-vignette");
    private AuthReadiness239(){}
    public static String check()throws Exception{
        File ready=new File(DIRECTORY,"aurora241-ready");if(!ready.isFile())return "暗角成片组件未就绪，请更新 All in one 2.0.30 后重启";
        String boot=new String(Files.readAllBytes(new File("/proc/sys/kernel/random/boot_id").toPath()),StandardCharsets.UTF_8).trim();
        String actual=new String(Files.readAllBytes(ready.toPath()),StandardCharsets.UTF_8).trim();
        if(!("Aurora241 "+boot+" photo-candy-mounted").equals(actual))return "原生组件的版本、开机标识或实际成片库证明不匹配";
        File formula=new File(DIRECTORY,"colorDark.glsl");if(!formula.isFile()||formula.length()==0||formula.length()>16384)return "原生暗角公式缺失或无效";
        for(byte b:Files.readAllBytes(formula.toPath()))if(b==0)return "原生暗角公式包含 NUL";
        return null;
    }
}
