package com.trianguloy.urlchecker.modules.companions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class IncognitoIntentHelperTest {

    private static Intent viewIntent(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"));
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        return intent;
    }

    @Test
    public void kindForPackageRecognizesBrowsers() {
        assertEquals(IncognitoIntentHelper.BrowserKind.FIREFOX,
                IncognitoIntentHelper.kindForPackage("org.mozilla.firefox"));
        assertEquals(IncognitoIntentHelper.BrowserKind.CHROMIUM,
                IncognitoIntentHelper.kindForPackage("com.android.chrome"));
        assertEquals(IncognitoIntentHelper.BrowserKind.CHROMIUM,
                IncognitoIntentHelper.kindForPackage("com.brave.browser"));
        assertEquals(IncognitoIntentHelper.BrowserKind.SAMSUNG,
                IncognitoIntentHelper.kindForPackage("com.sec.android.app.sbrowser"));
        assertEquals(IncognitoIntentHelper.BrowserKind.EDGE,
                IncognitoIntentHelper.kindForPackage("com.microsoft.emmx"));
    }

    @Test
    public void onlyFirefoxSupportsThirdPartyPrivateMode() {
        assertTrue(IncognitoIntentHelper.supportsThirdPartyPrivateMode("org.mozilla.firefox"));
        assertFalse(IncognitoIntentHelper.supportsThirdPartyPrivateMode("com.brave.browser"));
        assertFalse(IncognitoIntentHelper.supportsThirdPartyPrivateMode("com.android.chrome"));
        assertFalse(IncognitoIntentHelper.supportsThirdPartyPrivateMode("com.sec.android.app.sbrowser"));
        assertFalse(IncognitoIntentHelper.supportsThirdPartyPrivateMode("com.microsoft.emmx"));
    }

    @Test
    public void applyOffClearsExtras() {
        Intent intent = viewIntent("org.mozilla.firefox");
        intent.putExtra(IncognitoIntentHelper.FIREFOX_EXTRA, true);
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, false));
        assertFalse(IncognitoIntentHelper.isIncognitoRequested(intent));
    }

    @Test
    public void applyFirefoxExtraOnlyForFirefox() {
        Intent intent = viewIntent("org.mozilla.firefox");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.FIREFOX_EXTRA, false));
    }

    @Test
    public void applyBraveReturnsFalse() {
        assertFalse(IncognitoIntentHelper.applyIncognitoExtras(viewIntent("com.brave.browser"), true));
    }

    @Test
    public void applySamsungReturnsFalse() {
        assertFalse(IncognitoIntentHelper.applyIncognitoExtras(
                viewIntent("com.sec.android.app.sbrowser"), true));
    }

    @Test
    public void applyChromeReturnsFalse() {
        assertFalse(IncognitoIntentHelper.applyIncognitoExtras(viewIntent("com.android.chrome"), true));
    }

    @Test
    public void firefoxViewComponent() {
        ComponentName cn = IncognitoIntentHelper.firefoxViewComponent("org.mozilla.firefox");
        assertEquals("org.mozilla.firefox", cn.getPackageName());
        assertEquals(IncognitoIntentHelper.FIREFOX_INTENT_RECEIVER, cn.getClassName());
    }

    @Test
    public void firefoxViewComponentNullForBrave() {
        assertNull(IncognitoIntentHelper.firefoxViewComponent("com.brave.browser"));
    }

    @Test
    public void resolveTargetPackageFromComponent() {
        Intent intent = viewIntent(null);
        intent.setComponent(new ComponentName("com.sec.android.app.sbrowser", ".Main"));
        assertEquals("com.sec.android.app.sbrowser", IncognitoIntentHelper.resolveTargetPackage(intent));
    }
}
