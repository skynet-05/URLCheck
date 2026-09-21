package com.trianguloy.urlchecker.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DomainRoutingEngineTest {

    @Test
    public void onionWildcardMatches() {
        assertTrue(DomainRoutingEngine.matchScore("*.onion", "abc.onion") > 0);
        assertEquals(-1, DomainRoutingEngine.matchScore("*.onion", "example.com"));
    }
}
