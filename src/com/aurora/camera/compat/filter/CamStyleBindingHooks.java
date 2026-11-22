package com.aurora.camera.compat.filter;

import android.util.Log;
import com.aurora.camera.compat.HookPlan;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.diagnostics.HookDiagnostics;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** Rebind cached CamStyle rows before the panel reads their display metadata. */
public final class CamStyleBindingHooks implements HookPlan {
    private static final String CATALOG = "aurora.camstyle.catalog";
    private static final String REFRESH = "aurora.camstyle.refresh";
    private final FilterWorkspace215 workspace;
    private final Class<?> filterType;
    private final Field panelWorkspace, panelMode, panelAdapter, rowBinding, originalCache, itemId;
    private final Method show, initialize, cloudInitialize, cloudCatalog, repository, component;
    private final Method items, key, parameter, notifyAdapter;

    public CamStyleBindingHooks(Host host) throws ReflectiveOperationException {
        workspace = new FilterWorkspace215(host);
        filterType = host.type("r2.t");
        Class<?> panel = host.type("com.android.camera.features.mode.capture.T");
        show = host.method(panel, "eu");
        panelWorkspace = host.field("U9.M", "V");
        panelAdapter = host.field("U9.M", "Q");
        panelMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        rowBinding = host.field("com.android.camera.features.mode.capture.e", "M");
        originalCache = host.field("com.android.camera.features.mode.capture.e", "P");
        itemId = host.field("com.android.camera.data.data.d", "r");
        initialize = host.method("v2.U", "e", int.class);
        cloudInitialize = host.method("v2.U", "d", int.class, Map.class);
        cloudCatalog = host.method("ti.e", "b");
        repository = host.method("g2.a", "a");
        component = host.method("Xh.b", "x", Class.class);
        items = host.method("r2.a", "getItems");
        key = host.method("v2.U", "getKey", int.class);
        parameter = host.method("U9.S", "H", String.class);
        notifyAdapter = host.method("androidx.recyclerview.widget.RecyclerView$g", "notifyDataSetChanged");
    }

    @Override public String id() { return "camstyle-filter-bindings"; }

    private int catalogMode(int mode) {
        return mode * 2 + (workspace.frontFacing() ? 1 : 0);
    }

    private int rebind(Object owner, int mode) throws IllegalAccessException {
        Object filter = call(component, call(repository, null), filterType);
        Integer oldMode = (Integer) XposedHelpers.getAdditionalInstanceField(filter, CATALOG);
        boolean rebuild = !Integer.valueOf(catalogMode(mode)).equals(oldMode);
        if (rebuild) call(cloudInitialize, filter, mode, call(cloudCatalog, null));

        List<?> current = (List<?>) call(items, filter);
        Map<String,Object> byId = new HashMap<>();
        for (Object item : current) byId.putIfAbsent((String) read(itemId, item), item);
        String modeKey = (String) call(key, filter, mode);
        int rebound = 0;
        int corrected = 0;
        for (Object row : workspace.raw(owner)) {
            Object item = byId.get(call(parameter, row, modeKey));
            Object previous = rowBinding.get(row);
            if (item == null || item == previous) continue;
            if (previous != null && !read(itemId, item).equals(read(itemId, previous))) corrected++;
            rowBinding.set(row, item);
            originalCache.set(row, null);
            rebound++;
        }
        Log.i("AuroraCamStyleBindings", "mode=" + mode + " catalog=" + current.size()
                + " rebuilt=" + rebuild + " rebound=" + rebound + " corrected=" + corrected);
        return rebound;
    }

    @Override public void install() {
        StrictHook track = new StrictHook(1500) {
            @Override protected void after(MethodHookParam param) {
                if (!param.hasThrowable() && filterType.isInstance(param.thisObject)) {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, CATALOG,
                            catalogMode((Integer) param.args[0]));
                }
            }
        };
        HookDiagnostics.hookMethod(initialize, track);
        HookDiagnostics.hookMethod(cloudInitialize, track);
        HookDiagnostics.hookMethod(show, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws Throwable {
                Object owner = read(panelWorkspace, param.thisObject);
                int mode = panelMode.getInt(param.thisObject);
                if (!workspace.camStyle.isInstance(owner) || mode != workspace.currentMode()) return;
                CamStyleAccess260.lock();
                try {
                    if (rebind(owner, mode) > 0) param.setObjectExtra(REFRESH, read(panelAdapter, param.thisObject));
                } finally {
                    CamStyleAccess260.unlock();
                }
            }

            @Override protected void after(MethodHookParam param) {
                Object adapter = param.getObjectExtra(REFRESH);
                if (!param.hasThrowable() && adapter != null) call(notifyAdapter, adapter);
            }
        });
    }
}
