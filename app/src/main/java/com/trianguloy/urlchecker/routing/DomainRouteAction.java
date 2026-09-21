package com.trianguloy.urlchecker.routing;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.Enums;

/** How to open a link for a matching host pattern. */
public enum DomainRouteAction implements Enums.IdEnum, Enums.StringEnum {
    TOR(0, R.string.domain_routing_tor),
    CLEARNET(1, R.string.domain_routing_clearnet),
    ASK(2, R.string.domain_routing_ask);

    private final int id;
    private final int string;

    DomainRouteAction(int id, int string) {
        this.id = id;
        this.string = string;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getStringResource() {
        return string;
    }

    public static DomainRouteAction fromString(String s) {
        if (s == null) return ASK;
        return switch (s.toLowerCase()) {
            case "tor", "tor_preview" -> TOR;
            case "clearnet", "browser", "open" -> CLEARNET;
            default -> ASK;
        };
    }

    public String toCatalogValue() {
        return name().toLowerCase();
    }
}
