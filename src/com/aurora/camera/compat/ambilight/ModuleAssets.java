package com.aurora.camera.compat.ambilight;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Two exact module-owned assets, never a fallback for arbitrary host assets. */
public final class ModuleAssets {
    public static final String CONFIG_NAME = "qigsaw_5.0.0.0_2.0.json";
    public static final String SPLIT_MD5 = "189952ca0ebbc0027b77b6461027ddff";
    public static final long SPLIT_BYTES = 2912883L;
    private final String moduleApkPath;
    private byte[] config;
    private byte[] split;

    public ModuleAssets(String moduleApkPath) {
        this.moduleApkPath = moduleApkPath;
    }

    public synchronized InputStream config() {
        if (config == null) config = asset(CONFIG_NAME);
        return new ByteArrayInputStream(config);
    }

    public synchronized InputStream split() {
        if (split == null) split = asset("ambilight.zip");
        return new ByteArrayInputStream(split);
    }

    private byte[] asset(String name) {
        try (ZipFile module = new ZipFile(moduleApkPath)) {
            return read(module, "assets/ambilight/" + name);
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot read Ambilight asset " + name, failure);
        }
    }

    private static byte[] read(ZipFile module, String name) throws IOException {
        ZipEntry entry = module.getEntry(name);
        if (entry == null) throw new IOException("Missing module asset " + name);
        try (InputStream input = module.getInputStream(entry);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
