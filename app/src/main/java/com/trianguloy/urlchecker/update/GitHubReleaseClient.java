package com.trianguloy.urlchecker.update;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers Link Guard OTA updates. Uses the public releases Atom feed first (no API quota);
 * REST API is optional fallback when the feed fails.
 */
public final class GitHubReleaseClient {

    private GitHubReleaseClient() {
    }

    public static UpdateRelease findLatestUpdate(
            Context context,
            int installedVersionCode,
            String installedVersionName) throws Exception {
        if (OtaCheckCache.hasValid(context, installedVersionCode)) {
            return OtaCheckCache.get(context, installedVersionCode);
        }

        Exception atomError = null;
        try {
            UpdateRelease fromAtom = GitHubReleasesAtomClient.findLatestUpdate(installedVersionCode, installedVersionName);
            OtaCheckCache.put(context, installedVersionCode, fromAtom);
            return fromAtom;
        } catch (Exception e) {
            atomError = e;
        }

        try {
            UpdateRelease fromApi = findLatestFromApi(installedVersionCode, installedVersionName);
            OtaCheckCache.put(context, installedVersionCode, fromApi);
            return fromApi;
        } catch (UpdateCheckException apiError) {
            if (GitHubHttp.isRateLimitMessage(apiError.getMessage()) && atomError != null) {
                throw preferAtomFailure(atomError, apiError);
            }
            throw apiError;
        }
    }

    private static UpdateCheckException preferAtomFailure(Exception atomError, UpdateCheckException apiError) {
        if (atomError instanceof UpdateCheckException) return (UpdateCheckException) atomError;
        return new UpdateCheckException(atomError.getMessage() != null
                ? atomError.getMessage()
                : apiError.getMessage());
    }

    private static UpdateRelease findLatestFromApi(int installedVersionCode, String installedVersionName) throws Exception {
        JSONArray releases = fetchAllReleases();
        List<UpdateRelease> candidates = new ArrayList<>();
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.getJSONObject(i);
            if (release.optBoolean("draft", false)) continue;

            String tag = release.optString("tag_name", "");
            if (!LinkGuardReleaseParser.isOtaTag(tag)) continue;

            JSONObject asset = LinkGuardReleaseParser.pickLinkGuardApk(release.optJSONArray("assets"));
            UpdateRelease candidate = LinkGuardReleaseParser.parseRelease(
                    tag, release.optString("body", ""), asset);
            if (candidate != null) candidates.add(candidate);
        }
        return LinkGuardReleaseParser.pickNewestEligible(candidates, installedVersionCode, installedVersionName);
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
