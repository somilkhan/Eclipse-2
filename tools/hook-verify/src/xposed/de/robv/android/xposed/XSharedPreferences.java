package de.robv.android.xposed;

import android.content.SharedPreferences;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** Host-JVM stand-in: always empty, every getter returns its default. */
public final class XSharedPreferences implements SharedPreferences {
    public XSharedPreferences(File prefFile) {}
    public XSharedPreferences(String packageName) {}
    public XSharedPreferences(String packageName, String prefFileName) {}
    public boolean makeWorldReadable() { return false; }
    public File getFile() { return new File("/nonexistent"); }
    public synchronized void reload() {}
    public synchronized boolean hasFileChanged() { return false; }
    @Override public Map<String, ?> getAll() { return Collections.emptyMap(); }
    @Override public String getString(String key, String defValue) { return defValue; }
    @Override public Set<String> getStringSet(String key, Set<String> defValues) { return defValues; }
    @Override public int getInt(String key, int defValue) { return defValue; }
    @Override public long getLong(String key, long defValue) { return defValue; }
    @Override public float getFloat(String key, float defValue) { return defValue; }
    @Override public boolean getBoolean(String key, boolean defValue) { return defValue; }
    @Override public boolean contains(String key) { return false; }
    @Override public Editor edit() { throw new UnsupportedOperationException("read-only"); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
}
