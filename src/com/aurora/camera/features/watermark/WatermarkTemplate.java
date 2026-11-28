package com.aurora.camera.features.watermark;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

final class WatermarkTemplate {
    private static final String CHILD_VIEWS = "childViews";
    private static final String GENERATED_SUFFIX = "prometheus_leica_suffix";

    private WatermarkTemplate() {}

    static boolean hasConvertibleModel(Object node) {
        if (node instanceof JSONArray) {
            JSONArray items = (JSONArray) node;
            for (int index = 0; index < items.length(); index++) {
                if (hasConvertibleModel(items.opt(index))) return true;
            }
        } else if (node instanceof JSONObject) {
            JSONObject object = (JSONObject) node;
            JSONArray views = object.optJSONArray(CHILD_VIEWS);
            if (views != null) {
                if (findLogoForModel(views) != null) return true;
                for (int index = 0; index < views.length(); index++) {
                    Object view = views.opt(index);
                    if (view instanceof JSONObject
                            && GENERATED_SUFFIX.equals(((JSONObject) view).optString("id"))) continue;
                    if (hasConvertibleModel(view)) return true;
                }
            }
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!CHILD_VIEWS.equals(key) && hasConvertibleModel(object.opt(key))) return true;
            }
        }
        return false;
    }

    static JSONObject findLogoForModel(JSONArray views) {
        boolean hasModel = false;
        JSONObject logo = null;
        for (int index = 0; index < views.length(); index++) {
            JSONObject view = views.optJSONObject(index);
            if (view == null) continue;
            String id = view.optString("id");
            if ("model".equals(id)) hasModel = true;
            if ("logo".equals(id)) logo = view;
        }
        return hasModel ? logo : null;
    }
}
