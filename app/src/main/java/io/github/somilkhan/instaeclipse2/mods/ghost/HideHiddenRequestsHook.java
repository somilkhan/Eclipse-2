package io.github.somilkhan.instaeclipse2.mods.ghost;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.somilkhan.instaeclipse2.utils.core.JitGuard;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureFlags;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/**
 * Hides the "Hidden requests" (spam folder) row at the top of the message requests list
 * (port of Piko's "Hide hidden requests").
 *
 * <p>The row is its own layout, {@code pending_threads_spam_folder_row} (present on
 * 458.0.0.0.57), inflated into the requests RecyclerView. When it is inflated it is set to GONE
 * with zero height, since a GONE list item still keeps its slot otherwise. It only takes effect
 * for rows inflated after the switch is turned on; reopen the requests page.
 */
public class HideHiddenRequestsHook {

    private static final String TAG = "(InstaEclipse | HiddenRequests): ";
    private static final String LAYOUT = "pending_threads_spam_folder_row";
    private static volatile int layoutId = -1;

    public void install() {
        try {
            XposedHelpers.findAndHookMethod(LayoutInflater.class, "inflate",
                    int.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) { JitGuard.tick(param.method);
                            if (!FeatureFlags.hideHiddenRequests) return;
                            if (!(param.getResult() instanceof View row)) return;
                            // attachToRoot returns the parent, not the row: leave that case alone.
                            if (Boolean.TRUE.equals(param.args[2]) && param.args[1] != null) return;
                            if ((Integer) param.args[0] != layoutId(row)) return;
                            collapse(row);
                        }
                    });
            FeatureStatusTracker.setHooked("HideHiddenRequests");
            ModuleLog.line(TAG + "✅ Hooked LayoutInflater.inflate");
        } catch (Throwable t) {
            ModuleLog.line(TAG + "❌ " + t);
        }
    }

    @SuppressLint("DiscouragedApi")
    private static int layoutId(View v) {
        int id = layoutId;
        if (id != -1) return id;
        try {
            id = v.getResources().getIdentifier(LAYOUT, "layout", v.getContext().getPackageName());
        } catch (Throwable t) {
            id = 0;
        }
        if (id == 0) ModuleLog.line(TAG + "⚠️ layout " + LAYOUT + " not in this build");
        layoutId = id;
        return id;
    }

    private static void collapse(View row) {
        try {
            row.setVisibility(View.GONE);
            ViewGroup.LayoutParams lp = row.getLayoutParams();
            if (lp != null) {
                lp.height = 0;
                row.setLayoutParams(lp);
            }
        } catch (Throwable t) {
            ModuleLog.line(TAG + "⚠️ collapse: " + t);
        }
    }
}
