package com.InvestmentAccCalcEngine.simulator;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of available simulation strategies.
 *
 * Stores strategy factories (Suppliers) rather than instances,
 * so each simulation run gets a fresh strategy with clean internal state.
 * This prevents bugs like a "propertiesBought" counter carrying over
 * between runs.
 */
@Component
public class StrategyRegistry {

    private final Map<String, Supplier<Strategy>> factories = new LinkedHashMap<>();

    public StrategyRegistry() {
    }

    /**
     * Register a strategy factory. The supplier is called each time
     * the strategy is selected for a run, producing a fresh instance.
     *
     * Usage:
     *   registry.register("1", () -> new BuyAndHoldStrategy());
     *   registry.register("2", () -> new BuyAndHoldStrategy(300000, 2, 6.5, 30));
     */
    public void register(String key, Supplier<Strategy> factory) {
        factories.put(key, factory);
    }

    /**
     * Create a fresh strategy instance for the given key.
     * Returns null if the key is not registered.
     */
    public Strategy create(String key) {
        Supplier<Strategy> factory = factories.get(key);
        return factory != null ? factory.get() : null;
    }

    /**
     * Create fresh instances of all registered strategies.
     * Used by the comparison runner to get clean copies for each run.
     */
    public Map<String, Strategy> createAll() {
        Map<String, Strategy> instances = new LinkedHashMap<>();
        factories.forEach((key, factory) -> instances.put(key, factory.get()));
        return instances;
    }

    /**
     * Get strategy names for display (creates temporary instances just for getName()).
     */
    public Map<String, String> getDisplayNames() {
        Map<String, String> names = new LinkedHashMap<>();
        factories.forEach((key, factory) -> names.put(key, factory.get().getName()));
        return names;
    }

    public boolean isEmpty() {
        return factories.isEmpty();
    }

    public int size() {
        return factories.size();
    }
}
