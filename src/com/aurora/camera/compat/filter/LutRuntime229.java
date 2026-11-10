package com.aurora.camera.compat.filter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseArray;
import com.aurora.camera.compat.*;
import de.robv.android.xposed.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;

/** LSP-only extension of the pinned native LUT protocol, including MIVI delivery. */
public final class LutRuntime229 {
    public static final class Entry {
        public final int id,slot;public final String key,name;public final int[] defaults;
        Entry(int id,int slot,String key,String name){this(id,slot,key,name,new int[]{0,0});}
        Entry(int id,int slot,String key,String name,int[] defaults){this.id=id;this.slot=slot;this.key=key;this.name=name;this.defaults=defaults.clone();}
    }
    private final Host h;private final Options229 options;
    private final LutWorkspace237 workspace;
    private ImportedVideoLut256 video;
    private final Method values,valueOf,factory,byCategory,bitmap,publish,write,controller,catalog,remap,attributes;
    private final Field enumCategory,enumParameters,enumTokens,rowId,rowName,rowDynamic,attrNoise,attrBlack,attrWhite,attrParams,lookupName,lookupParameters;
    private final Field tagId,tagSoft,coreOwner,attributesCache,nativeCatalogs;
    private final Method core;
    private final int noFilter;
    private Object[] base,expanded;private int first;
    private final ThreadLocal<Boolean> indexing=new ThreadLocal<>();
    private final ThreadLocal<Boolean> workspaceCatalog=new ThreadLocal<>();
    public LutRuntime229(Host h)throws ReflectiveOperationException{
        this.h=h;options=new Options229(h);workspace=new LutWorkspace237(h,this);values=h.method("o3.d","values");valueOf=h.method("o3.d","valueOf",String.class);noFilter=h.field("i3.b","P").getInt(null);
        enumCategory=h.field("o3.d","a");enumParameters=h.field("o3.d","b");enumTokens=h.field("o3.d","c");
        factory=h.method("xi.h0","f",h.type("o3.d"),boolean.class,int.class,int.class);byCategory=h.method("xi.h0","h",h.type("o3.c"));
        bitmap=h.method("Nu.g","b",Context.class,String.class);
        publish=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","lambda$saveLutToCameraFileDir$1",boolean.class,String.class,String.class,String.class);
        write=h.method("com.xiaomi.camera.mivi.filter.MIVILutSaver","writeLutToDir",String.class,String.class,byte[].class);
        controller=h.method("com.xiaomi.camera.effect.EffectController","u");catalog=h.method("com.xiaomi.camera.effect.EffectController","q",int.class);
        attributesCache=h.field("com.xiaomi.camera.effect.EffectController","U");
        nativeCatalogs=h.field("com.xiaomi.camera.effect.EffectController","R");
        remap=h.method("v2.U","r",ArrayList.class,int.class,int.class);
        rowId=h.field("com.android.camera.data.data.d","r");rowName=h.field("com.android.camera.data.data.d","l");rowDynamic=h.field("com.android.camera.data.data.d","o");
        attributes=h.method("si.f","e",int.class,int.class,int.class,boolean.class);
        attrNoise=h.field("Mu.d","f");attrBlack=h.field("Mu.d","l");attrWhite=h.field("Mu.d","m");attrParams=h.field("Mu.d","i");
        lookupName=h.field("o3.b","j");lookupParameters=h.field("o3.b","l");
        tagId=h.field("com.xiaomi.camera.mivi.filter.MIVIRenderTag","mFilterId");tagSoft=h.field("com.xiaomi.camera.mivi.filter.MIVIRenderTag","mSoftFocusType");
        core=h.method("com.xiaomi.camera.mivi.filter.MIVIRenderTag$TagForRenderCore","initFilterParams",Map.class,h.type("o3.b"));coreOwner=h.field("com.xiaomi.camera.mivi.filter.MIVIRenderTag$TagForRenderCore","this$0");
    }
    public LutStore229 store(){return new LutStore229(options.context());}
    public String customName(int id){int slot=slot(id);if(slot==0)return null;LutStore229 store=store();return store.label(LutStore229.token(slot),store.name(slot));}
    public void attachVideo(ImportedVideoLut256 video){this.video=video;}
    public ImportedVideoLut256 video(){return video;}
    public int restoreBundled(CustomFilterStore253.Progress progress)throws java.io.IOException{return store().restoreBundled(h.moduleApkPath,progress);}
    public void showRestoredBundled()throws java.io.IOException,ReflectiveOperationException{workspace.restoreBundledVisibility();refresh();}
    int slot(int id){if(id==noFilter)return 0;int ordinal=id&65535;return ordinal>=first&&ordinal<first+LutStore229.SLOTS?ordinal-first+1:0;}
    public int id(int slot){return (2<<16)|(first+slot-1);}
    List<?> workspaceCatalog(int category){
        Boolean previous=workspaceCatalog.get();workspaceCatalog.set(true);
        try{return (List<?>)FilterWorkspace215.call(catalog,FilterWorkspace215.call(controller,null),category);}
        finally{if(previous==null)workspaceCatalog.remove();else workspaceCatalog.set(previous);}
    }
    public String identity(int id){int slot=slot(id);if(slot>0)return LutStore229.token(slot);int index=id&65535;return index<base.length?((Enum<?>)base[index]).name():null;}
    private int[] effects(int id){
        LutStore229 store=store();String profile=RecipeSharing219Hooks.selectedEffectsKey();
        // A configured CamStyle recipe uses its own native StyleAdjust stage.
        // Disable additional LUT noise/mist so a shared base LUT is not applied twice.
        if(profile!=null&&store.effects(profile)!=null)return new int[]{0,0};
        String key=identity(id);return key==null?null:store.effects(key);
    }
    public int[] effects(Entry entry){int[] saved=store().effects(entry.key);return saved==null?entry.defaults.clone():saved;}
    private float[] parameters(float[] nativeValues,int[] effects){float[] result=nativeValues.clone();result[4]=new float[]{0f,.2f,.25f,.3f}[effects[0]];return result;}
    public void refresh()throws ReflectiveOperationException{
        LutStore229 store=store();for(int i=1;i<=LutStore229.SLOTS;i++)enumTokens.set(expanded[first+i-1],new String[]{store.resourceToken(i)});
        Object owner=controller.invoke(null);((Map<?,?>)attributesCache.get(owner)).clear();
        workspace.refreshLoaded();
        video.refreshLoaded();
        CameraLutShare246Hooks.requestSnapshot();
    }
    public List<Entry> entries()throws ReflectiveOperationException{
        LinkedHashMap<String,Entry> rows=new LinkedHashMap<>();Object owner=controller.invoke(null);Context context=options.context();LutStore229 store=store();
        @SuppressWarnings("unchecked") SparseArray<ArrayList<?>> all=(SparseArray<ArrayList<?>>)nativeCatalogs.get(owner);
        Field name=h.field("i3.b","c"),group=h.field("i3.b","o");Method id=h.method("i3.b","a");
        // 1770 registers still LUT catalogs 2/7/10. Category 1 is the no-effect
        // render namespace and has no catalog. Read the complete native source,
        // rather than a visible/filtered catalog projection.
        for(int category:new int[]{2,7,10}){
            List<?> nativeRows=all.get(category);if(nativeRows==null)continue;
            for(Object row:nativeRows){
                int value=(Integer)id.invoke(row);String key=identity(value);
                if(key==null||"N_ORIGINAL".equals(key)||"LEICA".equals(group.get(row)))continue;
                int resource=name.getInt(row);if(resource<=0)continue;
                rows.putIfAbsent(key,new Entry(value,0,key,store.label(key,context.getString(resource))));
            }
        }
        if(rows.isEmpty())throw new IllegalStateException("Native still LUT catalogs 2/7/10 contain no named non-Leica entries");
        try{
            for(RecipeCatalog219.Recipe recipe:RecipeCatalog219.scan(new java.io.File(context.getFilesDir(),"Workspace"),Collections.emptyMap())){
                String key=LutStore229.recipeKey(recipe.kind,recipe.id);int grain=recipe.colors[5],soft=recipe.colors[6];
                int[] defaults={grain<=0?0:grain<=20?1:grain<=25?2:3,soft<=0?0:soft<=3?1:2};
                rows.put(key,new Entry(recipe.filter,0,key,store.label(key,recipe.name),defaults));
            }
        }catch(java.io.IOException failure){throw new IllegalStateException("Cannot read custom filter workspace",failure);}
        for(int slot:store.slots()){String key=LutStore229.token(slot);rows.put(key,new Entry(id(slot),slot,key,store.label(key,store.name(slot))));}
        return new ArrayList<>(rows.values());
    }
    private List<Object> appendCatalog(int category,List<?> nativeRows)throws ReflectiveOperationException{
        // Optional/unsupported native catalogs keep their original null/empty
        // result; never fabricate category 1 or change another feature's rows.
        if(nativeRows==null||nativeRows.isEmpty())return null;
        if(category==9||category==12){
            ArrayList<Object> rows=new ArrayList<>(nativeRows);
            @SuppressWarnings("unchecked") SparseArray<ArrayList<?>> catalogs=(SparseArray<ArrayList<?>>)nativeCatalogs.get(controller.invoke(null));
            LeicaLooks273.applyVideo(h,rows,catalogs.get(12));return rows;
        }
        if(category!=2&&category!=7&&category!=10)return null;
        ArrayList<Object> rows=new ArrayList<>(nativeRows);
        if(category==2||category==10)LeicaLooks273.apply(h,category,rows);
        int order=rows.size();
        Constructor<?> constructor=h.type("i3.b").getDeclaredConstructor(int.class,int.class,int.class,int.class,int.class);constructor.setAccessible(true);
        int label=h.field("i3.b","c").getInt(rows.get(0)),icon=h.field("i3.b","e").getInt(rows.get(0));
        LutStore229 store=store();
        for(int slot:Boolean.TRUE.equals(workspaceCatalog.get())?store.configuredSlots():store.slots())rows.add(constructor.newInstance(category,first+slot-1,label,icon,order++));
        // i3.b.o is a NORMAL/LEICA/FILM grouping token, not display text.
        // Custom display names are applied to the mapped component rows below.
        return rows;
    }
    public void install()throws ReflectiveOperationException{
        base=(Object[])values.invoke(null);first=base.length;expanded=Arrays.copyOf(base,base.length+LutStore229.SLOTS);
        Object template=base[0];for(Object item:base)if("N_ORIGINAL".equals(((Enum<?>)item).name()))template=item;
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");Field singleton=unsafeClass.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);Method allocate=unsafeClass.getMethod("allocateInstance",Class.class);
        Field name=Enum.class.getDeclaredField("name"),ordinal=Enum.class.getDeclaredField("ordinal");name.setAccessible(true);ordinal.setAccessible(true);
        for(int slot=1;slot<=LutStore229.SLOTS;slot++){
            Object added=allocate.invoke(unsafe,h.type("o3.d"));name.set(added,"AURORA_USER_LUT229_"+slot);ordinal.setInt(added,first+slot-1);
            enumCategory.set(added,enumCategory.get(template));enumParameters.set(added,((float[])enumParameters.get(template)).clone());enumTokens.set(added,new String[]{LutStore229.token(slot)});expanded[first+slot-1]=added;
        }
        XposedBridge.hookMethod(values,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())p.setResult((Boolean.TRUE.equals(indexing.get())?base:expanded).clone());}});
        XposedBridge.hookMethod(valueOf,new StrictHook(){@Override protected void before(MethodHookParam p){String key=(String)p.args[0];for(int i=first;i<expanded.length;i++)if(((Enum<?>)expanded[i]).name().equals(key)){p.setResult(expanded[i]);return;}}});
        XposedBridge.hookMethod(byCategory,new StrictHook(){@Override protected void before(MethodHookParam p){p.setObjectExtra("229.enum.indexing",indexing.get());indexing.set(true);}@Override protected void after(MethodHookParam p){Boolean old=(Boolean)p.getObjectExtra("229.enum.indexing");if(old==null)indexing.remove();else indexing.set(old);}});
        XposedBridge.hookMethod(factory,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int slot=((Enum<?>)p.args[0]).ordinal()-first+1;if(slot<1||slot>LutStore229.SLOTS)return;
            String token=store().resourceToken(slot);if(store().name(slot)==null)throw new IllegalStateException("Selected custom LUT is not configured: "+slot);
            p.setResult(XposedHelpers.newInstance(h.type("o3.b"),512,token,enumParameters.get(p.args[0]),(Integer)p.args[3]));
        }@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;int ordinal=((Enum<?>)p.args[0]).ordinal(),slot=ordinal-first+1;int id=slot>=1&&slot<=LutStore229.SLOTS?id(slot):65536|ordinal;int[] effects=effects(id);if(effects!=null)lookupParameters.set(p.getResult(),parameters((float[])lookupParameters.get(p.getResult()),effects));
        }});
        XposedBridge.hookMethod(bitmap,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int slot=LutStore229.slot((String)p.args[1]);if(slot==0)return;Bitmap bitmap=BitmapFactory.decodeFile(store().file(slot).getAbsolutePath());if(bitmap==null)throw new java.io.IOException("Custom LUT bitmap missing: "+p.args[1]);p.setResult(bitmap);
        }});
        XposedBridge.hookMethod(publish,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int slot=LutStore229.slot((String)p.args[1]);if(slot==0)return;
            if(!(Boolean)write.invoke(null,p.args[2],p.args[3],Files.readAllBytes(store().file(slot).toPath())))throw new java.io.IOException("MIVI LUT delivery failed: "+p.args[1]);p.setResult(null);
        }});
        XposedBridge.hookMethod(catalog,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable()||options.context()==null)return;
            List<Object> rows=appendCatalog((Integer)p.args[0],(List<?>)p.getResult());if(rows!=null)p.setResult(rows);
        }});
        XposedBridge.hookMethod(remap,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;Field items=h.field("com.android.camera.data.data.c","mItems");Object list=items.get(p.thisObject);if(!(list instanceof List))return;
            for(Object row:(List<?>)list){int id=Integer.parseInt((String)rowId.get(row));int slot=slot(id);String key=identity(id);String label=key==null?null:store().label(key,slot>0?store().name(slot):null);if(label!=null){rowName.setInt(row,-1);rowDynamic.set(row,label);}}
        }});
        Method profileLabel=h.method("com.android.camera.features.mode.capture.e","z",Context.class);Field profileId=h.field("U9.S","a");
        XposedBridge.hookMethod(profileLabel,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;String id=(String)profileId.get(p.thisObject);String kind=id.startsWith("MiCamStyleFront_")?"MiCamStyleFront":id.startsWith("MiCamStyleRear_")?"MiCamStyleRear":null;
            if(kind!=null){int slot=workspace.ownedSlot(p.thisObject);String name=slot>0?store().name(slot):null;if(name!=null)p.setResult(store().label(LutStore229.token(slot),name));else if(slot==0)p.setResult(store().label(LutStore229.recipeKey(kind,id),(String)p.getResult()));}
        }});
        XposedBridge.hookMethod(attributes,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;int[] value=effects((Integer)p.args[0]);if(value==null)return;Object result=p.getResult();attrNoise.setBoolean(result,value[0]>0);attrBlack.setBoolean(result,value[1]==1);attrWhite.setBoolean(result,value[1]==2);float[] nativeValues=(float[])attrParams.get(result);if(nativeValues==null)nativeValues=(float[])h.field("k3.a","a").get(null);attrParams.set(result,parameters(nativeValues,value));
        }});
        // initParams runs inside the native constructor. Soft-focus must be
        // selected before it serializes the Candy script, not after construction.
        Method initParams=h.method("com.xiaomi.camera.mivi.filter.MIVIRenderTag","initParams");
        XposedBridge.hookMethod(initParams,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
            int[] value=effects(tagId.getInt(p.thisObject));if(value!=null)tagSoft.set(p.thisObject,h.type("com.xiaomi.camera.effect.EffectController$c").getEnumConstants()[value[1]]);
        }});
        XposedBridge.hookMethod(core,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
            if(p.hasThrowable())return;int[] value=effects(tagId.getInt(coreOwner.get(p.thisObject)));if(value==null)return;
            @SuppressWarnings("unchecked") Map<String,Object> params=(Map<String,Object>)p.args[0];params.put("needNoise",value[0]>0);params.put("effects",parameters((float[])lookupParameters.get(p.args[1]),value));
        }});
        // P uses low index 512 as a sentinel, not an enum. Expanded values must
        // not change the two native lookup factories' original bounds decision.
        preserveNoFilter(attributes);
        preserveNoFilter(h.method("si.f","d",int.class,int.class));
        workspace.install();
        new DynamicFilterLabels257(h,this).install();
        new PhotoFilterVisibility257(h,this).install();
    }
    private void preserveNoFilter(Method method){
        XposedBridge.hookMethod(method,new StrictHook(2000){
            @Override protected void before(MethodHookParam p){
                if((Integer)p.args[0]!=noFilter)return;
                p.setObjectExtra("229.original-filter-view",true);p.setObjectExtra("229.previous-filter-view",indexing.get());indexing.set(true);
            }
            @Override protected void after(MethodHookParam p){
                if(p.getObjectExtra("229.original-filter-view")==null)return;
                Boolean previous=(Boolean)p.getObjectExtra("229.previous-filter-view");if(previous==null)indexing.remove();else indexing.set(previous);
            }
        });
    }
}
