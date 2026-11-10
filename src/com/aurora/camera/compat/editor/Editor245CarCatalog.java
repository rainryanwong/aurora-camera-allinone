package com.aurora.camera.compat.editor;

import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.*;
import java.util.*;

/** Re-evaluate native device-family gates once and publish only missing native car_* items. */
public final class Editor245CarCatalog {
    private final Method catalog;
    private final Method[] gates = new Method[3];
    private final Field itemTemplate, templateName, templateId, categoryId;
    private final ThreadLocal<Boolean> discovering = new ThreadLocal<>();
    public Editor245CarCatalog(Host h) throws ReflectiveOperationException {
        catalog = h.method("vg.h", "d", h.type("S3.b"), h.type("kh.d"));
        String[] names = {"b", "e", "i"};
        for (int i = 0; i < names.length; i++) {
            gates[i] = h.method("jb.a", names[i]);
            if (gates[i].getReturnType() != boolean.class) throw new NoSuchMethodException("Editor245 native car device gate differs");
        }
        itemTemplate = h.field("com.miui.mediaeditor.photo.watermark.model.menu.WatermarkItem", "c");
        templateName = h.field("Fg.b", "a"); templateId = h.field("Fg.b", "d");
        categoryId = h.field("com.miui.mediaeditor.photo.watermark.model.menu.WatermarkCategory", "c");
        if (catalog.getReturnType() != LinkedHashMap.class || templateName.getType() != String.class || templateId.getType() != int.class) {
            throw new NoSuchMethodException("Editor245 native car catalog contract differs");
        }
    }
    public void install() {
        for (Method gate : gates) XposedBridge.hookMethod(gate, new StrictHook() {
            @Override protected void before(MethodHookParam p) { if (Boolean.TRUE.equals(discovering.get())) p.setResult(true); }
        });
        XposedBridge.hookMethod(catalog, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable() || Boolean.TRUE.equals(discovering.get())) return;
                discovering.set(true);
                Object discovered;
                try { discovered = catalog.invoke(p.thisObject, p.args); }
                finally { discovering.remove(); }
                p.setResult(merge((Map<?,?>) p.getResult(), (Map<?,?>) discovered));
            }
        });
    }
    private static boolean car(String name) { return name != null && name.matches("car_[0-9]+"); }
    @SuppressWarnings("unchecked")
    private Object merge(Map<?,?> nativeMap, Map<?,?> discovered) throws IllegalAccessException {
        LinkedHashMap<Object,List<Object>> result = new LinkedHashMap<>();
        Set<String> names = new HashSet<>(); Set<Integer> ids = new HashSet<>();
        for (Map.Entry<?,?> category : nativeMap.entrySet()) {
            List<Object> entries = new ArrayList<>((List<Object>) category.getValue());
            result.put(category.getKey(), entries);
            for (Object entry : entries) {
                Object template = itemTemplate.get(entry);
                names.add((String) templateName.get(template)); ids.add(templateId.getInt(template));
            }
        }
        int added = 0;
        for (Map.Entry<?,?> category : discovered.entrySet()) for (Object entry : (List<Object>) category.getValue()) {
            Object template = itemTemplate.get(entry);
            String name = (String) templateName.get(template); int id = templateId.getInt(template);
            if (!car(name) || names.contains(name) || ids.contains(id)) continue;
            Object target = null;
            for (Object existing : result.keySet()) if (Objects.equals(categoryId.get(existing), categoryId.get(category.getKey()))) { target = existing; break; }
            if (target == null) { target = category.getKey(); result.put(target, new ArrayList<Object>()); }
            result.get(target).add(entry); names.add(name); ids.add(id); added++;
        }
        if (added == 0) return nativeMap;
        Log.i("AuroraEditor245", "native car watermark entries restored=" + added + "; existing native IDs preserved");
        return result;
    }
}
