package ps.reso.instaeclipse;

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

import ps.reso.instaeclipse.fragments.FeaturesFragment;
import ps.reso.instaeclipse.fragments.HelpFragment;
import ps.reso.instaeclipse.fragments.HomeFragment;
import ps.reso.instaeclipse.fragments.LoggingFragment;
import ps.reso.instaeclipse.ui.glass.GlassNavPill;
import ps.reso.instaeclipse.ui.glass.Motion;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

/**
 * Shell: edge-to-edge window, fragments scroll underneath a frosted compact top bar and a floating
 * glass nav pill. Fragments opt into inset handling with view tags (no fragment code needed):
 * <ul>
 *   <li>{@code ie_scroll} – the main scroller: top padding = status bar, bottom = above the pill;
 *       drives the compact-title fade and pill hide-on-scroll.</li>
 *   <li>{@code ie_large_title} – the large title inside it (its text becomes the compact title).</li>
 *   <li>{@code ie_scroll_list} – a scroller below a fixed header: bottom padding + pill hiding.</li>
 *   <li>{@code ie_inset_top} / {@code ie_inset_bottom} – pad only that edge.</li>
 *   <li>{@code ie_inset_bottom_margin} – bottom margin so a fixed panel ends above the pill.</li>
 *   <li>{@code ie_float_above_nav} – bottom margin so a FAB floats above the pill.</li>
 * </ul>
 */
public class MainActivity extends AppCompatActivity {

    private GlassNavPill nav;
    private View topBar;
    private TextView topTitle;
    private Insets bars = Insets.NONE;
    private View currentRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // NOTE: intentionally NOT applying DynamicColors — the app ships a fixed branded palette
        // shared with the in-Instagram sheet (see Theme.InstaEclipse).
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        ps.reso.instaeclipse.mods.media.FrameSessionCache.cleanupOrphans(getCacheDir());
        // Only on a fresh launch: recreation (rotation, dark-mode/locale change, process-death
        // restore) must not re-run the network check and stack another dialog.
        if (savedInstanceState == null) VersionCheckUtility.checkForUpdates(this);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
        nav = findViewById(R.id.bottom_navigation);
        topBar = findViewById(R.id.top_app_bar);
        topTitle = findViewById(R.id.toolbar_title_name);
        nav.setBlurSource(fragmentContainer);
        ((ps.reso.instaeclipse.ui.glass.BackdropBlurLayout) topBar).setBlurSource(fragmentContainer);
        ((ps.reso.instaeclipse.ui.glass.BackdropBlurLayout) topBar).setRimVisible(false);

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
                        if (f.getId() == R.id.fragment_container) {
                            onContentView(view);
                        }
                    }
                }, false);

        // Load the HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Select Home only on a fresh launch. After recreation the pill restores its own
        // selection (saved state), matching the fragment the FragmentManager restored.
        // Re-tapping the current tab is a no-op inside the pill (keeps scroll/search state).
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_home);

        nav.setOnItemSelectedListener(id -> {
            Fragment selectedFragment = null;
            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_features) {
                selectedFragment = new FeaturesFragment();
            } else if (id == R.id.nav_logs) {
                selectedFragment = new LoggingFragment();
            } else if (id == R.id.nav_help) {
                selectedFragment = new HelpFragment();
            }
            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.ie_fade_in, R.anim.ie_fade_out)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
        });
    }

    /** Selects a tab exactly like a user tap would (used by tests and deep links). */
    public void selectTab(int navId) {
        nav.setSelectedItemId(navId);
    }

    private void applyChromeInsets() {
        int gap = Math.round(Motion.dp(this, 12));
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) nav.getLayoutParams();
        int side = Math.round(Motion.dp(this, 20));
        // Keep the pill a comfortable thumb width on tablets / landscape.
        int maxW = Math.round(Motion.dp(this, 440));
        int w = getResources().getDisplayMetrics().widthPixels - bars.left - bars.right;
        int extra = Math.max(0, (w - 2 * side - maxW) / 2);
        lp.leftMargin = bars.left + side + extra;
        lp.rightMargin = bars.right + side + extra;
        lp.bottomMargin = bars.bottom + gap;
        nav.setLayoutParams(lp);
        topBar.setPadding(bars.left, bars.top, bars.right, 0);
    }

    /** Space content must keep free at the bottom so its last row clears the floating pill. */
    private int bottomClearance() {
        int navH = nav.getHeight() > 0 ? nav.getHeight() : Math.round(Motion.dp(this, 62));
        return bars.bottom + Math.round(Motion.dp(this, 12)) + navH + Math.round(Motion.dp(this, 16));
    }

    private void onContentView(View view) {
        currentRoot = view;
        // New screen: show the pill, reset the compact title.
        nav.setHidden(false);
        topBar.setAlpha(0f);
        View title = view.findViewWithTag("ie_large_title");
        topTitle.setText(title instanceof TextView ? ((TextView) title).getText() : getString(R.string.app_name));
        applyContentInsets(view);
        View scroller = view.findViewWithTag("ie_scroll");
        wireScroll(scroller != null ? scroller : view.findViewWithTag("ie_scroll_list"),
                scroller != null ? title : null);
    }

    private void applyContentInsets(View root) {
        int bottom = bottomClearance();
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
        int[] base = (int[]) v.getTag(R.id.ie_base_padding);
        if (base == null) {
            base = new int[]{v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), v.getPaddingBottom()};
            v.setTag(R.id.ie_base_padding, base);
        }
        v.setPadding(base[0], top >= 0 ? base[1] + top : v.getPaddingTop(),
                base[2], bottom >= 0 ? base[3] + bottom : v.getPaddingBottom());
        if (v instanceof ViewGroup) ((ViewGroup) v).setClipToPadding(false);
    }

    private void wireScroll(View scroller, View largeTitle) {
        if (scroller == null) return;
        final int slop = Math.round(Motion.dp(this, 6));
        final int[] last = {0};
        ScrollSink sink = y -> {
            // Compact bar frosts in as the large title passes under it.
            if (largeTitle != null) {
                float start = largeTitle.getTop() + largeTitle.getHeight() * 0.35f;
                float range = Math.max(1f, largeTitle.getHeight() * 0.65f);
                float p = Math.max(0f, Math.min(1f, (y - start) / range));
                if (topBar.getAlpha() != p) topBar.setAlpha(p);
            }
            int dy = y - last[0];
            if (Math.abs(dy) > slop) {
                nav.setHidden(dy > 0 && y > Motion.dp(this, 160));
                last[0] = y;
            }
        };
        if (scroller instanceof NestedScrollView) {
            ((NestedScrollView) scroller).setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> {
                        sink.onScroll(y);
                        if (y + v.getHeight() >= ((NestedScrollView) v).getChildAt(0).getHeight()) nav.setHidden(false);
                    });
        } else if (scroller instanceof RecyclerView) {
            ((RecyclerView) scroller).addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    sink.onScroll(rv.computeVerticalScrollOffset());
                    if (!rv.canScrollVertically(1)) nav.setHidden(false);
                }
            });
        }
    }

    private interface ScrollSink { void onScroll(int y); }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
