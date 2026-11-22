package de.robv.android.xposed;
import java.lang.reflect.Member;
public abstract class XC_MethodHook {
 public XC_MethodHook() {}
 public XC_MethodHook(int priority) {}
 protected void beforeHookedMethod(MethodHookParam p) throws Throwable {}
 protected void afterHookedMethod(MethodHookParam p) throws Throwable {}
 public static final class MethodHookParam { public Member method; public Object[] args; public Object thisObject; public Object getResult(){return null;} public Throwable getThrowable(){return null;} public boolean hasThrowable(){return false;} public void setResult(Object value){} public void setThrowable(Throwable error){} public void setObjectExtra(String key,Object value){} public Object getObjectExtra(String key){return null;} }
 public class Unhook { public Member getHookedMethod(){return null;} public XC_MethodHook getCallback(){return null;} public void unhook(){} }
}
