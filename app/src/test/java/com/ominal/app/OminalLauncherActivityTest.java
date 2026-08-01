package com.ominal.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OminalLauncherActivityTest {

    @Test
    public void launchesChatInRetainedApplicationTask() {
        OminalLauncherActivity activity = Robolectric.buildActivity(OminalLauncherActivity.class)
            .create()
            .get();

        Intent launched = shadowOf(activity).getNextStartedActivity();
        assertEquals(OringutanActivity.class.getName(), launched.getComponent().getClassName());
        assertTrue((launched.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertTrue((launched.getFlags() & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0);
        assertTrue((launched.getFlags() & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0);
        assertTrue(activity.isFinishing());
    }
}
