package de.robv.android.xposed;

/** Compile-only API declaration; excluded from the module DEX. */
public final class XposedHelpers {
    public static Object callMethod(Object receiver,String name,Object... args){return null;}
    public static Object callStaticMethod(Class<?> owner,String name,Object... args){return null;}
    public static Object newInstance(Class<?> owner,Object... args){return null;}
    public static Object getObjectField(Object receiver,String name){return null;}
    public static void setObjectField(Object receiver,String name,Object value){}
    public static void setIntField(Object receiver,String name,int value){}
    public static void setBooleanField(Object receiver,String name,boolean value){}
    public static Object getAdditionalInstanceField(Object receiver, String key) { return null; }
    public static Object setAdditionalInstanceField(Object receiver, String key, Object value) {
        return null;
    }
    public static Object removeAdditionalInstanceField(Object receiver, String key) { return null; }
}
