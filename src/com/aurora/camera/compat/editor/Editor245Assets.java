package com.aurora.camera.compat.editor;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.zip.ZipFile;

/** Verified static assets only. Existing nonmatching editor files are never replaced. */
public final class Editor245Assets {
    private Editor245Assets() {}
    private static final java.util.concurrent.locks.ReentrantLock INITIALIZING = new java.util.concurrent.locks.ReentrantLock();
    public static final class Guard implements AutoCloseable {
        private final RandomAccessFile file;
        private final FileLock held;
        private Guard(RandomAccessFile file, FileLock held) { this.file = file; this.held = held; }
        @Override public void close() throws IOException {
            try { try { held.close(); } finally { file.close(); } }
            finally { INITIALIZING.unlock(); }
        }
    }
    public static Guard lock(File files) throws IOException {
        INITIALIZING.lock();
        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(new File(files, "aurora-editor245-assets.lock"), "rw");
            return new Guard(file, file.getChannel().lock());
        } catch (IOException | RuntimeException error) {
            try { if (file != null) file.close(); }
            catch (IOException cleanup) { error.addSuppressed(cleanup); }
            finally { INITIALIZING.unlock(); }
            throw error;
        }
    }
    public static final class Publication {
        public final Set<String> groups = new LinkedHashSet<>();
        public final List<String> conflicts = new ArrayList<>();
        public int created;
    }
    private static byte[] bytes(InputStream in) throws IOException {
        try (InputStream source = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = source.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toByteArray();
        }
    }
    public static String sha(byte[] value) {
        try {
            StringBuilder out = new StringBuilder(64);
            for (byte b : MessageDigest.getInstance("SHA-256").digest(value)) {
                out.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String group(String relative) throws IOException {
        if (relative.startsWith("/") || relative.contains("\\") || relative.contains("..")) {
            throw new IOException("Unsafe editor watermark asset path: " + relative);
        }
        String group = relative.substring(0, relative.indexOf('/'));
        if (!group.equals("color_chart") && !group.equals("signature")) throw new IOException("Unexpected editor watermark group");
        return group;
    }
    public static Publication publish(File files, String moduleApk) throws IOException {
        try (Guard guard = lock(files)) { return publishLocked(files, moduleApk); }
    }
    /** Caller holds Guard across the original native clear/copy and this publication. */
    public static Publication publishLocked(File files, String moduleApk) throws IOException {
        try (ZipFile module = new ZipFile(moduleApk)) {
            java.util.zip.ZipEntry manifestEntry = module.getEntry("assets/editor245/local-watermarks.tsv");
            if (manifestEntry == null) throw new IOException("Editor245 watermark manifest is missing");
            byte[] manifest = bytes(module.getInputStream(manifestEntry));
            if (!sha(manifest).equals(Editor245TemplateData.MANIFEST_SHA256)) throw new IOException("Editor245 watermark manifest identity differs");
            Map<String,String> hashes = new LinkedHashMap<>();
            for (String line : new String(manifest, StandardCharsets.UTF_8).split("\n")) {
                String[] row = line.split("\t", -1);
                if (row.length != 3 || !row[0].equals("asset") || !row[2].matches("[0-9a-f]{64}")) throw new IOException("Invalid editor watermark manifest row");
                group(row[1]);
                if (hashes.put(row[1], row[2]) != null) throw new IOException("Duplicate editor watermark payload " + row[1]);
            }
            if (hashes.size() != 97) throw new IOException("Expected exactly 97 static editor watermark files");
            File root = new File(files, "watermarks");
            Publication result = new Publication();
            Set<String> rejected = new HashSet<>();
            Set<String> missing = new LinkedHashSet<>();
            for (Map.Entry<String,String> asset : hashes.entrySet()) {
                File target = new File(root, asset.getKey());
                if (!target.exists()) missing.add(asset.getKey());
                else if (!target.isFile() || !sha(Files.readAllBytes(target.toPath())).equals(asset.getValue())) {
                    rejected.add(group(asset.getKey())); result.conflicts.add(asset.getKey());
                }
            }
            // Existing files were checked under the same guard. Read source payloads only
            // for missing files, and verify all of them before publishing any resource.
            Map<String,byte[]> content = new LinkedHashMap<>();
            for (String relative : missing) {
                if (rejected.contains(group(relative))) continue;
                java.util.zip.ZipEntry entry = module.getEntry("assets/editor245/watermarks/" + relative);
                if (entry == null) throw new IOException("Missing editor watermark payload " + relative);
                byte[] value = bytes(module.getInputStream(entry));
                if (!sha(value).equals(hashes.get(relative))) throw new IOException("Editor watermark payload identity differs " + relative);
                content.put(relative, value);
            }
            for (Map.Entry<String,byte[]> asset : content.entrySet()) {
                File target = new File(root, asset.getKey());
                Files.createDirectories(target.toPath().getParent());
                java.nio.file.Path temporary = Files.createTempFile(target.toPath().getParent(), ".editor245-", ".tmp");
                try {
                    try (FileOutputStream out = new FileOutputStream(temporary.toFile())) { out.write(asset.getValue()); out.getFD().sync(); }
                    Files.move(temporary, target.toPath());
                    result.created++;
                } finally { Files.deleteIfExists(temporary); }
                if (!sha(Files.readAllBytes(target.toPath())).equals(hashes.get(asset.getKey()))) throw new IOException("Editor watermark publication verification failed " + asset.getKey());
            }
            for (Editor245TemplateData.Group g : Editor245TemplateData.GROUPS) if (!rejected.contains(g.id)) result.groups.add(g.id);
            return result;
        }
    }
}
