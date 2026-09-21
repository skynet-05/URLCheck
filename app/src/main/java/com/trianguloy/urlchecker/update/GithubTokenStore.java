package com.trianguloy.urlchecker.update;

import android.content.Context;
import android.content.SharedPreferences;

/** Optional GitHub PAT for REST API fallback (Atom is primary). Stored in app-private prefs only. */
public final class GithubTokenStore {

    private static final String PREFS = "linkguard_github_token";

    private GithubTokenStore() {
    }

    public static String getToken(Context context) {
        return prefs(context).getString("pat", "");
    }

    public static void setToken(Context context, String token) {
        prefs(context).edit().putString("pat", token == null ? "" : token.trim()).apply();
    }

    public static boolean hasToken(Context context) {
        String t = getToken(context);
        return t != null && !t.isEmpty();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
