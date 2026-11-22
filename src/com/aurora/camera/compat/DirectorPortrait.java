package com.aurora.camera.compat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Portrait layout for the native director controls and manual parameter cards. */
final class DirectorPortrait implements HookPlan {
    private static final int DIRECTOR = 164;
    private static final int DIRECTOR_PORTRAIT_LAYOUT = 5;
    private static final int CONTROL_GAP = 0x7f070cf8;
    private static final int SHUTTER_GAP = 0x7f070cfa;
    private static final float CONTROL_WIDTH_DP = 42.18f;
    private static final int PARAMETER_LABEL_ROW = 0x7f0b01f3;
    private static final int PARAMETER_VALUE = 0x7f0b01f7;
    private static final int AUTO_BADGE = 0x7f0b01f8;
    private static final String APPLIED = "director-portrait-layout";

    private final Constructor<?> createControls;
    private final Method layoutAlerts;
    private final Method layoutThumbnail;
    private final Method windowBounds;
    private final Field controls;
    private final Field portraitRow;
    private final Field fragmentMode;

    private final Method buildParameters;
    private final Method isLandscape;
    private final Method parametersContext;
    private final Method dataConfig;
    private final Method component;
    private final Method parameterTitle;
    private final Class<?> videoQuality;
    private final Field parameters;
    private final Field parameterTypes;
    private final Field parameterPositions;
    private final Field resolution;
    private final Field frameRate;
    private final Method bindParameter;
    private final Field adapterMode;
    private final Field adapterDegree;
    private final Field holderView;
    private final Class<?> whiteBalance;
    private final Class<?> shutterSpeed;
    private final Class<?> iso;

    static HookPlan prepare(Host host) throws ReflectiveOperationException {
        return new DirectorPortrait(host);
    }

    private DirectorPortrait(Host host) throws ReflectiveOperationException {
        Class<?> row = host.type("z4.n");
        createControls = row.getDeclaredConstructor(ViewGroup.class, boolean.class, host.type("Aa.w"));
        createControls.setAccessible(true);
        layoutAlerts = host.method("X9.i", "a3");
        layoutThumbnail = host.method("z4.O", "c", Context.class, int.class, boolean.class);
        windowBounds = host.method("K2.b", "g", int.class);
        controls = host.field("X9.i", "T1");
        portraitRow = host.field(row, "c");
        fragmentMode = host.field("com.android.camera.fragment.h", "mCurrentMode");

        Class<?> manual = host.type("M9.k");
        buildParameters = host.method(manual, "ms");
        isLandscape = host.method("com.android.camera.fragment.h", "isLandScape");
        parametersContext = host.method("androidx.fragment.app.Fragment", "getContext");
        dataConfig = host.method("g2.a", "a");
        component = host.method("Xh.b", "x", Class.class);
        parameterTitle = host.method("com.android.camera.data.data.c", "getDisplayTitleString");
        videoQuality = host.type("r2.h0");
        parameters = host.field(manual, "h");
        parameterTypes = host.field(manual, "i");
        parameterPositions = host.field(manual, "j");
        resolution = host.field(videoQuality, "g");
        frameRate = host.field(videoQuality, "h");
        bindParameter = host.method("N9.c", "w", host.type("com.android.camera.fragment.w"), int.class);
        adapterMode = host.field("R4.D", "c");
        adapterDegree = host.field("R4.D", "f");
        holderView = host.field("androidx.recyclerview.widget.RecyclerView$C", "itemView");
        whiteBalance = host.type("r2.k1");
        shutterSpeed = host.type("r2.E0");
        iso = host.type("r2.M0");
    }

    @Override public String id() { return "DirectorPortrait"; }

    @Override public void install() {
        HookDiagnostics.hookMethod(createControls, new PortraitHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (!param.hasThrowable() && layoutPortraitRow(param.thisObject)) mark(param);
            }
        });
        HookDiagnostics.hookMethod(layoutAlerts, new PortraitHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                if (param.hasThrowable() || fragmentMode.getInt(param.thisObject) != DIRECTOR) return;
                Object model = controls.get(param.thisObject);
                if (model != null && layoutPortraitRow(model)) mark(param);
            }
        });
        HookDiagnostics.hookMethod(layoutThumbnail, new PortraitHook() {
            @Override protected void after(MethodHookParam param) {
                if (param.hasThrowable() || (int) param.args[1] != DIRECTOR_PORTRAIT_LAYOUT
                        || (boolean) param.args[2]) return;
                RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) param.getResult();
                layout.setMarginStart(((Context) param.args[0]).getResources().getDimensionPixelSize(CONTROL_GAP));
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(buildParameters, new PortraitHook() {
            @Override protected void after(MethodHookParam param) throws ReflectiveOperationException {
                Object fragment = param.thisObject;
                if (param.hasThrowable() || fragmentMode.getInt(fragment) != DIRECTOR
                        || (boolean) isLandscape.invoke(fragment)) return;
                addVideoParameters(fragment);
                mark(param);
            }
        });
        HookDiagnostics.hookMethod(bindParameter, new PortraitHook() {
            @Override protected void after(MethodHookParam param) throws IllegalAccessException {
                if (param.hasThrowable() || adapterMode.getInt(param.thisObject) != DIRECTOR) return;
                View item = (View) holderView.get(param.args[0]);
                View label = item.findViewById(PARAMETER_LABEL_ROW);
                TextView value = item.findViewById(PARAMETER_VALUE);
                if (label == null || value == null) return;
                boolean portrait = adapterDegree.getFloat(param.thisObject) % 180f == 0f;
                LinearLayout card = (LinearLayout) item;
                View first = portrait ? label : value;
                if (card.getChildAt(0) != first) {
                    View second = portrait ? value : label;
                    LinearLayout.LayoutParams firstParams = (LinearLayout.LayoutParams) first.getLayoutParams();
                    LinearLayout.LayoutParams secondParams = (LinearLayout.LayoutParams) second.getLayoutParams();
                    int gap = firstParams.topMargin;
                    firstParams.topMargin = secondParams.topMargin;
                    secondParams.topMargin = gap;
                    card.removeView(first);
                    card.addView(first, 0, firstParams);
                    second.setLayoutParams(secondParams);
                    mark(param);
                }
                if (!portrait) return;
                View badge = item.findViewById(AUTO_BADGE);
                if (badge.getVisibility() == View.VISIBLE) {
                    Object component = item.getTag();
                    String automatic = whiteBalance.isInstance(component) ? "AWB"
                            : shutterSpeed.isInstance(component) || iso.isInstance(component) ? "AUTO" : null;
                    if (automatic != null) {
                        CharSequence before = value.getText();
                        CharSequence description = item.getContentDescription();
                        if (!TextUtils.isEmpty(before) && description != null) {
                            item.setContentDescription(TextUtils.replace(description,
                                    new String[]{before.toString()}, new CharSequence[]{automatic}));
                        }
                        value.setText(automatic);
                        value.setContentDescription(automatic);
                    }
                    badge.setVisibility(View.GONE);
                }
                mark(param);
            }
        });
    }

    private boolean layoutPortraitRow(Object model) throws ReflectiveOperationException {
        LinearLayout row = (LinearLayout) portraitRow.get(model);
        if (row == null) return false;
        int count = row.getChildCount();
        Resources resources = row.getResources();
        int gap = resources.getDimensionPixelSize(CONTROL_GAP);
        int shutter = resources.getDimensionPixelSize(SHUTTER_GAP);
        int width = Math.round(CONTROL_WIDTH_DP * resources.getDisplayMetrics().density);
        Rect bounds = (Rect) windowBounds.invoke(null, DIRECTOR_PORTRAIT_LAYOUT);
        int shortSide = Math.min(bounds.width(), bounds.height());
        // The 14 Ultra stock row starts at the first control on the shutter's left.
        int first = (shortSide - shutter) / 2 - (count / 2 - 1) * (width + gap) - width;
        for (int i = 0; i < count; i++) {
            View control = row.getChildAt(i);
            LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) control.getLayoutParams();
            layout.width = width;
            layout.setMarginStart(i == 0 ? first : i == count / 2 ? shutter : gap);
            control.setLayoutParams(layout);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private void addVideoParameters(Object fragment) throws ReflectiveOperationException {
        Object quality = component.invoke(dataConfig.invoke(null), videoQuality);
        List<Object> rows = (List<Object>) parameters.get(fragment);
        List<Integer> types = (List<Integer>) parameterTypes.get(fragment);
        Map<String, Integer> positions = (Map<String, Integer>) parameterPositions.get(fragment);
        Context context = (Context) parametersContext.invoke(fragment);
        for (Object item : new Object[]{resolution.get(quality), frameRate.get(quality)}) {
            rows.add(null);
            types.add(1);
            positions.put(context.getString((int) parameterTitle.invoke(item)), rows.size());
            rows.add(item);
            types.add(0);
        }
    }

    private abstract static class PortraitHook extends StrictHook {
        final void mark(MethodHookParam param) { param.setObjectExtra(APPLIED, Boolean.TRUE); }
        @Override protected boolean countsAsRun(MethodHookParam param) {
            return Boolean.TRUE.equals(param.getObjectExtra(APPLIED));
        }
    }
}
