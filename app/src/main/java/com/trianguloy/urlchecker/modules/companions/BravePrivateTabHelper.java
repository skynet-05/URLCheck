package com.trianguloy.urlchecker.modules.companions;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Copy link + launch Brave blank private tab via Chromium {@code IncognitoTabLauncher}.
 * See <a href="https://github.com/chromium/chromium/blob/main/chrome/android/java/src/org/chromium/chrome/browser/incognito/IncognitoTabLauncher.java">IncognitoTabLauncher</a>.
 */
public final class BravePrivateTabHelper {

    public static final String PKG_BRAVE = "com.brave.browser";
    public static final String PKG_BRAVE_BETA = "com.brave.browser_beta";
    public static final String PKG_BRAVE_NIGHTLY = "com.brave.browser_nightly";

    /**
     * Intent action handled by Chromium's exported {@code IncognitoTabLauncher} activity.
     */
    public static final String ACTION_OPEN_PRIVATE_TAB =
            "org.chromium.chrome.browser.incognito.OPEN_PRIVATE_TAB";

    public static final String INCOGNITO_TAB_LAUNCHER =
            "org.chromium.chrome.browser.incognito.IncognitoTabLauncher";

    private static final String[] BRAVE_PACKAGES = {
            PKG_BRAVE, PKG_BRAVE_BETA, PKG_BRAVE_NIGHTLY
    };

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
     * Starts Brave's {@code IncognitoTabLauncher} with no URL (blank focused omnibox).
     *
     * @return true if startActivity succeeded
     */
    public static boolean tryLaunchNewPrivateTab(Context context, String bravePackage) {
        Intent intent = buildOpenPrivateTabIntent(bravePackage);
        if (!isIncognitoTabLauncherResolvable(context, bravePackage)) {
            return false;
        }
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    @VisibleForTesting
    static Intent buildOpenPrivateTabIntent(String bravePackage) {
        Intent intent = new Intent(ACTION_OPEN_PRIVATE_TAB);
        intent.setComponent(new ComponentName(bravePackage, INCOGNITO_TAB_LAUNCHER));
        return intent;
    }

    @VisibleForTesting
    static boolean isIncognitoTabLauncherResolvable(Context context, String bravePackage) {
        Intent intent = buildOpenPrivateTabIntent(bravePackage);
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

    /** Component enabled and exported in the installed Brave build (not disabled by policy). */
    public static boolean isIncognitoTabLauncherEnabled(Context context, String bravePackage) {
        try {
            var info = context.getPackageManager().getActivityInfo(
                    new ComponentName(bravePackage, INCOGNITO_TAB_LAUNCHER),
                    PackageManager.GET_META_DATA);
            return info.enabled && info.exported;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
