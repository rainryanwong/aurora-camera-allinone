import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.TreeSet;

/** Exact user-0 settings for the three LSP APKs shipped by the 2.0.2 candidate. */
public final class CameraLspSetup {
    static final String[] MODULES = {"com.prometheus.camera.m3v16save", "com.aurora.camera.livemotion", "com.aurora.camera.compat"};
    static final String[][] SCOPES = {{"com.android.camera", "com.miui.mediaeditor"}, {"com.android.camera"}, {"com.android.camera", "com.miui.mediaeditor"}};
    static final String DATABASE = "/data/adb/lspd/config/modules_config.db";
    static final File BACKUP = new File("/data/adb/camera68_aurora_lsp/settings.before.json");

    public static void main(String[] args) {
        try {
            if (args.length == 0) throw new IllegalArgumentException("Expected check, apply or restore");
            boolean readOnly = "check".equals(args[0]);
            if (!(readOnly || "apply".equals(args[0]) || "restore".equals(args[0]))) {
                throw new IllegalArgumentException("Unknown operation");
            }
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(DATABASE, null,
                    readOnly ? SQLiteDatabase.OPEN_READONLY : SQLiteDatabase.OPEN_READWRITE)) {
                if (readOnly) {
                    checkSchema(db);
                    System.out.println("LSPosed settings schema verified; settings unchanged");
                    for (String module : MODULES) System.out.println(snapshot(db, module));
                } else if ("apply".equals(args[0])) {
                    if (args.length != 4) throw new IllegalArgumentException("Expected installed M3, LiveMotion and CameraCompat APK paths");
                    if (!new File(args[1]).isFile() || !new File(args[2]).isFile() || !new File(args[3]).isFile()) {
                        throw new IllegalArgumentException("Installed module APK is missing");
                    }
                    apply(db, BACKUP, new String[]{args[1], args[2], args[3]});
                } else {
                    restore(db, BACKUP);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    static void checkSchema(SQLiteDatabase db) {
        columns(db, "modules", new String[]{"module_pkg_name"}, "apk_path");
        columns(db, "modules_state", new String[]{"module_pkg_name", "user_id"}, "enabled", "scope_request_blocked");
        columns(db, "scope", new String[]{"module_pkg_name", "app_pkg_name", "user_id"});
    }

    static void columns(SQLiteDatabase db, String table, String[] key, String... fields) {
        TreeSet<String> actual = new TreeSet<>();
        TreeSet<String> primaryKey = new TreeSet<>();
        TreeSet<String> supplied = new TreeSet<>(Arrays.asList(key));
        supplied.addAll(Arrays.asList(fields));
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            while (c.moveToNext()) {
                String name = c.getString(1);
                actual.add(name);
                if (c.getInt(5) != 0) primaryKey.add(name);
                if (!supplied.contains(name) && c.getInt(3) != 0 && c.isNull(4)) {
                    throw new IllegalStateException("Required LSPosed insert field: " + table + "." + name);
                }
            }
        }
        // Explicit SQL columns tolerate reordered/added fields. Identity keys must
        // still keep INSERT OR IGNORE and user-0 updates on a single target row.
        if (!actual.containsAll(Arrays.asList(fields))
                || !primaryKey.equals(new TreeSet<>(Arrays.asList(key)))) {
            throw new IllegalStateException("Unsupported LSPosed table contract: " + table);
        }
    }

    static JSONObject snapshot(SQLiteDatabase db, String module) throws Exception {
        JSONObject result = new JSONObject().put("module", module).put("state_exists", false);
        try (Cursor c = db.rawQuery("SELECT enabled,scope_request_blocked FROM modules_state WHERE module_pkg_name=? AND user_id=0", new String[]{module})) {
            if (c.moveToFirst()) result.put("state_exists", true).put("enabled", c.getInt(0)).put("scope_request_blocked", c.getInt(1));
        }
        JSONArray scopes = new JSONArray();
        try (Cursor c = db.rawQuery("SELECT app_pkg_name FROM scope WHERE module_pkg_name=? AND user_id=0 ORDER BY app_pkg_name", new String[]{module})) {
            while (c.moveToNext()) scopes.put(c.getString(0));
        }
        return result.put("scopes", scopes);
    }

    static TreeSet<String> scopeSet(JSONArray values) throws Exception {
        TreeSet<String> result = new TreeSet<>();
        for (int i = 0; i < values.length(); i++) result.add(values.getString(i));
        return result;
    }

    static boolean appliedState(JSONObject state, String[] scopes, int blocked) throws Exception {
        return state.getBoolean("state_exists") && state.getInt("enabled") == 1
                && state.getInt("scope_request_blocked") == blocked
                && scopeSet(state.getJSONArray("scopes")).equals(new TreeSet<>(Arrays.asList(scopes)));
    }

    static void setScope(SQLiteDatabase db, String module, JSONArray scopes) throws Exception {
        db.delete("scope", "module_pkg_name=? AND user_id=0", new String[]{module});
        for (int i = 0; i < scopes.length(); i++) {
            db.execSQL("INSERT INTO scope(module_pkg_name,app_pkg_name,user_id) VALUES(?,?,0)", new Object[]{module, scopes.getString(i)});
        }
    }

    static void apply(SQLiteDatabase db, File backup, String[] paths) throws Exception {
        if (paths.length != MODULES.length) throw new IllegalArgumentException("Expected three installed LSP paths");
        checkSchema(db);
        db.beginTransaction();
        try {
            JSONObject before;
            boolean writeBackup = false;
            if (backup.isFile()) {
                before = new JSONObject(new String(new AtomicFile(backup).readFully(), StandardCharsets.UTF_8));
                if (before.getInt("format") != 1) throw new IllegalStateException("Unknown settings backup format");
                // A 1.8.0–2.0.1 backup legitimately predates CameraCompat. Preserve
                // both original snapshots and add only the newly introduced target.
                before.getJSONObject(MODULES[0]);
                before.getJSONObject(MODULES[1]);
                if (!before.has(MODULES[2])) {
                    before.put(MODULES[2], snapshot(db, MODULES[2]));
                    writeBackup = true;
                }
            } else {
                before = new JSONObject().put("format", 1);
                for (String module : MODULES) before.put(module, snapshot(db, module));
                writeBackup = true;
            }
            // Every target must have a valid snapshot before the first SQL write.
            for (String module : MODULES) before.getJSONObject(module);
            if (writeBackup) {
                File parent = backup.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) throw new IllegalStateException("Cannot create settings backup directory");
                AtomicFile atomic = new AtomicFile(backup);
                FileOutputStream out = atomic.startWrite();
                try {
                    out.write(before.toString(2).getBytes(StandardCharsets.UTF_8));
                    atomic.finishWrite(out);
                } catch (Throwable e) {
                    atomic.failWrite(out);
                    throw e;
                }
            }
            for (int i = 0; i < MODULES.length; i++) {
                String module = MODULES[i];
                before.getJSONObject(module);
                int blocked = snapshot(db, module).optInt("scope_request_blocked", 0);
                db.execSQL("INSERT OR IGNORE INTO modules(module_pkg_name,apk_path) VALUES(?,?)", new Object[]{module, paths[i]});
                db.execSQL("UPDATE modules SET apk_path=? WHERE module_pkg_name=?", new Object[]{paths[i], module});
                db.execSQL("INSERT OR IGNORE INTO modules_state(module_pkg_name,user_id,enabled,scope_request_blocked) VALUES(?,0,1,0)", new Object[]{module});
                db.execSQL("UPDATE modules_state SET enabled=1 WHERE module_pkg_name=? AND user_id=0", new Object[]{module});
                setScope(db, module, new JSONArray(Arrays.asList(SCOPES[i])));
                JSONObject actual = snapshot(db, module);
                if (!appliedState(actual, SCOPES[i], blocked)) {
                    throw new IllegalStateException("Settings readback mismatch: " + module);
                }
                System.out.println("configured " + actual);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        System.out.println("Three modules enabled for user 0; reboot loads the saved LSPosed configuration");
    }

    static void restore(SQLiteDatabase db, File backup) throws Exception {
        if (!backup.isFile()) { System.out.println("No saved LSP settings to restore"); return; }
        checkSchema(db);
        JSONObject before = new JSONObject(new String(new AtomicFile(backup).readFully(), StandardCharsets.UTF_8));
        if (before.getInt("format") != 1) throw new IllegalStateException("Unknown settings backup format");
        boolean skipped = false;
        db.beginTransaction();
        try {
            for (int i = 0; i < MODULES.length; i++) {
                String module = MODULES[i];
                // Restore can also run before a new candidate's apply succeeded.
                if (i == 2 && !before.has(module)) {
                    System.out.println("No prior CameraCompat settings in legacy backup");
                    continue;
                }
                JSONObject old = before.getJSONObject(module);
                int oldBlocked = old.optInt("scope_request_blocked", 0);
                if (!appliedState(snapshot(db, module), SCOPES[i], oldBlocked)) {
                    skipped = true;
                    System.out.println("Preserved later user changes: " + module);
                    continue;
                }
                setScope(db, module, old.getJSONArray("scopes"));
                if (old.getBoolean("state_exists")) {
                    db.execSQL("UPDATE modules_state SET enabled=? WHERE module_pkg_name=? AND user_id=0", new Object[]{old.getInt("enabled"), module});
                } else {
                    db.delete("modules_state", "module_pkg_name=? AND user_id=0", new String[]{module});
                }
                System.out.println("restored " + snapshot(db, module));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        if (!skipped) new AtomicFile(backup).delete();
    }
}
