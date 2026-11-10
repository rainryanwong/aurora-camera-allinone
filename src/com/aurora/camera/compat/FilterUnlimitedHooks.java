package com.aurora.camera.compat;

import android.view.View;
import com.aurora.camera.compat.filter.FilterDisplay;
import com.aurora.camera.compat.filter.FilterSelectionNotifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Complete FilterUnlimited R2 behavior without changing the camera's classes. */
public final class FilterUnlimitedHooks {
    private static final String DISPLAY = "aurora.compat.filter.display";
    private static final String NOTIFIER = "aurora.compat.filter.notifier";
    private static final String RESTORE = "aurora.compat.filter.restore";

    private FilterUnlimitedHooks() {}

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        final Access access = new Access(host);
        return new HookPlan() {
            @Override public String id() { return "filter-unlimited"; }

            @Override public void install() {
                XposedBridge.hookMethod(access.capacity, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        param.setResult(Integer.MAX_VALUE);
                    }
                });
                XposedBridge.hookMethod(access.capacityHint, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        int capacity = (Integer) call(access.capacity, param.thisObject);
                        param.setResult(call(access.fragmentString, param.thisObject,
                                0x7f1415fd, new Object[] {capacity}));
                    }
                });
                for (Method comparator : access.comparators) {
                    XposedBridge.hookMethod(comparator, new StrictHook() {
                        @Override protected void before(MethodHookParam param) {
                            param.setResult(Integer.compare(access.order(param.args[0]),
                                    access.order(param.args[1])));
                        }
                    });
                }
                XposedBridge.hookMethod(access.beginLoad, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        XposedHelpers.removeAdditionalInstanceField(param.thisObject, DISPLAY);
                    }
                });
                XposedBridge.hookMethod(access.bind, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (!param.hasThrowable()) access.refresh(param.thisObject, (Integer) param.args[0]);
                    }
                });
                XposedBridge.hookMethod(access.reload, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (!param.hasThrowable()) {
                            boolean changed = access.refresh(param.thisObject, (Integer) param.args[0]);
                            param.setResult((Boolean) param.getResult() | changed);
                        }
                    }
                });
                XposedBridge.hookMethod(access.remove, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        if (!param.hasThrowable() && access.camStyle.isInstance(param.thisObject)) {
                            access.refresh(param.thisObject, (Integer) read(access.workspaceMode, param.thisObject));
                        }
                    }
                });
                XposedBridge.hookMethod(access.findIdentity, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        param.args[0] = access.identity(param.thisObject, (String) param.args[0]);
                    }
                });
                XposedBridge.hookMethod(access.findPosition, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        Object item = param.args[0];
                        if (item == null) { param.setResult(-1); return; }
                        List<Object> rows = access.list(param.thisObject);
                        String identity = access.identity(param.thisObject, access.identity(item));
                        for (int index = 0; index < rows.size(); index++) {
                            if (access.identity(rows.get(index)).equals(identity)) {
                                param.setResult(index);
                                return;
                            }
                        }
                        param.setResult(-1);
                    }
                });
                XposedBridge.hookMethod(access.selectionNotify, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        Object owner = param.thisObject;
                        if (!access.camStyle.isInstance(read(access.ownerWorkspace, owner))
                                || read(access.ownerAdapter, owner) == null) return;
                        access.notifier(owner).enqueue((Integer) param.args[0], (Integer) param.args[1]);
                        if ((Integer) param.args[1] >= 0) call(access.feedback,
                                call(access.feedbackInstance, null));
                        param.setResult(null);
                    }
                });

                // The stock method parses EVERY directory and sorts all accepted
                // objects before the final 65-entry append loop. Keep that entire
                // method, including its original error paths and manual-pref edits.
                // Observe its parser results only within this exact restore call,
                // then append the already-parsed tail after a successful return.
                final ThreadLocal<RestoreCapture> restoring = new ThreadLocal<>();
                XposedBridge.hookMethod(access.restore, new StrictHook() {
                    @Override protected void before(MethodHookParam param) {
                        boolean enabled = access.camStyle.isInstance(param.thisObject);
                        RestoreCapture capture = new RestoreCapture(param.thisObject,
                                (Integer) param.args[0], enabled,
                                enabled ? access.rawList(param.thisObject) : null, restoring.get());
                        param.setObjectExtra(RESTORE, capture);
                        restoring.set(capture);
                    }
                    @Override protected void after(MethodHookParam param) throws Throwable {
                        RestoreCapture capture = (RestoreCapture) param.getObjectExtra(RESTORE);
                        restoring.set(capture.previous);
                        if (capture.enabled && !param.hasThrowable() && Boolean.TRUE.equals(param.getResult())) {
                            access.appendRestoredTail(capture);
                        }
                    }
                });
                XposedBridge.hookMethod(access.parseRow, new StrictHook() {
                    @Override protected void after(MethodHookParam param) {
                        RestoreCapture capture = restoring.get();
                        if (capture != null && capture.enabled && !param.hasThrowable()
                                && (Integer) param.args[0] == capture.mode && param.getResult() != null) {
                            capture.parsed.add(param.getResult());
                        }
                    }
                });
            }
        };
    }

    private static final class RestoreCapture {
        final Object owner;
        final int mode;
        final boolean enabled;
        final List<Object> originalList;
        final int originalSize;
        final RestoreCapture previous;
        final ArrayList<Object> parsed = new ArrayList<>();
        RestoreCapture(Object owner, int mode, boolean enabled, List<Object> originalList,
                RestoreCapture previous) {
            this.owner = owner;
            this.mode = mode;
            this.enabled = enabled;
            this.originalList = originalList;
            this.originalSize = enabled ? originalList.size() : 0;
            this.previous = previous;
        }
    }

    /** Exact 1770 accessors resolved before any hook registration. */
    private static final class Access implements FilterDisplay.Rows {
        final Class<?> camStyle;
        final Field rowIdentity, rowPath, rowSource, rowStyle, rowSavedStyle, rowBinding;
        final Field rowVersion, rowOrder, rowOfficial, rowSelected, rowCurrent, rowOriginal;
        final Field boundValue, workspaceMode, itemList, ownerView, ownerAdapter, ownerWorkspace;
        final Method capacity, capacityHint, fragmentString, beginLoad, bind, reload, remove;
        final Method findIdentity, findPosition, selectionNotify, restore, parseRow, getList;
        final Method repository, repositoryGet, componentGet, componentSet;
        final Method feedbackInstance, feedback, adapterCount, adapterNotify;
        final Method[] comparators;
        final Comparator<Object> orderComparator;
        final Class<?> selectionComponent;

        Access(Host host) throws ReflectiveOperationException {
            Class<?> base = host.type("U9.a");
            Class<?> row = host.type("U9.S");
            Class<?> effectRow = host.type("com.android.camera.features.mode.capture.e");
            Class<?> owner = host.type("U9.M");
            Class<?> adapter = host.type("U9.b");
            Class<?> workspaces = host.type("com.xiaomi.microfilm.vlog.vv.v");
            Class<?> binding = host.type("com.android.camera.data.data.d");
            Class<?> fragment = host.type("androidx.fragment.app.Fragment");
            Class<?> recycler = host.type("androidx.recyclerview.widget.RecyclerView");
            Class<?> recyclerAdapter = host.type("androidx.recyclerview.widget.RecyclerView$g");
            Class<?> feedbackType = host.type("Rr.d");
            camStyle = host.type("com.android.camera.features.mode.capture.d");
            selectionComponent = host.type("r2.Z0");
            if (!base.isAssignableFrom(camStyle) || !row.isAssignableFrom(effectRow)
                    || !View.class.isAssignableFrom(recycler)) {
                throw new NoSuchFieldException("Filter host inheritance contract");
            }
            rowIdentity = field(host, row, "a", String.class);
            rowPath = field(host, row, "g", String.class);
            rowSource = field(host, row, "k", String.class);
            rowStyle = field(host, row, "H", String.class);
            rowSavedStyle = field(host, row, "s", String.class);
            rowBinding = field(host, effectRow, "M", binding);
            rowVersion = field(host, row, "d", int.class);
            rowOrder = field(host, row, "K", int.class);
            rowOfficial = field(host, row, "p", boolean.class);
            rowSelected = field(host, row, "n", boolean.class);
            rowCurrent = field(host, row, "e", HashMap.class);
            rowOriginal = field(host, row, "f", HashMap.class);
            boundValue = field(host, binding, "r", String.class);
            workspaceMode = field(host, base, "a", int.class);
            itemList = field(host, workspaces, "mItemList", List.class);
            ownerView = field(host, owner, "O", recycler);
            ownerAdapter = field(host, owner, "Q", adapter);
            ownerWorkspace = field(host, owner, "V", base);
            capacity = method(host, "com.android.camera.features.mode.capture.f", "Zs", int.class, false);
            capacityHint = method(host, "com.android.camera.features.mode.capture.f", "gt", String.class, false);
            fragmentString = method(host, fragment.getName(), "getString", String.class, false,
                    int.class, Object[].class);
            beginLoad = method(host, camStyle.getName(), "G", void.class, false, int.class);
            bind = method(host, camStyle.getName(), "f", void.class, false, int.class);
            reload = method(host, camStyle.getName(), "Q", boolean.class, false, int.class,
                    host.type("android.content.Context"), ConcurrentHashMap.class);
            remove = method(host, base.getName(), "H", void.class, false, row);
            findIdentity = method(host, base.getName(), "s", row, false, String.class);
            findPosition = method(host, base.getName(), "B", int.class, false, row);
            selectionNotify = method(host, owner.getName(), "St", void.class, false, int.class, int.class);
            restore = method(host, base.getName(), "restoreWorkspace", boolean.class, false, int.class);
            parseRow = method(host, row.getName(), "N", row, true,
                    int.class, String.class, Class.class, String.class);
            getList = method(host, workspaces.getName(), "getList", List.class, false);
            repository = method(host, "g2.a", "a", host.type("r2.n1"), true);
            repositoryGet = method(host, "Xh.b", "x", Object.class, false, Class.class);
            componentGet = method(host, "com.android.camera.data.data.c", "getComponentValue",
                    String.class, false, int.class);
            componentSet = method(host, "com.android.camera.data.data.c", "setComponentValue",
                    void.class, false, int.class, String.class);
            feedbackInstance = method(host, feedbackType.getName(), "a", feedbackType, true);
            feedback = method(host, feedbackType.getName(), "r", void.class, false);
            adapterCount = method(host, recyclerAdapter.getName(), "getItemCount", int.class, false);
            adapterNotify = method(host, recyclerAdapter.getName(), "notifyItemChanged", void.class, false,
                    int.class, Object.class);
            comparators = new Method[] {
                method(host, "com.android.camera.features.mode.capture.b", "compare", int.class, false,
                        Object.class, Object.class),
                method(host, "com.android.camera.features.mode.capture.c", "compare", int.class, false,
                        Object.class, Object.class),
                method(host, "U9.a$b", "compare", int.class, false, Object.class, Object.class)
            };
            // capture.d.J is final and returns true in stock 1770. Its restore
            // comparator is therefore exactly U9.a$b, the same fixed R2 order.
            Method sortPolicy = method(host, camStyle.getName(), "J", boolean.class, false);
            if (!Modifier.isFinal(sortPolicy.getModifiers())) {
                throw new NoSuchMethodException("CamStyle final J() sort policy contract");
            }
            Class<?> comparator = host.type("U9.a$b");
            if (!Comparator.class.isAssignableFrom(comparator)) {
                throw new NoSuchMethodException("CamStyle order comparator contract");
            }
            // R8's DEX class has no declared <init>; its caller invokes Object's
            // constructor directly. Do not invent a reflective no-arg ctor.
            orderComparator = new Comparator<Object>() {
                @Override public int compare(Object first, Object second) {
                    return Integer.compare(order(first), order(second));
                }
            };
        }

        @SuppressWarnings("unchecked") List<Object> list(Object workspace) {
            return (List<Object>) call(getList, workspace);
        }
        @SuppressWarnings("unchecked") List<Object> rawList(Object workspace) {
            return (List<Object>) read(itemList, workspace);
        }
        String identity(Object workspace, String identity) {
            if (camStyle.isInstance(workspace)) {
                FilterDisplay display = (FilterDisplay) XposedHelpers.getAdditionalInstanceField(workspace, DISPLAY);
                if (display != null) return display.identity(identity);
            }
            return identity;
        }
        boolean refresh(Object workspace, int mode) {
            FilterDisplay display = (FilterDisplay) XposedHelpers.getAdditionalInstanceField(workspace, DISPLAY);
            if (display == null) {
                display = new FilterDisplay(this);
                XposedHelpers.setAdditionalInstanceField(workspace, DISPLAY, display);
            }
            Object component = call(repositoryGet, call(repository, null), selectionComponent);
            String before = (String) call(componentGet, component, mode);
            String after = display.normalize(rawList(workspace), before);
            if (!Objects.equals(before, after)) call(componentSet, component, mode, after);
            return display.changed;
        }
        void appendRestoredTail(RestoreCapture capture) {
            if (rawList(capture.owner) != capture.originalList) {
                throw new IllegalStateException("CamStyle restore replaced its item-list identity");
            }
            int inserted = capture.originalList.size() - capture.originalSize;
            if (inserted < 0 || inserted > capture.parsed.size()) {
                throw new IllegalStateException("CamStyle restore append count changed");
            }
            if (inserted == capture.parsed.size()) return;
            Collections.sort(capture.parsed, orderComparator);
            for (int index = 0; index < inserted; index++) {
                if (capture.originalList.get(capture.originalSize + index) != capture.parsed.get(index)) {
                    throw new IllegalStateException("CamStyle restore sorted prefix changed");
                }
            }
            capture.originalList.addAll(capture.parsed.subList(inserted, capture.parsed.size()));
        }
        @SuppressWarnings("unchecked") FilterSelectionNotifier notifier(Object owner) {
            WeakReference<FilterSelectionNotifier> reference = (WeakReference<FilterSelectionNotifier>)
                    XposedHelpers.getAdditionalInstanceField(owner, NOTIFIER);
            FilterSelectionNotifier notifier = reference == null ? null : reference.get();
            if (notifier == null) {
                final WeakReference<Object> ownerReference = new WeakReference<>(owner);
                notifier = new FilterSelectionNotifier(new FilterSelectionNotifier.Owner() {
                    @Override public View view() {
                        Object current = ownerReference.get();
                        return current == null ? null : (View) read(ownerView, current);
                    }
                    @Override public Object adapter() {
                        Object current = ownerReference.get();
                        return current == null ? null : read(ownerAdapter, current);
                    }
                    @Override public int count(Object adapter) { return (Integer) call(adapterCount, adapter); }
                    @Override public void notifyItemChanged(Object adapter, int position) {
                        call(adapterNotify, adapter, position, Boolean.TRUE);
                    }
                });
                // A pending View callback owns the notifier until its batch runs.
                // The attachment itself must not root View/adapter -> Fragment.
                XposedHelpers.setAdditionalInstanceField(owner, NOTIFIER, new WeakReference<>(notifier));
            }
            return notifier;
        }

        @Override public String identity(Object row) { return (String) read(rowIdentity, row); }
        @Override public String path(Object row) { return (String) read(rowPath, row); }
        @Override public String source(Object row) { return (String) read(rowSource, row); }
        @Override public String style(Object row) { return (String) read(rowStyle, row); }
        @Override public String savedStyle(Object row) { return (String) read(rowSavedStyle, row); }
        @Override public boolean hasBinding(Object row) { return read(rowBinding, row) != null; }
        @Override public String boundEffect(Object row) {
            Object binding = read(rowBinding, row);
            return binding == null ? null : (String) read(boundValue, binding);
        }
        @Override public int version(Object row) { return (Integer) read(rowVersion, row); }
        @Override public int order(Object row) { return (Integer) read(rowOrder, row); }
        @Override public boolean officialFlag(Object row) { return (Boolean) read(rowOfficial, row); }
        @Override public boolean selected(Object row) { return (Boolean) read(rowSelected, row); }
        @SuppressWarnings("unchecked") @Override public Map<String, String> current(Object row) {
            return (Map<String, String>) read(rowCurrent, row);
        }
        @SuppressWarnings("unchecked") @Override public Map<String, String> originals(Object row) {
            return (Map<String, String>) read(rowOriginal, row);
        }
        @Override public void originals(Object row, HashMap<String, String> values) { write(rowOriginal, row, values); }
        @Override public void selected(Object row, boolean selected) { write(rowSelected, row, selected); }
    }

    private static Field field(Host host, Class<?> owner, String name, Class<?> type)
            throws ReflectiveOperationException {
        Field result = host.field(owner, name);
        if (result.getType() != type || Modifier.isStatic(result.getModifiers())) {
            throw new NoSuchFieldException(owner.getName() + "." + name + ":" + type.getName());
        }
        return result;
    }
    private static Method method(Host host, String owner, String name, Class<?> result,
            boolean isStatic, Class<?>... parameters) throws ReflectiveOperationException {
        Method method = host.method(owner, name, parameters);
        if (method.getReturnType() != result || Modifier.isStatic(method.getModifiers()) != isStatic) {
            throw new NoSuchMethodException(owner + "." + name + " result/static contract");
        }
        return method;
    }
    private static Object read(Field field, Object receiver) {
        try { return field.get(receiver); }
        catch (IllegalAccessException failure) { throw new IllegalStateException("Resolved filter field inaccessible", failure); }
    }
    private static void write(Field field, Object receiver, Object value) {
        try { field.set(receiver, value); }
        catch (IllegalAccessException failure) { throw new IllegalStateException("Resolved filter field inaccessible", failure); }
    }
    private static Object call(Method method, Object receiver, Object... arguments) {
        try { return method.invoke(receiver, arguments); }
        catch (IllegalAccessException failure) {
            throw new IllegalStateException("Resolved filter method inaccessible: " + method, failure);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IllegalStateException("Filter host invocation failed: " + method, cause);
        }
    }
}
