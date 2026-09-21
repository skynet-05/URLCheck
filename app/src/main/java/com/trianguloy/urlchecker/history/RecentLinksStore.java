package com.trianguloy.urlchecker.history;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.BoolPref;
import com.trianguloy.urlchecker.utilities.generics.GenericPref.IntPref;
import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Persisted recent checked URLs with retention. */
public final class RecentLinksStore {

    private static final String FILE = "recent_links";

    public static BoolPref ENABLED(Context context) {
        return new BoolPref("recent_links_enabled", true, context);
    }

    public static IntPref RETENTION_DAYS(Context context) {
        return new IntPref("recent_links_retention_days", 7, context);
    }

    private RecentLinksStore() {
    }

    public static void record(Context context, String url, int threatFlagCount) {
        if (!ENABLED(context).get() || url == null || url.isBlank()) return;
        try {
            JSONArray arr = loadRaw(context);
            JSONArray next = new JSONArray();
            long now = System.currentTimeMillis();
            next.put(new JSONObject()
                    .put("url", url)
                    .put("t", now)
                    .put("threats", threatFlagCount));
            for (int i = 0; i < arr.length() && next.length() < 200; i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!url.equals(o.optString("url"))) next.put(o);
            }
            prune(context, next);
            save(context, next);
        } catch (JSONException ignored) {
        }
    }

    public static List<Entry> list(Context context) {
        prune(context, null);
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray arr = loadRaw(context);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Entry(
                        o.optString("url"),
                        o.optLong("t"),
                        o.optInt("threats", 0)));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    public static void delete(Context context, String url) {
        try {
            JSONArray arr = loadRaw(context);
            JSONArray next = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (!url.equals(o.optString("url"))) next.put(o);
            }
            save(context, next);
        } catch (JSONException ignored) {
        }
    }

    public static void wipeAll(Context context) {
        new InternalFile(FILE, context).delete();
    }

    private static void prune(Context context, JSONArray inPlace) {
        try {
            JSONArray arr = inPlace != null ? inPlace : loadRaw(context);
            long cutoff = System.currentTimeMillis()
                    - RETENTION_DAYS(context).get() * 24L * 60 * 60 * 1000;
            JSONArray next = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.optLong("t") >= cutoff) next.put(o);
            }
            if (inPlace == null) save(context, next);
        } catch (JSONException ignored) {
        }
    }

    private static JSONArray loadRaw(Context context) throws JSONException {
        String content = new InternalFile(FILE, context).get();
        if (content == null) return new JSONArray();
        return new JSONArray(content);
    }

    private static void save(Context context, JSONArray arr) {
        new InternalFile(FILE, context).set(arr.toString());
    }

    public record Entry(String url, long timestampMs, int threatCount) {
    }
}
