package de.robv.android.xposed;
import java.lang.reflect.Member;
public final class XposedBridge {
 public static XC_MethodHook.Unhook hookMethod(Member m,XC_MethodHook h){return null;}
 public static void log(String s){}
 public static Object invokeOriginalMethod(Member m,Object receiver,Object[] args)
  throws java.lang.reflect.InvocationTargetException,IllegalAccessException {return null;}
}
