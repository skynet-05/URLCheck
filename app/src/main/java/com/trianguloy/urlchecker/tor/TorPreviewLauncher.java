package com.trianguloy.urlchecker.tor;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.TorPreviewActivity;
import com.trianguloy.urlchecker.routing.DomainRoutingHelper;

/**
 * Starts the Tor sandbox preview flow from the URL inspection dialog.
 */
public final class TorPreviewLauncher {

    public static final String EXTRA_URL = "tor_preview_url";

    private TorPreviewLauncher() {
    }

    public static void start(Activity activity, String url) {
        if (url == null || url.isBlank()) return;

        com.trianguloy.urlchecker.routing.DomainRoutingHelper.startTorPreview(activity, url);
    }

    /** Called after domain routing resolved to Tor. */
    public static void startTorPreviewDirect(Activity activity, String url) {
        if (!OrbotTorHelper.isOrbotInstalled(activity)) {
            OrbotOnboarding.showMissing(activity);
            return;
        }

        Toast.makeText(activity, R.string.tor_connecting, Toast.LENGTH_SHORT).show();

        OrbotTorHelper.ensureTorRunning(activity, new OrbotTorHelper.Callback() {
            @Override
            public void onTorReady(OrbotTorHelper.TorProxy proxy) {
                activity.runOnUiThread(() -> {
                    Intent intent = new Intent(activity, TorPreviewActivity.class);
                    intent.putExtra(EXTRA_URL, url);
                    intent.putExtra(TorPreviewActivity.EXTRA_PROXY_HOST, proxy.host());
                    intent.putExtra(TorPreviewActivity.EXTRA_PROXY_PORT, proxy.httpPort());
                    activity.startActivity(intent);
                });
            }

            @Override
            public void onTorError(int messageResId) {
                activity.runOnUiThread(() -> OrbotOnboarding.showNotReady(activity, messageResId));
            }
        });
    }
}
