package com.trianguloy.urlchecker.activities;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ScrollView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowMenu;

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

    @Test
    public void homeUpFinishesActivity() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        Menu menu = ShadowMenu.newInstance();
        MenuItem home = menu.add(0, android.R.id.home, 0, "up");
        assertTrue(AndroidUtils.onHomeUpPressed(activity, home));
    }
}
