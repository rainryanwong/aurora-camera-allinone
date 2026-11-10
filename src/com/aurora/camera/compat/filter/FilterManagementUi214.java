package com.aurora.camera.compat.filter;

import android.content.ClipData;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** Phoenix card/drag/pin layout, adapted from the retained F13 UI only. */
public final class FilterManagementUi214 extends LinearLayout {
    public interface Controller {
        int count();int id(int position);String name(int id);boolean fixed(int id);boolean enabled(int id);boolean first(int id);
        String status();void search(String text);void toggle(int id,boolean enabled);void pin(int id);void moveBefore(int id,int target);
        void moveMenu(int id);void selectAll();void invert();void backup();void restore();void reset();void close();
    }
    private final Controller controller;
    private final Constructor<?> sliding;
    private final int foreground,card,muted,background,accent=0xff3482ff;
    private final ListView list;
    private final TextView info,empty;
    private final BaseAdapter adapter;
    private final Object dragOwner=new Object();
    private static final class DragToken {final Object owner;final int id;DragToken(Object owner,int id){this.owner=owner;this.id=id;}}
    public FilterManagementUi214(Context context,Controller controller,Constructor<?> sliding){
        super(context);this.controller=controller;this.sliding=sliding;
        boolean dark=(getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        foreground=dark?0xffeeeeee:0xff111111;card=dark?0xff242424:Color.WHITE;background=dark?0xff101010:0xfff5f5f5;muted=dark?0xffaaaaaa:0xff858585;
        setOrientation(VERTICAL);setBackgroundColor(background);setFitsSystemWindows(true);setPadding(dp(20),0,dp(20),dp(12));
        LinearLayout navigation=new LinearLayout(context);navigation.setGravity(Gravity.CENTER_VERTICAL);
        Icon back=new Icon(context,3,foreground);back.setPadding(dp(11),dp(11),dp(11),dp(11));back.setContentDescription("返回");back.setOnClickListener(v->controller.close());
        navigation.addView(back,new LayoutParams(dp(48),dp(48)));View spacer=new View(context);navigation.addView(spacer,new LayoutParams(0,1,1));
        TextView done=label("完成",16,accent);done.setGravity(Gravity.CENTER);done.setOnClickListener(v->controller.close());navigation.addView(done,new LayoutParams(dp(60),dp(48)));
        addView(navigation,new LayoutParams(-1,dp(56)));
        TextView title=label("滤镜自定义",32,foreground);title.setPadding(dp(4),dp(8),0,dp(18));title.setAccessibilityHeading(true);addView(title,new LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(context);actions.setGravity(Gravity.CENTER);actions.setBackground(round(card,20));
        action(actions,"恢复备份",0,()->controller.restore());action(actions,"创建备份",1,()->controller.backup());action(actions,"重置排序与显示",2,()->controller.reset());
        addView(actions,new LayoutParams(-1,dp(88)));
        LinearLayout tools=new LinearLayout(context);tools.setOrientation(VERTICAL);tools.setPadding(dp(14),dp(2),dp(14),dp(2));tools.setBackground(round(card,20));
        EditText query=new EditText(context);query.setSingleLine(true);query.setTextSize(15);query.setTextColor(foreground);query.setHintTextColor(muted);query.setHint("搜索滤镜名称或编号");query.setContentDescription("搜索滤镜名称或编号");
        query.setBackgroundColor(Color.TRANSPARENT);query.setPadding(0,0,0,0);tools.addView(query,new LayoutParams(-1,dp(48)));
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){controller.search(s.toString());}public void afterTextChanged(Editable e){}});
        LinearLayout batch=new LinearLayout(context);batch.setGravity(Gravity.CENTER_VERTICAL);
        info=label("",12,muted);info.setMaxLines(2);batch.addView(info,new LayoutParams(0,dp(40),1));
        textAction(batch,"全选",()->controller.selectAll());textAction(batch,"反选",()->controller.invert());tools.addView(batch,new LayoutParams(-1,-2));
        LayoutParams toolsLayout=new LayoutParams(-1,-2);toolsLayout.topMargin=dp(12);addView(tools,toolsLayout);
        LinearLayout listCard=new LinearLayout(context);listCard.setOrientation(VERTICAL);listCard.setBackground(round(card,20));listCard.setClipToOutline(true);
        LayoutParams listLayout=new LayoutParams(-1,0,1);listLayout.topMargin=dp(12);addView(listCard,listLayout);
        list=new ListView(context);list.setDivider(null);list.setCacheColorHint(Color.TRANSPARENT);list.setVerticalScrollBarEnabled(true);list.setClipToPadding(false);list.setPadding(0,dp(6),0,dp(6));
        empty=label("没有匹配的滤镜",16,muted);empty.setGravity(Gravity.CENTER);listCard.addView(empty,new LayoutParams(-1,dp(100)));listCard.addView(list,new LayoutParams(-1,-1));list.setEmptyView(empty);
        adapter=new BaseAdapter(){
            public int getCount(){return controller.count();}public Object getItem(int position){return controller.id(position);}public long getItemId(int position){return controller.id(position);}public boolean hasStableIds(){return true;}
            public View getView(int position,View recycled,ViewGroup parent){
                Holder holder=recycled!=null?(Holder)recycled.getTag():new Holder();holder.bind(controller.id(position));return holder.row;
            }
        };list.setAdapter(adapter);
        list.setOnDragListener((v,event)->{
            if(!owns(event))return false;
            if(event.getAction()==DragEvent.ACTION_DRAG_LOCATION){int y=(int)event.getY();if(y<dp(60))list.smoothScrollBy(-dp(24),60);else if(y>list.getHeight()-dp(60))list.smoothScrollBy(dp(24),60);}
            return event.getAction()!=DragEvent.ACTION_DROP;
        });
        TextView help=label("长按左侧手柄拖动排序 · 全选、反选作用于搜索结果",12,muted);help.setGravity(Gravity.CENTER);help.setPadding(0,dp(8),0,0);addView(help,new LayoutParams(-1,-2));
    }
    public int backgroundColor(){return background;}
    public void refresh(){info.setText(controller.status());adapter.notifyDataSetChanged();}
    public void revealTop(){list.setSelection(0);}
    private boolean owns(DragEvent e){return e.getLocalState() instanceof DragToken&&((DragToken)e.getLocalState()).owner==dragOwner;}
    private final class Holder {
        final LinearLayout row;final Grip grip;final TextView title,pin;final CompoundButton toggle;int id;
        Holder(){
            row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(6),0,dp(14),0);row.setMinimumHeight(dp(68));row.setTag(this);
            grip=new Grip(getContext(),muted);row.addView(grip,new LayoutParams(dp(40),dp(68)));
            title=label("",16,foreground);title.setMaxLines(2);title.setPadding(0,dp(8),dp(6),dp(8));row.addView(title,new LayoutParams(0,-2,1));
            pin=label("置顶",13,accent);pin.setGravity(Gravity.CENTER);row.addView(pin,new LayoutParams(dp(48),dp(48)));
            try{toggle=(CompoundButton)sliding.newInstance(getContext(),null);}catch(ReflectiveOperationException failure){throw new IllegalStateException("Native MIUI SlidingButton construction failed",failure instanceof InvocationTargetException?((InvocationTargetException)failure).getCause():failure);}
            LayoutParams switchLayout=new LayoutParams(-2,-2);switchLayout.leftMargin=dp(6);row.addView(toggle,switchLayout);
            pin.setOnClickListener(v->controller.pin(id));title.setOnClickListener(v->{if(!controller.fixed(id))controller.moveMenu(id);});
            grip.setOnLongClickListener(v->{if(controller.fixed(id))return false;return v.startDragAndDrop(ClipData.newPlainText("filter-order",""),new View.DragShadowBuilder(row),new DragToken(dragOwner,id),0);});
            row.setOnDragListener((v,event)->{
                if(!owns(event)||controller.fixed(id))return false;
                switch(event.getAction()){
                    case DragEvent.ACTION_DRAG_ENTERED:row.setBackgroundColor(0x163482ff);return true;
                    case DragEvent.ACTION_DRAG_EXITED:case DragEvent.ACTION_DRAG_ENDED:row.setBackgroundColor(Color.TRANSPARENT);return true;
                    case DragEvent.ACTION_DROP:row.setBackgroundColor(Color.TRANSPARENT);controller.moveBefore(((DragToken)event.getLocalState()).id,id);return true;
                    default:return true;
                }
            });
        }
        void bind(int id){
            this.id=id;String name=controller.name(id);boolean fixed=controller.fixed(id),first=controller.first(id);
            row.setBackgroundColor(Color.TRANSPARENT);title.setText(name);grip.setAlpha(fixed?0.3f:1f);grip.setContentDescription(fixed?"原图固定置顶":"拖动排序 "+name);
            pin.setEnabled(!fixed&&!first);pin.setText(first?"已置顶":"置顶");pin.setTextColor(fixed||first?muted:accent);pin.setContentDescription(fixed?"原图固定置顶":first?name+" 已在最前":"置顶 "+name);
            // ListView recycling must not persist a previous row's listener/value.
            toggle.setOnCheckedChangeListener(null);toggle.setChecked(controller.enabled(id));toggle.setEnabled(!fixed);toggle.setContentDescription("显示 "+name);
            toggle.setOnCheckedChangeListener((button,checked)->controller.toggle(this.id,checked));
        }
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String text,float size,int color){TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setTextColor(color);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private void textAction(LinearLayout parent,String text,Runnable action){TextView v=label(text,14,accent);v.setGravity(Gravity.CENTER);v.setOnClickListener(w->action.run());parent.addView(v,new LayoutParams(dp(48),dp(40)));}
    private void action(LinearLayout parent,String text,int icon,Runnable action){LinearLayout cell=new LinearLayout(getContext());cell.setGravity(Gravity.CENTER);cell.setOrientation(VERTICAL);Icon image=new Icon(getContext(),icon,foreground);cell.addView(image,new LayoutParams(dp(26),dp(26)));TextView label=label(text,13,foreground);label.setGravity(Gravity.CENTER);label.setMaxLines(2);LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(9);cell.addView(label,lp);cell.setContentDescription(text);cell.setOnClickListener(v->action.run());parent.addView(cell,new LayoutParams(0,-1,1));}
    private static final class Grip extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);Grip(Context c,int color){super(c);paint.setColor(color);}
        @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density;for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x+=2)c.drawCircle(getWidth()/2f+x*3*d,getHeight()/2f+y*6*d,1.4f*d,paint);}
    }
    private static final class Icon extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final int type;
        Icon(Context c,int type,int color){super(c);this.type=type;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override protected void onDraw(Canvas c){c.save();c.translate(getPaddingLeft(),getPaddingTop());c.scale((getWidth()-getPaddingLeft()-getPaddingRight())/26f,(getHeight()-getPaddingTop()-getPaddingBottom())/26f);
            if(type==3){c.drawLine(16,4,7,13,paint);c.drawLine(7,13,16,22,paint);}
            else if(type==2){c.drawArc(3.5f,3.5f,22.5f,22.5f,-100,145,false,paint);c.drawArc(3.5f,3.5f,22.5f,22.5f,80,145,false,paint);arrow(c,45);arrow(c,225);}
            else {c.drawLine(4,16,4,23,paint);c.drawLine(4,23,22,23,paint);c.drawLine(22,23,22,16,paint);float a=type==0?16:3,b=type==0?3:16;c.drawLine(13,b,13,a,paint);float direction=type==0?-1:1;c.drawLine(13,a,8,a+direction*5,paint);c.drawLine(13,a,18,a+direction*5,paint);}c.restore();}
        void arrow(Canvas c,float degrees){double a=Math.toRadians(degrees);float x=13+9.5f*(float)Math.cos(a),y=13+9.5f*(float)Math.sin(a),tx=-(float)Math.sin(a),ty=(float)Math.cos(a);Path p=new Path();p.moveTo(x+tx,y+ty);p.lineTo(x-3.4f*tx-2.2f*ty,y-3.4f*ty+2.2f*tx);p.lineTo(x-3.4f*tx+2.2f*ty,y-3.4f*ty-2.2f*tx);p.close();paint.setStyle(Paint.Style.FILL);c.drawPath(p,paint);paint.setStyle(Paint.Style.STROKE);}
    }
}
