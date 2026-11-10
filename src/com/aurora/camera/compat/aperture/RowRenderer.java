package com.aurora.camera.compat.aperture;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageView;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Objects;

/** The accepted base-aperture branch, before stock consumes its string as a resource ID. */
final class RowRenderer {
    private final Class<?> aperture, professional;
    private final Field component, rowView, keepDisabled, resourceDisplay, autoDisplay;
    private final Method title, value, defaultValue, display, resourceValue, disabled, automatic;
    private final Method icon, shadowIcon, description, colour, drawable, colourFilter, draw;
    private final Method scale, fontProfile, fontName, applyFont;
    private final Field fontService;
    private final Constructor<?> drawState;

    RowRenderer(Host h,Class<?> aperture,Class<?> professional) throws ReflectiveOperationException {
        this.aperture=aperture; this.professional=professional;
        Class<?> base=h.type("com.android.camera.data.data.c");
        component=h.field("Q4.x0","b"); rowView=h.field("Q4.x0","a");
        keepDisabled=h.field(base,"mIsKeepValueWhenDisabled");
        resourceDisplay=h.field(base,"mIsDisplayStringFromResourceId"); autoDisplay=h.field(aperture,"f0");
        title=h.method(aperture,"getDisplayTitleString"); value=h.method(base,"getComponentValue",int.class);
        defaultValue=h.method(base,"getDefaultValue",int.class); display=h.method(aperture,"c",int.class);
        resourceValue=h.method(base,"getValueDisplayString",int.class); disabled=h.method(aperture,"disableUpdate");
        automatic=h.method(aperture,"b"); icon=h.method(base,"getValueSelectedDrawable",int.class);
        shadowIcon=h.method(base,"getValueSelectedShadowDrawable",int.class);
        description=h.method(base,"getContentDescriptionString"); colour=h.method("com.android.camera.data.data.x","z");
        drawable=h.method("j.a","a",Context.class,int.class); colourFilter=h.method("f2.a","f",int.class,boolean.class);
        scale=h.method("R1.h","j",float.class,View[].class);
        fontService=h.field("q9.a","a"); fontProfile=h.method("q9.b","d");
        fontName=h.method("r9.f","j"); applyFont=h.method("pa.a","c",TextView.class,String.class);
        Class<?> state=h.type("Q4.E$a");
        drawState=state.getDeclaredConstructor(ImageView.class,TextView.class,TextView.class,TextView.class,
                boolean.class,boolean.class,boolean.class,int.class,Drawable.class,Drawable.class);
        drawState.setAccessible(true); draw=h.method("Q4.E","b",state);
    }

    boolean bind(ArrayList<?> selectors,int index,int selected,int mode,int runningMode)
            throws ReflectiveOperationException {
        Object selector=selectors.get(index), data=component.get(selector);
        if (!aperture.isInstance(data) || professional.isInstance(data)) return false;
        View root=(View)rowView.get(selector);
        scale.invoke(null,0.9f,new View[]{root});
        // Q4.F has no own state: its native superclass initialization must run.
        root.setAccessibilityDelegate(new SelectorAccessibility());
        TextView valueText=root.findViewById(0x7f0b06e1);
        ImageView autoIcon=root.findViewById(0x7f0b06e3);
        TextView autoText=root.findViewById(0x7f0b06e2);
        TextView titleText=root.findViewById(0x7f0b06dd);
        for (TextView text:new TextView[]{valueText,autoText,titleText})
            applyFont.invoke(null,text,fontName.invoke(fontProfile.invoke(fontService.get(null))));
        int activeColour=(Integer)colour.invoke(null);
        Drawable plain=(Drawable)drawable.invoke(null,root.getContext(),0x7f080775);
        Drawable active=(Drawable)drawable.invoke(null,root.getContext(),0x7f080775);
        plain.setBounds(0,0,plain.getIntrinsicWidth(),plain.getIntrinsicHeight());
        if (active!=null) {
            active.setColorFilter((ColorFilter)colourFilter.invoke(null,activeColour,true));
            active.setBounds(0,0,active.getIntrinsicWidth(),active.getIntrinsicHeight());
        }
        boolean blocked=(Boolean)disabled.invoke(data);
        int label=ApertureRules.ai(runningMode,true,false) ? 0x7f140d73 : (Integer)title.invoke(data);
        titleText.setText(label);
        boolean manualWhenDisabled=blocked && !keepDisabled.getBoolean(data);
        // Both the accepted v2.k disabled-string branch and its normal branch
        // enable the row; disabled remains a separate native draw/click state.
        root.setEnabled(true);
        root.setVisibility(View.VISIBLE);
        boolean atDefault=!manualWhenDisabled && Objects.equals(value.invoke(data,mode),defaultValue.invoke(data,mode));
        String text;
        if (manualWhenDisabled || atDefault && autoDisplay.getBoolean(data)
                || !resourceDisplay.getBoolean(data)) {
            text=(String)display.invoke(data,mode);
        } else {
            text=root.getResources().getString((Integer)resourceValue.invoke(data,mode));
        }
        valueText.setText(text);
        valueText.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        if (TextUtils.isEmpty(text)) {
            if ((Integer)icon.invoke(data,mode)==-1 || (Integer)shadowIcon.invoke(data,mode)==-1) return true;
            autoIcon.setVisibility(View.VISIBLE); autoIcon.setActivated(index==selected);
            valueText.setBackground(null);
        }
        int descriptionId=(Integer)description.invoke(data);
        CharSequence name=descriptionId>0 ? root.getContext().getString(descriptionId) : titleText.getText();
        CharSequence spoken=text;
        try {
            long numeric=Long.parseLong(text);
            SpannableString number=new SpannableString(text);
            number.setSpan(new TtsSpan.CardinalBuilder().setNumber(numeric).build(),0,text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spoken=number;
        } catch (NumberFormatException nonIntegerAperture) {
            // The original renderer deliberately uses plain speech for aperture decimals/Auto.
        }
        SpannableStringBuilder content=new SpannableStringBuilder().append(name).append(" ")
                .append(spoken).append(" ").append(root.getContext().getString(
                        index==selected ? 0x7f1400b9 : 0x7f1400ba));
        root.setContentDescription(content); valueText.setContentDescription(spoken);
        titleText.setContentDescription(name);
        Object state=drawState.newInstance(autoIcon,autoText,valueText,titleText,
                automatic.invoke(data),index==selected,blocked,activeColour,active,plain);
        draw.invoke(null,state);
        return true;
    }

    /** Same Q4.F behavior, with a real AccessibilityDelegate superclass constructor. */
    private static final class SelectorAccessibility extends View.AccessibilityDelegate {
        @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfo info) {
            if (host==null) throw new NullPointerException("host");
            if (info==null) throw new NullPointerException("info");
            super.onInitializeAccessibilityNodeInfo(host,info);
            info.setClassName("android.widget.Button");
        }
    }
}
