package io.github.somilkhan.instaeclipse2.utils.toast;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.github.somilkhan.instaeclipse2.R;
import io.github.somilkhan.instaeclipse2.utils.feature.FeatureStatusTracker;
import io.github.somilkhan.instaeclipse2.utils.i18n.I18n;
import io.github.somilkhan.instaeclipse2.utils.log.ModuleLog;

/** Compact startup feedback; full per-feature health is shown only when requested. */
public final class CustomToast {
    public static boolean toastShown = false;
    private static final String HEALTH_BANNER_TAG =
            "io.github.somilkhan.instaeclipse2.feature_health_banner";

    private CustomToast() {}

    public static void showCustomToast(Context context, String message) {
        if (context == null) {
            ModuleLog.line("❌ CustomToast: Context is null!");
            return;
        }
        IeToast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showFeatureSummary(Activity activity, String title,
                                         Map<String, FeatureStatusTracker.HealthState> status) {
        if (!isUsable(activity) || status == null || status.isEmpty()) return;
        try {
            activity.runOnUiThread(() -> showFeatureSummaryOnUi(activity, title, status));
        } catch (Throwable t) {
            ModuleLog.line("(IE|Toast) health summary dispatch failed: " + t);
            showFallback(activity, title);
        }
    }

    private static void showFeatureSummaryOnUi(Activity activity, String title,
                                               Map<String, FeatureStatusTracker.HealthState> status) {
        if (!isUsable(activity)) return;
        String text = title;
        LinearLayout banner = null;
        FrameLayout root = null;
        try {
            int observed = count(status, FeatureStatusTracker.HealthState.RUNTIME_OBSERVED);
            int installed = count(status, FeatureStatusTracker.HealthState.INSTALLED);
            int pending = count(status, FeatureStatusTracker.HealthState.NOT_EXERCISED);
            int failed = count(status, FeatureStatusTracker.HealthState.FAILED);
            String summary = I18n.t(activity, R.string.menu_health_summary,
                    observed, installed, pending, failed);
            text = title + " · " + summary;
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof FrameLayout) || !content.isAttachedToWindow()) {
                showFallback(activity, text);
                return;
            }
            root = (FrameLayout) content;
            View previous = root.findViewWithTag(HEALTH_BANNER_TAG);
            if (previous != null && previous.getParent() instanceof ViewGroup) {
                ((ViewGroup) previous.getParent()).removeView(previous);
            }

            banner = new LinearLayout(activity);
            banner.setTag(HEALTH_BANNER_TAG);
            banner.setOrientation(LinearLayout.HORIZONTAL);
            banner.setGravity(Gravity.CENTER_VERTICAL);
            banner.setPadding(dp(activity, 12), dp(activity, 4), dp(activity, 4), dp(activity, 4));
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xFF0E0E10);
            background.setCornerRadius(dp(activity, 16));
            banner.setBackground(background);
            banner.setElevation(dp(activity, 6));

            TextView summaryView = new TextView(activity);
            summaryView.setText(text);
            summaryView.setTextColor(0xFFF5F5F7);
            summaryView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            summaryView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            summaryView.setGravity(Gravity.CENTER_VERTICAL);
            summaryView.setMaxLines(2);
            summaryView.setEllipsize(TextUtils.TruncateAt.END);
            summaryView.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            summaryParams.setMarginEnd(dp(activity, 8));
            banner.addView(summaryView, summaryParams);

            TextView details = new TextView(activity);
            String detailsLabel = I18n.t(activity, R.string.ig_toast_feature_details);
            details.setText(detailsLabel);
            details.setContentDescription(detailsLabel);
            details.setTextColor(0xFFFF5F45);
            details.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            details.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            details.setGravity(Gravity.CENTER);
            details.setPadding(dp(activity, 8), 0, dp(activity, 8), 0);
            details.setMinWidth(dp(activity, 48));
            details.setMinHeight(dp(activity, 48));
            details.setFocusable(true);
            details.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            WeakReference<Activity> host = new WeakReference<>(activity);
            LinearLayout card = banner;
            FrameLayout container = root;
            details.setOnClickListener(view -> {
                try {
                    Activity current = host.get();
                    if (!isUsable(current) || !view.isAttachedToWindow()
                            || !container.isAttachedToWindow() || card.getParent() != container) return;
                    showFeatureDetails(current, status);
                    container.removeView(card);
                } catch (Throwable t) {
                    ModuleLog.line("(IE|Toast) health details callback failed: " + t);
                }
            });
            banner.addView(details, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            params.leftMargin = dp(activity, 16);
            params.rightMargin = dp(activity, 16);
            params.bottomMargin = dp(activity, 12);
            banner.setLayoutParams(params);
            // Observe insets on our own view, never replace the host content's listener.
            banner.setOnApplyWindowInsetsListener((view, insets) -> {
                try {
                    if (view.isAttachedToWindow() && view.getParent() == container) {
                        updateBottomMargin(container, view);
                    }
                } catch (Throwable t) {
                    ModuleLog.line("(IE|Toast) health banner insets failed: " + t);
                }
                return insets;
            });
            long timeout = bannerTimeout(activity);
            Runnable dismiss = () -> {
                try {
                    if (container.isAttachedToWindow() && card.isAttachedToWindow()
                            && card.getParent() == container) {
                        container.removeView(card);
                    }
                } catch (Throwable t) {
                    ModuleLog.line("(IE|Toast) health banner dismissal failed: " + t);
                }
            };
            banner.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    try {
                        if (!isUsable(host.get()) || view.getParent() != container) {
                            if (view.getParent() == container) container.removeView(view);
                            return;
                        }
                        updateBottomMargin(container, view);
                        view.requestApplyInsets();
                        view.postDelayed(dismiss, timeout);
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Toast) health banner attachment failed: " + t);
                        try {
                            if (view.getParent() == container) container.removeView(view);
                        } catch (Throwable ignored) {}
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View view) {
                    try {
                        view.removeCallbacks(dismiss);
                        view.setOnApplyWindowInsetsListener(null);
                        details.setOnClickListener(null);
                        view.removeOnAttachStateChangeListener(this);
                    } catch (Throwable t) {
                        ModuleLog.line("(IE|Toast) health banner detachment failed: " + t);
                    }
                }
            });
            root.addView(banner);
        } catch (Throwable t) {
            ModuleLog.line("(IE|Toast) compact health summary failed: " + t);
            if (banner != null && root != null && banner.getParent() == root) {
                try { root.removeView(banner); } catch (Throwable ignored) {}
            }
            showFallback(activity, text);
        }
    }

    private static void updateBottomMargin(FrameLayout root, View banner) {
        int bottomInset = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowInsets insets = root.getRootWindowInsets();
            if (insets != null) {
                bottomInset = Math.max(insets.getSystemWindowInsetBottom(), insets.getStableInsetBottom());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    bottomInset = Math.max(bottomInset, insets.getMandatorySystemGestureInsets().bottom);
                }
            }
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) banner.getLayoutParams();
        int margin = dp(root.getContext(), 12) + bottomInset;
        if (params.bottomMargin != margin) {
            params.bottomMargin = margin;
            banner.setLayoutParams(params);
        }
    }

    private static long bannerTimeout(Context context) {
        int timeout = 6000;
        try {
            AccessibilityManager manager = (AccessibilityManager)
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (manager != null) {
                if (manager.isTouchExplorationEnabled()) timeout = 12000;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    timeout = Math.max(timeout, manager.getRecommendedTimeoutMillis(timeout,
                            AccessibilityManager.FLAG_CONTENT_TEXT | AccessibilityManager.FLAG_CONTENT_CONTROLS));
                }
            }
        } catch (Throwable t) {
            ModuleLog.line("(IE|Toast) accessibility timeout unavailable: " + t);
        }
        return timeout;
    }

    private static boolean isUsable(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void showFallback(Activity activity, String text) {
        if (!isUsable(activity)) return;
        try {
            IeToast.makeText(activity, text, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            ModuleLog.line("(IE|Toast) health summary fallback failed: " + t);
        }
    }

    private static int count(Map<String, FeatureStatusTracker.HealthState> status,
                             FeatureStatusTracker.HealthState target) {
        int count = 0;
        for (FeatureStatusTracker.HealthState state : status.values()) {
            if (state == target) count++;
        }
        return count;
    }

    private static void showFeatureDetails(Activity activity,
                                           Map<String, FeatureStatusTracker.HealthState> status) {
        if (!isUsable(activity)) return;
        final Dialog[] dialog = {null};
        try {
            List<String> keys = new ArrayList<>(status.keySet());
            Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
            int observed = count(status, FeatureStatusTracker.HealthState.RUNTIME_OBSERVED);
            int installed = count(status, FeatureStatusTracker.HealthState.INSTALLED);
            int pending = count(status, FeatureStatusTracker.HealthState.NOT_EXERCISED);
            int failed = count(status, FeatureStatusTracker.HealthState.FAILED);

            LinearLayout sheet = new LinearLayout(activity);
            sheet.setTag("io.github.somilkhan.instaeclipse2.feature_health_sheet");
            sheet.setOrientation(LinearLayout.VERTICAL);
            sheet.setPadding(dp(activity, 20), dp(activity, 10), dp(activity, 20), dp(activity, 18));
            sheet.setBackground(roundRect(0xFF0E0E10, dp(activity, 26), 0x1F8E8E96, dp(activity, 1)));

            View handle = new View(activity);
            handle.setBackground(roundRect(0xFF5C5C66, dp(activity, 99), Color.TRANSPARENT, 0));
            LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(activity, 36), dp(activity, 4));
            handleParams.gravity = Gravity.CENTER_HORIZONTAL;
            handleParams.bottomMargin = dp(activity, 14);
            sheet.addView(handle, handleParams);

            LinearLayout heading = new LinearLayout(activity);
            heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = text(activity, I18n.t(activity, R.string.menu_health_title), 22,
                    0xFFF5F5F7, true);
            title.setId(android.R.id.title);
            heading.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView close = text(activity, "×", 30, 0xFF8E8E96, false);
            close.setGravity(Gravity.CENTER);
            close.setContentDescription(I18n.t(activity, R.string.menu_health_sheet_close));
            close.setMinWidth(dp(activity, 48));
            close.setMinHeight(dp(activity, 48));
            close.setFocusable(true);
            close.setOnClickListener(v -> { if (dialog[0] != null) dialog[0].dismiss(); });
            heading.addView(close);
            sheet.addView(heading);

            sheet.addView(text(activity,
                    I18n.t(activity, R.string.menu_health_sheet_intro, keys.size()),
                    13, 0xFF8E8E96, false));

            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(false);
            scroll.setClipToPadding(false);
            LinearLayout body = new LinearLayout(activity);
            body.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dp(activity, 18);
            scroll.addView(body, bodyParams);

            LinearLayout counts = new LinearLayout(activity);
            counts.setOrientation(LinearLayout.VERTICAL);
            counts.addView(summaryRow(activity,
                    new String[]{I18n.t(activity, R.string.menu_health_short_runtime), Integer.toString(observed),
                            I18n.t(activity, R.string.menu_health_short_installed), Integer.toString(installed)},
                    new int[]{0xFF4CC38A, 0xFF8E8E96}));
            LinearLayout.LayoutParams secondRow = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            secondRow.topMargin = dp(activity, 8);
            counts.addView(summaryRow(activity,
                    new String[]{I18n.t(activity, R.string.menu_health_short_unverified), Integer.toString(pending),
                            I18n.t(activity, R.string.menu_health_short_failed), Integer.toString(failed)},
                    new int[]{0xFFE5A84B, 0xFFFF6B60}), secondRow);
            body.addView(counts);

            TextView explanation = text(activity, I18n.t(activity, R.string.menu_health_note),
                    12, 0xFF8E8E96, false);
            explanation.setPadding(dp(activity, 2), dp(activity, 14), dp(activity, 2), dp(activity, 16));
            body.addView(explanation);

            LinearLayout rows = new LinearLayout(activity);
            rows.setOrientation(LinearLayout.VERTICAL);
            rows.setBackground(roundRect(0xFF000000, dp(activity, 18), 0x1F8E8E96, dp(activity, 1)));
            int index = 0;
            for (String key : keys) {
                FeatureStatusTracker.HealthState state = status.get(key);
                if (index++ > 0) {
                    View divider = new View(activity);
                    divider.setBackgroundColor(0x1FFFFFFF);
                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 1));
                    dividerParams.setMargins(dp(activity, 14), 0, dp(activity, 14), 0);
                    rows.addView(divider, dividerParams);
                }
                rows.addView(healthRow(activity, key, state));
            }
            body.addView(rows);
            sheet.addView(scroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

            TextView done = text(activity, I18n.t(activity, R.string.menu_health_sheet_done), 14,
                    0xFF000000, true);
            done.setGravity(Gravity.CENTER);
            done.setMinHeight(dp(activity, 48));
            done.setBackground(roundRect(0xFFFF5F45, dp(activity, 999), Color.TRANSPARENT, 0));
            done.setOnClickListener(v -> { if (dialog[0] != null) dialog[0].dismiss(); });
            LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            doneParams.topMargin = dp(activity, 14);
            sheet.addView(done, doneParams);

            dialog[0] = new Dialog(activity, android.R.style.Theme_Material_NoActionBar);
            dialog[0].setContentView(sheet);
            dialog[0].setCancelable(true);
            dialog[0].setCanceledOnTouchOutside(true);
            android.view.Window window = dialog[0].getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.84f);
                params.gravity = Gravity.BOTTOM;
                params.dimAmount = 0.58f;
                window.setAttributes(params);
                window.setNavigationBarColor(0xFF000000);
            }
            dialog[0].show();
            if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    Math.round(activity.getResources().getDisplayMetrics().heightPixels * 0.84f));
        } catch (Throwable t) {
            if (dialog[0] != null && dialog[0].isShowing()) dialog[0].dismiss();
            ModuleLog.line("(IE|Toast) health details failed: " + t);
        }
    }

    private static LinearLayout summaryRow(Context context, String[] items, int[] colors) {
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(summaryTile(context, items[0], items[1], colors[0]),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        right.setMarginStart(dp(context, 8));
        row.addView(summaryTile(context, items[2], items[3], colors[1]), right);
        return row;
    }

    private static View summaryTile(Context context, String label, String count, int accent) {
        LinearLayout tile = new LinearLayout(context);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(dp(context, 12), dp(context, 10), dp(context, 8), dp(context, 10));
        tile.setBackground(roundRect(0xFF1A1A1E, dp(context, 14), 0x17FFFFFF, dp(context, 1)));
        View dot = new View(context);
        dot.setBackground(roundRect(accent, dp(context, 99), Color.TRANSPARENT, 0));
        tile.addView(dot, new LinearLayout.LayoutParams(dp(context, 7), dp(context, 7)));
        TextView text = text(context, label, 11, 0xFFB8B8C0, true);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(dp(context, 7));
        tile.addView(text, textParams);
        TextView value = text(context, count, 16, accent, true);
        tile.addView(value);
        return tile;
    }

    private static View healthRow(Context context, String key, FeatureStatusTracker.HealthState state) {
        int color = stateColor(state);
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(context, 14), dp(context, 11), dp(context, 14), dp(context, 11));
        View dot = new View(context);
        dot.setBackground(roundRect(color, dp(context, 99), Color.TRANSPARENT, 0));
        row.addView(dot, new LinearLayout.LayoutParams(dp(context, 8), dp(context, 8)));
        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        textParams.setMarginStart(dp(context, 12));
        row.addView(textColumn, textParams);
        TextView name = text(context, FeatureStatusTracker.getLabel(context, key), 14, 0xFFF5F5F7, true);
        textColumn.addView(name);
        TextView stateText = text(context, stateLabel(context, state), 12, color, false);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stateParams.topMargin = dp(context, 3);
        textColumn.addView(stateText, stateParams);
        if (state == FeatureStatusTracker.HealthState.FAILED) {
            TextView reason = text(context, FeatureStatusTracker.getFailureReason(key), 12, 0xFF8E8E96, false);
            reason.setMaxLines(3);
            reason.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams reasonParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            reasonParams.topMargin = dp(context, 3);
            textColumn.addView(reason, reasonParams);
        }
        return row;
    }

    private static int stateColor(FeatureStatusTracker.HealthState state) {
        if (state == FeatureStatusTracker.HealthState.RUNTIME_OBSERVED) return 0xFF4CC38A;
        if (state == FeatureStatusTracker.HealthState.INSTALLED) return 0xFFFF5F45;
        if (state == FeatureStatusTracker.HealthState.FAILED) return 0xFFFF6B60;
        return 0xFFE5A84B;
    }

    private static String stateLabel(Context context, FeatureStatusTracker.HealthState state) {
        int res = state == FeatureStatusTracker.HealthState.RUNTIME_OBSERVED
                ? R.string.menu_health_state_runtime
                : state == FeatureStatusTracker.HealthState.INSTALLED
                ? R.string.menu_health_state_installed
                : state == FeatureStatusTracker.HealthState.FAILED
                ? R.string.menu_health_state_failed : R.string.menu_health_state_unverified;
        return I18n.t(context, res);
    }

    private static TextView text(Context context, String value, int sp, int color, boolean medium) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return view;
    }

    private static GradientDrawable roundRect(int color, float radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) drawable.setStroke(strokeWidth, strokeColor);
        return drawable;
    }
}
