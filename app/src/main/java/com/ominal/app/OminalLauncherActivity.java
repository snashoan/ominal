package com.ominal.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.core.splashscreen.SplashScreen;

/**
 * Keeps launcher aliases independent from the long-lived chat task.
 */
public final class OminalLauncherActivity extends Activity {
    private boolean mForwarded;
    private View mHandoff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> true);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        super.onCreate(savedInstanceState);
        FrameLayout handoff = new FrameLayout(this);
        mHandoff = handoff;
        handoff.setBackgroundColor(Color.BLACK);
        handoff.setFocusableInTouchMode(true);
        handoff.requestFocus();
        setContentView(handoff);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && handoff.getWindowInsetsController() != null) {
            handoff.getWindowInsetsController().hide(WindowInsets.Type.ime());
        }

        ImageView mark = new ImageView(this);
        mark.setImageResource(com.ominal.R.drawable.gir_final_logo_white);
        int markSize = Math.round(96f * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(markSize, markSize);
        markParams.gravity = android.view.Gravity.CENTER;
        handoff.addView(mark, markParams);

        handoff.post(() -> hideIme(handoff));
        handoff.postDelayed(this::openMainActivity, 350L);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || mForwarded || mHandoff == null) return;
        hideIme(mHandoff);
        mHandoff.postDelayed(this::openMainActivity, 80L);
    }

    private void hideIme(View target) {
        InputMethodManager input = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
        if (input != null) input.hideSoftInputFromWindow(target.getWindowToken(), 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && target.getWindowInsetsController() != null)
            target.getWindowInsetsController().hide(WindowInsets.Type.ime());
    }

    private void openMainActivity() {
        if (mForwarded || isFinishing()) return;
        mForwarded = true;
        Intent intent = new Intent(this, OringutanActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
