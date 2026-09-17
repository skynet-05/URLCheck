package com.trianguloy.urlchecker.tor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.TorPreviewActivity;

/**
 * Starts the Tor sandbox preview flow from the URL inspection dialog.
 */
public final class TorPreviewLauncher {

    public static final String EXTRA_URL = "tor_preview_url";

    private TorPreviewLauncher() {
    }

    public static void start(Activity activity, String url) {
        if (url == null || url.isBlank()) return;

        if (!OrbotTorHelper.isOrbotInstalled(activity)) {
            showOrbotMissingDialog(activity);
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
                activity.runOnUiThread(() ->
                        Toast.makeText(activity, messageResId, Toast.LENGTH_LONG).show());
            }
        });
    }

    private static void showOrbotMissingDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tor_orbot_missing_title)
                .setMessage(R.string.tor_orbot_missing)
                .setPositiveButton(R.string.tor_orbot_install, (d, w) -> {
                    Intent market = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://f-droid.org/packages/" + OrbotTorHelper.ORBOT_PACKAGE + "/"));
                    activity.startActivity(market);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
