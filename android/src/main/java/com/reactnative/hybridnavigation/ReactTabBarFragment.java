package com.reactnative.hybridnavigation;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.reactnative.hybridnavigation.Constants.ACTION_SET_TAB_ITEM;
import static com.reactnative.hybridnavigation.Constants.ACTION_UPDATE_TAB_BAR;
import static com.reactnative.hybridnavigation.Constants.ARG_ACTION;
import static com.reactnative.hybridnavigation.Constants.ARG_OPTIONS;
import static com.reactnative.hybridnavigation.HBDEventEmitter.EVENT_NAVIGATION;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_INDEX;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_ON;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_REQUEST_CODE;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_RESULT_CODE;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_RESULT_DATA;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_SCENE_ID;
import static com.reactnative.hybridnavigation.HBDEventEmitter.ON_COMPONENT_RESULT;
import static com.reactnative.hybridnavigation.Parameters.mergeOptions;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.uimanager.PixelUtil;
import com.navigation.androidx.AwesomeFragment;
import com.navigation.androidx.AwesomeToolbar;
import com.navigation.androidx.DefaultTabBarProvider;
import com.navigation.androidx.FragmentHelper;
import com.navigation.androidx.Style;
import com.navigation.androidx.TabBar;
import com.navigation.androidx.TabBarFragment;
import com.navigation.androidx.TabBarItem;
import com.navigation.androidx.TabBarProvider;
import com.navigation.androidx.TransitionAnimation;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by listen on 2018/1/15.
 */

public class ReactTabBarFragment extends TabBarFragment {

    private static final String SAVED_OPTIONS = "hybrid_options";

    private final ReactBridgeManager bridgeManager = ReactBridgeManager.get();

    @NonNull
    public ReactBridgeManager getReactBridgeManager() {
        return bridgeManager;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            options = savedInstanceState.getBundle(SAVED_OPTIONS);
        }
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        if (leftTabBarEnable) {
            root.post(() -> {
                int tabWidth = Math.round(PixelUtil.toPixelFromDIP(60));
                ReactTabBar tabBar = getTabBar();
                if (tabBar != null) {
                    FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tabWidth, MATCH_PARENT);
                    layoutParams.gravity = Gravity.LEFT;
                    tabBar.setLayoutParams(layoutParams);
                    FrameLayout container = (FrameLayout) tabBar.getChildAt(0);
                    layoutParams = new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
                    container.setLayoutParams(layoutParams);
                    int count = container.getChildCount();
                    for (int i = 0; i < count; i++) {
                        container.getChildAt(i).setLayoutParams(layoutParams);
                    }
                    FrameLayout contentView = root.findViewById(R.id.tabs_content);
                    layoutParams = (FrameLayout.LayoutParams) contentView.getLayoutParams();
                    layoutParams.width = root.getWidth() - tabWidth;
                    layoutParams.gravity = Gravity.RIGHT;
                    contentView.setLayoutParams(layoutParams);
                }
            });
        }
    }

    @Nullable
    @Override
    protected AwesomeToolbar onCreateToolbar(View parent) {
        return leftTabBarEnable ? null : super.onCreateToolbar(parent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bridgeManager.watchMemory(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle(SAVED_OPTIONS, options);
    }

    private final static TransitionAnimation FadeShort = new TransitionAnimation(R.anim.nav_fade_in_short, R.anim.nav_fade_out_short, R.anim.nav_fade_in_short, R.anim.nav_fade_out_short);
    private final static TransitionAnimation DelayShort = new TransitionAnimation(R.anim.nav_delay_short, R.anim.nav_delay_short, R.anim.nav_delay_short, R.anim.nav_delay_short);

    @Override
    protected void setPresentAnimation(AwesomeFragment current, AwesomeFragment previous) {
        ReactFragment reactFragment = Utils.findReactFragment(current);
        if (reactFragment != null && !reactFragment.isFirstRenderCompleted()) {
            List<AwesomeFragment> children = getChildFragments();
            if (children.indexOf(current) > children.indexOf(previous)) {
                current.setAnimation(FadeShort);
                previous.setAnimation(DelayShort);
            } else {
                current.setAnimation(TransitionAnimation.None);
                previous.setAnimation(FadeShort);
            }
            return;
        }
        super.setPresentAnimation(current, previous);
    }

    @Override
    protected void onCustomStyle(@NonNull Style style) {
        super.onCustomStyle(style);
        Bundle options = getOptions();
        String tabBarColor = options.getString("tabBarColor");
        if (tabBarColor != null) {
            style.setTabBarBackgroundColor(tabBarColor);
        }

        String tabBarItemColor = options.getString("tabBarItemColor");
        String tabBarUnselectedItemColor = options.getString("tabBarUnselectedItemColor");

        if (tabBarItemColor != null) {
            style.setTabBarItemColor(tabBarItemColor);
            style.setTabBarUnselectedItemColor(tabBarUnselectedItemColor);
        } else {
            options.putString("tabBarItemColor", style.getTabBarItemColor());
            options.putString("tabBarUnselectedItemColor", style.getTabBarUnselectedItemColor());
            options.putString("tabBarBadgeColor", style.getTabBarBadgeColor());
        }

        Bundle shadowImage = options.getBundle("tabBarShadowImage");
        if (shadowImage != null) {
            style.setTabBarShadow(Utils.createTabBarShadow(requireContext(), shadowImage));
        }
    }

    @Override
    public void updateTabBar(Bundle options) {
        super.updateTabBar(options);
        if (getTabBarProvider() instanceof DefaultTabBarProvider) {
            String action = options.getString(ARG_ACTION);
            if (action == null) {
                return;
            }

            switch (action) {
                case ACTION_SET_TAB_ITEM:
                    setTabItem(options.getParcelableArrayList(ARG_OPTIONS));
                    break;
                case ACTION_UPDATE_TAB_BAR:
                    updateTabBarAppearance(options.getBundle(ARG_OPTIONS));
                    break;
            }
        }
    }

    private void setTabItem(@Nullable ArrayList<Bundle> options) {
        if (options == null) {
            return;
        }

        TabBar tabBar = getTabBar();
        if (tabBar == null) {
            return;
        }

        for (Bundle option : options) {
            int index = (int) option.getDouble("index");
            TabBarItem tabBarItem = tabBar.getTabBarItem(index);
            if (tabBarItem == null) {
                continue;
            }

            // title
            String title = option.getString("title");
            if (title != null) {
                tabBarItem.title = title;
            }

            // icon
            Bundle icon = option.getBundle("icon");
            if (icon != null) {
                Bundle unselected = icon.getBundle("unselected");
                Bundle selected = icon.getBundle("selected");
                if (unselected != null) {
                    tabBarItem.unselectedIconUri = unselected.getString("uri");
                }
                tabBarItem.iconUri = selected.getString("uri");
            }

            // badge
            Bundle badge = option.getBundle("badge");
            if (badge != null) {
                boolean hidden = badge.getBoolean("hidden", true);
                String text = !hidden ? badge.getString("text", "") : "";
                boolean dot = !hidden && badge.getBoolean("dot", false);

                tabBarItem.badgeText = text;
                tabBarItem.showDotBadge = dot;
            }
            
            tabBar.renderTabView(index);
        }
    }

    private void updateTabBarAppearance(@Nullable Bundle options) {
        TabBar tabBar = getTabBar();
        if (options == null || tabBar == null) {
            return;
        }

        String tabBarColor = options.getString("tabBarColor");
        Bundle shadowImage = options.getBundle("tabBarShadowImage");
        String tabBarItemColor = options.getString("tabBarItemColor");
        String tabBarUnselectedItemColor = options.getString("tabBarUnselectedItemColor");

        setOptions(mergeOptions(getOptions(), options));

        if (tabBarColor != null) {
            mStyle.setTabBarBackgroundColor(tabBarColor);
            tabBar.setBarBackgroundColor(tabBarColor);
            setNeedsNavigationBarAppearanceUpdate();
        }

        if (shadowImage != null) {
            tabBar.setShadowDrawable(Utils.createTabBarShadow(requireContext(), shadowImage));
        }

        if (tabBarItemColor != null) {
            tabBar.setSelectedItemColor(tabBarItemColor);
        }

        if (tabBarUnselectedItemColor != null) {
            tabBar.setUnselectedItemColor(tabBarUnselectedItemColor);
        }

        tabBar.initialise(tabBar.getCurrentSelectedPosition());
    }

    private Bundle options;

    @NonNull
    public Bundle getOptions() {
        if (options == null) {
            Bundle args = FragmentHelper.getArguments(this);
            options = args.getBundle(ARG_OPTIONS);
            if (options == null) {
                options = new Bundle();
            }
        }
        return options;
    }

    public void setOptions(@NonNull Bundle options) {
        this.options = options;
    }

    public void setIntercepted(boolean intercepted) {
        this.intercepted = intercepted;
    }

    private boolean intercepted = true;

    @Override
    public void setSelectedIndex(int index, @Nullable Runnable completion) {
        if (!isAdded()) {
            super.setSelectedIndex(index, completion);
            return;
        }

        if (shouldIntercept(index)) {
            TabBarProvider tabBarProvider = getTabBarProvider();
            if (tabBarProvider != null) {
                tabBarProvider.setSelectedIndex(getSelectedIndex());
            }
            return;
        }

        super.setSelectedIndex(index, completion);
        intercepted = true;
    }

    private boolean shouldIntercept(int index) {
        if (bridgeManager.hasRootLayout() && this.intercepted) {
            Bundle data = new Bundle();
            data.putString(KEY_SCENE_ID, getSceneId());
            data.putString(KEY_INDEX, getSelectedIndex() + "-" + index);
            HBDEventEmitter.sendEvent(HBDEventEmitter.EVENT_SWITCH_TAB, Arguments.fromBundle(data));
            return true;
        }
        return false;
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        Bundle options = getOptions();
        String tabBarModuleName = options.getString("tabBarModuleName");
        if (tabBarModuleName != null) {
            Bundle result = new Bundle();
            result.putInt(KEY_REQUEST_CODE, requestCode);
            result.putInt(KEY_RESULT_CODE, resultCode);
            result.putBundle(KEY_RESULT_DATA, data);
            result.putString(KEY_SCENE_ID, getSceneId());
            result.putString(KEY_ON, ON_COMPONENT_RESULT);
            HBDEventEmitter.sendEvent(EVENT_NAVIGATION, Arguments.fromBundle(result));
        }
    }

    public Style getStyle() {
        return mStyle;
    }

    private boolean leftTabBarEnable = false;

    public void setLeftTabBarEnable() {
        leftTabBarEnable = true;
    }

}
