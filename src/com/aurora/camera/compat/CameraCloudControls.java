package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/** Controls the stock 1770 camera's cloud rule requests and cached configuration. */
public final class CameraCloudControls implements HookPlan {
    static final String TITLE = "相机云控功能配置";
    static final String PAGE = "cloud_controls";
    static final String ENTRY = "aurora_camera_cloud_controls";
    private static final String TAG = "AuroraCameraCloud";
    private static final String OPTIONS = "aurora_features229";
    private static final String PENDING = "aurora_camera_cloud_reset_pending";
    private static final String RESET_ERROR = "aurora_camera_cloud_reset_error";
    private static final String STOCK_LIST = "aurora_cloud_reset_stock_list";
    private static final String SUPPORT_LIST = "supported_watermark_list";
    private static final String DYNAMIC_FILE = "dynamicInfo.json";
    private static final File ODM_DYNAMIC = new File("/odm/etc/camera/xiaomi/EcoInfoDynamic.json");

    enum Control {
        FEATURE("拍摄功能云控", "aurora_block_cloud_feature", "camera_feature"),
        DYNAMIC("动态相机策略云控", "aurora_block_cloud_dynamic", "camera_dynamic"),
        MIVI("MiVi 配置云控", "aurora_block_cloud_mivi", "camera_app_mivi_v4_0"),
        FILTER("滤镜云控", "aurora_block_cloud_filter", "camera_app_filter_leica", "camera_app_filter_none_leica"),
        WATERMARK("水印云控", "aurora_block_cloud_watermark", "watermark_config", "video_watermark_config", "watermark_config_for_dev");

        final String title;
        final String key;
        final String[] modules;

        Control(String title, String key, String... modules) {
            this.title = title;
            this.key = key;
            this.modules = modules;
        }
    }

    private final Method attach, fetch, cached, acceptResponse, saveRules;
    private final Method readMiViFile, applyMiVi, copyAssets, filterWatermarks;
    private final Field responseModule, saveModule;
    private final Class<?> videoWatermarks;
    private final Constructor<?> ignoredRequest, requestFailure, requestResult;
    private volatile Context context;
    private volatile SharedPreferences options;
    private volatile String restoredDynamic;

    private CameraCloudControls(Host host) throws ReflectiveOperationException {
        attach = host.method("com.android.camera.CameraAppImpl", "attachBaseContext", Context.class);
        fetch = host.method("Xe.f", "f", String.class, boolean.class);
        cached = host.method("Xe.f", "e", String.class);
        acceptResponse = host.method("ef.e", "invoke", Object.class);
        responseModule = host.field("ef.e", "a");
        saveRules = host.method("Xe.a", "run");
        saveModule = host.field("Xe.a", "c");
        ignoredRequest = constructor(host, "df.d", int.class, Throwable.class);
        requestFailure = constructor(host, "Ue.j$a", Throwable.class);
        requestResult = constructor(host, "Ue.j", Object.class);
        readMiViFile = host.method("s3.b", "b", String.class);
        applyMiVi = host.method("s3.a", "run");
        copyAssets = host.method("U8.a", "b", Context.class, String.class, File.class, boolean.class);
        filterWatermarks = host.method("Gg.O", "d", boolean.class);
        videoWatermarks = host.type("Gg.F");
    }

    private static Constructor<?> constructor(Host host, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        Constructor<?> constructor = host.type(name).getDeclaredConstructor(parameters);
        constructor.setAccessible(true);
        return constructor;
    }

    public static HookPlan resolve(Host host) throws ReflectiveOperationException {
        return new CameraCloudControls(host);
    }

    @Override public String id() { return "camera-cloud-controls"; }

    static boolean queueReset(SharedPreferences options) {
        return options.edit().putBoolean(PENDING, true).remove(RESET_ERROR).commit();
    }

    static String resetError(SharedPreferences options) {
        return options.getString(RESET_ERROR, null);
    }

    private boolean blocked(String module) {
        SharedPreferences preferences = options;
        if (preferences == null || module == null) return false;
        for (Control control : Control.values()) {
            for (String name : control.modules) {
                if (name.equals(module)) return preferences.getBoolean(control.key, false);
            }
        }
        return false;
    }

    @Override public void install() {
        XposedBridge.hookMethod(attach, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                context = (Context) param.args[0];
                options = context.getSharedPreferences(OPTIONS, Context.MODE_PRIVATE);
                if (options.getBoolean(PENDING, false)) resetAtStartup();
            }
        });
        XposedBridge.hookMethod(fetch, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws Throwable {
                String module = (String) param.args[0];
                if (!blocked(module)) return;
                synchronized ((module + "CloudDataLock").intern()) {
                    param.setResult(cached.invoke(param.thisObject, module));
                }
            }
        });
        XposedBridge.hookMethod(acceptResponse, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws Throwable {
                if (!blocked((String) responseModule.get(param.thisObject))) return;
                // The stock requester uses HttpError 1 when it skips a refresh.
                Object error = ignoredRequest.newInstance(1, null);
                param.setResult(requestResult.newInstance(requestFailure.newInstance(error)));
            }
        });
        XposedBridge.hookMethod(saveRules, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws Throwable {
                if (blocked((String) saveModule.get(param.thisObject))) param.setResult(null);
            }
        });
        XposedBridge.hookMethod(readMiViFile, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                String local = restoredDynamic;
                if (local != null && DYNAMIC_FILE.equals(param.args[0])) param.setResult(local);
            }
        });
        XposedBridge.hookMethod(applyMiVi, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                param.setObjectExtra(PENDING, restoredDynamic);
            }

            @Override protected void after(MethodHookParam param) {
                if (!param.hasThrowable() && param.getObjectExtra(PENDING) != null) restoredDynamic = null;
            }
        });
        XposedBridge.hookMethod(filterWatermarks, new StrictHook() {
            @Override protected void before(MethodHookParam param) {
                if (context == null || !Boolean.TRUE.equals(param.args[0])) return;
                String name = videoWatermarks.isInstance(param.thisObject) ? "video_watermark_setting" : "watermark_setting";
                SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                String stock = preferences.getString(STOCK_LIST, null);
                if (stock != null && stock.equals(preferences.getString(SUPPORT_LIST, null))) {
                    // Keep userData in working folders while the stock list hides cloud-only entries.
                    param.args[0] = false;
                }
            }
        });
    }

    private void resetAtStartup() {
        try {
            String local = new String(Files.readAllBytes(ODM_DYNAMIC.toPath()), StandardCharsets.UTF_8);
            new JSONObject(local).getString("versionDynamic");
            restoreWatermarks("watermarks", "watermark_setting");
            restoreWatermarks("video_watermarks", "video_watermark_setting");
            clearRules();
            clearFilterDirectoryCache();
            for (String name : Arrays.asList(DYNAMIC_FILE, "mivi_info.json", "debug_mivi_info.json")) {
                Files.deleteIfExists(new File(context.getCacheDir(), name).toPath());
            }
            commit(context.getSharedPreferences("camera_settings_global", Context.MODE_PRIVATE).edit()
                    .remove("pref_miviinfo_for_hal_version_eco")
                    .remove("pref_miviinfo_for_hal_version_dynamic")
                    .remove("pref_wm_curversion_support_list")
                    .remove("pref_wm_download_no_remind_current_style"));
            restoredDynamic = local;
            commit(options.edit().remove(PENDING).remove(RESET_ERROR));
            Log.i(TAG, "Cleared camera cloud rules and restored stock configuration");
        } catch (IOException | JSONException | ReflectiveOperationException | SQLiteException failure) {
            Log.e(TAG, "Camera cloud reset failed", failure);
            Throwable detail = failure.getCause() == null ? failure : failure.getCause();
            if (!options.edit().remove(PENDING).putString(RESET_ERROR, detail.toString()).commit()) {
                Log.e(TAG, "Could not record camera cloud reset failure");
            }
        }
    }

    private void clearRules() throws IOException {
        Set<String> modules = new LinkedHashSet<>();
        for (Control control : Control.values()) modules.addAll(Arrays.asList(control.modules));
        File file = context.getDatabasePath("CloudConfig.db");
        if (file.isFile()) {
            try (SQLiteDatabase database = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                database.beginTransaction();
                try {
                    for (String module : modules) {
                        database.delete("cloudConfigCache", "moduleKey = ?", new String[]{module});
                    }
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }
            }
        }
        SharedPreferences cloud = context.getSharedPreferences("com.miui.camerainfra.cloudconfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = cloud.edit();
        for (String module : modules) {
            editor.remove("report_rule_id_" + module).remove("report_time_" + module)
                    .remove("pref_last_max_version_" + context.getPackageName() + "_" + module);
            for (String key : cloud.getAll().keySet()) {
                if (key.startsWith("pref_debug_mode_" + module + "_")) editor.remove(key);
            }
        }
        commit(editor);
    }

    private void clearFilterDirectoryCache() throws IOException {
        SharedPreferences workspace = context.getSharedPreferences("camera_settings_workspace", Context.MODE_PRIVATE);
        if (workspace.contains("CLOUD_FILTER")) commit(workspace.edit().remove("CLOUD_FILTER"));
    }

    private void restoreWatermarks(String assetRoot, String store)
            throws IOException, ReflectiveOperationException, JSONException {
        AssetManager assets = context.getAssets();
        Set<String> ids = new LinkedHashSet<>();
        for (String group : assets.list(assetRoot)) {
            String groupPath = assetRoot + "/" + group;
            for (String entry : assets.list(groupPath)) {
                String path = groupPath + "/" + entry;
                if (!Arrays.asList(assets.list(path)).contains("meta.json")) continue;
                try (InputStream input = assets.open(path + "/meta.json")) {
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    for (int count; (count = input.read(buffer)) != -1; ) data.write(buffer, 0, count);
                    JSONObject metadata = new JSONObject(new String(data.toByteArray(), StandardCharsets.UTF_8));
                    ids.add(metadata.getJSONObject("metadata").getString("id"));
                }
            }
        }
        if (ids.isEmpty()) throw new IOException("No stock watermark metadata in " + assetRoot);
        File internal = new File(context.getFilesDir(), assetRoot);
        File externalFiles = context.getExternalFilesDir(null);
        File external = externalFiles == null ? null : new File(externalFiles, assetRoot);
        if (external != null && external.isDirectory()) {
            copyAssets.invoke(null, context, assetRoot, external, true);
        }
        if (internal.isDirectory() || external == null || !external.isDirectory()) {
            copyAssets.invoke(null, context, assetRoot, internal, true);
        }
        String list = String.join(",", ids);
        commit(context.getSharedPreferences(store, Context.MODE_PRIVATE).edit()
                .putString(SUPPORT_LIST, list).putString(STOCK_LIST, list)
                .remove("watermark_sync_times").remove("watermark_last_sync_date")
                .remove("watermark_last_sync_time"));
    }

    private static void commit(SharedPreferences.Editor editor) throws IOException {
        if (!editor.commit()) throw new IOException("Could not save camera cloud settings");
    }
}
