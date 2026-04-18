package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents an active rental property investment.
 * Tracks all income and expenses from the landlord's perspective.
 */
@Setter
@Getter
public class RentalProperty {
    private final String address;
    private BigDecimal propertyValue;
    private BigDecimal monthlyRent;

    private final BigDecimal vacancyRate;
    private BigDecimal taxAndInsurance;
    private BigDecimal maintenanceReserve;
    private final BigDecimal propertyManagementRate;
    private final BigDecimal utilitiesLandlord;
    private final BigDecimal lawnCareSnow;
    private final BigDecimal internet;
    private final BigDecimal electric;
    private BigDecimal capExReserve;
    private BigDecimal otherMiscExpenses;

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
     * Deep copy constructor for simulation state isolation.
     * Note: takes a pre-copied Property to maintain correct references.
     */
    public RentalProperty(RentalProperty other, Property copiedProperty) {
        this.address = other.address;
        this.propertyValue = other.propertyValue;
        this.monthlyRent = other.monthlyRent;
        this.vacancyRate = other.vacancyRate;
        this.taxAndInsurance = other.taxAndInsurance;
        this.maintenanceReserve = other.maintenanceReserve;
        this.propertyManagementRate = other.propertyManagementRate;
        this.utilitiesLandlord = other.utilitiesLandlord;
        this.lawnCareSnow = other.lawnCareSnow;
        this.internet = other.internet;
        this.electric = other.electric;
        this.capExReserve = other.capExReserve;
        this.otherMiscExpenses = other.otherMiscExpenses;
        this.mortgageIndex = other.mortgageIndex;
        this.property = copiedProperty; // use the already-copied Property
        this.totalRentCollected = other.totalRentCollected;
        this.totalExpensesPaid = other.totalExpensesPaid;
        this.totalVacancyLoss = other.totalVacancyLoss;
        this.monthsOwned = other.monthsOwned;
        this.vacantMonths = other.vacantMonths;
    }

    public MonthlyRentalResult processMonth() {
        monthsOwned++;

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

        BigDecimal mgmtFee = rentThisMonth.multiply(propertyManagementRate).setScale(2, RoundingMode.HALF_UP);

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

    public BigDecimal getEstimatedMonthlyCashFlow() {
        return monthlyRent.subtract(getEstimatedMonthlyExpenses());
    }

    public BigDecimal getGrossYield() {
        if (propertyValue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return monthlyRent.multiply(BigDecimal.valueOf(12))
                .divide(propertyValue, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

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
