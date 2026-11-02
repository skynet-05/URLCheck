package com.trianguloy.urlchecker.modules.companions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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
        assertEquals(IncognitoIntentHelper.BrowserKind.UNSUPPORTED,
                IncognitoIntentHelper.kindForPackage("com.example.browser"));
    }

    @Test
    public void applyOffClearsExtras() {
        Intent intent = viewIntent("com.android.chrome");
        intent.putExtra(IncognitoIntentHelper.CHROME_EXTRA, true);
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, false));
        assertFalse(IncognitoIntentHelper.isIncognitoRequested(intent));
    }

    @Test
    public void applyFirefoxExtra() {
        Intent intent = viewIntent("org.mozilla.firefox");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.FIREFOX_EXTRA, false));
        assertFalse(intent.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }

    @Test
    public void applyChromeExtra() {
        Intent intent = viewIntent("com.android.chrome");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }

    @Test
    public void applySamsungExtras() {
        Intent intent = viewIntent("com.sec.android.app.sbrowser");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.SAMSUNG_SECRET_MODE, false));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.SAMSUNG_USE_SECRET, false));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }

    @Test
    public void applyEdgeExtra() {
        Intent intent = viewIntent("com.microsoft.emmx");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.EDGE_INPRIVATE, false));
    }

    @Test
    public void applyBraveUsesChromeIncognitoExtra() {
        Intent intent = viewIntent("com.brave.browser");
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.CHROME_EXTRA, false));
    }

    @Test
    public void applyCustomTabsIncognitoEvenWithoutKnownPackage() {
        Intent intent = viewIntent(null);
        intent.setComponent(new ComponentName("com.unknown.app", ".Browser"));
        Bundle extras = new Bundle();
        extras.putBinder(IncognitoIntentHelper.CTABS_SESSION, null);
        intent.putExtras(extras);
        assertTrue(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.CTABS_INCOGNITO, false));
        assertTrue(intent.getBooleanExtra(IncognitoIntentHelper.CTABS_INCOGNITO_LEGACY, false));
    }

    @Test
    public void applyUnsupportedBrowserReturnsFalse() {
        Intent intent = viewIntent("com.opera.browser");
        assertFalse(IncognitoIntentHelper.applyIncognitoExtras(intent, true));
    }

    @Test
    public void resolveTargetPackageFromComponent() {
        Intent intent = viewIntent(null);
        intent.setComponent(new ComponentName("com.sec.android.app.sbrowser", ".Main"));
        assertEquals("com.sec.android.app.sbrowser", IncognitoIntentHelper.resolveTargetPackage(intent));
    }
}
