package com.aurora.camera.compat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** Restores the two adapter-only spacers expected by the master filter panel. */
final class DirectorFilterCards {
    private static final int DIRECTOR = 164;
    private static final int VERTICAL_CARD_SIZE = 0x7f0706d0; // R.dimen.c85
    private static final String CARD_SCOPE = "director-filter-cards";

    private DirectorFilterCards() {}

    static Runnable prepare(Host host) throws ReflectiveOperationException {
        Class<?> menuAdapter = host.type("P9.n");
        Class<?> adapterData = host.type("com.android.camera.fragment.d");
        Class<?> adapterLayout = host.type("com.android.camera.fragment.o");
        Class<?> holder = host.type("androidx.recyclerview.widget.RecyclerView$C");
        Class<?> component = host.type("com.android.camera.data.data.c");
        Constructor<?> initializeAdapter = adapterData.getDeclaredConstructor(Context.class, component, boolean.class);
        initializeAdapter.setAccessible(true);
        Field data = host.field(adapterData, "f");
        Method cardWidth = host.method(adapterLayout, "C");
        Method count = host.method(adapterLayout, "getItemCount");
        Method viewType = host.method(adapterLayout, "getItemViewType", int.class);
        Method bind = host.method(adapterLayout, "onBindViewHolder", holder, int.class);
        Method itemText = host.method(adapterData, "u", int.class);
        Field holderView = host.field(holder, "itemView");
        Method initPanel = host.method("com.android.camera.fragment.u0", "initView", View.class);
        Field mode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        Method vertical = host.method("K2.b", "W");
        Method screenWidth = host.method("K2.b", "k");
        String strip = "com.android.camera2.compat.theme.custom.mm.filter.MasterFilterSelectView";
        Method readAdapter = host.method("androidx.recyclerview.widget.RecyclerView", "getAdapter");
        Method initialLayout = host.method(strip, "onGlobalLayout");
        Method scrollToPosition = host.method("androidx.recyclerview.widget.LinearLayoutManager",
                "scrollToPositionWithOffset", int.class, int.class);

        WeakHashMap<Object, ArrayList<?>> padded = new WeakHashMap<>();
        ThreadLocal<Boolean> inDirectorPanel = new ThreadLocal<>();
        ThreadLocal<Integer> initialOffset = new ThreadLocal<>();
        return () -> {
            HookDiagnostics.hookMethod(initPanel, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    param.setObjectExtra(CARD_SCOPE, inDirectorPanel.get());
                    boolean director = mode.getInt(param.thisObject) == DIRECTOR;
                    inDirectorPanel.set(director);
                    param.setObjectExtra("director-filter-panel", director);
                }

                @Override protected void after(MethodHookParam param) {
                    Boolean previous = (Boolean) param.getObjectExtra(CARD_SCOPE);
                    if (previous == null) inDirectorPanel.remove();
                    else inDirectorPanel.set(previous);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-filter-panel"));
                }
            });
            HookDiagnostics.hookMethod(initializeAdapter, new StrictHook() {
                @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                    if (param.hasThrowable() || !menuAdapter.isInstance(param.thisObject)
                            || !Boolean.TRUE.equals(inDirectorPanel.get())) return;
                    padded.put(param.thisObject, null);
                    pad(padded, data, param.thisObject);
                    param.setObjectExtra(CARD_SCOPE, Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(CARD_SCOPE));
                }
            });
            StrictHook keepSpacers = new StrictHook() {
                @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                    if (countsAsRun(param)) pad(padded, data, param.thisObject);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return padded.containsKey(param.thisObject);
                }
            };
            HookDiagnostics.hookMethod(count, keepSpacers);
            HookDiagnostics.hookMethod(itemText, keepSpacers);

            HookDiagnostics.hookMethod(viewType, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object adapter = param.thisObject;
                    if (!padded.containsKey(adapter)) return;
                    int position = (int) param.args[0];
                    int last = pad(padded, data, adapter).size() - 1;
                    if (position != 0 && position != last) return;
                    param.setResult((boolean) vertical.invoke(null) ? (position == 0 ? 2 : 3) : 1);
                    param.setObjectExtra(CARD_SCOPE, Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(CARD_SCOPE));
                }
            });
            HookDiagnostics.hookMethod(bind, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    Object adapter = param.thisObject;
                    if (!padded.containsKey(adapter)) return;
                    int position = (int) param.args[1];
                    int last = pad(padded, data, adapter).size() - 1;
                    if (position != 0 && position != last) return;
                    View spacer = (View) holderView.get(param.args[0]);
                    if ((boolean) vertical.invoke(null)) {
                        int unit = spacer.getResources().getDimensionPixelSize(VERTICAL_CARD_SIZE);
                        spacer.setLayoutParams(new ViewGroup.LayoutParams(1, position == 0 ? unit : unit * 3));
                    } else {
                        int inset = ((int) screenWidth.invoke(null) - (int) cardWidth.invoke(adapter)) / 2;
                        spacer.setLayoutParams(new ViewGroup.LayoutParams(Math.max(inset, 0), 1));
                    }
                    param.setObjectExtra(CARD_SCOPE, Boolean.TRUE);
                    param.setResult(null);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(CARD_SCOPE));
                }
            });

            HookDiagnostics.hookMethod(initialLayout, new StrictHook() {
                @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                    param.setObjectExtra(CARD_SCOPE, initialOffset.get());
                    Object adapter = readAdapter.invoke(param.thisObject);
                    if (!padded.containsKey(adapter) || (boolean) vertical.invoke(null)) {
                        initialOffset.remove();
                        return;
                    }
                    int card = (int) cardWidth.invoke(adapter);
                    initialOffset.set(((int) screenWidth.invoke(null) - card) / 2 - card);
                    param.setObjectExtra("director-filter-snap", Boolean.TRUE);
                }

                @Override protected void after(MethodHookParam param) {
                    Integer previous = (Integer) param.getObjectExtra(CARD_SCOPE);
                    if (previous == null) initialOffset.remove();
                    else initialOffset.set(previous);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra("director-filter-snap"));
                }
            });
            HookDiagnostics.hookMethod(scrollToPosition, new StrictHook() {
                @Override protected void before(MethodHookParam param) {
                    Integer offset = initialOffset.get();
                    if (offset == null) return;
                    param.args[1] = offset;
                    param.setObjectExtra(CARD_SCOPE, Boolean.TRUE);
                }

                @Override protected boolean countsAsRun(MethodHookParam param) {
                    return Boolean.TRUE.equals(param.getObjectExtra(CARD_SCOPE));
                }
            });
        };
    }

    private static ArrayList<?> pad(WeakHashMap<Object, ArrayList<?>> padded, Field data, Object adapter)
            throws IllegalAccessException {
        ArrayList<?> items = (ArrayList<?>) data.get(adapter);
        if (padded.get(adapter) != items) {
            items.add(0, null);
            items.add(null);
            padded.put(adapter, items);
        }
        return items;
    }
}
