package com.aurora.camera.compat.filter;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.*;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import com.aurora.camera.compat.*;
import com.aurora.camera.compat.ui.*;
import de.robv.android.xposed.*;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Phoenix import/effects page, using the existing opaque-page and MIUIX contracts. */
public final class LutPage229 {
    private final Host host;private final LutRuntime229 runtime;private final MiuixDialogs dialogs;private final Options229 options;
    private final Map<Activity,WeakReference<Page>> pages=Collections.synchronizedMap(new WeakHashMap<>());
    private final Method settings,destroy;
    private final Consumer<Object> importFilters;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor(task->{Thread thread=new Thread(task,"BundledFilterRestore");thread.setDaemon(true);return thread;});
    private final int progressAppearance;
    public LutPage229(Host h,LutRuntime229 runtime,Consumer<Object> importFilters)throws ReflectiveOperationException{
        host=h;this.runtime=runtime;options=new Options229(h);dialogs=new MiuixDialogs(h);
        this.importFilters=importFilters;
        progressAppearance=h.field("tx.a$l","Miuix_AppCompat_TextAppearance_Medium").getInt(null);
        settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        destroy=h.method("androidx.fragment.app.m","onDestroy");
    }
    private Page page(Activity owner){WeakReference<Page> ref=pages.get(owner);return ref==null?null:ref.get();}
    public void install(){
        XposedBridge.hookMethod(settings,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())options.addAction(p.thisObject,"aurora_lut_import_effects229","滤镜导入与效果","导入滤镜，设置每款滤镜的颗粒和柔光","pref_aurora_filter_manager",()->{
            Activity owner=(Activity)XposedHelpers.callMethod(p.thisObject,"requireActivity");Page page=page(owner);if(page==null){page=new Page(owner,p.thisObject);pages.put(owner,new WeakReference<>(page));}page.show();
        });}});
        XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void before(MethodHookParam p){Page page=page((Activity)p.thisObject);pages.remove((Activity)p.thisObject);if(page!=null){page.dismissProgress();page.dialog.dismiss();}}});
    }
    private final class Page {
        final Activity owner;final Object fragment;final Dialog dialog;final LinearLayout root,body;final boolean dark;final int foreground,background,muted,card;
        List<LutRuntime229.Entry> entries=Collections.emptyList();LinearLayout list;String query="";
        Dialog progressDialog;ProgressBar progressBar;TextView progressText;boolean restoring;
        Page(Activity owner,Object fragment){
            this.owner=owner;this.fragment=fragment;dark=(owner.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
            foreground=dark?0xffeeeeee:0xff111111;background=dark?0xff101010:0xfff5f5f5;muted=dark?0xffaaaaaa:0xff858585;card=dark?0xff242424:0xffffffff;
            dialog=new Dialog(owner,android.R.style.Theme_DeviceDefault_NoActionBar);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            root=new LinearLayout(owner);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background);
            TextView back=text("‹",38,foreground);back.setPadding(dp(22),0,0,0);back.setGravity(Gravity.CENTER_VERTICAL);root.addView(back,new LinearLayout.LayoutParams(-1,dp(58)));back.setOnClickListener(v->dialog.dismiss());
            ScrollView scroll=new ScrollView(owner);body=new LinearLayout(owner);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(12),dp(4),dp(12),dp(24));scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=insets.getInsets(WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));return insets;});
            dialog.setContentView(root);dialog.setOnDismissListener(d->pages.remove(owner));
        }
        int dp(int n){return Math.round(n*owner.getResources().getDisplayMetrics().density);}
        TextView text(String value,int size,int color){TextView v=new TextView(owner);v.setText(value);v.setTextSize(size);v.setTextColor(color);return v;}
        LinearLayout card(){LinearLayout v=new LinearLayout(owner);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(16),dp(16),dp(16),dp(16));GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(24));v.setBackground(shape);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(12);body.addView(v,p);return v;}
        LinearLayout item(LinearLayout parent,String title,String summary,int height){
            LinearLayout row=new LinearLayout(owner);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(height));
            LinearLayout labels=new LinearLayout(owner);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(0,dp(10),dp(8),dp(10));labels.addView(text(title,18,foreground));labels.addView(text(summary,14,muted));row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
            TextView arrow=text("›",26,muted);arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(20),-2));parent.addView(row,new LinearLayout.LayoutParams(-1,-2));return row;
        }
        void restoreAction(LinearLayout parent){
            Object preference=XposedHelpers.newInstance(hostType("miuix.preference.TextPreference"),owner,null);
            XposedHelpers.callMethod(preference,"a0","aurora_restore_bundled_filters257");XposedHelpers.callMethod(preference,"e0","恢复模块内置所有滤镜");XposedHelpers.callMethod(preference,"c0","补回已删除的内置滤镜，并恢复默认状态");XposedHelpers.setBooleanField(preference,"t",false);
            LayoutInflater inflater=LayoutInflater.from(owner);View row=inflater.inflate((Integer)XposedHelpers.getObjectField(preference,"V"),parent,false);
            ViewGroup widget=row.findViewById(android.R.id.widget_frame);int widgetLayout=(Integer)XposedHelpers.getObjectField(preference,"W");
            if(widget!=null&&widgetLayout!=0)inflater.inflate(widgetLayout,widget);
            XposedHelpers.callMethod(preference,"D",XposedHelpers.newInstance(hostType("androidx.preference.l"),row));
            row.setOnClickListener(view->confirmRestore());parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        void show(){if(!render())return;dialog.show();FullScreenPage220.apply(dialog.getWindow(),root,background,dark);}
        boolean render(){
            try{entries=runtime.entries();}catch(ReflectiveOperationException|IllegalStateException failure){fail(failure);return false;}
            body.removeAllViews();TextView title=text("滤镜导入与效果",32,foreground);title.setPadding(dp(16),dp(8),0,dp(18));body.addView(title);
            LinearLayout note=card();note.addView(text("Rec.709 3D LUT",18,foreground));note.addView(text("支持 16、17、32、33、64、65 阶 .cube；DOMAIN 必须为 0–1。不支持 1D shaper。导入后在本机转换为相机原生 LUT。",14,muted));
            LinearLayout action=card();action.setPadding(dp(16),dp(6),dp(16),dp(6));item(action,"导入滤镜","支持单个滤镜和 ZIP 整合包，整合包可选择要导入的滤镜",0).setOnClickListener(v->{dialog.dismiss();importFilters.accept(fragment);});
            LinearLayout restore=card();restore.setPadding(0,0,0,0);restoreAction(restore);
            // Same field, card padding and live substring search as filter management.
            LinearLayout tools=card();tools.setPadding(dp(14),dp(2),dp(14),dp(2));GradientDrawable shape=new GradientDrawable();shape.setColor(card);shape.setCornerRadius(dp(20));tools.setBackground(shape);
            EditText search=new EditText(owner);search.setSingleLine(true);search.setTextSize(15);search.setTextColor(foreground);search.setHintTextColor(muted);search.setHint("搜索滤镜名称");search.setContentDescription("搜索滤镜名称");search.setBackgroundColor(Color.TRANSPARENT);search.setPadding(0,0,0,0);search.setText(query);search.setSelection(search.length());tools.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
            TextView heading=text("非徕卡滤镜",13,muted);heading.setPadding(dp(16),dp(8),0,dp(10));body.addView(heading);list=card();list.setPadding(dp(16),0,dp(16),0);
            search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();renderList();}public void afterTextChanged(Editable e){}});
            renderList();return true;
        }
        void confirmRestore(){
            if(restoring)return;
            dialogs.builder(owner).setTitle("恢复模块内置所有滤镜？").setMessage("补回已删除的内置滤镜，并恢复内置滤镜的默认名称、启用状态、颗粒和柔光。您自行导入的滤镜会保留。").setNegativeButton("取消",null).setPositiveButton("恢复",(dialog,which)->restoreBundled()).show();
        }
        void restoreBundled(){
            if(restoring)return;restoring=true;
            LinearLayout content=new LinearLayout(owner);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(24),dp(12),dp(24),dp(12));
            progressBar=(ProgressBar)XposedHelpers.newInstance(hostType("miuix.androidbasewidget.widget.ProgressBar"),owner,null);progressBar.setIndeterminate(true);content.addView(progressBar,new LinearLayout.LayoutParams(-1,-2));
            progressText=new TextView(owner);progressText.setTextAppearance(progressAppearance);progressText.setText("正在读取内置滤镜…");content.addView(progressText,new LinearLayout.LayoutParams(-1,-2));
            progressDialog=dialogs.builder(owner).setTitle("恢复模块内置所有滤镜").setView(content).create();progressDialog.setCancelable(false);progressDialog.show();
            io.execute(()->{try{
                int restored=runtime.restoreBundled((done,total,name)->main.post(()->{if(progressDialog==null)return;progressBar.setIndeterminate(total<=0);if(total>0){progressBar.setMax(total);progressBar.setProgress(done);}progressText.setText(done+" / "+total+"\n"+name);}));
                main.post(()->{restoring=false;dismissProgress();try{runtime.showRestoredBundled();if(owner.isFinishing()||owner.isDestroyed())return;entries=runtime.entries();renderList();dialogs.builder(owner).setTitle("恢复完成").setMessage("已恢复 "+restored+" 项内置滤镜").setPositiveButton("确定",null).show();}catch(java.io.IOException|ReflectiveOperationException|RuntimeException failure){fail(failure);}});
            }catch(Exception failure){main.post(()->{restoring=false;dismissProgress();if(!owner.isFinishing()&&!owner.isDestroyed())fail(failure);});}});
        }
        void dismissProgress(){if(progressDialog!=null){progressDialog.dismiss();progressDialog=null;}}
        void renderList(){
            list.removeAllViews();String wanted=query.trim().toLowerCase(Locale.ROOT);int count=0;
            for(LutRuntime229.Entry entry:entries){
                if(!entry.name.toLowerCase(Locale.ROOT).contains(wanted))continue;count++;
                LinearLayout row=item(list,entry.name,LutStore229.summary(runtime.effects(entry)),72);row.setOnClickListener(v->edit(entry));
                if(entry.slot>0)row.setOnLongClickListener(v->{manage(entry);return true;});
            }
            if(count==0){TextView empty=text("没有匹配的滤镜",16,muted);empty.setGravity(Gravity.CENTER);list.addView(empty,new LinearLayout.LayoutParams(-1,dp(100)));}
        }
        void edit(LutRuntime229.Entry entry){
            int[] saved=runtime.store().effects(entry.key),initial=runtime.effects(entry);int[] grain={initial[0]},soft={initial[1]};
            LinearLayout controls=new LinearLayout(owner);controls.setPadding(dp(24),dp(8),dp(24),dp(4));controls.setOrientation(LinearLayout.VERTICAL);
            EditText name=(EditText)XposedHelpers.newInstance(hostType("miuix.androidbasewidget.widget.StateEditText"),owner,null);name.setSingleLine(true);name.setSelectAllOnFocus(true);name.setText(entry.name);name.setTextSize(16);
            controls.addView(section("名称"));controls.addView(name,new LinearLayout.LayoutParams(-1,-2));controls.addView(section("成像效果"));
            option(controls,"颗粒",new String[]{"无","轻","中","强"},grain);option(controls,"柔光",new String[]{"无","黑柔","白柔"},soft);
            TextView hint=text("颗粒与柔光为独立渲染阶段，可同时启用。",13,muted);hint.setPadding(0,dp(10),0,dp(8));controls.addView(hint);
            ScrollView scroll=new ScrollView(owner);scroll.setFillViewport(true);scroll.addView(controls);
            dialogs.builder(owner).setTitle("编辑滤镜").setView(scroll).setPositiveButton("保存",(d,n)->{try{runtime.store().saveEdits(entry.key,entry.slot,name.getText().toString(),grain[0],soft[0],saved!=null||grain[0]!=initial[0]||soft[0]!=initial[1]);runtime.refresh();render();}catch(Exception e){fail(e);}}).show();
        }
        Class<?> hostType(String name){try{return host.type(name);}catch(ClassNotFoundException e){throw new IllegalStateException(e);}}
        TextView section(String title){TextView view=text(title,13,muted);view.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);view.setPadding(0,dp(18),0,dp(8));return view;}
        void option(LinearLayout parent,String title,String[] names,int[] selected){
            Spinner selector=(Spinner)XposedHelpers.newInstance(hostType("miuix.appcompat.widget.Spinner"),owner,null);selector.setPrompt(title);
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(owner,android.R.layout.simple_spinner_item,android.R.id.text1,names){
                @Override public View getView(int position,View convert,ViewGroup group){TextView view=(TextView)super.getView(position,convert,group);view.setTextSize(16);view.setTextColor(muted);view.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);return view;}
                @Override public View getDropDownView(int position,View convert,ViewGroup group){
                    CheckedTextView view=convert instanceof CheckedTextView?(CheckedTextView)convert:new CheckedTextView(owner,null,0);
                    view.setId(android.R.id.text1);view.setText(names[position]);view.setTextSize(16);view.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);view.setMinHeight(dp(48));view.setPadding(dp(24),0,dp(24),0);
                    view.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_checked},{}},new int[]{0xff3482ff,foreground}));
                    view.setCheckMarkDrawable(new SelectionMark(dp(20),0xff3482ff));view.setChecked(position==selected[0]);return view;
                }
            };adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);selector.setAdapter(adapter);selector.setSelection(selected[0]);
            selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onNothingSelected(AdapterView<?> parent){}public void onItemSelected(AdapterView<?> parent,View view,int position,long id){selected[0]=position;}});
            LinearLayout row=new LinearLayout(owner);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(4),0,dp(4));TextView label=text(title,16,foreground);row.addView(label,new LinearLayout.LayoutParams(0,dp(52),1));row.addView(selector,new LinearLayout.LayoutParams(dp(152),dp(52)));parent.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        void manage(LutRuntime229.Entry entry){dialogs.builder(owner).setTitle(entry.name).setItems(new String[]{"重命名","删除"},(d,n)->{
            if(n==0){EditText edit=new EditText(owner);edit.setSingleLine(true);edit.setText(entry.name);dialogs.builder(owner).setTitle("重命名 LUT").setView(edit).setNegativeButton("取消",null).setPositiveButton("保存",(a,b)->{try{runtime.store().rename(entry.slot,edit.getText().toString());render();}catch(IOException e){fail(e);}}).show();}
            else dialogs.builder(owner).setTitle("删除 LUT").setMessage("删除“"+entry.name+"”？").setNegativeButton("取消",null).setPositiveButton("删除",(a,b)->{try{runtime.store().remove(entry.slot);runtime.refresh();render();}catch(Exception e){fail(e);}}).show();
        }).show();}
        void fail(Exception error){android.util.Log.e("AuroraLut229","LUT operation failed",error);dialogs.builder(owner).setTitle("操作未完成").setMessage(error.getMessage()).setPositiveButton("确定",null).show();}
    }
    private static final class SelectionMark extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int size;private boolean checked;
        SelectionMark(int size,int color){this.size=size;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(size*.115f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
        @Override public boolean isStateful(){return true;}
        @Override protected boolean onStateChange(int[] states){boolean next=false;for(int state:states)if(state==android.R.attr.state_checked){next=true;break;}if(next==checked)return false;checked=next;invalidateSelf();return true;}
        @Override public void draw(Canvas canvas){if(!checked)return;Rect b=getBounds();Path path=new Path();path.moveTo(b.left+b.width()*.18f,b.top+b.height()*.52f);path.lineTo(b.left+b.width()*.40f,b.top+b.height()*.73f);path.lineTo(b.left+b.width()*.82f,b.top+b.height()*.25f);canvas.drawPath(path,paint);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return size;}
        @Override public int getIntrinsicHeight(){return size;}
    }
}
