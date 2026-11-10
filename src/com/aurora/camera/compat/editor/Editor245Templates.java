package com.aurora.camera.compat.editor;

import android.content.Context;
import android.util.Log;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.editor.cover247.Editor247Covers;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/** Construct the target's real CloudWatermarkConfigData; native fetch/parser/render stay in use. */
public final class Editor245Templates {
    private final Host host;
    private final boolean nativeCloud;
    private final Field context, configGroups, configVersion, categoryId, categoryB, categoryC, categoryName, categoryDevices, categoryEntries, entryId, entryCover;
    private final Constructor<?> configConstructor, categoryConstructor, entryConstructor;
    private final Field[] entryFields;
    private volatile Set<String> available = Collections.emptySet();
    private volatile boolean prepared, nativeInitializationFailed;
    public Editor245Templates(Host h) throws ReflectiveOperationException {
        host = h;
        nativeCloud = h.usesNativeCloudFilter();
        String prefix = "com.miui.mediaeditor.photo.watermark.model.cloudwatermark.";
        Class<?> config = h.type(prefix + "CloudWatermarkConfigData"), category = h.type(prefix + "CloudCategoryData"), entry = h.type(prefix + "CloudWatermarkData");
        context = h.field("y2.a", "c");
        configGroups = h.field(config, "a"); configVersion = h.field(config, "b");
        categoryId = h.field(category, "a"); categoryB = h.field(category, "b"); categoryC = h.field(category, "c");
        categoryName = h.field(category, "d"); categoryDevices = h.field(category, "e"); categoryEntries = h.field(category, "f"); entryId = h.field(entry, "a"); entryCover = h.field(entry, "i");
        configConstructor = config.getConstructor(List.class, String.class);
        categoryConstructor = category.getConstructor(String.class, String.class, String.class, String.class, List.class, List.class);
        List<Class<?>> entryParameters = new ArrayList<>(Arrays.asList(String.class, String.class, List.class, List.class, String.class, String.class,
                long.class, long.class, String.class, String.class, String.class, boolean.class, List.class, List.class,
                List.class, List.class, List.class, List.class, List.class, double.class, h.type(prefix + "CloudWatermarkGuideData")));
        if (nativeCloud) entryParameters.add(boolean.class);
        entryConstructor = entry.getConstructor(entryParameters.toArray(new Class<?>[0]));
        String[] names={"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","x","y"};
        entryFields=new Field[names.length];
        for(int i=0;i<names.length;i++)entryFields[i]=h.field(entry,names[i]);
        if (!Context.class.isAssignableFrom(context.getType()) || configGroups.getType() != List.class
                || configVersion.getType() != String.class || categoryId.getType() != String.class
                || categoryEntries.getType() != List.class || entryId.getType() != String.class || entryCover.getType() != String.class) {
            throw new NoSuchFieldException("Editor245 local watermark model ABI differs");
        }
    }
    /** Called after successful native c() copy, before l() initializes Jm.Q's working directory. */
    public Editor245Assets.Guard begin() throws Exception {
        Context app = (Context) context.get(null);
        if (app == null) throw new IllegalStateException("Editor app context is missing");
        Editor245Assets.Guard guard = Editor245Assets.lock(app.getFilesDir());
        return guard;
    }
    public void invalidate() { available = Collections.emptySet(); prepared = false; nativeInitializationFailed = true; }
    /** Catalog preload precedes the native maker on a cold editor process. Caller holds Guard. */
    public void prepare() throws Exception {
        if (!prepared && !nativeInitializationFailed) publish();
    }
    public Set<String> availableGroups() { return new HashSet<>(available); }
    /** Relax Leica model admission before the native catalog filters it. */
    public String unlockLeica(String source) throws Exception {
        return EditorWatermarkEnvelope292.unlock(source);
    }
    @SuppressWarnings("unchecked")
    public Object unlockLeica(Object source) throws Exception {
        List<Object> groups=new ArrayList<>((List<Object>)configGroups.get(source));
        boolean changed=false;
        for(int i=0;i<groups.size();i++) {
            Object group=groups.get(i);
            if(!"leica".equals(categoryId.get(group)))continue;
            List<Object> entries=new ArrayList<>();
            for(Object entry:(List<Object>)categoryEntries.get(group)) {
                Object[] arguments=new Object[entryFields.length];
                for(int j=0;j<arguments.length;j++)arguments[j]=entryFields[j].get(entry);
                arguments[12]=Collections.emptyList();
                arguments[13]=Collections.singletonList("*");
                arguments[14]=Collections.emptyList();
                entries.add(entryConstructor.newInstance(arguments));
            }
            groups.set(i,categoryConstructor.newInstance(categoryId.get(group),categoryB.get(group),categoryC.get(group),
                    categoryName.get(group),Collections.singletonList("*"),entries));
            changed=true;
        }
        return changed?configConstructor.newInstance(groups,configVersion.get(source)):source;
    }
    public void publish() throws Exception {
        Context app = (Context) context.get(null);
        Editor245Assets.Publication publication = Editor245Assets.publishLocked(app.getFilesDir(), host.moduleApkPath);
        available = publication.groups;
        prepared = true;
        nativeInitializationFailed = false;
        Log.i("AuroraEditor245", "static watermark assets verified; created=" + publication.created + "; groups=" + available);
        if (!publication.conflicts.isEmpty()) Log.e("AuroraEditor245", "existing watermark asset conflicts preserved; excluded groups; paths=" + publication.conflicts);
    }
    @SuppressWarnings("unchecked")
    public synchronized Object merge(Object original, Object unfiltered) throws Exception {
        if (original == null) return null;
        List<Object> groups = new ArrayList<>((List<Object>) configGroups.get(original));
        // Keep only the cloud cover URL. Template metadata comes from the verified local
        // resources; incoming cloud versions may carry different product/type restrictions.
        Map<String,Object> cloudEntries = new HashMap<>();
        if (unfiltered != null) for (Object group : (List<Object>) configGroups.get(unfiltered)) {
            for (Object entry : (List<Object>) categoryEntries.get(group)) {
                cloudEntries.put(categoryId.get(group) + "/" + entryId.get(entry), entry);
            }
        }
        Set<String> nativeKeys = new HashSet<>();
        for (Object group : groups) for (Object entry : (List<Object>) categoryEntries.get(group)) nativeKeys.add(categoryId.get(group) + "/" + entryId.get(entry));
        int added = 0, preservedCovers = 0;
        List<String> missingCovers = new ArrayList<>();
        for (Editor245TemplateData.Group supplement : Editor245TemplateData.GROUPS) {
            if (!available.contains(supplement.id)) continue;
            List<Object> newEntries = new ArrayList<>();
            for (Editor245TemplateData.Template entry : supplement.entries) {
                if (!nativeKeys.add(supplement.id + "/" + entry.id)) continue;
                Object cloud = cloudEntries.get(supplement.id + "/" + entry.id);
                String cover = cloud == null ? "" : (String) entryCover.get(cloud);
                if (cover.isEmpty()) missingCovers.add(supplement.id + "/" + entry.id);
                else preservedCovers++;
                if (cover.isEmpty()) cover = Editor247Covers.marker(supplement.id, entry.id);
                List<Object> empty = Collections.emptyList();
                String label = Locale.getDefault().getLanguage().equals("zh") ? entry.labelZh : entry.label;
                List<Object> arguments = new ArrayList<>(Arrays.asList(entry.id, label, Arrays.asList(entry.content), Arrays.asList(entry.required),
                        entry.type, entry.engine, 0L, Long.MAX_VALUE, cover, "", "", false, empty, Collections.singletonList("*"),
                        empty, Collections.singletonList("*"), empty, Arrays.asList(entry.market), Collections.singletonList("ALL"), 1.0d, null));
                if (nativeCloud) arguments.add(false);
                newEntries.add(entryConstructor.newInstance(arguments.toArray()));
            }
            if (newEntries.isEmpty()) continue;
            int target = -1;
            for (int i = 0; i < groups.size(); i++) if (supplement.id.equals(categoryId.get(groups.get(i)))) { target = i; break; }
            if (target == -1) groups.add(categoryConstructor.newInstance(supplement.id, "", "", supplement.name, Collections.singletonList("*"), newEntries));
            else {
                Object old = groups.get(target);
                List<Object> entries = new ArrayList<>((List<Object>) categoryEntries.get(old)); entries.addAll(newEntries);
                groups.set(target, categoryConstructor.newInstance(categoryId.get(old), categoryB.get(old), categoryC.get(old), categoryName.get(old), categoryDevices.get(old), entries));
            }
            added += newEntries.size();
        }
        if (added == 0) return original;
        Log.i("AuroraEditor245", "native cloud watermark catalog supplemented; added=" + added
                + "; preserved_cloud_covers=" + preservedCovers + "; missing_cloud_covers=" + missingCovers
                + "; render/save acceptance pending");
        return configConstructor.newInstance(groups, configVersion.get(original));
    }
}
