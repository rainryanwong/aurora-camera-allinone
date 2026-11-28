package com.aurora.camera.compat.sharedlut;

import android.content.*;
import android.content.pm.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Blocking I/O: callers dispatch on their existing background worker, never the editor render/UI thread. */
public final class LutShare246Client {
    private static final String MODULE_PACKAGE="com.aurora.camera.compat";
    private LutShare246Client() {}
    private static ContentResolver resolver(Context context) throws IOException {
        if(context==null) throw new IOException("LUT context is missing");
        ProviderInfo provider=context.getPackageManager().resolveContentProvider(LutShare246.AUTHORITY,0);
        if(provider==null) throw new IOException("LUT provider is not installed or not visible to "+context.getPackageName());
        if(!MODULE_PACKAGE.equals(provider.packageName) || !LutShare246Provider.class.getName().equals(provider.name)
                || !provider.exported || provider.grantUriPermissions) throw new SecurityException("Unexpected LUT provider identity");
        if(!context.getPackageManager().hasSigningCertificate(MODULE_PACKAGE,
                LutShare246Access.certificateBytes(LutShare246Access.MODULE_CERT_SHA256),PackageManager.CERT_INPUT_SHA256))
            throw new SecurityException("Unexpected LUT provider certificate");
        return context.getContentResolver();
    }
    public static LutShare246.Catalog read(Context context) throws IOException {
        try { return read(resolver(context)); }
        catch(RuntimeException failure) { throw new IOException("Cannot read shared LUT catalog",failure); }
    }
    private static LutShare246.Catalog read(ContentResolver resolver) throws IOException {
        try(Cursor cursor=resolver.query(LutShare246.CATALOG_URI,LutShare246.COLUMNS,null,null,null)) {
            if(cursor==null || !cursor.moveToFirst()) throw new IOException("LUT provider returned no catalog");
            LutShare246.Catalog catalog=LutShare246.decode(cursor.getBlob(cursor.getColumnIndexOrThrow("catalog")));
            if(!catalog.generation.equals(cursor.getString(cursor.getColumnIndexOrThrow("generation"))) || cursor.moveToNext())
                throw new IOException("Inconsistent LUT catalog response");
            return catalog;
        }
    }
    public static void publish(Context context,List<LutShare246.ExportItem> items) throws IOException {
        String token=null; ContentResolver resolver=null;
        try {
            if(items==null) throw new IOException("LUT export list is missing");
            List<LutShare246.Row> rows=new ArrayList<LutShare246.Row>();
            for(LutShare246.ExportItem item:items) {
                if(item==null || !item.file.isFile() || item.file.length()>LutShare246.MAX_PNG_BYTES) throw new IOException("Invalid LUT export file");
                rows.add(item.row);
            }
            LutShare246.validateRows(rows); resolver=resolver(context);
            Bundle begin=new Bundle(); begin.putByteArray("catalog",LutShare246.encode(new LutShare246.Catalog(read(resolver).generation,rows)));
            Bundle opened=resolver.call(LutShare246.CATALOG_URI,"begin",null,begin);
            token=opened==null?null:opened.getString("token");
            if(token==null || !token.matches("[a-f0-9]{32}")) throw new IOException("Invalid LUT publication session");
            for(int i=0;i<items.size();i++) {
                Uri stage=Uri.parse("content://"+LutShare246.AUTHORITY+"/stage/"+token+"/"+i);
                try(InputStream input=new FileInputStream(items.get(i).file); OutputStream output=resolver.openOutputStream(stage,"w")) {
                    if(output==null) throw new IOException("LUT provider returned no upload stream");
                    LutShare246Store.copyBounded(input,output,LutShare246.MAX_PNG_BYTES);
                }
                Bundle seal=new Bundle(); seal.putInt("index",i);
                if(resolver.call(LutShare246.CATALOG_URI,"seal",token,seal)==null) throw new IOException("Missing LUT upload confirmation");
            }
            Bundle committed=resolver.call(LutShare246.CATALOG_URI,"commit",token,null);
            if(committed==null || !token.equals(committed.getString("generation"))) throw new IOException("Missing LUT publication confirmation");
            token=null;
        } catch(IOException | RuntimeException failure) {
            if(token!=null && resolver!=null) {
                try { resolver.call(LutShare246.CATALOG_URI,"abort",token,null); }
                catch(RuntimeException abortFailure) { failure.addSuppressed(abortFailure); }
            }
            if(failure instanceof IOException) throw (IOException)failure;
            throw new IOException("Cannot publish shared LUT catalog",failure);
        }
    }
    /** Owned by the editor's single I/O worker. Reuse verification for unchanged private files. */
    public static final class Materializer {
        private final Map<String,StructStat> verified=new HashMap<>();

        public File cached(Context context,LutShare246.Row row) throws IOException {
            File destination=new File(directory(context),row.revision+".png");
            StructStat current=stat(destination);
            if(current==null) {
                verified.remove(row.revision);
                return null;
            }
            if(sameFile(verified.get(row.revision),current)) return destination;
            if(!row.revision.equals(LutShare246.sha256(destination))
                    || !sameFile(current,stat(destination))) {
                verified.remove(row.revision);
                throw new IOException("Cached editor LUT revision is corrupt or changed: "+row.id);
            }
            verified.put(row.revision,current);
            return destination;
        }

        public File materialize(Context context,LutShare246.Row row) throws IOException {
            File existing=cached(context,row);
            if(existing!=null) return existing;
            // Keep prior revisions on disk for native render/save operations that still use them.
            File directory=directory(context);
            Files.createDirectories(directory.toPath());
            File destination=new File(directory,row.revision+".png");
            Path temporary=Files.createTempFile(directory.toPath(),"lut-",".tmp");
            try {
                try(InputStream input=resolver(context).openInputStream(LutShare246.blobUri(row.revision)); FileOutputStream output=new FileOutputStream(temporary.toFile())) {
                    if(input==null) throw new IOException("LUT provider returned no download stream");
                    LutShare246Store.copyBounded(input,output,LutShare246.MAX_PNG_BYTES); output.getFD().sync();
                } catch(RuntimeException failure) { throw new IOException("Cannot materialize shared LUT",failure); }
                if(!row.revision.equals(LutShare246.sha256(temporary.toFile()))) throw new IOException("Downloaded LUT revision mismatch");
                LutShare246Provider.validatePng(temporary.toFile());
                Files.move(temporary,destination.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                StructStat published=stat(destination);
                if(published==null) throw new IOException("Published editor LUT is missing: "+row.id);
                verified.put(row.revision,published);
                return destination;
            } finally { Files.deleteIfExists(temporary); }
        }

        public void retain(List<LutShare246.Row> rows) {
            Set<String> revisions=new HashSet<>();
            for(LutShare246.Row row:rows) revisions.add(row.revision);
            verified.keySet().retainAll(revisions);
        }

        private static File directory(Context context) { return new File(context.getFilesDir(),"lutshare246"); }

        private static StructStat stat(File file) throws IOException {
            try {
                StructStat attributes=Os.stat(file.getPath());
                if(!OsConstants.S_ISREG(attributes.st_mode)) throw new IOException("Editor LUT is not a regular file: "+file);
                return attributes;
            } catch(ErrnoException failure) {
                if(failure.errno==OsConstants.ENOENT) return null;
                throw new IOException("Cannot read editor LUT identity: "+file,failure);
            }
        }

        private static boolean sameFile(StructStat first,StructStat current) {
            return first!=null && current!=null
                    && first.st_dev==current.st_dev && first.st_ino==current.st_ino && first.st_size==current.st_size
                    && first.st_mtim.equals(current.st_mtim) && first.st_ctim.equals(current.st_ctim);
        }
    }
}
