package io.github.somilkhan.instaeclipse2.utils.feature;

import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.github.somilkhan.instaeclipse2.utils.i18n.I18n;

public class FeatureStatusTracker {
    private static final Map<String, Boolean> features = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Integer> labels = Collections.synchronizedMap(new HashMap<>());

    // These sets persist for the process: companion sync can refresh enabled flags after startup,
    // but Xposed hooks already installed in this process remain installed.
    private static final Set<String> hooked = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> runtimeObserved = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> broken = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, String> failureReasons = Collections.synchronizedMap(new HashMap<>());

    private static final String DEFAULT_FAILURE_REASON = "Hook failed; check the module log for details.";

    /** Conservative health state. Runtime observed is only set by explicit successful feature paths. */
    public enum HealthState {
        NOT_EXERCISED,
        INSTALLED,
        RUNTIME_OBSERVED,
        FAILED
    }

    public static void setEnabled(String name, int labelResId) {
        features.put(name, hooked.contains(name) && !broken.contains(name));
        labels.put(name, labelResId);
    }

    /** Mark an enabled feature as broken (its hook could not be resolved/installed). */
    public static void setBroken(String name, int labelResId) {
        setBroken(name, labelResId, DEFAULT_FAILURE_REASON);
    }

    /** Mark broken and retain a short reason for the in-app health detail dialog. */
    public static void setBroken(String name, int labelResId, String reason) {
        labels.put(name, labelResId);
        setBroken(name, reason);
    }

    /** Mark broken reusing whatever label was already registered (label comes from FeatureManager). */
    public static void setBroken(String name) {
        setBroken(name, DEFAULT_FAILURE_REASON);
    }

    public static void setBroken(String name, String reason) {
        broken.add(name);
        failureReasons.put(name, reason == null || reason.trim().isEmpty()
                ? DEFAULT_FAILURE_REASON : reason.trim());
        features.put(name, false);
    }

    public static boolean isBroken(String name) {
        return broken.contains(name);
    }

    public static void setDisabled(String name) {
        features.remove(name);
        labels.remove(name);
    }

    /** Records that the hook was installed; this alone does not claim the feature ran successfully. */
    public static void setHooked(String name) {
        hooked.add(name);
        broken.remove(name);
        failureReasons.remove(name);
        if (features.containsKey(name)) {
            features.put(name, true);
        }
    }

    /** Records a successful runtime path (for example, a gate was intercepted or an action completed). */
    public static void setRuntimeObserved(String name) {
        setHooked(name);
        runtimeObserved.add(name);
    }

    public static HealthState getHealthState(String name) {
        if (broken.contains(name)) return HealthState.FAILED;
        if (runtimeObserved.contains(name)) return HealthState.RUNTIME_OBSERVED;
        if (hooked.contains(name)) return HealthState.INSTALLED;
        return HealthState.NOT_EXERCISED;
    }

    /** Snapshot of currently enabled features and their evidence states. */
    public static Map<String, HealthState> getHealthStatus() {
        Map<String, HealthState> snapshot = new HashMap<>();
        synchronized (features) {
            for (String name : features.keySet()) snapshot.put(name, getHealthState(name));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public static String getFailureReason(String name) {
        String reason = failureReasons.get(name);
        return reason == null ? DEFAULT_FAILURE_REASON : reason;
    }

    public static String getLabel(Context ctx, String key) {
        Integer resId = labels.get(key);
        return resId != null ? I18n.t(ctx, resId) : key;
    }

    /** Legacy boolean API used by the existing load toast; true means installed, not necessarily exercised. */
    public static Map<String, Boolean> getStatus() {
        return features;
    }

    public static boolean hasEnabledFeatures() {
        return !features.isEmpty();
    }
}
