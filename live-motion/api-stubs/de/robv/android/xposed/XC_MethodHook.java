package de.robv.android.xposed;
public abstract class XC_MethodHook {
 protected void beforeHookedMethod(MethodHookParam p) throws Throwable {}
 protected void afterHookedMethod(MethodHookParam p) throws Throwable {}
 public static final class MethodHookParam { public Object[] args; public Object thisObject; public Object getResult(){return null;} public boolean hasThrowable(){return false;} public void setResult(Object value){} public void setObjectExtra(String key,Object value){} public Object getObjectExtra(String key){return null;} }
 public class Unhook {}
}
