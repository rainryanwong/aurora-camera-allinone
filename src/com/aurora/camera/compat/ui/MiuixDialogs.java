package com.aurora.camera.compat.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import com.aurora.camera.compat.Host;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Exact stock 1770 MIUIX builder, shared by module-owned settings surfaces. */
public final class MiuixDialogs {
    private final Constructor<?> constructor;
    private final Method title,message,items,choices,positive,negative,view,create;
    public MiuixDialogs(Host h)throws ReflectiveOperationException {
        Class<?> b=h.type("miuix.appcompat.app.l$a");
        constructor=b.getDeclaredConstructor(Context.class);constructor.setAccessible(true);
        title=h.method(b,"D",CharSequence.class);message=h.method(b,"o",CharSequence.class);
        items=h.method(b,"m",CharSequence[].class,DialogInterface.OnClickListener.class);
        choices=h.method(b,"B",CharSequence[].class,int.class,DialogInterface.OnClickListener.class);
        positive=h.method(b,"z",CharSequence.class,DialogInterface.OnClickListener.class);
        negative=h.method(b,"r",CharSequence.class,DialogInterface.OnClickListener.class);
        view=h.method(b,"E",View.class);create=h.method(b,"c");
        if(!Dialog.class.isAssignableFrom(create.getReturnType()))throw new NoSuchMethodException("MIUIX dialog result");
    }
    public Builder builder(Context context){try{return new Builder(constructor.newInstance(context));}catch(ReflectiveOperationException e){throw new IllegalStateException("MIUIX builder",e);}}
    public final class Builder {
        private final Object nativeBuilder;
        private Builder(Object value){nativeBuilder=value;}
        private Builder call(Method m,Object... args){try{m.invoke(nativeBuilder,args);return this;}catch(ReflectiveOperationException e){throw new IllegalStateException("MIUIX "+m.getName(),e);}}
        public Builder setTitle(CharSequence text){return call(title,text);}
        public Builder setMessage(CharSequence text){return call(message,text);}
        public Builder setItems(CharSequence[] values,DialogInterface.OnClickListener listener){return call(items,values,listener);}
        public Builder setSingleChoiceItems(CharSequence[] values,int selected,DialogInterface.OnClickListener listener){return call(choices,values,selected,listener);}
        public Builder setPositiveButton(CharSequence text,DialogInterface.OnClickListener listener){return call(positive,text,listener);}
        public Builder setNegativeButton(CharSequence text,DialogInterface.OnClickListener listener){return call(negative,text,listener);}
        public Builder setView(View content){return call(view,content);}
        public Dialog create(){try{return (Dialog)create.invoke(nativeBuilder);}catch(ReflectiveOperationException e){throw new IllegalStateException("MIUIX dialog",e);}}
        public Dialog show(){Dialog d=create();d.show();return d;}
    }
}
