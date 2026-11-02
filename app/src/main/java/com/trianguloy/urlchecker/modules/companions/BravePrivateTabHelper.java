package com.trianguloy.urlchecker.modules.companions;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;

/** Copy link + best-effort Brave private tab (Chromium blocks URL load from third parties). */
public final class BravePrivateTabHelper {

    public static final String PKG_BRAVE = "com.brave.browser";
    public static final String PKG_BRAVE_BETA = "com.brave.browser_beta";
    public static final String PKG_BRAVE_NIGHTLY = "com.brave.browser_nightly";

    private static final String[] BRAVE_PACKAGES = {
            PKG_BRAVE, PKG_BRAVE_BETA, PKG_BRAVE_NIGHTLY
    };

    /** Chromium launcher entry used by Brave (inherits Chrome document model). */
    static final String CHROME_LAUNCHER_ACTIVITY =
            "org.chromium.chrome.browser.document.ChromeLauncherActivity";

    private BravePrivateTabHelper() {
    }

    public static boolean isBravePackage(@Nullable String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        for (String pkg : BRAVE_PACKAGES) {
            if (pkg.equals(packageName)) return true;
        }
        return false;
    }

    @Nullable
    public static String resolveInstalledBravePackage(Context context) {
        for (String pkg : BRAVE_PACKAGES) {
            if (PrivateLinkBrowser.isInstalled(context, pkg)) return pkg;
        }
        return null;
    }

    public static void copyUrlSensitive(Context context, String url) {
        ClipData clip = ClipData.newPlainText("", url);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PersistableBundle extras = new PersistableBundle();
            extras.putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true);
            clip.getDescription().setExtras(extras);
        }
        var clipboard = context.getSystemService(android.content.ClipboardManager.class);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Best-effort launch of a new private tab without loading the URL from Link Guard.
     * Chromium {@code isAllowedIncognitoIntent} usually rejects third-party callers; we still try
     * documented-shaped intents and return whether an activity was started.
     */
    public static boolean tryLaunchNewPrivateTab(Context context, String bravePackage) {
        for (Intent candidate : buildPrivateTabLaunchCandidates(context, bravePackage)) {
            if (candidate == null) continue;
            if (candidate.resolveActivity(context.getPackageManager()) == null) continue;
            context.startActivity(candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        return false;
    }

    static Intent[] buildPrivateTabLaunchCandidates(Context context, String bravePackage) {
        var pm = context.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(bravePackage);
        Intent fromLaunch = null;
        if (launch != null) {
            fromLaunch = new Intent(launch);
            fromLaunch.setData(null);
            fromLaunch.removeCategory(Intent.CATEGORY_LAUNCHER);
            fromLaunch.putExtra(IncognitoIntentHelper.CHROME_EXTRA, true);
        }
        Intent aboutBlank = new Intent(Intent.ACTION_VIEW, Uri.parse("about:blank"));
        aboutBlank.setPackage(bravePackage);
        aboutBlank.putExtra(IncognitoIntentHelper.CHROME_EXTRA, true);

        Intent launcherActivity = new Intent(Intent.ACTION_MAIN);
        launcherActivity.setClassName(bravePackage, CHROME_LAUNCHER_ACTIVITY);
        launcherActivity.putExtra(IncognitoIntentHelper.CHROME_EXTRA, true);

        return new Intent[] {fromLaunch, aboutBlank, launcherActivity};
    }
}
