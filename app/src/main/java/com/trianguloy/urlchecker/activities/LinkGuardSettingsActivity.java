package com.trianguloy.urlchecker.activities;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.history.RecentLinksStore;
import com.trianguloy.urlchecker.routing.DomainRoutingCatalog;
import com.trianguloy.urlchecker.routing.DomainRoutingPrefs;
import com.trianguloy.urlchecker.tor.TorPreviewPrefs;
import com.trianguloy.urlchecker.update.GithubTokenStore;
import com.trianguloy.urlchecker.utilities.AndroidSettings;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.LocaleUtils;

/** Link Guard–specific settings (Tor, routing, history, OTA token). */
public class LinkGuardSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidSettings.setTheme(this, false);
        LocaleUtils.setLocale(this);
        setContentView(R.layout.activity_link_guard_settings);
        setTitle(R.string.linkguard_settings_title);
        AndroidUtils.configureUp(this);

        TorPreviewPrefs.JAVASCRIPT(this).attachToSwitch(findViewById(R.id.tor_pref_js));
        TorPreviewPrefs.IMAGES(this).attachToSwitch(findViewById(R.id.tor_pref_images));
        TorPreviewPrefs.ALLOW_DOWNLOADS(this).attachToSwitch(findViewById(R.id.tor_pref_downloads));
        TorPreviewPrefs.AUTO_CLEAR_ON_CLOSE(this).attachToSwitch(findViewById(R.id.tor_pref_auto_clear));

        SeekBar timeout = findViewById(R.id.tor_pref_timeout);
        TextView timeoutLabel = findViewById(R.id.tor_pref_timeout_label);
        TorPreviewPrefs.LOAD_TIMEOUT_SEC(this).attachToSeekBar(timeout, timeoutLabel,
                v -> android.util.Pair.create(v, getString(R.string.tor_timeout_seconds, v)),
                v -> Math.max(30, v));
        timeout.setProgress(TorPreviewPrefs.LOAD_TIMEOUT_SEC(this).get());

        RecentLinksStore.ENABLED(this).attachToSwitch(findViewById(R.id.recent_links_enabled));
        SeekBar retention = findViewById(R.id.recent_links_retention);
        TextView retentionLabel = findViewById(R.id.recent_links_retention_label);
        RecentLinksStore.RETENTION_DAYS(this).attachToSeekBar(retention, retentionLabel,
                v -> android.util.Pair.create(v, getString(R.string.recent_links_days, v)),
                v -> Math.max(1, v));

        DomainRoutingPrefs.DEFAULT_ACTION(this).attachToSpinner(
                (Spinner) findViewById(R.id.domain_default_action), null);

        findViewById(R.id.domain_routing_edit).setOnClickListener(v ->
                new DomainRoutingCatalog(this).showEditor());

        EditText pat = findViewById(R.id.github_pat);
        pat.setText(GithubTokenStore.getToken(this));
        findViewById(R.id.github_pat_save).setOnClickListener(v ->
                GithubTokenStore.setToken(this, pat.getText().toString()));

        findViewById(R.id.open_recent_links).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, RecentLinksActivity.class)));
    }
}
