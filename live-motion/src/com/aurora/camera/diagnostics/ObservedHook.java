package com.aurora.camera.diagnostics;

import de.robv.android.xposed.XC_MethodHook;
import java.lang.reflect.Member;
import java.util.concurrent.ConcurrentHashMap;

/** Records callback transitions while keeping the original Xposed callback and priority. */
public abstract class ObservedHook extends XC_MethodHook {
    private volatile Member firstMethod;
    private volatile HookDiagnostics.Point firstPoint;
    private volatile ConcurrentHashMap<Member, HookDiagnostics.Point> otherPoints;

    protected ObservedHook() {}
    protected ObservedHook(int priority) { super(priority); }
    protected void runBefore(MethodHookParam param) throws Throwable {}
    protected void runAfter(MethodHookParam param) throws Throwable {}

    synchronized void observe(Member method, HookDiagnostics.Point point) {
        if (firstMethod == null || firstMethod.equals(method)) {
            firstPoint = point;
            firstMethod = method;
        } else {
            if (otherPoints == null) otherPoints = new ConcurrentHashMap<>();
            otherPoints.put(method, point);
        }
    }

    HookDiagnostics.Point point(Member method) {
        if (firstMethod == method || (firstMethod != null && firstMethod.equals(method))) {
            return firstPoint;
        }
        return otherPoints == null ? null : otherPoints.get(method);
    }

    @Override protected final void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Throwable previous = param.getThrowable();
        try {
            runBefore(param);
            result(param, previous, false);
        } catch (Throwable failure) {
            callbackError(param, failure);
            throw failure;
        }
    }

    @Override protected final void afterHookedMethod(MethodHookParam param) throws Throwable {
        Throwable previous = param.getThrowable();
        try {
            runAfter(param);
            result(param, previous, true);
        } catch (Throwable failure) {
            callbackError(param, failure);
            throw failure;
        }
    }

    private void result(MethodHookParam param, Throwable previous, boolean completed) {
        HookDiagnostics.Point point = point(param.method);
        if (point == null) return;
        Throwable current = param.getThrowable();
        if (current != null && current != previous) point.error("callback-error", current);
        else if (completed) point.ran();
    }

    private void callbackError(MethodHookParam param, Throwable failure) {
        HookDiagnostics.Point point = point(param.method);
        if (point != null) point.error("callback-error", failure);
    }
}
