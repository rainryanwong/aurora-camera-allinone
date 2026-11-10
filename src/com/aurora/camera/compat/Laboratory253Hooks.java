package com.aurora.camera.compat;

import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;
import com.aurora.camera.compat.filter.*;
import com.aurora.camera.compat.settings.CameraSettingsBackup253;
import com.aurora.camera.compat.ui.*;
import de.robv.android.xposed.*;
import java.io.*;
import java.lang.reflect.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

/** Native camera preference pages for user filters and complete settings backup. */
public final class Laboratory253Hooks {
    private static final String PAGE="aurora_settings_page253",CUSTOM="aurora_custom_filter_manager253",BACKUP="aurora_settings_backup253",BOUND="aurora_filter_selection253",SEARCH="aurora_filter_search254";
    private static final int IMPORT=0x53a1,WRITE=0x53a2,RESTORE=0x53a3;
    private final Host host;
    private final LutRuntime229 runtime;
    private final NativePreferences240 preferences;
    private final MiuixDialogs dialogs;
    private final Options229 options;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService io=Executors.newSingleThreadExecutor(task->{Thread t=new Thread(task,"CameraSettingsIO");t.setDaemon(true);return t;});
    private final Map<Object,Page> pages=new WeakHashMap<>();
    private final Map<String,ImportJob> imports=new HashMap<>();
    private final Method lab,populate,register,initialize,resume,update,result,destroy,bind,bindText,start;
    private CustomFilterStore253 store;
    public Laboratory253Hooks(Host host,LutRuntime229 runtime)throws ReflectiveOperationException{
        this.host=host;this.runtime=runtime;preferences=new NativePreferences240(host);dialogs=new MiuixDialogs(host);options=new Options229(host);
        lab=host.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        populate=host.method("com.android.camera.fragment.settings.CameraPreferenceFragment","addCurrentPreferences");
        register=host.method("com.android.camera.fragment.settings.CameraPreferenceFragment","registerPreferenceListener");
        initialize=host.method("com.android.camera.fragment.settings.b","initializeActivity");resume=host.method("com.android.camera.fragment.settings.CameraPreferenceFragment","onResume");
        update=host.method("com.android.camera.fragment.settings.b","updatePreferences",host.type("androidx.preference.PreferenceGroup"),SharedPreferences.class);
        result=host.method("androidx.fragment.app.Fragment","onActivityResult",int.class,int.class,Intent.class);
        destroy=host.method("androidx.fragment.app.Fragment","onDestroy");bind=host.method("androidx.preference.Preference","D",host.type("androidx.preference.l"));
        bindText=host.method("miuix.preference.TextPreference","D",host.type("androidx.preference.l"));
        start=host.method("androidx.fragment.app.Fragment","startActivityForResult",Intent.class,int.class);
        host.type("miuix.androidbasewidget.widget.ProgressBar").getDeclaredConstructor(Context.class,android.util.AttributeSet.class);
        host.type("miuix.androidbasewidget.widget.StateEditText").getDeclaredConstructor(Context.class,android.util.AttributeSet.class);
    }
    private Class<?> type(String name){try{return host.type(name);}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}}
    private Object call(Object target,String name,Object...args){return XposedHelpers.callMethod(target,name,args);}
    private Activity activity(Object fragment){return(Activity)call(fragment,"requireActivity");}
    private Bundle state(Object fragment){Bundle args=(Bundle)call(fragment,"getArguments");if(args==null){args=new Bundle();call(fragment,"setArguments",args);}return args;}
    private String kind(Object fragment){
        Activity owner=(Activity)call(fragment,"getActivity");if(owner==null||!owner.getClass().getName().equals("com.android.camera.fragment.settings.PreferenceExtraActivity"))return null;
        Bundle args=(Bundle)call(fragment,"getArguments");return args!=null&&args.containsKey(PAGE)?args.getString(PAGE):owner.getIntent().getStringExtra(PAGE);
    }
    private CustomFilterStore253 store(){
        if(store==null)try{store=new CustomFilterStore253(host,runtime);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}return store;
    }
    public void install(){
        XposedBridge.hookMethod(lab,new StrictHook(2000){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable())return;Object f=p.thisObject;
            options.addAction(f,CUSTOM,"自定义滤镜管理","搜索、去重和批量删除自建配方与导入滤镜",()->open(f,"filters"));
            options.addAction(f,BACKUP,"备份与恢复","备份相机设置、菜单与模式布局及模块选项",()->open(f,"backup"));
            options.addSwitch(f,FilterStrength270.KEY,"滤镜强度调节","打开照片滤镜面板时显示强度操作条",null,null);
            options.addSwitch(f,SuperTelephotoHooks.THREE_STATES,"AI超级长焦三档切换","提供关闭、自动、开启三档选择",null,null);
            options.addSwitch(f,SuperTelephotoHooks.ZOOM10,"AI超级长焦10倍及以上启用","将超级长焦增强的启用倍率调整为10×及以上",null,null);
            SuperTelephotoHooks.mergeLensOptions(options.prefs());
            options.addSwitch(f,SuperTelephotoHooks.EXTENDED,"AI超级长焦扩展镜头支持","将AI超级长焦算法进一步放开至主摄及3.2x镜头可用，经实测，多数场景下增强后效果不及原图直出，请根据实际情况选择启用，而非盲目开启。",null,null);
            preferences.regroupLaboratoryTools(f);
        }});
        XposedBridge.hookMethod(populate,new StrictHook(){@Override protected void before(MethodHookParam p){
            String kind=kind(p.thisObject);if(kind==null)return;Page page=page(p.thisObject,kind);page.render();if(!kind.equals("backup"))main.post(page::load);p.setResult(null);
        }});
        XposedBridge.hookMethod(register,new StrictHook(){@Override protected void before(MethodHookParam p){if(kind(p.thisObject)!=null)p.setResult(null);}});
        XposedBridge.hookMethod(update,new StrictHook(){@Override protected void before(MethodHookParam p){if(kind(p.thisObject)!=null)p.setResult(null);}});
        StrictHook title=new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&kind(p.thisObject)!=null){Page page=page(p.thisObject,kind(p.thisObject));page.title();if(page.resumeImportJob())return;if(p.method.equals(resume)){page.launchImport();if(!page.kind.equals("backup")&&page.loaded&&!page.busy&&state(p.thisObject).getInt("document",0)==0){page.loaded=false;page.load();}}}}};
        XposedBridge.hookMethod(initialize,title);XposedBridge.hookMethod(resume,title);
        XposedBridge.hookMethod(result,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable()||kind(p.thisObject)==null)return;Page page=page(p.thisObject,kind(p.thisObject));int request=(Integer)p.args[0];
            if(request!=state(p.thisObject).getInt("document",0))return;state(p.thisObject).remove("document");XposedHelpers.setBooleanField(p.thisObject,"mGoToActivity",false);
            if((Integer)p.args[1]==Activity.RESULT_OK)page.document(request,(Intent)p.args[2]);
        }});
        XposedBridge.hookMethod(destroy,new StrictHook(){@Override protected void before(MethodHookParam p){Page page=pages.remove(p.thisObject);if(page!=null){page.dismissProgress();ImportJob job=imports.get(state(p.thisObject).getString("import_job"));if(job!=null){if(job.page.get()==page)job.page.clear();if(!activity(p.thisObject).isChangingConfigurations()){job.abandoned=true;finishImportJob(job);}}else if(page.kind.equals("import")&&!activity(p.thisObject).isChangingConfigurations())page.discardImport();}}});
        XposedBridge.hookMethod(bind,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable())return;View item=(View)XposedHelpers.getObjectField(p.args[0],"itemView");Object metadata=XposedHelpers.getAdditionalInstanceField(p.thisObject,BOUND);
            if(metadata instanceof Object[]){Object[] data=(Object[])metadata;Page page=(Page)data[0];String id=(String)data[1];item.setOnLongClickListener(view->{page.selecting=true;page.selected.add(id);page.save();page.refreshRows();return true;});XposedHelpers.setAdditionalInstanceField(item,BOUND,true);}
            else if(XposedHelpers.removeAdditionalInstanceField(item,BOUND)!=null)item.setOnLongClickListener(null);
        }});
        XposedBridge.hookMethod(bindText,new StrictHook(){@Override protected void after(MethodHookParam p){
            if(p.hasThrowable())return;Object owner=XposedHelpers.getAdditionalInstanceField(p.thisObject,SEARCH);
            if(owner instanceof Page)((Page)owner).bindSearch((ViewGroup)XposedHelpers.getObjectField(p.args[0],"itemView"));
        }});
    }
    private Page page(Object fragment,String kind){Page page=pages.get(fragment);if(page==null){page=new Page(fragment,kind);pages.put(fragment,page);}return page;}
    public void openImport(Object fragment){open(fragment,"filters",true);}
    private void open(Object fragment,String kind){open(fragment,kind,false);}
    private void open(Object fragment,String kind,boolean importOnOpen){
        Activity owner=activity(fragment);Intent intent=new Intent(owner,type("com.android.camera.fragment.settings.PreferenceExtraActivity"));
        intent.putExtra("target_tag","com.android.camera.fragment.settings.CameraPreferenceFragment").putExtra(PAGE,kind);
        intent.putExtra("import_on_open",importOnOpen);
        try{intent.putExtra("from_where",host.field("com.android.camera.fragment.settings.b","mFromWhere").getInt(null));}
        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        XposedHelpers.setBooleanField(fragment,"mGoToActivity",true);owner.startActivity(intent);
    }
    private void pushPage(Object fragment,Bundle args,String tag){
        Object next=XposedHelpers.newInstance(type("com.android.camera.fragment.settings.CameraPreferenceFragment"));call(next,"setArguments",args);
        Object edit=XposedHelpers.newInstance(type("androidx.fragment.app.a"),call(fragment,"getParentFragmentManager"));call(edit,"i",android.R.id.content,next,tag);XposedHelpers.setBooleanField(edit,"g",true);XposedHelpers.setObjectField(edit,"i",tag);call(edit,"n",false);
    }
    private Object listener(String type,String method,java.util.function.Function<Object[],Object> action){
        return Proxy.newProxyInstance(host.loader,new Class<?>[]{type(type)},(proxy,called,args)->{
            if(called.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,called.getName(),args);
            if(!called.getName().equals(method))throw new IllegalStateException(called.toString());return action.apply(args);
        });
    }
    private final class ImportJob {
        final String id=UUID.randomUUID().toString(),title;
        WeakReference<Page> page;
        boolean complete,abandoned;
        int completed,total;
        String name="";
        CustomFilterStore253.ImportBatch batch;
        CustomFilterStore253.ImportResult result;
        Exception failure;
        ImportJob(Page page,String title){this.page=new WeakReference<>(page);this.title=title;imports.put(id,this);state(page.fragment).putString("import_job",id);}
        void progress(int done,int count,String label){main.post(()->{completed=done;total=count;name=label;Page owner=page.get();if(owner!=null&&owner.attached())owner.progress(done,count,label);});}
    }
    private void finishImportJob(ImportJob job){
        if(!job.complete)return;
        if(job.abandoned){imports.remove(job.id);if(job.batch!=null)io.execute(()->{try{job.batch.close();}catch(IOException failure){Log.e("CameraFilterManager","Import staging cleanup failed",failure);}});return;}
        Page owner=job.page.get();if(owner!=null&&owner.attached()&&(Boolean)call(owner.fragment,"isResumed"))owner.completeImportJob(job);
    }
    private final class Page {
        final Object fragment;
        final String kind;
        final Set<String> selected=new LinkedHashSet<>();
        List<CustomFilterStore253.Entry> entries=new ArrayList<>();
        CustomFilterStore253.ImportBatch importBatch;
        final List<List<String>> groups=new ArrayList<>();
        String query;
        boolean selecting,busy,loaded;
        Dialog progressDialog;
        ProgressBar progressBar;
        TextView progressText;
        final List<WeakReference<TextView>> selectionCounts=new ArrayList<>();
        final List<Object> resultGroups=new ArrayList<>();
        Object deleteAction,cancelSelection;
        Page(Object fragment,String kind){
            this.fragment=fragment;this.kind=kind;Bundle state=state(fragment);query=state.getString("query","");selecting=state.getBoolean("selecting",kind.equals("duplicates"));
            ArrayList<String> ids=state.getStringArrayList("selected");if(ids!=null)selected.addAll(ids);
            ArrayList<String> saved=state.getStringArrayList("groups");if(saved!=null)for(String group:saved)groups.add(Arrays.asList(group.split("\n")));
        }
        boolean attached(){return(Boolean)call(fragment,"isAdded")&&!activity(fragment).isFinishing();}
        void launchImport(){
            Bundle saved=state(fragment);
            if(kind.equals("filters")&&activity(fragment).getIntent().getBooleanExtra("import_on_open",false)&&!saved.getBoolean("import_launch_consumed",false)){
                saved.putBoolean("import_launch_consumed",true);main.post(()->{if(attached())choose(IMPORT);});
            }
        }
        void title(){com.aurora.camera.compat.ui.NativePageTitle257.set(activity(fragment),kind.equals("backup")?"备份与恢复":kind.equals("import")?"选择待导入滤镜":kind.equals("duplicates")?"重复滤镜":"自定义滤镜管理");}
        void save(){Bundle state=state(fragment);state.putString("query",query);state.putBoolean("selecting",selecting);state.putStringArrayList("selected",new ArrayList<>(selected));ArrayList<String> groupIds=new ArrayList<>();for(List<String> group:groups)groupIds.add(String.join("\n",group));state.putStringArrayList("groups",groupIds);}
        Object category(Object root,String key,String title,int order){return preferences.category(fragment,root,key,title,order);}
        Object row(Object group,String key,String title,String summary,Runnable action){
            Object row=preferences.text(fragment);call(row,"a0",key);call(row,"e0",title);call(row,"c0",summary);XposedHelpers.setBooleanField(row,"t",false);
            if(action!=null)XposedHelpers.setObjectField(row,"f",listener("androidx.preference.Preference$d","onPreferenceClick",args->{action.run();return true;}));
            call(row,"Y",!busy);call(group,"j0",row);return row;
        }
        void render(){
            if(!attached())return;Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");call(root,"m0");resultGroups.clear();title();
            if(kind.equals("backup")){
                Object group=category(root,"backup_actions","相机与模块设置",0);
                row(group,"settings_backup_create","创建备份","保存设置、顶部与下拉菜单布局、模式排序、模块选项及设置引用的滤镜资源",()->choose(WRITE));
                row(group,"settings_backup_restore","恢复备份","选择备份文件并恢复设置",()->dialogs.builder(activity(fragment)).setTitle("恢复相机设置").setMessage("选择备份后，将替换对应的相机设置、菜单布局和模块选项。恢复完成后关闭相机，再重新打开。").setNegativeButton("取消",null).setPositiveButton("选择备份",(d,w)->choose(RESTORE)).show());return;
            }
            if(kind.equals("import")){renderImport(root);return;}
            Object tools=category(root,"filter_tools",null,0);
            if(kind.equals("filters")){
                row(tools,"custom_duplicates","检测重复滤镜","按完整配方与 LUT 内容分组",this::scan);
            }
            if(kind.equals("duplicates"))row(tools,"custom_auto_select","自动选择重复项","每组保留第一项，选中其余重复项",()->autoSelect());
            deleteAction=row(tools,"custom_delete","删除所选滤镜","已选择 "+selected.size()+" 项",this::confirmDelete);
            cancelSelection=row(tools,"custom_cancel_selection","退出选择","",()->{selecting=false;selected.clear();save();refreshRows();});
            call(deleteAction,"f0",selecting);call(cancelSelection,"f0",selecting);
            addSearch(root);renderRows(root);
        }
        Object resultGroup(Object root,String key,String title,int order){Object group=category(root,key,title,order);resultGroups.add(group);return group;}
        void renderRows(Object root){
            if(kind.equals("import")){renderImportRows(root);return;}
            if(!loaded){row(resultGroup(root,"custom_rows",null,2),"custom_loading","正在读取滤镜…","",null);return;}
            if(kind.equals("duplicates")){
                Map<String,CustomFilterStore253.Entry> byId=byId();int groupIndex=0;
                for(List<String> ids:groups){List<CustomFilterStore253.Entry> present=new ArrayList<>();for(String id:ids)if(byId.containsKey(id)&&matches(byId.get(id)))present.add(byId.get(id));if(present.isEmpty())continue;
                    Object group=resultGroup(root,"duplicate_group_"+groupIndex,"重复组 "+(++groupIndex)+" · "+present.size()+" 项",groupIndex+1);for(CustomFilterStore253.Entry entry:present)addEntry(group,entry);
                }
                if(groupIndex==0)row(resultGroup(root,"duplicate_empty",null,2),"custom_empty","没有重复滤镜","",null);
            }else{
                List<CustomFilterStore253.Entry> shown=visible();Object group=resultGroup(root,"custom_rows","自建配方与导入滤镜 · "+shown.size()+" 项",2);
                for(CustomFilterStore253.Entry entry:shown)addEntry(group,entry);
                if(shown.isEmpty())row(group,"custom_empty",query.isEmpty()?"还没有自定义滤镜":"没有匹配的滤镜","",null);
            }
        }
        void refreshRows(){
            if(!attached())return;Object root=XposedHelpers.getObjectField(fragment,"mPreferenceGroup");
            for(Object group:resultGroups)call(root,"n0",group);resultGroups.clear();renderRows(root);updateSelection();
        }
        void addSearch(Object root){
            Object group=category(root,"filter_search",null,1);Object row=row(group,"filter_search_controls","","",null);
            XposedHelpers.setIntField(row,"V",android.R.layout.activity_list_item);XposedHelpers.setIntField(row,"W",0);
            XposedHelpers.setBooleanField(row,"e0",false);XposedHelpers.setAdditionalInstanceField(row,SEARCH,this);
        }
        int dp(int value){return Math.round(value*activity(fragment).getResources().getDisplayMetrics().density);}
        int color(int attribute){TypedValue value=new TypedValue();activity(fragment).getTheme().resolveAttribute(attribute,value,true);return value.resourceId==0?value.data:activity(fragment).getColor(value.resourceId);}
        void bindSearch(ViewGroup holder){
            holder.setOnClickListener(null);holder.setOnLongClickListener(null);holder.setClickable(false);holder.setFocusable(false);
            Object previous=XposedHelpers.getAdditionalInstanceField(holder,SEARCH);
            if(previous==this){updateSelection();return;}
            holder.removeAllViews();
            LinearLayout content=new LinearLayout(activity(fragment));content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(6),dp(20),dp(6));
            EditText input=(EditText)XposedHelpers.newInstance(type("miuix.androidbasewidget.widget.StateEditText"),activity(fragment),null);
            input.setSingleLine(true);input.setTextSize(16);input.setHint("搜索滤镜名称或标识");input.setContentDescription("搜索滤镜名称或标识");input.setBackgroundColor(android.graphics.Color.TRANSPARENT);input.setPadding(0,0,0,0);input.setText(query);input.setSelection(input.length());
            content.addView(input,new LinearLayout.LayoutParams(-1,dp(52)));
            LinearLayout actions=new LinearLayout(activity(fragment));actions.setGravity(Gravity.CENTER_VERTICAL);TextView selectionCount=new TextView(activity(fragment));selectionCount.setTextSize(12);selectionCount.setTextColor(color(android.R.attr.textColorSecondary));selectionCounts.add(new WeakReference<>(selectionCount));
            actions.addView(selectionCount,new LinearLayout.LayoutParams(0,-2,1));
            for(boolean invert:new boolean[]{false,true}){TextView action=new TextView(activity(fragment));action.setText(invert?"反选":"全选");action.setTextSize(13);action.setTextColor(0xff3482ff);action.setGravity(Gravity.CENTER);action.setMinWidth(dp(48));action.setPadding(dp(8),0,dp(8),0);action.setOnClickListener(view->{if(busy)return;if(kind.equals("import"))batchImport(invert);else batch(invert);});actions.addView(action,new LinearLayout.LayoutParams(-2,dp(48)));}
            content.addView(actions,new LinearLayout.LayoutParams(-1,-2));holder.addView(content,new ViewGroup.LayoutParams(-1,-2));XposedHelpers.setAdditionalInstanceField(holder,SEARCH,this);updateSelection();
            input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence text,int start,int count,int after){}public void afterTextChanged(Editable text){}public void onTextChanged(CharSequence text,int start,int before,int count){query=text.toString();save();refreshRows();}});
        }
        void updateSelection(){
            int total,shown=0,checked=0;
            if(kind.equals("import")){total=importBatch==null?0:importBatch.items.size();if(importBatch!=null)for(CustomFilterStore253.ImportItem item:importBatch.items)if(importMatches(item)){shown++;if(selected.contains(Integer.toString(item.index)))checked++;}}
            else{total=entries.size();if(kind.equals("duplicates")){Set<String> ids=new HashSet<>();for(List<String> group:groups)ids.addAll(group);total=0;for(CustomFilterStore253.Entry entry:entries)if(ids.contains(entry.id))total++;}List<CustomFilterStore253.Entry> shownRows=visible();shown=shownRows.size();for(CustomFilterStore253.Entry entry:shownRows)if(selected.contains(entry.id))checked++;}
            for(java.util.Iterator<WeakReference<TextView>> it=selectionCounts.iterator();it.hasNext();){TextView count=it.next().get();if(count==null)it.remove();else count.setText("已选择滤镜数："+checked+"/"+shown+"，共计"+total+"项");}
            if(!kind.equals("import")&&deleteAction!=null){call(deleteAction,"c0","已选择 "+selected.size()+" 项");call(deleteAction,"f0",selecting);call(cancelSelection,"f0",selecting);}
            title();
        }
        void addEntry(Object group,CustomFilterStore253.Entry entry){
            Object row;String description=entry.description+(entry.slot>0?entry.enabled?" · 已启用":" · 未启用":"");
            if(selecting){
                row=preferences.selection(fragment);call(row,"a0","custom_item_"+entry.id);call(row,"e0",entry.name);call(row,"c0",description);XposedHelpers.setBooleanField(row,"t",false);call(row,"setChecked",selected.contains(entry.id));
                XposedHelpers.setObjectField(row,"e",listener("androidx.preference.Preference$c","onPreferenceChange",args->{if(Boolean.TRUE.equals(args[1]))selected.add(entry.id);else selected.remove(entry.id);save();updateSelection();return true;}));call(group,"j0",row);
            }else row=row(group,"custom_item_"+entry.id,entry.name,description,()->{
                MiuixDialogs.Builder dialog=dialogs.builder(activity(fragment)).setTitle(entry.name).setMessage(description+"\n长按滤镜可批量选择与删除");
                if(entry.slot>0)dialog.setNegativeButton("取消",null).setPositiveButton(entry.enabled?"在相机中停用":"在相机中启用",(d,w)->{
                    try{store().enabled(entry,!entry.enabled);store().refresh();loaded=false;load();}catch(Exception failure){error(failure);}
                });else dialog.setPositiveButton("确定",null);dialog.show();
            });
            XposedHelpers.setAdditionalInstanceField(row,BOUND,new Object[]{this,entry.id});
        }
        boolean matches(CustomFilterStore253.Entry entry){String wanted=query.trim().toLowerCase(Locale.ROOT);return entry.name.toLowerCase(Locale.ROOT).contains(wanted)||entry.id.toLowerCase(Locale.ROOT).contains(wanted);}
        Map<String,CustomFilterStore253.Entry> byId(){Map<String,CustomFilterStore253.Entry> result=new LinkedHashMap<>();for(CustomFilterStore253.Entry entry:entries)result.put(entry.id,entry);return result;}
        List<CustomFilterStore253.Entry> visible(){Set<String> duplicateIds=new HashSet<>();for(List<String> group:groups)duplicateIds.addAll(group);List<CustomFilterStore253.Entry> result=new ArrayList<>();for(CustomFilterStore253.Entry entry:entries)if(matches(entry)&&(!kind.equals("duplicates")||duplicateIds.contains(entry.id)))result.add(entry);return result;}
        void batch(boolean invert){selecting=true;for(CustomFilterStore253.Entry entry:visible())if(invert&&selected.contains(entry.id))selected.remove(entry.id);else selected.add(entry.id);save();refreshRows();}
        void autoSelect(){Set<String> next=new LinkedHashSet<>();Set<String> present=byId().keySet();for(List<String> group:groups){boolean kept=false;for(String id:group)if(present.contains(id)){if(kept)next.add(id);else kept=true;}}try{next.removeAll(store().referencedLuts(next));}catch(IOException failure){error(failure);return;}selecting=true;selected.clear();selected.addAll(next);save();refreshRows();}
        void load(){
            if(resumeImportJob())return;
            if(kind.equals("import")){loadImport();return;}
            if(busy||loaded||!attached())return;busy=true;io.execute(()->{try{List<CustomFilterStore253.Entry> result=store().entries();main.post(()->{busy=false;if(!attached())return;entries=result;loaded=true;selected.retainAll(byId().keySet());save();render();});}catch(Exception failure){main.post(()->{busy=false;error(failure);});}});
        }
        void choose(int request){
            Intent intent=new Intent(request==WRITE?Intent.ACTION_CREATE_DOCUMENT:Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(request==IMPORT?"*/*":"application/zip");
            intent.addFlags(request==WRITE?Intent.FLAG_GRANT_WRITE_URI_PERMISSION:Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(request==IMPORT)intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
            if(request==WRITE)intent.putExtra(Intent.EXTRA_TITLE,"相机设置备份-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".zip");
            state(fragment).putInt("document",request);XposedHelpers.setBooleanField(fragment,"mGoToActivity",true);
            try{start.invoke(fragment,intent,request);}catch(ReflectiveOperationException|ActivityNotFoundException|SecurityException failure){state(fragment).remove("document");XposedHelpers.setBooleanField(fragment,"mGoToActivity",false);error(failure);}
        }
        void document(int request,Intent data){
            LinkedHashSet<Uri> uris=new LinkedHashSet<>();if(data!=null){if(data.getData()!=null)uris.add(data.getData());ClipData clip=data.getClipData();if(clip!=null)for(int i=0;i<clip.getItemCount();i++)uris.add(clip.getItemAt(i).getUri());}
            if(uris.isEmpty()){error(new IOException("文件选择器未返回文档"));return;}for(Uri uri:uris)if(uri==null||!"content".equals(uri.getScheme())){error(new IOException("文件选择器返回的文档无效"));return;}
            if(request==IMPORT)importFiles(new ArrayList<>(uris));else backup(request,uris.iterator().next());
        }
        void importFiles(List<Uri> uris){
            ImportJob job=new ImportJob(this,"解析滤镜包");begin(job.title);io.execute(()->{try{CustomFilterStore253.ImportBatch batch=store().prepareFiles(uris,job::progress);main.post(()->{job.batch=batch;job.complete=true;finishImportJob(job);});}catch(Exception failure){main.post(()->{job.failure=failure;job.complete=true;finishImportJob(job);});}});
        }
        void loadImport(){
            if(busy||loaded||state(fragment).containsKey("import_result")||!attached())return;busy=true;String token=state(fragment).getString("import_token");
            io.execute(()->{try{CustomFilterStore253.ImportBatch batch=store().prepared(token);main.post(()->{
                busy=false;if(!attached())return;importBatch=batch;loaded=true;
                if(!state(fragment).getBoolean("import_selection_initialized",false)){selected.clear();for(CustomFilterStore253.ImportItem item:batch.items)selected.add(Integer.toString(item.index));state(fragment).putBoolean("import_selection_initialized",true);save();}render();
            });}catch(Exception failure){main.post(()->{busy=false;error(failure);});}});
        }
        void renderImport(Object root){
            String result=state(fragment).getString("import_result");
            if(result!=null){Object group=category(root,"import_result",null,0);row(group,"import_result_details","导入结果",result,()->message("导入结果",result));row(group,"import_return","返回滤镜管理","",()->activity(fragment).onBackPressed());return;}
            Object tools=category(root,"import_tools",null,0);
            row(tools,"pending_import","导入所选滤镜","已选择 "+selected.size()+" 项",()->{
                if(importBatch==null)return;if(selected.isEmpty()){message("导入滤镜","请至少选择一个滤镜");return;}Set<Integer> ids=new LinkedHashSet<>();for(String id:selected)ids.add(Integer.parseInt(id));runImport(importBatch,ids);
            });row(tools,"pending_cancel","取消导入","",()->activity(fragment).onBackPressed());
            if(importBatch!=null&&!importBatch.unreadable.isEmpty())row(tools,"pending_errors","解析失败的文件",importBatch.unreadable.size()+" 个文件",()->message("文件解析结果",String.join("\n",importBatch.unreadable)));
            addSearch(root);renderImportRows(root);
        }
        void renderImportRows(Object root){
            if(!loaded){row(resultGroup(root,"pending_rows",null,2),"pending_loading","正在读取待导入滤镜…","",null);return;}
            Object group=resultGroup(root,"pending_rows","待导入滤镜 · "+importBatch.items.size()+" 项",2);int shown=0;
            for(CustomFilterStore253.ImportItem item:importBatch.items){if(!importMatches(item))continue;shown++;String id=Integer.toString(item.index);Object row=preferences.selection(fragment);call(row,"a0","pending_item_"+id);call(row,"e0",item.name);call(row,"c0",item.preset?"相机配方":"LUT");XposedHelpers.setBooleanField(row,"t",false);call(row,"setChecked",selected.contains(id));
                XposedHelpers.setObjectField(row,"e",listener("androidx.preference.Preference$c","onPreferenceChange",args->{if(Boolean.TRUE.equals(args[1]))selected.add(id);else selected.remove(id);save();updateSelection();Object action=call(XposedHelpers.getObjectField(fragment,"mPreferenceGroup"),"k0","pending_import");call(action,"c0","已选择 "+selected.size()+" 项");return true;}));call(group,"j0",row);
            }
            if(shown==0)row(group,"pending_empty","没有匹配的滤镜","",null);
        }
        boolean importMatches(CustomFilterStore253.ImportItem item){String wanted=query.trim().toLowerCase(Locale.ROOT);return item.name.toLowerCase(Locale.ROOT).contains(wanted)||Integer.toString(item.index+1).contains(wanted);}
        void batchImport(boolean invert){if(importBatch==null)return;for(CustomFilterStore253.ImportItem item:importBatch.items)if(importMatches(item)){String id=Integer.toString(item.index);if(invert&&selected.contains(id))selected.remove(id);else selected.add(id);}save();refreshRows();Object action=call(XposedHelpers.getObjectField(fragment,"mPreferenceGroup"),"k0","pending_import");call(action,"c0","已选择 "+selected.size()+" 项");}
        void runImport(CustomFilterStore253.ImportBatch batch,Set<Integer> ids){
            ImportJob job=new ImportJob(this,"导入滤镜");begin(job.title);io.execute(()->{try{CustomFilterStore253.ImportResult result=store().importSelected(batch,ids,job::progress);main.post(()->{job.result=result;job.complete=true;finishImportJob(job);});}catch(Exception failure){main.post(()->{job.failure=failure;job.complete=true;finishImportJob(job);});}});
        }
        boolean resumeImportJob(){
            String id=state(fragment).getString("import_job");if(id==null)return false;ImportJob job=imports.get(id);
            if(job==null){state(fragment).remove("import_job");if(kind.equals("import")){state(fragment).putString("import_result","上次导入结果无法恢复，请返回滤镜管理查看已导入项目。");render();}else error(new IOException("上次导入结果无法恢复，请查看已导入的滤镜。"));return true;}
            job.page=new WeakReference<>(this);busy=true;if(progressDialog==null)begin(job.title);progress(job.completed,job.total,job.name);finishImportJob(job);return true;
        }
        void completeImportJob(ImportJob job){
            state(fragment).remove("import_job");imports.remove(job.id);end();
            if(job.failure!=null){if(kind.equals("import")){state(fragment).putString("import_result","导入失败："+job.failure.getMessage());render();}error(job.failure);return;}
            if(job.batch!=null){
                if(job.batch.needsSelection){Bundle args=new Bundle();args.putString(PAGE,"import");args.putString("import_token",job.batch.token);pushPage(fragment,args,"custom_filter_import");}
                else{Set<Integer> all=new LinkedHashSet<>();for(CustomFilterStore253.ImportItem item:job.batch.items)all.add(item.index);runImport(job.batch,all);}return;
            }
            String summary=job.result.summary();try{store().refresh();}catch(Exception failure){Log.e("CameraFilterManager","Imported filter refresh failed",failure);summary+="\n\n滤镜列表更新失败："+failure.getMessage();}
            if(kind.equals("import")){state(fragment).putString("import_result",summary);render();message("导入结果",summary);}else{loaded=false;load();message("导入结果",summary);}
        }
        void discardImport(){String token=state(fragment).getString("import_token");if(token!=null)io.execute(()->{try{store().discard(token);}catch(IOException failure){Log.e("CameraFilterManager","Import staging cleanup failed",failure);}});
        }
        void scan(){
            begin("检测重复滤镜");io.execute(()->{try{CustomFilterStore253.ScanResult result=store().duplicates(store().entries(),this::progress);main.post(()->{
                end();if(!attached())return;String text="检测滤镜："+result.total+"\n重复组："+result.groups.size()+"\n可去重："+result.duplicates()+(result.failures.isEmpty()?"":"\n\n检测失败：\n"+String.join("\n",result.failures));
                MiuixDialogs.Builder builder=dialogs.builder(activity(fragment)).setTitle("去重检测结果").setMessage(text).setNegativeButton("关闭",null);
                if(!result.groups.isEmpty())builder.setPositiveButton("查看重复项",(d,w)->openDuplicates(result));builder.show();
            });}catch(Exception failure){main.post(()->{end();error(failure);});}});
        }
        void openDuplicates(CustomFilterStore253.ScanResult result){
            Bundle args=new Bundle();args.putString(PAGE,"duplicates");args.putBoolean("selecting",true);ArrayList<String> groupIds=new ArrayList<>(),selectedIds=new ArrayList<>();
            for(List<CustomFilterStore253.Entry> group:result.groups){List<String> ids=new ArrayList<>();for(CustomFilterStore253.Entry entry:group)ids.add(entry.id);groupIds.add(String.join("\n",ids));selectedIds.addAll(ids.subList(1,ids.size()));}
            try{selectedIds.removeAll(store().referencedLuts(new HashSet<>(selectedIds)));}catch(IOException failure){error(failure);return;}
            args.putStringArrayList("groups",groupIds);args.putStringArrayList("selected",selectedIds);pushPage(fragment,args,"custom_filter_duplicates");
        }
        void confirmDelete(){
            if(selected.isEmpty()){message("删除滤镜","请先选择滤镜");return;}dialogs.builder(activity(fragment)).setTitle("删除 "+selected.size()+" 个滤镜？").setMessage("所选自建配方与导入滤镜将被删除。").setNegativeButton("取消",null).setPositiveButton("删除",(d,w)->{
                List<String> failures=new ArrayList<>();Map<String,CustomFilterStore253.Entry> current=byId();int removed=0;
                List<CustomFilterStore253.Entry> deleting=new ArrayList<>();for(String id:selected)if(current.containsKey(id))deleting.add(current.get(id));deleting.sort(Comparator.comparingInt(entry->entry.recipe!=null?0:entry.slot==0?1:2));
                for(CustomFilterStore253.Entry entry:deleting)try{store().remove(entry);removed++;selected.remove(entry.id);}catch(Exception failure){Log.e("CameraFilterManager","Delete failed: "+entry.id,failure);failures.add(entry.name+"："+failure.getMessage());}
                try{store().refresh();}catch(Exception failure){failures.add(failure.getMessage());}save();loaded=false;load();message("删除结果","已删除 "+removed+" 项"+(failures.isEmpty()?"":"\n"+String.join("\n",failures)));
            }).show();
        }
        void backup(int request,Uri uri){
            begin(request==WRITE?"创建相机设置备份":"恢复相机设置");io.execute(()->{try{
                String summary;
                if(request==WRITE)try(OutputStream output=activity(fragment).getContentResolver().openOutputStream(uri,"wt")){if(output==null)throw new IOException("无法创建备份文件");summary=CameraSettingsBackup253.write(activity(fragment),output);}
                else try(InputStream input=activity(fragment).getContentResolver().openInputStream(uri)){if(input==null)throw new IOException("无法读取备份文件");summary=CameraSettingsBackup253.restore(activity(fragment),input);}
                final String text=summary;main.post(()->{end();if(!attached())return;if(request==WRITE)message("备份完成",text);else{Dialog done=dialogs.builder(activity(fragment)).setTitle("恢复完成").setMessage(text+"\n关闭相机后重新打开即可生效。").setPositiveButton("关闭相机",(d,w)->CameraSettingsBackup253.finishForReload(activity(fragment))).create();done.setCancelable(false);done.show();}});
            }catch(Exception failure){main.post(()->{end();error(failure);});}});
        }
        void begin(String title){
            busy=true;Context context=activity(fragment);LinearLayout content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);int padding=Math.round(24*context.getResources().getDisplayMetrics().density);content.setPadding(padding,padding/2,padding,padding/2);
            progressBar=(ProgressBar)XposedHelpers.newInstance(type("miuix.androidbasewidget.widget.ProgressBar"),context,null);progressBar.setIndeterminate(true);content.addView(progressBar,new LinearLayout.LayoutParams(-1,-2));progressText=new TextView(context);progressText.setText("正在处理…");content.addView(progressText,new LinearLayout.LayoutParams(-1,-2));progressDialog=dialogs.builder(context).setTitle(title).setView(content).create();progressDialog.setCancelable(false);progressDialog.show();
        }
        void progress(int done,int total,String name){main.post(()->{if(progressDialog==null)return;progressBar.setIndeterminate(total<=0);if(total>0){progressBar.setMax(total);progressBar.setProgress(done);}progressText.setText(done+" / "+total+"\n"+name);});}
        void dismissProgress(){if(progressDialog!=null){progressDialog.dismiss();progressDialog=null;}}
        void end(){busy=false;dismissProgress();}
        void message(String title,String text){if(attached())dialogs.builder(activity(fragment)).setTitle(title).setMessage(text).setPositiveButton("确定",null).show();}
        void error(Exception failure){Log.e("CameraSettings","Settings operation failed",failure);message("操作未完成",failure.getMessage()==null?failure.getClass().getSimpleName():failure.getMessage());}
    }
}
