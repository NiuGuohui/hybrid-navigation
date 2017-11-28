package com.navigationhybrid;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Arguments;

/**
 * Created by Listen on 2017/11/20.
 */

public class ReactNavigationFragment extends NavigationFragment {

    protected static final String TAG = "ReactNative";

    ReactBridgeManager bridgeManager = ReactBridgeManager.instance;
    ReactRootView reactRootView;
    ReactNavigationFragmentViewGroup containerLayout;
    Handler handler = new Handler();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, toString() + "#onCreateView");
        if (navigator.anim != PresentAnimation.None) {
            postponeEnterTransition();
        }
        View view = inflater.inflate(R.layout.fragment_react, container, false);
        containerLayout = view.findViewById(R.id.react_content);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (topBar != null) {
            setupTopBar();
        }
        initReactNative();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        reactRootView = null;
    }

    @Override
    public void onFragmentResult(int requestCode, int resultCode, Bundle data) {
        super.onFragmentResult(requestCode, resultCode, data);
        Bundle result = new Bundle();
        result.putInt(Navigator.REQUEST_CODE_KEY, requestCode);
        result.putInt(Navigator.RESULT_CODE_KEY, resultCode);
        result.putBundle(Navigator.RESULT_DATA_KEY, data);
        result.putString(PROPS_NAV_ID, navigator.navId);
        result.putString(PROPS_SCENE_ID, navigator.sceneId);
        bridgeManager.sendEvent(Navigator.ON_COMPONENT_RESULT_EVENT, Arguments.fromBundle(result));
    }

    private void initReactNative() {
        if (reactRootView != null || getView() == null) {
            return;
        }

        if (bridgeManager.isReactModuleInRegistry()) {
            bridgeManager.addReactModuleRegistryListener(new ReactBridgeManager.ReactModuleRegistryListener() {
                @Override
                public void onReactModuleRegistryCompleted() {
                    bridgeManager.removeReactModuleRegistryListener(this);
                    Log.w(TAG, ReactNavigationFragment.this.toString() + " onReactModuleRegistryCompleted");
                    initReactNative();
                }
            });
            return;
        }

        Log.d(TAG, toString() + " bridge is initialized");

        if (reactRootView == null && getView() != null) {
            reactRootView = new ReactRootView(getContext());
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            containerLayout.addView(reactRootView, layoutParams);
            containerLayout.setReactRootView(reactRootView);
            String moduleName = getArguments().getString(NAVIGATION_MODULE_NAME);
            Bundle initialProps = getArguments().getBundle(NAVIGATION_PROPS);
            reactRootView.startReactApplication(bridgeManager.getReactInstanceManager(), moduleName, initialProps);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startPostponedEnterTransition();
                }
            }, 2000);
        }
    }

    public void signalFirstRenderComplete() {
        Log.d(TAG, "signalFirstRenderComplete");
        startPostponedEnterTransition();
    }

    protected void setupTopBar() {
        Bundle options = getOptions();
        if (options == null) {
            options = new Bundle();
        }

        Bundle titleItem = options.getBundle("titleItem");
        garden.setTitleItem(titleItem);

        Bundle rightBarButtonItem = options.getBundle("rightBarButtonItem");
        garden.setRightBarButtonItem(rightBarButtonItem);

        Bundle leftBarButtonItem = options.getBundle("leftBarButtonItem");
        if (leftBarButtonItem != null) {
            garden.setLeftBarButtonItem(leftBarButtonItem);
        } else {
            if (!navigator.isRoot()) {
                Toolbar toolbar = topBar.getToolbar();
                // FIXME 根据不同主题选择不同颜色的返回按钮
                toolbar.setNavigationIcon(R.drawable.nav_ic_arrow_back_white);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        navigator.pop();
                    }
                });
            }
        }
    }

//    @Override
//    public void postponeEnterTransition() {
//        super.postponeEnterTransition();
//        Log.d(TAG, "postponeEnterTransition");
//        getActivity().supportPostponeEnterTransition();
//    }
//
//    @Override
//    public void startPostponedEnterTransition() {
//        super.startPostponedEnterTransition();
//        Log.d(TAG, "startPostponeEnterTransition");
//        if (getActivity() != null) {
//            getActivity().supportStartPostponedEnterTransition();
//        }
//    }


}
