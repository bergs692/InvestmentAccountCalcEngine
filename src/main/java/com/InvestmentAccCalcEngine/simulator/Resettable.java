package com.InvestmentAccCalcEngine.simulator;

/**
 * Interface for services that can snapshot and restore their state.
 * Implement this on each service so simulations run in isolation
 * without polluting the live CLI state.
 *
 * Usage pattern:
 *   Object snapshot = service.snapshot();
 *   // ... run simulation ...
 *   service.restore(snapshot);
 */
public interface Resettable {

    /**
     * Capture the current state as an opaque snapshot.
     * The returned object should be a deep copy — mutations
     * during the simulation must not affect the snapshot.
     */
    Object snapshot();

    /**
     * Restore state from a previously captured snapshot.
     * After this call, the service should behave exactly
     * as it did when snapshot() was called.
     */
    void restore(Object snapshot);
}
