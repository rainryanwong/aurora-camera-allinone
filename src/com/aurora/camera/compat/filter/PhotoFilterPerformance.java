package com.aurora.camera.compat.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.aurora.camera.compat.HookPlan;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** Prepares the native Photo workspace and shares decoded LUTs between its Candy renderers. */
public final class PhotoFilterPerformance implements HookPlan {
    private static final int PHOTO = 163;
    private static final String TAG = "AuroraPhotoFilters";
    private final FilterWorkspace215 workspace;
    private final Method firstFrame, open, resumed, findIndex, render, loadBitmap;
    private final Field panelMode, lutDebugMode;
    private final Class<?> rearPanel, frontPanel, renderer;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Executor worker = Executors.newSingleThreadExecutor(task ->
            new Thread(task, "AuroraPhotoFilters"));
    private final boolean[] loading = new boolean[2];
    private final boolean[] prepared = new boolean[2];
    private final Map<Object, Boolean> pendingPanels = new WeakHashMap<>();
    private final ThreadLocal<Boolean> candyRender = new ThreadLocal<>();
    private final Map<String, WeakReference<Bitmap>> decoded = new HashMap<>();

    public PhotoFilterPerformance(Host host) throws ReflectiveOperationException {
        workspace = new FilterWorkspace215(host);
        firstFrame = host.method("com.android.camera.features.mode.capture.CaptureModule",
                "notifyFirstFrameArrived", int.class);
        open = host.method("com.android.camera.features.mode.capture.T", "Ot");
        resumed = host.method("androidx.fragment.app.Fragment", "isResumed");
        panelMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        rearPanel = host.type("com.android.camera.features.mode.capture.X");
        frontPanel = host.type("com.android.camera.features.mode.capture.W");
        findIndex = host.method("U9.a", "B", workspace.rowClass);
        renderer = host.type("Tu.j");
        render = host.method("Tu.h", "g", host.type("Iu.w"));
        loadBitmap = host.method("com.xiaomi.utils.OpenGl3dUtils", "a",
                Context.class, String.class, boolean.class, boolean.class);
        lutDebugMode = host.field("Nu.i", "a");
    }

    @Override public String id() { return "photo-filter-performance"; }

    private void prepare(boolean front) {
        int facing = front ? 1 : 0;
        if (prepared[facing] || loading[facing] || workspace.currentMode() != PHOTO
                || workspace.frontFacing() != front) return;
        loading[facing] = true;
        worker.execute(() -> {
            boolean loaded = false;
            CamStyleAccess260.lock();
            try {
                if (workspace.currentMode() == PHOTO && workspace.frontFacing() == front) {
                    loaded = !workspace.raw(workspace.workspace(front)).isEmpty();
                }
            } catch (IOException | RuntimeException failure) {
                Log.e(TAG, "Camera workspace preparation failed", failure);
            } finally {
                CamStyleAccess260.unlock();
                boolean ready = loaded;
                main.post(() -> complete(front, ready));
            }
        });
    }

    private void complete(boolean front, boolean ready) {
        int facing = front ? 1 : 0;
        loading[facing] = false;
        prepared[facing] = ready;
        pendingPanels.entrySet().removeIf(entry -> {
            if (entry.getValue() != front) return false;
            Object panel = entry.getKey();
            if (panel != null && (Boolean) call(resumed, panel)
                    && (Integer) read(panelMode, panel) == PHOTO && workspace.currentMode() == PHOTO
                    && workspace.frontFacing() == front) call(open, panel);
            return true;
        });
    }

    private String bitmapKey(StrictHook.MethodHookParam param) throws IllegalAccessException {
        if (!Boolean.TRUE.equals(candyRender.get()) || lutDebugMode.getInt(null) != 0
                || workspace.currentMode() != PHOTO) return null;
        String name = (String) param.args[1];
        if (name == null || name.isEmpty() || name.startsWith("lut_cvstyle_") || (Boolean) param.args[3]
                || (Boolean) param.args[2] && Character.isDigit(name.charAt(0))) return null;
        return name;
    }

    @Override public void install() {
        HookDiagnostics.hookMethod(firstFrame, new StrictHook() {
            @Override protected void after(MethodHookParam param) {
                if (!param.hasThrowable() && workspace.currentMode() == PHOTO) {
                    boolean front = workspace.frontFacing();
                    main.post(() -> prepare(front));
                }
            }
        });
        HookDiagnostics.hookMethod(open, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                if ((Integer) read(panelMode, param.thisObject) != PHOTO) return;
                boolean front = frontPanel.isInstance(param.thisObject);
                if ((front || rearPanel.isInstance(param.thisObject)) && loading[front ? 1 : 0]) {
                    pendingPanels.put(param.thisObject, front);
                    param.setResult(null);
                }
            }
        });
        HookDiagnostics.hookMethod(findIndex, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                if (!workspace.camStyle.isInstance(param.thisObject)) return;
                Object selected = param.args[0];
                int index = -1;
                if (selected != null) {
                    Object identity = read(workspace.identity, selected);
                    List<?> rows = (List<?>) call(workspace.getList, param.thisObject);
                    for (int i = 0; i < rows.size(); i++) {
                        if (read(workspace.identity, rows.get(i)).equals(identity)) {
                            index = i;
                            break;
                        }
                    }
                }
                param.setResult(index);
            }
        });
        // Tu.j's Candy path keeps its Bitmap until the renderer is released.
        // The standalone 3D upload path recycles its input and keeps separate ownership.
        HookDiagnostics.hookMethod(render, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                if (renderer.isInstance(param.thisObject)) candyRender.set(true);
            }
            @Override protected void after(MethodHookParam param) {
                if (renderer.isInstance(param.thisObject)) candyRender.remove();
            }
        });
        HookDiagnostics.hookMethod(loadBitmap, new StrictHook() {
            private static final String KEY = "photo-filter-lut";
            @Override protected void before(MethodHookParam param) throws Throwable {
                String key = bitmapKey(param);
                if (key == null) return;
                synchronized (decoded) {
                    WeakReference<Bitmap> reference = decoded.get(key);
                    Bitmap bitmap = reference == null ? null : reference.get();
                    if (bitmap != null) {
                        param.setResult(bitmap);
                        return;
                    }
                    decoded.values().removeIf(ref -> ref.get() == null);
                }
                param.setObjectExtra(KEY, key);
            }
            @Override protected void after(MethodHookParam param) {
                String key = (String) param.getObjectExtra(KEY);
                if (key != null && !param.hasThrowable() && param.getResult() instanceof Bitmap) {
                    synchronized (decoded) {
                        decoded.put(key, new WeakReference<>((Bitmap) param.getResult()));
                    }
                }
            }
        });
    }
}
