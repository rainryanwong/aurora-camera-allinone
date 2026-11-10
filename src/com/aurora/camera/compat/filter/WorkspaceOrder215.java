package com.aurora.camera.compat.filter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Stable workspace identities, never effect IDs (many presets share an effect). */
public final class WorkspaceOrder215 {
    private WorkspaceOrder215() {}
    public static <T> boolean moveTo(List<T> rows,T item,T target) {
        int from=rows.indexOf(item),to=rows.indexOf(target);
        if(from<0||to<0||from==to)return false;
        // Capture the target position BEFORE removal: downward and upward agree.
        rows.remove(from);rows.add(to,item);return true;
    }
    public static List<String> restore(List<String> current,List<String> saved) {
        LinkedHashSet<String> remaining=new LinkedHashSet<>(current);
        ArrayList<String> result=new ArrayList<>();
        for(String id:saved)if(remaining.remove(id))result.add(id);
        result.addAll(remaining);return result;
    }
}
