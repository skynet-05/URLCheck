package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.tor.TorPreviewLauncher;
import com.trianguloy.urlchecker.tor.TorWebViewProxy;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

/**
 * Sandboxed in-app preview over Orbot's Tor proxy. Session data is cleared on exit.
 * External schemes and app links require explicit confirmation — nothing opens outside the WebView automatically.
 */
public class TorPreviewActivity extends Activity {

    public static final String EXTRA_PROXY_HOST = "tor_proxy_host";
    public static final String EXTRA_PROXY_PORT = "tor_proxy_port";

    private WebView webView;
    private ProgressBar progress;
    private String initialUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_tor_preview);
        AndroidUtils.configureUp(this);

        initialUrl = getIntent().getStringExtra(TorPreviewLauncher.EXTRA_URL);
        String proxyHost = getIntent().getStringExtra(EXTRA_PROXY_HOST);
        int proxyPort = getIntent().getIntExtra(EXTRA_PROXY_PORT, 8118);

        if (initialUrl == null || proxyHost == null) {
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle(R.string.tor_preview_title);

        progress = findViewById(R.id.tor_progress);
        webView = findViewById(R.id.tor_webview);
        configureWebView();

        TorWebViewProxy.apply(this, proxyHost, proxyPort, new TorWebViewProxy.Callback() {
            @Override
            public void onProxyReady() {
                runOnUiThread(() -> webView.loadUrl(initialUrl));
            }

            @Override
            public void onProxyFailed() {
                runOnUiThread(() -> {
                    Toast.makeText(TorPreviewActivity.this, R.string.tor_proxy_failed, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setGeolocationEnabled(false);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progress.setVisibility(android.view.View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progress.setVisibility(android.view.View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleNavigation(Uri.parse(url));
            }
        });
    }

    private boolean handleNavigation(Uri uri) {
        String scheme = uri.getScheme();
        if (scheme == null) return true;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            return false;
        }
        confirmExternalNavigation(uri);
        return true;
    }

    private void confirmExternalNavigation(Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.tor_external_link_title)
                .setMessage(getString(R.string.tor_external_link_message, uri.toString()))
                .setPositiveButton(R.string.tor_external_link_open, (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    PackageUtils.startActivity(intent, R.string.toast_noApp, this);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tor_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.tor_open_browser) {
            openInNormalBrowser();
            return true;
        }
        if (id == R.id.tor_reload) {
            webView.reload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openInNormalBrowser() {
        String url = webView.getUrl();
        if (url == null || url.isEmpty()) url = initialUrl;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        PackageUtils.startActivity(intent, R.string.toast_noApp, this);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        CookieManager.getInstance().removeAllCookies(null);
        CookieManager.getInstance().flush();
        TorWebViewProxy.clear(this, null);
        super.onDestroy();
    }
}
