package ps.reso.instaeclipse.mods.media;

/**
 * Instagram model classes that moved package between versions. Every list is ordered newest
 * first and keeps the old names, so lookups keep working on older Instagram builds.
 */
public final class MediaClassNames {
    private MediaClassNames() {}

    /** Public video-version interface with {@code getUrl()}: api.schemas on IG 457, model.mediasize before. */
    public static final String[] VIDEO_VERSION_INTF = {
            "com.instagram.api.schemas.VideoVersionIntf",
            "com.instagram.model.mediasize.VideoVersionIntf",
    };

    /** Concrete video version model. */
    public static final String[] VIDEO_VERSION = {
            "com.instagram.api.schemas.VideoVersion",
            "com.instagram.model.mediasize.VideoVersion",
    };

    /** Pando-backed immutable video version. */
    public static final String[] IMMUTABLE_PANDO_VIDEO_VERSION = {
            "com.instagram.api.schemas.ImmutablePandoVideoVersion",
            "com.instagram.model.mediasize.ImmutablePandoVideoVersion",
    };

    /** First class of {@code names} that the loader knows, or null. */
    public static Class<?> loadFirst(ClassLoader cl, String... names) {
        for (String n : names) {
            try {
                return cl.loadClass(n); // no static init
            } catch (Throwable ignored) {}
        }
        return null;
    }
}
