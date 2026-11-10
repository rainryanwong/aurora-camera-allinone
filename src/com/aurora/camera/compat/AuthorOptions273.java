package com.aurora.camera.compat;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Local author switches in the camera's private files directory. */
public final class AuthorOptions273 {
    private AuthorOptions273() {}
    public static boolean enabled(Context context, String name) {
        File file = new File(context.getFilesDir(), name);
        if (!file.exists()) return false;
        try (FileInputStream input = new FileInputStream(file)) {
            return input.read() == '1';
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read author option " + name, failure);
        }
    }
}
