package com.aurora.camera.compat.sharedlut;

/** Pure caller-policy boundary, also exercised by JVM fixtures. UID is the Android app security principal. */
public final class LutShare246Access {
    public static final String CAMERA="com.android.camera", EDITOR="com.miui.mediaeditor";
    public static final String CERT_SHA256="c9009d01ebf9f5d0302bc71b2fe9aa9a47a432bba17308a3111b75d7b2149025";
    public static final String MODULE_CERT_SHA256="08e04cdba0d0374cee03318fb4e886fb6b4b4a0365d16ecc5be8fa617721c2dc";
    private LutShare246Access() {}
    static byte[] certificateBytes(String certificate) {
        byte[] bytes=new byte[32];
        for(int i=0;i<bytes.length;i++) bytes[i]=(byte)Integer.parseInt(certificate.substring(i*2,i*2+2),16);
        return bytes;
    }
    public static void enforce(boolean write,int uid,String[] uidPackages,int cameraUid,int editorUid,
                               boolean cameraSigned,boolean editorSigned) {
        boolean camera=false,editor=false;
        if(uidPackages!=null) for(String name:uidPackages) {
            camera |= CAMERA.equals(name) && uid==cameraUid && cameraSigned;
            editor |= EDITOR.equals(name) && uid==editorUid && editorSigned;
        }
        if(!(write?camera:(camera||editor))) throw new SecurityException("LUT caller is not authorized");
    }
}
