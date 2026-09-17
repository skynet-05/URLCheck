package com.trianguloy.urlchecker.tor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Routes WebView traffic through Orbot's local HTTP proxy (Privoxy).
 * Tradeoff: WebView is not as isolated as a dedicated Tor Browser; this is a cautious preview only.
 *
 * On Android 10+ / current Samsung System WebView, {@code WebView.setProxy} reflection is removed;
 * {@link WebViewFeature#PROXY_OVERRIDE} via AndroidX WebKit is the supported path.
 */
public final class TorWebViewProxy {

    private static final String TAG = "TorWebViewProxy";

    public interface Callback {
        void onProxyReady();

        void onProxyFailed();
    }

    private TorWebViewProxy() {
    }

    /**
     * Configures the process-wide WebView proxy, then invokes the callback.
     * Call {@link #clear(Context, Runnable)} when the preview activity is destroyed.
     */
    public static void apply(Context context, String host, int port, Callback callback) {
        String proxyRule = host + ":" + port;
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyConfig config = new ProxyConfig.Builder()
                    .addProxyRule(proxyRule)
                    .build();
            Executor executor = Runnable::run;
            try {
                ProxyController.getInstance().setProxyOverride(config, executor, () -> {
                    Log.i(TAG, "PROXY_OVERRIDE active: " + proxyRule);
                    callback.onProxyReady();
                });
            } catch (Exception e) {
                Log.e(TAG, "ProxyController.setProxyOverride failed", e);
                tryLegacyReflection(context, host, port, callback);
            }
            return;
        }
        Log.w(TAG, "PROXY_OVERRIDE not supported by this WebView; trying legacy reflection");
        tryLegacyReflection(context, host, port, callback);
    }

    public static void clear(Context context, Runnable whenDone) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            try {
                ProxyController.getInstance().clearProxyOverride(Runnable::run, () -> {
                    Log.d(TAG, "PROXY_OVERRIDE cleared");
                    if (whenDone != null) whenDone.run();
                });
                return;
            } catch (Exception e) {
                Log.w(TAG, "Failed to clear proxy override", e);
            }
        }
        if (whenDone != null) whenDone.run();
    }

    private static void tryLegacyReflection(Context context, String host, int port, Callback callback) {
        if (setProxyReflection(context, host, port)) {
            callback.onProxyReady();
        } else {
            callback.onProxyFailed();
        }
    }

    @SuppressLint("PrivateApi")
    private static boolean setProxyReflection(Context context, String host, int port) {
        try {
            Class<?> webViewClass = Class.forName("android.webkit.WebView");
            Method setProxy = webViewClass.getDeclaredMethod(
                    "setProxy",
                    android.app.Application.class,
                    String.class,
                    int.class);
            setProxy.setAccessible(true);
            setProxy.invoke(null, context.getApplicationContext(), host, port);
            Log.i(TAG, "Legacy WebView.setProxy reflection succeeded");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "WebView.setProxy reflection failed (expected on API 29+ / Samsung WebView)", e);
            return false;
        }
    }
}
