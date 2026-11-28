package com.aurora.camera.diagnostics;

import android.os.Process;
import android.util.Log;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Member;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class HookDiagnostics {
    @FunctionalInterface
    public interface Installer<E extends Throwable> {
        void run() throws E;
    }
    private static final String TAG = "AuroraHookDiagnostics";
    private static final ThreadLocal<String> installing = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> missingRegistration = new ThreadLocal<>();
    private static final ThreadLocal<List<XC_MethodHook.Unhook>> registrations = new ThreadLocal<>();
    private static volatile Journal journal;

    private HookDiagnostics() {}

    public static void start(String dataDir, String component, String version) {
        journal = new Journal(new File(dataDir, "cache/aurora-hooks-" + component + ".tsv"), version);
        journal.add("started");
    }

    private static String processStart() {
        try {
            String stat = new String(Files.readAllBytes(new File("/proc/self/stat").toPath()),
                    StandardCharsets.US_ASCII);
            return stat.substring(stat.lastIndexOf(')') + 2).split(" ")[19];
        } catch (IOException | IndexOutOfBoundsException error) {
            Log.w(TAG, "Cannot read process start time", error);
            return "";
        }
    }

    private static String bootId() {
        try {
            return new String(Files.readAllBytes(new File("/proc/sys/kernel/random/boot_id").toPath()),
                    StandardCharsets.US_ASCII).trim();
        } catch (IOException error) {
            // App SELinux policies can hide boot_id. PID and process start time remain available.
            return "";
        }
    }

    public static void resolving(String group) {
        installing.set(group);
        event("group", group, "resolving");
    }

    public static void resolved() {
        event("group", installing.get(), "resolved");
        installing.remove();
    }

    public static <E extends Throwable> void install(String group, Installer<E> action) throws E {
        String outerGroup = installing.get();
        Boolean outerMissing = missingRegistration.get();
        List<XC_MethodHook.Unhook> outerHandles = registrations.get();
        List<XC_MethodHook.Unhook> handles = new ArrayList<>();
        installing.set(group);
        missingRegistration.remove();
        registrations.set(handles);
        event("group", group, "installing");
        try {
            action.run();
            // A single missing hook already has its own diagnostic. Preserve the
            // other independent hooks in older groups which allow partial support.
            if (allHooksRegistered()) event("group", group, "registered");
        } catch (Throwable error) {
            for (int i = handles.size() - 1; i >= 0; i--) {
                try { unhook(handles.get(i)); }
                catch (RuntimeException | LinkageError cleanup) { error.addSuppressed(cleanup); }
            }
            failure("install-error", group, error);
            throw error;
        } finally {
            if (outerGroup == null) installing.remove(); else installing.set(outerGroup);
            if (outerMissing == null) missingRegistration.remove(); else missingRegistration.set(outerMissing);
            if (outerHandles == null) registrations.remove(); else registrations.set(outerHandles);
        }
    }

    /** Install an independent feature. install() records its exact failure and removes its hooks. */
    public static boolean tryInstall(String group, Installer<ReflectiveOperationException> action) {
        try {
            install(group, () -> {
                action.run();
                if (!allHooksRegistered()) throw new IllegalStateException("Incomplete registration: " + group);
            });
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            return false;
        }
    }

    public static boolean allHooksRegistered() {
        return installing.get() != null && !Boolean.TRUE.equals(missingRegistration.get());
    }

    public static Operation operation(String target) {
        Journal current = journal;
        String group = installing.get();
        return new Operation(current == null ? null : new Point(current, group, target, group));
    }

    public static final class Operation {
        private final Point point;

        private Operation(Point point) { this.point = point; }

        public void registered() { if (point != null) point.write("registered", ""); }
        public void ran() { if (point != null) point.ran(); }
        public void failed(Throwable error) { if (point != null) point.error("callback-error", error); }
    }

    public static void resolutionFailed(Throwable failure) {
        String group = installing.get();
        installing.remove();
        failure("resolve-error", group == null ? "" : group, failure);
    }

    public static void skipped(String group) {
        event("group", group, "skipped");
    }

    public static void completed(String group) {
        event("group", group, "ready");
    }

    public static XC_MethodHook.Unhook hookMethod(Member method, ObservedHook callback) {
        Journal current = journal;
        if (current == null) {
            XC_MethodHook.Unhook handle = XposedBridge.hookMethod(method, callback);
            if (handle == null && installing.get() != null) missingRegistration.set(true);
            remember(handle);
            return handle;
        }
        String owner = owner(callback.getClass());
        String group = installing.get();
        Point point = new Point(current, group == null ? owner : group, method.toString(), owner);
        callback.observe(method, point);
        try {
            XC_MethodHook.Unhook handle = XposedBridge.hookMethod(method, callback);
            if (handle == null) {
                if (group != null) missingRegistration.set(true);
                point.error("register-error", new IllegalStateException("LSPosed returned no registration"));
            } else {
                remember(handle);
                point.write("registered", "");
            }
            return handle;
        } catch (RuntimeException | LinkageError failure) {
            point.error("register-error", failure);
            throw failure;
        }
    }

    private static void remember(XC_MethodHook.Unhook handle) {
        List<XC_MethodHook.Unhook> handles = registrations.get();
        if (handles != null && handle != null) handles.add(handle);
    }

    public static void unhook(XC_MethodHook.Unhook handle) {
        handle.unhook();
        List<XC_MethodHook.Unhook> handles = registrations.get();
        if (handles != null) handles.remove(handle);
        ObservedHook callback = (ObservedHook) handle.getCallback();
        Point point = callback.point(handle.getHookedMethod());
        if (point != null) point.write("removed", "");
    }

    private static String owner(Class<?> type) {
        String name = type.getName();
        int anonymous = name.indexOf('$');
        return name.substring(name.lastIndexOf('.') + 1, anonymous < 0 ? name.length() : anonymous);
    }

    private static void failure(String state, String group, Throwable failure) {
        event("error", group, state, failure.toString());
        Log.e(TAG, state + " " + group, failure);
    }

    private static void event(String... fields) {
        Journal current = journal;
        if (current != null) current.add(fields);
    }

    static final class Point {
        private final Journal journal;
        private final String id;
        private final String group;
        private final String target;
        private final String owner;
        private final AtomicBoolean ran = new AtomicBoolean();
        private final AtomicBoolean failed = new AtomicBoolean();

        Point(Journal journal, String group, String target, String owner) {
            this.journal = journal;
            id = Integer.toString(journal.nextId.incrementAndGet());
            this.group = group;
            this.target = target;
            this.owner = owner;
        }

        void ran() {
            if (!ran.get() && ran.compareAndSet(false, true)) write("ran", "");
        }

        void error(String stage, Throwable error) {
            if (!failed.get() && failed.compareAndSet(false, true)) {
                write(stage, error.toString());
                Log.e(TAG, group + " " + stage + " " + target, error);
            }
        }

        void write(String state, String detail) {
            journal.add("hook", id, group, state, target, owner, detail);
        }
    }

    private static final class Journal {
        private final File file;
        private final String version;
        private final ScheduledExecutorService writer = Executors.newSingleThreadScheduledExecutor(
                task -> new Thread(task, "AuroraHookDiagnostics"));
        private final List<String> pending = new ArrayList<>();
        private final AtomicInteger nextId = new AtomicInteger();
        private boolean initialized;

        Journal(File file, String version) {
            this.file = file;
            this.version = version;
        }

        synchronized void add(String... fields) {
            boolean schedule = pending.isEmpty();
            StringBuilder line = new StringBuilder();
            for (String field : fields) {
                if (line.length() != 0) line.append('\t');
                if (field != null) line.append(field.replace('\t', ' ').replace('\r', ' ').replace('\n', ' '));
            }
            pending.add(line.toString());
            if (schedule) writer.schedule(this::flush, 250, TimeUnit.MILLISECONDS);
        }

        private void flush() {
            List<String> lines;
            synchronized (this) {
                lines = new ArrayList<>(pending);
                pending.clear();
            }
            File destination = initialized ? file : new File(file.getPath() + ".pending");
            try {
                Files.createDirectories(file.getParentFile().toPath());
                try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(destination, initialized), StandardCharsets.UTF_8))) {
                    if (!initialized) {
                        output.write("session\t" + Process.myPid() + "\t" + processStart()
                                + "\t" + bootId() + "\t" + version);
                        output.newLine();
                    }
                    for (String line : lines) {
                        output.write(line);
                        output.newLine();
                    }
                }
                if (!initialized && !destination.renameTo(file)) {
                    throw new IOException("Cannot publish " + file);
                }
                initialized = true;
            } catch (IOException | SecurityException error) {
                Log.e(TAG, "Cannot save " + file, error);
            }
        }
    }
}
