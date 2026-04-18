package com.InvestmentAccCalcEngine.simulator;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of available simulation strategies.
 * Add new strategies here — they'll automatically appear in the CLI menu.
 */
@Component
public class StrategyRegistry {

    private final Map<String, Strategy> strategies = new LinkedHashMap<>();

    public StrategyRegistry() {
        // Register all strategies here
        // Key is what the user types, value is the strategy instance
    }

    /**
     * Register a strategy programmatically.
     */
    public void register(String key, Strategy strategy) {
        strategies.put(key, strategy);
    }

    /**
     * Get all registered strategies.
     */
    public Map<String, Strategy> getAll() {
        return strategies;
    }

    /**
     * Look up a strategy by its key.
     */
    public Strategy get(String key) {
        return strategies.get(key);
    }

    public boolean isEmpty() {
        return strategies.isEmpty();
    }
}
