package ps.reso.instaeclipse.utils.pacing;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import ps.reso.instaeclipse.R;
import ps.reso.instaeclipse.utils.dialog.IeDialog;
import ps.reso.instaeclipse.utils.feature.FeatureFlags;
import ps.reso.instaeclipse.utils.i18n.I18n;

/**
 * A paced bulk run inside Instagram: a {@link SafePacer} (enabled by the "Safe pacing" setting)
 * plus a small progress dialog with Stop (cancels after the current item) and Hide (keeps going).
 *
 * <pre>
 * PacedRun run = PacedRun.start(ctx, title, items.size());   // any thread
 * for (int i = 0; i &lt; items.size(); i++) {                    // background thread
 *     if (!run.awaitTurn(i)) break;                            // waits; false once stopped
 *     ...do item i...
 *     run.done(i + 1);
 * }
 * run.finish();
 * </pre>
 */
public final class PacedRun {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public final SafePacer pacer;
    private final Context ctx;
    private final int total;
    private volatile IeDialog dialog;
    private volatile boolean finished;

    private PacedRun(Context ctx, int total, SafePacer pacer) {
        this.ctx = ctx;
        this.total = total;
        this.pacer = pacer;
    }

    public static PacedRun start(Context ctx, CharSequence title, int total) {
        PacedRun run = new PacedRun(ctx, total, SafePacer.create(FeatureFlags.safePacing));
        if (ctx != null && total > 1) run.onMain(() -> run.show(title));
        return run;
    }

    private void show(CharSequence title) {
        if (finished) return;
        try {
            IeDialog d = new IeDialog(ctx).title(title).message(progressText(0));
            d.secondary(I18n.t(ctx, R.string.ie_pacing_stop), pacer::cancel);
            d.primary(I18n.t(ctx, R.string.ie_pacing_hide), null);
            d.cancelable(true);
            d.show();
            dialog = d;
        } catch (Throwable ignored) {
            // No window (e.g. activity gone): the run still works, just without a dialog.
        }
    }

    private String progressText(int done) {
        return I18n.t(ctx, R.string.ie_pacing_progress, done, total);
    }

    /** Background thread: waits the paced delay before item {@code index}; false if stopped. */
    public boolean awaitTurn(int index) {
        return pacer.awaitTurn(index);
    }

    public boolean isCancelled() {
        return pacer.isCancelled();
    }

    /** Reports progress ({@code done} items finished). */
    public void done(int done) {
        onMain(() -> {
            IeDialog d = dialog;
            if (d != null && d.isShowing()) d.message(progressText(done));
        });
    }

    /** Closes the dialog; safe to call from any thread, more than once. */
    public void finish() {
        finished = true;
        onMain(() -> {
            IeDialog d = dialog;
            if (d != null) d.dismiss();
            dialog = null;
        });
    }

    /** "Stopped after 3 of 12" when the user stopped the run, else null. */
    public String stoppedText(int done) {
        return pacer.isCancelled() && ctx != null ? I18n.t(ctx, R.string.ie_pacing_stopped, done, total) : null;
    }

    private static void onMain(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) r.run();
        else MAIN.post(r);
    }
}
