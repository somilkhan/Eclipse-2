package io.github.somilkhan.instaeclipse2.utils.root;

/** Result of the last root check in this process. */
public enum RootStatus {
    /** Not checked yet (root switch off, or nothing has needed it since the app started). */
    UNKNOWN,
    /** No {@code su} binary could be started. */
    NOT_ROOTED,
    /** {@code su} exists but refused, timed out (prompt not answered) or did not run as uid 0. */
    DENIED,
    GRANTED
}
