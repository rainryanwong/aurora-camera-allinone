package com.aurora.camera.compat.ambilight;

import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/** The accepted pre-session Task failure -> original camera failure UI adapter. */
public final class LocalInstallFailure implements InvocationHandler {
    private final Object owner;
    private final Field pending;
    private final Field listener;
    private final Class<?> splitException;
    private final Method errorCode;
    private final Method createState;
    private final Method stateUpdate;

    public LocalInstallFailure(Object owner, Field pending, Field listener,
            Class<?> splitException, Method errorCode, Method createState,
            Method stateUpdate) {
        this.owner = owner;
        this.pending = pending;
        this.listener = listener;
        this.splitException = splitException;
        this.errorCode = errorCode;
        this.createState = createState;
        this.stateUpdate = stateUpdate;
    }

    @Override public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return proxy == args[0];
            if ("toString".equals(method.getName())) return "AmbilightLocalInstallFailure";
        }
        if (!"onFailure".equals(method.getName()) || args.length != 1
                || !(args[0] instanceof Exception)) {
            throw new IllegalArgumentException("Unexpected Ambilight failure callback " + method);
        }
        Exception failure = (Exception) args[0];
        Log.e("FML", "Ambilight startInstall Task failed before session callback", failure);
        ConcurrentHashMap<?, ?> tasks = (ConcurrentHashMap<?, ?>) pending.get(owner);
        if (tasks.remove("ambilight") == null) return null;
        int code = -100;
        if (splitException.isInstance(failure)) code = (Integer) call(errorCode, failure);
        Bundle state = new Bundle();
        state.putInt("status", 6);
        state.putInt("error_code", code);
        ArrayList<String> names = new ArrayList<>();
        names.add("ambilight");
        state.putStringArrayList("module_names", names);
        call(stateUpdate, listener.get(owner), call(createState, null, state));
        return null;
    }

    private static Object call(Method method, Object receiver, Object... args)
            throws Throwable {
        try {
            return method.invoke(receiver, args);
        } catch (InvocationTargetException wrapper) {
            throw wrapper.getCause();
        }
    }
}
