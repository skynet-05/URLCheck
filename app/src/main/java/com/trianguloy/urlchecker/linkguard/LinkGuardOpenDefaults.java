package com.trianguloy.urlchecker.linkguard;

import android.content.Context;

import com.trianguloy.urlchecker.BuildConfig;
import com.trianguloy.urlchecker.modules.companions.CTabs;
import com.trianguloy.urlchecker.modules.companions.Incognito;
import com.trianguloy.urlchecker.modules.companions.OnOffConfig;
import com.trianguloy.urlchecker.modules.companions.PrivateLinkBrowser;
import com.trianguloy.urlchecker.routing.DomainRouteAction;
import com.trianguloy.urlchecker.routing.DomainRoutingPrefs;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;

/**
 * One-time migration so Link Guard opens links in the external browser by default
 * (Custom Tabs off, routing clearnet) while keeping *.onion → Tor in the catalog.
 */
public final class LinkGuardOpenDefaults {

    private static final String MIGRATION_KEY = "linkguard_open_defaults_v1";
    private static final String PRIVATE_BROWSER_MIGRATION_KEY = "linkguard_private_browser_default_v1";

    private LinkGuardOpenDefaults() {
    }

    public static void applyIfNeeded(Context context) {
        if (!BuildConfig.WHITE_LABEL) return;
        var prefs = GenericPref.getPrefs(context);
        if (!prefs.getBoolean(MIGRATION_KEY, false)) {
            DomainRoutingPrefs.DEFAULT_ACTION(context).set(DomainRouteAction.CLEARNET);
            CTabs.PREF(context).set(OnOffConfig.DEFAULT_OFF);
            Incognito.PREF(context).set(OnOffConfig.DEFAULT_OFF);
            prefs.edit().putBoolean(MIGRATION_KEY, true).apply();
        }

        if (!prefs.getBoolean(PRIVATE_BROWSER_MIGRATION_KEY, false)) {
            if (PrivateLinkBrowser.PREF(context).get().isEmpty()
                    && PrivateLinkBrowser.isInstalled(context, PrivateLinkBrowser.PKG_FIREFOX)) {
                PrivateLinkBrowser.PREF(context).set(PrivateLinkBrowser.PKG_FIREFOX);
            }
            prefs.edit().putBoolean(PRIVATE_BROWSER_MIGRATION_KEY, true).apply();
        }
    }
}
