package com.aurora.camera.compat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.aurora.camera.compat.ui.MiuixDialogs;
import com.aurora.camera.compat.ui.NativePreferences240;
import com.aurora.camera.compat.ui.NativeNotes249;
import com.aurora.camera.compat.ui.MiuixSeekBar241;
import com.aurora.camera.compat.ui.AuthNavigation241;
import com.aurora.camera.compat.ui.AuthDocumentPicker241;
import com.aurora.camera.compat.vignette.AuthFormula239;
import com.aurora.camera.compat.vignette.AuthReadiness239;
import com.aurora.camera.compat.vignette.AuthRuntime241;
import com.aurora.camera.compat.vignette.AuthBody241;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipFile;

/** Integrated AUTHVignette 1.1.0, plus the 1770 Java CvShader preview path. */
public final class AuthVignette239Hooks {
    private interface Work{void run()throws Exception;}
    private static final String PAGE="aurora_auth_page239",EXPERIMENTAL="aurora_auth_experimental239";
    private static final String STRENGTH="aurora_auth_strength239",RANGE="aurora_auth_range239",PROTECT="aurora_auth_protect239",SHAPE="aurora_auth_shape239",ENABLE="aurora_auth_enable239";
    private static final String NOTE_TEXT="暗角着色器需搭配徕卡经典风格选项使用。“跟随原生”选项，着色器调节仅对自带暗角的滤镜生效，如需使用14 Ultra经典风格+全局暗角自定义调节，请在徕卡经典风格中选择13 Ultra 选项。";
    private AuthVignette239Hooks(){}
    public static HookPlan resolve(final Host h)throws ReflectiveOperationException{
        final Method settings=h.method("com.android.camera.fragment.settings.CameraAdvancePreferenceFragment","registerPreferenceListener");
        final Method populate=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","addCurrentPreferences");
        final Method register=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","registerPreferenceListener");
        final Method initialize=h.method("com.android.camera.fragment.settings.b","initializeActivity");
        final Method resume=h.method("com.android.camera.fragment.settings.CameraPreferenceFragment","onResume");
        final Method result=h.method("androidx.fragment.app.Fragment","onActivityResult",int.class,int.class,Intent.class);
        h.method("androidx.fragment.app.Fragment","isAdded");
        final Method library=h.method("dalvik.system.BaseDexClassLoader","findLibrary",String.class);
        final Method shaderSource=h.method("u9.a","p"),shaderInit=h.method("u9.a","q"),shaderDraw=h.method("u9.a","c",h.type("j3.b"));
        final MiuixDialogs dialogs=new MiuixDialogs(h);
        final NativePreferences240 nativePreferences=new NativePreferences240(h);
        final NativeNotes249 notes=new NativeNotes249(h,nativePreferences);
        final AuthRuntime241 runtime=new AuthRuntime241(h);
        final MiuixSeekBar241 sliders=new MiuixSeekBar241(h);
        final AuthNavigation241 navigation=new AuthNavigation241(h);
        final AuthDocumentPicker241 documents=new AuthDocumentPicker241(h);
        return new HookPlan(){
            final Handler ui=new Handler(Looper.getMainLooper());
            final ExecutorService io=Executors.newSingleThreadExecutor(task->{Thread t=new Thread(task,"AuroraAUTH239");t.setDaemon(true);return t;});
            final Map<Object,Boolean> pages=Collections.synchronizedMap(new WeakHashMap<>());
            boolean busy;
            String unavailable,template;
            volatile String body;
            volatile long revision;
            public String id(){return "239.authvignette-native-and-settings";}
            Class<?> type(String name){try{return h.type(name);}catch(ClassNotFoundException failure){throw new IllegalStateException(failure);}}
            Object call(Object owner,String name,Object...args){return XposedHelpers.callMethod(owner,name,args);}
            Context app(){return(Context)XposedHelpers.callStaticMethod(type("com.xiaomi.camera.basic.Global"),"getContext");}
            Activity activity(Object f){return(Activity)call(f,"requireActivity");}
            SharedPreferences prefs(){return app().getSharedPreferences("phoenix_vignette",0);}
            boolean own(Object f){Activity owner=activity(f);return owner.getClass().getName().equals("com.android.camera.fragment.settings.PreferenceExtraActivity")&&owner.getIntent().getBooleanExtra(PAGE,false);}
            boolean experimental(Object f){return navigation.experimental(f,activity(f).getIntent().getBooleanExtra(EXPERIMENTAL,false));}
            int strength(){return prefs().getInt("strength",100);}int range(){return prefs().getInt("range",100);}
            boolean protect(){return prefs().getBoolean("protect",true);}boolean shape(){return prefs().getBoolean("shape",true);}
            boolean custom(){return prefs().getBoolean("enabled",false);}String fileName(){return prefs().getString("name","");}
            File file(String name){return new File(AuthReadiness239.DIRECTORY,name);}
            void message(Object f,String text){
                if((Boolean)call(f,"isAdded"))Toast.makeText(activity(f),text,Toast.LENGTH_LONG).show();
                else Log.i("AuroraAUTH241","settings_message page_detached text="+text);
            }
            String asset()throws IOException{
                try(ZipFile zip=new ZipFile(h.moduleApkPath);InputStream in=zip.getInputStream(zip.getEntry("assets/auth239/vignette-template.glsl"));ByteArrayOutputStream out=new ByteArrayOutputStream()){
                    byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return new String(out.toByteArray(),StandardCharsets.UTF_8);
                }
            }
            void reload()throws IOException{body=new String(Files.readAllBytes(file("colorDark.glsl").toPath()),StandardCharsets.UTF_8);revision++;runtime.changed(revision,body);}
            void open(Object f,boolean experimental){
                if(experimental&&own(f)){
                    navigation.openExperimental(f);
                    Log.i("AuroraAUTH241","experimental_navigation route=native_fragment_backstack");return;
                }
                Activity owner=activity(f);Intent intent=new Intent(owner,type("com.android.camera.fragment.settings.PreferenceExtraActivity"));
                intent.putExtra("target_tag","com.android.camera.fragment.settings.CameraPreferenceFragment");intent.putExtra(PAGE,true);intent.putExtra(EXPERIMENTAL,experimental);
                try{intent.putExtra("from_where",h.field("com.android.camera.fragment.settings.b","mFromWhere").getInt(null));}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                XposedHelpers.setBooleanField(f,"mGoToActivity",true);owner.startActivity(intent);
            }
            Object click(Runnable action){return Proxy.newProxyInstance(h.loader,new Class<?>[]{type("androidx.preference.Preference$d")},(proxy,m,args)->{
                if(m.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,m.getName(),args);
                if(!m.getName().equals("onPreferenceClick"))throw new IllegalStateException(m.toString());action.run();return true;
            });}
            Object row(Object f,Object group,String key,String title,String summary,Runnable action){
                Object p=nativePreferences.text(f);call(p,"a0",key);call(p,"e0",title);call(p,"c0",summary);XposedHelpers.setBooleanField(p,"t",false);
                if(action!=null)XposedHelpers.setObjectField(p,"f",click(action));call(group,"j0",p);return p;
            }
            Object category(Object f,Object screen,String key,String title){return nativePreferences.category(f,screen,key,title,Integer.MAX_VALUE);}
            void entry(Object f){
                Object screen=XposedHelpers.getObjectField(f,"mPreferenceGroup"),group=call(screen,"k0","category_aurora_classic_controls239");
                if(group==null)throw new IllegalStateException("Classic settings category is missing");
                if(call(group,"k0","aurora_auth_entry239")==null)row(f,group,"aurora_auth_entry239","徕卡经典暗角着色器",unavailable==null?"":unavailable,()->{if(unavailable!=null)message(f,unavailable);else open(f,false);});
                nativePreferences.regroupLaboratory(f);
            }
            void toggle(Object f,Object group,String key,String title,String summary,boolean checked){
                Object p=nativePreferences.toggle(f);call(p,"a0",key);call(p,"e0",title);call(p,"c0",summary);XposedHelpers.setBooleanField(p,"t",false);call(p,"setChecked",checked);
                XposedHelpers.setObjectField(p,"e",Proxy.newProxyInstance(h.loader,new Class<?>[]{type("androidx.preference.Preference$c")},(proxy,m,args)->{
                    if(m.getDeclaringClass()==Object.class)return Options229.objectMethod(proxy,m.getName(),args);
                    if(!m.getName().equals("onPreferenceChange"))throw new IllegalStateException(m.toString());
                    boolean value=(Boolean)args[1];apply(f,strength(),key.equals(PROTECT)?value:protect(),range(),key.equals(SHAPE)?value:shape(),key.equals(ENABLE)?value:custom());return false;
                }));call(group,"j0",p);
            }
            void populate(Object f){
                navigation.preparePage(f,experimental(f));
                pages.put(f,true);Object screen=XposedHelpers.getObjectField(f,"mPreferenceGroup");if(!experimental(f))notes.addAuth(f,screen,NOTE_TEXT);Object controls=category(f,screen,"aurora_auth_controls239",null);
                if(!experimental(f)){
                    row(f,controls,STRENGTH,"暗角强度",strength()+"%",()->slider(f,false));toggle(f,controls,PROTECT,"高光保护","关闭高光保护以实现更符合物理的效果",protect());
                    Object geometry=category(f,screen,"aurora_auth_geometry239","暗角形态调节");row(f,geometry,RANGE,"暗角范围",range()+"%",()->slider(f,true));toggle(f,geometry,SHAPE,"暗角形状调整","原生状态为开启，关闭更符合物理",shape());
                    Object extra=category(f,screen,"aurora_auth_extra239",null);row(f,extra,"aurora_auth_extra_link239","实验性","导入自定义 GLSL 着色器",()->open(f,true));
                }else{
                    toggle(f,controls,ENABLE,"启用自定义着色器","",custom());
                    row(f,controls,"aurora_auth_import239","导入 GLSL 文件","UTF-8 函数体，含换行不超过 16 KiB",()->chooseFile(f));
                    row(f,controls,"aurora_auth_file239","当前文件",fileName().isEmpty()?"未导入":fileName(),null);
                    row(f,controls,"aurora_auth_delete239","删除","",()->operation(f,()->{
                        request(AuthFormula239.formula(strength(),protect(),range(),shape()));
                        Files.delete(file("custom.glsl").toPath());if(!prefs().edit().remove("name").putBoolean("enabled",false).commit())throw new IOException("Cannot save deleted formula state");reload();
                    },"已删除"));
                }
                com.aurora.camera.compat.ui.NativePageTitle257.set(activity(f),experimental(f)?"实验性":"徕卡经典暗角着色器");refresh();
            }
            void refresh(){
                for(Object f:new ArrayList<>(pages.keySet())){
                    if(!(Boolean)call(f,"isAdded"))continue;
                    Object screen=XposedHelpers.getObjectField(f,"mPreferenceGroup");
                    for(String key:new String[]{STRENGTH,RANGE,PROTECT,SHAPE,ENABLE,"aurora_auth_import239","aurora_auth_delete239","aurora_auth_file239"}){
                        Object p=call(screen,"k0",key);if(p==null)continue;
                        boolean normal=key.equals(STRENGTH)||key.equals(RANGE)||key.equals(PROTECT)||key.equals(SHAPE);
                        boolean choosing=key.equals("aurora_auth_import239")&&experimental(f)&&documents.pending(f);
                        call(p,"Y",!busy&&!choosing&&(!normal||!custom())&&(!(key.equals(ENABLE)||key.equals("aurora_auth_delete239"))||!fileName().isEmpty()));
                        if(key.equals(STRENGTH)||key.equals(RANGE))call(p,"c0",(key.equals(STRENGTH)?strength():range())+"%");
                        if(key.equals(PROTECT)||key.equals(SHAPE)||key.equals(ENABLE))call(p,"setChecked",key.equals(PROTECT)?protect():key.equals(SHAPE)?shape():custom());
                        if(key.equals("aurora_auth_file239"))call(p,"c0",fileName().isEmpty()?"未导入":fileName());
                    }
                }
            }
            void slider(Object f,boolean radius){
                Context c=activity(f);LinearLayout content=new LinearLayout(c);content.setOrientation(LinearLayout.VERTICAL);int padding=Math.round(24*c.getResources().getDisplayMetrics().density);content.setPadding(padding,padding/2,padding,padding/2);
                TextView value=new TextView(c);value.setTextSize(18);content.addView(value);SeekBar seek=sliders.create(c);seek.setMax(radius?150:600);seek.setProgress(radius?range():AuthFormula239.position(strength()));value.setText((radius?range():strength())+"%");
                LinearLayout.LayoutParams sliderLayout=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,Math.round(48*c.getResources().getDisplayMetrics().density));sliderLayout.topMargin=Math.round(8*c.getResources().getDisplayMetrics().density);content.addView(seek,sliderLayout);
                Log.i("AuroraAUTH241","slider_open kind="+(radius?"range":"strength")+" class="+seek.getClass().getName());
                seek.setContentDescription(radius?"暗角范围":"暗角强度");seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int progress,boolean user){value.setText((radius?progress:AuthFormula239.strength(progress))+"%");}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
                dialogs.builder(c).setTitle(radius?"暗角范围":"暗角强度").setView(content).setNegativeButton("取消",null).setPositiveButton("应用",(d,w)->apply(f,radius?strength():AuthFormula239.strength(seek.getProgress()),protect(),radius?seek.getProgress():range(),shape(),false)).show();
            }
            void chooseFile(Object f){
                try{if(documents.choose(f))Log.i("AuroraAUTH241","document_picker_launched owner=fragment request="+AuthDocumentPicker241.REQUEST);}
                catch(ActivityNotFoundException|SecurityException failure){Log.e("AuroraAUTH241","document picker launch failed",failure);message(f,"无法打开文件选择器："+failure.getMessage());}
                finally{refresh();}
            }
            void operation(Object f,Work work,String success){
                if(busy)throw new IllegalStateException("AUTH operation already running");busy=true;refresh();
                io.execute(()->{
                    String error=null;try{work.run();}catch(Exception failure){Log.e("AuroraAUTH239","operation failed",failure);error=failure.getMessage();}
                    final String result=error;ui.post(()->{busy=false;refresh();message(f,result==null?success:"操作失败："+result);});
                });
            }
            void apply(Object f,int strength,boolean protect,int range,boolean shape,boolean custom){operation(f,()->{
                String next=custom?new String(Files.readAllBytes(file("custom.glsl").toPath()),StandardCharsets.UTF_8):AuthFormula239.formula(strength,protect,range,shape);
                validate(next);request(next);reload();
                if(!prefs().edit().putInt("strength",strength).putBoolean("protect",protect).putInt("range",range).putBoolean("shape",shape).putBoolean("enabled",custom).commit())throw new IOException("公式已同步，但无法保存设置");
            },"已应用");}
            void atomic(File target,String text)throws IOException{
                File next=new File(target.getParentFile(),target.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(next)){out.write(text.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
                Files.move(next.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            }
            void request(String formula)throws Exception{
                formula=AuthBody241.normalize(formula);
                String issue=AuthReadiness239.check();if(issue!=null)throw new IOException(issue);
                String token=Long.toString(System.nanoTime());atomic(file("request.glsl"),formula);atomic(file("request"),token+" apply\n");
                long end=SystemClock.elapsedRealtime()+8000;while(SystemClock.elapsedRealtime()<end){
                    File ack=file("ack");if(ack.isFile()){
                        String result=new String(Files.readAllBytes(ack.toPath()),StandardCharsets.UTF_8).trim();
                        if(result.startsWith(token+" ")){if(!result.equals(token+" OK"))throw new IOException(result.substring(token.length()+1));return;}
                    }Thread.sleep(50);
                }throw new IOException("原生公式服务未响应，请检查 auth239/formula-service.log");
            }
            void importFile(Object f,Uri uri){operation(f,()->{
                AuthDocumentPicker241.requireDocument(uri);String name;
                try(Cursor cursor=app().getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                    if(cursor==null||!cursor.moveToFirst())throw new IOException("无法读取文件名");name=cursor.getString(0);
                }
                if(name==null||!name.toLowerCase(Locale.ROOT).endsWith(".glsl"))throw new IOException("请选择 .glsl 文件");
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=app().getContentResolver().openInputStream(uri)){
                    if(in==null)throw new IOException("无法读取文件");byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>16384)throw new IOException("文件超过 16 KiB");}
                }
                String next=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(out.toByteArray())).toString();if(next.startsWith("\uFEFF"))next=next.substring(1);next=AuthBody241.normalize(next);validate(next);
                if(custom())request(AuthFormula239.formula(strength(),protect(),range(),shape()));atomic(file("custom.glsl"),next);
                if(!prefs().edit().putString("name",name).putBoolean("enabled",false).commit())throw new IOException("Cannot save imported GLSL state");reload();
                Log.i("AuroraAUTH241","document_imported bytes="+next.getBytes(StandardCharsets.UTF_8).length+" enabled=false");
            },"已导入，可开启自定义着色器");}
            int compile(int type,String text)throws IOException{
                int shader=GLES20.glCreateShader(type);GLES20.glShaderSource(shader,text);GLES20.glCompileShader(shader);int[] ok=new int[1];GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);
                if(ok[0]==0){String error=GLES20.glGetShaderInfoLog(shader);GLES20.glDeleteShader(shader);throw new IOException("GLSL 编译失败："+error);}return shader;
            }
            void link(String vertexText,String fragmentText)throws IOException{
                int vertex=0,fragment=0,program=0;
                try{
                    vertex=compile(GLES20.GL_VERTEX_SHADER,vertexText);fragment=compile(GLES20.GL_FRAGMENT_SHADER,fragmentText);
                    program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vertex);GLES20.glAttachShader(program,fragment);GLES20.glLinkProgram(program);int[] ok=new int[1];GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
                    if(ok[0]==0)throw new IOException("GLSL 链接失败："+GLES20.glGetProgramInfoLog(program));
                }finally{if(program!=0)GLES20.glDeleteProgram(program);if(vertex!=0)GLES20.glDeleteShader(vertex);if(fragment!=0)GLES20.glDeleteShader(fragment);}
            }
            void validate(String next)throws Exception{
                next=AuthBody241.normalize(next);
                EGLDisplay display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);EGLContext context=EGL14.EGL_NO_CONTEXT;EGLSurface surface=EGL14.EGL_NO_SURFACE;int[] version=new int[2];
                if(!EGL14.eglInitialize(display,version,0,version,1))throw new IOException("无法初始化 GLES");
                try{
                    EGLConfig[] config=new EGLConfig[1];int[] count=new int[1];
                    if(!EGL14.eglChooseConfig(display,new int[]{EGL14.EGL_SURFACE_TYPE,EGL14.EGL_PBUFFER_BIT,EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_NONE},0,config,0,1,count,0)||count[0]==0)throw new IOException("无法选择 GLES 3 配置");
                    context=EGL14.eglCreateContext(display,config[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE},0);surface=EGL14.eglCreatePbufferSurface(display,config[0],new int[]{EGL14.EGL_WIDTH,1,EGL14.EGL_HEIGHT,1,EGL14.EGL_NONE},0);
                    if(!EGL14.eglMakeCurrent(display,surface,surface,context))throw new IOException("无法创建 GLES 上下文");
                    link("attribute vec4 position; varying vec2 tc; void main(){gl_Position=position;tc=position.xy;}",template.replace("/*PHOENIX_BODY*/",next));
                    String vertexText;try(InputStream in=app().getAssets().open("shading_script/vertex_normal.txt");ByteArrayOutputStream out=new ByteArrayOutputStream()){
                        byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);vertexText=new String(out.toByteArray(),StandardCharsets.UTF_8);
                    }
                    link(vertexText,AuthFormula239.preview(template,next));
                }finally{
                    EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context);EGL14.eglReleaseThread();
                }
            }
            void uniforms(Object renderer){
                int program=(Integer)XposedHelpers.getObjectField(renderer,"j");GLES20.glUseProgram(program);
                for(String name:new String[]{"DarkStrength","SmoothStartValue","SmoothEndValue","Falloff","NoiseStrength"}){
                    float value=name.equals("SmoothStartValue")?1.4f:name.equals("SmoothEndValue")?.2f:name.equals("NoiseStrength")?0f:1f;GLES20.glUniform1f(GLES20.glGetUniformLocation(program,name),value);
                }
                GLES20.glUniform1f(GLES20.glGetUniformLocation(program,"CurrentTime"),SystemClock.elapsedRealtime());
            }
            public void install(){
                try{unavailable=AuthReadiness239.check();template=asset();if(unavailable==null)reload();}
                catch(Exception failure){unavailable="AUTH 原生组件读取失败："+failure.getMessage();Log.e("AuroraAUTH239",unavailable,failure);}
                // After callbacks run in reverse priority order: Classic's 50 runs first.
                XposedBridge.hookMethod(settings,new StrictHook(100){@Override protected void after(MethodHookParam p){if(!p.hasThrowable())entry(p.thisObject);}});
                notes.install();
                XposedBridge.hookMethod(populate,new StrictHook(){@Override protected void before(MethodHookParam p){if(own(p.thisObject)){populate(p.thisObject);p.setResult(null);}}});
                XposedBridge.hookMethod(register,new StrictHook(){@Override protected void before(MethodHookParam p){if(own(p.thisObject))p.setResult(null);}});
                StrictHook title=new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&own(p.thisObject))com.aurora.camera.compat.ui.NativePageTitle257.set(activity(p.thisObject),experimental(p.thisObject)?"实验性":"徕卡经典暗角着色器");}};
                XposedBridge.hookMethod(initialize,title);XposedBridge.hookMethod(resume,title);
                XposedBridge.hookMethod(resume,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()&&own(p.thisObject))refresh();}});
                XposedBridge.hookMethod(result,new StrictHook(){@Override protected void after(MethodHookParam p){
                    if(p.hasThrowable()||!own(p.thisObject)||!experimental(p.thisObject))return;
                    if(documents.result(p.thisObject,(Integer)p.args[0],(Integer)p.args[1],(Intent)p.args[2],(fragment,uri)->importFile(fragment,uri))){
                        Log.i("AuroraAUTH241","document_picker_result request="+p.args[0]+" result="+p.args[1]);refresh();
                    }
                }});
                if(unavailable!=null){String blocked="AuroraAUTH248 hooks_blocked reason="+unavailable;Log.w("AuroraAUTH239",blocked+"; native hooks not registered");XposedBridge.log(blocked);return;}
                XposedBridge.hookMethod(library,new StrictHook(){@Override protected void before(MethodHookParam p){if(p.thisObject==h.loader&&"MiFilterSDK".equals(p.args[0])){String path=h.moduleApkPath+"!/lib/arm64-v8a/libMiFilterSDK.so";Log.i("AuroraAUTH241","preview_library_resolved path="+path);p.setResult(path);}}});
                runtime.install();
                XposedBridge.hookMethod(shaderSource,new StrictHook(){@Override protected void before(MethodHookParam p){p.setResult(AuthFormula239.preview(template,body));}});
                XposedBridge.hookMethod(shaderInit,new StrictHook(){@Override protected void after(MethodHookParam p){if(!p.hasThrowable()){uniforms(p.thisObject);XposedHelpers.setAdditionalInstanceField(p.thisObject,"aurora.auth239.revision",revision);}else Log.e("AuroraAUTH248","java_shader_compile_failed "+runtime.identity(),p.getThrowable());}});
                XposedBridge.hookMethod(shaderDraw,new StrictHook(){@Override protected void before(MethodHookParam p)throws Throwable{
                    Object previous=XposedHelpers.getAdditionalInstanceField(p.thisObject,"aurora.auth239.revision");
                    if(previous!=null&&(Long)previous!=revision){int program=(Integer)XposedHelpers.getObjectField(p.thisObject,"j");XposedHelpers.callStaticMethod(type("com.xiaomi.gl.MIGL"),"glDeleteProgram",program,"AUTH239");shaderInit.invoke(p.thisObject);}
                    uniforms(p.thisObject);
                }});
                String ready="AuroraAUTH248 hooks_ready "+runtime.identity();Log.i("AuroraAUTH248",ready);XposedBridge.log(ready);
            }
        };
    }
}
