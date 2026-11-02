package com.trianguloy.urlchecker.modules.companions;

import android.content.ComponentName;
import android.content.Intent;

/** Builds browser-specific incognito / private-browsing extras on VIEW intents. */
public final class IncognitoIntentHelper {

    public static final String FIREFOX_EXTRA = "private_browsing_mode";
    public static final String CHROME_EXTRA = "com.google.android.apps.chrome.EXTRA_OPEN_NEW_INCOGNITO_TAB";
    public static final String CTABS_INCOGNITO = "androidx.browser.customtabs.extra.OPEN_IN_INCOGNITO";
    public static final String CTABS_INCOGNITO_LEGACY = "android.support.customtabs.extra.OPEN_IN_INCOGNITO";
    public static final String CTABS_SESSION = "android.support.customtabs.extra.SESSION";

    public static final String SAMSUNG_SECRET_MODE = "com.sec.android.app.sbrowser.secret_mode";
    public static final String SAMSUNG_USE_SECRET = "com.sec.android.app.sbrowser.UseSecretWebBrowser";
    public static final String EDGE_INPRIVATE = "com.microsoft.emmx.extra.LaunchInPrivateMode";

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

    public static String resolveTargetPackage(Intent intent) {
        if (intent.getPackage() != null) return intent.getPackage();
        ComponentName component = intent.getComponent();
        if (component != null) return component.getPackageName();
        return null;
    }

    public static boolean usesCustomTabs(Intent intent) {
        return intent.hasExtra(CTABS_SESSION);
    }

    public static boolean isIncognitoRequested(Intent intent) {
        return intent.getBooleanExtra(FIREFOX_EXTRA, false)
                || intent.getBooleanExtra(CHROME_EXTRA, false)
                || intent.getBooleanExtra(CTABS_INCOGNITO, false)
                || intent.getBooleanExtra(CTABS_INCOGNITO_LEGACY, false)
                || intent.getBooleanExtra(SAMSUNG_SECRET_MODE, false)
                || intent.getBooleanExtra(SAMSUNG_USE_SECRET, false)
                || intent.getBooleanExtra(EDGE_INPRIVATE, false);
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
     * @return true if this browser (or Custom Tab path) has a known private-mode API
     */
    public static boolean applyIncognitoExtras(Intent intent, boolean incognitoOn) {
        clearIncognitoExtras(intent);
        if (!incognitoOn) return true;

        boolean customTabs = usesCustomTabs(intent);
        String pkg = resolveTargetPackage(intent);
        BrowserKind kind = kindForPackage(pkg);

        if (customTabs) {
            intent.putExtra(CTABS_INCOGNITO, true);
            intent.putExtra(CTABS_INCOGNITO_LEGACY, true);
        }

        switch (kind) {
            case FIREFOX -> intent.putExtra(FIREFOX_EXTRA, true);
            case CHROMIUM -> intent.putExtra(CHROME_EXTRA, true);
            case SAMSUNG -> {
                intent.putExtra(SAMSUNG_SECRET_MODE, true);
                intent.putExtra(SAMSUNG_USE_SECRET, true);
                intent.putExtra(CHROME_EXTRA, true);
            }
            case EDGE -> intent.putExtra(EDGE_INPRIVATE, true);
            case UNSUPPORTED -> {
                if (customTabs) return true;
                return false;
            }
        }
        return true;
    }
}
