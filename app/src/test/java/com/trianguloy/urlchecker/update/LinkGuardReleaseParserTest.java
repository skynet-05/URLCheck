package com.trianguloy.urlchecker.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LinkGuardReleaseParserTest {

    @Test
    public void onlyLinkguardSemverTagsQualify() {
        assertTrue(LinkGuardReleaseParser.isOtaTag("linkguard-v3.5.4"));
        assertFalse(LinkGuardReleaseParser.isOtaTag("whitelabel-tor-linkcheck"));
        assertFalse(LinkGuardReleaseParser.isOtaTag("tor-sandbox-preview-status-icon"));
        assertFalse(LinkGuardReleaseParser.isOtaTag("linkguard-v"));
    }

    @Test
    public void versionNameFromTag() {
        assertEquals("3.5.4", LinkGuardReleaseParser.versionNameFromTag("linkguard-v3.5.4"));
        assertNull(LinkGuardReleaseParser.versionNameFromTag("whitelabel-tor-linkcheck"));
    }

    @Test
    public void versionCodeFromBodyRequired() {
        assertEquals(51, LinkGuardReleaseParser.versionCodeFromBody("versionCode: 51\nversionName: 3.5.4"));
        assertEquals(-1, LinkGuardReleaseParser.versionCodeFromBody("no code here"));
    }
}
