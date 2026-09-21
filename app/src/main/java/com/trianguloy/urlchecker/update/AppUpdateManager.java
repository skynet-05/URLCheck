package com.trianguloy.urlchecker.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.LongPref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** Checks GitHub for Link Guard APK updates and installs them. */
public final class AppUpdateManager {

    private static final long SILENT_INTERVAL_MS = 24 * 60 * 60 * 1000L;

    public interface UiCallback {
        void onNoUpdate();

        void onError(String message);
    }

    private AppUpdateManager() {
    }

    private static LongPref lastSilentCheckPref(Activity activity) {
        return new LongPref("ota_last_silent_check", 0L, activity);
    }

    /** Non-blocking check on launch; shows dialog only when an update exists. */
    public static void checkOnLaunch(Activity activity) {
        if (!LinkGuardUpdateConfig.isEnabled()) return;
        long now = System.currentTimeMillis();
        if (now - lastSilentCheckPref(activity).get() < SILENT_INTERVAL_MS) return;
        lastSilentCheckPref(activity).set(now);
        check(activity, false, null);
    }

    /** User-initiated check from Settings / About. */
    public static void checkManual(Activity activity) {
        if (!LinkGuardUpdateConfig.isEnabled()) return;
        check(activity, true, null);
    }

    private static void check(Activity activity, boolean manual, UiCallback callback) {
        android.app.ProgressDialog progress = manual ? new android.app.ProgressDialog(activity) : null;
        if (progress != null) {
            progress.setMessage(activity.getString(R.string.ota_checking));
            progress.show();
        }

        new Thread(() -> {
            try {
                UpdateRelease update = GitHubReleaseClient.findLatestUpdate(
                        BuildConfig.VERSION_CODE,
                        BuildConfig.VERSION_NAME);
                runOnUi(activity, () -> {
                    if (progress != null) progress.dismiss();
                    if (update == null) {
                        if (manual) {
                            new AlertDialog.Builder(activity)
                                    .setMessage(R.string.ota_up_to_date)
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        }
                        if (callback != null) callback.onNoUpdate();
                        return;
                    }
                    promptInstall(activity, update);
                });
            } catch (Exception e) {
                runOnUi(activity, () -> {
                    if (progress != null) progress.dismiss();
                    if (manual) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.ota_error_title)
                                .setMessage(activity.getString(R.string.ota_error_message, e.getMessage()))
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                    if (callback != null) callback.onError(e.getMessage());
                });
            }
        }).start();
    }

    private static void promptInstall(Activity activity, UpdateRelease update) {
        String message = activity.getString(
                R.string.ota_update_available,
                update.versionName,
                update.versionCode > 0 ? String.valueOf(update.versionCode) : update.tagName);
        new AlertDialog.Builder(activity)
                .setTitle(R.string.ota_update_title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.ota_download_install, (d, w) -> downloadAndInstall(activity, update))
                .show();
    }

    private static void downloadAndInstall(Activity activity, UpdateRelease update) {
        android.app.ProgressDialog progress = new android.app.ProgressDialog(activity);
        progress.setMessage(activity.getString(R.string.ota_downloading));
        progress.setCancelable(false);
        progress.show();

        new Thread(() -> {
            try {
                File apk = downloadApk(activity, update, progress);
                runOnUi(activity, () -> {
                    progress.dismiss();
                    if (!ApkSignatureCompat.signaturesMatch(activity, apk)) {
                        new AlertDialog.Builder(activity)
                                .setTitle(R.string.ota_signature_title)
                                .setMessage(R.string.ota_signature_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                        return;
                    }
                    ApkInstallHelper.install(activity, apk);
                });
            } catch (Exception e) {
                runOnUi(activity, () -> {
                    progress.dismiss();
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.ota_error_title)
                            .setMessage(activity.getString(R.string.ota_error_message, e.getMessage()))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }
        }).start();
    }

    private static File downloadApk(Activity activity, UpdateRelease update, android.app.ProgressDialog progress) throws Exception {
        File dir = new File(activity.getExternalFilesDir(null), "updates");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new Exception("Cannot create update folder");
        }
        File out = new File(dir, "LinkGuard-update.apk");
        if (out.exists()) out.delete();

        HttpURLConnection conn = (HttpURLConnection) new URL(update.apkUrl).openConnection();
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(120_000);
        conn.setInstanceFollowRedirects(true);
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new Exception("Download HTTP " + conn.getResponseCode());
        }

        int total = conn.getContentLength();
        try (InputStream in = conn.getInputStream(); FileOutputStream fos = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int read;
            int done = 0;
            while ((read = in.read(buf)) != -1) {
                fos.write(buf, 0, read);
                done += read;
                if (total > 0) {
                    int pct = (int) (100L * done / total);
                    runOnUi(activity, () -> progress.setMessage(
                            activity.getString(R.string.ota_downloading_percent, pct)));
                }
            }
        } finally {
            conn.disconnect();
        }
        return out;
    }

    private static void runOnUi(Activity activity, Runnable runnable) {
        if (activity.isFinishing()) return;
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
