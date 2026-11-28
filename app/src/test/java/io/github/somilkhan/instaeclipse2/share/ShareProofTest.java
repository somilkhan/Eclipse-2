package io.github.somilkhan.instaeclipse2.share;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ShareProofTest {
    private static Intent link() {
        return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/p/ABC/"));
    }

    @Test
    public void companionProofIsAccepted() {
        Context ctx = ApplicationProvider.getApplicationContext();
        assertTrue(ShareProof.isFromCompanion(ShareProof.attach(ctx, link())));
    }

    @Test
    public void oldBooleanExtraIsRejected() {
        Intent forged = link().putExtra("io.github.somilkhan.instaeclipse2.SHARE_DOWNLOAD", true)
                .putExtra(ShareProof.EXTRA, true);
        assertFalse(ShareProof.isFromCompanion(forged));
        assertFalse(ShareProof.isFromCompanion(link()));
        assertFalse(ShareProof.isFromCompanion(null));
    }

    @Test
    public void pendingIntentFromAnotherAppIsRejected() {
        Context ctx = ApplicationProvider.getApplicationContext();
        PendingIntent other = PendingIntent.getBroadcast(ctx, 1, new Intent("x"), PendingIntent.FLAG_IMMUTABLE);
        Shadows.shadowOf(other).setCreatorPackage("com.example.attacker");
        assertFalse(ShareProof.isFromCompanion(link().putExtra(ShareProof.EXTRA, other)));
    }
}
