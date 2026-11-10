package com.aurora.camera.compat.ui;

import android.content.res.TypedArray;
import android.view.View;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;

/** One native binder for the two fixed Classic/AUTH information rows. */
public final class NativeNotes249 {
    private static final String AUTH="aurora_auth_note248",CLASSIC="aurora_classic_note249",STYLE="aurora.native.notes249.style";
    private static boolean installed;
    private final NativePreferences240 preferences;
    private final Method bind;
    public NativeNotes249(Host host,NativePreferences240 preferences)throws ReflectiveOperationException{
        this.preferences=preferences;
        bind=host.method("miuix.preference.TextPreference","D",host.type("androidx.preference.l"));
        host.field("androidx.preference.Preference","r");host.field("miuix.preference.BasePreference","e0");host.field("miuix.preference.BasePreference","f0");
    }
    private static Object call(Object owner,String method,Object...args){return XposedHelpers.callMethod(owner,method,args);}
    private static boolean isNote(Object preference){Object key=XposedHelpers.getObjectField(preference,"m");return AUTH.equals(key)||CLASSIC.equals(key);}
    public void addAuth(Object fragment,Object screen,String text){add(fragment,screen,AUTH,"aurora_auth_note_group248",text);}
    public void addClassic(Object fragment,Object screen,String text){add(fragment,screen,CLASSIC,"aurora_classic_note_group249",text);}
    private void add(Object fragment,Object screen,String key,String groupKey,String text){
        Object group=preferences.category(fragment,screen,groupKey,null,-1);
        if(call(group,"k0",key)!=null)return;
        Object row=preferences.text(fragment);call(row,"a0",key);call(row,"e0","使用说明");call(row,"c0",text);
        XposedHelpers.setBooleanField(row,"t",false);XposedHelpers.setBooleanField(row,"r",false);XposedHelpers.setBooleanField(row,"e0",false);XposedHelpers.setBooleanField(row,"f0",false);call(group,"j0",row);
    }
    public void install(){
        synchronized(NativeNotes249.class){
            if(installed)return;
            XposedBridge.hookMethod(bind,new StrictHook(){@Override protected void before(MethodHookParam p){prepareNote(p.thisObject,p.args[0]);}@Override protected void after(MethodHookParam p){if(!p.hasThrowable())bindNote(p.thisObject,p.args[0]);}});
            installed=true;
        }
    }
    private static Object[] textStyle(TextView view){return view==null?null:new Object[]{view.getTextSize(),view.isSingleLine(),view.getMaxLines(),view.getEllipsize(),view.getTextColors(),view.getLineSpacingExtra(),view.getLineSpacingMultiplier()};}
    private static void restoreText(TextView view,Object[] style){
        if(view==null||style==null)return;view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,(Float)style[0]);view.setSingleLine((Boolean)style[1]);view.setMaxLines((Integer)style[2]);view.setEllipsize((android.text.TextUtils.TruncateAt)style[3]);view.setTextColor((android.content.res.ColorStateList)style[4]);view.setLineSpacing((Float)style[5],(Float)style[6]);
    }
    private static View noteArrow(View item){int id=item.getResources().getIdentifier("arrow_right","id",item.getContext().getPackageName());return id==0?null:item.findViewById(id);}
    private static void prepareNote(Object preference,Object holder){
        View item=(View)XposedHelpers.getObjectField(holder,"itemView");
        Object[] previous=(Object[])XposedHelpers.removeAdditionalInstanceField(item,STYLE);
        if(previous!=null){
            item.setPaddingRelative((Integer)previous[0],(Integer)previous[1],(Integer)previous[2],(Integer)previous[3]);restoreText(item.findViewById(android.R.id.title),(Object[])previous[4]);restoreText(item.findViewById(android.R.id.summary),(Object[])previous[5]);
            View widget=item.findViewById(android.R.id.widget_frame),arrow=noteArrow(item);
            if(widget!=null&&previous[6]!=null)widget.setVisibility((Integer)previous[6]);if(arrow!=null&&previous[7]!=null)arrow.setVisibility((Integer)previous[7]);
        }
        if(!isNote(preference))return;
        View widget=item.findViewById(android.R.id.widget_frame),arrow=noteArrow(item);
        XposedHelpers.setAdditionalInstanceField(item,STYLE,new Object[]{item.getPaddingStart(),item.getPaddingTop(),item.getPaddingEnd(),item.getPaddingBottom(),textStyle(item.findViewById(android.R.id.title)),textStyle(item.findViewById(android.R.id.summary)),widget==null?null:widget.getVisibility(),arrow==null?null:arrow.getVisibility()});
    }
    private static void bindNote(Object preference,Object holder){
        if(!isNote(preference))return;
        View item=(View)XposedHelpers.getObjectField(holder,"itemView");
        item.setOnClickListener(null);item.setClickable(false);item.setFocusable(false);item.setLongClickable(false);
        int padding=Math.round(16*item.getResources().getDisplayMetrics().density);
        item.setPaddingRelative(item.getPaddingStart(),padding,item.getPaddingEnd(),padding);
        TextView title=item.findViewById(android.R.id.title),summary=item.findViewById(android.R.id.summary);
        if(title!=null){
            title.setTextSize(18);title.setSingleLine(false);title.setMaxLines(Integer.MAX_VALUE);title.setEllipsize(null);
            TypedArray colors=item.getContext().obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
            try{if(colors.getColorStateList(0)!=null)title.setTextColor(colors.getColorStateList(0));}finally{colors.recycle();}
        }
        if(summary!=null){summary.setTextSize(14);summary.setSingleLine(false);summary.setMaxLines(Integer.MAX_VALUE);summary.setEllipsize(null);summary.setLineSpacing(Math.round(4*item.getResources().getDisplayMetrics().density),1f);}
        View widget=item.findViewById(android.R.id.widget_frame);if(widget!=null)widget.setVisibility(View.GONE);
        View arrow=noteArrow(item);if(arrow!=null)arrow.setVisibility(View.GONE);
    }
}
