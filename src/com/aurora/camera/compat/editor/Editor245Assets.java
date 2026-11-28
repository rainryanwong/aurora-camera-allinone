package com.aurora.camera.compat.editor;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
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
    private static final String ASSET_ROOT="assets/editor245/";
    private static final java.util.concurrent.locks.ReentrantLock INITIALIZING = new java.util.concurrent.locks.ReentrantLock();
    private final String moduleApk;
    private final Map<File,StructStat> verified = new HashMap<>();
    private Map<String,String> hashes;

    public Editor245Assets(String moduleApk) { this.moduleApk = moduleApk; }
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
    private static StructStat stat(File target) throws IOException {
        try { return Os.stat(target.getPath()); }
        catch (ErrnoException error) {
            if (error.errno == OsConstants.ENOENT) return null;
            throw new IOException("Cannot read editor watermark file: " + target, error);
        }
    }

    private static boolean sameFile(StructStat known, StructStat current) {
        return known != null && current != null
                && known.st_dev == current.st_dev && known.st_ino == current.st_ino && known.st_size == current.st_size
                && known.st_mtim.equals(current.st_mtim) && known.st_ctim.equals(current.st_ctim);
    }

    private Map<String,String> manifest() throws IOException {
        if (hashes != null) return hashes;
        try (ZipFile module = new ZipFile(moduleApk)) {
            java.util.zip.ZipEntry manifestEntry = module.getEntry(ASSET_ROOT + "local-watermarks.tsv");
            if (manifestEntry == null) throw new IOException("Editor245 watermark manifest is missing");
            byte[] manifest = bytes(module.getInputStream(manifestEntry));
            if (!sha(manifest).equals(Editor245TemplateData.MANIFEST_SHA256)) throw new IOException("Editor245 watermark manifest identity differs");
            Map<String,String> parsed = new LinkedHashMap<>();
            for (String line : new String(manifest, StandardCharsets.UTF_8).split("\n")) {
                String[] row = line.split("\t", -1);
                if (row.length != 3 || !row[0].equals("asset") || !row[2].matches("[0-9a-f]{64}")) throw new IOException("Invalid editor watermark manifest row");
                group(row[1]);
                if (parsed.put(row[1], row[2]) != null) throw new IOException("Duplicate editor watermark payload " + row[1]);
            }
            if (parsed.size() != 97) throw new IOException("Expected exactly 97 static editor watermark files");
            hashes = parsed;
            return hashes;
        }
    }

    /** Caller holds Guard across the original native clear/copy and this publication. */
    public Publication publishLocked(File files) throws IOException {
        Map<String,String> manifest = manifest();
        File root = new File(files, "watermarks");
        Publication result = new Publication();
        Set<String> rejected = new HashSet<>();
        Set<String> missing = new LinkedHashSet<>();
        for (Map.Entry<String,String> asset : manifest.entrySet()) {
            String relative = asset.getKey();
            File target = new File(root, relative);
            StructStat current = stat(target);
            if (current == null) {
                verified.remove(target);
                missing.add(relative);
                continue;
            }
            if (sameFile(verified.get(target), current)) continue;
            if (OsConstants.S_ISREG(current.st_mode) && sha(Files.readAllBytes(target.toPath())).equals(asset.getValue())
                    && sameFile(current, stat(target))) {
                verified.put(target, current);
            } else {
                verified.remove(target);
                rejected.add(group(relative));
                result.conflicts.add(relative);
            }
        }
        Map<String,byte[]> content = new LinkedHashMap<>();
        if (!missing.isEmpty()) {
            // Verify every missing payload for the accepted groups before writing any of them.
            try (ZipFile module = new ZipFile(moduleApk)) {
                for (String relative : missing) {
                    if (rejected.contains(group(relative))) continue;
                    java.util.zip.ZipEntry entry = module.getEntry(ASSET_ROOT + "watermarks/" + relative);
                    if (entry == null) throw new IOException("Missing editor watermark payload " + relative);
                    byte[] value = bytes(module.getInputStream(entry));
                    if (!sha(value).equals(manifest.get(relative))) throw new IOException("Editor watermark payload identity differs " + relative);
                    content.put(relative, value);
                }
            }
        }
        for (Map.Entry<String,byte[]> asset : content.entrySet()) {
            File target = new File(root, asset.getKey());
            Files.createDirectories(target.toPath().getParent());
            java.nio.file.Path temporary = Files.createTempFile(target.toPath().getParent(), ".editor245-", ".tmp");
            try {
                try (FileOutputStream out = new FileOutputStream(temporary.toFile())) {
                    out.write(asset.getValue());
                    out.getFD().sync();
                }
                Files.move(temporary, target.toPath());
                // The bytes were checked before writing this file and the move succeeded.
                StructStat published = stat(target);
                if (published == null) throw new IOException("Published editor watermark is missing: " + target);
                verified.put(target, published);
                result.created++;
            } finally { Files.deleteIfExists(temporary); }
        }
        for (Editor245TemplateData.Group g : Editor245TemplateData.GROUPS) if (!rejected.contains(g.id)) result.groups.add(g.id);
        return result;
    }
}
