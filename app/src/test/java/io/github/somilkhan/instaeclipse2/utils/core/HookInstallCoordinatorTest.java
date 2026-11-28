package io.github.somilkhan.instaeclipse2.utils.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookInstallCoordinatorTest {
    @Test
    public void allowsOnlyOneSweepPerProcess() {
        HookInstallCoordinator coordinator = new HookInstallCoordinator();

        assertEquals(HookInstallCoordinator.State.NOT_STARTED, coordinator.getState());
        assertTrue(coordinator.tryStart());
        assertFalse(coordinator.tryStart());
        assertEquals(HookInstallCoordinator.State.RUNNING, coordinator.getState());
        assertTrue(coordinator.markComplete());
        assertFalse(coordinator.tryStart());
        assertEquals(HookInstallCoordinator.State.COMPLETE, coordinator.getState());
    }

    @Test
    public void failedSweepCannotBeReportedAsCompleteOrRestartedInSameProcess() {
        HookInstallCoordinator coordinator = new HookInstallCoordinator();

        assertTrue(coordinator.tryStart());
        assertTrue(coordinator.markFailed());
        assertFalse(coordinator.markComplete());
        assertFalse(coordinator.tryStart());
        assertEquals(HookInstallCoordinator.State.FAILED, coordinator.getState());
    }

    @Test
    public void completionAndFailureRequireRunningState() {
        HookInstallCoordinator coordinator = new HookInstallCoordinator();

        assertFalse(coordinator.markComplete());
        assertFalse(coordinator.markFailed());
        assertEquals(HookInstallCoordinator.State.NOT_STARTED, coordinator.getState());
    }
}
