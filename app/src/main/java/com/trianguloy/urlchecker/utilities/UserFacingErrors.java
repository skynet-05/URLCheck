package com.trianguloy.urlchecker.utilities;

import android.content.Context;

import com.trianguloy.urlchecker.R;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.net.ssl.SSLException;

/** Maps exceptions and HTTP codes to short user-visible messages. */
public final class UserFacingErrors {

    private UserFacingErrors() {
    }

    public static String networkError(Context context, IOException e) {
        if (e instanceof SocketTimeoutException) {
            return context.getString(R.string.err_network_timeout);
        }
        if (e instanceof UnknownHostException) {
            return context.getString(R.string.err_network_dns);
        }
        if (e instanceof ConnectException) {
            return context.getString(R.string.err_network_connect);
        }
        if (e instanceof SSLException) {
            return context.getString(R.string.err_network_ssl);
        }
        String msg = e.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase(Locale.ROOT);
            if (lower.contains("timeout")) return context.getString(R.string.err_network_timeout);
            if (lower.contains("unable to resolve host") || lower.contains("unknown host")) {
                return context.getString(R.string.err_network_dns);
            }
            if (lower.contains("connection") && lower.contains("refused")) {
                return context.getString(R.string.err_network_connect);
            }
        }
        return context.getString(R.string.err_network_generic, msg != null ? msg : e.getClass().getSimpleName());
    }

    public static String httpStatus(Context context, int code) {
        if (code == 429) return context.getString(R.string.err_http_429);
        if (code == 401 || code == 403) return context.getString(R.string.err_http_auth, code);
        if (code >= 500) return context.getString(R.string.err_http_server, code);
        if (code == 404) return context.getString(R.string.err_http_404);
        if (code >= 400) return context.getString(R.string.err_http_client, code);
        return context.getString(R.string.err_http_other, code);
    }

    public static String unshortApiError(Context context, int httpCode, String apiError, String requestedUrl) {
        if (httpCode > 0 && (httpCode < 200 || httpCode >= 300)) {
            return context.getString(R.string.mUnshort_error_fmt, httpStatus(context, httpCode));
        }
        if (isNotShortened(apiError, requestedUrl)) {
            return context.getString(R.string.mUnshort_not_shortened);
        }
        if (apiError == null || apiError.isBlank()
                || apiError.toLowerCase(Locale.ROOT).contains("unknown error")) {
            return context.getString(R.string.mUnshort_not_shortened);
        }
        return context.getString(R.string.mUnshort_error_fmt, apiError);
    }

    public static boolean isNotShortened(String apiError, String requestedUrl) {
        if (requestedUrl == null) return false;
        if (apiError == null) return true;
        String lower = apiError.toLowerCase(Locale.ROOT);
        return lower.contains("unknown error")
                || lower.contains("not a valid")
                || lower.contains("not shortened")
                || lower.contains("no redirect");
    }

    public static String otaFailure(Context context, Throwable e) {
        if (e == null) return context.getString(R.string.err_unknown);
        String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) return msg;
        if (e instanceof IOException) return networkError(context, (IOException) e);
        return context.getString(R.string.err_unknown);
    }

    public static String downloadFailure(Context context, int httpCode) {
        if (httpCode > 0) return context.getString(R.string.ota_error_message, httpStatus(context, httpCode));
        return context.getString(R.string.err_unknown);
    }
}
