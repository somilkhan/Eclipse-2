package io.github.somilkhan.instaeclipse2.utils.ipc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Re-signed com.instagram.android (LSPatch / unclone) must be pinnable; random packages never. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CloneTrustHostTest {
    @Test
    public void reSignedOfficialPackageIsPinnable() {
        Context ctx = ApplicationProvider.getApplicationContext();
        // Not installed / not Meta-signed in the test VM, so it must go through user approval.
        assertFalse(CompanionBridge.isOfficialHostSigner(ctx));
        assertTrue(CloneTrust.isPinnable(ctx, "com.instagram.android"));
    }

    @Test
    public void unsupportedPackagesAreNeverPinnable() {
        Context ctx = ApplicationProvider.getApplicationContext();
        assertFalse(CloneTrust.isPinnable(ctx, "com.example.attacker"));
        assertFalse(CloneTrust.isPinnable(ctx, null));
    }

    @Test
    public void untrustedHostIsNotTrusted() {
        Context ctx = ApplicationProvider.getApplicationContext();
        assertFalse(CompanionBridge.isTrustedHost(ctx, "com.instagram.android"));
    }
}
