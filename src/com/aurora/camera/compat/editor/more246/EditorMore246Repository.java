package com.aurora.camera.compat.editor.more246;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.aurora.camera.compat.sharedlut.LutShare246;
import com.aurora.camera.compat.sharedlut.LutShare246Client;
import com.aurora.camera.compat.sharedlut.LutShare257Bootstrap;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** One worker owns IPC/files. Only complete revision-addressed snapshots reach the editor. */
public final class EditorMore246Repository {
    private static final String TAG = "AuroraEditorMore246";
    private static final int MAGIC = 0x454d3236;
    public interface Listener { void changed(List<Materialized> rows); }
    public static final class Materialized {
        public final LutShare246.Row row;
        public final File file;
        Materialized(LutShare246.Row row, File file) { this.row=row; this.file=file; }
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t=new Thread(r,"AuroraEditorMore246-io"); t.setDaemon(true); return t;
    });
    private volatile List<Materialized> snapshot=Collections.emptyList();
    private Context context;
    private Listener listener;
    private ContentObserver observer;
    private int epoch;
    private boolean running, dirty;

    public List<Materialized> snapshot() { return snapshot; }

    /** Called on the editor main thread; registration precedes the first read. */
    public synchronized void start(Context value, Listener callback) {
        if (context != null) return;
        context=value.getApplicationContext(); listener=callback; final int owner=++epoch;
        worker.execute(() -> {
            try {
                List<Materialized> cached=readCache(value);
                if (cached!=null) deliver(owner,cached);
            } catch (IOException failure) { Log.e(TAG,"Cached catalog could not be recovered",failure); }
            LutShare257Bootstrap.connect(value,()->connected(owner));
        });
    }

    private synchronized void connected(int owner) {
        if(context==null||owner!=epoch)return;
        observer=new ContentObserver(main) {
            @Override public void onChange(boolean selfChange) { refresh(); }
        };
        context.getContentResolver().registerContentObserver(LutShare246.CATALOG_URI,false,observer);
        refresh();
    }

    /** Does not remove immutable PNG revisions still held by native operations. */
    public synchronized void stop() {
        if (context==null) return;
        if(observer!=null)context.getContentResolver().unregisterContentObserver(observer);
        context=null; listener=null; observer=null; ++epoch; dirty=false;
    }

    public synchronized void refresh() {
        if (context==null) return;
        if (running) { dirty=true; return; }
        running=true; final Context ownerContext=context; final int owner=epoch;
        worker.execute(() -> {
            try {
                LutShare246.Catalog catalog=LutShare246Client.read(ownerContext);
                List<Materialized> complete=new ArrayList<>();
                for (LutShare246.Row row:catalog.rows)
                    complete.add(new Materialized(row,LutShare246Client.materialize(ownerContext,row)));
                Collections.sort(complete,(a,b) -> Integer.compare(a.row.order,b.row.order));
                List<Materialized> ready=Collections.unmodifiableList(complete);
                writeCache(ownerContext,ready);
                deliver(owner,ready);
            } catch (IOException failure) {
                // A transport/copy error is not an empty successful publication.
                Log.e(TAG,"Shared LUT refresh failed; retaining last complete catalog",failure);
            } finally {
                synchronized (EditorMore246Repository.this) {
                    running=false;
                    if (dirty && context!=null) { dirty=false; refresh(); }
                }
            }
        });
    }

    private void deliver(int owner,List<Materialized> complete) {
        main.post(() -> {
            Listener callback;
            synchronized (EditorMore246Repository.this) {
                if (owner!=epoch || context==null) return;
                snapshot=complete; callback=listener;
            }
            callback.changed(complete);
        });
    }

    private static File cache(Context context) { return new File(context.getFilesDir(),"editor_more246.catalog"); }
    private static void writeCache(Context context,List<Materialized> rows) throws IOException {
        File destination=cache(context);
        Path temporary=Files.createTempFile(context.getFilesDir().toPath(),"editor-more246-",".tmp");
        try {
            try (FileOutputStream file=new FileOutputStream(temporary.toFile()); DataOutputStream output=new DataOutputStream(file)) {
                output.writeInt(MAGIC+1); output.writeInt(rows.size());
                for (Materialized item:rows) {
                    item.row.write(output);
                }
                output.flush(); file.getFD().sync();
            }
            Files.move(temporary,destination.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static List<Materialized> readCache(Context context) throws IOException {
        File file=cache(context); if (!file.exists()) return null;
        if (file.length()>LutShare246.MAX_CATALOG_BYTES) throw new IOException("Cached catalog exceeds filter contract");
        List<Materialized> result=new ArrayList<>(); Set<String> ids=new HashSet<>();
        try (DataInputStream input=new DataInputStream(new FileInputStream(file))) {
            int magic=input.readInt();if(magic!=MAGIC&&magic!=MAGIC+1)throw new IOException("Cached catalog schema differs");
            int count=input.readInt(); if (count<0 || count>LutShare246.MAX_ROWS) throw new IOException("Cached row count differs");
            for (int i=0;i<count;i++) {
                LutShare246.Row row;
                try { row=LutShare246.Row.read(input,magic==MAGIC+1); }
                catch (IllegalArgumentException invalid) { throw new IOException("Invalid cached row",invalid); }
                if (!ids.add(row.id)) throw new IOException("Duplicate cached LUT id");
                File png=new File(new File(context.getFilesDir(),"lutshare246"),row.revision+".png");
                if (!png.isFile() || !row.revision.equals(LutShare246.sha256(png)))
                    throw new IOException("Cached PNG missing or corrupt: "+row.id);
                result.add(new Materialized(row,png));
            }
            if (input.read()!=-1) throw new IOException("Trailing cached catalog bytes");
        }
        return Collections.unmodifiableList(result);
    }
}
