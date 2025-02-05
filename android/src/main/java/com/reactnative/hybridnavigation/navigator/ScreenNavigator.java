package com.reactnative.hybridnavigation.navigator;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.navigation.androidx.AwesomeFragment;
import com.navigation.androidx.FragmentHelper;
import com.navigation.androidx.PresentationStyle;
import com.navigation.androidx.TransitionAnimation;
import com.reactnative.hybridnavigation.HybridFragment;
import com.reactnative.hybridnavigation.ReactBridgeManager;
import com.reactnative.hybridnavigation.ReactStackFragment;

import java.util.Arrays;
import java.util.List;


public class ScreenNavigator implements Navigator {

    final static String TAG = "Navigator";

    private final List<String> supportActions = Arrays.asList("present", "presentLayout", "dismiss", "showModal", "showModalLayout", "hideModal");

    @Override
    @NonNull
    public String name() {
        return "screen";
    }

    @Override
    @NonNull
    public List<String> supportActions() {
        return supportActions;
    }

    @Override
    @Nullable
    public AwesomeFragment createFragment(@NonNull ReadableMap layout) {
        if (layout.hasKey(name())) {
            ReadableMap screen = layout.getMap(name());
            if (screen == null) {
                throw new IllegalArgumentException("screen should be an object.");
            }

            String moduleName = screen.getString("moduleName");
            if (moduleName == null) {
                throw new IllegalArgumentException("moduleName is required.");
            }

            Bundle props = buildProps(screen);
            Bundle options = buildOptions(screen);

            return getReactBridgeManager().createFragment(moduleName, props, options);
        }
        return null;
    }

    @Override
    public Bundle buildRouteGraph(@NonNull AwesomeFragment fragment) {
        if (!(fragment instanceof HybridFragment)) {
            return null;
        }
        if (!fragment.isAdded()) {
            return null;
        }

        HybridFragment screen = (HybridFragment) fragment;
        Bundle route = new Bundle();
        route.putString("layout", name());
        route.putString("sceneId", screen.getSceneId());
        route.putString("moduleName", screen.getModuleName());
        route.putString("mode", Navigator.Util.getMode(fragment));
        return route;
    }

    @Override
    public HybridFragment primaryFragment(@NonNull AwesomeFragment fragment) {
        if (!(fragment instanceof HybridFragment)) {
            return null;
        }
        if (!fragment.isAdded()) {
            return null;
        }

        AwesomeFragment presented = FragmentHelper.getFragmentAfter(fragment);
        if (presented == null) {
            return (HybridFragment) fragment;
        }
        return (HybridFragment) presented;
    }

    @Override
    public void handleNavigation(@NonNull AwesomeFragment target, @NonNull String action, @NonNull ReadableMap extras, @NonNull Promise promise) {
        switch (action) {
            case "present":
                handlePresent(target, extras, promise);
                break;
            case "dismiss":
                handleDismiss(target, promise);
                break;
            case "showModal":
                handleShowModal(target, extras, promise);
                break;
            case "hideModal":
                handleHideModal(target, promise);
                break;
            case "presentLayout":
                handlePresentLayout(target, extras, promise);
                break;
            case "showModalLayout":
                handleShowModalLayout(target, extras, promise);
                break;

        }
    }

    private void handleShowModalLayout(@NonNull AwesomeFragment presenting, @NonNull ReadableMap extras, @NonNull Promise promise) {
        ReadableMap layout = extras.getMap("layout");
        AwesomeFragment presented = getReactBridgeManager().createFragment(layout);
        if (presented == null) {
            promise.resolve(false);
            return;
        }
        presented.setPresentationStyle(PresentationStyle.OverFullScreen);
        int requestCode = extras.getInt("requestCode");
        presenting.presentFragment(presented, requestCode, TransitionAnimation.None, () -> promise.resolve(true));
    }

    private void handlePresentLayout(@NonNull AwesomeFragment presenting, @NonNull ReadableMap extras, @NonNull Promise promise) {
        ReadableMap layout = extras.getMap("layout");
        AwesomeFragment presented = getReactBridgeManager().createFragment(layout);
        if (presented == null) {
            promise.resolve(false);
            return;
        }
        int requestCode = extras.getInt("requestCode");
        presenting.presentFragment(presented, requestCode, () -> promise.resolve(true));
    }

    private void handleShowModal(@NonNull AwesomeFragment presenting, @NonNull ReadableMap extras, @NonNull Promise promise) {
        AwesomeFragment presented = createFragmentWithExtras(extras);
        if (presented == null) {
            promise.resolve(false);
            return;
        }
        presented.setPresentationStyle(PresentationStyle.OverFullScreen);
        int requestCode = extras.getInt("requestCode");
        presenting.presentFragment(presented, requestCode, TransitionAnimation.Fade, () -> promise.resolve(true));
    }
    
    private void handleHideModal(@NonNull AwesomeFragment target, @NonNull Promise promise) {
        AwesomeFragment presenting = target.getPresentingFragment();
        if (presenting == null) {
            target.dismissFragment(TransitionAnimation.Fade, () -> promise.resolve(true));
            return;
        }
        presenting.dismissFragment(TransitionAnimation.Fade, () -> promise.resolve(true));
    }

    private void handleDismiss(@NonNull AwesomeFragment target, @NonNull Promise promise) {
        AwesomeFragment presenting = target.getPresentingFragment();
        if (presenting == null) {
            target.dismissFragment(() -> promise.resolve(true));
            return;
        }
        presenting.dismissFragment(() -> promise.resolve(true));
    }

    private void handlePresent(@NonNull AwesomeFragment target, @NonNull ReadableMap extras, @NonNull Promise promise) {
        AwesomeFragment fragment = createFragmentWithExtras(extras);
        if (fragment == null) {
            promise.resolve(false);
            return;
        }
        int requestCode = extras.getInt("requestCode");
        ReactStackFragment stackFragment = new ReactStackFragment();
        stackFragment.setRootFragment(fragment);
        target.presentFragment(stackFragment, requestCode, () -> promise.resolve(true));
    }
    
    private AwesomeFragment createFragmentWithExtras(@NonNull ReadableMap extras) {
        if (!extras.hasKey("moduleName")) {
            return null;
        }

        String moduleName = extras.getString("moduleName");
        if (moduleName == null) {
            return null;
        }

        Bundle props = buildProps(extras);
        Bundle options = buildOptions(extras);
        return getReactBridgeManager().createFragment(moduleName, props, options);
    }

    @Nullable
    private Bundle buildOptions(@NonNull ReadableMap extras) {
        if (!extras.hasKey("options")) {
            return null;
        }
        return Arguments.toBundle(extras.getMap("options"));
    }

    @Nullable
    private Bundle buildProps(@NonNull ReadableMap extras) {
        if (!extras.hasKey("props")) {
            return null;
        }
        return Arguments.toBundle(extras.getMap("props"));
    }

    private ReactBridgeManager getReactBridgeManager() {
        return ReactBridgeManager.get();
    }
}
