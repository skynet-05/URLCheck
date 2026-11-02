package com.trianguloy.urlchecker.modules.companions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class BravePrivateTabHelperTest {

    @Test
    public void recognizesBravePackageVariants() {
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE));
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE_BETA));
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE_NIGHTLY));
        assertFalse(BravePrivateTabHelper.isBravePackage("com.android.chrome"));
    }

    @Test
    public void launchCandidatesIncludeIncognitoExtraWithoutUrl() {
        Intent[] intents = BravePrivateTabHelper.buildPrivateTabLaunchCandidates(
                org.robolectric.RuntimeEnvironment.getApplication(), BravePrivateTabHelper.PKG_BRAVE);
        assertTrue(intents.length >= 2);
        Intent aboutBlank = intents[1];
        assertEquals(Intent.ACTION_VIEW, aboutBlank.getAction());
        assertEquals(Uri.parse("about:blank"), aboutBlank.getData());
        assertEquals(BravePrivateTabHelper.PKG_BRAVE, aboutBlank.getPackage());
        assertTrue(aboutBlank.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }

    @Test
    public void launchCandidateFromMainHasIncognitoExtraAndNoData() {
        Intent[] intents = BravePrivateTabHelper.buildPrivateTabLaunchCandidates(
                org.robolectric.RuntimeEnvironment.getApplication(), BravePrivateTabHelper.PKG_BRAVE);
        Intent fromLaunch = intents[0];
        if (fromLaunch != null) {
            assertTrue(fromLaunch.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
            assertEquals(null, fromLaunch.getData());
        }
    }

    @Test
    public void launcherActivityCandidateUsesChromeLauncherActivity() {
        Intent[] intents = BravePrivateTabHelper.buildPrivateTabLaunchCandidates(
                org.robolectric.RuntimeEnvironment.getApplication(), BravePrivateTabHelper.PKG_BRAVE);
        Intent launcher = intents[2];
        assertNotNull(launcher);
        assertEquals(BravePrivateTabHelper.CHROME_LAUNCHER_ACTIVITY,
                launcher.getComponent().getClassName());
        assertTrue(launcher.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }
}
