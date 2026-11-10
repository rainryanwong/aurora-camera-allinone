package com.aurora.camera.compat.sharedlut;

import android.net.Uri;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/** Versioned, bounded metadata only. PNG bytes travel over file descriptors. */
public final class LutShare246 {
    public static final String AUTHORITY = "com.aurora.camera.compat.lutshare246";
    public static final Uri CATALOG_URI = Uri.parse("content://" + AUTHORITY + "/catalog");
    public static final int MAX_ROWS = 512, MAX_CATALOG_BYTES = 262144, MAX_PNG_BYTES = 4194304;
    static final String[] COLUMNS = {"generation", "catalog"};
    private static final int MAGIC = 0x4c533246, VERSION = 2;
    private LutShare246() {}

    public static final class Row {
        public final String id, name, revision;
        public final int order;
        public final Recipe248 recipe;
        public Row(String id, String name, String revision, int order) {
            this(id,name,revision,order,null);
        }
        public Row(String id, String name, String revision, int order,Recipe248 recipe) {
            if (id == null || !id.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Invalid LUT id");
            if (name == null || name.trim().isEmpty() || name.length() > 128 || name.indexOf('\u0000') >= 0)
                throw new IllegalArgumentException("Invalid LUT name");
            if (!isRevision(revision)) throw new IllegalArgumentException("Invalid LUT revision");
            if (order < 0 || order >= MAX_ROWS) throw new IllegalArgumentException("Invalid LUT slot order");
            this.id=id; this.name=name; this.revision=revision; this.order=order;this.recipe=recipe;
        }
        public String effectIdentity(){return recipe==null?"":recipe.identity();}
        public void write(DataOutput output)throws IOException{output.writeUTF(id);output.writeUTF(name);output.writeUTF(revision);output.writeInt(order);output.writeBoolean(recipe!=null);if(recipe!=null)recipe.write(output);}
        public static Row read(DataInput input,boolean extended)throws IOException{
            String id=input.readUTF(),name=input.readUTF(),revision=input.readUTF();int order=input.readInt();
            return new Row(id,name,revision,order,extended&&input.readBoolean()?Recipe248.read(input):null);
        }
    }
    public static final class ExportItem {
        public final Row row;
        public final File file;
        public ExportItem(Row row, File file) {
            if (row == null || file == null) throw new IllegalArgumentException("Missing export item");
            this.row=row; this.file=file;
        }
    }
    public static final class Catalog {
        public final String generation;
        public final List<Row> rows;
        public Catalog(String generation, List<Row> rows) {
            if (generation == null || !generation.matches("empty|[a-f0-9]{32}")) throw new IllegalArgumentException("Invalid catalog generation");
            validateRows(rows);
            this.generation=generation;
            this.rows=Collections.unmodifiableList(new ArrayList<Row>(rows));
        }
    }
    public static boolean isRevision(String revision) { return revision != null && revision.matches("[a-f0-9]{64}"); }
    public static String sha256(File file) throws IOException {
        try (InputStream input=new FileInputStream(file)) { return sha256(input); }
    }
    static String sha256(InputStream input) throws IOException {
        final MessageDigest digest;
        try { digest=MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        byte[] buffer=new byte[65536]; int count;
        while ((count=input.read(buffer)) != -1) digest.update(buffer,0,count);
        StringBuilder result=new StringBuilder(64);
        for (byte value:digest.digest()) { result.append(Character.forDigit((value>>>4)&15,16)); result.append(Character.forDigit(value&15,16)); }
        return result.toString();
    }
    static void validateRows(List<Row> rows) {
        if (rows == null || rows.size()>MAX_ROWS) throw new IllegalArgumentException("Catalog row limit exceeded");
        Set<String> ids=new HashSet<String>();
        for (Row row:rows) if (row==null || !ids.add(row.id)) throw new IllegalArgumentException("Duplicate or missing LUT id");
    }
    static byte[] encode(Catalog catalog) throws IOException {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        DataOutputStream output=new DataOutputStream(buffer);
        output.writeInt(MAGIC); output.writeInt(VERSION); output.writeUTF(catalog.generation); output.writeInt(catalog.rows.size());
        for (Row row:catalog.rows) {
            row.write(output);
        }
        output.flush();
        if (buffer.size()>MAX_CATALOG_BYTES) throw new IOException("Catalog byte limit exceeded");
        return buffer.toByteArray();
    }
    static Catalog decode(byte[] data) throws IOException {
        if (data==null || data.length>MAX_CATALOG_BYTES) throw new IOException("Invalid catalog length");
        DataInputStream input=new DataInputStream(new ByteArrayInputStream(data));
        if (input.readInt()!=MAGIC) throw new IOException("Unsupported catalog magic");
        int version=input.readInt();if(version!=1&&version!=VERSION)throw new IOException("Unsupported catalog version");
        String generation=input.readUTF(); int size=input.readInt();
        if (size<0 || size>MAX_ROWS) throw new IOException("Invalid catalog count");
        List<Row> rows=new ArrayList<Row>();
        try {
            for (int i=0;i<size;i++) {
                rows.add(Row.read(input,version>=2));
            }
            if (input.read()!=-1) throw new IOException("Trailing catalog data");
            return new Catalog(generation,rows);
        } catch (IllegalArgumentException invalid) { throw new IOException("Invalid catalog metadata",invalid); }
    }
    static Uri blobUri(String revision) {
        if (!isRevision(revision)) throw new IllegalArgumentException("Invalid revision");
        return Uri.parse("content://"+AUTHORITY+"/blob/"+revision);
    }
    static List<String> parsePath(String uri) {
        String prefix="content://"+AUTHORITY+"/";
        if(uri==null || !uri.startsWith(prefix)) throw new IllegalArgumentException("Invalid LUT URI");
        String path=uri.substring(prefix.length());
        if(!path.matches("catalog|blob/[a-f0-9]{64}|stage/[a-f0-9]{32}/(?:0|[1-9][0-9]{0,2})"))
            throw new IllegalArgumentException("Unsupported or noncanonical LUT URI");
        if(path.startsWith("stage/")&&Integer.parseInt(path.substring(path.lastIndexOf('/')+1))>=MAX_ROWS)throw new IllegalArgumentException("LUT stage index exceeds row limit");
        return Collections.unmodifiableList(Arrays.asList(path.split("/")));
    }
}
