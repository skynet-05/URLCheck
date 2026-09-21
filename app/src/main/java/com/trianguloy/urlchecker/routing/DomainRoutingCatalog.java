package com.trianguloy.urlchecker.routing;

import android.app.Activity;
import android.content.Context;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.JsonCatalog;

import org.json.JSONException;
import org.json.JSONObject;

/** User-defined host patterns → tor / clearnet / ask. */
public class DomainRoutingCatalog extends JsonCatalog {

    public DomainRoutingCatalog(Activity cntx) {
        super(cntx, "domain_routing", R.string.domain_routing_editor);
    }

    @Override
    public JSONObject buildBuiltIn(Context cntx) throws JSONException {
        return new JSONObject()
                .put("*.onion", DomainRouteAction.TOR.toCatalogValue());
    }
}
