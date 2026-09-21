package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.modules.companions.VersionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared rules for which GitHub releases count as Link Guard OTA. */
final class LinkGuardReleaseParser {

    static final String OTA_APK_NAME = "LinkGuard.apk";

    private static final Pattern TAG_SEMVER =
            Pattern.compile("^linkguard-v(\\d+(?:\\.\\d+)*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_VERSION_CODE =
            Pattern.compile("versionCode\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY_VERSION_NAME =
            Pattern.compile("versionName\\s*:\\s*([^\\s\\n<]+)", Pattern.CASE_INSENSITIVE);

    private LinkGuardReleaseParser() {
    }

    static boolean isOtaTag(String tag) {
        return tag != null && TAG_SEMVER.matcher(tag).matches();
    }

    static String versionNameFromTag(String tag) {
        Matcher m = TAG_SEMVER.matcher(tag);
        if (!m.matches()) return null;
        return m.group(1);
    }

    static int versionCodeFromBody(String body) {
        if (body == null) return -1;
        Matcher m = BODY_VERSION_CODE.matcher(body);
        if (!m.find()) return -1;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String versionNameFromBody(String body) {
        if (body == null) return null;
        Matcher m = BODY_VERSION_NAME.matcher(body);
        if (!m.find()) return null;
        return m.group(1).trim();
    }

    /** Requires {@link #OTA_APK_NAME} exactly — no other APK assets. */
    static JSONObject pickLinkGuardApk(JSONArray assets) throws org.json.JSONException {
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            if (OTA_APK_NAME.equalsIgnoreCase(asset.optString("name", ""))) {
                return asset;
            }
        }
        return null;
    }

    static UpdateRelease parseRelease(String tag, String body, JSONObject asset) {
        if (!isOtaTag(tag) || asset == null) return null;

        int versionCode = versionCodeFromBody(body);
        if (versionCode < 0) return null;

        String versionName = versionNameFromBody(body);
        if (versionName == null || versionName.isEmpty()) {
            versionName = versionNameFromTag(tag);
        }
        if (versionName == null || versionName.isEmpty()) return null;

        String url = asset.optString("browser_download_url", "");
        if (url.isEmpty()) return null;

        return new UpdateRelease(tag, versionCode, versionName, url, asset.optLong("size", 0));
    }

    static boolean isNewerThanInstalled(UpdateRelease release, int installedVersionCode, String installedVersionName) {
        if (release.versionCode > installedVersionCode) return true;
        if (release.versionCode < installedVersionCode) return false;
        if (release.versionName.equals(installedVersionName)) return false;
        return VersionManager.isVersionNewer(release.versionName);
    }

    static UpdateRelease pickBest(UpdateRelease best, UpdateRelease candidate) {
        if (candidate == null) return best;
        if (best == null) return candidate;
        if (candidate.versionCode > best.versionCode) return candidate;
        if (candidate.versionCode < best.versionCode) return best;
        if (VersionManager.isVersionNewer(candidate.versionName)) return candidate;
        return best;
    }

    static String apkDownloadUrlForTag(String tag) {
        return "https://github.com/" + LinkGuardUpdateConfig.GITHUB_OWNER + "/"
                + LinkGuardUpdateConfig.GITHUB_REPO + "/releases/download/" + tag + "/"
                + OTA_APK_NAME;
    }

    static UpdateRelease releaseFromTagAndBody(String tag, String body) {
        if (!isOtaTag(tag)) return null;
        int versionCode = versionCodeFromBody(body);
        if (versionCode < 0) return null;
        String versionName = versionNameFromBody(body);
        if (versionName == null || versionName.isEmpty()) {
            versionName = versionNameFromTag(tag);
        }
        if (versionName == null || versionName.isEmpty()) return null;
        return new UpdateRelease(tag, versionCode, versionName, apkDownloadUrlForTag(tag), 0);
    }

    /** Newest installable release newer than installed, with a reachable APK URL. */
    static UpdateRelease pickNewestEligible(
            List<UpdateRelease> candidates,
            int installedVersionCode,
            String installedVersionName) throws IOException {
        List<UpdateRelease> newer = new ArrayList<>();
        for (UpdateRelease c : candidates) {
            if (c != null && isNewerThanInstalled(c, installedVersionCode, installedVersionName)) {
                newer.add(c);
            }
        }
        if (newer.isEmpty()) return null;
        newer.sort(Comparator.comparingInt((UpdateRelease r) -> r.versionCode).reversed());
        for (UpdateRelease c : newer) {
            if (GitHubHttp.isApkDownloadAvailable(c.apkUrl)) return c;
        }
        return null;
    }
}
