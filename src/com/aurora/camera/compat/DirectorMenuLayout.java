package com.aurora.camera.compat;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Shares the director catalog and native editor preference across both menus. */
final class DirectorMenuLayout implements HookPlan {
    private static final int MODE = 164;
    private static final int RESOLUTION = 173;
    private static final int FRAME_RATE = 174;
    private static final int MORE = 197;
    private static final int FOCUS_PEAK = 199;
    private static final int QUALITY = 208;
    private static final int ASPECT_RATIO = 210;
    private static final int BLANK = 216;
    private static final int EXIT = 217;
    private static final int WATERMARK = 223;
    private static final int SETTINGS = 224;
    private static final int PHOTO_CAPTURE_METHOD = 225;
    private static final int EXPOSURE_FEEDBACK = 258;
    private static final int LOG = 260;
    private static final int NATIVE_SLOTS = 6;
    private static final int LEFT_SLOTS = 2;
    private static final int EDITABLE_SLOTS = 3;
    private static final String APPLIED = "director-menu-layout";

    private final Class<?> directorUi;
    private final Class<?> editorType;
    private final Field itemId, itemGravity, itemPosition, itemMenuProvider;
    private final Field rowId, rowIcon, rowAnimation, rowTitle, menuIcon, menuAnimation, menuTitle, fragmentMode, componentMode;
    private final int watermarkIcon;
    private final Field editorTop, editorMenu, editorBlanks, editorInitialRows, adapterItems, editorHolderView;
    private final Field deviceFeatures;
    private final Method nativeMenu, nativeTop, build, readMenuResource, dataGlobal, component;
    private final Method readLayout, device, supportsPeaking, getViewModel, getModeUi;
    private final Method editableIds, defaultLayout, dropdown, topbar, writeLayout, bindEditor, collectEditor;
    private final Method notifyAdapter, bindEditorItem;
    private final Method allocateRow;
    private final Object allocator;
    private final Class<?> rowType;
    private final Host host;
    private DirectorReferenceLines referenceLines;
    private DirectorRecordingFormats recordingFormats;
    private final Map<Object, Boolean> directorMenuAdapters = new WeakHashMap<>();

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        return new DirectorMenuLayout(host);
    }

    private DirectorMenuLayout(Host host) throws ReflectiveOperationException {
        directorUi = host.type("C3.b");
        editorType = host.type("u2.y");
        Class<?> editorFragment = host.type("Y9.q");
        Class<?> item = host.type("b5.i");
        rowType = host.type("com.android.camera.data.data.d");
        Class<?> adapter = host.type("Y9.A");
        Class<?> holder = host.type("androidx.recyclerview.widget.RecyclerView$C");
        // R8 inlines the plain ComponentDataItem constructor in 1770.
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        allocator = host.field(unsafe, "theUnsafe").get(null);
        allocateRow = host.method(unsafe, "allocateInstance", Class.class);
        itemId = host.field(item, "c");
        itemGravity = host.field(item, "a");
        itemPosition = host.field(item, "b");
        itemMenuProvider = host.field(item, "h");
        rowId = host.field(rowType, "r");
        rowIcon = host.field(rowType, "c");
        rowAnimation = host.field(rowType, "j");
        menuAnimation = host.field("b5.a", "b");
        watermarkIcon = host.field("Rh.b", "ic_watermark_top_cloud_config").getInt(null);
        rowTitle = host.field(rowType, "l");
        menuIcon = host.field("b5.a", "a");
        menuTitle = host.field("b5.a", "c");
        fragmentMode = host.field("com.android.camera.fragment.h", "mCurrentMode");
        componentMode = host.field("com.android.camera.data.data.c", "mCurrentMode");
        editorTop = host.field(editorFragment, "g");
        editorMenu = host.field(editorFragment, "h");
        editorBlanks = host.field(editorFragment, "n");
        editorInitialRows = host.field(editorFragment, "o");
        adapterItems = host.field(adapter, "b");
        editorHolderView = host.field(holder, "itemView");
        nativeMenu = host.method(directorUi, "l");
        nativeTop = host.method(directorUi, "e");
        build = host.method("X9.w5", "E", int.class);
        readMenuResource = host.method("b5.i$b", "c", int.class);
        dataGlobal = host.method("g2.a", "g");
        component = host.method("Xh.b", "x", Class.class);
        readLayout = host.method(editorType, "U", int.class);
        device = host.method("Pe.b", "D");
        deviceFeatures = host.field("Pe.b", "e");
        supportsPeaking = host.method(deviceFeatures.getType(), "s4");
        getViewModel = host.method("com.xiaomi.camera.base.ui.fragments.d", "getCameraMainViewModel");
        getModeUi = host.method(getViewModel.getReturnType(), "l");
        editableIds = host.method("Y9.O", "b", int.class, Optional.class);
        defaultLayout = host.method("Y9.O", "f", int.class, Optional.class);
        dropdown = host.method("p5.W", "zs");
        topbar = host.method("X9.i0", "s0", int.class, Optional.class, boolean.class);
        writeLayout = host.method(editorType, "Y", ArrayList.class, boolean.class);
        bindEditor = host.method(editorFragment, "ns", ArrayList.class);
        collectEditor = host.method(editorFragment, "qs");
        notifyAdapter = host.method("androidx.recyclerview.widget.RecyclerView$g", "notifyDataSetChanged");
        bindEditorItem = host.method(adapter, "u", host.type("Y9.T"), String.class);
        this.host = host;
    }

    @Override public String id() {
        return "DirectorMenuLayout";
    }

    @Override public void install() {
        HookDiagnostics.hookMethod(editableIds, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || (int) param.args[0] != MODE) return;
                Object ui = ((Optional<?>) param.args[1]).orElse(null);
                ArrayList<Integer> ids = new ArrayList<>(catalog(ui).keySet());
                ids.removeIf(DirectorMenuLayout::fixedMenuItem);
                param.setResult(ids);
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(defaultLayout, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || (int) param.args[0] != MODE) return;
                Object ui = ((Optional<?>) param.args[1]).orElse(null);
                if (!directorUi.isInstance(ui)) return;
                param.setResult(normalize(ids(param.getResult()), catalog(ui)));
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(dropdown, new LayoutHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                if (fragmentMode.getInt(param.thisObject) != MODE) return;
                Map<Integer, Object> cards = catalog(modeUi(param.thisObject));
                if (cards.isEmpty()) return;
                param.setResult(menuCards(cards, layout()));
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(topbar, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || (int) param.args[0] != MODE) return;
                Object ui = ((Optional<?>) param.args[1]).orElse(null);
                if (!directorUi.isInstance(ui)) return;
                LinkedHashMap<Integer, Object> stock = new LinkedHashMap<>();
                for (Object item : (List<?>) param.getResult()) stock.put(itemId.getInt(item), item);
                if (!stock.containsKey(MORE)) return;
                Map<Integer, Object> cards = catalog(ui);
                List<Integer> saved = normalize(layout(), cards);
                ArrayList<Object> displayed = new ArrayList<>();
                addTop(displayed, stock.get(EXIT), Gravity.START);
                for (int i = 0; i < LEFT_SLOTS; i++) {
                    addTop(displayed, topItem(saved.get(i), cards), Gravity.START);
                }
                addTop(displayed, stock.get(MORE), Gravity.CENTER);
                addTop(displayed, stock.get(QUALITY), Gravity.END);
                addTop(displayed, topItem(saved.get(LEFT_SLOTS), cards), Gravity.END);
                param.setResult(displayed);
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(writeLayout, new LayoutHook() {
            @Override protected void before(MethodHookParam param) throws ReflectiveOperationException {
                if (componentMode.getInt(param.thisObject) != MODE) return;
                // The catalog changes with the selected camera. Keep the native six-value header,
                // leaving the positions occupied by the fixed director controls out of the editor.
                ArrayList<Integer> saved = ids(param.args[0]);
                ArrayList<Integer> normalized = new ArrayList<>();
                for (int i = 0; i < NATIVE_SLOTS; i++) {
                    int id = i < EDITABLE_SLOTS && i < saved.size() ? saved.get(i) : BLANK;
                    normalized.add(fixedMenuItem(id) || excluded(id) ? BLANK : id);
                }
                LinkedHashSet<Integer> remaining = new LinkedHashSet<>();
                for (int i = EDITABLE_SLOTS; i < saved.size(); i++) {
                    int id = saved.get(i);
                    if (id != BLANK && !fixedMenuItem(id) && !excluded(id)
                            && !normalized.contains(id)) remaining.add(id);
                }
                normalized.addAll(remaining);
                param.args[0] = normalized;
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(bindEditor, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || fragmentMode.getInt(param.thisObject) != MODE) return;
                Map<Integer, Object> cards = catalog(modeUi(param.thisObject));
                if (cards.isEmpty()) return;
                Object bar = editorTop.get(param.thisObject);
                Object menu = editorMenu.get(param.thisObject);
                directorMenuAdapters.put(menu, true);
                List<Object> barRows = rows(bar);
                List<Object> menuRows = rows(menu);
                Map<Integer, Object> nativeRows = new LinkedHashMap<>();
                for (Object row : paramItems(param)) nativeRows.put(value(row), row);
                List<Integer> saved = normalize(layout(), cards);
                barRows.clear();
                for (int i = 0; i < EDITABLE_SLOTS; i++) {
                    int id = saved.get(i);
                    barRows.add(editorRow(id, nativeRows, cards.get(id)));
                }
                menuRows.clear();
                for (Object item : menuCards(cards, saved)) {
                    int id = itemId.getInt(item);
                    menuRows.add(editorRow(id, nativeRows, item));
                }
                ViewGroup placeholders = (ViewGroup) editorBlanks.get(param.thisObject);
                for (int i = EDITABLE_SLOTS; i < placeholders.getChildCount(); i++) {
                    placeholders.getChildAt(i).setVisibility(View.GONE);
                }
                notifyAdapter.invoke(bar);
                notifyAdapter.invoke(menu);
                editorInitialRows.set(param.thisObject, collectEditor.invoke(param.thisObject));
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(collectEditor, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || fragmentMode.getInt(param.thisObject) != MODE) return;
                ArrayList<Object> result = new ArrayList<>(paramItems(param.getResult()));
                for (int i = EDITABLE_SLOTS; i < NATIVE_SLOTS; i++) {
                    result.add(i, editorRow(BLANK, java.util.Collections.emptyMap(), null));
                }
                for (int i = result.size() - 1; i >= NATIVE_SLOTS; i--) {
                    if (fixedMenuItem(value(result.get(i)))) result.remove(i);
                }
                param.setResult(result);
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(bindEditorItem, new LayoutHook() {
            @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                if (param.hasThrowable() || !directorMenuAdapters.containsKey(param.thisObject)) return;
                int id = Integer.parseInt((String) param.args[1]);
                if (!fixedMenuItem(id)) return;
                ((View) editorHolderView.get(param.args[0])).setOnLongClickListener(null);
                mark(param);
            }
        });
        HookDiagnostics.tryInstall("DirectorReferenceLines", () -> {
            DirectorReferenceLines lines = new DirectorReferenceLines(host);
            lines.install();
            if (HookDiagnostics.allHooksRegistered()) referenceLines = lines;
        });
        HookDiagnostics.tryInstall("DirectorRecordingFormats", () -> {
            DirectorRecordingFormats formats = new DirectorRecordingFormats(host);
            formats.install();
            if (HookDiagnostics.allHooksRegistered()) recordingFormats = formats;
        });
    }

    private Object modeUi(Object fragment) throws ReflectiveOperationException {
        return ((Optional<?>) getModeUi.invoke(getViewModel.invoke(fragment))).orElse(null);
    }

    private Map<Integer, Object> catalog(Object ui) throws ReflectiveOperationException {
        LinkedHashMap<Integer, Object> cards = new LinkedHashMap<>();
        if (!directorUi.isInstance(ui)) return cards;
        Object settings = null;
        for (Object item : (List<?>) nativeMenu.invoke(ui)) {
            int id = itemId.getInt(item);
            if (id == SETTINGS) settings = item;
            else if (!excluded(id)) {
                // Use the native drawable for LOG and the native dropdown click handler for ratio.
                cards.put(id, id == LOG || id == ASPECT_RATIO ? build.invoke(null, id) : item);
            }
        }
        // The native editor advertises movable items from both groups, such as
        // multi-camera connection, even when their default placement is the top bar.
        for (Object item : (List<?>) nativeTop.invoke(ui)) {
            int id = itemId.getInt(item);
            if (!excluded(id) && !cards.containsKey(id)) cards.put(id, build.invoke(null, id));
        }
        if (referenceLines == null) cards.remove(DirectorReferenceLines.ITEM_ID);
        if (recordingFormats == null) {
            cards.remove(DirectorRecordingFormats.DOLBY_VISION);
            cards.remove(DirectorRecordingFormats.MASTER_SHOT);
        }
        cards.put(WATERMARK, build.invoke(null, WATERMARK));
        if (referenceLines != null) cards.put(DirectorReferenceLines.ITEM_ID, referenceLines.item());
        if ((boolean) supportsPeaking.invoke(deviceFeatures.get(device.invoke(null)))) {
            cards.put(FOCUS_PEAK, build.invoke(null, FOCUS_PEAK));
        }
        cards.put(EXPOSURE_FEEDBACK, build.invoke(null, EXPOSURE_FEEDBACK));
        if (recordingFormats != null) recordingFormats.addCards(cards);
        if (settings != null) cards.put(SETTINGS, settings);
        return cards;
    }

    private ArrayList<Object> menuCards(Map<Integer, Object> cards, List<Integer> layout) {
        List<Integer> slots = normalize(layout, cards);
        LinkedHashSet<Integer> order = new LinkedHashSet<>();
        order.addAll(slots.subList(NATIVE_SLOTS, slots.size()));
        order.addAll(cards.keySet());
        order.removeAll(slots.subList(0, EDITABLE_SLOTS));
        ArrayList<Object> items = new ArrayList<>();
        for (int id : order) items.add(cards.get(id));
        return items;
    }

    private static ArrayList<Integer> normalize(List<Integer> saved, Map<Integer, Object> cards) {
        ArrayList<Integer> result = new ArrayList<>(NATIVE_SLOTS);
        LinkedHashSet<Integer> remaining = new LinkedHashSet<>();
        for (int i = 0; i < NATIVE_SLOTS; i++) {
            int id = i < saved.size() ? saved.get(i) : BLANK;
            if (i < EDITABLE_SLOTS && cards.containsKey(id) && !fixedMenuItem(id)
                    && !result.contains(id)) result.add(id);
            else {
                result.add(BLANK);
                if (cards.containsKey(id) && !fixedMenuItem(id)) remaining.add(id);
            }
        }
        for (int i = NATIVE_SLOTS; i < saved.size(); i++) {
            int id = saved.get(i);
            if (cards.containsKey(id) && !fixedMenuItem(id)) remaining.add(id);
        }
        for (int id : cards.keySet()) if (!fixedMenuItem(id)) remaining.add(id);
        remaining.removeAll(result);
        result.addAll(remaining);
        return result;
    }

    private static boolean fixedMenuItem(int id) {
        return id == SETTINGS || id == DirectorRecordingFormats.DOLBY_VISION
                || id == DirectorRecordingFormats.MASTER_SHOT;
    }

    private static boolean excluded(int id) {
        return id == QUALITY || id == RESOLUTION || id == FRAME_RATE
                || id == EXIT || id == MORE || id == PHOTO_CAPTURE_METHOD;
    }

    private void addTop(List<Object> top, Object item, int gravity) throws IllegalAccessException {
        if (item == null) return;
        itemGravity.setInt(item, gravity);
        itemPosition.setInt(item, top.size());
        top.add(item);
    }

    private Object topItem(int id, Map<Integer, Object> cards) throws ReflectiveOperationException {
        if (id == BLANK) return null;
        return id == DirectorReferenceLines.ITEM_ID ? cards.get(id) : build.invoke(null, id);
    }

    private List<Integer> layout() throws ReflectiveOperationException {
        Object editor = component.invoke(dataGlobal.invoke(null), editorType);
        return ids(readLayout.invoke(editor, MODE));
    }

    private Object editorRow(int id, Map<Integer, Object> rows, Object item)
            throws ReflectiveOperationException {
        Object row = rows.get(id);
        if (row != null && id != WATERMARK) return row;
        if (row == null) {
            row = allocateRow.invoke(allocator, rowType);
            rowId.set(row, String.valueOf(id));
            rowIcon.setInt(row, -1);
            rowTitle.setInt(row, -1);
        }
        if (item != null) {
            Object resource = readMenuResource.invoke(itemMenuProvider.get(item), MODE);
            rowIcon.setInt(row, id == DirectorRecordingFormats.DOLBY_VISION
                    ? DirectorRecordingFormats.DOLBY_EDITOR_ICON : menuIcon.getInt(resource));
            if (id == WATERMARK) {
                rowIcon.setInt(row, watermarkIcon);
                rowAnimation.setInt(row, menuAnimation.getInt(resource));
            }
            rowTitle.setInt(row, menuTitle.getInt(resource));
        }
        return row;
    }

    private int value(Object row) throws IllegalAccessException {
        return Integer.parseInt((String) rowId.get(row));
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Integer> ids(Object value) {
        return new ArrayList<>((List<Integer>) value);
    }

    @SuppressWarnings("unchecked")
    private List<Object> rows(Object adapter) throws IllegalAccessException {
        return (List<Object>) adapterItems.get(adapter);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> paramItems(Object value) {
        return (List<Object>) value;
    }

    private static List<Object> paramItems(StrictHook.MethodHookParam param) {
        return paramItems(param.args[0]);
    }

    private abstract static class LayoutHook extends StrictHook {
        protected void mark(MethodHookParam param) {
            param.setObjectExtra(APPLIED, Boolean.TRUE);
        }

        @Override protected boolean countsAsRun(MethodHookParam param) {
            return Boolean.TRUE.equals(param.getObjectExtra(APPLIED));
        }
    }
}
