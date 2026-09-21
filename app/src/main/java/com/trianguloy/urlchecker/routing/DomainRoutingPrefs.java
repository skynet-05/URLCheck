package com.trianguloy.urlchecker.routing;

import android.content.Context;

import com.trianguloy.urlchecker.utilities.generics.GenericPref.EnumerationPref;

/** Default when no catalog rule matches. */
public final class DomainRoutingPrefs {

    public static EnumerationPref<DomainRouteAction> DEFAULT_ACTION(Context context) {
        return new EnumerationPref<>("domain_route_default", DomainRouteAction.ASK, DomainRouteAction.class, context);
    }

    private DomainRoutingPrefs() {
    }
}
