package com.aurora.camera.compat;

import android.view.View;
import android.util.Log;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Proxy;

/** Native 1770 Cclock (215) and physical aperture (3392) menu contracts. */
public final class TopControls239Hooks {
    private static final int CCLOCK=215, APERTURE=3392;
    private TopControls239Hooks(){}
    public static HookPlan resolve(final Host h)throws ReflectiveOperationException{
        final Method factory=h.method("X9.w5","E",int.class);
        final Method available=h.method("u2.y","T",int.class,ArrayList.class);
        final Method rows=h.method("u2.y","Y",ArrayList.class,boolean.class);
        final Method defaults=h.method("Y9.O","f",int.class,java.util.Optional.class);
        h.type("b5.j$a").getDeclaredConstructor();h.method("b5.j$a","a");
        h.method("R6.D","a");h.method("R6.D","fk",int.class);
        final Method apertureRefresh=h.method("U1.b","Vg");
        final Method tray=h.method("R6.s1","a");
        h.type("M1.c").getDeclaredConstructor(int.class);
        final Class<?> rowType=h.type("com.android.camera.data.data.d");
        // R8 inlines every stock row constructor; reflection has no <init>.
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field singleton=unsafeType.getDeclaredField("theUnsafe");singleton.setAccessible(true);
        final Object unsafe=singleton.get(null);
        final Method allocate=unsafeType.getDeclaredMethod("allocateInstance",Class.class);allocate.setAccessible(true);
        h.method("r6.W","B2");
        h.field("r2.i","b");h.field("v2.k","W");
        h.type("L4.a").getDeclaredConstructor(Object.class,int.class);
        final Set<Method> providers=new LinkedHashSet<>();
        for(String owner:new String[]{"r4.b","com.android.camera.features.mode.video.a","com.android.camera.features.mode.video.b",
            "com.android.camera.features.mode.pro.rec.a","f4.a",
            "com.android.camera.features.mode.portrait.e","com.android.camera.features.mode.portrait.k",
            "com.android.camera.features.mode.portrait.l"}){
            // getMethod intentionally resolves the real inherited implementation.
            providers.add(h.type(owner).getMethod("e"));providers.add(h.type(owner).getMethod("l"));
        }
        return new HookPlan(){
            public String id(){return "239.native-cclock-and-aperture-menu";}
            Object component(String type,boolean running)throws ClassNotFoundException{
                return XposedHelpers.callMethod(XposedHelpers.callStaticMethod(h.type("g2.a"),running?"j":"a"),"x",h.type(type));
            }
            int id(int mode)throws ClassNotFoundException{
                if(mode==162||mode==180)return (Boolean)XposedHelpers.getObjectField(component("r2.i",false),"b")?CCLOCK:0;
                if(mode==171||mode==175)return (Boolean)XposedHelpers.getObjectField(component("v2.k",true),"W")?APERTURE:0;
                return 0;
            }
            Object cclock(boolean top)throws ReflectiveOperationException{
                Object builder=XposedHelpers.newInstance(h.type("b5.i$a"));
                XposedHelpers.setIntField(builder,"a",CCLOCK);XposedHelpers.setIntField(builder,"b",8388613);
                Object cc=component("r2.i",false);
                XposedHelpers.setObjectField(builder,"d",XposedHelpers.newInstance(h.type("L4.a"),cc,3));
                XposedHelpers.setObjectField(builder,"c",Proxy.newProxyInstance(h.loader,new Class<?>[]{h.type("b5.i$c")},(proxy,m,args)->{
                    if(m.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,m.getName(),args);
                    if(!m.getName().equals("c"))throw new IllegalStateException(m.toString());
                    int mode=(Integer)args[0];Object item=XposedHelpers.newInstance(h.type("b5.j$a"));
                    XposedHelpers.setIntField(item,"a",h.field("Rh.b","ic_video_cclock_menu").getInt(null));
                    XposedHelpers.setIntField(item,"e",0x7f1403da);
                    XposedHelpers.setBooleanField(item,"g",(Boolean)XposedHelpers.callMethod(cc,"isSwitchOn",mode));
                    XposedHelpers.setBooleanField(item,"i",!(Boolean)XposedHelpers.getObjectField(cc,"b")||(Integer)XposedHelpers.callMethod(cc,"n")!=0);
                    return XposedHelpers.callMethod(item,"a");
                }));
                Object analytics=XposedHelpers.newInstance(h.type("O9.g"),cc,1);
                if(top){
                    // Top clicks invoke the native dispatcher themselves; the
                    // dropdown dispatcher does that after its separate listener.
                    XposedHelpers.setObjectField(builder,"e",(View.OnClickListener)view->{
                        @SuppressWarnings("unchecked") java.util.Optional<Object> config=(java.util.Optional<Object>)XposedHelpers.callStaticMethod(type("R6.D"),"a");
                        Log.i("AuroraTop240","top_click config=215 protocol="+config.isPresent());
                        config.ifPresent(target->XposedHelpers.callMethod(target,"fk",CCLOCK));
                    });
                    XposedHelpers.setObjectField(builder,"f",analytics);
                }else XposedHelpers.setObjectField(builder,"e",analytics);
                return XposedHelpers.newInstance(h.type("b5.i"),builder);
            }
            ArrayList<Integer> dropdownWithoutBlanks(List<Integer> chosen){
                ArrayList<Integer> result=new ArrayList<>();
                for(int i=0;i<chosen.size();i++){
                    int token=chosen.get(i);
                    // 223 is the already-hidden video watermark. Its old UI-only
                    // hiding replaced it with an invisible cell, leaving a gap.
                    if(i<6)result.add(token==223?216:token);
                    else if(token!=216&&token!=223)result.add(token);
                }
                return result;
            }
            Class<?> type(String name){try{return h.type(name);}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}}
            void migrate(Object menu,int mode,int token){
                String key=(String)XposedHelpers.callMethod(menu,"getKey",mode)+(token==CCLOCK?"_aurora_controls240b":"_aurora_controls239");
                Object parent=XposedHelpers.getObjectField(menu,"mParentDataItem");
                if((Boolean)XposedHelpers.callMethod(parent,"f",key))return;
                @SuppressWarnings("unchecked") List<Integer> saved=(List<Integer>)XposedHelpers.callMethod(menu,"U",mode);
                if(saved.isEmpty())return;
                ArrayList<Integer> updated=token==CCLOCK?dropdownWithoutBlanks(saved):new ArrayList<>(saved);
                if(!updated.contains(token)){updated.add(token);XposedHelpers.callMethod(menu,"Y",updated,false);}
                else if(!updated.equals(saved))XposedHelpers.callMethod(menu,"Y",updated,false);
                XposedHelpers.callMethod(parent,"g");XposedHelpers.callMethod(parent,"r",key,"done");XposedHelpers.callMethod(parent,"c");
            }
            public void install(){
                XposedBridge.hookMethod(apertureRefresh,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;
                    Object global=XposedHelpers.callStaticMethod(type("g2.a"),"g");
                    int mode=(Integer)XposedHelpers.callMethod(global,"D",XposedHelpers.getObjectField(global,"u"));
                    if(mode==171||mode==175){
                        @SuppressWarnings("unchecked") java.util.Optional<Object> panel=(java.util.Optional<Object>)tray.invoke(null);
                        @SuppressWarnings("unchecked") java.util.function.Consumer<Object> refresh=(java.util.function.Consumer<Object>)XposedHelpers.newInstance(type("M1.c"),1);
                        panel.ifPresent(refresh);
                    }
                }});
                XposedBridge.hookMethod(factory,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    if((Integer)p.args[0]==CCLOCK)p.setResult(cclock(true));
                }});
                XposedBridge.hookMethod(defaults,new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;int mode=(Integer)p.args[0];if((mode!=162&&mode!=180)||id(mode)!=CCLOCK)return;
                    @SuppressWarnings("unchecked") List<Integer> previous=(List<Integer>)p.getResult();
                    ArrayList<Integer> result=dropdownWithoutBlanks(previous);if(!result.contains(CCLOCK))result.add(CCLOCK);p.setResult(result);
                }});
                StrictHook provider=new StrictHook(){@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;
                    int mode=(Integer)XposedHelpers.callMethod(p.thisObject,"getModuleId"),token=id(mode);if(token==0)return;
                    // e() supplies the native default top layout. 215 belongs in
                    // l()'s dropdown until the user explicitly drags it to the top.
                    if(token==CCLOCK&&p.method.getName().equals("e"))return;
                    @SuppressWarnings("unchecked") List<Object> previous=(List<Object>)p.getResult();
                    ArrayList<Object> result=new ArrayList<>(previous);
                    if(token==CCLOCK)result.removeIf(row->(Integer)XposedHelpers.getObjectField(row,"c")==223);
                    for(Object row:result)if((Integer)XposedHelpers.getObjectField(row,"c")==token){if(!result.equals(previous))p.setResult(result);return;}
                    result.add(token==CCLOCK?cclock(false):factory.invoke(null,APERTURE));p.setResult(result);
                }};
                for(Method method:providers)XposedBridge.hookMethod(method,provider);
                XposedBridge.hookMethod(available,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    int mode=(Integer)p.args[0],token=id(mode);if(token==0)return;
                    @SuppressWarnings("unchecked") ArrayList<Integer> values=(ArrayList<Integer>)p.args[1];
                    if(!values.contains(token))values.add(token);migrate(p.thisObject,mode,token);
                    if(token==CCLOCK)Log.i("AuroraTop240","available mode="+mode+" ownerMode="+XposedHelpers.getObjectField(p.thisObject,"mCurrentMode")+" values="+values+" saved="+XposedHelpers.callMethod(p.thisObject,"U",mode));
                }});
                XposedBridge.hookMethod(rows,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    int mode=(Integer)XposedHelpers.getObjectField(p.thisObject,"mCurrentMode");if(mode!=162&&mode!=180)return;
                    @SuppressWarnings("unchecked") List<Integer> chosen=(List<Integer>)p.args[0];
                    ArrayList<Integer> clean=dropdownWithoutBlanks(chosen);
                    // Editor cancel/restore can replay a snapshot captured before
                    // T() migrated it. Keep the new supported option available.
                    if(id(mode)==CCLOCK&&!clean.contains(CCLOCK))clean.add(CCLOCK);
                    if(!clean.equals(chosen))Log.i("AuroraTop240","complete_video_rows mode="+mode+" before="+chosen+" after="+clean);p.args[0]=clean;
                }@Override protected void after(MethodHookParam p)throws Throwable{
                    if(p.hasThrowable())return;
                    int mode=(Integer)XposedHelpers.getObjectField(p.thisObject,"mCurrentMode");if(mode!=162&&mode!=180)return;
                    @SuppressWarnings("unchecked") List<Integer> chosen=(List<Integer>)p.args[0];if(!chosen.contains(CCLOCK))return;
                    @SuppressWarnings("unchecked") List<Object> items=(List<Object>)XposedHelpers.getObjectField(p.thisObject,"mItems");
                    for(Object row:items)if("215".equals(XposedHelpers.getObjectField(row,"r")))return;
                    Object row=allocate.invoke(unsafe,rowType);
                    for(String name:new String[]{"c","d","e","f","g","i","k","l"})XposedHelpers.setIntField(row,name,-1);
                    XposedHelpers.setObjectField(row,"r","215");
                    XposedHelpers.setIntField(row,"c",h.field("Rh.b","ic_video_cclock_menu").getInt(null));
                    XposedHelpers.setIntField(row,"l",0x7f1403da);
                    // j is a Lottie raw resource, not an activated-state icon.
                    XposedHelpers.setIntField(row,"j",0);
                    @SuppressWarnings("unchecked") List<Integer> normalized=(List<Integer>)XposedHelpers.callStaticMethod(h.type("u2.y"),"c0",p.args[0]);
                    items.add(Math.min(normalized.indexOf(CCLOCK),items.size()),row);
                    ArrayList<String> descriptions=new ArrayList<>();for(Object item:items)descriptions.add(XposedHelpers.getObjectField(item,"r")+":icon="+XposedHelpers.getObjectField(item,"c")+":anim="+XposedHelpers.getObjectField(item,"j"));
                    Log.i("AuroraTop240","editor_rows mode="+mode+" chosen="+chosen+" rows="+descriptions);
                }});
            }
        };
    }
}
