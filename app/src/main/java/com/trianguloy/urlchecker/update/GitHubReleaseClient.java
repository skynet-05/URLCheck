package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.modules.companions.VersionManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fetches and parses GitHub releases (REST API, Atom feed fallback). */
public final class GitHubReleaseClient {

    private static final Pattern VERSION_CODE = Pattern.compile("versionCode\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION_NAME = Pattern.compile("versionName\\s*:\\s*([^\\s\\n]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG_CODE = Pattern.compile("(?i)^linkguard-v(\\d+)$");

    private GitHubReleaseClient() {
    }

    public static UpdateRelease findLatestUpdate(int installedVersionCode, String installedVersionName) throws Exception {
        try {
            return findLatestFromApi(installedVersionCode, installedVersionName);
        } catch (UpdateCheckException apiError) {
            try {
                UpdateRelease atom = GitHubReleasesAtomClient.findLatestUpdate(installedVersionCode, installedVersionName);
                if (atom != null) return atom;
            } catch (Exception ignored) {
            }
            throw apiError;
        }
    }

    private static UpdateRelease findLatestFromApi(int installedVersionCode, String installedVersionName) throws Exception {
        JSONArray releases = fetchAllReleases();
        UpdateRelease best = null;
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.optBoolean("draft", false)) continue;

            String tag = release.optString("tag_name", "");
            String body = release.optString("body", "");
            JSONObject asset = pickApkAsset(release.optJSONArray("assets"), tag);
            if (asset == null) continue;

            int remoteCode = parseVersionCode(body, tag);
            String remoteName = parseVersionName(body, tag);
            if (!isNewer(remoteCode, remoteName, installedVersionCode, installedVersionName)) continue;

            String url = asset.optString("browser_download_url", "");
            if (url.isEmpty()) continue;

            UpdateRelease candidate = new UpdateRelease(
                    tag,
                    remoteCode,
                    remoteName,
                    url,
                    asset.optLong("size", 0)
            );
            if (best == null || candidate.versionCode > best.versionCode
                    || (candidate.versionCode == best.versionCode
                    && VersionManager.isVersionNewer(candidate.versionName))) {
                best = candidate;
            }
        }
        return best;
    }

    private static JSONArray fetchAllReleases() throws Exception {
        JSONArray all = new JSONArray();
        String url = LinkGuardUpdateConfig.RELEASES_API;
        for (int page = 0; page < 3 && url != null; page++) {
            HttpURLConnection conn = GitHubHttp.openGet(url);
            int code = conn.getResponseCode();
            String body = GitHubHttp.readBody(conn);
            if (code != 200) {
                conn.disconnect();
                throw GitHubHttp.httpFailure(code, body, "GitHub API");
            }
            JSONArray pageData = new JSONArray(body);
            for (int i = 0; i < pageData.length(); i++) {
                all.put(pageData.getJSONObject(i));
            }
            url = nextPageUrl(conn);
            conn.disconnect();
            if (pageData.length() == 0) break;
        }
        return all;
    }

    private static String nextPageUrl(HttpURLConnection conn) {
        String link = conn.getHeaderField("Link");
        if (link == null) return null;
        for (String part : link.split(",")) {
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<');
                int end = part.indexOf('>');
                if (start >= 0 && end > start) {
                    return part.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    private static JSONObject pickApkAsset(JSONArray assets, String tag) throws org.json.JSONException {
        if (assets == null) return null;
        JSONObject fallback = null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
            if (!name.endsWith(".apk")) continue;
            if (name.contains("linkguard")) return asset;
            if (fallback == null && qualifiesByTag(tag)) fallback = asset;
        }
        return fallback;
    }

    private static boolean qualifiesByTag(String tag) {
        if (tag == null) return false;
        String lower = tag.toLowerCase(Locale.ROOT);
        return lower.startsWith(LinkGuardUpdateConfig.TAG_PREFIX)
                || lower.startsWith("whitelabel-")
                || lower.startsWith("linkguard-");
    }

    private static int parseVersionCode(String body, String tag) {
        Matcher m = VERSION_CODE.matcher(body);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        if (tag != null) {
            Matcher tagCode = TAG_CODE.matcher(tag);
            if (tagCode.matches()) {
                return Integer.parseInt(tagCode.group(1));
            }
        }
        return -1;
    }

    private static String parseVersionName(String body, String tag) {
        Matcher m = VERSION_NAME.matcher(body);
        if (m.find()) return m.group(1).trim();
        if (tag != null && tag.toLowerCase(Locale.ROOT).startsWith(LinkGuardUpdateConfig.TAG_PREFIX)) {
            return tag.substring(LinkGuardUpdateConfig.TAG_PREFIX.length());
        }
        return tag != null ? tag : "";
    }

    private static boolean isNewer(int remoteCode, String remoteName, int localCode, String localName) {
        if (remoteCode > 0) {
            if (remoteCode > localCode) return true;
            if (remoteCode < localCode) return false;
        }
        if (remoteName == null || remoteName.isEmpty()) return false;
        if (remoteName.equals(localName)) return false;
        return VersionManager.isVersionNewer(remoteName);
    }
}
