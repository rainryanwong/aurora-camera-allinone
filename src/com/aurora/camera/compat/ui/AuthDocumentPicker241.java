package com.aurora.camera.compat.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.aurora.camera.compat.Host;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Let the host FragmentManager retain request ownership across recreation. */
public final class AuthDocumentPicker241 {
    public static final int REQUEST=0x5391;
    private static final String PENDING="aurora_auth_document_pending241";
    public interface Import{void accept(Object fragment,Uri uri);}
    private final Method arguments,start;
    public AuthDocumentPicker241(Host host)throws ReflectiveOperationException{
        arguments=host.method("androidx.fragment.app.Fragment","getArguments");
        start=host.method("androidx.fragment.app.Fragment","startActivityForResult",Intent.class,int.class);
    }
    private static RuntimeException failed(String operation,ReflectiveOperationException failure){
        Throwable cause=failure instanceof InvocationTargetException?((InvocationTargetException)failure).getCause():failure;
        return cause instanceof RuntimeException?(RuntimeException)cause:new IllegalStateException(operation,cause);
    }
    private Bundle state(Object fragment){
        try{Bundle state=(Bundle)arguments.invoke(fragment);if(state==null)throw new IllegalStateException("AUTH experimental fragment has no arguments");return state;}
        catch(ReflectiveOperationException failure){throw failed("AUTH document arguments",failure);}
    }
    public boolean pending(Object fragment){return state(fragment).getBoolean(PENDING,false);}
    public boolean choose(Object fragment){
        Bundle state=state(fragment);if(state.getBoolean(PENDING,false))return false;
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        state.putBoolean(PENDING,true);
        try{start.invoke(fragment,intent,REQUEST);return true;}
        catch(ReflectiveOperationException failure){state.putBoolean(PENDING,false);throw failed("AUTH document chooser",failure);}
    }
    public boolean result(Object fragment,int request,int result,Intent data,Import imported){
        if(request!=REQUEST||!pending(fragment))return false;
        state(fragment).putBoolean(PENDING,false);
        if(result==Activity.RESULT_OK)imported.accept(fragment,data==null?null:data.getData());
        return true;
    }
    public static void requireDocument(Uri uri)throws IOException{
        if(uri==null)throw new IOException("未返回 GLSL 文件");
        if(!"content".equals(uri.getScheme()))throw new IOException("文件选择器未返回可读取的文档 URI");
    }
}
