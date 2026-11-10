package com.aurora.camera.compat;

import android.util.AtomicFile;
import android.util.Log;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Reuse a verified package identity while PackageManager's installed file is unchanged. */
final class HostApkIdentity {
    private static final String TAG = "AuroraCameraCompat";

    static String verify(String source, String dataDirectory, String expected)
            throws IOException, NoSuchAlgorithmException {
        File apk = new File(source);
        StructStat attributes;
        try { attributes = Os.stat(source); }
        catch (ErrnoException failure) { throw failure.rethrowAsIOException(); }
        String identity = expected + "\n" + apk.getCanonicalPath() + "\n"
                + attributes.st_dev + ":" + attributes.st_ino + "\n" + attributes.st_size + "\n"
                + attributes.st_mtim.tv_sec + ":" + attributes.st_mtim.tv_nsec + "\n"
                + attributes.st_ctim.tv_sec + ":" + attributes.st_ctim.tv_nsec + "\n";
        AtomicFile cache = dataDirectory == null ? null
                : new AtomicFile(new File(dataDirectory, "cache/aurora-host-apk.identity"));
        if (cache != null && cache.getBaseFile().isFile()) {
            try {
                if (identity.equals(new String(cache.readFully(), StandardCharsets.UTF_8))) return expected;
            } catch (IOException failure) {
                Log.w(TAG, "Cannot read host identity cache; checking installed APK", failure);
            }
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream input = new FileInputStream(apk)) {
            byte[] buffer = new byte[65536];
            for (int count; (count = input.read(buffer)) != -1;) digest.update(buffer, 0, count);
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest.digest()) {
            hex.append(Character.forDigit((value >>> 4) & 15, 16));
            hex.append(Character.forDigit(value & 15, 16));
        }
        String actual = hex.toString();
        if (cache != null && expected.equals(actual)) {
            FileOutputStream output = null;
            try {
                output = cache.startWrite();
                output.write(identity.getBytes(StandardCharsets.UTF_8));
                cache.finishWrite(output);
            } catch (IOException failure) {
                if (output != null) cache.failWrite(output);
                Log.w(TAG, "Cannot save verified host identity", failure);
            }
        }
        return actual;
    }

    private HostApkIdentity() {}
}
