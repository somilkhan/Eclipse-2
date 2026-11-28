package io.github.somilkhan.instaeclipse2.compat.callbacks;
import io.github.somilkhan.instaeclipse2.compat.callbacks.XCallback;
public abstract class XC_LoadPackage extends XCallback {
    public static final class LoadPackageParam extends XCallback.Param {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public android.content.pm.ApplicationInfo appInfo;
        public boolean isFirstApplication;
    }
}
