package com.trianguloy.urlchecker.tor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;

import com.trianguloy.urlchecker.R;

/** Explains Orbot setup when Tor preview cannot proceed. */
public final class OrbotOnboarding {

    private OrbotOnboarding() {
    }

    public static void showMissing(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tor_orbot_missing_title)
                .setMessage(activity.getString(R.string.tor_orbot_missing, activity.getString(R.string.app_name)))
                .setPositiveButton(R.string.tor_orbot_install_play, (d, w) -> openPlay(activity))
                .setNeutralButton(R.string.tor_orbot_install, (d, w) -> openFdroid(activity))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void showNotReady(Activity activity, int messageResId) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.tor_orbot_not_ready_title)
                .setMessage(activity.getString(messageResId) + "\n\n" + activity.getString(R.string.tor_orbot_samsung_hint))
                .setPositiveButton(R.string.tor_orbot_open_app, (d, w) -> openOrbot(activity))
                .setNeutralButton(R.string.tor_orbot_retry, (d, w) -> OrbotTorHelper.ensureTorRunning(activity, new OrbotTorHelper.Callback() {
                    @Override
                    public void onTorReady(OrbotTorHelper.TorProxy proxy) {
                    }

                    @Override
                    public void onTorError(int messageResId2) {
                        activity.runOnUiThread(() ->
                                new AlertDialog.Builder(activity)
                                        .setMessage(messageResId2)
                                        .setPositiveButton(android.R.string.ok, null)
                                        .show());
                    }
                }))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void openOrbot(Activity activity) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(OrbotTorHelper.ORBOT_PACKAGE);
        if (launch != null) {
            activity.startActivity(launch);
            return;
        }
        Intent start = new Intent(OrbotTorHelper.ACTION_START);
        start.setPackage(OrbotTorHelper.ORBOT_PACKAGE);
        start.putExtra(OrbotTorHelper.EXTRA_PACKAGE_NAME, activity.getPackageName());
        activity.sendBroadcast(start);
    }

    private static void openPlay(Activity activity) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + OrbotTorHelper.ORBOT_PACKAGE)));
        } catch (Exception e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + OrbotTorHelper.ORBOT_PACKAGE)));
        }
    }

    private static void openFdroid(Activity activity) {
        activity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://f-droid.org/packages/" + OrbotTorHelper.ORBOT_PACKAGE + "/")));
    }
}
