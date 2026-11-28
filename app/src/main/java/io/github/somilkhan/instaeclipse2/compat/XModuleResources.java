package io.github.somilkhan.instaeclipse2.compat;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
/** Loads module resources without the removed legacy resource-hook API. */
public final class XModuleResources {
    private final Resources resources;
    private XModuleResources(Resources resources) { this.resources = resources; }
    public static XModuleResources createInstance(String apkPath, Resources ignored) {
        try {
            AssetManager assets = Resources.getSystem().getAssets();
            java.lang.reflect.Method add = AssetManager.class.getMethod("addAssetPath", String.class);
            add.setAccessible(true); add.invoke(assets, apkPath);
            Resources system = Resources.getSystem();
            return new XModuleResources(new Resources(assets, system.getDisplayMetrics(), system.getConfiguration()));
        } catch (Throwable t) { throw new IllegalStateException("Unable to load module resources", t); }
    }
    public AssetManager getAssets() { return resources.getAssets(); }
    public Drawable getDrawable(int id) { return resources.getDrawable(id); }
    public Drawable getDrawable(int id, android.content.res.Resources.Theme theme) { return resources.getDrawable(id, theme); }
    public String getString(int id) { return resources.getString(id); }
    public String getString(int id, Object... args) { return resources.getString(id, args); }
}
