package com.trianguloy.urlchecker.tor;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebView;

import java.lang.reflect.Method;

/**
 * Routes WebView traffic through Orbot's local HTTP proxy (Privoxy).
 * Tradeoff: WebView is not as isolated as a dedicated Tor Browser; this is a cautious preview only.
 * Uses reflection because WebView proxy APIs are not exposed on all API levels.
 */
public final class TorWebViewProxy {

    private static final String TAG = "TorWebViewProxy";

    private TorWebViewProxy() {
    }

    @SuppressLint("PrivateApi")
    public static boolean apply(WebView webView, String host, int port) {
        return setProxyReflection(webView, host, port);
    }

    public static void clear() {
        // No reliable process-wide unset on older WebView; preview activity destroys the WebView instance.
        Log.d(TAG, "Tor preview WebView destroyed; no persistent proxy state kept in-app.");
    }

    @SuppressLint("PrivateApi")
    private static boolean setProxyReflection(WebView webView, String host, int port) {
        try {
            Class<?> webViewClass = Class.forName("android.webkit.WebView");
            Method setProxy = webViewClass.getDeclaredMethod(
                    "setProxy",
                    android.app.Application.class,
                    String.class,
                    int.class);
            setProxy.setAccessible(true);
            setProxy.invoke(webView, webView.getContext().getApplicationContext(), host, port);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "WebView proxy reflection failed", e);
            return false;
        }
    }
}
