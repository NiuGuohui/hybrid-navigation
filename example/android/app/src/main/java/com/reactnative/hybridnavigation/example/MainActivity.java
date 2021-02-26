package com.reactnative.hybridnavigation.example;

import android.os.Bundle;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.UiThreadUtil;
import com.navigation.androidx.AwesomeFragment;
import com.reactnative.hybridnavigation.ReactAppCompatActivity;

public class MainActivity extends ReactAppCompatActivity {

    private SplashFragment splashFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String tag = savedInstanceState.getString("splash_tag");
            if (tag != null) {
                splashFragment = (SplashFragment) getSupportFragmentManager().findFragmentByTag(tag);
            }
        }

        if (splashFragment == null) {
            if (!isReactModuleRegisterCompleted()) {
                splashFragment = new SplashFragment();
                showDialog(splashFragment, 0);
            }
        } else {
            if (isReactModuleRegisterCompleted()) {
                if (splashFragment != null) {
                    splashFragment.hideDialog();
                    splashFragment = null;
                }
            }
        }
    }

    @Override
    protected void setActivityRootFragmentSync(AwesomeFragment fragment, int tag) {
        super.setActivityRootFragmentSync(fragment, tag);
        if (splashFragment != null) {
            // 如果发现有白屏，请调整 delayInMs 参数
            UiThreadUtil.runOnUiThread(() -> {
                if (splashFragment != null) {
                    splashFragment.hideDialog();
                    splashFragment = null;
                }
            }, 200);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (splashFragment != null) {
            outState.putString("splash_tag", splashFragment.getSceneId());
        }
    }

    @Override
    protected boolean handleBackPressed() {
        // 按返回键并不真正退出 APP，而是把它移到后台
        moveTaskToBack(false);
        return true;
    }

}