package com.trianguloy.urlchecker.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.view.View;
import android.widget.ScrollView;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class SettingsActivityTest {

    @Test
    public void linkGuardSettingsEntryVisibleAndLaunchesActivity() {
        if (!BuildConfig.WHITE_LABEL) return;

        ActivityController<SettingsActivity> controller =
                Robolectric.buildActivity(SettingsActivity.class).setup();
        SettingsActivity activity = controller.get();
        controller.visible();

        View linkGuard = activity.findViewById(R.id.btn_link_guard_settings);
        ScrollView scroll = activity.findViewById(R.id.settings_scroll);
        assertNotNull(linkGuard);
        assertNotNull(scroll);
        assertEquals(View.VISIBLE, linkGuard.getVisibility());

        AndroidUtils.scrollToShowChild(scroll, linkGuard);
        Robolectric.getForegroundThreadScheduler().runToEndOfTasks();

        assertTrue(linkGuard.getBottom() <= scroll.getScrollY() + scroll.getHeight());

        linkGuard.performClick();
        Intent next = ShadowApplication.getInstance().getNextStartedActivity();
        assertNotNull(next);
        assertEquals(LinkGuardSettingsActivity.class.getName(), next.getComponent().getClassName());
    }
}
