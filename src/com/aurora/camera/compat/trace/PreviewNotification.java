package com.aurora.camera.compat.trace;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class PreviewNotification extends ContentProvider {
    private static final String CHANNEL = "preview_diagnostics";
    private static final String TAG = "AuroraPreviewMarker";
    private static final int ID = 2085;
    private static final String PREFIX = "com.aurora.camera.preview.";

    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String token, Bundle extras) {
        if (Binder.getCallingUid() != 0) throw new SecurityException("Root module only");
        long identity = Binder.clearCallingIdentity();
        try {
            Bundle result = new Bundle();
            result.putBoolean("posted", update(getContext(), method, token));
            result.putBoolean("fileCapture", true);
            return result;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private static boolean update(Context context, String method, String token) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if ("STOP".equals(method)) {
            manager.cancel(ID);
            return true;
        }
        if (!"START".equals(method) || token == null || !token.matches("[a-f0-9-]{36}"))
            throw new IllegalArgumentException("Invalid preview notification command");
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "预览逐帧诊断",
                NotificationManager.IMPORTANCE_LOW));
        if (!manager.areNotificationsEnabled()
                || manager.getNotificationChannel(CHANNEL).getImportance() == NotificationManager.IMPORTANCE_NONE) {
            return false;
        }
        Intent mark = new Intent(context, Mark.class)
                .setAction(PREFIX + "MARK")
                .setData(Uri.fromParts("aurora-preview", token, null));
        PendingIntent action = PendingIntent.getBroadcast(context, 0, mark,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_ONE_SHOT);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("预览逐帧诊断")
                .setContentText("遇到异常时点「标记异常」，随后继续取景几秒")
                .setContentIntent(action)
                .addAction(new Notification.Action.Builder(null, "标记异常", action).build())
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setTimeoutAfter(600_000L)
                .build();
        manager.notify(ID, notification);
        return true;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
        throw new UnsupportedOperationException();
    }
    @Override public String getType(Uri uri) { throw new UnsupportedOperationException(); }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException();
    }

    public static final class Mark extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            Uri session = intent.getData();
            if (!(PREFIX + "MARK").equals(intent.getAction()) || session == null
                    || !"aurora-preview".equals(session.getScheme())) return;
            String marker = session.getSchemeSpecificPart() + " " + SystemClock.elapsedRealtimeNanos();
            AtomicFile file = new AtomicFile(new File(context.getCacheDir(), "aurora-preview.marker"));
            FileOutputStream stream = null;
            try {
                stream = file.startWrite();
                stream.write((marker + "\n").getBytes(StandardCharsets.UTF_8));
                file.finishWrite(stream);
            } catch (IOException failure) {
                file.failWrite(stream);
                Log.e(TAG, "Could not save preview marker", failure);
                return;
            }
            Log.i(TAG, marker);
            context.getSystemService(NotificationManager.class).cancel(ID);
        }
    }
}
