package io.github.somilkhan.instaeclipse2.utils.log;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Privacy redaction for module log lines. The log is exported and attached to crash reports,
 * which users post to public issues, so it must never carry:
 * <ul>
 *   <li>signed CDN URLs ({@code oh=}/{@code oe=} query strings are time-limited bearer links to
 *       the media, including private-account content the user can see);</li>
 *   <li>share links / post shortcodes;</li>
 *   <li>usernames or media ids of the accounts the user interacted with.</li>
 * </ul>
 * Call sites use {@link #url} and {@link #id}; {@link Logging#append} applies {@link #text} to
 * every line as a safety net. All methods are pure, null-safe and idempotent.
 */
public final class LogRedact {

    private LogRedact() {}

    // scheme://authority[rest-of-url]. The authority stops at the first path/query/fragment
    // delimiter; the rest runs to whitespace or a quote/bracket that usually ends a URL in text.
    private static final Pattern URL = Pattern.compile(
            "\\b([A-Za-z][A-Za-z0-9+.-]{1,15})://([^\\s/?#\"'<>()\\[\\]{}]*)([^\\s\"'<>()\\[\\]{}]*)");

    private static final String ELLIPSIS = "…";

    /** Non-identifying first path segments worth keeping for debugging (CDN vs share link). */
    private static final java.util.Set<String> STRUCTURAL_SEGMENTS = new java.util.HashSet<>(
            java.util.Arrays.asList("v", "o1", "p", "reel", "reels", "tv", "stories", "s", "api", "graphql", "direct"));

    /**
     * Keeps scheme, host and a known structural first path segment (enough to tell e.g. a CDN
     * {@code /v/} URL from a share link {@code /p/}), and drops the remaining path, query, fragment and any
     * userinfo. {@code content://} and {@code file://} are local and left untouched.
     */
    public static String url(Object value) {
        if (value == null) return "null";
        String s = String.valueOf(value);
        if (s.indexOf("://") < 0) return s.isEmpty() ? s : id(s);
        return text(s);
    }

    /** Short, stable, non-reversible tag for a username / media id / pk ({@code #1a2b3c4d}). */
    public static String id(Object value) {
        if (value == null) return "null";
        String s = String.valueOf(value);
        if (s.isEmpty()) return "\"\"";
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(9).append('#');
            for (int i = 0; i < 4; i++) {
                sb.append(Character.forDigit((d[i] >> 4) & 0xf, 16)).append(Character.forDigit(d[i] & 0xf, 16));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "#?";
        }
    }

    /** Replaces every URL in free text (messages, stack traces, exception text) with {@link #url}. */
    public static String text(String line) {
        if (line == null || line.indexOf("://") < 0) return line;
        Matcher m = URL.matcher(line);
        StringBuffer out = null;
        while (m.find()) {
            String scheme = m.group(1);
            if (scheme.equalsIgnoreCase("content") || scheme.equalsIgnoreCase("file")) continue;
            if (out == null) out = new StringBuffer(line.length());
            m.appendReplacement(out, Matcher.quoteReplacement(redactMatch(scheme, m.group(2), m.group(3))));
        }
        if (out == null) return line;
        m.appendTail(out);
        return out.toString();
    }

    private static String redactMatch(String scheme, String authority, String rest) {
        int at = authority.lastIndexOf('@');
        String host = at >= 0 ? authority.substring(at + 1) : authority;
        StringBuilder sb = new StringBuilder(scheme.length() + host.length() + 16)
                .append(scheme).append("://").append(host);
        if (rest.isEmpty()) return sb.toString();
        if (rest.charAt(0) == '/') {
            int end = 1;
            while (end < rest.length() && "/?#".indexOf(rest.charAt(end)) < 0) end++;
            String first = rest.substring(1, end);
            // Only keep a known structural first segment, and only when more follows: anything
            // else may be the identifying part itself (a username, shortcode or filename).
            if (end < rest.length() && STRUCTURAL_SEGMENTS.contains(first.toLowerCase(java.util.Locale.ROOT))) {
                sb.append('/').append(first).append('/').append(ELLIPSIS);
            } else if (rest.equals("/")) {
                sb.append('/');
            } else {
                sb.append('/').append(ELLIPSIS);
            }
        } else {
            sb.append('/').append(ELLIPSIS);
        }
        return sb.toString();
    }
}
