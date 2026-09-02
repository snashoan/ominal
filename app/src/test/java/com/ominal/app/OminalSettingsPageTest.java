package com.ominal.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ominal.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class OminalSettingsPageTest {
    @Test
    public void exposesBackNavigationAndPageRows() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        OminalInteractionSheet.Theme theme = new OminalInteractionSheet.Theme(
            Color.BLACK, Color.DKGRAY, Color.WHITE, Color.GRAY,
            Color.DKGRAY, Color.CYAN, Color.BLACK);
        OminalInteractionSheet.Row runtime = new OminalInteractionSheet.Row(
            "agent", "Runtime", "Choose for this conversation", "Codex",
            false, true, false, R.drawable.ic_lucide_bot);
        AtomicBoolean backed = new AtomicBoolean();
        AtomicReference<String> selected = new AtomicReference<>();

        View page = OminalSettingsPage.create(activity, theme,
            Collections.singletonList(new OminalInteractionSheet.Section(
                "Agent", Collections.singletonList(runtime))),
            () -> backed.set(true), selected::set);

        assertEquals("settings-page", page.getTag());
        View runtimeRow = page.findViewWithTag("settings-row:agent");
        View back = page.findViewWithTag("settings-back");
        assertNotNull(runtimeRow);
        assertNotNull(back);

        runtimeRow.performClick();
        back.performClick();
        assertEquals("agent", selected.get());
        assertTrue(backed.get());
    }

    @Test
    public void rendersSourceIconAndDrillInChevron() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        OminalInteractionSheet.Theme theme = new OminalInteractionSheet.Theme(
            Color.BLACK, Color.DKGRAY, Color.WHITE, Color.GRAY,
            Color.DKGRAY, Color.CYAN, Color.BLACK);
        OminalInteractionSheet.Row account = new OminalInteractionSheet.Row(
            "account", "Account", "Codex sign-in and session", "Manage",
            false, true, false, R.drawable.ic_lucide_circle_user);
        View page = OminalSettingsPage.create(activity, theme,
            Collections.singletonList(new OminalInteractionSheet.Section(
                "Agent", Collections.singletonList(account))), () -> { }, ignored -> { });

        LinearLayout row = (LinearLayout) page.findViewWithTag("settings-row:account");
        assertNotNull(row);
        assertTrue(row.getChildAt(0) instanceof ImageView);
        assertTrue(row.getChildAt(row.getChildCount() - 1) instanceof ImageView);
    }

    @Test
    public void formKeepsSaveAndClearActionsOutsideScrollableContent() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        OminalInteractionSheet.Theme theme = new OminalInteractionSheet.Theme(
            Color.BLACK, Color.DKGRAY, Color.WHITE, Color.GRAY,
            Color.DKGRAY, Color.CYAN, Color.BLACK);
        AtomicBoolean saved = new AtomicBoolean();
        AtomicBoolean cleared = new AtomicBoolean();

        View page = OminalSettingsPage.createForm(activity, theme, "Profile",
            "Shared context", new TextView(activity), () -> { },
            () -> saved.set(true), () -> cleared.set(true));

        assertEquals("settings-form-page", page.getTag());
        LinearLayout root = (LinearLayout) page;
        LinearLayout actions = (LinearLayout) root.getChildAt(root.getChildCount() - 1);
        actions.getChildAt(0).performClick();
        actions.getChildAt(1).performClick();
        assertTrue(cleared.get());
        assertTrue(saved.get());
    }
}
