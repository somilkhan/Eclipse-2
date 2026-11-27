package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.IXposedHookLoadPackage;

public abstract class XC_LoadPackage extends XCallback implements IXposedHookLoadPackage {
    @SuppressWarnings("deprecation")
    public XC_LoadPackage() { super(); }

    public static final class LoadPackageParam extends XCallback.Param {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public android.content.pm.ApplicationInfo appInfo;
        public boolean isFirstApplication;
        @SuppressWarnings("deprecation")
        public LoadPackageParam() { super(); }
    }
}
