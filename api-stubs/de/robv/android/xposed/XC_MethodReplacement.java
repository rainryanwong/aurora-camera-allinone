package de.robv.android.xposed;

/** Compile-only API declaration; excluded from the module DEX. */
public abstract class XC_MethodReplacement extends XC_MethodHook {
    public XC_MethodReplacement() {}
    public XC_MethodReplacement(int priority) { super(priority); }
    public static XC_MethodReplacement returnConstant(final Object result) { return null; }
    public static XC_MethodReplacement returnConstant(int priority, final Object result) { return null; }
    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;
}
