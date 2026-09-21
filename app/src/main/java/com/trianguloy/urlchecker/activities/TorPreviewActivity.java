package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.tor.TorPreviewLauncher;
import com.trianguloy.urlchecker.tor.TorPreviewPrefs;
import com.trianguloy.urlchecker.tor.TorPreviewSession;
import com.trianguloy.urlchecker.tor.TorStatusCoordinator;
import com.trianguloy.urlchecker.tor.TorWebViewProxy;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;
import com.trianguloy.urlchecker.utilities.methods.PackageUtils;

/**
 * Sandboxed in-app preview over Orbot's Tor proxy. Session data is cleared on exit.
 */
public class TorPreviewActivity extends Activity implements TorStatusCoordinator.Host {

    public static final String EXTRA_PROXY_HOST = "tor_proxy_host";
    public static final String EXTRA_PROXY_PORT = "tor_proxy_port";

    private WebView webView;
    private ProgressBar progress;
    private View errorPanel;
    private TextView errorText;
    private String initialUrl;
    private String proxyHost;
    private int proxyPort;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loadTimeoutRunnable;
    private boolean loadFinished;
    private TorStatusCoordinator torStatus;
    private ImageView statusIcon;
    private TextView statusText;
    private ProgressBar statusProgress;
    private View retryBtn;
    private View retryNewCircuitBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_tor_preview);
        AndroidUtils.configureUp(this);

        initialUrl = getIntent().getStringExtra(TorPreviewLauncher.EXTRA_URL);
        proxyHost = getIntent().getStringExtra(EXTRA_PROXY_HOST);
        proxyPort = getIntent().getIntExtra(EXTRA_PROXY_PORT, 8118);

        if (initialUrl == null || proxyHost == null) {
            Toast.makeText(this, R.string.invalid, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setTitle(R.string.tor_preview_title);

        progress = findViewById(R.id.tor_progress);
        webView = findViewById(R.id.tor_webview);
        errorPanel = findViewById(R.id.tor_error_panel);
        errorText = findViewById(R.id.tor_error_message);
        statusIcon = findViewById(R.id.tor_status_icon);
        statusText = findViewById(R.id.tor_status_text);
        statusProgress = findViewById(R.id.tor_status_progress);
        retryBtn = findViewById(R.id.tor_retry);
        retryNewCircuitBtn = findViewById(R.id.tor_retry_new_circuit);
        retryBtn.setOnClickListener(v -> retryLoad(false));
        retryNewCircuitBtn.setOnClickListener(v -> retryLoad(true));

        torStatus = new TorStatusCoordinator(this);
        torStatus.refresh(false);

        configureWebView();
        applyProxyAndLoad(initialUrl);
    }

    @Override
    public Activity torHostActivity() {
        return this;
    }

    @Override
    public ImageView statusIcon() {
        return statusIcon;
    }

    @Override
    public TextView statusText() {
        return statusText;
    }

    @Override
    public ProgressBar statusProgress() {
        return statusProgress;
    }

    @Override
    public View[] circuitControls() {
        return new View[]{retryNewCircuitBtn, retryBtn};
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(TorPreviewPrefs.JAVASCRIPT(this).get());
        settings.setLoadsImagesAutomatically(TorPreviewPrefs.IMAGES(this).get());
        settings.setBlockNetworkImage(!TorPreviewPrefs.IMAGES(this).get());
        settings.setDomStorageEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setGeolocationEnabled(false);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, false);

        if (TorPreviewPrefs.ALLOW_DOWNLOADS(this).get()) {
            webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                    Toast.makeText(this, R.string.tor_download_blocked, Toast.LENGTH_SHORT).show());
        } else {
            webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
                    Toast.makeText(this, R.string.tor_download_blocked, Toast.LENGTH_SHORT).show());
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                loadFinished = false;
                showError(false);
                progress.setVisibility(View.VISIBLE);
                scheduleLoadTimeout();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                loadFinished = true;
                cancelLoadTimeout();
                progress.setVisibility(View.GONE);
                showError(false);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    showLoadError(getString(R.string.tor_load_failed));
                }
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

    private void applyProxyAndLoad(String url) {
        TorWebViewProxy.apply(this, proxyHost, proxyPort, new TorWebViewProxy.Callback() {
            @Override
            public void onProxyReady() {
                runOnUiThread(() -> webView.loadUrl(url));
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

    private void scheduleLoadTimeout() {
        cancelLoadTimeout();
        long ms = TorPreviewPrefs.LOAD_TIMEOUT_SEC(this).get() * 1000L;
        loadTimeoutRunnable = () -> {
            if (!loadFinished) showLoadError(getString(R.string.tor_load_timeout));
        };
        handler.postDelayed(loadTimeoutRunnable, ms);
    }

    private void cancelLoadTimeout() {
        if (loadTimeoutRunnable != null) handler.removeCallbacks(loadTimeoutRunnable);
    }

    private void showLoadError(String message) {
        cancelLoadTimeout();
        progress.setVisibility(View.GONE);
        errorText.setText(message);
        showError(true);
    }

    private void showError(boolean show) {
        errorPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        webView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void retryLoad(boolean newCircuit) {
        if (torStatus.isCircuitBusy()) return;
        Runnable reload = () -> {
            showError(false);
            String url = webView.getUrl();
            if (url == null || url.isEmpty() || url.startsWith("about:")) url = initialUrl;
            webView.loadUrl(url);
        };
        if (newCircuit) {
            torStatus.requestNewCircuit(reload);
        } else {
            reload.run();
        }
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
        if (id == R.id.tor_clear_session) {
            TorPreviewSession.clear(webView);
            Toast.makeText(this, R.string.tor_session_cleared, Toast.LENGTH_SHORT).show();
            webView.loadUrl(initialUrl);
            return true;
        }
        if (id == R.id.tor_new_circuit) {
            if (!torStatus.isCircuitBusy()) {
                torStatus.requestNewCircuit(() -> webView.reload());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem nym = menu.findItem(R.id.tor_new_circuit);
        if (nym != null) nym.setEnabled(!torStatus.isCircuitBusy());
        return super.onPrepareOptionsMenu(menu);
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
        cancelLoadTimeout();
        if (torStatus != null) torStatus.destroy();
        if (webView != null) {
            if (TorPreviewPrefs.AUTO_CLEAR_ON_CLOSE(this).get()) {
                TorPreviewSession.clear(webView);
            }
            webView.stopLoading();
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
