package com.aurora.camera.compat;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;
import android.util.Log;
import com.aurora.camera.compat.editor.more246.EditorMore246Repository;
import com.aurora.camera.compat.editor.more246.EditorMore246Repository.Materialized;
import com.aurora.camera.compat.editor.more246.EditorRecipe248;
import com.aurora.camera.compat.editor.EditorLeicaFilters292;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Imported LUTs and camera recipes use the editor's dynamically resolved native pipeline. */
public final class EditorMore246Hooks {
    private static final String TAG="AuroraEditorMore246";
    private static final String VM="com.miui.mediaeditor.photo.filter.viewmodel.PhotoFilterViewModel";
    private static final String CATEGORY_KEY="aurora_editor_more246";
    private static final int CATEGORY=24600, FIRST_RUNTIME_ID=0x24600000;
    private final EditorMore246Repository repository=new EditorMore246Repository();
    private final Method localCatalog, finalWriter, pathResolver, cleared, liveRender;
    private final Method renderBitmap;
    private final EditorRecipe248 recipeRenderer;
    private final boolean nativeCloud;
    private final EditorLeicaFilters292 leicaFilters;
    private final Method itemId, itemDegree, liveSet;
    private final Constructor<?> categoryConstructor, filterConstructor, entryConstructor, liveConstructor;
    private final Field app, writerVm, writerCategories, categoryId, categoryKey, categoryItems;
    private final Field entryItem, entryDegree, vmRepository, vmCategories, vmEntries, vmSelected, vmSelectedEvent;
    private final Field operationPayload,operationAction;
    private final Field entryStyle;
    private final Constructor<?> styleConstructor;
    private final Method styleScript;
    private final Object filterModule;
    private final Class<?> categoryType, entryType;
    private final Map<Object,Session> sessions=new WeakHashMap<>();
    private final Map<String,Binding> revisions=new LinkedHashMap<>();
    private final Map<Integer,Binding> byRuntimeId=new ConcurrentHashMap<>();
    private volatile List<Binding> current=Collections.emptyList();
    private int nextId=FIRST_RUNTIME_ID;

    private static final class Binding {
        final int id; final Materialized materialized;
        Binding(int id,Materialized materialized) { this.id=id; this.materialized=materialized; }
    }
    private static final class Session {
        List<Object> stockCategories;
        Session(List<Object> stock) { stockCategories=stock; }
    }

    public EditorMore246Hooks(Host h) throws ReflectiveOperationException {
        nativeCloud=h.usesNativeCloudFilter();
        leicaFilters=new EditorLeicaFilters292(h);
        Class<?> item=h.type("ud.a"); categoryType=h.type("ud.b"); entryType=h.type("ud.e");
        localCatalog=h.method("qd.i","a"); app=h.field("qd.i","a");
        finalWriter=h.method("Bd.i$a","invokeSuspend",Object.class);
        writerVm=h.field("Bd.i$a","a"); writerCategories=h.field("Bd.i$a","b");
        pathResolver=h.method("Ob.l","b",int.class);
        categoryId=h.field(categoryType,"a"); categoryKey=h.field(categoryType,"d"); categoryItems=h.field(categoryType,"b");
        categoryConstructor=nativeCloud ? categoryType.getDeclaredConstructor(List.class,String.class,String.class,String.class,int.class)
                : categoryType.getDeclaredConstructor(String.class,String.class,int.class,String.class,List.class);
        filterConstructor=nativeCloud ? h.type("ud.h").getDeclaredConstructor(String.class,int.class,int.class,int.class,int.class,String.class,int.class)
                : h.type("ud.h").getDeclaredConstructor(int.class,int.class,String.class,int.class,int.class,int.class,String.class,String.class,String.class);
        entryStyle=nativeCloud ? h.field(entryType,"styleParams") : null;
        styleConstructor=nativeCloud ? entryStyle.getType().getDeclaredConstructor() : null;
        styleScript=nativeCloud ? h.method(entryStyle.getType(),"a") : null;
        entryConstructor=nativeCloud ? entryType.getDeclaredConstructor(item,int.class,int.class,boolean.class,boolean.class,boolean.class,entryStyle.getType())
                : entryType.getDeclaredConstructor(item,int.class,int.class,boolean.class,boolean.class,boolean.class);
        itemId=h.method(item,"d"); itemDegree=h.method(item,"e");
        entryItem=h.field(entryType,"a"); entryDegree=h.field(entryType,"b");
        vmRepository=h.field(VM,"b"); vmCategories=h.field(VM,"l"); vmEntries=h.field(VM,"m");
        vmSelected=h.field(VM,"d"); vmSelectedEvent=h.field(VM,"i"); cleared=h.method(VM,"onCleared");
        Class<?> mutable=h.type("androidx.lifecycle.MutableLiveData");
        liveSet=nativeCloud ? h.method("androidx.lifecycle.LiveData","setValue",Object.class) : h.method(mutable,"setValue",Object.class);
        liveRender=h.method("pd.l","generateLiveRenderData",h.type("Gf.d"),Size.class,h.type("Oo.l"),boolean.class);
        operationPayload=h.field("Gf.d","c");
        operationAction=h.field("Gf.d","b");
        Class<?> layer=h.type("com.miui.mediaeditor.photo.filter.FilterLayer");
        renderBitmap=h.method(layer,"t",layer,CopyOnWriteArrayList.class,Bitmap.class);
        recipeRenderer=new EditorRecipe248(h);
        Class<?> module=h.type("Je.q$a"); filterModule=h.field(module,"a").get(null);
        liveConstructor=h.type("Je.q").getDeclaredConstructor(String.class,String.class,int.class,String.class,module);
        if (localCatalog.getReturnType()!=ArrayList.class || finalWriter.getReturnType()!=Object.class
                || pathResolver.getReturnType()!=String.class || categoryId.getType()!=int.class
                || categoryKey.getType()!=String.class || categoryItems.getType()!=(nativeCloud?List.class:Object.class)
                || writerCategories.getType()!=List.class || writerVm.getType()!=h.type(VM)
                || entryItem.getType()!=item || entryDegree.getType()!=int.class
                || vmRepository.getType()!=LinkedHashMap.class || vmSelected.getType()!=categoryType
                || vmCategories.getType()!=mutable || vmEntries.getType()!=mutable || vmSelectedEvent.getType()!=mutable
                || itemId.getReturnType()!=int.class || itemDegree.getReturnType()!=int.class
                || liveRender.getReturnType()!=List.class || cleared.getReturnType()!=void.class
                ||renderBitmap.getReturnType()!=void.class||!Modifier.isStatic(renderBitmap.getModifiers())||operationAction.getType()!=String.class)
            throw new NoSuchMethodException("EditorMore246 pinned raw ABI differs");
    }

    public void install() {
        leicaFilters.install();
        XposedBridge.hookMethod(localCatalog,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (!p.hasThrowable()) p.setResult(categories((List<?>)p.getResult()));
            }
        });
        XposedBridge.hookMethod(finalWriter,new StrictHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                writerCategories.set(p.thisObject,categories((List<?>)writerCategories.get(p.thisObject)));
            }
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable() || p.getResult()==null) return; // Native null means filter_list is not ready.
                Object vm=writerVm.get(p.thisObject);
                sessions.put(vm,new Session(stockCategories((List<?>)writerCategories.get(p.thisObject))));
                publish(vm,sessions.get(vm));
                repository.start((Context)app.get(null),rows -> {
                    try { update(rows); }
                    catch (ReflectiveOperationException failure) {
                        Log.e(TAG,"Could not publish pure LUT catalog",failure);
                        throw new IllegalStateException("EditorMore246 native publication failed",failure);
                    }
                });
            }
        });
        XposedBridge.hookMethod(pathResolver,new StrictHook() {
            @Override protected void before(MethodHookParam p) {
                Binding binding=byRuntimeId.get((Integer)p.args[0]);
                if (binding!=null) p.setResult(binding.materialized.file.getAbsolutePath());
            }
        });
        XposedBridge.hookMethod(liveRender,new StrictHook() {
            @Override protected void after(MethodHookParam p) throws Throwable {
                if (p.hasThrowable()) return;
                Object entry=operationPayload.get(p.args[0]);
                if (!entryType.isInstance(entry)) return;
                int id=(Integer)itemId.invoke(entryItem.get(entry));
                Binding binding=byRuntimeId.get(id); if (binding==null) return;
                if(binding.materialized.row.recipe!=null)throw new UnsupportedOperationException("相机配方目前仅支持普通照片编辑；动态照片/视频渲染未适配");
                int degree=entryDegree.getInt(entry);
                // Stock routes the display names 黑柔/白柔 to mist. Imported names remain display-only.
                p.setResult(Collections.singletonList(liveConstructor.newInstance(binding.materialized.file.getAbsolutePath(),
                        "aurora-more246:"+id,degree,"",filterModule)));
            }
        });
        XposedBridge.hookMethod(renderBitmap,new StrictHook(){
            @Override protected void before(MethodHookParam p)throws Throwable{
                List<?> operations=(List<?>)p.args[1];if(operations.isEmpty())return;
                Object operation=operations.get(operations.size()-1),entry=operationPayload.get(operation);if(!entryType.isInstance(entry))return;
                Binding binding=byRuntimeId.get((Integer)itemId.invoke(entryItem.get(entry)));
                if(binding==null||binding.materialized.row.recipe==null)return;
                String action=(String)operationAction.get(operation);if(!"APPLY_FILTER".equals(action)&&!"UPDATE_PROGRESS".equals(action))return;
                String palette=null;
                if(nativeCloud) {
                    Object style=entryStyle.get(entry);
                    palette=((String)styleScript.invoke(style)).substring(1);
                }
                recipeRenderer.apply((Bitmap)p.args[2],binding.materialized.file,binding.materialized.row.recipe,entryDegree.getInt(entry),palette);
                p.setResult(null);
            }
        });
        XposedBridge.hookMethod(cleared,new StrictHook() {
            @Override protected void after(MethodHookParam p)throws Throwable {
                sessions.remove(p.thisObject); if (sessions.isEmpty()) {repository.stop();recipeRenderer.close();}
            }
        });
    }

    private void update(List<Materialized> rows) throws ReflectiveOperationException {
        List<Binding> ready=new ArrayList<>();
        for (Materialized materialized:rows) {
            String identity=materialized.row.id+"\n"+materialized.row.revision+"\n"+materialized.row.effectIdentity();
            Binding previous=revisions.get(identity);
            int id=previous==null ? nextId++ : previous.id;
            Binding binding=new Binding(id,materialized);
            revisions.put(identity,binding); byRuntimeId.put(id,binding); ready.add(binding);
        }
        current=Collections.unmodifiableList(ready);
        for (Map.Entry<Object,Session> session:new ArrayList<>(sessions.entrySet()))
            publish(session.getKey(),session.getValue());
        Log.i(TAG,"Published pure LUT catalog rows="+ready.size());
    }

    private boolean ours(Object value) throws IllegalAccessException {
        return value!=null && categoryType.isInstance(value) && CATEGORY_KEY.equals(categoryKey.get(value));
    }
    private List<Object> stockCategories(List<?> input) throws IllegalAccessException {
        List<Object> stock=new ArrayList<>();
        for (Object value:input) if (!ours(value)) stock.add(value);
        return stock;
    }
    private Object nativeItem(Binding binding) throws ReflectiveOperationException {
        if(nativeCloud)return filterConstructor.newInstance(binding.materialized.row.name,binding.id,0,0,CATEGORY,binding.materialized.file.getAbsolutePath(),100);
        return filterConstructor.newInstance(binding.id,0,binding.materialized.row.name,0,CATEGORY,100,
                binding.materialized.file.getAbsolutePath(),"LUT","");
    }
    private ArrayList<Object> categories(List<?> input) throws ReflectiveOperationException {
        ArrayList<Object> result=new ArrayList<>(stockCategories(input));
        for (Object category:result) {
            if (categoryId.getInt(category)==CATEGORY) throw new IllegalStateException("Stock category ID collides with More246");
            for (Object item:(List<?>)categoryItems.get(category)) {
                int id=(Integer)itemId.invoke(item);
                if (byRuntimeId.containsKey(id)) throw new IllegalStateException("Stock filter ID collides with More246: "+id);
            }
        }
        if (!current.isEmpty()) {
            List<Object> items=new ArrayList<>(); for (Binding binding:current) items.add(nativeItem(binding));
            result.add(nativeCloud ? categoryConstructor.newInstance(items,"更多",CATEGORY_KEY,"",CATEGORY)
                    : categoryConstructor.newInstance("更多",CATEGORY_KEY,CATEGORY,"",items));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void publish(Object vm,Session session) throws ReflectiveOperationException {
        List<Object> categories=categories(session.stockCategories);
        Map<String,Object> map=(Map<String,Object>)vmRepository.get(vm);
        Object original=map.get("filter_list");
        if (!(original instanceof List)) throw new IllegalStateException("Native filter_list is unavailable");
        List<Object> entries=new ArrayList<>(); Map<Integer,Object> previous=new HashMap<>();
        for (Object value:(List<?>)original) {
            int id=(Integer)itemId.invoke(entryItem.get(value));
            if (byRuntimeId.containsKey(id)) previous.put(id,value); else entries.add(value);
        }
        for (Binding binding:current) {
            Object old=previous.get(binding.id);
            int degree=old==null ? 100 : entryDegree.getInt(old);
            entries.add(nativeCloud ? entryConstructor.newInstance(nativeItem(binding),degree,17,false,false,false,
                    old==null?styleConstructor.newInstance():entryStyle.get(old))
                    : entryConstructor.newInstance(nativeItem(binding),degree,17,false,false,false));
        }
        map.put("filter_list",entries);
        liveSet.invoke(vmCategories.get(vm),categories); liveSet.invoke(vmEntries.get(vm),entries);
        Object selected=vmSelected.get(vm);
        if (ours(selected)) {
            Object replacement=null;
            for (Object category:categories) if (ours(category)) replacement=category;
            if (replacement==null && !categories.isEmpty()) replacement=categories.get(0);
            if (replacement!=null) { vmSelected.set(vm,replacement); liveSet.invoke(vmSelectedEvent.get(vm),replacement); }
        }
    }
}
