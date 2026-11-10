package com.android.camera.port;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONArray;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;

/** W1 adaptation of Phoenix DeviceNameOverride / DevicePresetStore semantics.
 * OEM device configuration and native libraries are untouched. Optional metadata sync is handled by the paired LSP.
 * Versioned reflection names below are mapped to the pinned 6.8 v16 DEX.
 */
public final class AuroraWatermarkName {
    private static final String TAG = "AuroraWatermarkName";
    private static final String PREFS = "aurora_watermark_names";
    private static final String KEY = "pref_aurora_watermark_device_name";
    private static final String ACTIVE = "selected";
    private static final String CUSTOM = "custom_presets";
    private static final Map<Object,Map<String,WeakReference<Object>>> ROWS = new WeakHashMap<>();
    private static volatile boolean unavailableLogged;
    public static ClassLoader hostLoader;
    private static Class<?> hostType(String name)throws ClassNotFoundException{return Class.forName(name,false,hostLoader);}
    public static final String PAGE_FLAG="aurora_watermark_page217";
    public static boolean own(Object fragment) {try{android.app.Activity a=(android.app.Activity)call(fragment,"requireActivity",new Class<?>[0]);return a.getIntent().getBooleanExtra(PAGE_FLAG,false);}catch(Exception e){throw new IllegalStateException("Watermark page owner",e);}}
    private static boolean metadata(Object fragment)throws Exception {android.app.Activity a=(android.app.Activity)call(fragment,"requireActivity",new Class<?>[0]);return a.getIntent().getBooleanExtra("aurora_watermark_metadata217",false);}
    private static final String PAGE = "com.android.camera.port.AuroraWatermarkNameFragment";
    private static final String[] FIXED = {"", "14 Ultra", "工程型号", "18 Fold", "14 Ultra Ti", "17 Ultra by Leica", "leitzphone powered by xiaomi"};

    private AuroraWatermarkName() {}

    private static Object clickListener(Object fragment,boolean page)throws Exception {
        Class<?> type=hostType("androidx.preference.Preference$d");
        return java.lang.reflect.Proxy.newProxyInstance(hostLoader,new Class<?>[]{type},(proxy,m,args)->{
            if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraWatermarkClick217";}
            return page?pageClick(fragment,args[0]):onPreferenceClick(fragment,args[0]);
        });
    }
    private static Object changeListener(Object fragment)throws Exception {
        Class<?> type=hostType("androidx.preference.Preference$c");
        return java.lang.reflect.Proxy.newProxyInstance(hostLoader,new Class<?>[]{type},(proxy,m,args)->{
            if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraWatermarkChange217";}
            return metadataChange(fragment,args[0],args[1]);
        });
    }
    public static void open(Object fragment)throws Exception {
        android.app.Activity owner=(android.app.Activity)call(fragment,"requireActivity",new Class<?>[0]);
        android.content.Intent intent=new android.content.Intent(owner,hostType("com.android.camera.fragment.settings.PreferenceExtraActivity"));
        intent.putExtra("target_tag","com.android.camera.fragment.settings.CameraPreferenceFragment");
        intent.putExtra("from_where",field(hostType("com.android.camera.fragment.settings.b"),"mFromWhere").getInt(null));
        intent.putExtra(PAGE_FLAG,true);
        field(fragment.getClass(),"mGoToActivity").setBoolean(fragment,true);
        owner.startActivity(intent);
    }
    public static void populatePage(Object fragment)throws Exception {if(metadata(fragment))populateMetadata(fragment);else populate(fragment);}
    public static void resumePage(Object fragment)throws Exception {if(metadata(fragment))metadataTitle(fragment);else title(fragment);}
    public static boolean back(android.app.Activity owner)throws Exception {
        if(!owner.getIntent().getBooleanExtra(PAGE_FLAG,false)||!owner.getIntent().getBooleanExtra("aurora_watermark_metadata217",false))return false;
        owner.getIntent().putExtra("aurora_watermark_metadata217",false);
        Object fragment=field(hostType("com.android.camera.fragment.settings.a"),"U").get(owner);
        populate(fragment);title(fragment);return true;
    }


    private static Field field(Class<?> type, String name) throws Exception {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static Context global() throws Exception {
        Class<?> c = hostType("com.xiaomi.camera.basic.Global");
        return (Context) c.getMethod("getContext").invoke(null);
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static boolean saveString(Context context,String key,String value) {
        SharedPreferences preferences=prefs(context);boolean existed=preferences.contains(key);String previous=preferences.getString(key,null);
        if(preferences.edit().putString(key,value).commit())return true;
        SharedPreferences.Editor restore=preferences.edit();if(existed)restore.putString(key,previous);else restore.remove(key);
        if(!restore.commit())throw new IllegalStateException("Watermark preference rollback failed");
        Log.e(TAG,"Preference commit failed; prior value restored");return false;
    }

    private static String selected(Context c) {
        return c == null ? "" : prefs(c).getString(ACTIVE, "").trim();
    }

    /** Empty selection is the OEM result, not a new spoofed device identity. */
    public static String apply(String original) {
        try {
            String value = selected(global());
            if ("工程型号".equals(value)) return android.os.Build.MODEL;
            return value.isEmpty() ? original : value;
        } catch (Exception e) {
            if (!unavailableLogged) {
                unavailableLogged = true;
                Log.w(TAG, "Global context unavailable; preserve OEM model", e);
            }
            return original;
        }
    }

    /** Use the same logo/model/locale inputs as WmSettingFragment.setMixTextData. */
    private static void sync(Object item) throws Exception {
        Class<?> holder = hostType("Pe.b$b");
        Object identity = field(holder, "a").get(null);
        String logo = (String) call(identity, "z", new Class<?>[0]);
        String model = (String) call(identity, "A", new Class<?>[0]);
        Class<?> locale = hostType("Pe.c");
        boolean isChina = (Boolean) locale.getMethod("b").invoke(null);
        call(item, "J0", new Class<?>[]{String.class, String.class, boolean.class}, logo, model, isChina);
    }

    /** Runs before preview render and before native n0 serialization/publication. */
    public static void prepare(Object item) {
        try {
            if (!selected(global()).isEmpty()) sync(item);
        } catch (Exception e) {
            Log.e(TAG, "Model refresh failed; OEM render/publication continues", e);
        }
    }

    public static void install(Object fragment) {
        try {
            Context c = (Context) call(fragment, "requireContext", new Class<?>[0]);
            Object screen = call(fragment, "findPreference", new Class<?>[]{CharSequence.class}, "capture_watermark_setting");
            if (screen == null) return;
            Object existing = call(fragment, "findPreference", new Class<?>[]{CharSequence.class}, KEY);
            Class<?> preference = hostType("androidx.preference.Preference");
            if (existing == null) {
                existing = preference.getConstructor(Context.class, AttributeSet.class).newInstance(c, null);
                call(existing, "a0", new Class<?>[]{String.class}, KEY);
                call(existing, "e0", new Class<?>[]{String.class}, "水印机型");
                // Same host-native click listener as the other watermark rows.
                field(preference, "f").set(existing,clickListener(fragment,false));
                call(screen, "j0", new Class<?>[]{preference}, existing);
                Log.i(TAG, "W1 preference attached (v16)");
            }
            String value = selected(c);
            call(existing, "c0", new Class<?>[]{CharSequence.class}, value.isEmpty() ? "本机（默认）" : value);
        } catch (Exception e) {
            Log.e(TAG, "W1 preference unavailable", e);
        }
    }

    public static boolean onPreferenceClick(Object fragment, Object preference) {
        try {
            if (!KEY.equals(field(preference.getClass(), "m").get(preference))) return false;
            open(fragment);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "W1 preference click failed", e);
            // Consume only our row; never change OEM row behavior.
            return false;
        }
    }

    private static List<String> custom(Context c) throws Exception {
        JSONArray values = new JSONArray(prefs(c).getString(CUSTOM, "[]"));
        List<String> result = new ArrayList<>();
        for (int i = 0; i < values.length(); i++) {
            String value = values.optString(i).trim();
            if (!value.isEmpty() && !result.contains(value)) result.add(value);
        }
        return result;
    }

    private static String clean(String value) {
        // Phoenix separates the Xiaomi logo from the editable model text.
        String result = value.trim().replaceAll("\\s+", " ");
        if (result.regionMatches(true, 0, "xiaomi ", 0, 7)) result = result.substring(7).trim();
        return result.replaceFirst("(?i)\\s*by\\s*leica\\s*$", " by Leica")
            .replaceFirst("(?i)(\\d)\\s*(ultra|pro|ti)\\b", "$1 $2")
            .replaceFirst("(?i)\\bultra\\b", "Ultra").replaceFirst("(?i)\\bpro\\b", "Pro")
            .replaceFirst("(?i)\\btitanium\\s*$", "Ti").replaceFirst("(?i)\\bti\\s*$", "Ti").trim();
    }

    private static String cleanPreset(String value) {
        String raw=value.trim().replaceAll("\\s+"," ");
        String normalized=clean(raw);
        // Phoenix preserves user spelling for plain 14ultra variants; other names are normalized by the store.
        if("14 Ultra".equals(normalized) && "14ultra".equals(raw.replace(" ","").toLowerCase(java.util.Locale.ROOT)))return raw;
        return normalized;
    }

    private static void updateSelection(Object fragment, Context c) throws Exception {
        Map<String,WeakReference<Object>> rows=ROWS.get(fragment);
        if(rows==null) { populate(fragment); return; }
        String active=selected(c);
        for(Map.Entry<String,WeakReference<Object>> e:rows.entrySet()) {
            Object row=e.getValue().get();
            if(row==null)continue;
            boolean checked=e.getKey().equals(active);
            boolean old=(Boolean)call(row,"isChecked",new Class<?>[0]);
            if(old!=checked) call(row,"setChecked",new Class<?>[]{boolean.class},checked);
        }
    }

    private static void choose(Object fragment, Context c, String value) {
        if (!saveString(c,ACTIVE,value)) {
            Toast.makeText(c, "保存失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Object item;
            try { item = field(fragment.getClass(), "watermarkItem").get(fragment); }
            catch (NoSuchFieldException noTemplate) {
                // Locate the native manager accessor rather than constructing a second manager.
                item = currentItem();
            }
            sync(item);  // Also restores OEM inputs when the selection becomes empty.
            call(item, "n0", new Class<?>[0]); // Existing MiSys publication, not manual vendor writes.
            if (own(fragment)) updateSelection(fragment,c);
            else install(fragment);
            Log.i(TAG, "Selection saved; native publication requested; default=" + value.isEmpty());
        } catch (Exception e) {
            Log.e(TAG, "Selection saved but native refresh failed", e);
            Toast.makeText(c, "文字已保存，请重新进入水印设置刷新", Toast.LENGTH_LONG).show();
            if (own(fragment)) {
                try { updateSelection(fragment,c); } catch(Exception ignored) { Log.w(TAG,"Selection UI refresh failed",ignored); }
            }
            else install(fragment);
        }
    }

    private static Object currentItem() throws Exception {
        // Same manager selection used by v16 WmSettingFragment.initWatermarkData.
        Object manager = hostType("U8.d").getMethod("b", boolean.class).invoke(null, false);
        return call(manager, "a", new Class<?>[0]);
    }

    private static Object row(Object fragment, Object group, String key, String title, boolean choice, boolean checked) throws Exception {
        Context c = (Context) call(fragment, "requireContext", new Class<?>[0]);
        Class<?> base = hostType("androidx.preference.Preference");
        Class<?> type = hostType(choice ? "miuix.preference.SingleChoicePreference" : "androidx.preference.Preference");
        Object value = type.getConstructor(Context.class, AttributeSet.class).newInstance(c, null);
        call(value, "a0", new Class<?>[]{String.class}, key);
        call(value, "e0", new Class<?>[]{String.class}, title);
        field(base, "t").setBoolean(value, false);
        field(base, "f").set(value,clickListener(fragment,true));
        if (choice) call(value, "setChecked", new Class<?>[]{boolean.class}, checked);
        call(group, "j0", new Class<?>[]{base}, value);
        return value;
    }

    public static void populate(Object fragment) {
        try {
            Context c = (Context) call(fragment, "requireContext", new Class<?>[0]);
            Object screen = field(fragment.getClass(), "mPreferenceGroup").get(fragment);
            call(screen, "m0", new Class<?>[0]);
            Class<?> base = hostType("androidx.preference.Preference");
            Object actions = call(fragment, "addCategory", new Class<?>[]{String.class, int.class}, "aurora_name_actions", -1);
            call(screen, "j0", new Class<?>[]{base}, actions);
            row(fragment, actions, "aurora_name_add", "新增预设", false, false);
            row(fragment, actions, "aurora_name_metadata", "同步元数据", false, false);
            row(fragment, actions, "aurora_name_manage", "管理预设", false, false);
            Object presets = call(fragment, "addCategory", new Class<?>[]{String.class, int.class}, "aurora_name_presets", -1);
            call(screen, "j0", new Class<?>[]{base}, presets);
            List<String> values = new ArrayList<>();
            for (String value : FIXED) values.add(value);
            for (String value : custom(c)) if (!values.contains(value)) values.add(value);
            Map<String,WeakReference<Object>> rows=new LinkedHashMap<>();
            for (String value : values) rows.put(value,new WeakReference<>(row(fragment, presets, "aurora_name_value:" + value,
                value.isEmpty() ? "本机" : value.equals("leitzphone powered by xiaomi") ? "Leitz Phone" : value,
                true, value.equals(selected(c)))));
            ROWS.put(fragment,rows);
        } catch (Exception e) { Log.e(TAG, "Preset page construction failed", e); }
    }

    public static void title(Object fragment) {
        try {
            Object activity=call(fragment,"requireActivity",new Class<?>[0]);
            ((android.app.Activity)activity).setTitle("水印机型");
            Object bar=call(activity,"getAppCompatActionBar",new Class<?>[0]);
            if(bar!=null) call(bar,"i",new Class<?>[]{String.class},"水印机型");
        }
        catch (Exception e) { Log.w(TAG, "Page title update failed", e); }
    }

    public static boolean pageClick(Object fragment, Object preference) {
        try {
            Context c = (Context) call(fragment, "requireContext", new Class<?>[0]);
            String key = (String) field(preference.getClass(), "m").get(preference);
            if (key.equals("aurora_name_add")) showAdd(fragment, c);
            else if (key.equals("aurora_name_metadata")) {((android.app.Activity)call(fragment,"requireActivity",new Class<?>[0])).getIntent().putExtra("aurora_watermark_metadata217",true);populateMetadata(fragment);metadataTitle(fragment);}
            else if (key.equals("aurora_name_manage")) showDelete(fragment, c);
            else if (key.startsWith("aurora_name_value:")) choose(fragment, c, key.substring(18));
            else return false;
            return true;
        } catch (Exception e) { Log.e(TAG, "Preset page click failed", e); return false; }
    }

    // Miuix l$a is the v16 equivalent of Phoenix h$a; no platform-dialog fallback.
    public static void populateMetadata(Object fragment) {
        try {
            Context c=(Context)call(fragment,"requireContext",new Class<?>[0]);
            Object screen=field(fragment.getClass(),"mPreferenceGroup").get(fragment);
            call(screen,"m0",new Class<?>[0]);
            Class<?> base=hostType("androidx.preference.Preference");
            for(String key:new String[]{"metadata_image_enabled","metadata_watermark_enabled"}) {
                Object row=hostType("androidx.preference.SwitchPreference").getConstructor(Context.class,AttributeSet.class).newInstance(c,null);
                call(row,"a0",new Class<?>[]{String.class},key);
                call(row,"e0",new Class<?>[]{String.class},key.equals("metadata_image_enabled")?"图片元数据":"水印元数据");
                field(base,"t").setBoolean(row,false); field(base,"e").set(row,changeListener(fragment));
                call(row,"setChecked",new Class<?>[]{boolean.class},prefs(c).getBoolean(key,true));
                call(screen,"j0",new Class<?>[]{base},row);
            }
        } catch(Exception e) { Log.e(TAG,"Metadata page unavailable",e); }
    }
    public static boolean metadataChange(Object fragment,Object preference,Object value) {
        try {
            String key=(String)field(preference.getClass(),"m").get(preference);
            if(!key.equals("metadata_image_enabled") && !key.equals("metadata_watermark_enabled")) return false;
            Context c=(Context)call(fragment,"requireContext",new Class<?>[0]);
            SharedPreferences preferences=prefs(c);boolean old=preferences.getBoolean(key,true),existed=preferences.contains(key);
            if(preferences.edit().putBoolean(key,(Boolean)value).commit())return true;
            SharedPreferences.Editor restore=preferences.edit();if(existed)restore.putBoolean(key,old);else restore.remove(key);
            if(!restore.commit())throw new IllegalStateException("Metadata preference rollback failed");return false;
        } catch(Exception e) { Log.e(TAG,"Metadata preference commit failed",e); return false; }
    }
    public static void metadataTitle(Object fragment) {
        try {
            Object activity=call(fragment,"requireActivity",new Class<?>[0]);
            ((android.app.Activity)activity).setTitle("同步元数据");
            Object bar=call(activity,"getAppCompatActionBar",new Class<?>[0]);
            if(bar!=null) call(bar,"i",new Class<?>[]{String.class},"同步元数据");
        } catch(Exception e) { Log.w(TAG,"Metadata title update failed",e); }
    }
    private static Object dialogBuilder(Context c, String title) throws Exception {
        Object b=hostType("miuix.appcompat.app.l$a").getConstructor(Context.class).newInstance(c);
        call(b,"D",new Class<?>[]{CharSequence.class},title);
        call(b,"r",new Class<?>[]{CharSequence.class,DialogInterface.OnClickListener.class},"取消",null);
        return b;
    }
    private static Dialog showDialog(Object builder) throws Exception {
        Dialog d=(Dialog)call(builder,"c",new Class<?>[0]); d.show(); return d;
    }

    private static void showAdd(Object fragment, Context c) {
        try {
            EditText input=new EditText(c);
            input.setSingleLine(true);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)});
            input.setHint("请输入机型名称");
            LinearLayout content=new LinearLayout(c); content.setOrientation(LinearLayout.VERTICAL);
            float density=c.getResources().getDisplayMetrics().density;
            content.setPadding((int)(24*density),(int)(12*density),(int)(24*density),(int)(12*density));
            content.addView(input,new LinearLayout.LayoutParams(-1,-2));
            Object b=hostType("miuix.appcompat.app.l$a").getConstructor(Context.class).newInstance(c);
            call(b,"D",new Class<?>[]{CharSequence.class},"新增预设");
            call(b,"E",new Class<?>[]{View.class},content);
            call(b,"z",new Class<?>[]{CharSequence.class,DialogInterface.OnClickListener.class},"修正并保存",null);
            call(b,"r",new Class<?>[]{CharSequence.class,DialogInterface.OnClickListener.class},"保存预设",null);
            Dialog dialog=showDialog(b);
            dialog.setCancelable(true);
            for(int id:new int[]{-1,-2}) {
              Button button=(Button)call(dialog,"l",new Class<?>[]{int.class},id);
              final boolean normalize=id==-1;
              if(id==-2) {
                  button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                  button.setTextColor(android.graphics.Color.BLACK);
              }
              button.setOnClickListener(v -> {
                String value=input.getText().toString().trim();
                if(normalize)value=clean(value);
                value=cleanPreset(value);
                if(value.isEmpty()) { input.setError("请输入机型文字"); return; }
                try {
                    List<String> values=custom(c);
                    boolean fixed=false;
                    for(String preset:FIXED)if(preset.equals(value)&&value.equals(clean(value)))fixed=true;
                    if(fixed||values.contains(value)) { input.setError("预设已存在"); return; }
                    values.add(value);
                    if(!saveString(c,CUSTOM,new JSONArray(values).toString())) throw new IllegalStateException("Preference commit failed");
                    // Phoenix adds a preset without selecting it or publishing another model.
                    populate(fragment); dialog.dismiss();
                } catch(Exception e) { Log.e(TAG,"Add preset failed",e); input.setError("预设保存失败"); }
              });
            }
        } catch(Exception e) { Log.e(TAG,"MIUI dialog unavailable",e); Toast.makeText(c,"预设页面暂不可用",Toast.LENGTH_SHORT).show(); }
    }

    private static void showDelete(Object fragment, Context c) {
        try {
            List<String> values = custom(c);
            if (values.isEmpty()) {
                Toast.makeText(c, "暂无自定义预设", Toast.LENGTH_SHORT).show();
                return;
            }
            Object list=dialogBuilder(c,"删除自定义预设");
            DialogInterface.OnClickListener select=(d, which) -> {
                    String value = values.get(which);
                    try {
                    Object confirmBuilder=dialogBuilder(c,"删除“" + value + "”？");
                    DialogInterface.OnClickListener delete=(confirm, button) -> {
                            values.remove(which);
                            if (!saveString(c,CUSTOM,new JSONArray(values).toString())) {
                                Toast.makeText(c, "删除失败", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (value.equals(selected(c))) { choose(fragment, c, ""); populate(fragment); }
                            else if (own(fragment)) populate(fragment);
                            else install(fragment);
                        };
                    call(confirmBuilder,"z",new Class<?>[]{CharSequence.class,DialogInterface.OnClickListener.class},"删除",delete);
                    showDialog(confirmBuilder);
                    } catch(Exception e) { Log.e(TAG,"MIUI confirm unavailable",e); }
                };
            call(list,"m",new Class<?>[]{CharSequence[].class,DialogInterface.OnClickListener.class},values.toArray(new String[0]),select);
            showDialog(list);
        } catch (Exception e) { Log.e(TAG, "Delete preset failed", e); }
    }
}
