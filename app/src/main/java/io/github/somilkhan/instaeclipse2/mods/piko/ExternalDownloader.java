package io.github.somilkhan.instaeclipse2.mods.piko;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.core.IgModels;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.i18n.I18n;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;
import io.github.somilkhan.instaeclipse2.utils.toast.IeToast;

/**
 * "Open in external downloader" (Piko's "External downloader"): sends the post's permalink to
 * another app as plain text, the way Instagram's own share sheet would. A configured package gets
 * it directly; otherwise (or if that app is gone) the system chooser opens.
 *
 * <p>The menu row is a carrier MediaOption handled by PostDownloadContextMenuHook, like Copy image
 * / Media info / Playback speed.
 */
public final class ExternalDownloader {

    /** Debug-only MediaOption values that never show for normal users; first one present wins. */
    public static final String[] CARRIERS = {"SHOP_LANE_DEBUG", "DEBUG_AD"};

    private ExternalDownloader() {}

    public static String link(Object media) {
        String code = IgModels.readString(media, "code");
        String type = IgModels.readString(media, "product_type");
        // A carousel slide has no code of its own and its pk is not a post; link the parent.
        String id = code == null ? IgModels.readString(media, "carousel_parent_id") : null;
        if (id == null) id = IgModels.readString(media, "pk");
        if (id == null) id = IgModels.readString(media, "id");
        return PostLinks.permalink(code, type, id);
    }

    public static void open(Context ctx, Object media) {
        String link = link(media);
        if (link == null) {
            IeToast.makeText(ctx, I18n.t(ctx, R.string.ig_external_downloader_no_link), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link);
        String pkg = FeatureFlags.externalDownloaderPackage == null ? "" : FeatureFlags.externalDownloaderPackage.trim();
        if (!pkg.isEmpty()) {
            try {
                ctx.startActivity(new Intent(send).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            } catch (ActivityNotFoundException e) {
                ModuleLog.line("(InstaEclipse | ExternalDownloader): ⚠️ " + pkg + " can't receive links, using the chooser");
            } catch (Throwable t) {
                ModuleLog.line("(InstaEclipse | ExternalDownloader): ⚠️ " + t);
            }
        }
        try {
            Intent chooser = Intent.createChooser(send, I18n.t(ctx, R.string.ig_external_downloader_title));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(chooser);
        } catch (Throwable t) {
            ModuleLog.line("(InstaEclipse | ExternalDownloader): ❌ " + t);
        }
    }
}
