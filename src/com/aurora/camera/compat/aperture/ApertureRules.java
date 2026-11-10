package com.aurora.camera.compat.aperture;

/** Small value policies from the accepted DEX; no host reflection or fallback. */
public final class ApertureRules {
    private ApertureRules() {}

    public static int setupMode(int mode) {
        return mode == 168 || mode == 171 || mode == 173 || mode == 175 || mode == 225 || mode == 256 ? 163 : mode;
    }

    public static int validationMode(int mode) {
        return mode == 171 || mode == 173 || mode == 175 || mode == 225 || mode == 256 ? 163 : mode;
    }

    public static boolean ai(int mode, boolean aperture, boolean professionalAperture) {
        return mode == 168 && aperture && !professionalAperture;
    }

    public static boolean programmaticValue(String value) {
        return !(Float.parseFloat(value) < 0.0f);
    }
}
