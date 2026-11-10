package com.aurora.camera.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Exact 1770 host declarations; callers name the real declaring owner. */
public final class Host {
    /** Optional editor-only symbol map; camera callers retain exact stock declarations. */
    public interface Symbols {
        String typeName(String requested) throws ClassNotFoundException;
        String methodName(String actualOwner, String requested, String actualParameters) throws NoSuchMethodException;
        String fieldName(String actualOwner, String requested) throws NoSuchFieldException;
        int[] resourceConstants(String actualMethodId);
        default boolean usesNativeCloudFilter() { return false; }
    }
    public final ClassLoader loader;
    public final String moduleApkPath;
    private final Symbols symbols;

    public Host(ClassLoader loader) {
        this(loader, null);
    }

    public Host(ClassLoader loader, String moduleApkPath) {
        this(loader, moduleApkPath, null);
    }

    public Host(ClassLoader loader, String moduleApkPath, Symbols symbols) {
        this.loader = loader;
        this.moduleApkPath = moduleApkPath;
        this.symbols = symbols;
    }

    public boolean usesNativeCloudFilter() {
        return symbols != null && symbols.usesNativeCloudFilter();
    }

    public Class<?> type(String binaryName) throws ClassNotFoundException {
        return Class.forName(symbols == null ? binaryName : symbols.typeName(binaryName), false, loader);
    }

    public Method method(String className, String name, Class<?>... parameters)
            throws ReflectiveOperationException {
        return method(type(className), name, parameters);
    }

    public Method method(Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        String actualName = name;
        if (symbols != null) {
            StringBuilder descriptor = new StringBuilder("(");
            for (Class<?> parameter : parameters) descriptor.append(descriptor(parameter));
            descriptor.append(')');
            actualName = symbols.methodName(owner.getName(), name, descriptor.toString());
        }
        Method result = owner.getDeclaredMethod(actualName, parameters);
        result.setAccessible(true);
        return result;
    }

    public Field field(String className, String name) throws ReflectiveOperationException {
        return field(type(className), name);
    }

    public Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field result = owner.getDeclaredField(symbols == null ? name : symbols.fieldName(owner.getName(), name));
        result.setAccessible(true);
        return result;
    }

    public int[] resourceConstants(Method method) {
        if (symbols == null) return null;
        StringBuilder id = new StringBuilder(descriptor(method.getDeclaringClass())).append("->")
                .append(method.getName()).append('(');
        for (Class<?> parameter : method.getParameterTypes()) id.append(descriptor(parameter));
        id.append(')').append(descriptor(method.getReturnType()));
        return symbols.resourceConstants(id.toString());
    }

    private static String descriptor(Class<?> type) {
        if (type.isArray()) return type.getName().replace('.', '/');
        if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        return "D";
    }
}
