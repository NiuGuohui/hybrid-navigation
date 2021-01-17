package com.reactnative.hybridnavigation.navigator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.navigation.androidx.AwesomeFragment;
import com.navigation.androidx.DrawerFragment;
import com.navigation.androidx.NavigationFragment;
import com.navigation.androidx.TabBarFragment;
import com.reactnative.hybridnavigation.HybridFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NavigatorRegistry {

    private final List<String> layouts = new ArrayList<>();
    private final HashMap<String, Navigator> actionNavigatorPairs = new HashMap<>();
    private final HashMap<String, Navigator> layoutNavigatorPairs = new HashMap<>();
    private final HashMap<Class<?>, String> classLayoutPairs = new HashMap<>();

    public NavigatorRegistry() {
        register(new ScreenNavigator());
        register(new StackNavigator());
        register(new TabNavigator());
        register(new DrawerNavigator());
    }

    public void register(@NonNull Navigator navigator) {
        layouts.add(navigator.name());

        for (String action : navigator.supportActions()) {
            if (actionNavigatorPairs.containsKey(action)) {
                Navigator duplicated = actionNavigatorPairs.get(action);
                throw new IllegalArgumentException(navigator.getClass().getName() + " 想要注册的 action " + action + " 已经被 " + duplicated.getClass().getName() + " 所注册。");
            }
            actionNavigatorPairs.put(action, navigator);
        }

        String layout = navigator.name();
        if (layoutNavigatorPairs.containsKey(layout)) {
            Navigator duplicated = layoutNavigatorPairs.get(layout);
            throw new IllegalArgumentException("Duplicated layout " + layout + ", which has registered through " + duplicated.getClass().getName());
        }
        layoutNavigatorPairs.put(layout, navigator);
    }

    @Nullable
    public Navigator navigatorForAction(@NonNull String action) {
        return actionNavigatorPairs.get(action);
    }

    @Nullable
    public Navigator navigatorForLayout(@NonNull String layout) {
        return layoutNavigatorPairs.get(layout);
    }

    @Nullable
    public String layoutForFragment(@NonNull AwesomeFragment fragment) {
        String layout = classLayoutPairs.get(fragment.getClass());
        if (layout == null) {
            if (fragment instanceof HybridFragment) {
                return "screen";
            }
            if (fragment instanceof NavigationFragment) {
                return "stack";
            }
            if (fragment instanceof TabBarFragment) {
                return "tabs";
            }
            if (fragment instanceof DrawerFragment) {
                return "drawer";
            }
        }
        return layout;
    }

    public void setLayoutForFragment(@NonNull String layout, @NonNull AwesomeFragment fragment) {
        String current = classLayoutPairs.get(fragment.getClass());
        if (current == null || !current.equals(layout)) {
            classLayoutPairs.put(fragment.getClass(), layout);
        }
    }

    public List<String> allLayouts() {
        return layouts;
    }
}
