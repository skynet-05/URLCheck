package com.trianguloy.urlchecker.tor;

import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;

/** Wipes Tor preview WebView state between sessions. */
public final class TorPreviewSession {

    private TorPreviewSession() {
    }

    public static void clear(WebView webView) {
        if (webView == null) return;
        webView.stopLoading();
        webView.clearHistory();
        webView.clearCache(true);
        webView.clearFormData();
        WebStorage.getInstance().deleteAllData();
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        webView.loadUrl("about:blank");
    }
}
