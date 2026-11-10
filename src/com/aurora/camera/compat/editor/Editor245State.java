package com.aurora.camera.compat.editor;

import android.content.Context;
import android.util.AtomicFile;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;

/** Same-UID editor processes share one atomic file; no exported provider or camera prefs. */
public final class Editor245State {
    private Editor245State() {}
    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "aurora-editor245-model.txt"));
    }

    public static synchronized String read(Context context) throws IOException {
        try (RandomAccessFile owner = new RandomAccessFile(new File(context.getFilesDir(), "aurora-editor245-model.lock"), "rw");
                FileLock held = owner.getChannel().lock()) {
            return readLocked(context);
        }
    }

    private static String readLocked(Context context) throws IOException {
        AtomicFile state = file(context);
        try (FileInputStream in = state.openRead(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[1024];
            int count;
            while ((count = in.read(bytes)) != -1) out.write(bytes, 0, count);
            return Editor245Policy.name(new String(out.toByteArray(), StandardCharsets.UTF_8));
        } catch (FileNotFoundException missing) {
            if (!state.getBaseFile().exists()) return "";
            throw missing;
        }
    }

    public static synchronized void write(Context context, String selected) throws IOException {
        try (RandomAccessFile owner = new RandomAccessFile(new File(context.getFilesDir(), "aurora-editor245-model.lock"), "rw");
                FileLock held = owner.getChannel().lock()) {
            writeLocked(context, selected);
        }
    }

    private static void writeLocked(Context context, String selected) throws IOException {
        String value = Editor245Policy.name(selected);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        AtomicFile state = file(context);
        FileOutputStream out = state.startWrite();
        try {
            out.write(bytes);
            out.getFD().sync();
            state.finishWrite(out);
        } catch (IOException | RuntimeException failure) {
            state.failWrite(out);
            throw failure;
        }
        if (!readLocked(context).equals(value)) throw new IOException("Editor model commit did not persist the selected value");
    }
}
