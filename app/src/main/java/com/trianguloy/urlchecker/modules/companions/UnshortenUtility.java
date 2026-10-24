package com.trianguloy.urlchecker.modules.companions;

import static com.trianguloy.urlchecker.utilities.methods.JavaUtils.sUTF_8;

import android.util.Pair;

import com.trianguloy.urlchecker.utilities.UserFacingErrors;
import com.trianguloy.urlchecker.utilities.methods.HttpUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/** Unshorten logic extracted from main class. */
public class UnshortenUtility {
    public record UnshortenResult(
            boolean success,
            String error,
            String finalUrl,
            int remainingCalls,
            int usageLimit,
            boolean notShortened
    ) {
    }

    /** Calls the unshorten api to unshorten [url]. Can be authenticated ([token]!=null) or not */
    public static UnshortenResult unshort(String url, String token) throws IOException, JSONException {
        String apiUrl = token.isEmpty()
                ? "https://unshorten.me/json/" + URLEncoder.encode(url, sUTF_8)
                : "https://unshorten.me/api/v2/unshorten?url=" + URLEncoder.encode(url, sUTF_8);
        Map<String, String> headers = token.isEmpty()
                ? Map.of()
                : Map.of(
                        "Content-Type", "application/json",
                        "Authorization", "Token " + token);
        Pair<Integer, String> http = HttpUtils.readFromUrl(apiUrl, headers);

        int httpCode = http.first;
        String body = http.second;

        if (httpCode < 200 || httpCode >= 300) {
            return failure("http:" + httpCode, url, 0, 10, false);
        }

        var response = new JSONObject(body);

        var finalUrl = response.optString(token.isEmpty() ? "resolved_url" : "unshortened_url", "");
        var usageCount = Integer.parseInt(response.optString("usage_count", "0"));
        int usageLimit = 10;
        int remainingCalls = usageLimit - usageCount;
        var error = response.optString("error", "");
        var success = response.optBoolean("success", false);
        String requested = response.optString("requested_url", url);

        try {
            remainingCalls = Integer.parseInt(response.optString("remaining_calls", ""));
            usageLimit = usageCount + remainingCalls;
        } catch (NumberFormatException ignore) {
        }

        if (!success) {
            if (finalUrl.isEmpty() || UserFacingErrors.isNotShortened(error, requested)) {
                return new UnshortenResult(true, null, url, remainingCalls, usageLimit, true);
            }
            return failure(error.isEmpty() ? "api" : error, url, remainingCalls, usageLimit, false);
        }

        if (finalUrl.isEmpty()) {
            finalUrl = url;
        }

        return new UnshortenResult(
                true,
                null,
                finalUrl,
                remainingCalls,
                usageLimit,
                finalUrl.equals(url) || finalUrl.equals(requested)
        );
    }

    private static UnshortenResult failure(String error, String url, int remaining, int limit, boolean notShortened) {
        return new UnshortenResult(false, error, url, remaining, limit, notShortened);
    }
}
