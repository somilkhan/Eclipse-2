package io.github.somilkhan.instaeclipse2.compat;
import android.content.SharedPreferences;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
public final class XSharedPreferences implements SharedPreferences {
    private final String file;
    private volatile SharedPreferences delegate;
    public XSharedPreferences(File ignored) { this.file = "instaeclipse_cache"; }
    public XSharedPreferences(String packageName) { this.file = "default"; }
    public XSharedPreferences(String packageName, String prefFileName) { this.file = prefFileName; }
    private SharedPreferences prefs() {
        if (delegate == null) {
            try {
                io.github.libxposed.api.XposedInterface api = XposedBridge.framework();
                if ((api.getFrameworkProperties() & io.github.libxposed.api.XposedInterface.PROP_CAP_REMOTE) == 0) return null;
                delegate = api.getRemotePreferences(file);
            } catch (Throwable ignored) {}
        }
        return delegate;
    }
    public boolean makeWorldReadable() { return false; }
    public File getFile() { return new File("/nonexistent"); }
    public synchronized void reload() { }
    public synchronized boolean hasFileChanged() { return false; }
    @Override public Map<String, ?> getAll() { SharedPreferences p=prefs(); return p==null?Collections.emptyMap():p.getAll(); }
    @Override public String getString(String k,String d){SharedPreferences p=prefs();return p==null?d:p.getString(k,d);}
    @Override public Set<String> getStringSet(String k,Set<String>d){SharedPreferences p=prefs();return p==null?d:p.getStringSet(k,d);}
    @Override public int getInt(String k,int d){SharedPreferences p=prefs();return p==null?d:p.getInt(k,d);}
    @Override public long getLong(String k,long d){SharedPreferences p=prefs();return p==null?d:p.getLong(k,d);}
    @Override public float getFloat(String k,float d){SharedPreferences p=prefs();return p==null?d:p.getFloat(k,d);}
    @Override public boolean getBoolean(String k,boolean d){SharedPreferences p=prefs();return p==null?d:p.getBoolean(k,d);}
    @Override public boolean contains(String k){SharedPreferences p=prefs();return p!=null&&p.contains(k);}
    @Override public Editor edit(){throw new UnsupportedOperationException("Remote preferences are read-only in hooked apps");}
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
}
