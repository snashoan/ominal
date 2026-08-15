package com.ominal.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Keeps launcher aliases independent from the long-lived chat task.
 */
public final class OminalLauncherActivity extends Activity {
    private boolean mForwarded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        FrameLayout handoff = new FrameLayout(this);
        handoff.setBackgroundColor(Color.BLACK);
        handoff.setFocusableInTouchMode(true);
        handoff.requestFocus();
        setContentView(handoff);

        ImageView mark = new ImageView(this);
        mark.setImageResource(com.ominal.R.drawable.splash_mark);
        mark.setAlpha(0f);
        mark.setScaleX(0.84f);
        mark.setScaleY(0.84f);
        int markSize = Math.round(96f * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams markParams = new FrameLayout.LayoutParams(markSize, markSize);
        markParams.gravity = android.view.Gravity.CENTER;
        handoff.addView(mark, markParams);
        mark.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180L).start();

        handoff.post(() -> {
            InputMethodManager input = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (input != null) input.hideSoftInputFromWindow(handoff.getWindowToken(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && handoff.getWindowInsetsController() != null) {
                handoff.getWindowInsetsController().hide(WindowInsets.Type.ime());
            }
            handoff.postDelayed(this::openMainActivity, 200L);
        });
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
