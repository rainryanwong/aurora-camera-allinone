package de.robv.android.xposed.callbacks;

import android.content.pm.ApplicationInfo;

/** Compile-only API declaration; excluded from the module DEX. */
public final class XC_LoadPackage {
    public static final class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}
