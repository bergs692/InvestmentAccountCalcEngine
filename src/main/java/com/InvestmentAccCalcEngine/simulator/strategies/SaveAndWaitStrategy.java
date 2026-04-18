package com.InvestmentAccCalcEngine.simulator.strategies;

import com.InvestmentAccCalcEngine.simulator.SimulationContext;
import com.InvestmentAccCalcEngine.simulator.Strategy;

/**
 * Baseline strategy: do absolutely nothing — just collect salary.
 * Useful as a comparison against active investment strategies.
 */
public class SaveAndWaitStrategy implements Strategy {

    @Override
    public String getName() {
        return "Save & Wait (baseline - no investments)";
    }

    @Override
    public void execute(SimulationContext ctx) {
        // Intentionally empty — salary deposits and existing
        // mortgage/rental processing happen automatically.
    }
}
