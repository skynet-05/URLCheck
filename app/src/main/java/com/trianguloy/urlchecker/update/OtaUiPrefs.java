package com.trianguloy.urlchecker.update;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.LongPref;

/** Last OTA check timestamp for UI. */
public final class OtaUiPrefs {

    public static LongPref LAST_CHECK_AT(Context context) {
        return new LongPref("ota_last_check_at", 0L, context);
    }

    private OtaUiPrefs() {
    }
}
