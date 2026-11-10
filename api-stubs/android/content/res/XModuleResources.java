package android.content.res;
/** Compile-only Xposed API declaration; framework supplies the implementation. */
public class XModuleResources extends Resources {
    private XModuleResources(){super(null,null,null);}
    public static XModuleResources createInstance(String path,XResources parent){throw new UnsupportedOperationException("compile-only");}
}
