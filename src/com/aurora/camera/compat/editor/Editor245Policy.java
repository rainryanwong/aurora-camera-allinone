package com.aurora.camera.compat.editor;

/** Local MediaEditor behavior policy; symbol/version resolution belongs to the DEX contract. */
public final class Editor245Policy {
    public static final String PACKAGE = "com.miui.mediaeditor";
    public static final String VERSION = "2.4.0.4.3 / 2.4.0.5.2; offline checked DEX contract";
    /** Profile-generation reference; Editor245Entry does not use this as a version allowlist. */
    public static final String SHA256 = "6f60c968d89f6548e607621e47121027866c72cd1c3455bd7df855c862e93ed6";
    private Editor245Policy() {}

    public static boolean process(String packageName, String processName) {
        return PACKAGE.equals(packageName) && (PACKAGE.equals(processName)
                || (PACKAGE + ":photo_editor").equals(processName));
    }

    /** Only the single-line boundary is restricted; punctuation and Unicode stay literal. */
    public static String name(String input) {
        if (input == null) throw new IllegalArgumentException("机型文字不能为空值");
        String value = input;
        for (int i = 0; i < value.length();) {
            char unit = value.charAt(i);
            if (Character.isSurrogate(unit)) {
                if (!Character.isHighSurrogate(unit) || i + 1 == value.length()
                        || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    throw new IllegalArgumentException("机型文字包含不完整字符");
                }
            }
            int point = value.codePointAt(i);
            if (Character.isISOControl(point) || point == 0x2028 || point == 0x2029) {
                throw new IllegalArgumentException("机型文字请保持单行");
            }
            i += Character.charCount(point);
        }
        return value.trim();
    }

    public static boolean modelId(String id) {
        return "model".equalsIgnoreCase(id) || "model_versionName".equalsIgnoreCase(id);
    }

    /** Both strings also describe product/series rejection; the caller must recheck native EXIF requirements. */
    public static boolean leicaEligibility(String category, int reason) {
        return leicaEligibility(category, reason, null);
    }

    public static boolean leicaEligibility(String category, int reason, int[] nativeReasons) {
        if (nativeReasons != null) {
            if (!"leica".equalsIgnoreCase(category)) return false;
            for (int value : nativeReasons) if (value == reason) return true;
            return false;
        }
        return "leica".equalsIgnoreCase(category)
                && (reason == 0x7f140bec || reason == 0x7f1407e2);
    }
}
