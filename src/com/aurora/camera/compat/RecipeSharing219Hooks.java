package com.aurora.camera.compat;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import com.aurora.camera.compat.filter.RecipeCatalog219;
import com.aurora.camera.compat.filter.RecipeCatalog219.Recipe;
import com.aurora.camera.compat.filter.FilterWorkspace215;
import com.aurora.camera.compat.filter.LutStore229;
import com.aurora.camera.compat.filter.CamStylePreview249;
import com.aurora.camera.compat.filter.WorkspaceFilterOrder270;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Project recipes into the native basic filter UI, with native filter IDs intact. */
public final class RecipeSharing219Hooks implements HookPlan {
    private static RecipeSharing219Hooks instance;
    private final FilterWorkspace215 effectWorkspace;
    private final CamStylePreview249 nativePreview;
    private static final String ROW="219.recipe",PHOTO="219.photo-render";
    private final Method global,data,facing,current,init,adapter,choose,index,enabled,rowFactory,componentValue,filterValue,engine,add,remove,palette,softLight,update,activeConfig,activeModule;
    private final Method cloudRefresh,filterViewInit,restoreItems,notifyAdapter;
    private final Method frontEntry,basicEntry,showPanel;
    private final Field entryMode,entryClick;
    private final Method labelBind,labelName,thumbnailTask,componentItems;
    private final Field taskKind,taskOwner;
    private final ThreadLocal<Object> thumbnailOwner=new ThreadLocal<>();
    private final Method[] renderCreate,renderDestroy;
    private final Method[] styleConsumers;
    private final Method controller;
    private final Field metadataName,metadataJson;
    private final Field intent,fragmentMode,component,fullComponentItems,rows,adapterRows,adapterOwner,callback,pictureNode;
    private final Field rowValue,rowName,rowLabel,rowData;
    private final Field filterView,filterViewRows,filterViewIndex,selectedIndex,clickIndex;
    private final Class<?> fragmentClass,filterClass;
    private final Class<?>[] colors=new Class<?>[8];
    private final Field[] copyFields;
    private final Map<String,Recipe> selected=new HashMap<>();
    private final ThreadLocal<Integer> styleDepth=ThreadLocal.withInitial(()->0);
    private volatile boolean restored;
    private volatile String renderKey;
    private RecipeSharing219Hooks(Host h)throws ReflectiveOperationException {
        nativePreview=new CamStylePreview249(h);
        effectWorkspace=new FilterWorkspace215(h);
        global=h.method("com.xiaomi.camera.basic.Global","getContext");data=h.method("g2.a","g");facing=h.method("u2.Y","B");current=h.method("u2.Y","D",int.class);intent=h.field("u2.Y","u");
        frontEntry=h.method("Z4.m","g",boolean.class);basicEntry=h.method("Z4.m","a");entryMode=h.field("Z4.m","b");entryClick=h.field("Z4.a","c");
        showPanel=h.method("R6.D","Pa",int.class,List.class,String.class);
        labelBind=h.method("com.android.camera.fragment.I","m",h.type("com.android.camera.data.data.d"));labelName=h.method("com.android.camera.fragment.I","h",String.class);
        thumbnailTask=h.method("Is.a","run");taskKind=h.field("Is.a","a");taskOwner=h.field("Is.a","b");componentItems=h.method("r2.a","getItems");
        fragmentClass=h.type("com.android.camera.fragment.j0");filterClass=h.type("r2.t");
        init=h.method("P9.i","Ts");adapter=h.method(fragmentClass,"Ns");choose=h.method(fragmentClass,"et",int.class,String.class,boolean.class);index=h.method("P9.i","Qs");enabled=h.method("com.android.camera.fragment.a","isEnableClick");
        fragmentMode=h.field("com.android.camera.fragment.h","mCurrentMode");component=h.field("P9.i","P");fullComponentItems=h.field("com.android.camera.data.data.c","mItems");rows=h.field("P9.i","R");adapterRows=h.field("com.android.camera.fragment.d","f");adapterOwner=h.field("P9.i","O");
        cloudRefresh=h.method("P9.i","Ks",h.type("P9.i"),java.util.concurrent.ConcurrentHashMap.class);
        filterViewInit=h.method("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","c",h.type("com.android.camera.data.data.c"),int.class,h.type("P9.i"));
        restoreItems=h.method("r2.a","n",int.class,ArrayList.class);notifyAdapter=h.method("androidx.recyclerview.widget.RecyclerView$g","notifyDataSetChanged");
        filterView=h.field("P9.i","M");filterViewRows=h.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","a");filterViewIndex=h.field("com.android.camera2.compat.theme.custom.cv.FilterSelectViewCV","b");
        selectedIndex=h.field("P9.i","V");clickIndex=h.field("P9.i","c0");
        Class<?> row=h.type("com.android.camera.data.data.d");rowFactory=h.method("v2.m0","y");rowValue=h.field(row,"r");rowName=h.field(row,"o");rowLabel=h.field(row,"l");rowData=h.field(row,"a");
        ArrayList<Field> fields=new ArrayList<>();for(Field field:row.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);fields.add(field);}copyFields=fields.toArray(new Field[0]);
        String[] names={"f1","t0","r0","d1","h1","R0","X0","j1"};for(int i=0;i<colors.length;i++)colors[i]=h.type("r2."+names[i]);
        componentValue=h.method("com.android.camera.data.data.c","getComponentValue",int.class);filterValue=h.method("v2.U","getComponentValue",int.class);
        String[] renderOwners={"com.android.camera.features.mode.capture.CaptureModule","com.android.camera.features.mode.street.StreetModule"};
        renderCreate=new Method[2];renderDestroy=new Method[2];for(int i=0;i<2;i++){renderCreate[i]=h.method(renderOwners[i],"onRenderEngineCreate");renderDestroy[i]=h.method(renderOwners[i],"onRenderEngineDestroy");}
        callback=h.field("com.android.camera.module.u","mCallback");engine=h.method("com.android.camera.module.b0","fi");pictureNode=h.field("Ku.d","P");add=h.method("F8.j","V0",h.type("Ku.d"));remove=h.method("F8.j","c1",h.type("Ku.d"));
        palette=h.method("com.android.camera.data.data.F","R0");softLight=h.method("v2.j0","s",int.class);
        update=h.method("R6.D","updatePreferenceInWorkThread",int[].class);activeConfig=h.method("R6.D","a");activeModule=h.method("r6.W","ja");
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");metadataName=h.field("com.xiaomi.camera.effect.EffectController","Y");metadataJson=h.field("com.xiaomi.camera.effect.EffectController","X");
        String[] updates={"updateStyleTone","updateStyleTune","updateStyleTemperature","updateStyleTexture","updateStyleVibrance","updateStyleNoise","updateStyleSoftLight","updateStyleVignette"};
        styleConsumers=new Method[9];for(int i=0;i<8;i++)styleConsumers[i]=h.method("com.android.camera.module.Camera2Module",updates[i]);styleConsumers[8]=h.method("com.xiaomi.camera.effect.EffectController","c");
    }
    public static HookPlan resolve(Host h)throws ReflectiveOperationException{instance=new RecipeSharing219Hooks(h);return instance;}
    public static String selectedEffectsKey(){
        if(instance==null)return null; // No recipe context before feature resolution.
        try{
            int mode=instance.mode();if(mode!=163&&mode!=225&&mode!=229)return null;
            Recipe recipe=instance.active();return recipe==null?instance.effectWorkspace.selectedEffectsKey():LutStore229.recipeKey(recipe.kind,recipe.id);
        }catch(Exception failure){throw new IllegalStateException("Cannot resolve selected custom filter",failure);}
    }
    public String id(){return "220.shared-photo-recipes";}
    private Context context()throws Exception{return (Context)global.invoke(null);}
    private int mode()throws Exception{Object state=data.invoke(null);return (Integer)current.invoke(state,intent.getInt(state));}
    private boolean front()throws Exception{return (Integer)facing.invoke(data.invoke(null))==1;}
    static boolean supports(int mode,boolean front){return mode==229||mode==225||(mode==163&&front);}
    private String key()throws Exception{return mode()+":"+(front()?1:0);}
    private boolean ownsStyle()throws Exception{return supports(mode(),front())&&key().equals(renderKey);}
    private List<Recipe> catalog()throws Exception {
        Map<String,java.util.Set<String>> hidden=new HashMap<>();SharedPreferences visibility=context().getSharedPreferences("aurora_filter_workspace_v2",0);
        for(String kind:new String[]{"MiCamStyleRear","MiCamStyleFront"})hidden.put(kind,new HashSet<>(visibility.getStringSet("hidden:"+kind,Collections.emptySet())));
        return RecipeCatalog219.scan(new File(context().getFilesDir(),"Workspace"),hidden);
    }
    private Recipe active()throws Exception {
        if(!supports(mode(),front()))return null;
        synchronized(selected){
            if(!restored){SharedPreferences pref=context().getSharedPreferences("aurora_shared_recipes219",0);for(Recipe r:catalog())for(String k:new String[]{"225:0","225:1","229:0","229:1","163:1"})if(r.identity().equals(pref.getString(k,"")))selected.put(k,r);restored=true;}
            Recipe recipe=selected.get(key());if(recipe==null)return null;
            if(recipe.directory.isDirectory())return recipe;
            selected.remove(key());context().getSharedPreferences("aurora_shared_recipes219",0).edit().remove(key()).apply();return null;
        }
    }
    private void select(Recipe recipe)throws Exception {
        synchronized(selected){active();String key=key();if(recipe==null)selected.remove(key);else selected.put(key,recipe);context().getSharedPreferences("aurora_shared_recipes219",0).edit().putString(key,recipe==null?"":recipe.identity()).apply();}
    }
    @SuppressWarnings("unchecked") private void project(Object fragment)throws Exception {
        if(!fragmentClass.isInstance(fragment)||!supports(fragmentMode.getInt(fragment),front())||!filterClass.isInstance(component.get(fragment)))return;
        ArrayList<Object> output=new ArrayList<>();Map<String,Object> bases=new HashMap<>();
        for(Object row:(List<Object>)rows.get(fragment))if(XposedHelpers.getAdditionalInstanceField(row,ROW)==null)output.add(row);
        for(Object row:(List<?>)fullComponentItems.get(component.get(fragment)))bases.put((String)rowValue.get(row),row);
        int nativeCount=output.size();List<Recipe> catalog=catalog();Recipe active=active();boolean retained=active==null;
        for(Recipe recipe:catalog){Object base=bases.get(String.valueOf(recipe.filter));if(base==null)continue;
            Object row=rowFactory.invoke(null);for(Field field:copyFields)field.set(row,field.get(base));rowData.set(row,null);rowLabel.setInt(row,-1);rowName.set(row,recipe.name);
            XposedHelpers.setAdditionalInstanceField(row,ROW,recipe);output.add(row);if(active!=null&&active.identity().equals(recipe.identity())){retained=true;synchronized(selected){selected.put(key(),recipe);}}
        }
        int currentMode=fragmentMode.getInt(fragment);
        if(currentMode==225||currentMode==229){
            Map<String,Integer> order=new HashMap<>();
            for(WorkspaceFilterOrder270.Row row:WorkspaceFilterOrder270.cached(context(),"MiCamStyleRear")){
                String id=row.official?"filter:"+row.filter:"recipe:MiCamStyleRear/"+row.identity;
                order.putIfAbsent(id,order.size());
            }
            Map<Object,Integer> ranks=new java.util.IdentityHashMap<>();
            for(Object row:output){
                Recipe recipe=(Recipe)XposedHelpers.getAdditionalInstanceField(row,ROW);
                String value=(String)rowValue.get(row);
                ranks.put(row,recipe==null&&String.valueOf(RecipeCatalog219.ORIGINAL).equals(value)?-1:
                        order.getOrDefault(recipe==null?"filter:"+value:"recipe:"+recipe.identity(),Integer.MAX_VALUE));
            }
            output.sort(java.util.Comparator.comparingInt(ranks::get));
        }
        if(!retained)select(null);rows.set(fragment,output);
        Object listAdapter=adapterOwner.get(fragment);if(listAdapter!=null)adapterRows.set(listAdapter,new ArrayList<>(output));
        Object view=filterView.get(fragment);if(view!=null)filterViewRows.set(view,new ArrayList<>(output));
        android.util.Log.i("AuroraCameraCompat","recipe220 project mode="+fragmentMode.getInt(fragment)+" front="+front()+" source="+catalog.size()+" appended="+(output.size()-nativeCount)+" visible="+output.size());
    }
    private int recipeIndex(Object fragment)throws Exception {
        Recipe current=active();if(current==null)return -1;
        List<?> list=(List<?>)rows.get(fragment);for(int i=0;i<list.size();i++){Object recipe=XposedHelpers.getAdditionalInstanceField(list.get(i),ROW);if(recipe instanceof Recipe&&((Recipe)recipe).identity().equals(current.identity()))return i;}
        return -1;
    }
    private void synchronizeVisible(Object fragment)throws Exception {
        if(!fragmentClass.isInstance(fragment)||!supports(fragmentMode.getInt(fragment),front()))return;
        project(fragment);int position=recipeIndex(fragment);if(position<0)position=(Integer)index.invoke(fragment);selectedIndex.setInt(fragment,position);
        ((java.util.concurrent.atomic.AtomicInteger)clickIndex.get(fragment)).set(position);
        Object view=filterView.get(fragment);if(view!=null)filterViewIndex.setInt(view,position);
        Object listAdapter=adapterOwner.get(fragment);if(listAdapter!=null)notifyAdapter.invoke(listAdapter);
    }
    private void render(Object module,boolean create)throws Exception {
        Object owner=callback.get(module);if(owner==null)return;Object target=engine.invoke(owner);if(target==null)return;
        (create?add:remove).invoke(target,pictureNode.get(null));XposedHelpers.setAdditionalInstanceField(module,PHOTO,create);renderKey=create?key():null;
    }
    @SuppressWarnings("unchecked") private void refresh()throws Exception {
        java.util.Optional<Object> owner=(java.util.Optional<Object>)activeConfig.invoke(null);
        if(owner.isPresent()){
            java.util.Optional<Object> module=(java.util.Optional<Object>)activeModule.invoke(owner.get());
            if(active()!=null&&module.isPresent())render(module.get(),true);
            update.invoke(owner.get(),new int[]{2,105,106,107,108,121,160,161,159});
        }
        Recipe active=active();Object effect=controller.invoke(null);metadataName.set(effect,active==null?"":active.name);metadataJson.set(effect,"");
    }
    public void install(){
        XposedBridge.hookMethod(labelBind,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;Object recipe=XposedHelpers.getAdditionalInstanceField(p.args[0],ROW);
            if(recipe instanceof Recipe){Recipe r=(Recipe)recipe;labelName.invoke(p.thisObject,new LutStore229(context()).label(LutStore229.recipeKey(r.kind,r.id),r.name));}
        }});
        // The native GL task reads P.getItems(), not the adapter's list. Expose
        // the projection only inside this exact thumbnail task; master catalogs
        // and native restore/save callers continue to see native rows.
        XposedBridge.hookMethod(thumbnailTask,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                Object owner=taskOwner.get(p.thisObject);
                if(taskKind.getInt(p.thisObject)!=3||!fragmentClass.isInstance(owner)||!supports(fragmentMode.getInt(owner),front()))return;
                p.setObjectExtra("220.thumbnail.previous",thumbnailOwner.get());p.setObjectExtra("220.thumbnail.scope",true);thumbnailOwner.set(owner);
            }
            @Override protected void after(MethodHookParam p){
                if(p.getObjectExtra("220.thumbnail.scope")==null)return;Object previous=p.getObjectExtra("220.thumbnail.previous");
                if(previous==null)thumbnailOwner.remove();else thumbnailOwner.set(previous);
            }
        });
        XposedBridge.hookMethod(componentItems,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            Object owner=thumbnailOwner.get();if(!p.hasThrowable()&&owner!=null&&component.get(owner)==p.thisObject)p.setResult(rows.get(owner));
        }});
        // Selfie CamStyle opens U9.M, not j0. Use the camera's existing basic
        // filter button and Pa transaction, with an explicit filter-only tab.
        // This avoids importing rear-only camera controls with workspace items.
        XposedBridge.hookMethod(frontEntry,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            if(entryMode.getInt(p.thisObject)!=163||!front())return;
            Object button=basicEntry.invoke(p.thisObject);
            entryClick.set(button,(View.OnClickListener)view->{
                try{
                    java.util.Optional<?> config=(java.util.Optional<?>)activeConfig.invoke(null);
                    if(config.isPresent())showPanel.invoke(config.get(),5,Collections.singletonList(rowFactory.invoke(null)),"7");
                }catch(ReflectiveOperationException e){throw new IllegalStateException("Front recipe filter panel failed",e);}
            });
            p.setResult(button);
        }});
        XposedBridge.hookMethod(init,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())project(p.thisObject);}});
        XposedBridge.hookMethod(adapter,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable()&&supports(fragmentMode.getInt(p.thisObject),front()))adapterRows.set(p.getResult(),new ArrayList<>((List<?>)rows.get(p.thisObject)));}});
        XposedBridge.hookMethod(index,new StrictHook(){
          @Override protected void before(MethodHookParam p)throws Throwable{project(p.thisObject);}
          @Override protected void after(MethodHookParam p)throws Throwable{
            if(!fragmentClass.isInstance(p.thisObject)||!supports(fragmentMode.getInt(p.thisObject),front()))return;
            int selected=recipeIndex(p.thisObject);if(selected>=0)p.setResult(selected);
        }});
        XposedBridge.hookMethod(cloudRefresh,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())synchronizeVisible(p.args[0]);}});
        XposedBridge.hookMethod(filterViewInit,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable())synchronizeVisible(p.args[2]);}});
        XposedBridge.hookMethod(restoreItems,new StrictHook(){@Override protected void before(MethodHookParam p){
            if(!filterClass.isInstance(p.thisObject))return;ArrayList<Object> nativeRows=new ArrayList<>();boolean projected=false;
            for(Object row:(List<?>)p.args[1])if(XposedHelpers.getAdditionalInstanceField(row,ROW)==null)nativeRows.add(row);else projected=true;
            if(projected)p.args[1]=nativeRows;
        }});
        XposedBridge.hookMethod(choose,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable {
                if(!supports(fragmentMode.getInt(p.thisObject),front())||!(Boolean)enabled.invoke(p.thisObject))return;
                Object row=((List<?>)rows.get(p.thisObject)).get((Integer)p.args[0]);Recipe recipe=(Recipe)XposedHelpers.getAdditionalInstanceField(row,ROW);
                if(recipe!=null&&!recipe.directory.isDirectory())throw new IOException("所选滤镜已被删除，请重新打开滤镜面板");
                p.setObjectExtra("219.selection.before",active());select(recipe);p.setObjectExtra("219.selection",true);
            }
            @Override protected void after(MethodHookParam p)throws Throwable {
                if(p.getObjectExtra("219.selection")==null)return;
                if(p.hasThrowable()){select((Recipe)p.getObjectExtra("219.selection.before"));return;}refresh();
            }
        });
        StrictHook value=new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            if((Integer)p.args[0]!=mode())return;Recipe recipe=active();
            if(filterClass.isInstance(p.thisObject)){if(recipe!=null)p.setResult(String.valueOf(recipe.filter));return;}
            for(int i=0;i<colors.length;i++)if(colors[i].isInstance(p.thisObject)){
                String key=selectedEffectsKey();int[] configured=key==null?null:new LutStore229(context()).effects(key);
                if(configured!=null&&(i==5||i==6))p.setResult(String.valueOf(i==5?LutStore229.styleNoise(configured[0]):LutStore229.styleSoftLight(configured[1])));
                else if(recipe!=null)p.setResult(String.valueOf(recipe.colors[i]));else if(ownsStyle())p.setResult("0");return;
            }
        }};
        XposedBridge.hookMethod(componentValue,value);XposedBridge.hookMethod(filterValue,value);
        for(Method method:styleConsumers)XposedBridge.hookMethod(method,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{String key=selectedEffectsKey();if(active()!=null||ownsStyle()||(key!=null&&new LutStore229(context()).effects(key)!=null)){styleDepth.set(styleDepth.get()+1);p.setObjectExtra("219.style.scope",true);}}
            @Override protected void after(MethodHookParam p){if(p.getObjectExtra("219.style.scope")==null)return;int depth=styleDepth.get()-1;if(depth==0)styleDepth.remove();else styleDepth.set(depth);}
        });
        // F.R0 also controls the CamStyle menu. Qualify only the actual pixel
        // parameter consumers; selecting a recipe must not replace the native UI.
        XposedBridge.hookMethod(palette,new StrictHook(){@Override protected void before(MethodHookParam p){if(styleDepth.get()>0)p.setResult(true);}});
        XposedBridge.hookMethod(softLight,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(styleDepth.get()>0&&(Integer)p.args[0]==mode())p.setResult(true);}});
        for(Method method:renderCreate)XposedBridge.hookMethod(method,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{if(!p.hasThrowable()&&active()!=null)render(p.thisObject,true);}});
        for(Method method:renderDestroy)XposedBridge.hookMethod(method,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{if(Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(p.thisObject,PHOTO)))render(p.thisObject,false);}});
        nativePreview.install();
    }
}
