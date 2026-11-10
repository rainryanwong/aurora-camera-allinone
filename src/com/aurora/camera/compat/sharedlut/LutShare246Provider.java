package com.aurora.camera.compat.sharedlut;

import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exported only for the pinned official camera/editor certificates; no URI grants or arbitrary paths. */
public final class LutShare246Provider extends ContentProvider {
    private LutShare246Store store;
    private final ExecutorService receivers=Executors.newFixedThreadPool(2);
    private final ExecutorService bootstrap=Executors.newSingleThreadExecutor();
    private final AtomicBoolean bootstrapPending=new AtomicBoolean();
    private final Map<String,Upload> uploads=new ConcurrentHashMap<String,Upload>();
    private static final class Upload {
        final Future<?> future; final ParcelFileDescriptor reader;
        Upload(Future<?> future,ParcelFileDescriptor reader) { this.future=future; this.reader=reader; }
        void cancel() throws IOException { future.cancel(true); reader.close(); }
    }
    private static final byte[] CERT=LutShare246Access.certificateBytes(LutShare246Access.CERT_SHA256);
    private static final String CAMERA_BOOTSTRAP_AUTHORITY="com.android.camera.settings.CameraSettingsSearchProvider";
    private static final Uri CAMERA_BOOTSTRAP_URI=Uri.parse("content://"+CAMERA_BOOTSTRAP_AUTHORITY+"/aurora_lutshare246");
    @Override public boolean onCreate() {
        try {
            store=new LutShare246Store(new File(getContext().getFilesDir(),"lutshare246"),new LutShare246Store.PngValidator() {
                @Override public void validate(File file) throws IOException { validatePng(file); }
            });
            return true;
        } catch(IOException failure) { Log.e("AuroraLutShare246","Cannot open LUT catalog",failure); return false; }
    }
    static void validatePng(File file) throws IOException {
        byte[] header=new byte[8];
        try(DataInputStream input=new DataInputStream(new FileInputStream(file))) { input.readFully(header); }
        byte[] png={(byte)137,80,78,71,13,10,26,10};
        if(!Arrays.equals(header,png)) throw new IOException("LUT is not a PNG");
        BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        if(options.outWidth!=512 || options.outHeight!=512 || !"image/png".equals(options.outMimeType)) throw new IOException("Expected 512x512 PNG LUT");
        options.inJustDecodeBounds=false; options.inScaled=false;
        Bitmap bitmap=BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        if(bitmap==null) throw new IOException("Cannot decode LUT PNG");
        try { if(bitmap.getWidth()!=512 || bitmap.getHeight()!=512) throw new IOException("Invalid decoded LUT size"); }
        finally { bitmap.recycle(); }
    }
    private int authorize(boolean write) {
        int uid=Binder.getCallingUid(); PackageManager pm=getContext().getPackageManager();
        String[] packages=pm.getPackagesForUid(uid);
        int cameraUid=packageUid(pm,LutShare246Access.CAMERA), editorUid=packageUid(pm,LutShare246Access.EDITOR);
        LutShare246Access.enforce(write,uid,packages,cameraUid,editorUid,
            cameraUid==uid && pm.hasSigningCertificate(LutShare246Access.CAMERA,CERT,PackageManager.CERT_INPUT_SHA256),
            editorUid==uid && pm.hasSigningCertificate(LutShare246Access.EDITOR,CERT,PackageManager.CERT_INPUT_SHA256));
        if(store==null) throw new IllegalStateException("LUT catalog unavailable");
        return uid;
    }
    private static int packageUid(PackageManager pm,String name) {
        try { return pm.getApplicationInfo(name,0).uid; }
        catch(PackageManager.NameNotFoundException missing) { return -1; }
    }
    /** Official automatic visibility: a provider owner can see apps that access that provider. */
    private void requestCameraSync() {
        if(!bootstrapPending.compareAndSet(false,true)) return;
        bootstrap.execute(new Runnable() {
            @Override public void run() {
                long identity=Binder.clearCallingIdentity();
                try {
                    PackageManager pm=getContext().getPackageManager();
                    ProviderInfo provider=pm.resolveContentProvider(CAMERA_BOOTSTRAP_AUTHORITY,0);
                    if(provider==null || !LutShare246Access.CAMERA.equals(provider.packageName)
                            || !CAMERA_BOOTSTRAP_AUTHORITY.equals(provider.name) || !provider.exported
                            || provider.readPermission!=null
                            || !pm.hasSigningCertificate(LutShare246Access.CAMERA,CERT,PackageManager.CERT_INPUT_SHA256))
                        throw new SecurityException("Unexpected camera bootstrap provider");
                    // The exact URI is intercepted by the camera hook. It schedules work and immediately returns a small status row.
                    // This worker never waits for camera publication and never carries through the editor's Binder identity.
                    try(Cursor cursor=getContext().getContentResolver().query(CAMERA_BOOTSTRAP_URI,null,null,null,null)) {
                        if(cursor==null || !cursor.moveToFirst() || cursor.getColumnIndex("status")<0
                                || !"scheduled".equals(cursor.getString(cursor.getColumnIndexOrThrow("status"))) || cursor.moveToNext())
                            throw new IllegalStateException("Camera bootstrap hook did not acknowledge request");
                    }
                } catch(RuntimeException failure) {
                    Log.e("AuroraLutShare246","Camera LUT bootstrap failed; next editor read may request it again",failure);
                } finally { Binder.restoreCallingIdentity(identity); bootstrapPending.set(false); }
            }
        });
    }
    private static List<String> path(Uri uri) {
        return LutShare246.parsePath(uri==null?null:uri.toString());
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) {
        int uid=authorize(false);
        if(!path(uri).equals(Collections.singletonList("catalog")) || selection!=null || selectionArgs!=null || sortOrder!=null
                || (projection!=null && !Arrays.equals(projection,LutShare246.COLUMNS))) throw new IllegalArgumentException("Unsupported LUT query");
        if(uid==packageUid(getContext().getPackageManager(),LutShare246Access.EDITOR)) requestCameraSync();
        LutShare246.Catalog catalog=store.read();
        try {
            MatrixCursor result=new MatrixCursor(LutShare246.COLUMNS,1);
            result.addRow(new Object[]{catalog.generation,LutShare246.encode(catalog)});
            result.setNotificationUri(getContext().getContentResolver(),LutShare246.CATALOG_URI); return result;
        } catch(IOException failure) { throw new IllegalStateException("Cannot encode LUT catalog",failure); }
    }
    @Override public Cursor query(Uri uri,String[] projection,Bundle queryArgs,CancellationSignal signal) {
        authorize(false);
        if(queryArgs!=null && !queryArgs.isEmpty()) throw new IllegalArgumentException("LUT query arguments are unsupported");
        return query(uri,projection,null,null,null);
    }
    @Override public String getType(Uri uri) {
        authorize(false); List<String> parts=path(uri);
        if(parts.equals(Collections.singletonList("catalog"))) return "vnd.android.cursor.item/vnd.aurora.lutshare246";
        if(parts.size()==2 && "blob".equals(parts.get(0)) && LutShare246.isRevision(parts.get(1))) return "image/png";
        throw new IllegalArgumentException("Unsupported LUT URI");
    }
    @Override public String getTypeAnonymous(Uri uri) { return getType(uri); }
    @Override public Bundle call(String authority,String method,String arg,Bundle extras) {
        if(!LutShare246.AUTHORITY.equals(authority)) throw new IllegalArgumentException("Invalid LUT authority");
        return call(method,arg,extras);
    }
    @Override public Bundle call(String method,String token,Bundle extras) {
        int uid=authorize(true);
        try {
            Bundle result=new Bundle();
            if("begin".equals(method)) {
                if(token!=null || extras==null || extras.size()!=1 || !extras.containsKey("catalog")) throw new IllegalArgumentException("Invalid begin arguments");
                result.putString("token",store.begin(uid,extras.getByteArray("catalog")));
            } else if("seal".equals(method)) {
                if(extras==null || extras.size()!=1 || !extras.containsKey("index")) throw new IllegalArgumentException("Invalid seal arguments");
                int index=extras.getInt("index",-1);
                Upload upload=uploads.get(uploadKey(uid,token,index));
                if(upload==null) throw new IOException("No LUT upload to seal");
                try { upload.future.get(30,TimeUnit.SECONDS); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("LUT upload interrupted",interrupted); }
                catch(ExecutionException failed) { throw new IOException("LUT upload failed",failed.getCause()); }
                catch(TimeoutException timedOut) { throw new IOException("LUT upload did not finish",timedOut); }
                uploads.remove(uploadKey(uid,token,index),upload);
            } else if("commit".equals(method)) {
                noExtras(extras); result.putString("generation",store.commit(uid,token).generation);
                getContext().getContentResolver().notifyChange(LutShare246.CATALOG_URI,null);
            } else if("abort".equals(method)) {
                noExtras(extras); store.abort(uid,token);
                String prefix=uid+":"+token+":";
                for(String key:new ArrayList<String>(uploads.keySet())) if(key.startsWith(prefix)) {
                    Upload upload=uploads.remove(key); if(upload!=null) upload.cancel();
                }
            } else throw new IllegalArgumentException("Unsupported LUT method");
            return result;
        } catch(IOException failure) {
            Log.e("AuroraLutShare246","LUT publication failed: "+method,failure);
            throw new IllegalStateException("LUT publication failed: "+method,failure);
        }
    }
    private static void noExtras(Bundle extras) { if(extras!=null && !extras.isEmpty()) throw new IllegalArgumentException("Unexpected LUT arguments"); }
    private static String uploadKey(int uid,String token,int index) {
        if(token==null || !token.matches("[a-f0-9]{32}") || index<0 || index>=LutShare246.MAX_ROWS) throw new IllegalArgumentException("Invalid LUT upload key");
        return uid+":"+token+":"+index;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        final int uid=authorize(!"r".equals(mode)); final List<String> parts=path(uri);
        try {
            if("r".equals(mode) && parts.size()==2 && "blob".equals(parts.get(0)))
                return ParcelFileDescriptor.open(store.openBlob(parts.get(1)),ParcelFileDescriptor.MODE_READ_ONLY);
            if(!"w".equals(mode) || parts.size()!=3 || !"stage".equals(parts.get(0))) throw new IllegalArgumentException("Unsupported LUT file mode or URI");
            final String token=parts.get(1);
            // parsePath already verifies canonical decimal syntax and the shared MAX_ROWS bound.
            final int index=Integer.parseInt(parts.get(2)); final String key=uploadKey(uid,token,index);
            store.claim(uid,token,index);
            final ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createReliablePipe();
            FutureTask<Void> task=new FutureTask<Void>(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    try(InputStream input=new ParcelFileDescriptor.AutoCloseInputStream(pipe[0])) { store.receive(uid,token,index,input); }
                    catch(IOException | RuntimeException failure) {
                        try { pipe[0].closeWithError("LUT upload rejected"); } catch(IOException closeFailure) { failure.addSuppressed(closeFailure); }
                        throw failure;
                    }
                    return null;
                }
            });
            uploads.put(key,new Upload(task,pipe[0])); receivers.execute(task); return pipe[1];
        } catch(IOException failure) { FileNotFoundException out=new FileNotFoundException("LUT transfer unavailable"); out.initCause(failure); throw out; }
    }
    @Override public Uri insert(Uri uri,ContentValues values) { authorize(true); path(uri); throw new UnsupportedOperationException("Use LUT publication protocol"); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { authorize(true); path(uri); throw new UnsupportedOperationException("Use LUT publication protocol"); }
    @Override public int delete(Uri uri,String selection,String[] args) { authorize(true); path(uri); throw new UnsupportedOperationException("Use LUT publication protocol"); }
}
