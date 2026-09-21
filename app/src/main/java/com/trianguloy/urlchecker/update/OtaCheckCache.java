package com.trianguloy.urlchecker.update;

import android.content.Context;
import android.content.SharedPreferences;

/** Short-lived cache so manual/silent checks do not hammer GitHub. */
final class OtaCheckCache {

    private static final long TTL_MS = 15 * 60 * 1000L;
    private static final String PREFS = "linkguard_ota_cache";

    private static volatile long memoryAt;
    private static volatile int memoryInstalledCode = -1;
    private static volatile UpdateRelease memoryRelease;
    private static volatile boolean memoryNoUpdate;

    private OtaCheckCache() {
    }

    static boolean hasValid(Context context, int installedVersionCode) {
        long now = System.currentTimeMillis();
        if (memoryInstalledCode == installedVersionCode && now - memoryAt < TTL_MS) {
            return true;
        }
        SharedPreferences prefs = prefs(context);
        int code = prefs.getInt("installed_vc", -1);
        long at = prefs.getLong("checked_at", 0L);
        return code == installedVersionCode && at > 0 && now - at < TTL_MS;
    }

    /** {@code null} means no update was found (still a valid cached result). */
    static UpdateRelease get(Context context, int installedVersionCode) {
        long now = System.currentTimeMillis();
        if (memoryInstalledCode == installedVersionCode && now - memoryAt < TTL_MS) {
            return memoryNoUpdate ? null : memoryRelease;
        }
        SharedPreferences prefs = prefs(context);
        if (prefs.getInt("installed_vc", -1) != installedVersionCode) return null;
        if (now - prefs.getLong("checked_at", 0L) >= TTL_MS) return null;
        if (prefs.getBoolean("no_update", false)) return null;
        String tag = prefs.getString("tag", null);
        if (tag == null) return null;
        return new UpdateRelease(
                tag,
                prefs.getInt("version_code", 0),
                prefs.getString("version_name", ""),
                prefs.getString("apk_url", ""),
                prefs.getLong("apk_size", 0L));
    }

    static void put(Context context, int installedVersionCode, UpdateRelease release) {
        long now = System.currentTimeMillis();
        memoryAt = now;
        memoryInstalledCode = installedVersionCode;
        memoryRelease = release;
        memoryNoUpdate = release == null;

        SharedPreferences.Editor ed = prefs(context).edit();
        ed.putLong("checked_at", now);
        ed.putInt("installed_vc", installedVersionCode);
        if (release == null) {
            ed.putBoolean("no_update", true);
            ed.remove("tag");
        } else {
            ed.putBoolean("no_update", false);
            ed.putString("tag", release.tagName);
            ed.putInt("version_code", release.versionCode);
            ed.putString("version_name", release.versionName);
            ed.putString("apk_url", release.apkUrl);
            ed.putLong("apk_size", release.apkSizeBytes);
        }
        ed.apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
