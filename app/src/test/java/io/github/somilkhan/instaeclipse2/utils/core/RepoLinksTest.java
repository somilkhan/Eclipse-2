package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.BuildConfig;

public class RepoLinksTest {

    @Test
    public void repoComesFromSingleBuildConfigField() {
        assertEquals("somilkhan/Eclipse-2", BuildConfig.GITHUB_REPO);
        assertEquals(BuildConfig.GITHUB_REPO, RepoLinks.REPO);
    }

    @Test
    public void allLinksDeriveFromRepo() {
        assertEquals("https://github.com/somilkhan/Eclipse-2", RepoLinks.REPO_URL);
        assertEquals(RepoLinks.REPO_URL, ExportBrand.GITHUB);
        assertEquals("https://github.com/somilkhan/Eclipse-2/releases/latest", RepoLinks.RELEASES_LATEST_URL);
        assertEquals("https://raw.githubusercontent.com/somilkhan/Eclipse-2/refs/heads/main/version.json",
                RepoLinks.VERSION_JSON_URL);
        assertTrue(RepoLinks.ISSUES_URL.startsWith(RepoLinks.REPO_URL));
        assertTrue(RepoLinks.CONTRIBUTORS_URL.startsWith(RepoLinks.REPO_URL));
    }
}
