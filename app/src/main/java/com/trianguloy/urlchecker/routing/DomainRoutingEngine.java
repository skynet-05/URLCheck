package com.trianguloy.urlchecker.routing;

import android.content.Context;
import android.net.Uri;

import com.trianguloy.urlchecker.utilities.wrappers.InternalFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Locale;

/** Resolves domain routing from built-in rules, catalog, and default pref. */
public final class DomainRoutingEngine {

    private DomainRoutingEngine() {
    }

    public static DomainRouteAction resolve(Context context, String url) {
        String host = hostOf(url);
        if (host == null) return DomainRoutingPrefs.DEFAULT_ACTION(context).get();

        DomainRouteAction fromCatalog = matchCatalog(context, host);
        if (fromCatalog != null) return fromCatalog;

        if (host.endsWith(".onion")) return DomainRouteAction.TOR;

        return DomainRoutingPrefs.DEFAULT_ACTION(context).get();
    }

    private static String hostOf(String url) {
        try {
            Uri uri = Uri.parse(url);
            return uri.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    private static DomainRouteAction matchCatalog(Context context, String host) {
        try {
            JSONObject catalog = loadCatalog(context);
            DomainRouteAction best = null;
            int bestScore = -1;
            Iterator<String> keys = catalog.keys();
            while (keys.hasNext()) {
                String pattern = keys.next();
                if (pattern.startsWith("_")) continue;
                int score = matchScore(pattern, host);
                if (score > bestScore) {
                    bestScore = score;
                    best = DomainRouteAction.fromString(catalog.optString(pattern));
                }
            }
            return bestScore >= 0 ? best : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Higher score = more specific match. */
    static int matchScore(String pattern, String host) {
        if (pattern == null || host == null) return -1;
        String p = pattern.toLowerCase(Locale.ROOT);
        String h = host.toLowerCase(Locale.ROOT);
        if (p.equals(h)) return 1000;
        if (p.startsWith("*.")) {
            String suffix = p.substring(1);
            if (h.endsWith(suffix) && h.length() > suffix.length()) return 500 + suffix.length();
        }
        if (p.contains("*")) {
            String regex = p.replace(".", "\\.").replace("*", ".*");
            if (h.matches(regex)) return 100;
        }
        return -1;
    }

    static JSONObject loadCatalog(Context context) throws JSONException {
        InternalFile custom = new InternalFile("domain_routing", context);
        String content = custom.get();
        if (content != null) return new JSONObject(content);
        return new JSONObject().put("*.onion", DomainRouteAction.TOR.toCatalogValue());
    }
}
