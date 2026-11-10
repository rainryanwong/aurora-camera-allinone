package com.aurora.camera.compat.filter;

import android.os.IBinder;
import android.os.RemoteException;
import com.aurora.camera.compat.Host;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.NoSuchElementException;

/** Stock AIDL and donor MiSys V2 HIDL buffer-write protocols. */
final class NativeVideoLutWriter256 {
    private static final String AIDL_SERVICE="vendor.xiaomi.hardware.misys.common.IMiSysImpl/default";
    private static final String HIDL_INTERFACE="vendor.xiaomi.hardware.misys@2.0::IMiSys";
    private final Field aidlCache;
    private final Class<?> serviceFailure;
    private final Method checkService,asInterface,aidlWrite,getHidlService,transact;
    private final Constructor<?> parcel;
    private final Method writeToken,writeString,writeBytes,writeLength,verifySuccess,readResult,releaseStorage,release;

    NativeVideoLutWriter256(Host h)throws ReflectiveOperationException{
        aidlCache=h.field("on.a","a");serviceFailure=h.type("android.os.ServiceSpecificException");
        checkService=h.method("android.os.ServiceManager","checkService",String.class);
        asInterface=h.method("vendor.xiaomi.hardware.misys.common.IMiSysImpl$Stub","asInterface",IBinder.class);
        aidlWrite=h.method("vendor.xiaomi.hardware.misys.common.IMiSysImpl","MiSysWriteBuffer",String.class,String.class,byte[].class,long.class);
        getHidlService=h.method("android.os.HwBinder","getService",String.class,String.class,boolean.class);
        Class<?> p=h.type("android.os.HwParcel");parcel=p.getDeclaredConstructor();parcel.setAccessible(true);
        transact=h.method("android.os.IHwBinder","transact",int.class,p,p,int.class);
        writeToken=h.method(p,"writeInterfaceToken",String.class);writeString=h.method(p,"writeString",String.class);
        writeBytes=h.method(p,"writeInt8Vector",ArrayList.class);writeLength=h.method(p,"writeInt64",long.class);
        verifySuccess=h.method(p,"verifySuccess");readResult=h.method(p,"readInt32");
        releaseStorage=h.method(p,"releaseTemporaryStorage");release=h.method(p,"release");
    }

    void write(String directory,String name,byte[] bytes)throws ReflectiveOperationException,IOException{
        Object service=aidlCache.get(null);
        if(service==null){
            IBinder binder=(IBinder)invoke(checkService,null,AIDL_SERVICE);
            if(binder!=null)service=invoke(asInterface,null,binder);
        }
        if(service!=null){
            invoke(aidlWrite,service,directory,name,bytes,(long)bytes.length);
            return;
        }
        Object binder;
        try{binder=invoke(getHidlService,null,HIDL_INTERFACE,"default",false);}
        catch(InvocationTargetException failure){
            if(failure.getCause() instanceof NoSuchElementException)throw new IOException("MiSys V2 HIDL service is unavailable",failure.getCause());
            throw failure;
        }
        if(binder==null)throw new IOException("MiSys V2 HIDL service is unavailable");
        ArrayList<Byte> data=new ArrayList<>(bytes.length);for(byte value:bytes)data.add(value);
        Object request=parcel.newInstance(),reply=parcel.newInstance();
        try{
            invoke(writeToken,request,HIDL_INTERFACE);invoke(writeString,request,directory);invoke(writeString,request,name);
            invoke(writeBytes,request,data);invoke(writeLength,request,(long)data.size());
            invoke(transact,binder,2,request,reply,0);invoke(verifySuccess,reply);
            int result=(Integer)invoke(readResult,reply);
            if(result!=0)throw new IOException("MiSys V2 HIDL write returned "+result+" for "+name);
        }finally{
            try{invoke(releaseStorage,request);}finally{invoke(release,reply);}
        }
    }

    private Object invoke(Method method,Object owner,Object... arguments)throws ReflectiveOperationException,IOException{
        try{return method.invoke(owner,arguments);}
        catch(InvocationTargetException failure){
            Throwable cause=failure.getCause();
            if(cause instanceof RemoteException||serviceFailure.isInstance(cause)||cause instanceof SecurityException)
                throw new IOException("MiSys "+method.getName()+" failed",cause);
            throw failure;
        }
    }
}
