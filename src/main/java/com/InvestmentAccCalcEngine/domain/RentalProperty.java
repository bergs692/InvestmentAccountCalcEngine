package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents an active rental property investment.
 * Tracks all income and expenses from the landlord's perspective.
 *
 * Monthly cash flow =
 *   + Rent (when occupied)
 *   - Vacancy loss (estimated % of rent, applied as reduced rent months)
 *   - Property tax & insurance
 *   - Maintenance reserve
 *   - Property management fee (% of rent)
 *   - Utilities (landlord portion)
 *   - Lawn care / snow removal
 *   - Internet (if landlord-provided)
 *   - Electric (landlord portion)
 *   - CapEx / repair reserve
 *   - Other misc expenses
 *
 * Mortgage is tracked separately via ActiveMortgage and linked by mortgageIndex.
 */
@Setter
@Getter
public class RentalProperty {
    private final String address;
    private BigDecimal propertyValue;
    private BigDecimal monthlyRent;

    // Expense fields (monthly amounts, stored as positive values)
    private final BigDecimal vacancyRate;           // e.g. 0.05 = 5% vacancy
    private BigDecimal taxAndInsurance;        // monthly property tax + homeowner's insurance
    private BigDecimal maintenanceReserve;     // monthly maintenance set-aside
    private final BigDecimal propertyManagementRate; // e.g. 0.10 = 10% of rent
    private final BigDecimal utilitiesLandlord;      // landlord-paid utilities (not tenant-offset)
    private final BigDecimal lawnCareSnow;           // lawn care / snow removal (MN winters!)
    private final BigDecimal internet;               // if landlord provides internet
    private final BigDecimal electric;               // landlord-paid electric
    private BigDecimal capExReserve;           // capital expenditure / major repair reserve
    private BigDecimal otherMiscExpenses;      // catch-all for other costs

    // Linked mortgage (index into MortgageService's activeMortgages list, -1 if none)
    private final int mortgageIndex;

    private final Property property;

    // Running totals
    private BigDecimal totalRentCollected;
    private BigDecimal totalExpensesPaid;
    private BigDecimal totalVacancyLoss;
    private int monthsOwned;
    private int vacantMonths;

    public RentalProperty(String address, BigDecimal propertyValue, BigDecimal monthlyRent,
                          BigDecimal vacancyRate, BigDecimal taxAndInsurance,
                          BigDecimal maintenanceReserve, BigDecimal propertyManagementRate,
                          BigDecimal utilitiesLandlord, BigDecimal lawnCareSnow,
                          BigDecimal internet, BigDecimal electric,
                          BigDecimal capExReserve, BigDecimal otherMiscExpenses,
                          int mortgageIndex, Property property) {
        this.address = address;
        this.propertyValue = propertyValue;
        this.monthlyRent = monthlyRent;
        this.vacancyRate = vacancyRate;
        this.taxAndInsurance = taxAndInsurance;
        this.maintenanceReserve = maintenanceReserve;
        this.propertyManagementRate = propertyManagementRate;
        this.utilitiesLandlord = utilitiesLandlord;
        this.lawnCareSnow = lawnCareSnow;
        this.internet = internet;
        this.electric = electric;
        this.capExReserve = capExReserve;
        this.otherMiscExpenses = otherMiscExpenses;
        this.mortgageIndex = mortgageIndex;
        this.property = property;

        this.totalRentCollected = BigDecimal.ZERO;
        this.totalExpensesPaid = BigDecimal.ZERO;
        this.totalVacancyLoss = BigDecimal.ZERO;
        this.monthsOwned = 0;
        this.vacantMonths = 0;
    }

    /**
     * Simulate one month. Determines if occupied (based on vacancy rate probability),
     * calculates rent income and all expenses, returns net cash flow for the month.
     * Net cash flow does NOT include mortgage — that's handled separately.
     */
    public MonthlyRentalResult processMonth() {
        monthsOwned++;

        // Simple vacancy model: if random < vacancyRate, this month is vacant
        boolean occupied = Math.random() >= vacancyRate.doubleValue();

        BigDecimal rentThisMonth;
        if (occupied) {
            rentThisMonth = monthlyRent;
        } else {
            rentThisMonth = BigDecimal.ZERO;
            vacantMonths++;
            totalVacancyLoss = totalVacancyLoss.add(monthlyRent);
        }
        totalRentCollected = totalRentCollected.add(rentThisMonth);

        // Property management is % of rent collected (only when occupied)
        BigDecimal mgmtFee = rentThisMonth.multiply(propertyManagementRate).setScale(2, RoundingMode.HALF_UP);

        // Total landlord expenses this month (all fixed + management fee)
        BigDecimal totalExpenses = taxAndInsurance
                .add(maintenanceReserve)
                .add(mgmtFee)
                .add(utilitiesLandlord)
                .add(lawnCareSnow)
                .add(internet)
                .add(electric)
                .add(capExReserve)
                .add(otherMiscExpenses);

        totalExpensesPaid = totalExpensesPaid.add(totalExpenses);

        BigDecimal netCashFlow = rentThisMonth.subtract(totalExpenses);

        return new MonthlyRentalResult(occupied, rentThisMonth, totalExpenses, mgmtFee, netCashFlow);
    }

    /**
     * Recalculate expenses that are derived from property value.
     * Call yearly after property appreciation is applied.
     *
     * Uses the same rates from initial setup:
     *   - Tax + insurance: 2% of value / 12
     *   - Maintenance:     1.5% of value / 12
     *   - CapEx reserve:   1.5% of value / 12
     *   - Other misc:      3.3% of monthly rent
     */
    public void recalculateExpensesFromValue(BigDecimal currentValue, BigDecimal currentRent) {
        this.taxAndInsurance = currentValue.multiply(new BigDecimal("0.02"))
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        this.maintenanceReserve = currentValue.multiply(new BigDecimal("0.015"))
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        this.capExReserve = currentValue.multiply(new BigDecimal("0.015"))
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        this.otherMiscExpenses = currentRent.multiply(new BigDecimal("0.033"))
            .setScale(2, RoundingMode.HALF_UP);
    }



    /**
     * Total landlord expenses per month (assuming occupied, for display/projection).
     */
    public BigDecimal getEstimatedMonthlyExpenses() {
        BigDecimal mgmtFee = monthlyRent.multiply(propertyManagementRate).setScale(2, RoundingMode.HALF_UP);
        return taxAndInsurance
                .add(maintenanceReserve)
                .add(mgmtFee)
                .add(utilitiesLandlord)
                .add(lawnCareSnow)
                .add(internet)
                .add(electric)
                .add(capExReserve)
                .add(otherMiscExpenses);
    }

    /**
     * Net monthly cash flow estimate (rent - expenses, not including mortgage).
     */
    public BigDecimal getEstimatedMonthlyCashFlow() {
        return monthlyRent.subtract(getEstimatedMonthlyExpenses());
    }

    /**
     * Gross yield = (annual rent / property value) * 100
     */
    public BigDecimal getGrossYield() {
        if (propertyValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return monthlyRent.multiply(BigDecimal.valueOf(12))
                .divide(propertyValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Actual occupancy rate so far.
     */
    public BigDecimal getOccupancyRate() {
        if (monthsOwned == 0) return BigDecimal.valueOf(100);
        int occupiedMonths = monthsOwned - vacantMonths;
        return BigDecimal.valueOf(occupiedMonths)
                .divide(BigDecimal.valueOf(monthsOwned), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

    public void incrementMonthsOwned() {
        this.monthsOwned++;
    }

    public void addToTotalRentCollected(BigDecimal amount) {
        this.totalRentCollected = this.totalRentCollected.add(amount);
    }



    /**
     * Result of processing one month.
     */
    @Getter
    @AllArgsConstructor
    public static class MonthlyRentalResult {
        private final boolean occupied;
        private final BigDecimal rentCollected;
        private final BigDecimal totalExpenses;
        private final BigDecimal managementFee;
        private final BigDecimal netCashFlow;
    }
}
