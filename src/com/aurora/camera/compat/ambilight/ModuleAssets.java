package com.aurora.camera.compat.ambilight;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Two exact module-owned assets, never a fallback for arbitrary host assets. */
public final class ModuleAssets {
    public static final String CONFIG_NAME = "qigsaw_5.0.0.0_2.0.json";
    public static final String SPLIT_MD5 = "189952ca0ebbc0027b77b6461027ddff";
    public static final long SPLIT_BYTES = 2912883L;
    private static final String CONFIG_SHA =
            "26e34d5dc8d0b1a3e812543f5d6c596945fc16e69dab551aed42949a62c3d60c";
    private static final String SPLIT_SHA =
            "86a4fa65da35abb22cfe98ce5bde721d4f8e853d20e0c47d680d56dce538b12d";
    private final byte[] config;
    private final byte[] split;

    private ModuleAssets(byte[] config, byte[] split) {
        this.config = config;
        this.split = split;
    }

    public static ModuleAssets load(String moduleApkPath) {
        if (moduleApkPath == null || moduleApkPath.length() == 0) {
            throw new IllegalStateException("Ambilight requires the real Xposed module APK path");
        }
        try (ZipFile module = new ZipFile(moduleApkPath)) {
            byte[] config = read(module, "assets/ambilight/" + CONFIG_NAME);
            byte[] split = read(module, "assets/ambilight/ambilight.zip");
            if (!CONFIG_SHA.equals(sha256(config)) || !SPLIT_SHA.equals(sha256(split))) {
                throw new IllegalStateException("Ambilight module asset identity mismatch");
            }
            return new ModuleAssets(config, split);
        } catch (IOException | NoSuchAlgorithmException failure) {
            throw new IllegalStateException("Cannot load exact Ambilight module assets", failure);
        }
    }

    public InputStream config() { return new ByteArrayInputStream(config); }
    public InputStream split() { return new ByteArrayInputStream(split); }

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

    private static String sha256(byte[] data) throws NoSuchAlgorithmException {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            int unsigned = value & 255;
            result.append(Character.forDigit(unsigned >>> 4, 16));
            result.append(Character.forDigit(unsigned & 15, 16));
        }
        return result.toString();
    }
}
