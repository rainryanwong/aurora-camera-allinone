package com.aurora.camera.compat.filter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import com.aurora.camera.compat.Host;
import com.aurora.camera.compat.StrictHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Native catalog management only. Does not import Phoenix catalogs, LUTs or render code. */
public final class FilterManager211 {
    private static final String KEY="pref_aurora_filter_manager", FORMAT="aurora-filter-order";
    private static final String STOCK="e25b1ed5581baddd4e1d7f1ae60c1e0f1b1251645df5ba90dfbb9c42953b4a07";
    private static final int SAVE=0x5211,LOAD=0x5212,LIMIT=1024*1024;
    private final Method global,controller,catalog,identity,find,add,title,key,activity,defaultFilter;
    private final Field catalogs,name,itemText,group,click,keyField,persistent;
    private final java.lang.reflect.Constructor<?> preference,sliding;
    private final Class<?> filterComponent,clickType;
    private final int none;
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"AuroraFilterBackup");t.setDaemon(true);return t;});
    private final Map<Activity,WeakReference<Page>> pages=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Integer,Entry> observed=new LinkedHashMap<>();
    private final Method filterItems,filterValue,settingsAdd,activityResult,destroy,remap;
    private final Field dataValue,dataTitle,dataText,rawItems;
    private volatile Map<Integer,State> state;
    private SharedPreferences prefs;
    private static final class State {
        final int rank;final boolean enabled;
        State(int rank,boolean enabled){this.rank=rank;this.enabled=enabled;}
    }
    private static final class Entry {
        final int id;final String label;
        Entry(int id,String label){this.id=id;this.label=label;}
    }
    private static final class DisplayCache {
        final Object raw,policy;final int size;final List<Object> rows;
        DisplayCache(Object raw,Object policy,int size,List<Object> rows){this.raw=raw;this.policy=policy;this.size=size;this.rows=rows;}
    }
    public FilterManager211(Host h)throws ReflectiveOperationException{
        global=h.method("com.xiaomi.camera.basic.Global","getContext");
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");
        catalog=h.method("com.xiaomi.camera.effect.EffectController","q",int.class);
        catalogs=h.field("com.xiaomi.camera.effect.EffectController","R");
        identity=h.method("i3.b","a");name=h.field("i3.b","c");itemText=h.field("i3.b","o");
        none=h.field("i3.b","P").getInt(null);
        defaultFilter=h.method("v2.U","getDefaultValue",int.class);
        filterComponent=h.type("r2.t");
        filterItems=h.method("r2.a","getItems");
        filterValue=h.method("v2.U","getComponentValue",int.class);
        remap=h.method("v2.U","r",ArrayList.class,int.class,int.class);
        rawItems=h.field("com.android.camera.data.data.c","mItems");
        dataValue=h.field("com.android.camera.data.data.d","r");
        dataTitle=h.field("com.android.camera.data.data.d","l");
        dataText=h.field("com.android.camera.data.data.d","o");
        settingsAdd=h.method("com.android.camera.fragment.settings.CameraCapturePreferenceFragment","addCurrentPreferences");
        activityResult=h.method("androidx.fragment.app.m","onActivityResult",int.class,int.class,Intent.class);
        destroy=h.method("androidx.fragment.app.m","onDestroy");
        group=h.field("com.android.camera.fragment.settings.b","mPreferenceGroup");
        find=h.method("androidx.preference.PreferenceGroup","k0",CharSequence.class);
        add=h.method("androidx.preference.PreferenceGroup","j0",h.type("androidx.preference.Preference"));
        title=h.method("androidx.preference.Preference","e0",String.class);key=h.method("androidx.preference.Preference","a0",String.class);
        keyField=h.field("androidx.preference.Preference","m");click=h.field("androidx.preference.Preference","f");
        persistent=h.field("androidx.preference.Preference","t");
        clickType=h.type("androidx.preference.Preference$d");
        // This is an action, not a camera enum value. ValuePreference would enter
        // g.b(key) during every native refresh and have its click listener replaced.
        preference=h.type("miuix.preference.TextPreference").getDeclaredConstructor(Context.class,h.type("android.util.AttributeSet"));preference.setAccessible(true);
        sliding=h.type("miuix.slidingwidget.widget.SlidingButton").getDeclaredConstructor(Context.class,h.type("android.util.AttributeSet"));sliding.setAccessible(true);
        activity=h.method("androidx.fragment.app.Fragment","requireActivity");
    }
    private static Object call(Method m,Object receiver,Object... args)throws Throwable{
        try{return m.invoke(receiver,args);}catch(InvocationTargetException e){throw e.getCause();}
    }
    private synchronized Map<Integer,State> states()throws Throwable{
        if(state==null){
            Context c=(Context)call(global,null);prefs=c.getSharedPreferences("aurora_filter_manager_v1",Context.MODE_PRIVATE);
            String saved=prefs.getString("state",null);
            state=saved==null?Collections.emptyMap():Collections.unmodifiableMap(parse(new JSONObject(saved),false));
        }
        return state;
    }
    private LinkedHashMap<Integer,State> parse(JSONObject root,boolean backup)throws JSONException{
        if(!FORMAT.equals(root.getString("format"))||root.getInt("version")!=1||!STOCK.equals(root.getString("camera_sha256")))throw new JSONException("备份格式或相机版本不匹配");
        JSONArray rows=root.getJSONArray("entries");if(rows.length()>8192)throw new JSONException("滤镜条目数量超出范围");
        LinkedHashMap<Integer,State> parsed=new LinkedHashMap<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);Object id=row.get("id"),enabled=row.get("enabled");
            if(!(id instanceof Integer)||!(enabled instanceof Boolean))throw new JSONException("滤镜条目类型错误");
            int value=(Integer)id;
            if(parsed.containsKey(value)||value<0||((value==none||value==0)&&!(Boolean)enabled))throw new JSONException("备份包含重复或无效的滤镜标识");
            parsed.put(value,new State(i,(Boolean)enabled));
        }
        return parsed;
    }
    private JSONObject encode(List<Entry> entries,Map<Integer,State> values)throws JSONException{
        JSONObject root=new JSONObject().put("format",FORMAT).put("version",1).put("camera_sha256",STOCK).put("created_at",System.currentTimeMillis());
        JSONArray rows=new JSONArray();for(Entry entry:entries){State s=values.get(entry.id);rows.put(new JSONObject().put("id",entry.id).put("enabled",entry.id==none||entry.id==0||s==null||s.enabled));}
        return root.put("entries",rows);
    }
    private synchronized void save(List<Entry> entries,Map<Integer,State> values)throws Throwable{
        states();String json=encode(entries,values).toString();
        if(!prefs.edit().putString("state",json).commit())throw new IOException("滤镜管理设置未能保存");
        LinkedHashMap<Integer,State> next=new LinkedHashMap<>();for(int i=0;i<entries.size();i++){Entry e=entries.get(i);State s=values.get(e.id);next.put(e.id,new State(i,e.id==none||e.id==0||s==null||s.enabled));}
        state=Collections.unmodifiableMap(next);
    }
    private ArrayList<Object> filtered(List<?> rows)throws Throwable{
        Context context=(Context)call(global,null);
        Map<Integer,State> current=states();
        HashMap<Object,Integer> ids=new HashMap<>();
        for(Object row:rows){
            int id=Integer.parseInt((String)dataValue.get(row));ids.put(row,id);
            int title=dataTitle.getInt(row);String supplied=(String)dataText.get(row);
            String label=supplied!=null&&!supplied.isEmpty()?supplied:(title>0?context.getString(title):(id==none||id==0?"原图":"滤镜 "+id));
            synchronized(observed){observed.put(id,new Entry(id,label));}
        }
        ArrayList<Object> result=new ArrayList<>();
        for(Object row:rows){int id=ids.get(row);State s=current.get(id);if(id==none||id==0||s==null||s.enabled)result.add(row);}
        result.sort((a,b)->{int ai=ids.get(a),bi=ids.get(b);if(ai==none||ai==0)return bi==none||bi==0?0:-1;if(bi==none||bi==0)return 1;
            State as=current.get(ai),bs=current.get(bi);return Integer.compare(as==null?Integer.MAX_VALUE:as.rank,bs==null?Integer.MAX_VALUE:bs.rank);
        });
        return result;
    }
    private List<Entry> entriesNative()throws Throwable{
        Context c=(Context)call(global,null);Object owner=call(controller,null);
        @SuppressWarnings("unchecked") SparseArray<ArrayList<?>> all=(SparseArray<ArrayList<?>>)catalogs.get(owner);
        LinkedHashMap<Integer,Entry> unique=new LinkedHashMap<>();
        // R retains the unmodified native catalog; hooks only return new list views.
        for(int i=0;i<all.size();i++)for(Object row:all.valueAt(i)){
            int id=(Integer)call(identity,row);if(unique.containsKey(id))continue;
            int text=name.getInt(row);if(text<=0&&id!=none&&id!=0)continue;
            String label=text>0?c.getString(text):(id==none?"原图":"滤镜 "+id);
            unique.put(id,new Entry(id,label));
        }
        synchronized(observed){unique.putAll(observed);}
        return new ArrayList<>(unique.values());
    }
    private List<Entry> entries()throws Throwable{
        ArrayList<Entry> result=new ArrayList<>(entriesNative());Map<Integer,State> current=states();
        result.sort(Comparator.comparingInt(e->{State s=current.get(e.id);return s==null?Integer.MAX_VALUE:s.rank;}));
        result.sort(Comparator.comparingInt(e->e.id==none||e.id==0?0:1));
        return result;
    }
    public void install(){
        XposedBridge.hookMethod(filterItems,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable()||p.getResult()==null||!filterComponent.isInstance(p.thisObject))return;
                List<?> input=(List<?>)p.getResult();Object raw=rawItems.get(p.thisObject),policy=states();
                DisplayCache cache=(DisplayCache)XposedHelpers.getAdditionalInstanceField(p.thisObject,"211.filter.display");
                if(cache==null||cache.raw!=raw||cache.policy!=policy||cache.size!=input.size()){
                    cache=new DisplayCache(raw,policy,input.size(),filtered(input));XposedHelpers.setAdditionalInstanceField(p.thisObject,"211.filter.display",cache);
                }
                p.setResult(new ArrayList<>(cache.rows));
            }
        });
        XposedBridge.hookMethod(remap,new StrictHook(){
            @Override protected void after(MethodHookParam p){if(filterComponent.isInstance(p.thisObject))XposedHelpers.removeAdditionalInstanceField(p.thisObject,"211.filter.display");}
        });
        XposedBridge.hookMethod(filterValue,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable()||!filterComponent.isInstance(p.thisObject))return;
                String value=(String)p.getResult();for(Map.Entry<Integer,State> e:states().entrySet())if(!e.getValue().enabled&&Integer.toString(e.getKey()).equals(value)){p.setResult(call(defaultFilter,p.thisObject,p.args[0]));return;}
            }
        });
        XposedBridge.hookMethod(settingsAdd,new StrictHook(){
            @Override protected void after(MethodHookParam p)throws Throwable{
                if(p.hasThrowable())return;Object root=group.get(p.thisObject);if(find.invoke(root,KEY)!=null)return;
                Activity host=(Activity)call(activity,p.thisObject);Object item=preference.newInstance(host,null);
                persistent.setBoolean(item,false);
                call(key,item,KEY);call(title,item,"滤镜自定义");
                Object listener=java.lang.reflect.Proxy.newProxyInstance(clickType.getClassLoader(),new Class<?>[]{clickType},(proxy,m,args)->{
                    if(m.getDeclaringClass()==Object.class){if(m.getName().equals("hashCode"))return System.identityHashCode(proxy);if(m.getName().equals("equals"))return proxy==args[0];return "AuroraFilterManagerClick";}
                    Page page=new Page(host);pages.put(host,new WeakReference<>(page));page.show();return true;
                });
                click.set(item,listener);call(add,root,item);
            }
        });
        XposedBridge.hookMethod(activityResult,new StrictHook(){
            @Override protected void before(MethodHookParam p){
                Activity owner=(Activity)p.thisObject;WeakReference<Page> ref=pages.get(owner);Page page=ref==null?null:ref.get();
                int request=(Integer)p.args[0];if(page!=null&&(request==SAVE||request==LOAD)&&page.pending==request){page.result(request,(Integer)p.args[1],(Intent)p.args[2]);p.setResult(null);}
            }
        });
        XposedBridge.hookMethod(destroy,new StrictHook(){
            @Override protected void before(MethodHookParam p){WeakReference<Page> ref=pages.remove((Activity)p.thisObject);Page page=ref==null?null:ref.get();if(page!=null){page.pending=0;page.dialog.dismiss();}}
        });
    }
    private final class Page {
        final Activity owner;final Dialog dialog;final List<Entry> rows;final HashMap<Integer,State> values;
        final ArrayList<Entry> visible=new ArrayList<>();final FilterManagementUi214 ui;
        final Map<Integer,Entry> byId=new HashMap<>();final Map<Integer,Integer> nativeOrder=new HashMap<>();
        String search="",status="";int pending;byte[] backup;
        Page(Activity owner)throws Throwable{
            this.owner=owner;rows=entries();values=new HashMap<>(states());dialog=new Dialog(owner);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            for(Entry entry:rows)byId.put(entry.id,entry);
            List<Entry> original=entriesNative();for(int i=0;i<original.size();i++)nativeOrder.put(original.get(i).id,i);
            ui=new FilterManagementUi214(owner,new FilterManagementUi214.Controller(){
                public int count(){return visible.size();}public int id(int position){return visible.get(position).id;}public String name(int id){return byId.get(id).label;}
                public boolean fixed(int id){return id==none||id==0;}public boolean enabled(int id){return on(id);}
                public boolean first(int id){for(Entry e:rows)if(!fixed(e.id))return e.id==id;return false;}
                public String status(){return Page.this.status;}
                public void search(String text){search=text.trim().toLowerCase(Locale.ROOT);refresh();}
                public void toggle(int id,boolean checked){if(!fixed(id)){values.put(id,new State(0,checked));commit();}}
                public void pin(int id){Page.this.pin(byId.get(id));}
                public void moveBefore(int id,int target){Page.this.moveBefore(byId.get(id),byId.get(target));}
                public void moveMenu(int id){Entry entry=byId.get(id);new AlertDialog.Builder(owner).setTitle(entry.label).setItems(new String[]{"上移","下移","置顶"},(d,which)->{if(which==2)Page.this.pin(entry);else move(entry,which==0?-1:1);}).show();}
                public void selectAll(){batch(false);}public void invert(){batch(true);}public void backup(){pick(SAVE);}public void restore(){pick(LOAD);}
                public void reset(){new AlertDialog.Builder(owner).setTitle("重置排序与显示").setMessage("恢复默认顺序并启用全部滤镜。滤镜资源和照片保持不变。").setNegativeButton("取消",null).setPositiveButton("重置",(d,w)->resetOrder()).show();}
                public void close(){dialog.dismiss();}
            },sliding);
            dialog.setContentView(ui);dialog.setOnDismissListener(d->{pending=0;pages.remove(owner);});refresh();
        }
        void show(){
            dialog.show();Window window=dialog.getWindow();if(window==null)return;
            window.setLayout(-1,-1);window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(ui.backgroundColor()));
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
            window.setDecorFitsSystemWindows(true);
        }
        boolean on(int id){State s=values.get(id);return id==none||id==0||s==null||s.enabled;}
        void refresh(){visible.clear();int count=0;for(Entry e:rows){if(on(e.id))count++;if(e.label.toLowerCase(Locale.ROOT).contains(search)||Integer.toString(e.id).contains(search))visible.add(e);}status="已启用 "+count+" / "+rows.size()+" · 显示 "+visible.size()+" 项";if(ui!=null)ui.refresh();}
        boolean commit(){try{save(rows,values);refresh();return true;}catch(Throwable failure){if(failure instanceof IOException||failure instanceof JSONException){values.clear();values.putAll(state);rows.sort(Comparator.comparingInt(e->{State s=state.get(e.id);return s==null?Integer.MAX_VALUE:s.rank;}));refresh();error((Exception)failure);return false;}throw failure instanceof RuntimeException?(RuntimeException)failure:new IllegalStateException(failure);}}
        void batch(boolean invert){for(Entry e:visible)if(e.id!=none&&e.id!=0)values.put(e.id,new State(0,!invert||!on(e.id)));commit();}
        void move(Entry entry,int delta){int at=visible.indexOf(entry),next=at+delta;if(next<0||next>=visible.size()||entry.id==none||entry.id==0||visible.get(next).id==none||visible.get(next).id==0)return;Collections.swap(rows,rows.indexOf(entry),rows.indexOf(visible.get(next)));commit();}
        void pin(Entry entry){if(entry.id==none||entry.id==0)return;rows.remove(entry);int at=0;while(at<rows.size()&&(rows.get(at).id==none||rows.get(at).id==0))at++;rows.add(at,entry);if(commit())ui.revealTop();}
        void moveBefore(Entry entry,Entry target){if(entry==target||entry.id==none||entry.id==0||target.id==none||target.id==0)return;rows.remove(entry);rows.add(rows.indexOf(target),entry);commit();}
        void resetOrder(){values.clear();rows.sort(Comparator.comparingInt(e->nativeOrder.get(e.id)));rows.sort(Comparator.comparingInt(e->e.id==none||e.id==0?0:1));if(commit())ui.revealTop();}
        void pick(int request){
            if(pending!=0)return;
            try{if(request==SAVE)backup=encode(rows,values).toString(2).getBytes(StandardCharsets.UTF_8);
                Intent intent=new Intent(request==SAVE?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json");
                if(request==SAVE)intent.putExtra(Intent.EXTRA_TITLE,"相机滤镜管理备份-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new java.util.Date())+".json");
                pending=request;owner.startActivityForResult(intent,request);
            }catch(JSONException|android.content.ActivityNotFoundException|SecurityException failure){pending=0;error(failure);}
        }
        void result(int request,int code,Intent data){
            pending=0;if(code!=Activity.RESULT_OK)return;Uri uri=data==null?null:data.getData();
            if(uri==null||!"content".equals(uri.getScheme())){error(new IOException("系统文件选择器未返回有效文档"));return;}
            final byte[] snapshot=backup;
            io.execute(()->{
                try{
                    if(request==SAVE){try(OutputStream output=owner.getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("无法创建备份文件");output.write(snapshot);output.flush();}owner.runOnUiThread(()->message("已创建备份","已保存排序和启用状态。滤镜资源与照片不写入此备份。"));}
                    else{
                        byte[] bytes;try(InputStream input=owner.getContentResolver().openInputStream(uri);ByteArrayOutputStream output=new ByteArrayOutputStream()){
                            if(input==null)throw new IOException("无法读取备份文件");byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){if(output.size()+n>LIMIT)throw new IOException("备份文件过大");output.write(buffer,0,n);}bytes=output.toByteArray();}
                        LinkedHashMap<Integer,State> restored=parse(new JSONObject(new String(bytes,StandardCharsets.UTF_8)),true);
                        owner.runOnUiThread(()->{
                            if(owner.isDestroyed()||!dialog.isShowing())return;
                            new AlertDialog.Builder(owner).setTitle("恢复滤镜管理备份").setMessage("恢复 "+restored.size()+" 项排序和启用状态。当前未安装的滤镜将跳过，其他滤镜保留在末尾。滤镜资源和照片不受影响。")
                                .setNegativeButton("取消",null).setPositiveButton("恢复",(d,w)->{
                                    rows.sort(Comparator.comparingInt(e->{State s=restored.get(e.id);return s==null?Integer.MAX_VALUE:s.rank;}));values.putAll(restored);if(commit())message("已恢复","返回相机后重新打开滤镜列表即可查看。");}).show();
                        });
                    }
                }catch(IOException|JSONException|SecurityException failure){owner.runOnUiThread(()->error(failure));}
            });
        }
        void error(Exception failure){Log.e("AuroraFilterBackup","Backup operation failed",failure);message("操作未完成",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());}
        void message(String title,String body){if(!owner.isDestroyed()&&dialog.isShowing())new AlertDialog.Builder(owner).setTitle(title).setMessage(body).setPositiveButton("确定",null).show();}
    }
}
