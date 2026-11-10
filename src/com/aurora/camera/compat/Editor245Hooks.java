package com.aurora.camera.compat;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.aurora.camera.compat.editor.Editor245Policy;
import com.aurora.camera.compat.editor.Editor245State;
import com.aurora.camera.compat.editor.Editor245Templates;
import com.aurora.camera.compat.editor.Editor245Assets;
import com.aurora.camera.compat.editor.Editor245Xml;
import com.aurora.camera.compat.editor.EditorWatermark266;
import com.aurora.camera.compat.editor.Editor245CarCatalog;
import com.aurora.camera.compat.editor.cover247.Editor247Covers;
import com.aurora.camera.compat.editor.EditorWatermarkNames;
import com.aurora.camera.compat.editor.EditorNativeCloud291;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Editor contracts resolved as one group before installing any watermark/filter hooks. */
public final class Editor245Hooks {
    private static final String TAG = "AuroraEditor245";
    private static final String SETTING = "com.miui.mediaeditor.photo.watermark.widgets.watermarksetting.WmSettingFragment";
    private static final String KEY = "aurora_editor245_model";
    private static final String PREVIOUS = "aurora.editor245.previous";
    private final Host host;
    private final boolean nativeCloud;
    private final Method catalog, capability, eligibility, render, text, paint, populate, click, initializeAssets, initializeManager;
    private final EditorWatermarkNames watermarkNames;
    private final EditorNativeCloud291 nativeEnvironment;
    private final ThreadLocal<Editor245Assets.Guard> assetOwner = new ThreadLocal<>();
    private final Editor245Templates templates;
    private final Editor245Xml xml;
    private final EditorWatermark266 specialWatermark;
    private final Editor245CarCatalog cars;
    private final Editor247Covers covers;
    private final Method nodeId, requireContext, findPreference, addPreference, setKey, setTitle, setSummary, setPersistent;
    private final Method preview, dialogTitle, dialogView, dialogPositive, dialogNegative, dialogShow, dialogButton;
    private final Constructor<?> preferenceConstructor, dialogConstructor;
    private final Field category, previewListener, failedReason;
    private final Field persistent;
    private final Class<?> modelNode, failedEligibility;
    private final Object success;
    private final int[] eligibilityReasons;
    private final ThreadLocal<Integer> catalogDepth = new ThreadLocal<>();
    private final ThreadLocal<ModelScope> modelScope = new ThreadLocal<>();
    private volatile boolean fontLogged;

    private static final class ModelScope {
        final Object node;
        final String selected;
        ModelScope(Object node, String selected) { this.node = node; this.selected = selected; }
    }

    public Editor245Hooks(Host h) throws ReflectiveOperationException {
        host = h;
        nativeCloud = h.usesNativeCloudFilter();
        nativeEnvironment = nativeCloud ? new EditorNativeCloud291(h) : null;
        watermarkNames = new EditorWatermarkNames(h);
        templates = new Editor245Templates(h);
        specialWatermark = new EditorWatermark266(h);
        xml = new Editor245Xml(h, specialWatermark);
        cars = nativeCloud ? null : new Editor245CarCatalog(h);
        covers = new Editor247Covers(h);
        initializeAssets = h.method("com.miui.mediaeditor.photo.watermask.CloudWatermarkMaker", "c");
        initializeManager = h.method("com.miui.mediaeditor.photo.watermask.CloudWatermarkMaker", "l", boolean.class);
        Class<?> config = h.type("com.miui.mediaeditor.photo.watermark.model.cloudwatermark.CloudWatermarkConfigData");
        catalog = h.method("vg.q", "a", nativeCloud ? String.class : config, List.class);
        capability = nativeCloud ? h.method("mb.a", "f", int.class) : h.method("mb.a", "f");
        eligibility = h.method("vg.t", "a", h.type("Fg.b"), String.class, h.type("S3.b"), h.type("kh.e"));
        eligibilityReasons = h.resourceConstants(eligibility);
        if (eligibilityReasons != null && eligibilityReasons.length != 2)
            throw new NoSuchMethodException("Editor eligibility must expose two native rejection resources");
        failedEligibility = h.type("Bg.a$b");
        failedReason = h.field(failedEligibility, "a");
        success = h.field("Bg.a$a", "a").get(null);
        modelNode = h.type("mn.o");
        nodeId = h.method("mn.a", "getId");
        text = h.method(modelNode, "i");
        render = h.method("com.xiaomi.cam.watermark.c", "f", Context.class, Bitmap.class,
                h.type("in.b"), h.type("kn.d"), h.type("nn.a"), String.class, float.class,
                h.type("Hn.e"), String.class, boolean.class, PorterDuff.Mode.class, h.type("Jm.X"), boolean.class, boolean.class);
        paint = h.method("on.b$a", "d", Context.class, h.type("jn.a"), h.type("ln.a$a"), String.class);
        Class<?> settings = h.type(SETTING), pref = h.type("androidx.preference.Preference");
        populate = h.method(settings, "A0");
        click = h.method(settings, "onPreferenceClick", pref);
        requireContext = settings.getMethod("requireContext");
        findPreference = nativeCloud ? h.method("androidx.preference.PreferenceFragmentCompat", "findPreference", String.class)
                : settings.getMethod("findPreference", CharSequence.class);
        category = h.field(settings, "D");
        previewListener = h.field(settings, "H");
        preview = h.method("Sg.a", "s");
        preferenceConstructor = pref.getConstructor(Context.class, AttributeSet.class);
        addPreference = nativeCloud ? h.method("androidx.preference.PreferenceGroup", "addPreference", pref)
                : h.type("androidx.preference.PreferenceGroup").getMethod("addPreference", pref);
        setKey = nativeCloud ? h.method(pref,"setKey",String.class) : pref.getMethod("setKey", String.class);
        setTitle = nativeCloud ? h.method(pref,"setTitle",String.class) : pref.getMethod("setTitle", CharSequence.class);
        setSummary = nativeCloud ? h.method(pref,"setSummary",CharSequence.class) : pref.getMethod("setSummary", CharSequence.class);
        setPersistent = nativeCloud ? null : pref.getMethod("setPersistent", boolean.class);
        persistent = nativeCloud ? h.field(pref,"persistent") : null;
        Class<?> builder = h.type("miuix.appcompat.app.AlertDialog$a");
        dialogConstructor = builder.getConstructor(Context.class);
        dialogTitle = h.method(builder, "B", CharSequence.class);
        dialogView = h.method(builder, "D", View.class);
        dialogPositive = h.method(builder, "w", CharSequence.class, DialogInterface.OnClickListener.class);
        dialogNegative = h.method(builder, "r", CharSequence.class, DialogInterface.OnClickListener.class);
        dialogShow = h.method(builder, "E");
        dialogButton = h.method("miuix.appcompat.app.AlertDialog", "g", int.class);
        if (initializeManager.getReturnType() != boolean.class || initializeAssets.getReturnType() != boolean.class || catalog.getReturnType() != config || capability.getReturnType() != boolean.class
                || eligibility.getReturnType() != h.type("Bg.a") || failedReason.getType() != int.class
                || !h.type("Bg.a").isInstance(success) || nodeId.getReturnType() != String.class
                || text.getReturnType() != String.class || render.getReturnType() != h.type("Hn.a")
                || paint.getReturnType() != TextPaint.class || populate.getReturnType() != void.class
                || category.getType() != h.type("androidx.preference.PreferenceCategory")
                || previewListener.getType() != h.type("Sg.a") || preview.getReturnType() != void.class
                || dialogShow.getReturnType() != h.type("miuix.appcompat.app.AlertDialog")
                || dialogButton.getReturnType() != Button.class) {
            throw new NoSuchMethodException("Editor245 pinned watermark ABI differs");
        }
    }

    public void install() {
        if (nativeEnvironment != null) nativeEnvironment.install();
        watermarkNames.install();
        specialWatermark.install(covers::rendering);
        xml.install();
        if (cars != null) cars.install();
        covers.install();
        StrictHook initializationGuard = new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (assetOwner.get() == null) {
                    Editor245Assets.Guard guard = templates.begin();
                    assetOwner.set(guard); p.setObjectExtra("editor245.assetGuard", guard);
                }
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                Editor245Assets.Guard guard = (Editor245Assets.Guard) p.getObjectExtra("editor245.assetGuard");
                if (guard != null) { assetOwner.remove(); guard.close(); }
                if (!p.hasThrowable() && Boolean.TRUE.equals(p.getResult())) covers.nativeReady(templates.availableGroups());
            }
        };
        // l(boolean) is synchronous and covers c(), file publication and native manager parsing.
        XposedBridge.hookMethod(initializeManager, initializationGuard);
        XposedBridge.hookMethod(initializeAssets, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                if (assetOwner.get() == null) {
                    Editor245Assets.Guard guard = templates.begin();
                    assetOwner.set(guard); p.setObjectExtra("editor245.assetGuard", guard);
                }
                templates.invalidate();
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                Editor245Assets.Guard guard = (Editor245Assets.Guard) p.getObjectExtra("editor245.assetGuard");
                try { if (!p.hasThrowable() && Boolean.TRUE.equals(p.getResult())) templates.publish(); }
                finally { if (guard != null) { assetOwner.remove(); guard.close(); } }
            }
        });
        XposedBridge.hookMethod(catalog, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Integer previous = catalogDepth.get();
                p.setObjectExtra(PREVIOUS, previous);
                catalogDepth.set(previous == null ? 1 : previous + 1);
                if (assetOwner.get() == null) {
                    Editor245Assets.Guard guard = templates.begin();
                    assetOwner.set(guard); p.setObjectExtra("editor247.catalogGuard", guard);
                }
                templates.prepare();
                p.args[0]=nativeCloud?templates.unlockLeica((String)p.args[0]):templates.unlockLeica(p.args[0]);
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                try { if (!p.hasThrowable()) p.setResult(templates.merge(p.getResult(), nativeCloud ? p.getResult() : p.args[0])); }
                finally {
                    Integer previous = (Integer) p.getObjectExtra(PREVIOUS);
                    if (previous == null) catalogDepth.remove(); else catalogDepth.set(previous);
                    Editor245Assets.Guard guard = (Editor245Assets.Guard) p.getObjectExtra("editor247.catalogGuard");
                    if (guard != null) { assetOwner.remove(); guard.close(); }
                }
            }
        });
        XposedBridge.hookMethod(capability, new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                Integer depth = catalogDepth.get();
                if (depth != null && depth > 0 && (!nativeCloud || ((Integer)p.args[0]) == 8)) p.setResult(true);
            }
        });
        XposedBridge.hookMethod(eligibility, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable() || !failedEligibility.isInstance(p.getResult())) return;
                if (Editor245Policy.leicaEligibility((String) p.args[1], failedReason.getInt(p.getResult()), eligibilityReasons)) {
                    // The native "camera" branch returns only AFTER the same template's required
                    // shooting fields were checked, but BEFORE product/series/market eligibility.
                    // Replay that pure branch: never synthesize EXIF or treat its shared error
                    // string as proof that the photo lacks parameters.
                    Object[] photoArguments = p.args.clone();
                    photoArguments[1] = "camera";
                    Object photoEligibility = XposedBridge.invokeOriginalMethod(eligibility, p.thisObject, photoArguments);
                    if (photoEligibility == success) {
                        Log.i(TAG, "Leica eligibility: native required photo fields accepted; product restriction unlocked");
                        p.setResult(success);
                    }
                }
            }
        });
        XposedBridge.hookMethod(render, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                p.setObjectExtra(PREVIOUS, modelScope.get());
                Object node = p.args[4];
                modelScope.remove();
                if (covers.rendering()) return;
                if (!modelNode.isInstance(node) || !Editor245Policy.modelId((String) nodeId.invoke(node))) return;
                String selected = Editor245State.read((Context) p.args[0]);
                String name = EditorWatermark266.model(selected.isEmpty() ? specialWatermark.deviceName(p.thisObject) : selected);
                String id = (String) nodeId.invoke(node);
                if (EditorWatermark266.special(name)) modelScope.set(new ModelScope(node, ""));
                else if (!selected.isEmpty()) {
                    boolean suffix = "model_versionName".equalsIgnoreCase(id);
                    String value = EditorWatermark266.titanium(name)
                            ? (suffix ? "Ti" : name.substring(0, name.length() - 3).trim())
                            : (suffix ? "" : selected);
                    modelScope.set(new ModelScope(node, value));
                }
            }
            @Override protected void after(MethodHookParam p) {
                ModelScope previous = (ModelScope) p.getObjectExtra(PREVIOUS);
                if (previous == null) modelScope.remove(); else modelScope.set(previous);
            }
        });
        XposedBridge.hookMethod(text, new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                ModelScope scope = modelScope.get();
                if (scope != null && scope.node == p.thisObject) p.setResult(scope.selected);
            }
        });
        XposedBridge.hookMethod(paint, new StrictHook() {
            @Override protected void after(MethodHookParam p) {
                ModelScope scope = modelScope.get();
                if (p.hasThrowable() || scope == null || !(p.getResult() instanceof TextPaint)) return;
                TextPaint textPaint = (TextPaint) p.getResult();
                if (!hasGlyphs(textPaint, scope.selected)) {
                    Typeface original = textPaint.getTypeface();
                    textPaint.setTypeface(Typeface.create("sans-serif", original == null ? Typeface.NORMAL : original.getStyle()));
                    if (!fontLogged) {
                        fontLogged = true;
                        Log.i(TAG, "custom model uses system glyph fallback; missing glyphs remain a system font limitation");
                    }
                }
            }
        });
        XposedBridge.hookMethod(populate, new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) row(p.thisObject);
            }
        });
        XposedBridge.hookMethod(click, new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                Object selected = findPreference.invoke(p.thisObject, KEY);
                if (selected == null || selected != p.args[0]) return;
                showEditor(p.thisObject, selected);
                p.setResult(true);
            }
        });
    }

    private void row(Object fragment) throws Throwable {
        Context context = (Context) requireContext.invoke(fragment);
        Object value = findPreference.invoke(fragment, KEY);
        if (value == null) {
            value = preferenceConstructor.newInstance(context, null);
            setKey.invoke(value, KEY);
            setTitle.invoke(value, "水印机型");
            if (nativeCloud) persistent.setBoolean(value, false); else setPersistent.invoke(value, false);
            Object group = category.get(fragment);
            if (group == null || !Boolean.TRUE.equals(addPreference.invoke(group, value))) {
                throw new IllegalStateException("Editor245 content category did not accept model preference");
            }
        }
        String selected = Editor245State.read(context);
        setSummary.invoke(value, selected.isEmpty() ? "照片原机型（默认）" : selected);
    }

    private void showEditor(Object fragment, Object preference) throws Throwable {
        Context context = (Context) requireContext.invoke(fragment);
        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setHint("留空使用照片原机型");
        input.setText(Editor245State.read(context));
        input.setSelectAllOnFocus(true);
        Object builder = dialogConstructor.newInstance(context);
        dialogTitle.invoke(builder, "水印机型");
        dialogView.invoke(builder, input);
        dialogPositive.invoke(builder, "保存", null);
        dialogNegative.invoke(builder, "取消", (DialogInterface.OnClickListener) (dialog, which) -> dialog.dismiss());
        Object dialog = dialogShow.invoke(builder);
        Button save = (Button) dialogButton.invoke(dialog, DialogInterface.BUTTON_POSITIVE);
        save.setOnClickListener(view -> {
            final String selected;
            try {
                selected = Editor245Policy.name(input.getText().toString());
                Editor245State.write(context, selected);
            } catch (java.io.IOException | IllegalArgumentException failure) {
                Log.e(TAG, "custom model was not saved", failure);
                input.setError(failure instanceof IllegalArgumentException ? failure.getMessage() : "保存失败，请重试");
                return;
            }
            ((DialogInterface) dialog).dismiss();
            try {
                setSummary.invoke(preference, selected.isEmpty() ? "照片原机型（默认）" : selected);
                Object listener = previewListener.get(fragment);
                if (listener == null) throw new IllegalStateException("Watermark preview listener is missing");
                preview.invoke(listener);
                Log.i(TAG, "editor model saved; native preview refresh requested; default=" + selected.isEmpty());
            } catch (ReflectiveOperationException | RuntimeException failure) {
                Log.e(TAG, "editor model saved; native preview refresh failed", failure);
                Toast.makeText(context, "机型已保存，请重新进入水印设置刷新", Toast.LENGTH_LONG).show();
            }
        });
    }

    private static boolean hasGlyphs(TextPaint paint, String value) {
        for (int i = 0; i < value.length();) {
            int point = value.codePointAt(i);
            int count = Character.charCount(point);
            if (point != 0x200d && point != 0xfe0f && point != 0xfe0e
                    && !Character.isWhitespace(point) && !paint.hasGlyph(value.substring(i, i + count))) return false;
            i += count;
        }
        return true;
    }
}
