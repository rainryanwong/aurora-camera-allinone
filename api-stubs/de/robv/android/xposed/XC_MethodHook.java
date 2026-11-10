package de.robv.android.xposed;

import java.lang.reflect.Member;

/** Compile-only API declaration; excluded from the module DEX. */
public abstract class XC_MethodHook {
    public XC_MethodHook() {}
    public XC_MethodHook(int priority) {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static final class MethodHookParam {
        public Member method;
        public Object thisObject;
        public Object[] args;
        public Object getResult() { return null; }
        public void setResult(Object result) {}
        public Throwable getThrowable() { return null; }
        public boolean hasThrowable() { return false; }
        public void setThrowable(Throwable throwable) {}
        public Object getResultOrThrowable() throws Throwable { return null; }
        public void setObjectExtra(String key, Object value) {}
        public Object getObjectExtra(String key) { return null; }
    }

    public final class Unhook {
        public void unhook() {}
    }
}
