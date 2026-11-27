package io.github.somilkhan.instaeclipse2.utils.core;

import io.github.somilkhan.instaeclipse2.BuildConfig;

/**
 * All links to this project's GitHub repository. The owner/name lives in exactly one place,
 * {@code BuildConfig.GITHUB_REPO} (set in app/build.gradle); everything else is derived here.
 *
 * <p>InstaEclipse was created by ReSo7200; credit links to the original author stay literal
 * and must not be routed through this class.
 */
public final class RepoLinks {

    /** "owner/name", e.g. somilkhan/InstaEclipse-2. */
    public static final String REPO = BuildConfig.GITHUB_REPO;

    public static final String REPO_URL = "https://github.com/" + REPO;
    public static final String ISSUES_URL = REPO_URL + "/issues";
    public static final String NEW_ISSUE_URL = REPO_URL + "/issues/new/choose";
    public static final String CONTRIBUTORS_URL = REPO_URL + "/graphs/contributors";
    public static final String RELEASES_LATEST_URL = REPO_URL + "/releases/latest";
    /** Update checker manifest, served from the default branch. */
    public static final String VERSION_JSON_URL =
            "https://raw.githubusercontent.com/" + REPO + "/refs/heads/main/version.json";

    private RepoLinks() {}
}
