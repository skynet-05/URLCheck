package com.trianguloy.urlchecker.update;

import com.trianguloy.urlchecker.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** GitHub REST/feed requests with headers required to avoid HTTP 403. */
final class GitHubHttp {

    static final String API_VERSION = "2022-11-28";

    private GitHubHttp() {
    }

    static String userAgent() {
        return "LinkGuard/" + BuildConfig.VERSION_CODE + " (+https://github.com/skynet-05/URLCheck)";
    }

    static HttpURLConnection openGet(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        applyApiHeaders(conn);
        return conn;
    }

    static void applyApiHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", userAgent());
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", API_VERSION);
    }

    static void applyFeedHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", userAgent());
        conn.setRequestProperty("Accept", "application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8");
    }

    static void applyDownloadHeaders(HttpURLConnection conn) {
        conn.setRequestProperty("User-Agent", userAgent());
        conn.setRequestProperty("Accept", "application/vnd.android.package-archive, */*;q=0.8");
    }

    static boolean isRateLimitResponse(int code, String body) {
        if (code != 403) return false;
        String lower = body == null ? "" : body.toLowerCase();
        return lower.contains("rate limit") || lower.contains("api rate limit");
    }

    static boolean isRateLimitMessage(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("rate limit");
    }

    /** True if the release asset URL responds OK (HEAD, or short GET if HEAD is rejected). */
    static boolean isApkDownloadAvailable(String url) throws IOException {
        if (url == null || url.isEmpty()) return false;
        int code = probeUrl(url, "HEAD");
        if (code == HttpURLConnection.HTTP_OK) return true;
        if (code == HttpURLConnection.HTTP_BAD_METHOD || code == 403 || code == 405) {
            code = probeUrl(url, "GET");
        }
        return code == HttpURLConnection.HTTP_OK;
    }

    private static int probeUrl(String urlString, String method) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(15_000);
        conn.setInstanceFollowRedirects(true);
        applyDownloadHeaders(conn);
        if ("GET".equals(method)) {
            conn.setRequestProperty("Range", "bytes=0-0");
        }
        int code = conn.getResponseCode();
        conn.disconnect();
        return code;
    }

    static String readBody(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (stream == null) return "";
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            return sb.toString();
        }
    }

    static UpdateCheckException httpFailure(int code, String body, String context) {
        String friendly = friendlyMessage(code, body);
        String msg = context + " HTTP " + code;
        if (!friendly.isEmpty()) msg += ": " + friendly;
        return new UpdateCheckException(msg);
    }

    private static String friendlyMessage(int code, String body) {
        String lower = body.toLowerCase();
        if (code == 403 && (lower.contains("rate limit") || lower.contains("api rate limit"))) {
            return "GitHub API rate limit reached. Try again in about an hour.";
        }
        if (code == 403 && lower.contains("user-agent")) {
            return "GitHub requires a valid User-Agent (fixed in newer Link Guard builds).";
        }
        if (code == 403) {
            return "GitHub denied the request. VPN, DNS, or network filter may block api.github.com.";
        }
        if (code == 404) {
            return "Releases not found.";
        }
        try {
            if (body.trim().startsWith("{")) {
                JSONObject o = new JSONObject(body);
                if (o.has("message")) return o.getString("message");
            }
        } catch (Exception ignored) {
        }
        return "";
    }
}
