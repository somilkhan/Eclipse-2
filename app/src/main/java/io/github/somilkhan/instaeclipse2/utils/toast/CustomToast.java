package io.github.somilkhan.instaeclipse2.utils.toast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
        try {
            List<String> keys = new ArrayList<>(status.keySet());
            Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
            StringBuilder message = new StringBuilder();
            for (String key : keys) {
                FeatureStatusTracker.HealthState state = status.get(key);
                message.append("• ").append(FeatureStatusTracker.getLabel(activity, key))
                        .append(" — ").append(stateLabel(activity, state));
                if (state == FeatureStatusTracker.HealthState.FAILED) {
                    message.append("\n  ").append(FeatureStatusTracker.getFailureReason(key));
                }
                message.append('\n');
            }
            // Explicit framework theme: Instagram need not expose AppCompat/Material attributes.
            new AlertDialog.Builder(activity, android.R.style.Theme_Material_Dialog_Alert)
                    .setTitle(I18n.t(activity, R.string.menu_health_title))
                    .setMessage(message.toString().trim())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Throwable t) {
            ModuleLog.line("(IE|Toast) health details failed: " + t);
        }
    }

    private static String stateLabel(Context context, FeatureStatusTracker.HealthState state) {
        int res;
        if (state == FeatureStatusTracker.HealthState.RUNTIME_OBSERVED) {
            res = R.string.menu_health_state_runtime;
        } else if (state == FeatureStatusTracker.HealthState.INSTALLED) {
            res = R.string.menu_health_state_installed;
        } else if (state == FeatureStatusTracker.HealthState.FAILED) {
            res = R.string.menu_health_state_failed;
        } else {
            res = R.string.menu_health_state_unverified;
        }
        return I18n.t(context, res);
    }
}
