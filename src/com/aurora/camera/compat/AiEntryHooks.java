package com.aurora.camera.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** F11: suppress the exact obsolete messages and keep local account gates scoped. */
public final class AiEntryHooks {
    private static final String RUN = "aurora.compat.ai.entryRun";
    private static final String ACCOUNT = "aurora.compat.ai.accountScope";
    private AiEntryHooks() {}
    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Method run = host.method("E1.W0", "run");
        final Field variant = host.field("E1.W0", "a");
        final Field activity = host.field("E1.W0", "b");
        final Field cameraHandler = host.field("com.android.camera.a", "V0");
        final Method send = Handler.class.getDeclaredMethod("sendEmptyMessage", int.class);
        final Method handle = host.method("com.android.camera.a$c", "handleMessage", Message.class);
        final Method workflow = host.method("S5.M", "d", host.type("com.android.camera.Camera"),
                ArrayList.class, int.class, host.type("Bo.a"), host.type("Ak.s"));
        final Method account = host.method("S5.f", "b", Context.class);
        if (variant.getType() != int.class || run.getReturnType() != void.class
                || account.getReturnType() != boolean.class || handle.getReturnType() != void.class)
            throw new NoSuchMethodException("F11 entry/message/account contract");
        send.setAccessible(true);
        return new HookPlan() {
            public String id() { return "F11.ai-entry-account-messages"; }
            public void install() {
                final ThreadLocal<Object> runHandler = new ThreadLocal<>();
                final ThreadLocal<Object> accountContext = new ThreadLocal<>();
                XposedBridge.hookMethod(run, new StrictHook() {
                    @Override protected void before(MethodHookParam p) throws Throwable {
                        p.setObjectExtra(RUN, runHandler.get());
                        // The packed-switch's sole entry is variant zero: startup. The default
                        // intent branch returns earlier and must keep its original messages.
                        if (variant.getInt(p.thisObject) == 0)
                            runHandler.set(cameraHandler.get(activity.get(p.thisObject)));
                        else runHandler.remove();
                    }
                    @Override protected void after(MethodHookParam p) {
                        Object previous = p.getObjectExtra(RUN);
                        if (previous == null) runHandler.remove(); else runHandler.set(previous);
                    }
                });
                XposedBridge.hookMethod(send, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        if ((Integer) p.args[0] == 11 && p.thisObject == runHandler.get())
                            p.setResult(false); // No message was enqueued; the deleted call had no result consumer.
                    }
                });
                XposedBridge.hookMethod(handle, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        // Actual packed-switch starts at 1; its removed toast branch is 13.
                        if (((Message) p.args[0]).what == 13) p.setResult(null);
                    }
                });
                XposedBridge.hookMethod(workflow, new StrictHook() {
                    @Override protected void before(MethodHookParam p) {
                        p.setObjectExtra(ACCOUNT, accountContext.get()); accountContext.set(p.args[0]);
                    }
                    @Override protected void after(MethodHookParam p) {
                        Object previous = p.getObjectExtra(ACCOUNT);
                        if (previous == null) accountContext.remove(); else accountContext.set(previous);
                    }
                });
                XposedBridge.hookMethod(account, new StrictHook() {
                    @Override protected void after(MethodHookParam p) {
                        Object expected = accountContext.get();
                        if (!p.hasThrowable() && expected != null && expected == p.args[0]) p.setResult(true);
                    }
                });
            }
        };
    }
}
