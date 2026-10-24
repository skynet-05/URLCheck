package com.trianguloy.urlchecker.activities;

import static org.junit.Assert.assertTrue;

import android.widget.ScrollView;

import com.trianguloy.urlchecker.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LinkGuardSettingsActivityTest {

    @Test
    public void rootIsScrollViewWithSystemWindows() {
        LinkGuardSettingsActivity activity = Robolectric.buildActivity(LinkGuardSettingsActivity.class)
                .setup()
                .get();
        ScrollView scroll = activity.findViewById(R.id.link_guard_settings_scroll);
        assertTrue(scroll != null);
        assertTrue(scroll.getFitsSystemWindows());
        assertTrue(activity.findViewById(R.id.link_guard_settings_content) != null);
    }
}
