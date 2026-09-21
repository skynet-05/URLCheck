package com.trianguloy.urlchecker.update;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UpdateVersionLogicTest {

    @Test
    public void otaRepoPointsAtUserFork() {
        assertEquals("skynet-05", LinkGuardUpdateConfig.GITHUB_OWNER);
        assertEquals("URLCheck", LinkGuardUpdateConfig.GITHUB_REPO);
        assertTrue(LinkGuardUpdateConfig.TAG_PREFIX.startsWith("linkguard-"));
    }
}
