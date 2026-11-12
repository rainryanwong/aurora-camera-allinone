package de.robv.android.xposed;
/** Compile-only API declaration. Never packaged into the module. */
public abstract class XC_MethodReplacement extends XC_MethodHook {
 protected abstract Object replaceHookedMethod(MethodHookParam p) throws Throwable;
}
