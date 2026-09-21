package com.trianguloy.urlchecker.tor;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.IntPref;

/** Tor preview WebView and session preferences. */
public final class TorPreviewPrefs {

    public static BoolPref JAVASCRIPT(Context context) {
        return new BoolPref("tor_preview_js", false, context);
    }

    public static BoolPref IMAGES(Context context) {
        return new BoolPref("tor_preview_images", true, context);
    }

    public static BoolPref ALLOW_DOWNLOADS(Context context) {
        return new BoolPref("tor_preview_downloads", false, context);
    }

    public static BoolPref AUTO_CLEAR_ON_CLOSE(Context context) {
        return new BoolPref("tor_preview_auto_clear", true, context);
    }

    public static IntPref LOAD_TIMEOUT_SEC(Context context) {
        return new IntPref("tor_preview_timeout_sec", 90, context);
    }

    private TorPreviewPrefs() {
    }
}
