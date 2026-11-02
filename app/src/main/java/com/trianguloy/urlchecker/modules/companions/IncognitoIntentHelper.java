package com.trianguloy.urlchecker.modules.companions;

import android.content.ComponentName;
import android.content.Intent;

/** Builds browser-specific incognito / private-browsing extras on VIEW intents. */
public final class IncognitoIntentHelper {

    public static final String FIREFOX_EXTRA = "private_browsing_mode";
    /** Chromium: honored only from Chrome / trusted first-party callers, not Link Guard. */
    public static final String CHROME_EXTRA = "com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB";
    public static final String CTABS_INCOGNITO = "androidx.browser.customtabs.extra.OPEN_IN_INCOGNITO";
    public static final String CTABS_INCOGNITO_LEGACY = "android.support.customtabs.extra.OPEN_IN_INCOGNITO";
    public static final String CTABS_SESSION = "android.support.customtabs.extra.SESSION";

    /** Legacy / undocumented Samsung extras — not a reliable third-party API (see 4.0.8 field testing). */
    public static final String SAMSUNG_SECRET_MODE = "com.sec.android.app.sbrowser.secret_mode";
    public static final String SAMSUNG_USE_SECRET = "com.sec.android.app.sbrowser.UseSecretWebBrowser";
    /** Undocumented; not listed in Microsoft Edge mobile developer docs. */
    public static final String EDGE_INPRIVATE = "com.microsoft.emmx.extra.LaunchInPrivateMode";

    public static final String FIREFOX_INTENT_RECEIVER =
            "org.mozilla.fenix.IntentReceiverActivity";

    public enum BrowserKind {
        FIREFOX,
        CHROMIUM,
        SAMSUNG,
        EDGE,
        UNSUPPORTED
    }

    private IncognitoIntentHelper() {
    }

    public static BrowserKind kindForPackage(String packageName) {
        if (packageName == null || packageName.isEmpty()) return BrowserKind.UNSUPPORTED;
        if (packageName.startsWith("org.mozilla.")) return BrowserKind.FIREFOX;
        if ("com.sec.android.app.sbrowser".equals(packageName)) return BrowserKind.SAMSUNG;
        if ("com.microsoft.emmx".equals(packageName)) return BrowserKind.EDGE;
        if ("com.android.chrome".equals(packageName)
                || "com.google.android.apps.chrome".equals(packageName)
                || "com.brave.browser".equals(packageName)
                || "com.chrome.beta".equals(packageName)
                || "com.chrome.dev".equals(packageName)
                || "com.chrome.canary".equals(packageName)) {
            return BrowserKind.CHROMIUM;
        }
        return BrowserKind.UNSUPPORTED;
    }

    /**
     * Only Firefox documents and implements {@link #FIREFOX_EXTRA} for third-party {@code ACTION_VIEW}
     * intents (Fenix {@code IntentReceiverActivity}).
     */
    public static boolean supportsThirdPartyPrivateMode(String packageName) {
        return kindForPackage(packageName) == BrowserKind.FIREFOX;
    }

    public static ComponentName firefoxViewComponent(String packageName) {
        if (!supportsThirdPartyPrivateMode(packageName)) return null;
        return new ComponentName(packageName, FIREFOX_INTENT_RECEIVER);
    }

    public static String resolveTargetPackage(Intent intent) {
        if (intent.getPackage() != null) return intent.getPackage();
        ComponentName component = intent.getComponent();
        if (component != null) return component.getPackageName();
        return null;
    }

    public static boolean usesCustomTabs(Intent intent) {
        return intent.hasExtra(CTABS_SESSION);
    }

    public static void stripCustomTabsSession(Intent intent) {
        intent.removeExtra(CTABS_SESSION);
    }

    public static boolean isIncognitoRequested(Intent intent) {
        return intent.getBooleanExtra(FIREFOX_EXTRA, false);
    }

    public static void clearIncognitoExtras(Intent intent) {
        intent.removeExtra(FIREFOX_EXTRA);
        intent.removeExtra(CHROME_EXTRA);
        intent.removeExtra(CTABS_INCOGNITO);
        intent.removeExtra(CTABS_INCOGNITO_LEGACY);
        intent.removeExtra(SAMSUNG_SECRET_MODE);
        intent.removeExtra(SAMSUNG_USE_SECRET);
        intent.removeExtra(EDGE_INPRIVATE);
    }

    /**
     * @return true if incognito is off, or Firefox (or Custom Tab + Firefox) private mode was applied
     */
    public static boolean applyIncognitoExtras(Intent intent, boolean incognitoOn) {
        clearIncognitoExtras(intent);
        if (!incognitoOn) return true;

        String pkg = resolveTargetPackage(intent);
        if (!supportsThirdPartyPrivateMode(pkg)) {
            return false;
        }

        intent.putExtra(FIREFOX_EXTRA, true);
        return true;
    }
}
