package com.trianguloy.urlchecker.update;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/** Fetches Link Guard OTA releases from GitHub REST API. */
public final class GitHubReleaseClient {

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
            if (!LinkGuardReleaseParser.isOtaTag(tag)) continue;

            JSONObject asset = LinkGuardReleaseParser.pickLinkGuardApk(release.optJSONArray("assets"));
            UpdateRelease candidate = LinkGuardReleaseParser.parseRelease(
                    tag, release.optString("body", ""), asset);
            if (candidate == null) continue;
            if (!LinkGuardReleaseParser.isNewerThanInstalled(candidate, installedVersionCode, installedVersionName)) {
                continue;
            }
            best = LinkGuardReleaseParser.pickBest(best, candidate);
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
}
