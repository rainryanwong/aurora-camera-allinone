package de.robv.android.xposed;

/** Compile-only API declaration; excluded from the module DEX. */
public interface IXposedHookZygoteInit {
    void initZygote(StartupParam param) throws Throwable;

    final class StartupParam {
        public String modulePath;
        public boolean startsSystemServer;
    }
}
