package com.aurora.camera.compat.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Accepted R2 in-memory display algorithm, with host fields behind one accessor. */
public final class FilterDisplay {
    public interface Rows {
        String identity(Object row);
        String path(Object row);
        String source(Object row);
        String style(Object row);
        String savedStyle(Object row);
        boolean hasBinding(Object row);
        String boundEffect(Object row);
        int version(Object row);
        int order(Object row);
        boolean officialFlag(Object row);
        boolean selected(Object row);
        Map<String, String> current(Object row);
        Map<String, String> originals(Object row);
        void originals(Object row, HashMap<String, String> values);
        void selected(Object row, boolean selected);
    }

    private static final String EFFECT = "pref_camera_shader_coloreffect_key";
    private final Rows access;
    private Map<String, Object> aliases = new HashMap<>();
    private Map<Object, Signature> signatures = new IdentityHashMap<>();
    public boolean changed;

    public FilterDisplay(Rows access) { this.access = access; }

    // U9.S.f is genuinely nullable when no po_ values were saved. R2's fix stays
    // in this one read-only boundary; do not initialize the host's absent map.
    private Map<String, String> originals(Object row) {
        Map<String, String> values = access.originals(row);
        return values == null ? Collections.<String, String>emptyMap() : values;
    }

    private final class Signature {
        final String style, bound;
        final int version;
        final Map<String, String> current, original;
        Signature(Object row) {
            style = access.style(row);
            version = access.version(row);
            bound = access.boundEffect(row);
            current = new HashMap<>(access.current(row));
            original = new HashMap<>(originals(row));
        }
        boolean matches(Object row) {
            return access.officialFlag(row) && "official".equals(access.source(row))
                    && Objects.equals(style, access.style(row))
                    && Objects.equals(style, access.savedStyle(row))
                    && version == access.version(row) && access.hasBinding(row)
                    && Objects.equals(bound, access.boundEffect(row))
                    && current.equals(access.current(row)) && original.equals(originals(row));
        }
    }

    private boolean official(Object row) {
        String style = access.style(row);
        String identity = access.identity(row);
        String path = access.path(row);
        String bound = access.boundEffect(row);
        return access.officialFlag(row) && "official".equals(access.source(row))
                && style != null && style.equals(access.savedStyle(row))
                && identity != null && path != null
                && identity.startsWith(style + "_official_") && path.endsWith("/" + identity)
                && access.hasBinding(row) && bound.equals(access.current(row).get(EFFECT));
    }

    private final class Group {
        final ArrayList<Object> rows = new ArrayList<>();
        final Map<String, String> original = new HashMap<>();
        final Map<String, String> reset;
        final Object first;
        Object representative;
        Group(Object row) {
            first = representative = row;
            reset = new HashMap<>(access.current(row));
            reset.putAll(originals(row));
            add(row);
        }
        boolean accepts(Object row) {
            if (access.version(first) != access.version(row)
                    || !access.style(first).equals(access.style(row))
                    || !access.current(first).equals(access.current(row))) return false;
            Map<String, String> rowReset = new HashMap<>(access.current(row));
            rowReset.putAll(originals(row));
            return reset.equals(rowReset);
        }
        void add(Object row) {
            rows.add(row);
            original.putAll(originals(row));
            if (access.identity(row).compareTo(access.identity(representative)) < 0) representative = row;
        }
    }

    public String normalize(List<Object> loaded, String selectedPath) {
        ArrayList<Object> before = new ArrayList<>(loaded);
        Set<Object> live = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        live.addAll(loaded);
        Map<String, Object> retained = new HashMap<>();
        for (Map.Entry<String, Object> entry : aliases.entrySet()) {
            Object row = entry.getValue();
            Signature signature = signatures.get(row);
            if (live.contains(row) && signature != null && signature.matches(row)) {
                retained.put(entry.getKey(), row);
            }
        }
        Map<String, ArrayList<Group>> buckets = new LinkedHashMap<>();
        ArrayList<Object> entries = new ArrayList<>();
        Map<String, Object> paths = new HashMap<>();
        Map<Object, Object> restoredCopies = new IdentityHashMap<>();
        for (Object row : loaded) {
            Object prior = paths.putIfAbsent(access.path(row), row);
            if (prior != null) {
                restoredCopies.put(row, prior);
                continue;
            }
            if (!official(row)) {
                entries.add(row);
                continue;
            }
            String effect = access.current(row).get(EFFECT);
            ArrayList<Group> bucket = buckets.get(effect);
            if (bucket == null) {
                bucket = new ArrayList<>();
                buckets.put(effect, bucket);
            }
            Group found = null;
            for (Group group : bucket) {
                if (group.accepts(row)) { found = group; break; }
            }
            if (found == null) {
                found = new Group(row);
                bucket.add(found);
                entries.add(found);
            } else {
                found.add(row);
            }
        }
        Map<Object, Object> representatives = new IdentityHashMap<>();
        ArrayList<Object> display = new ArrayList<>();
        for (Object entry : entries) {
            if (!(entry instanceof FilterDisplay.Group)) {
                representatives.put(entry, entry);
                display.add(entry);
                continue;
            }
            Group group = (Group) entry;
            Object representative = group.representative;
            boolean active = false;
            for (Object row : group.rows) {
                representatives.put(row, representative);
                active |= access.selected(row);
            }
            if (group.rows.size() > 1) {
                access.originals(representative, new HashMap<>(group.original));
                access.selected(representative, active);
            }
            display.add(representative);
        }
        for (Map.Entry<Object, Object> entry : restoredCopies.entrySet()) {
            representatives.put(entry.getKey(), representatives.get(entry.getValue()));
        }
        Map<String, Object> next = new HashMap<>();
        for (Map.Entry<String, Object> entry : retained.entrySet()) {
            Object representative = representatives.get(entry.getValue());
            if (representative != null) next.put(entry.getKey(), representative);
        }
        for (Object row : loaded) {
            Object representative = representatives.get(row);
            next.put(access.identity(row), representative);
            next.put(access.path(row), representative);
        }
        Object selected = next.get(selectedPath);
        if (selected != null) {
            for (Object row : display) access.selected(row, row == selected);
            selectedPath = access.path(selected);
        }
        Collections.sort(display, new Comparator<Object>() {
            @Override public int compare(Object first, Object second) {
                return Integer.compare(access.order(first), access.order(second));
            }
        });
        changed = !before.equals(display);
        if (changed) {
            loaded.clear();
            loaded.addAll(display);
        }
        aliases = next;
        signatures = new IdentityHashMap<>();
        for (Object row : display) if (official(row)) signatures.put(row, new Signature(row));
        return selectedPath;
    }

    public String identity(String identity) {
        Object row = aliases.get(identity);
        return row == null ? identity : access.identity(row);
    }
}
