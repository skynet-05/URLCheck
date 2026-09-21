package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.BuildConfig;

/**
 * OTA update convention for Link Guard (white-label builds).
 *
 * <p>Only GitHub releases with tag {@code linkguard-vX.Y.Z} and asset {@code LinkGuard.apk}
 * are considered. Other tags on the fork (whitelabel, tor-sandbox, etc.) are ignored.
 *
 * <p>Release body must include {@code versionCode: N}. Optional {@code versionName: X.Y.Z}.
 */
public final class LinkGuardUpdateConfig {

    public static final String GITHUB_OWNER = "skynet-05";
    public static final String GITHUB_REPO = "URLCheck";
    public static final String TAG_PREFIX = "linkguard-v";
    public static final String RELEASES_API =
            "https://api.github.com/repos/" + GITHUB_OWNER + "/" + GITHUB_REPO + "/releases?per_page=30";

    public static boolean isEnabled() {
        return BuildConfig.WHITE_LABEL;
    }

    private LinkGuardUpdateConfig() {
    }
}
