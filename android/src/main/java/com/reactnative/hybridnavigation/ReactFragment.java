package com.reactnative.hybridnavigation;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.reactnative.hybridnavigation.HBDEventEmitter.EVENT_NAVIGATION;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_ON;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_REQUEST_CODE;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_RESULT_CODE;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_RESULT_DATA;
import static com.reactnative.hybridnavigation.HBDEventEmitter.KEY_SCENE_ID;
import static com.reactnative.hybridnavigation.HBDEventEmitter.ON_COMPONENT_APPEAR;
import static com.reactnative.hybridnavigation.HBDEventEmitter.ON_COMPONENT_DISAPPEAR;
import static com.reactnative.hybridnavigation.HBDEventEmitter.ON_COMPONENT_RESULT;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;
import com.navigation.androidx.FragmentHelper;
import com.navigation.androidx.Style;
import com.navigation.androidx.TransitionAnimation;

/**
 * Created by Listen on 2018/1/15.
 */
public class ReactFragment extends HybridFragment implements ReactRootViewHolder.VisibilityObserver, ReactBridgeManager.ReactBridgeReloadListener {

    protected static final String TAG = "Navigator";

    private ViewGroup containerLayout;
    private ReactRootViewHolder reactRootViewHolder;
    private HBDReactRootView reactRootView;
    private HBDReactRootView reactTitleView;
    private boolean firstRenderCompleted;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.nav_fragment_react, container, false);
        containerLayout = view.findViewById(R.id.react_content);
        if (containerLayout instanceof ReactRootViewHolder) {
            reactRootViewHolder = (ReactRootViewHolder) containerLayout;
            reactRootViewHolder.setVisibilityObserver(this);
        }

        if (!FragmentHelper.isHidden(this) || getShowsDialog()) {
            if (getAnimation() != TransitionAnimation.None) {
                postponeEnterTransition();
            }
            initReactRootView();
        }

        return view;
    }

    @Override
    protected boolean extendedLayoutIncludesToolbar() {
        int color = mStyle.getToolbarBackgroundColor();
        float alpha = mStyle.getToolbarAlpha();
        Garden garden = getGarden();
        return Color.alpha(color) < 255
            || alpha < 1.0
            || garden.toolbarHidden
            || garden.extendedLayoutIncludesTopBar;
    }

    @Override
    protected void onCustomStyle(@NonNull Style style) {
        super.onCustomStyle(style);
        if (shouldPassThroughTouches()) {
            style.setScrimAlpha(0);
        }
    }

    @Override
    public void inspectVisibility(int visibility) {
        if (visibility != View.VISIBLE) {
            return;
        }
        if (reactRootView == null) {
            initReactRootView();
            initReactTitleView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (reactRootView == null) {
            initReactRootView();
            initReactTitleView();
        }

        if (isViewReady()) {
            reactRootView.addOnGlobalLayoutListener();
            sendViewAppearEvent(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isViewReady()) {
            sendViewAppearEvent(false);
            reactRootView.removeOnGlobalLayoutListener();
        }
    }

    private boolean isViewReady() {
        if (reactRootView == null) {
            return false;
        }
        return firstRenderCompleted;
    }

    private boolean reactViewAppeared = false;

    private void sendViewAppearEvent(boolean appear) {
        if (!isReactModuleRegisterCompleted()) {
            return;
        }

        // 当从前台进入后台时，不会触发 disappear, 这和 iOS 保持一致
        ReactContext reactContext = getCurrentReactContext();
        boolean isResumed = reactContext != null && reactContext.getLifecycleState() == LifecycleState.RESUMED;
        if (!isResumed) {
            return;
        }

        if (reactViewAppeared == appear) {
            return;
        }
        reactViewAppeared = appear;

        Bundle bundle = new Bundle();
        bundle.putString(KEY_SCENE_ID, getSceneId());
        bundle.putString(KEY_ON, appear ? ON_COMPONENT_APPEAR : ON_COMPONENT_DISAPPEAR);
        HBDEventEmitter.sendEvent(EVENT_NAVIGATION, Arguments.fromBundle(bundle));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!FragmentHelper.isHidden(this)) {
            initReactTitleView();
        }
        getReactBridgeManager().addReactBridgeReloadListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reactRootViewHolder != null) {
            reactRootViewHolder.setVisibilityObserver(null);
        }
        unmountReactView();
    }

    @Override
    public void onReload() {
        unmountReactView();
    }

    private void unmountReactView() {
        getReactBridgeManager().removeReactBridgeReloadListener(this);

        ReactContext reactContext = getCurrentReactContext();
        if (reactContext == null || !reactContext.hasActiveCatalystInstance()) {
            return;
        }

        if (reactRootView != null) {
            reactRootView.unmountReactApplication();
            reactRootView = null;
        }

        if (reactTitleView != null) {
            reactTitleView.unmountReactApplication();
            reactTitleView = null;
        }
    }

    @Override
    protected boolean onBackPressed() {
        if (getShowsDialog() && getReactNativeHost().hasInstance()) {
            getReactInstanceManager().onBackPressed();
            return true;
        }
        return super.onBackPressed();
    }

    public void signalFirstRenderComplete() {
        if (firstRenderCompleted) {
            return;
        }
        firstRenderCompleted = true;
        startPostponedEnterTransition();

        if (isViewReady() && isResumed()) {
            sendViewAppearEvent(true);
            reactRootView.addOnGlobalLayoutListener();
        }
    }

    public boolean isFirstRenderCompleted() {
        return firstRenderCompleted;
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        Bundle result = new Bundle();
        result.putInt(KEY_REQUEST_CODE, requestCode);
        result.putInt(KEY_RESULT_CODE, resultCode);
        result.putBundle(KEY_RESULT_DATA, data);
        result.putString(KEY_SCENE_ID, getSceneId());
        result.putString(KEY_ON, ON_COMPONENT_RESULT);
        HBDEventEmitter.sendEvent(EVENT_NAVIGATION, Arguments.fromBundle(result));
    }

    @Override
    public void setAppProperties(@NonNull Bundle props) {
        super.setAppProperties(props);
        if (reactRootView == null) {
            return;
        }
        
        if (isReactModuleRegisterCompleted()) {
            this.reactRootView.setAppProperties(getProps());
        }
    }

    private void initReactRootView() {
        if (reactRootView != null) {
            return;
        }
        
        if (getContext() == null) {
            return;
        }
        
        if (!isReactModuleRegisterCompleted()) {
            return;
        }
        
        reactRootView = createReactRootView();
        reactRootView.startReactApplication(getReactInstanceManager(), getModuleName(), getProps());
    }

    @NonNull
    private HBDReactRootView createReactRootView() {
        HBDReactRootView reactRootView = new HBDReactRootView(getContext());
        reactRootView.setShouldConsumeTouchEvent(!shouldPassThroughTouches());
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        containerLayout.addView(reactRootView, layoutParams);
        return reactRootView;
    }

    private void initReactTitleView() {
        if (reactTitleView != null) {
            return;
        }
        
        if (getContext() == null) {
            return;
        }

        if (!isReactModuleRegisterCompleted()) {
            return;
        }
        
        if (getToolbar() == null) {
            return;
        }

        Bundle titleItem = getOptions().getBundle("titleItem");
        if (titleItem == null) {
            return;
        }

        String moduleName = titleItem.getString("moduleName");
        if (moduleName == null) {
            return;
        }

        String fitting = titleItem.getString("layoutFitting");
        boolean expanded = "expanded".equals(fitting);
        reactTitleView = createReactTitleView(expanded);
        reactTitleView.startReactApplication(getReactInstanceManager(), moduleName, getProps());
    }

    private HBDReactRootView createReactTitleView( boolean expanded) {
        Toolbar.LayoutParams layoutParams = createTitleLayoutParams(expanded);
        HBDReactRootView reactTitleView = new HBDReactRootView(getContext());
        Toolbar toolbar = getToolbar();
        toolbar.addView(reactTitleView, layoutParams);
        return reactTitleView;
    }

    @NonNull
    private Toolbar.LayoutParams createTitleLayoutParams(boolean expanded) {
        if (expanded) {
            return new Toolbar.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER);
        }
        return new Toolbar.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER);
    }
    
    @Override
    public void postponeEnterTransition() {
        super.postponeEnterTransition();
        if (getActivity() == null) {
            return;
        }
        getActivity().supportPostponeEnterTransition();
    }

    @Override
    public void startPostponedEnterTransition() {
        super.startPostponedEnterTransition();
        if (getActivity() == null) {
            return;
        }
        getActivity().supportStartPostponedEnterTransition();
    }
    
}
