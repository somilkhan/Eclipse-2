package io.github.somilkhan.instaeclipse2;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.somilkhan.instaeclipse2.fragments.FeaturesFragment;
import io.github.somilkhan.instaeclipse2.fragments.HelpFragment;
import io.github.somilkhan.instaeclipse2.fragments.HomeFragment;
import io.github.somilkhan.instaeclipse2.fragments.LoggingFragment;
import io.github.somilkhan.instaeclipse2.ui.glass.GlassNavPill;
import io.github.somilkhan.instaeclipse2.ui.glass.Motion;
import io.github.somilkhan.instaeclipse2.utils.log.Logging;
import io.github.somilkhan.instaeclipse2.utils.version.VersionCheckUtility;

/**
 * Shell: edge-to-edge window, fragments scroll underneath a frosted compact top bar and a floating
 * glass nav pill. Fragments opt into inset handling with view tags (no fragment code needed):
 * <ul>
 *   <li>{@code ie_scroll} – the main scroller: top padding = status bar, bottom = above the pill;
 *       drives the compact-title fade and pill hide-on-scroll.</li>
 *   <li>{@code ie_large_title} – the large title inside it (its text becomes the compact title).</li>
 *   <li>{@code ie_scroll_list} – a scroller below a fixed header: bottom padding + tab bar minimising.</li>
 *   <li>{@code ie_inset_top} / {@code ie_inset_bottom} – pad only that edge.</li>
 *   <li>{@code ie_inset_bottom_margin} – bottom margin so a fixed panel ends above the pill.</li>
 *   <li>{@code ie_float_above_nav} – bottom margin so a FAB floats above the pill.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    private GlassNavPill nav;
    private View topBar;
    private io.github.somilkhan.instaeclipse2.ui.glass.ScrollEdgeEffect edge;
    private TextView topTitle;
    private Insets bars = Insets.NONE;
    private View currentRoot;
    private final io.github.somilkhan.instaeclipse2.ui.glass.TabReveal tabReveal =
            new io.github.somilkhan.instaeclipse2.ui.glass.TabReveal();
    private final io.github.somilkhan.instaeclipse2.ui.glass.CompanionEntrance entrance =
            new io.github.somilkhan.instaeclipse2.ui.glass.CompanionEntrance();
    private boolean coldStart;
    private boolean entranceStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // HyperSpring's current companion uses a fixed OLED palette, not wallpaper Monet.
        // Local mode only: never change the Instagram process's theme/night configuration.
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        super.onCreate(savedInstanceState);
        coldStart = savedInstanceState == null;
        Logging.init(this, "instaeclipse_companion.log");
        io.github.somilkhan.instaeclipse2.mods.media.FrameSessionCache.cleanupOrphans(getCacheDir());
        // Only on a fresh launch: recreation (rotation, dark-mode/locale change, process-death
        // restore) must not re-run the network check and stack another dialog.
        if (savedInstanceState == null) VersionCheckUtility.checkForUpdates(this);
        if (savedInstanceState == null) warnIfLegacyModuleInstalled();

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        nav = findViewById(R.id.bottom_navigation);
        topBar = findViewById(R.id.top_app_bar);
        topTitle = findViewById(R.id.toolbar_title_name);
        nav.setBlurSource(fragmentContainer);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        edge = io.github.somilkhan.instaeclipse2.ui.glass.ScrollEdgeEffect.install(
                findViewById(R.id.content_stage), fragmentContainer);
        edge.setProgress(0f);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            applyChromeInsets();
            if (currentRoot != null) applyContentInsets(currentRoot);
            return insets;
        });
        nav.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b - t != ob - ot && currentRoot != null) applyContentInsets(currentRoot);
        });

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm, @NonNull Fragment f,
                                                      @NonNull View view, Bundle s) {
                        // Only the visible tab owns the chrome. Pre-warmed / hidden tabs are wired
                        // when they are shown (showTab).
                        if (f.getId() == R.id.fragment_container && !f.isHidden()
                                && tagFor(currentTab).equals(f.getTag())) {
                            onContentView(view);
                        }
                    }
                }, false);

        if (savedInstanceState != null) {
            currentTab = savedInstanceState.getInt(STATE_TAB, R.id.nav_home);
        }

        // Load the HomeFragment by default
        if (savedInstanceState == null) {
            showTab(R.id.nav_home, false);
            // Inflate the other tabs (hidden, paused) once the first frame is on screen, so the
            // first tap on a tab never pays for inflating a 2000-line screen mid-animation.
            if (prewarmTabs) nav.postDelayed(this::prewarmNextTab, PREWARM_DELAY_MS);
        }

        // Select Home only on a fresh launch. After recreation the pill restores its own
        // selection (saved state), matching the fragment the FragmentManager restored.
        // Re-tapping the current tab is a no-op inside the pill (keeps scroll/search state).
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_home);

        nav.setOnItemSelectedListener(id -> {
            if (getSupportFragmentManager().isStateSaved()) {
                nav.setSelectedItemId(currentTab);
                return;
            }
            showTab(id, true);
        });
    }

    // ------------------------------------------------------------------ legacy module

    /** Package of the original InstaEclipse; older forks of this project used it too. */
    static final String LEGACY_PACKAGE = "ps.reso.instaeclipse";

    /**
     * Both modules hook the same Instagram methods when both are enabled in LSPosed: features
     * run twice, cold start pays for two DexKit scans, and the old hooks' failures fill the log.
     */
    private void warnIfLegacyModuleInstalled() {
        try {
            getPackageManager().getPackageInfo(LEGACY_PACKAGE, 0);
        } catch (Throwable notInstalled) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.legacy_module_title)
                .setMessage(R.string.legacy_module_message)
                .setPositiveButton(R.string.legacy_module_uninstall, (d, w) -> {
                    try {
                        startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                android.net.Uri.parse("package:" + LEGACY_PACKAGE)));
                    } catch (Throwable ignored) {
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ------------------------------------------------------------------ tabs

    /** Tests switch this off so screenshots only contain the selected tab. */
    public static boolean prewarmTabs = true;
    /** Test seam for settled screenshot suites; production keeps the HyperSpring entrance enabled. */
    public static boolean startupEntranceEnabled = true;
    private static final long PREWARM_DELAY_MS = 700;
    private static final String STATE_TAB = "ie_current_tab";
    private static final int[] TABS = {R.id.nav_home, R.id.nav_features, R.id.nav_logs, R.id.nav_help};
    private int currentTab = R.id.nav_home;

    private static String tagFor(int navId) {
        return "ie_tab_" + navId;
    }

    @androidx.annotation.Nullable
    private static Fragment newTab(int navId) {
        if (navId == R.id.nav_home) return new HomeFragment();
        if (navId == R.id.nav_features) return new FeaturesFragment();
        if (navId == R.id.nav_logs) return new LoggingFragment();
        if (navId == R.id.nav_help) return new HelpFragment();
        return null;
    }

    /**
     * Tabs are created once and then shown / hidden (like a UITabBarController), instead of being
     * re-inflated on every tap. Hidden tabs are capped at STARTED, so their onPause stops polling,
     * auto-scroll and IPC, and onResume refreshes them when they come back.
     */
    private void showTab(int navId, boolean animate) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        tabReveal.finish();
        String tag = tagFor(navId);
        Fragment target = fm.findFragmentByTag(tag);
        boolean created = target == null;
        if (created) {
            target = newTab(navId);
            if (target == null) return;
        }
        boolean wave = animate && navId != currentTab && !Motion.reduceMotion()
                && currentRoot != null && currentRoot.getWidth() > 0;
        android.widget.ImageView snapshot = wave ? snapshotPage(currentRoot) : null;
        currentTab = navId;
        androidx.fragment.app.FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        for (Fragment f : fm.getFragments()) {
            if (f == target || f.getId() != R.id.fragment_container) continue;
            if (!f.isHidden()) tx.hide(f);
            tx.setMaxLifecycle(f, androidx.lifecycle.Lifecycle.State.STARTED);
        }
        if (created) tx.add(R.id.fragment_container, target, tag);
        else tx.show(target);
        tx.setMaxLifecycle(target, androidx.lifecycle.Lifecycle.State.RESUMED);
        tx.commitNow();
        View v = target.getView();
        if (v == null) return; // first launch: the lifecycle callback wires it once the view exists
        if (!created) onContentView(v);
        v.animate().cancel();
        v.setAlpha(1f);
        v.setTranslationY(0f);
        ViewGroup container = findViewById(R.id.fragment_container);
        if (snapshot != null && !v.isLaidOut() && container.getWidth() > 0 && container.getHeight() > 0) {
            // A first tap may beat prewarming. Lay out the new match-parent page before its first
            // frame so it enters through the same wave as an already retained page.
            v.measure(View.MeasureSpec.makeMeasureSpec(container.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(container.getHeight(), View.MeasureSpec.EXACTLY));
            v.layout(0, 0, container.getWidth(), container.getHeight());
        }
        if (snapshot != null && v.isLaidOut()) {
            ViewGroup stage = findViewById(R.id.content_stage);
            container.addView(snapshot, new FrameLayout.LayoutParams(-1, -1));
            v.bringToFront();
            android.graphics.PointF center = nav.selectedCenterIn(stage);
            tabReveal.play(stage, container, java.util.Collections.singletonList(snapshot), v,
                    center.x, center.y, () -> {
                        container.removeView(snapshot);
                        snapshot.setImageDrawable(null);
                    });
        } else if (snapshot != null) {
            snapshot.setImageDrawable(null);
        }
    }

    /** Bounded bitmap: outgoing fragment is paused immediately rather than running behind the wave. */
    private android.widget.ImageView snapshotPage(View page) {
        if (page.getWidth() <= 0 || page.getHeight() <= 0) return null;
        try {
            float scale = Math.min(1f, 1600f / Math.max(page.getWidth(), page.getHeight()));
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                    Math.max(1, Math.round(page.getWidth() * scale)),
                    Math.max(1, Math.round(page.getHeight() * scale)), android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
            canvas.scale(scale, scale);
            page.draw(canvas);
            android.widget.ImageView image = new android.widget.ImageView(this);
            image.setImageBitmap(bitmap);
            image.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            image.setClickable(false);
            return image;
        } catch (RuntimeException | OutOfMemoryError failure) {
            return null; // Animation is optional; switching tabs must always work.
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (nav != null) nav.onHostStart();
    }

    @Override
    protected void onStop() {
        entrance.finish();
        tabReveal.finish();
        if (nav != null) nav.onHostStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        entrance.finish();
        tabReveal.finish();
        if (nav != null) nav.onHostStop();
        super.onDestroy();
    }

    private void prewarmNextTab() {
        if (isFinishing() || isDestroyed()) return;
        FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        for (int id : TABS) {
            if (id == currentTab || fm.findFragmentByTag(tagFor(id)) != null) continue;
            Fragment f = newTab(id);
            if (f == null) continue;
            fm.beginTransaction().setReorderingAllowed(true)
                    .add(R.id.fragment_container, f, tagFor(id))
                    .hide(f)
                    .setMaxLifecycle(f, androidx.lifecycle.Lifecycle.State.STARTED)
                    .commit();
            // One tab per pass keeps every single main-thread block short.
            nav.postDelayed(this::prewarmNextTab, 250);
            return;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle out) {
        tabReveal.finish();
        super.onSaveInstanceState(out);
        out.putInt(STATE_TAB, currentTab);
    }

    /** Selects a tab exactly like a user tap would (used by tests and deep links). */
    public void selectTab(int navId) {
        nav.setSelectedItemId(navId);
    }

    private void applyChromeInsets() {
        int gap = Math.round(Motion.dp(this, 12));
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) nav.getLayoutParams();
        int side = Math.round(Motion.dp(this, 22));
        // Keep the pill a comfortable thumb width on tablets / landscape.
        int maxW = Math.round(Motion.dp(this, 440));
        int w = getResources().getDisplayMetrics().widthPixels - bars.left - bars.right;
        int extra = Math.max(0, (w - 2 * side - maxW) / 2);
        lp.leftMargin = bars.left + side + extra;
        lp.rightMargin = bars.right + side + extra;
        lp.bottomMargin = bars.bottom + gap;
        nav.setLayoutParams(lp);
        topBar.setPadding(bars.left, bars.top, bars.right, 0);
        edge.setHeight(bars.top + Math.round(Motion.dp(this, 44 + 26)));
    }

    /** Space content must keep free at the bottom so its last row clears the floating pill. */
    private int bottomClearance() {
        int navH = nav.getHeight() > 0 ? nav.getHeight() : Math.round(Motion.dp(this, 66));
        return bars.bottom + Math.round(Motion.dp(this, 12)) + navH + Math.round(Motion.dp(this, 16));
    }

    private void onContentView(View view) {
        currentRoot = view;
        // New screen: show the pill, reset the compact title.
        nav.setMinimized(false);
        topBar.setAlpha(0f);
        edge.setProgress(0f);
        View title = view.findViewWithTag("ie_large_title");
        topTitle.setText(title instanceof TextView ? ((TextView) title).getText() : getString(R.string.app_name));
        applyContentInsets(view);
        View scroller = view.findViewWithTag("ie_scroll");
        View s = scroller != null ? scroller : view.findViewWithTag("ie_scroll_list");
        edge.bindScroller(s);
        if (s == null) return;
        // Tabs are kept alive, so a scroller is wired once; on re-show only the chrome is synced
        // to where that tab was left (RecyclerView listeners would otherwise stack up).
        ScrollSink sink = (ScrollSink) s.getTag(R.id.ie_scroll_sink);
        if (sink == null) {
            sink = wireScroll(s, scroller != null ? title : null);
            s.setTag(R.id.ie_scroll_sink, sink);
        }
        if (sink != null) sink.syncChrome(scrollY(s));
        if (startupEntranceEnabled && coldStart && !entranceStarted && currentTab == R.id.nav_home
                && title != null && !Boolean.getBoolean("instaeclipse.screenshotDir")) {
            entranceStarted = true;
            entrance.play(findViewById(R.id.main), view, nav);
        }
    }

    private static int scrollY(View s) {
        return s instanceof RecyclerView ? ((RecyclerView) s).computeVerticalScrollOffset() : s.getScrollY();
    }

    private static final String[] INSET_TAGS = {"ie_scroll", "ie_inset_top", "ie_inset_bottom",
            "ie_scroll_list", "ie_inset_bottom_margin", "ie_float_above_nav"};

    private void applyContentInsets(View root) {
        int bottom = bottomClearance();
        boolean tagged = false;
        for (String t : INSET_TAGS) if (root.findViewWithTag(t) != null) { tagged = true; break; }
        if (!tagged) {
            // Safety net: a screen without inset tags must still never draw under the status
            // bar / camera cutout or behind the floating pill.
            padTagged(root, root, bars.top, bottom);
            root.setPadding(bars.left, root.getPaddingTop(), bars.right, root.getPaddingBottom());
            return;
        }
        padTagged(root, "ie_scroll", bars.top, bottom);
        padTagged(root, "ie_inset_top", bars.top, -1);
        padTagged(root, "ie_inset_bottom", -1, bottom);
        padTagged(root, "ie_scroll_list", -1, bottom);
        View card = root.findViewWithTag("ie_inset_bottom_margin");
        if (card != null && card.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) card.getLayoutParams();
            lp.bottomMargin = bottom;
            card.setLayoutParams(lp);
        }
        View fab = root.findViewWithTag("ie_float_above_nav");
        if (fab != null && fab.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) fab.getLayoutParams();
            Object base = fab.getTag(R.id.ie_base_margin);
            if (base == null) { base = lp.bottomMargin; fab.setTag(R.id.ie_base_margin, base); }
            lp.bottomMargin = (int) base + bottom - Math.round(Motion.dp(this, 16));
            fab.setLayoutParams(lp);
        }
        // Horizontal cutouts / gesture insets in landscape.
        root.setPadding(bars.left, root.getPaddingTop(), bars.right, root.getPaddingBottom());
    }

    private static void padTagged(View root, String tag, int top, int bottom) {
        View v = root.findViewWithTag(tag);
        if (v == null) return;
        padTagged(root, v, top, bottom);
    }

    private static void padTagged(View root, View v, int top, int bottom) {
        int[] base = (int[]) v.getTag(R.id.ie_base_padding);
        if (base == null) {
            base = new int[]{v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom()};
            v.setTag(R.id.ie_base_padding, base);
        }
        v.setPadding(base[0], top >= 0 ? base[1] + top : v.getPaddingTop(),
                base[2], bottom >= 0 ? base[3] + bottom : v.getPaddingBottom());
        if (v instanceof ViewGroup) ((ViewGroup) v).setClipToPadding(false);
    }

    @androidx.annotation.Nullable
    private ScrollSink wireScroll(View scroller, View largeTitle) {
        if (scroller == null) return null;
        final float edgeRange = Motion.dp(this, 12);
        final float minimizeAt = Motion.dp(this, 24);
        final float restoreAt = Motion.dp(this, 12);
        ScrollSink sink = new ScrollSink() {
            private int last;
            private int travel;

            @Override
            public void syncChrome(int y) {
                // Compact bar frosts in as the large title passes under it.
                if (largeTitle != null) {
                    float start = largeTitle.getTop() + largeTitle.getHeight() * 0.35f;
                    float range = Math.max(1f, largeTitle.getHeight() * 0.65f);
                    float p = Math.max(0f, Math.min(1f, (y - start) / range));
                    if (topBar.getAlpha() != p) topBar.setAlpha(p);
                    edge.setProgress(Math.min(1f, Math.max(0f, y / edgeRange)));
                }
                last = y;
                travel = 0;
                // A retained tab may be restored at a non-zero scroll position. Sync the chrome
                // immediately instead of waiting for a later scroll callback.
                nav.setMinimized(y >= minimizeAt);
            }

            @Override
            public void onScroll(int y) {
                if (!scroller.isShown()) return; // Hidden retained tabs cannot own the chrome.
                if (largeTitle != null) {
                    float start = largeTitle.getTop() + largeTitle.getHeight() * 0.35f;
                    float range = Math.max(1f, largeTitle.getHeight() * 0.65f);
                    float p = Math.max(0f, Math.min(1f, (y - start) / range));
                    if (topBar.getAlpha() != p) topBar.setAlpha(p);
                    edge.setProgress(Math.min(1f, Math.max(0f, y / edgeRange)));
                }
                int dy = y - last;
                last = y;
                if (dy == 0) return;
                if ((dy > 0 && travel < 0) || (dy < 0 && travel > 0)) travel = 0;
                travel += dy;
                // HyperSpring shrinks the whole bar after 24dp down, restores after 12dp up.
                // Direction accumulation also works with slow, sub-touch-slop scroll frames.
                if (y <= restoreAt || travel <= -restoreAt) {
                    nav.setMinimized(false);
                    travel = 0;
                } else if (travel >= minimizeAt) {
                    nav.setMinimized(true);
                    travel = 0;
                }
            }
        };
        if (scroller instanceof NestedScrollView) {
            ((NestedScrollView) scroller).setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> sink.onScroll(y));
        } else if (scroller instanceof RecyclerView) {
            ((RecyclerView) scroller).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    sink.onScroll(rv.computeVerticalScrollOffset());
                }
            });
        }
        return sink;
    }

    private interface ScrollSink {
        void onScroll(int y);

        void syncChrome(int y);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
