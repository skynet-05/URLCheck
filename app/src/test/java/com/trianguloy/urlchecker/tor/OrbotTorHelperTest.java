package com.trianguloy.urlchecker.tor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/** Orbot integration constants (STATUS broadcast contract). */
public class OrbotTorHelperTest {

    @Test
    public void orbotPackageAndActionsAreStable() {
        assertEquals("org.torproject.android", OrbotTorHelper.ORBOT_PACKAGE);
        assertEquals("org.torproject.android.intent.action.STATUS", OrbotTorHelper.ACTION_STATUS);
        assertEquals("org.torproject.android.intent.action.START", OrbotTorHelper.ACTION_START);
        assertNotNull(OrbotTorHelper.STATUS_ON);
        assertEquals(8118, OrbotTorHelper.DEFAULT_HTTP_PROXY_PORT);
    }
}
