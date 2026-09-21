package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.BuildConfig;

/**
 * OTA update convention for Link Guard (white-label builds).
 *
 * <p>GitHub releases on {@link #GITHUB_OWNER}/{@link #GITHUB_REPO} are scanned. A release qualifies if:
 * <ul>
 *   <li>Tag starts with {@link #TAG_PREFIX} (e.g. {@code linkguard-v3.5.1}), or</li>
 *   <li>Tag starts with {@code whitelabel-}, or</li>
 *   <li>Any attached APK asset name contains {@code LinkGuard} (case-insensitive).</li>
 * </ul>
 *
 * <p>Preferred APK asset names: {@code LinkGuard.apk}, {@code LinkGuard-tor-debug.apk}.
 *
 * <p>Release body should include {@code versionCode: 48} (required for reliable OTA). Optional:
 * {@code versionName: 3.5.1}. If versionCode is missing, versionName from the tag is compared.
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
