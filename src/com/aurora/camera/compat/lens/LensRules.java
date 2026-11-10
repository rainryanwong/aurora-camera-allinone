package com.aurora.camera.compat.lens;

import java.util.Arrays;

/** Small table policies from the accepted APK, independent of reflection. */
public final class LensRules {
    private LensRules() {}

    public static float[] nightTwo(int mode,float[] values,boolean front) {
        if (mode!=173 || values==null || front) return values;
        boolean one=false,belowTwo=false;
        for (float value:values) {
            if (value==2.0f) return values;
            if (value<2.0f) belowTwo=true;
            if (value==1.0f) one=true;
        }
        if (!one || !belowTwo) return values;
        float[] result=Arrays.copyOf(values,values.length+1);
        result[values.length]=2.0f;
        Arrays.sort(result);
        return result;
    }
}
