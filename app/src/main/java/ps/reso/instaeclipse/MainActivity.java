package ps.reso.instaeclipse;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import ps.reso.instaeclipse.fragments.FeaturesFragment;
import ps.reso.instaeclipse.fragments.HelpFragment;
import ps.reso.instaeclipse.fragments.HomeFragment;
import ps.reso.instaeclipse.fragments.LoggingFragment;
import ps.reso.instaeclipse.utils.log.Logging;
import ps.reso.instaeclipse.utils.version.VersionCheckUtility;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // NOTE: intentionally NOT applying DynamicColors — 0.7 ships a fixed branded dark palette
        // (see Theme.InstaEclipse); adopting the wallpaper palette here would override the brand.
        super.onCreate(savedInstanceState);
        Logging.init(this, "instaeclipse_companion.log");
        ps.reso.instaeclipse.mods.media.FrameSessionCache.cleanupOrphans(getCacheDir());
        // Only on a fresh launch: recreation (rotation, dark-mode/locale change, process-death
        // restore) must not re-run the network check and stack another dialog.
        if (savedInstanceState == null) VersionCheckUtility.checkForUpdates(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.top_app_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Prevent status bar color changes on scroll
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        if (appBarLayout != null) {
            appBarLayout.setExpanded(true, false);
            appBarLayout.setLiftOnScroll(false);
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        FrameLayout fragmentContainer = findViewById(R.id.fragment_container);

        // On targetSdk 35+, edge-to-edge is enforced. The BottomNavigationView absorbs the
        // system gesture inset via fitsSystemWindows, making its actual height larger than the
        // fixed 82dp we had in XML. Sync the fragment container's bottom padding to match the
        // nav bar's real height after each layout pass.
        bottomNavigation.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            int navHeight = v.getHeight();
            if (fragmentContainer.getPaddingBottom() != navHeight) {
                fragmentContainer.setPadding(0, 0, 0, navHeight);
            }
        });

        // Load the HomeFragment by default
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Select Home only on a fresh launch. After recreation the BottomNavigationView restores
        // its own selection, matching the fragment the FragmentManager restored; forcing Home here
        // left e.g. the Logs tab on screen with the Home item highlighted.
        if (savedInstanceState == null) bottomNavigation.setSelectedItemId(R.id.nav_home);
        // Re-tapping the current tab must not rebuild it (lost scroll/search/sub-menu state).
        bottomNavigation.setOnItemReselectedListener(item -> { });

        // Handle bottom navigation item clicks
        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_features) {
                selectedFragment = new FeaturesFragment();
            } else if (item.getItemId() == R.id.nav_logs) {
                selectedFragment = new LoggingFragment();
            } else if (item.getItemId() == R.id.nav_help) {
                selectedFragment = new HelpFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });
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
