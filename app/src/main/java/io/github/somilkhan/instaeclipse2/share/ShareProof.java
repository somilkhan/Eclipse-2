package io.github.somilkhan.instaeclipse2.share;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;

import io.github.somilkhan.instaeclipse2.utils.core.CommonUtils;

/**
 * Proves that a "download this shared link" intent really comes from the companion.
 *
 * <p>A plain boolean extra can be set by any app, which let any installed app make Instagram fetch
 * and save a post under the user's session. Instead the companion attaches an immutable
 * PendingIntent it created; Android reports a PendingIntent's creator from the system side, so
 * another app can't forge one that names the companion.
 */
public final class ShareProof {
    public static final String EXTRA = "io.github.somilkhan.instaeclipse2.SHARE_DOWNLOAD_PROOF";
    private static final String ACTION = "io.github.somilkhan.instaeclipse2.SHARE_DOWNLOAD_PROOF";

    private ShareProof() { }

    /** Companion side: adds the proof to an intent sent to Instagram. */
    public static Intent attach(Context companion, Intent toInstagram) {
        Intent marker = new Intent(ACTION).setPackage(companion.getPackageName());
        PendingIntent proof = PendingIntent.getBroadcast(companion, 0, marker,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return toInstagram.putExtra(EXTRA, proof);
    }

    /** Instagram side: true only when the intent carries a proof created by the companion. */
    public static boolean isFromCompanion(Intent intent) {
        if (intent == null) return false;
        Parcelable p;
        try {
            p = intent.getParcelableExtra(EXTRA);
        } catch (RuntimeException badParcel) {
            return false;
        }
        if (!(p instanceof PendingIntent)) return false;
        return CommonUtils.MY_PACKAGE_NAME.equals(((PendingIntent) p).getCreatorPackage());
    }
}
