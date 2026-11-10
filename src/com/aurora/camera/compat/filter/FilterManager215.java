package com.aurora.camera.compat.filter;

import android.app.Activity;
import com.aurora.camera.compat.ui.MiuixDialogs;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import com.aurora.camera.compat.filter.FilterWorkspace215.Entry;
import de.robv.android.xposed.XposedBridge;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import static com.aurora.camera.compat.filter.FilterWorkspace215.call;
import static com.aurora.camera.compat.filter.FilterWorkspace215.read;

/** Laboratory and camera management share native workspace objects and order. */
public final class FilterManager215 {
    private static final String KEY="pref_aurora_filter_manager";
    private static final int SAVE=0x5211,LOAD=0x5212;
    private final FilterWorkspace215 workspace;
    private final LutRuntime229 runtime;
    private final PhoenixDefaults257 defaults;
    private final MiuixDialogs dialogs;
    private final Method settingsStop;private final Field externalNavigation;
    private final Method settingsAdd,activityResult,destroy,activity,find,add,title,summary,key,nativeInit,nativeResume,nativeRemove;
    private final Field group,click,persistent,prefOrder,nativeWorkspace;
    private final Constructor<?> preference,sliding;private final Class<?> clickType;
    private final Map<Activity,WeakReference<Page>> pages=Collections.synchronizedMap(new WeakHashMap<>());
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"AuroraFilterBackup");t.setDaemon(true);return t;});
    public FilterManager215(Host h,LutRuntime229 runtime)throws ReflectiveOperationException {
        this.runtime=runtime;workspace=new FilterWorkspace215(h,runtime);defaults=new PhoenixDefaults257(h,runtime);dialogs=new MiuixDialogs(h);
        settingsStop=h.method("com.android.camera.CameraPreferenceActivity","onStop");externalNavigation=h.field("com.android.camera.CameraPreferenceActivity","X");
        settingsAdd=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","addCurrentPreferences");
        activityResult=h.method("androidx.fragment.app.m","onActivityResult",int.class,int.class,Intent.class);destroy=h.method("androidx.fragment.app.m","onDestroy");activity=h.method("androidx.fragment.app.Fragment","requireActivity");
        group=h.field("com.android.camera.fragment.settings.b","mPreferenceGroup");find=h.method("androidx.preference.PreferenceGroup","k0",CharSequence.class);add=h.method("androidx.preference.PreferenceGroup","j0",h.type("androidx.preference.Preference"));
        summary=h.method("androidx.preference.Preference","c0",CharSequence.class);title=h.method("androidx.preference.Preference","e0",String.class);key=h.method("androidx.preference.Preference","a0",String.class);click=h.field("androidx.preference.Preference","f");persistent=h.field("androidx.preference.Preference","t");prefOrder=h.field("androidx.preference.Preference","g");clickType=h.type("androidx.preference.Preference$d");
        preference=h.type("miuix.preference.TextPreference").getDeclaredConstructor(Context.class,h.type("android.util.AttributeSet"));preference.setAccessible(true);
        sliding=h.type("miuix.slidingwidget.widget.SlidingButton").getDeclaredConstructor(Context.class,h.type("android.util.AttributeSet"));sliding.setAccessible(true);
        nativeInit=h.method("com.android.camera.features.mode.capture.a0","It",View.class);nativeResume=h.method("com.android.camera.features.mode.capture.a0","onResume");nativeWorkspace=h.field("U9.M","V");nativeRemove=h.method("U9.a","I",h.type("U9.S"));
    }
    public void install(){
        workspace.install();
        XposedBridge.hookMethod(settingsAdd,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;Object root=group.get(p.thisObject);if(call(find,root,KEY)!=null)return;
            Object category=call(find,root,"category_advance_setting");if(category==null)throw new IllegalStateException("Laboratory category missing");
            Activity owner=(Activity)call(activity,p.thisObject);Object row=preference.newInstance(owner,null);persistent.setBoolean(row,false);prefOrder.setInt(row,-300);call(key,row,KEY);call(title,row,"滤镜管理");call(summary,row,"排序、隐藏、备份和恢复拍照与视频滤镜");
            click.set(row,java.lang.reflect.Proxy.newProxyInstance(clickType.getClassLoader(),new Class<?>[]{clickType},(proxy,m,args)->{
                if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraCamStyleManagement";}
                openFacing(owner,workspace.frontFacing());return true;
            }));call(add,category,row);
        }});
        // Native grid keeps ALL rows so disabled presets can still be managed,
        // imported, renamed, deleted and reordered through its original actions.
        XposedBridge.hookMethod(nativeInit,new StrictHook(){
            @Override protected void before(MethodHookParam p){workspace.enter();}
            @Override protected void after(MethodHookParam p){workspace.leave();if(p.hasThrowable())return;
                ViewGroup root=(ViewGroup)p.args[0];Activity owner=(Activity)call(activity,p.thisObject);Object nativeWs=read(nativeWorkspace,p.thisObject);
                TextView button=new TextView(owner);button.setText("显示与排序");button.setTextColor(0xff3482ff);button.setTextSize(14);button.setGravity(Gravity.CENTER);button.setContentDescription("滤镜显示与排序");
                int width=Math.round(112*root.getResources().getDisplayMetrics().density),height=Math.round(48*root.getResources().getDisplayMetrics().density);
                root.addView(button,new ViewGroup.LayoutParams(width,height));
                View back=root.findViewById(0x7f0b0464);
                root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{button.setX(root.getWidth()-width);if(back!=null){int[] a=new int[2],z=new int[2];back.getLocationOnScreen(a);root.getLocationOnScreen(z);button.setY(a[1]-z[1]);}});
                button.setOnClickListener(v->open(owner,nativeWs));
            }
        });
        XposedBridge.hookMethod(nativeResume,new StrictHook(){@Override protected void before(MethodHookParam p){workspace.enter();}@Override protected void after(MethodHookParam p){workspace.leave();}});
        XposedBridge.hookMethod(activityResult,new StrictHook(){@Override protected void after(MethodHookParam p){
            Page page=page((Activity)p.thisObject);int request=(Integer)p.args[0];if(page!=null&&page.pending==request&&(request==SAVE||request==LOAD)){page.result(request,(Integer)p.args[1],(Intent)p.args[2]);}
        }});
        // CameraPreferenceActivity normally finishes when leaving for another app.
        // Preserve its native lifecycle; suppress only that finish predicate for our SAF request.
        XposedBridge.hookMethod(settingsStop,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{Page page=page((Activity)p.thisObject);if(page!=null&&page.pending!=0){p.setObjectExtra("217.filter.external",externalNavigation.getBoolean(p.thisObject));externalNavigation.setBoolean(p.thisObject,true);}}
            @Override protected void after(MethodHookParam p)throws Throwable{Object previous=p.getObjectExtra("217.filter.external");if(previous!=null)externalNavigation.setBoolean(p.thisObject,(Boolean)previous);}
        });
        XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void before(MethodHookParam p){Page page=page((Activity)p.thisObject);if(page!=null)page.dialog.dismiss();}});
    }
    private Page page(Activity owner){WeakReference<Page> ref=pages.get(owner);return ref==null?null:ref.get();}
    private void openFacing(Activity owner,boolean front){try{open(owner,workspace.workspace(front));}catch(IOException e){Log.e("AuroraFilterManager","Workspace unavailable",e);dialogs.builder(owner).setTitle("滤镜工作区尚未打开").setMessage(e.getMessage()).setPositiveButton("确定",null).show();}}
    private void open(Activity owner,Object ws){Page old=page(owner);if(old!=null&&old.ws==ws&&old.dialog.isShowing())return;try{Page page=new Page(owner,ws);pages.put(owner,new WeakReference<>(page));page.show();}catch(IOException e){Log.e("AuroraFilterManager","Open failed",e);dialogs.builder(owner).setTitle("无法打开滤镜管理").setMessage(e.getMessage()).setPositiveButton("确定",null).show();}}
    private final class Page {
        final Activity owner;final Dialog dialog;final Object ws;final VideoFilterWorkspace270 video;final List<Entry> rows=new ArrayList<>(),visible=new ArrayList<>();final Map<Integer,Entry> handles=new HashMap<>();final Map<String,Integer> ids=new HashMap<>();int nextId=1;
        final FilterManagementUi215 ui;final Runnable observer=this::onChange;
        Set<String> hidden;List<String> initial;String query="",status="";int pending;byte[] backup;
        Page(Activity owner,Object ws)throws IOException {
            this.owner=owner;this.ws=ws;video=ws==null?new VideoFilterWorkspace270(runtime,owner):null;rows.addAll(snapshot());hidden=hidden();if(video==null)initial=workspace.initial(ws,rows);
            dialog=new Dialog(owner,android.R.style.Theme_DeviceDefault_NoActionBar);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            ui=new FilterManagementUi215(owner,new FilterManagementUi215.Controller(){
                public int count(){return visible.size();}public int id(int position){return ids.get(visible.get(position).id);}public String name(int id){return handles.get(id).name;}
                public boolean fixed(int id){return handles.get(id).fixed;}public boolean enabled(int id){return Page.this.enabled(handles.get(id));}
                public boolean first(int id){for(Entry e:rows)if(!e.fixed)return e==handles.get(id);return false;}public String status(){return status;}
                public String workspace(){return video!=null?"视频滤镜":workspace.front.isInstance(ws)?"前置滤镜":"后置滤镜";}
                public void chooseWorkspace(){dialogs.builder(owner).setTitle("选择滤镜工作区").setItems(new String[]{"后置滤镜","前置滤镜","视频滤镜"},(d,n)->{dialog.dismiss();if(n==2)open(owner,null);else openFacing(owner,n==1);}).show();}
                public void search(String text){query=text.trim().toLowerCase(Locale.ROOT);refresh();}
                public void toggle(int id,boolean enabled){Entry e=handles.get(id);if(e.fixed)return;try{state(e,enabled);commit();}catch(IOException failure){reload();error(failure);}}
                public void pin(int id){Entry e=handles.get(id);if(e.fixed)return;rows.remove(e);int at=0;while(at<rows.size()&&rows.get(at).fixed)at++;rows.add(at,e);if(commit())ui.revealTop();}
                public void moveTo(int id,int target){Entry e=handles.get(id),t=handles.get(target);if(!e.fixed&&!t.fixed&&WorkspaceOrder215.moveTo(rows,e,t))commit();}
                public void moveMenu(int id){menu(handles.get(id));}
                public void selectAll(){batch(false);}public void invert(){batch(true);}public void backup(){pick(SAVE);}public void restore(){pick(LOAD);}
                public void reset(){dialogs.builder(owner).setTitle("重置排序与显示").setMessage(video!=null?"Leica Looks 紧接原图，其后按后置滤镜顺序排列模块滤镜，其他视频滤镜保留在后面，并恢复视频显示状态。":"按 Phoenix 默认顺序及开关状态排列相机自带和模块内置滤镜，自建与自行导入的滤镜保留在末尾。").setNegativeButton("取消",null).setPositiveButton("重置",(d,w)->{try{if(video!=null)video.reset();else defaults.reset(ws,owner);reload();ui.revealTop();}catch(IOException failure){error(failure);}}).show();}
                public void close(){dialog.dismiss();}
            },sliding);
            dialog.setContentView(ui);dialog.setOnDismissListener(d->{pending=0;workspace.unlisten(observer);if(page(owner)==this)pages.remove(owner);});refresh();workspace.listen(observer);
        }
        void show(){dialog.show();com.aurora.camera.compat.ui.FullScreenPage220.apply(dialog.getWindow(),ui,ui.backgroundColor(),ui.isDark());}
        void onChange(){if(dialog.isShowing())reload();}
        List<Entry> snapshot()throws IOException{return video!=null?video.snapshot():workspace.snapshot(ws,owner);}
        Set<String> hidden(){return video!=null?video.hidden():workspace.hidden(ws);}
        String kind(){return video!=null?VideoFilterWorkspace270.KIND:workspace.kind(ws);}
        boolean enabled(Entry entry){return video!=null?video.enabled(entry,hidden):workspace.enabled(entry,hidden);}
        void reload(){try{List<Entry> next=snapshot();rows.clear();rows.addAll(next);hidden=hidden();refresh();}catch(IOException failure){error(failure);}}
        void refresh(){visible.clear();handles.clear();int count=0;for(Entry e:rows){Integer handle=ids.get(e.id);if(handle==null){handle=nextId++;ids.put(e.id,handle);}handles.put(handle,e);if(enabled(e))count++;if(e.name.toLowerCase(Locale.ROOT).contains(query)||e.id.toLowerCase(Locale.ROOT).contains(query))visible.add(e);}status="已启用 "+count+" / "+rows.size()+" · 显示 "+visible.size()+" 项";if(ui!=null)ui.refresh();}
        boolean commit(){try{if(video!=null)video.apply(rows,hidden);else workspace.apply(ws,rows,hidden);reload();return true;}catch(IOException e){reload();error(e);return false;}}
        void state(Entry entry,boolean enabled)throws IOException{FilterWorkspace215.setVisibility(hidden,entry.id,enabled);}
        void batch(boolean invert){try{for(Entry e:visible)if(!e.fixed)state(e,!invert||!enabled(e));commit();}catch(IOException failure){reload();error(failure);}}
        Set<String> disabled(){Set<String> result=new HashSet<>(hidden);for(Entry e:rows)if(!enabled(e))result.add(e.id);return result;}
        void menu(Entry e){String[] choices=e.builtIn?new String[]{"上移","下移","置顶"}:new String[]{"上移","下移","置顶","删除滤镜"};dialogs.builder(owner).setTitle(e.name).setItems(choices,(d,n)->{
            if(n==3){dialogs.builder(owner).setTitle("删除 "+e.name+"？").setMessage("使用相机原生删除操作移除此自定义或导入滤镜。").setNegativeButton("取消",null).setPositiveButton("删除",(a,b)->{try{workspace.beforeDelete(ws,e,owner);call(nativeRemove,ws,e.nativeRow);reload();}catch(IOException failure){error(failure);}}).show();return;}
            int target=n==2?0:visible.indexOf(e)+(n==0?-1:1);if(n==2){while(target<rows.size()&&rows.get(target).fixed)target++;rows.remove(e);rows.add(target,e);commit();ui.revealTop();}
            else if(target>=0&&target<visible.size()&&!visible.get(target).fixed){WorkspaceOrder215.moveTo(rows,e,visible.get(target));commit();}
        }).show();}
        List<String> identities(){ArrayList<String> result=new ArrayList<>();for(Entry e:rows)result.add(e.id);return result;}
        void restoreOrder(List<String> saved){Map<String,Entry> byId=new HashMap<>();for(Entry e:rows)byId.put(e.id,e);List<String> ordered=WorkspaceOrder215.restore(identities(),saved);rows.clear();for(String id:ordered)rows.add(byId.get(id));rows.sort((a,b)->Boolean.compare(b.fixed,a.fixed));}
        void pick(int request){if(pending!=0)return;try{if(request==SAVE)backup=WorkspaceBackup215.encode(kind(),identities(),disabled()).toString(2).getBytes(StandardCharsets.UTF_8);Intent intent=new Intent(request==SAVE?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");if(request==SAVE)intent.putExtra(Intent.EXTRA_TITLE,"相机滤镜管理备份-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new java.util.Date())+".json");pending=request;owner.startActivityForResult(intent,request);}catch(JSONException|android.content.ActivityNotFoundException|SecurityException e){pending=0;error(e);}}
        void result(int request,int code,Intent data){pending=0;if(owner.isDestroyed())return;if(!dialog.isShowing())show();if(code!=Activity.RESULT_OK)return;Uri uri=data==null?null:data.getData();if(uri==null||!"content".equals(uri.getScheme())){error(new IOException("系统文件选择器未返回有效文档"));return;}final byte[] snapshot=backup;backup=null;io.execute(()->{try{
            if(request==SAVE){try(OutputStream out=owner.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("无法创建备份文件");out.write(snapshot);out.flush();}owner.runOnUiThread(()->message("已创建备份","已保存当前滤镜分组的排序和启用状态，不包含滤镜资源文件。"));}
            else{byte[] bytes;try(InputStream in=owner.getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){if(in==null)throw new IOException("无法读取备份文件");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>WorkspaceBackup215.LIMIT)throw new IOException("备份文件过大");out.write(buffer,0,n);}bytes=out.toByteArray();if(bytes.length==0)throw new IOException("备份文件为空，请重新创建有效备份");}
                LinkedHashMap<String,Boolean> restored=WorkspaceBackup215.parse(new JSONObject(new String(bytes,StandardCharsets.UTF_8)),kind());owner.runOnUiThread(()->{if(owner.isDestroyed()||!dialog.isShowing())return;dialogs.builder(owner).setTitle("恢复滤镜管理备份").setMessage("按当前工作区匹配排序和启用状态；已删除的条目跳过，新增条目保留在末尾。滤镜文件不写入此备份。").setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{try{reload();restoreOrder(new ArrayList<>(restored.keySet()));for(Entry e:rows)if(!e.fixed&&restored.containsKey(e.id))state(e,restored.get(e.id));commit();}catch(IOException failure){reload();error(failure);}}).show();});}
        }catch(IOException|JSONException|SecurityException e){owner.runOnUiThread(()->error(e));}});}
        void error(Exception e){Log.e("AuroraFilterManager","Operation failed",e);message("操作未完成",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}
        void message(String title,String text){if(!owner.isDestroyed()&&dialog.isShowing())dialogs.builder(owner).setTitle(title).setMessage(text).setPositiveButton("确定",null).show();}
    }
}
