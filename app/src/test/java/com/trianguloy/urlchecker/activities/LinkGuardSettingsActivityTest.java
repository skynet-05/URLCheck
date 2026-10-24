package com.trianguloy.urlchecker.activities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.ScrollView;

import com.trianguloy.urlchecker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LinkGuardSettingsActivityTest {

    @Test
    public void opensAtTopWithFirstTorSettingFullyVisible() {
        ActivityController<LinkGuardSettingsActivity> controller =
                Robolectric.buildActivity(LinkGuardSettingsActivity.class).setup();
        LinkGuardSettingsActivity activity = controller.get();
        controller.visible();

        ScrollView scroll = activity.findViewById(R.id.link_guard_settings_scroll);
        View first = activity.findViewById(R.id.tor_pref_js);
        assertTrue(scroll != null);
        assertTrue(first != null);
        assertFalse(scroll.getFitsSystemWindows());
        assertEquals(0, scroll.getScrollY());

        int[] firstLoc = new int[2];
        int[] scrollLoc = new int[2];
        first.getLocationInWindow(firstLoc);
        scroll.getLocationInWindow(scrollLoc);
        int topInScroll = firstLoc[1] - scrollLoc[1] - scroll.getPaddingTop();
        assertTrue("Tor JS switch should sit below scroll top padding, got " + topInScroll, topInScroll >= 0);
    }
}
