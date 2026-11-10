package com.aurora.camera.compat.filter;

import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Method;
import java.util.concurrent.locks.ReentrantLock;

/** CamStyle workspaces and their shared Photo component form one catalog. */
public final class CamStyleAccess260 {
    private static final ReentrantLock lock=new ReentrantLock();
    private static final String HELD="260.camstyle.lock";
    private CamStyleAccess260(){}
    public static void lock(){lock.lock();}
    public static void unlock(){lock.unlock();}
    // The outermost callback includes native code, R2 normalization and LUT
    // projection. Nested native calls and module wrappers use the same lock.
    static void guard(Method method,Class<?> owner){
        XposedBridge.hookMethod(method,new StrictHook(3000){
            @Override protected void before(MethodHookParam p){
                if(!owner.isInstance(p.thisObject))return;
                lock();p.setObjectExtra(HELD,true);
            }
            @Override protected void after(MethodHookParam p){
                if(p.getObjectExtra(HELD)!=null)unlock();
            }
        });
    }
}
