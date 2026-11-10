package com.aurora.camera.compat.lens;

import android.content.Context;
import android.content.res.Resources;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;
import com.aurora.camera.compat.Host;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The changed 168/175 N branches, including the native metadata/state tail. */
public final class UltraPixelPort {
    private final Method initialize, capM, capN, capIndex, capSize, capSupports, capUltra;
    private final Method repository, getComponent, specialOn, roles, wide, caps, featureProvider;
    private final Method photoSupported, originalIndex, specialCap, repositorySpecial, featureN3, featureX1;
    private final Field featureConfig, currentMode, items, specialM, specialN, enabled, duration, pending;
    private final Class<?> specialType, itemType;
    private final Method r, t, m, s, q, u, n, p, o, z, reset, setValue;
    private final Method propertyBoolean, propertyInt, context, itemIcon, itemNext;
    private final Field iconProvider, itemToken, itemAccessibility, itemA, itemC, itemF, itemG, itemH, itemL;
    private final Field[] itemDefaults;
    private final Field pixel32, accessibility32, pixel50, accessibility50, pixelXXX, accessibilityXXX;
    private final Method defaultValue, readMetadata, platform;
    private final Field cachedDuration, fallbackDuration, characteristics, metadataTag, metadataFlags;
    private final Constructor<?> durationConstructor;
    private final Field durationA, durationB, durationC, durationD, durationE;
    private final Object unsafe;
    private final Method allocate;

    public UltraPixelPort(Host h) throws ReflectiveOperationException {
        initialize=h.method("r2.e0","N",int.class,int.class,h.type("l9.g"));
        capM=h.method("l9.h","P0",h.type("l9.g")); capN=h.method("l9.h","f",h.type("l9.g"));
        capIndex=h.method("l9.h","g0",h.type("l9.g")); capSize=h.method("l9.h","E0",h.type("l9.g"));
        capSupports=h.method("l9.h","O4",h.type("l9.g"),Size.class); capUltra=h.method("l9.g","M0");
        repository=h.method("g2.a","j"); getComponent=h.method("Xh.b","x",Class.class);
        specialType=h.type("v2.h0"); specialOn=h.method("v2.c0","isSwitchOn",int.class);
        roles=h.method("v6.f","R"); wide=h.method("v6.f","e"); caps=h.method("v6.f","M",int.class);
        featureProvider=h.method("Pe.b","D"); featureConfig=h.field("Pe.b","e");
        photoSupported=h.method(featureConfig.getType(),"t1"); originalIndex=h.method("Pe.b","p");
        specialCap=h.method("l9.h","w3",h.type("l9.g")); repositorySpecial=h.method("v2.F0","F");
        featureN3=h.method(featureConfig.getType(),"n3"); featureX1=h.method(featureConfig.getType(),"X1");
        currentMode=h.field("com.android.camera.data.data.c","mCurrentMode");
        items=h.field("com.android.camera.data.data.c","mItems");
        specialM=h.field("r2.e0","m"); specialN=h.field("r2.e0","n"); enabled=h.field("r2.e0","f");
        duration=h.field("r2.e0","o"); pending=h.field("r2.e0","p");
        r=h.method("r2.e0","r",ArrayList.class); t=h.method("r2.e0","t",ArrayList.class);
        m=h.method("r2.e0","m",ArrayList.class); s=h.method("r2.e0","s",ArrayList.class);
        q=h.method("r2.e0","q",ArrayList.class); u=h.method("r2.e0","u",ArrayList.class);
        n=h.method("r2.e0","n",ArrayList.class); p=h.method("r2.e0","p",ArrayList.class);
        o=h.method("r2.e0","o",ArrayList.class); z=h.method("r2.e0","z");
        reset=h.method("r2.e0","G",int.class); defaultValue=h.method("r2.e0","A",String.class);
        setValue=h.method("com.android.camera.data.data.c","setComponentValue",int.class,String.class);
        propertyBoolean=h.method("Kr.g","c",String.class,boolean.class); propertyInt=h.method("Kr.g","e",String.class,int.class);
        context=h.method("com.xiaomi.camera.basic.Global","getContext"); iconProvider=h.field("Y6.j","a");
        itemIcon=h.method("Y6.k","s",String.class,boolean.class); itemType=h.type("com.android.camera.data.data.d");
        itemNext=h.method("E1.f0","d",ArrayList.class,itemType);
        itemToken=h.field(itemType,"r"); itemAccessibility=h.field(itemType,"w"); itemA=h.field(itemType,"A");
        itemC=h.field(itemType,"c"); itemF=h.field(itemType,"f"); itemG=h.field(itemType,"g");
        itemH=h.field(itemType,"h"); itemL=h.field(itemType,"l");
        itemDefaults=new Field[]{h.field(itemType,"c"),h.field(itemType,"d"),h.field(itemType,"e"),h.field(itemType,"f"),
                h.field(itemType,"g"),h.field(itemType,"i"),h.field(itemType,"k"),h.field(itemType,"l")};
        pixel32=h.field("Rh.e","ultra_pixel_32mp"); accessibility32=h.field("Rh.e","accessibility_ultra_pixel_32mp");
        pixel50=h.field("Rh.e","ultra_pixel_50mp"); accessibility50=h.field("Rh.e","accessibility_ultra_pixel_50mp");
        pixelXXX=h.field("Rh.e","tip_ultra_pixel_XXXM"); accessibilityXXX=h.field("Rh.e","accessibility_ultra_pixel_xxxmp");
        cachedDuration=h.field("l9.g","x"); fallbackDuration=h.field("l9.g","h7");
        characteristics=h.field("l9.g","d"); metadataTag=h.field("ia.w0","d4"); metadataFlags=h.field("ia.E0","a");
        readMetadata=h.method("ia.E0","i",CameraCharacteristics.class,h.type("ia.D0"),int.class);
        platform=h.method("Sa.f","a"); durationConstructor=h.type("ja.I").getDeclaredConstructor(); durationConstructor.setAccessible(true);
        durationA=h.field("ja.I","a"); durationB=h.field("ja.I","b"); durationC=h.field("ja.I","c");
        durationD=h.field("ja.I","d"); durationE=h.field("ja.I","e");
        // R8 removed d.<init>; the accepted dex invokes Object.<init> directly.
        // Allocate that exact zero-initialized host data object, then perform its explicit stores.
        Class<?> unsafeType=h.type("sun.misc.Unsafe"); Field singleton=unsafeType.getDeclaredField("theUnsafe");
        singleton.setAccessible(true); unsafe=singleton.get(null);
        allocate=unsafeType.getDeclaredMethod("allocateInstance",Class.class); allocate.setAccessible(true);
    }

    public void install() {
        XposedBridge.hookMethod(initialize,new LensHook() {
            @Override protected void before(MethodHookParam p) throws Throwable {
                int mode=(int)p.args[0];
                if (mode==168 || mode==175) { initialize(p.thisObject,mode,(int)p.args[1],p.args[2]); p.setResult(null); }
            }
        });
    }

    private Object capabilities() throws ReflectiveOperationException {
        Object provider=roles.invoke(null); return caps.invoke(provider,wide.invoke(provider));
    }

    private Object blankItem() throws ReflectiveOperationException {
        Object item=allocate.invoke(unsafe,itemType); defaults(item); return item;
    }
    private void defaults(Object item) throws ReflectiveOperationException {
        for (Field field:itemDefaults) field.setInt(item,-1); itemA.setInt(item,0);
    }
    private void fill(Object item,String token,boolean ordinaryIcon,Field label,Field accessibility,Resources resources)
            throws ReflectiveOperationException {
        itemToken.set(item,token); Object icons=iconProvider.get(null);
        itemC.setInt(item,(int)itemIcon.invoke(icons,token,ordinaryIcon));
        if (ordinaryIcon) itemF.setInt(item,(int)itemIcon.invoke(icons,token,true));
        itemG.setInt(item,(int)itemIcon.invoke(icons,token,true));
        itemH.setInt(item,(int)itemIcon.invoke(icons,token,false));
        itemL.setInt(item,label.getInt(null)); itemAccessibility.set(item,resources.getString(accessibility.getInt(null)));
    }

    private void photoItems(Object owner,int mode,Object cap,ArrayList<Object> values) throws ReflectiveOperationException {
        int index=(int)capIndex.invoke(null,cap); Size size=(Size)capSize.invoke(null,cap);
        boolean supported=(boolean)capSupports.invoke(null,cap,size);
        if (!supported && (boolean)specialOn.invoke(getComponent.invoke(repository.invoke(null),specialType),mode))
            supported=(boolean)capSupports.invoke(null,capabilities(),size);
        if (index>-1 && supported) {
            switch(index) {
                case 1:r.invoke(owner,values);break;
                case 2:t.invoke(owner,values);break;
                case 3:m.invoke(owner,values);break;
                case 4:
                    if (!(boolean)z.invoke(owner)) { s.invoke(owner,values); break; }
                    Object feature=featureConfig.get(featureProvider.invoke(null));
                    boolean isN3=(boolean)featureN3.invoke(feature);
                    boolean remove=(boolean)propertyBoolean.invoke(null,"com.xiaomi.remove200MFromProMode",false);
                    boolean restricted=(boolean)featureX1.invoke(feature) || remove;
                    // mode remains 168 here: the original literal photo-163 exception does not apply.
                    if (isN3 && (mode==163 || restricted)) { s.invoke(owner,values); break; }
                    Resources resources=((Context)context.invoke(null)).getResources();
                    n.invoke(null,values);
                    Object first=blankItem(); fill(first,"REARx5",true,pixel50,accessibility50,resources);
                    Object second=itemNext.invoke(null,values,first); defaults(second);
                    fill(second,"REARx7",true,pixelXXX,accessibilityXXX,resources); values.add(second);
                    defaultValue.invoke(owner,"REARx5"); break;
                case 6:q.invoke(owner,values);break;
                case 7:
                    Resources resources32=((Context)context.invoke(null)).getResources(); u.invoke(null,values);
                    Object item=blankItem(); fill(item,"REARx8",false,pixel32,accessibility32,resources32);
                    values.add(item); defaultValue.invoke(owner,"REARx8");break;
                default:XposedBridge.log("ComponentConfigUltraPixel Unknown rearPixel index: "+index);break;
            }
        } else if ((boolean)reset.invoke(owner,mode)) {
            Object main=capabilities(); int mainIndex=(int)capIndex.invoke(null,main);
            Size mainSize=(Size)capSize.invoke(null,main);
            if (mainIndex>-1 && (boolean)capSupports.invoke(null,main,mainSize)) {
                switch(mainIndex) {
                    case 1:case 3:o.invoke(null,values);break;
                    case 2:p.invoke(null,values);break;
                    case 4:case 6:n.invoke(null,values);break;
                    case 7:u.invoke(null,values);break;
                    default:break;
                }
            }
        }
    }

    private Object fallback(Object cap) throws ReflectiveOperationException {
        Object result=fallbackDuration.get(cap);
        if (result==null) { result=defaultDuration(); fallbackDuration.set(cap,result); }
        return result;
    }
    private Object defaultDuration() throws ReflectiveOperationException {
        Object value=durationConstructor.newInstance(); durationA.setInt(value,0);
        durationB.setInt(value,800); durationC.setInt(value,700); return value;
    }
    private Object captureDuration(Object cap) throws ReflectiveOperationException {
        if (cap==null) return defaultDuration();
        Object result=cachedDuration.get(cap);
        if (result!=null) return result;
        if ((boolean)capUltra.invoke(cap)) {
            int[] raw=(int[])readMetadata.invoke(null,characteristics.get(cap),metadataTag.get(null),metadataFlags.getInt(null));
            ArrayList<Object> rows=new ArrayList<>();
            if (raw!=null && raw.length>=5 && raw.length%5==0) {
                for (int i=0;i<raw.length;i+=5) {
                    Object row=durationConstructor.newInstance(); durationA.setInt(row,raw[i]); durationB.setInt(row,raw[i+1]);
                    durationC.setInt(row,raw[i+2]); durationD.setInt(row,raw[i+3]); durationE.setInt(row,raw[i+4]); rows.add(row);
                }
            } else XposedBridge.log("UltraPixelCaptureDuration Expected values!");
            if (!rows.isEmpty()) {
                for (Object row:rows) {
                    if ((boolean)platform.invoke(null) && durationA.getInt(row)==1
                            || !(boolean)platform.invoke(null) && durationA.getInt(row)==0) { result=row; break; }
                }
                if (result==null) result=rows.get(0);
            } else result=fallback(cap);
        } else result=fallback(cap);
        cachedDuration.set(cap,result); return result;
    }

    private void initialize(Object owner,int mode,int camera,Object cap) throws ReflectiveOperationException {
        specialM.setBoolean(owner,(boolean)capM.invoke(null,cap)); specialN.setInt(owner,(int)capN.invoke(null,cap));
        currentMode.setInt(owner,mode); ArrayList<Object> values=new ArrayList<>();
        if (cap!=null) {
            if (mode==175) {
                if (camera==0) {
                    Object feature=featureProvider.invoke(null); originalIndex.invoke(feature);
                    if (!((boolean)specialCap.invoke(null,cap) && (boolean)repositorySpecial.invoke(repository.invoke(null))))
                        featureN3.invoke(featureConfig.get(feature));
                    s.invoke(owner,values); // accepted rear 175 selector is forced to index 4.
                }
            } else if ((boolean)photoSupported.invoke(featureConfig.get(featureProvider.invoke(null)))) photoItems(owner,mode,cap,values);
        }
        items.set(owner,Collections.unmodifiableList(values));
        if (!values.isEmpty()) enabled.setBoolean(owner,cap!=null && (boolean)capUltra.invoke(cap));
        if (enabled.getBoolean(owner)) {
            Object value=captureDuration(cap);
            durationB.setInt(value,(int)propertyInt.invoke(null,"camera.debug.test.duration",durationB.getInt(value)));
            durationC.setInt(value,(int)propertyInt.invoke(null,"camera.debug.test.duration_save",durationC.getInt(value)));
            duration.set(owner,value); pending.setBoolean(owner,false);
        }
        if ((boolean)reset.invoke(owner,mode)) setValue.invoke(owner,mode,"OFF");
    }
}
