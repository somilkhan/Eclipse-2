package io.github.somilkhan.instaeclipse2.utils.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.github.somilkhan.instaeclipse2.R;

public class FeatureStatusTrackerTest {
    private static final String KEY = "FeatureStatusTrackerContractTest";

    @Test
    public void distinguishesInstalledRuntimeAndFailureStates() {
        FeatureStatusTracker.setEnabled(KEY, R.string.version);
        assertEquals(FeatureStatusTracker.HealthState.NOT_EXERCISED, FeatureStatusTracker.getHealthState(KEY));
        assertFalse(FeatureStatusTracker.getStatus().get(KEY));

        FeatureStatusTracker.setHooked(KEY);
        assertEquals(FeatureStatusTracker.HealthState.INSTALLED, FeatureStatusTracker.getHealthState(KEY));
        assertTrue(FeatureStatusTracker.getStatus().get(KEY));

        FeatureStatusTracker.setBroken(KEY, "synthetic failure reason");
        assertEquals(FeatureStatusTracker.HealthState.FAILED, FeatureStatusTracker.getHealthState(KEY));
        assertEquals("synthetic failure reason", FeatureStatusTracker.getFailureReason(KEY));
        assertFalse(FeatureStatusTracker.getStatus().get(KEY));

        FeatureStatusTracker.setHooked(KEY);
        assertEquals(FeatureStatusTracker.HealthState.INSTALLED, FeatureStatusTracker.getHealthState(KEY));
        FeatureStatusTracker.setRuntimeObserved(KEY);
        assertEquals(FeatureStatusTracker.HealthState.RUNTIME_OBSERVED, FeatureStatusTracker.getHealthState(KEY));
        assertTrue(FeatureStatusTracker.getStatus().get(KEY));

        FeatureStatusTracker.setDisabled(KEY);
        assertFalse(FeatureStatusTracker.getHealthStatus().containsKey(KEY));
    }
}
