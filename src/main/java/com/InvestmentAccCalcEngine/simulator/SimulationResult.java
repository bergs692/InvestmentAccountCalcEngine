package com.InvestmentAccCalcEngine.simulator;

import java.math.BigDecimal;
import java.util.List;

/**
 * Immutable snapshot of a completed simulation run.
 * Collected by {@link SimulationRunner} and displayed by the CLI.
 */
public class SimulationResult {

    private final String strategyName;
    private final int totalMonths;
    private final BigDecimal startingBalance;
    private final BigDecimal endingBalance;
    private final BigDecimal startingNetworth;
    private final BigDecimal endingNetworth;
    private final int propertiesOwned;
    private final int propertiesRented;
    private final int activeMortgages;
    private final List<MonthSnapshot> monthlySnapshots;
    private final List<String> eventLog;

    public SimulationResult(String strategyName,
                            int totalMonths,
                            BigDecimal startingBalance,
                            BigDecimal endingBalance,
                            BigDecimal startingNetworth,
                            BigDecimal endingNetworth,
                            int propertiesOwned,
                            int propertiesRented,
                            int activeMortgages,
                            List<MonthSnapshot> monthlySnapshots,
                            List<String> eventLog) {
        this.strategyName = strategyName;
        this.totalMonths = totalMonths;
        this.startingBalance = startingBalance;
        this.endingBalance = endingBalance;
        this.startingNetworth = startingNetworth;
        this.endingNetworth = endingNetworth;
        this.propertiesOwned = propertiesOwned;
        this.propertiesRented = propertiesRented;
        this.activeMortgages = activeMortgages;
        this.monthlySnapshots = monthlySnapshots;
        this.eventLog = eventLog;
    }

    // ── Getters ──

    public String getStrategyName()          { return strategyName; }
    public int getTotalMonths()              { return totalMonths; }
    public BigDecimal getStartingBalance()   { return startingBalance; }
    public BigDecimal getEndingBalance()     { return endingBalance; }
    public BigDecimal getStartingNetworth()  { return startingNetworth; }
    public BigDecimal getEndingNetworth()    { return endingNetworth; }
    public int getPropertiesOwned()          { return propertiesOwned; }
    public int getPropertiesRented()         { return propertiesRented; }
    public int getActiveMortgages()          { return activeMortgages; }
    public List<MonthSnapshot> getMonthlySnapshots() { return monthlySnapshots; }
    public List<String> getEventLog()        { return eventLog; }

    public BigDecimal getNetworthGain() {
        return endingNetworth.subtract(startingNetworth);
    }

    public BigDecimal getBalanceGain() {
        return endingBalance.subtract(startingBalance);
    }

    /**
     * Snapshot of key metrics at a single point in time.
     */
    public static class MonthSnapshot {
        private final int month;
        private final BigDecimal balance;
        private final BigDecimal networth;
        private final int propertiesOwned;

        public MonthSnapshot(int month, BigDecimal balance, BigDecimal networth, int propertiesOwned) {
            this.month = month;
            this.balance = balance;
            this.networth = networth;
            this.propertiesOwned = propertiesOwned;
        }

        public int getMonth()               { return month; }
        public BigDecimal getBalance()       { return balance; }
        public BigDecimal getNetworth()      { return networth; }
        public int getPropertiesOwned()      { return propertiesOwned; }
    }
}
