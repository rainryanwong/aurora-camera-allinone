package com.aurora.camera.compat.sharedlut;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Provider-process owner. Immutable blobs are retained for editor operations already in flight. */
public final class LutShare246Store {
    public interface PngValidator { void validate(File file) throws IOException; }
    private static final long SESSION_MILLIS=10*60*1000L;
    private final File root, blobs, staging, catalogFile;
    private final PngValidator validator;
    private final Map<String,Session> sessions=new HashMap<String,Session>();
    private LutShare246.Catalog current;
    private static final class Session {
        final int uid;
        final long created=System.currentTimeMillis();
        final String base, token;
        final List<LutShare246.Row> rows;
        final boolean[] claimed, complete;
        Session(int uid,String token,LutShare246.Catalog catalog,String base) {
            this.uid=uid; this.token=token; this.base=base; this.rows=catalog.rows;
            claimed=new boolean[rows.size()]; complete=new boolean[rows.size()];
        }
    }
    public LutShare246Store(File directory,PngValidator validator) throws IOException {
        this.root=directory; this.validator=validator;
        blobs=new File(root,"blobs"); staging=new File(root,"staging"); catalogFile=new File(root,"catalog.bin");
        Files.createDirectories(blobs.toPath()); Files.createDirectories(staging.toPath());
        // No session survives provider-process death; staged files are never part of a catalog.
        File[] abandoned=staging.listFiles();
        if (abandoned==null) throw new IOException("Cannot inspect staging directory");
        for (File file:abandoned) if (file.isFile() && file.getName().matches("[a-f0-9]{32}_[0-9]+\\.part")) Files.delete(file.toPath());
        if (catalogFile.exists()) {
            current=LutShare246.decode(readBounded(catalogFile,LutShare246.MAX_CATALOG_BYTES));
            for (LutShare246.Row row:current.rows) if (!blobFile(row.revision).isFile()) throw new IOException("Catalog references missing LUT");
        } else current=new LutShare246.Catalog("empty",Collections.<LutShare246.Row>emptyList());
    }
    public synchronized LutShare246.Catalog read() { return current; }
    public synchronized String begin(int uid,byte[] encoded) throws IOException {
        reapExpired();
        if (sessions.size()>=2) throw new IOException("LUT publication already active");
        LutShare246.Catalog proposed=LutShare246.decode(encoded);
        if (!current.generation.equals(proposed.generation)) throw new IOException("Stale catalog generation");
        String token=UUID.randomUUID().toString().replace("-","");
        sessions.put(token,new Session(uid,token,proposed,current.generation));
        return token;
    }
    public synchronized void claim(int uid,String token,int index) throws IOException {
        Session session=session(uid,token); checkIndex(session,index);
        if (session.claimed[index]) throw new IOException("LUT upload already opened");
        session.claimed[index]=true;
    }
    /** Only the provider's pipe receiver supplies bytes; no foreign writable FD is retained for a sealed blob. */
    public void receive(int uid,String token,int index,InputStream input) throws IOException {
        Session session; File part;
        synchronized(this) {
            session=session(uid,token); checkIndex(session,index);
            if (!session.claimed[index] || session.complete[index]) throw new IOException("Invalid upload state");
            part=partFile(session,index);
        }
        try {
            try (FileOutputStream output=new FileOutputStream(part)) {
                copyBounded(input,output,LutShare246.MAX_PNG_BYTES); output.getFD().sync();
            }
            if (!session.rows.get(index).revision.equals(LutShare246.sha256(part))) throw new IOException("LUT revision mismatch");
            validator.validate(part);
            synchronized(this) {
                if (session(uid,token)!=session) throw new IOException("Upload session changed");
                File destination=blobFile(session.rows.get(index).revision);
                // Reusing a content hash reuses an immutable file, without replacing an open native resource.
                if (destination.exists()) {
                    if (!session.rows.get(index).revision.equals(LutShare246.sha256(destination))) throw new IOException("Existing LUT blob is corrupt");
                    Files.delete(part.toPath());
                } else Files.move(part.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
                session.complete[index]=true;
            }
        } finally { Files.deleteIfExists(part.toPath()); }
    }
    public synchronized LutShare246.Catalog commit(int uid,String token) throws IOException {
        Session session=session(uid,token);
        if (!session.base.equals(current.generation)) throw new IOException("Concurrent catalog publication won; resnapshot required");
        for (boolean complete:session.complete) if (!complete) throw new IOException("LUT upload incomplete");
        LutShare246.Catalog next=new LutShare246.Catalog(token,session.rows);
        Path temporary=Files.createTempFile(root.toPath(),"catalog-",".tmp");
        try {
            try (FileOutputStream output=new FileOutputStream(temporary.toFile())) { output.write(LutShare246.encode(next)); output.getFD().sync(); }
            Files.move(temporary,catalogFile.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            current=next; sessions.remove(token); return next;
        } finally { Files.deleteIfExists(temporary); }
    }
    public synchronized void abort(int uid,String token) throws IOException {
        Session session=session(uid,token); sessions.remove(token);
        for(int i=0;i<session.rows.size();i++) Files.deleteIfExists(partFile(session,i).toPath());
    }
    public synchronized File openBlob(String revision) throws IOException {
        File file=blobFile(revision);
        if (!file.isFile()) throw new FileNotFoundException("Unknown LUT revision");
        return file;
    }
    private Session session(int uid,String token) throws IOException {
        if (token==null || !token.matches("[a-f0-9]{32}")) throw new IOException("Invalid session token");
        Session session=sessions.get(token);
        if(session==null || session.uid!=uid) throw new SecurityException("LUT session owner mismatch");
        if(System.currentTimeMillis()-session.created>SESSION_MILLIS) throw new IOException("LUT session expired");
        return session;
    }
    private void reapExpired() throws IOException {
        Iterator<Session> it=sessions.values().iterator();
        while(it.hasNext()) { Session session=it.next(); if(System.currentTimeMillis()-session.created>SESSION_MILLIS) {
            it.remove(); for(int i=0;i<session.rows.size();i++) Files.deleteIfExists(partFile(session,i).toPath());
        }}
    }
    private static void checkIndex(Session session,int index) throws IOException {
        if(index<0 || index>=session.rows.size()) throw new IOException("Invalid LUT upload index");
    }
    private File partFile(Session session,int index) { return new File(staging,session.token+"_"+index+".part"); }
    private File blobFile(String revision) throws IOException {
        if(!LutShare246.isRevision(revision)) throw new IOException("Invalid LUT revision");
        return new File(blobs,revision+".png");
    }
    static byte[] readBounded(File file,int max) throws IOException {
        try(InputStream input=new FileInputStream(file); ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            copyBounded(input,output,max); return output.toByteArray();
        }
    }
    static void copyBounded(InputStream input,OutputStream output,int max) throws IOException {
        byte[] buffer=new byte[65536]; int count,total=0;
        while((count=input.read(buffer))!=-1) {
            total+=count; if(total>max) throw new IOException("LUT transfer exceeds byte limit");
            output.write(buffer,0,count);
        }
    }
}
