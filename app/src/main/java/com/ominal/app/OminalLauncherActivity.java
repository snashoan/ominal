package com.ominal.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Keeps launcher aliases independent from the long-lived chat task.
 */
public final class OminalLauncherActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, OringutanActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
