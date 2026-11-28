package io.github.somilkhan.instaeclipse2.utils.download;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;

/**
 * Persistable SAF folder grants for the folder pickers.
 *
 * <p>{@code takePersistableUriPermission} throws when a document provider returns a tree without a
 * persistable grant, and a grant that is never released stays forever (Android caps them per app
 * and then evicts the oldest silently). Pickers take the new grant here and release the folder
 * they replace unless another setting still uses it.
 */
public final class SafGrants {
    public static final int FLAGS = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

    private SafGrants() { }

    /** False (and nothing taken) when the provider's grant can't be persisted. */
    public static boolean take(ContentResolver cr, Uri uri) {
        try {
            cr.takePersistableUriPermission(uri, FLAGS);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /** Releases {@code oldUri} after it was replaced by {@code newUri}, unless still in use. */
    public static void releaseReplaced(ContentResolver cr, String oldUri, String newUri, String... stillUsed) {
        if (!shouldRelease(oldUri, newUri, stillUsed)) return;
        try {
            cr.releasePersistableUriPermission(Uri.parse(oldUri), FLAGS);
        } catch (SecurityException | IllegalArgumentException ignored) {
            // Already gone (e.g. revoked by the user); nothing to release.
        }
    }

    static boolean shouldRelease(String oldUri, String newUri, String... stillUsed) {
        if (oldUri == null || oldUri.isEmpty() || oldUri.equals(newUri)) return false;
        if (stillUsed != null) for (String u : stillUsed) if (oldUri.equals(u)) return false;
        return true;
    }
}
