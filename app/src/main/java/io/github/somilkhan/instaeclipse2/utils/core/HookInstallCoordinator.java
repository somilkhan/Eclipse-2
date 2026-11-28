package io.github.somilkhan.instaeclipse2.utils.core;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-local single-flight coordinator for installing Xposed callbacks.
 * Persistent DexKit data is a discovery cache only; a new Instagram process gets a fresh instance
 * and must install its hooks again.
 */
public final class HookInstallCoordinator {
    public enum State {
        NOT_STARTED,
        RUNNING,
        COMPLETE,
        FAILED
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.NOT_STARTED);

    /** Returns true for exactly one worker start in this process. */
    public boolean tryStart() {
        return state.compareAndSet(State.NOT_STARTED, State.RUNNING);
    }

    /** Marks a sweep complete only after its runnable reaches the end normally. */
    public boolean markComplete() {
        return state.compareAndSet(State.RUNNING, State.COMPLETE);
    }

    /** A failed sweep is terminal for this process; a fresh process retries from a clean state. */
    public boolean markFailed() {
        return state.compareAndSet(State.RUNNING, State.FAILED);
    }

    public State getState() {
        return state.get();
    }
}
