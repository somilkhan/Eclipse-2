package io.github.somilkhan.instaeclipse2.compat.callbacks;

import java.util.HashMap;
import java.util.Map;

/** Host-JVM stand-in for the Xposed XCallback (hook-verify harness only). */
public abstract class XCallback implements Comparable<XCallback> {
    public static final int PRIORITY_DEFAULT = 50;
    public static final int PRIORITY_LOWEST = -10000;
    public static final int PRIORITY_HIGHEST = 10000;
    public final int priority;

    @Deprecated
    public XCallback() { this.priority = PRIORITY_DEFAULT; }

    public XCallback(int priority) { this.priority = priority; }

    public static abstract class Param {
        public final Object[] callbacks = null;
        private final Map<String, Object> extra = new HashMap<>();

        @Deprecated
        protected Param() {}

        public synchronized android.os.Bundle getExtra() { return null; }

        public Object getObjectExtra(String key) { synchronized (extra) { return extra.get(key); } }

        public void setObjectExtra(String key, Object o) { synchronized (extra) { extra.put(key, o); } }
    }

    @Override
    public int compareTo(XCallback other) {
        if (this == other) return 0;
        if (other.priority != this.priority) return other.priority - this.priority;
        return System.identityHashCode(this) < System.identityHashCode(other) ? -1 : 1;
    }
}
