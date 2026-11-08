package com.trianguloy.urlchecker.modules.companions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class BravePrivateTabHelperTest {

    @Test
    public void recognizesBravePackageVariants() {
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE));
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE_BETA));
        assertTrue(BravePrivateTabHelper.isBravePackage(BravePrivateTabHelper.PKG_BRAVE_NIGHTLY));
        assertFalse(BravePrivateTabHelper.isBravePackage("com.android.chrome"));
    }

    @Test
    public void openPrivateTabIntentUsesChromiumActionAndComponent() {
        Intent intent = BravePrivateTabHelper.buildOpenPrivateTabIntent(BravePrivateTabHelper.PKG_BRAVE);
        assertEquals(BravePrivateTabHelper.ACTION_OPEN_PRIVATE_TAB, intent.getAction());
        assertEquals(new ComponentName(BravePrivateTabHelper.PKG_BRAVE,
                BravePrivateTabHelper.INCOGNITO_TAB_LAUNCHER), intent.getComponent());
        assertNull(intent.getData());
        assertTrue(intent.getExtras() == null || intent.getExtras().isEmpty());
    }

    @Test
    public void openPrivateTabIntentNeverCarriesUrl() {
        Intent intent = BravePrivateTabHelper.buildOpenPrivateTabIntent(BravePrivateTabHelper.PKG_BRAVE_BETA);
        assertEquals(BravePrivateTabHelper.PKG_BRAVE_BETA, intent.getComponent().getPackageName());
        assertNull(intent.getDataString());
    }
}
