package com.aurora.camera.features.watermark;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.zip.ZipFile;
import org.json.JSONObject;

/** Template asset names and lossless OEM ASCII EXIF transport. */
public final class WatermarkLabels {
    private WatermarkLabels() {}
    private static Map<String,String> filters, lenses;

    public static synchronized void initialize(String moduleApk) {
        if (filters != null) return;
        try (ZipFile zip = new ZipFile(moduleApk);
             java.io.InputStream input = zip.getInputStream(zip.getEntry("assets/watermark-labels.json"))) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            for (int count; (count = input.read(buffer)) != -1;) bytes.write(buffer, 0, count);
            JSONObject data = new JSONObject(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
            Map<String,String> filterNames = names(data.getJSONObject("filters"));
            lenses = names(data.getJSONObject("lenses"));
            filters = filterNames;
        } catch (java.io.IOException | org.json.JSONException failure) {
            throw new IllegalStateException("Cannot load watermark label resources", failure);
        }
    }

    private static Map<String,String> names(JSONObject data) throws org.json.JSONException {
        Map<String,String> names = new HashMap<>();
        for (java.util.Iterator<String> keys = data.keys(); keys.hasNext();) {
            String key = keys.next(); names.put(key, data.getString(key));
        }
        return names;
    }

    private static String normalized(Map<String,String> names, String value) {
        if (value == null) return null;
        String canonical = names.get(value.trim().toLowerCase(Locale.ROOT));
        return canonical == null ? value : canonical;
    }

    public static String filter(String value) { return normalized(filters, value); }
    public static String lens(String value) { return normalized(lenses, value); }

    /** The neutral capture uses the template's existing Standard badge. */
    public static String displayFilter(String value) {
        String name = filter(value);
        return "Original".equals(name) ? "Standard" : name;
    }

    public static String auxiliary(String value) throws org.json.JSONException {
        JSONObject data = new JSONObject(value);
        if (data.has("filterName") && !data.isNull("filterName"))
            data.put("filterName", filter(data.getString("filterName")));
        if (data.has("cvLensName") && !data.isNull("cvLensName"))
            data.put("cvLensName", lens(data.getString("cvLensName")));
        return asciiJson(data.toString());
    }

    public static String asciiJson(String json) {
        StringBuilder result = new StringBuilder(json.length());
        final char[] hex = "0123456789abcdef".toCharArray();
        for (int i = 0; i < json.length(); i++) {
            char value = json.charAt(i);
            if (value <= 0x7f) result.append(value);
            else {
                result.append("\\u");
                result.append(hex[(value >>> 12) & 15]);
                result.append(hex[(value >>> 8) & 15]);
                result.append(hex[(value >>> 4) & 15]);
                result.append(hex[value & 15]);
            }
        }
        return result.toString();
    }
}
