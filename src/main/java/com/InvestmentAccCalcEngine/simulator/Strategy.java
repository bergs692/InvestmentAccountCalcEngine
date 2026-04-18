package com.InvestmentAccCalcEngine.simulator;

/**
 * A simulation strategy that makes decisions each month.
 *
 * Implement this interface to define a custom investment strategy.
 * The runner calls {@link #execute(SimulationContext)} once per month,
 * giving the strategy full access to buy, sell, rent, charge, deposit,
 * and inspect all simulation state.
 *
 * Example:
 * <pre>
 * public class BuyAndHoldStrategy implements Strategy {
 *     public String getName() { return "Buy & Hold"; }
 *
 *     public void onStart(SimulationContext ctx) {
 *         // Buy first property in month 1
 *     }
 *
 *     public void execute(SimulationContext ctx) {
 *         // Each month: rent out any unrented properties
 *     }
 * }
 * </pre>
 */
public interface Strategy {

    /**
     * Human-readable name for this strategy (shown in output).
     */
    String getName();

    /**
     * Called once before the simulation loop starts.
     * Use for initial purchases, setup logic, etc.
     * Default: no-op.
     */
    default void onStart(SimulationContext ctx) {}

    /**
     * Called once per month AFTER the month's automatic processing
     * (salary, mortgage payments, rental income, appreciation).
     *
     * This is where your strategy logic lives — buy properties,
     * sell at thresholds, adjust rentals, make charges, etc.
     */
    void execute(SimulationContext ctx);

    /**
     * Called once after the simulation loop ends.
     * Use for final reporting, cleanup, etc.
     * Default: no-op.
     */
    default void onComplete(SimulationContext ctx) {}
}
