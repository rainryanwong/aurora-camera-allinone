package com.aurora.camera.compat.street;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

/** Target-native camera controls in Phoenix's Legend layout. No duplicate capture controller. */
public final class LegendStreet217 {
    private final Object owner;
    private final Context context;
    private final android.content.res.Resources module;
    private final View oldFocus,oldEv,oldFilter,oldCv;
    private final ImageView filter,cv,evIcon,focusIcon;
    private final TextView ev,focusTitle,focusValue;
    private final Method config,component,value,auto,icon;
    private final Class<?> focusType,evType;
    private final Field cvComponent;
    private final float density;
    private final java.util.List<Placement> placements=new java.util.ArrayList<>();
    private final java.util.List<View> added=new java.util.ArrayList<>();
    private final View originalTop,originalBottom,originalBackground;
    private final int topVisibility,bottomVisibility,backgroundVisibility;
    private final LegendBackground217 background;
    private final LegendPreview219.Listener previewListener=this::preview;
    private final Drawable closeDrawable;private final ImageView closeView;
    private Rect lastPreview;private int lastHeight;
    private static final class Placement {
        final View view;final ViewGroup parent;final int index;final ViewGroup.LayoutParams layout;final Drawable background;final float rotation;
        Placement(View v){view=v;parent=(ViewGroup)v.getParent();index=parent.indexOfChild(v);layout=v.getLayoutParams();background=v.getBackground();rotation=v.getRotation();}
        void restore(){((ViewGroup)view.getParent()).removeView(view);parent.addView(view,index,layout);view.setBackground(background);view.setRotation(rotation);}
    }
    public LegendStreet217(Host h,Object owner,View root)throws Exception {
        this.owner=owner;context=root.getContext();module=android.content.res.XModuleResources.createInstance(h.moduleApkPath,null);module.updateConfiguration(context.getResources().getConfiguration(),context.getResources().getDisplayMetrics());density=context.getResources().getDisplayMetrics().density;
        config=h.method("g2.a","a");component=h.method("Xh.b","x",Class.class);value=h.method("com.android.camera.data.data.c","getComponentValue",int.class);
        focusType=h.type("r2.b0");evType=h.type("r2.F0");auto=h.method(focusType,"isSwitchOn",int.class);icon=h.method("r2.m","o");cvComponent=h.field("q4.h","t");
        oldFocus=(View)h.field("q4.u","Q").get(owner);oldEv=(View)h.field("q4.u","R").get(owner);oldFilter=(View)h.field("q4.u","S").get(owner);oldCv=(View)h.field("q4.u","T").get(owner);
        FrameLayout container=(FrameLayout)root.findViewById(0x7f0b03b8);
        View menu=root.findViewById(0x7f0b03c6),close=root.findViewById(0x7f0b03bd),thumbnail=root.findViewById(0x7f0b03bc),shutter=root.findViewById(0x7f0b03b7);
        originalTop=(View)menu.getParent();originalBottom=root.findViewById(0x7f0b03b3);topVisibility=originalTop.getVisibility();bottomVisibility=originalBottom.getVisibility();originalTop.setVisibility(View.GONE);originalBottom.setVisibility(View.GONE);originalBottom.setId(View.NO_ID);
        for(View view:new View[]{menu,close,thumbnail,shutter})placements.add(new Placement(view));
        closeView=(ImageView)close;closeDrawable=closeView.getDrawable();
        detach(menu);detach(close);detach(thumbnail);detach(shutter);
        // The existing controller keeps its native shutter/thumbnail/close/menu objects.
        RelativeLayout top=new RelativeLayout(context);top.setPadding(dp(17),dp(31.599976f),dp(17),0);container.addView(top,new FrameLayout.LayoutParams(-1,dp(96)));added.add(top);
        style(close);style(menu);thumbnail.setBackground(drawable("equipstreet_thumbnail_bg_new"));
        RelativeLayout.LayoutParams cp=relative(56,56);cp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);top.addView(close,cp);
        RelativeLayout.LayoutParams tp=relative(56,56);tp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);top.addView(thumbnail,tp);
        RelativeLayout.LayoutParams mp=relative(56,56);mp.addRule(RelativeLayout.LEFT_OF,thumbnail.getId());mp.rightMargin=dp(14);top.addView(menu,mp);
        ((ImageView)close).setImageDrawable(drawable("ic_equip_street_top_close_new"));((ImageView)thumbnail).setScaleType(ImageView.ScaleType.CENTER_CROP);
        RelativeLayout bottom=new RelativeLayout(context);bottom.setId(0x7f0b03b3);FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(103),Gravity.BOTTOM);bp.bottomMargin=dp(31);container.addView(bottom,bp);added.add(bottom);
        RelativeLayout.LayoutParams sp=relative(103,103);sp.addRule(RelativeLayout.CENTER_IN_PARENT);bottom.addView(shutter,sp);
        filter=image("ic_new_effect_button_normal","滤镜");filter.setId(View.generateViewId());RelativeLayout.LayoutParams fp=centerRelative(56,56);fp.addRule(RelativeLayout.LEFT_OF,shutter.getId());fp.rightMargin=dp(7);bottom.addView(filter,fp);filter.setOnClickListener(v->click(oldFilter));
        cv=image("ic_equip_street_top_close_new","徕卡风格");cv.setId(View.generateViewId());RelativeLayout.LayoutParams vp=centerRelative(56,56);vp.addRule(RelativeLayout.RIGHT_OF,shutter.getId());vp.leftMargin=dp(7);bottom.addView(cv,vp);cv.setOnClickListener(v->click(oldCv));
        FrameLayout evCell=new FrameLayout(context);style(evCell);RelativeLayout.LayoutParams ep=centerRelative(56,56);ep.addRule(RelativeLayout.LEFT_OF,filter.getId());ep.rightMargin=dp(13);bottom.addView(evCell,ep);evCell.setOnClickListener(v->click(oldEv));
        evIcon=imageContent("equip_street_top_ev_icon","曝光补偿");ev=new TextView(context);ev.setTextColor(-1);ev.setTextSize(16);ev.setGravity(Gravity.CENTER);evCell.addView(evIcon,new FrameLayout.LayoutParams(-1,-1));evCell.addView(ev,new FrameLayout.LayoutParams(-1,-1));
        FrameLayout focusCell=new FrameLayout(context);style(focusCell);RelativeLayout.LayoutParams fcp=centerRelative(56,56);fcp.addRule(RelativeLayout.RIGHT_OF,cv.getId());fcp.leftMargin=dp(13);bottom.addView(focusCell,fcp);focusCell.setOnClickListener(v->click(oldFocus));
        focusIcon=imageContent("ic_equip_street_focus_icon_new","自动对焦");focusCell.addView(focusIcon,new FrameLayout.LayoutParams(-1,-1));LinearLayout texts=new LinearLayout(context);texts.setOrientation(LinearLayout.VERTICAL);texts.setGravity(Gravity.CENTER);focusTitle=text("MF",13);focusValue=text("",13);texts.addView(focusTitle);texts.addView(focusValue);focusCell.addView(texts,new FrameLayout.LayoutParams(-1,-1));
        originalBackground=root.findViewById(0x7f0b03b2);backgroundVisibility=originalBackground.getVisibility();originalBackground.setVisibility(View.GONE);background=new LegendBackground217(context);container.addView(background,0,new FrameLayout.LayoutParams(-1,-1));added.add(background);
        lastPreview=new Rect((Rect)h.field("com.android.camera.ui.EquipStreetBgView","n").get(originalBackground));lastHeight=context.getResources().getDisplayMetrics().heightPixels;background.preview(lastPreview);
        LegendPreview219.attach(originalBackground,previewListener);
        preview(lastHeight,lastPreview);
        update();
    }
    public void restore()throws Exception {
        for(Placement p:placements)p.restore();
        for(View view:added)((ViewGroup)view.getParent()).removeView(view);
        originalTop.setVisibility(topVisibility);originalBottom.setId(0x7f0b03b3);originalBottom.setVisibility(bottomVisibility);originalBackground.setVisibility(backgroundVisibility);closeView.setImageDrawable(closeDrawable);
        LegendPreview219.detach(originalBackground,previewListener);
    }
    private void preview(int height,Rect rect){lastHeight=height;lastPreview=new Rect(rect);background.preview(lastPreview);background.setOnTouchListener((v,event)->event.getAction()==android.view.MotionEvent.ACTION_DOWN&&!lastPreview.contains((int)event.getX(),(int)event.getY()));}
    public void update()throws Exception {
        Object repo=config.invoke(null),focus=component.invoke(repo,focusType),exposure=component.invoke(repo,evType);
        boolean af=(Boolean)auto.invoke(focus,229);String distance=(String)value.invoke(focus,229);focusIcon.setVisibility(af?View.VISIBLE:View.GONE);focusTitle.setVisibility(af?View.GONE:View.VISIBLE);focusValue.setVisibility(af?View.GONE:View.VISIBLE);if(!af)focusValue.setText(distance+"m");
        float number=Float.parseFloat((String)value.invoke(exposure,229));boolean zero=number==0;evIcon.setVisibility(zero?View.VISIBLE:View.GONE);ev.setVisibility(zero?View.GONE:View.VISIBLE);if(!zero)ev.setText(String.format(Locale.ROOT,number>0?"+%.1f":"%.1f",number));
        int drawable=(Integer)icon.invoke(cvComponent.get(owner));if(drawable>0)cv.setImageResource(drawable);
        filter.setContentDescription(((TextView)oldFilter).getText());cv.setContentDescription("摄影风格 "+((TextView)findCvLabel()).getText());
    }
    private View findCvLabel(){return oldCv.findViewById(0x7f0b03b9);}
    private void click(View original){((View.OnClickListener)owner).onClick(original);}
    private void detach(View view){((ViewGroup)view.getParent()).removeView(view);}
    private int dp(float n){return Math.round(n*density);}
    private Drawable drawable(String name){int id=module.getIdentifier("street217_"+name,"drawable","com.aurora.camera.compat");if(id==0)throw new IllegalStateException("Missing Legend drawable "+name);return module.getDrawable(id,null);}
    private void style(View view){view.setBackground(drawable("bg_equip_street_top_menu_new"));view.setRotation(90);}
    private ImageView image(String name,String description){ImageView image=imageContent(name,description);style(image);return image;}
    private ImageView imageContent(String name,String description){ImageView image=new ImageView(context);image.setImageDrawable(drawable(name));image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);image.setContentDescription(description);return image;}
    private TextView text(String text,int size){TextView view=new TextView(context);view.setText(text);view.setTextSize(size);view.setTextColor(-1);view.setGravity(Gravity.CENTER);return view;}
    private RelativeLayout.LayoutParams relative(int w,int h){return new RelativeLayout.LayoutParams(dp(w),dp(h));}
    private RelativeLayout.LayoutParams centerRelative(int w,int h){RelativeLayout.LayoutParams p=relative(w,h);p.addRule(RelativeLayout.CENTER_VERTICAL);return p;}
}
