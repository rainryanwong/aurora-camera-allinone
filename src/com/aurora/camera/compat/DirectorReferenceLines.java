package com.aurora.camera.compat;

import android.view.View;
import com.aurora.camera.diagnostics.HookDiagnostics;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

final class DirectorReferenceLines {
    static final int ITEM_ID = 185;
    private static final int DIRECTOR_MODE = 164;
    private static final int CARD_VIEW_TYPE = 2;
    private static final int TITLE = 0x7f140f11;
    private static final int OFF_ICON = 0x7f080932;
    private static final int GOLDEN_SECTION_ICON = 0x7f08093a;
    private static final int GRID_ICON = 0x7f08093b;

    private final Class<?> modelType;
    private final Constructor<?> itemConstructor;
    private final Constructor<?> itemBuilderConstructor;
    private final Constructor<?> resourceBuilderConstructor;
    private final Method buildResource;
    private final Field builderId;
    private final Field builderProvider;
    private final Field builderClick;
    private final Field tileTitle;
    private final Field tileIcon;
    private final Field tileChecked;
    private final Method dataRunning;
    private final Method component;
    private final Method componentValue;
    private final Method menuProtocol;
    private final Method expand;
    private final Method topProtocol;
    private final Method expandTop;
    private final Field builderTopProvider;
    private final Field builderTopClick;
    private final Object topProvider;
    private final Method menuViewType;
    private final Field menuItems;
    private final Field itemProvider;
    private final Object provider;

    DirectorReferenceLines(Host host) throws ReflectiveOperationException {
        Class<?> itemType = host.type("b5.i");
        Class<?> builderType = host.type("b5.i$a");
        Class<?> providerType = host.type("b5.i$b");
        Class<?> topProviderType = host.type("b5.i$c");
        Class<?> resourceBuilderType = host.type("b5.a$a");
        Class<?> topResourceType = host.type("b5.j$a");
        Class<?> componentType = host.type("com.android.camera.data.data.c");
        modelType = host.type("t2.b");
        itemConstructor = itemType.getConstructor(builderType);
        itemBuilderConstructor = builderType.getConstructor();
        resourceBuilderConstructor = resourceBuilderType.getConstructor();
        buildResource = host.method(resourceBuilderType, "a");
        builderId = host.field(builderType, "a");
        builderProvider = host.field(builderType, "d");
        builderClick = host.field(builderType, "f");
        builderTopProvider = host.field(builderType, "c");
        builderTopClick = host.field(builderType, "e");
        tileIcon = host.field(resourceBuilderType, "a");
        tileTitle = host.field(resourceBuilderType, "c");
        tileChecked = host.field(resourceBuilderType, "f");
        dataRunning = host.method("g2.a", "h");
        component = host.method("Xh.b", "x", Class.class);
        componentValue = host.method(modelType, "getComponentValue", int.class);
        menuProtocol = host.method("R6.s1", "a");
        expand = host.method("R6.s1", "H3", componentType, View.class, int.class);
        topProtocol = host.method("R6.o1", "a");
        expandTop = host.method("R6.o1", "Lj", componentType, View.class, int.class);
        menuViewType = host.method("p5.c", "getItemViewType", int.class);
        menuItems = host.field("p5.c", "b");
        itemProvider = host.field(itemType, "h");
        provider = Proxy.newProxyInstance(host.loader, new Class<?>[] {providerType},
                (proxy, method, args) -> method.getDeclaringClass() == Object.class
                        ? method.invoke(this, args) : resource());
        Constructor<?> topResource = topResourceType.getConstructor();
        Method buildTopResource = host.method(topResourceType, "a");
        Field topIcon = host.field(topResourceType, "a");
        Field topTitle = host.field(topResourceType, "e");
        Field topChecked = host.field(topResourceType, "g");
        topProvider = Proxy.newProxyInstance(host.loader, new Class<?>[] {topProviderType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) return method.invoke(this, args);
                    String value = (String) componentValue.invoke(model(), DIRECTOR_MODE);
                    Object result = topResource.newInstance();
                    topIcon.setInt(result, icon(value));
                    topTitle.setInt(result, TITLE);
                    topChecked.setBoolean(result, !"off".equals(value));
                    return buildTopResource.invoke(result);
                });
    }

    Object item() throws ReflectiveOperationException {
        Object builder = itemBuilderConstructor.newInstance();
        builderId.setInt(builder, ITEM_ID);
        builderProvider.set(builder, provider);
        builderClick.set(builder, (View.OnClickListener) view -> show(menuProtocol, expand, view));
        builderTopProvider.set(builder, topProvider);
        builderTopClick.set(builder, (View.OnClickListener) view -> show(topProtocol, expandTop, view));
        return itemConstructor.newInstance(builder);
    }

    void install() {
        HookDiagnostics.hookMethod(menuViewType, new StrictHook() {
            @Override protected void before(MethodHookParam param) throws IllegalAccessException {
                List<?> items = (List<?>) menuItems.get(param.thisObject);
                Object item = items.get((int) param.args[0]);
                if (itemProvider.get(item) == provider) {
                    param.setResult(CARD_VIEW_TYPE);
                    param.setObjectExtra("director-reference-line-card", Boolean.TRUE);
                }
            }

            @Override protected boolean countsAsRun(MethodHookParam param) {
                return Boolean.TRUE.equals(param.getObjectExtra("director-reference-line-card"));
            }
        });
    }

    private Object model() throws ReflectiveOperationException {
        return component.invoke(dataRunning.invoke(null), modelType);
    }

    private Object resource() throws ReflectiveOperationException {
        String value = (String) componentValue.invoke(model(), DIRECTOR_MODE);
        Object builder = resourceBuilderConstructor.newInstance();
        tileTitle.setInt(builder, TITLE);
        tileIcon.setInt(builder, icon(value));
        tileChecked.setBoolean(builder, !"off".equals(value));
        return buildResource.invoke(builder);
    }

    private static int icon(String value) {
        switch (value) {
            case "golden_section":
                return GOLDEN_SECTION_ICON;
            case "jiugongge":
                return GRID_ICON;
            default:
                return OFF_ICON;
        }
    }

    private void show(Method lookup, Method show, View anchor) {
        try {
            Optional<?> protocol = (Optional<?>) lookup.invoke(null);
            if (protocol.isPresent()) show.invoke(protocol.get(), model(), anchor, ITEM_ID);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Unable to open reference line options", failure);
        }
    }
}
