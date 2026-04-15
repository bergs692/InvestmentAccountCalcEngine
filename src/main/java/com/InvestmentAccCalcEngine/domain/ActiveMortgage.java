package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class ActiveMortgage {
    private final String loanType;
    private final BigDecimal houseCost;
    private final BigDecimal depositPaid;
    private final BigDecimal originalLoanAmount;
    private final BigDecimal annualInterestRate;
    private final int termYears;
    private final int totalMonths;
    private final BigDecimal monthlyPayment;

    // --- PMI (Private Mortgage Insurance) — Minnesota rules ---
    // Required on conventional loans when down payment < 20% of house cost.
    // Rate: 0.3%–1.5% of loan amount/year (we use 0.75% as a typical MN estimate).
    // Under MN law (Minn. Stat. § 47.205), borrower can request cancellation at 80% LTV
    // based on current market value (more favorable than federal which uses original value).
    // Under federal Homeowners Protection Act, PMI auto-terminates at 78% LTV.
    private static final BigDecimal PMI_ANNUAL_RATE = new BigDecimal("0.75");
    private static final BigDecimal DOWN_PAYMENT_THRESHOLD = new BigDecimal("0.20");
    private static final BigDecimal LTV_CANCEL_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal LTV_AUTO_TERMINATE = new BigDecimal("0.78");

    private final boolean pmiRequired;
    private final BigDecimal monthlyPmi;
    private boolean pmiActive;
    private BigDecimal totalPmiPaid;

    @Setter
    private BigDecimal remainingBalance;
    private int monthsPaid;

    public ActiveMortgage(String loanType, BigDecimal houseCost, BigDecimal depositPaid,
                          BigDecimal originalLoanAmount, BigDecimal annualInterestRate,
                          int termYears, BigDecimal monthlyPayment) {
        this.loanType = loanType;
        this.houseCost = houseCost;
        this.depositPaid = depositPaid;
        this.originalLoanAmount = originalLoanAmount;
        this.annualInterestRate = annualInterestRate;
        this.termYears = termYears;
        this.totalMonths = termYears * 12;
        this.monthlyPayment = monthlyPayment;
        this.remainingBalance = originalLoanAmount;
        this.monthsPaid = 0;
        this.totalPmiPaid = BigDecimal.ZERO;

        // PMI required if down payment < 20% of house cost
        BigDecimal depositPercent = depositPaid.divide(houseCost, 10, RoundingMode.HALF_UP);
        this.pmiRequired = depositPercent.compareTo(DOWN_PAYMENT_THRESHOLD) < 0;
        this.pmiActive = this.pmiRequired;

        if (this.pmiRequired) {
            // Monthly PMI = (loan amount * 0.75%) / 12
            this.monthlyPmi = originalLoanAmount
                    .multiply(PMI_ANNUAL_RATE)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else {
            this.monthlyPmi = BigDecimal.ZERO;
        }
    }

    /**
     * Apply one month's payment. Returns the total amount charged
     * (mortgage payment + PMI if active).
     */
    public BigDecimal applyMonthlyPayment() {
        if (isFullyPaid()) {
            return BigDecimal.ZERO;
        }

        // Calculate this month's interest
        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal interestThisMonth = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        // Mortgage payment portion
        BigDecimal mortgageCharge = monthlyPayment;
        BigDecimal principalPortion = mortgageCharge.subtract(interestThisMonth);

        if (principalPortion.compareTo(remainingBalance) >= 0) {
            // Final payment — just pay off the rest plus this month's interest
            mortgageCharge = remainingBalance.add(interestThisMonth).setScale(2, RoundingMode.HALF_UP);
            remainingBalance = BigDecimal.ZERO;
        } else {
            remainingBalance = remainingBalance.subtract(principalPortion).setScale(2, RoundingMode.HALF_UP);
        }

        // PMI charge (if still active)
        BigDecimal pmiCharge = BigDecimal.ZERO;
        if (pmiActive) {
            pmiCharge = monthlyPmi;
            totalPmiPaid = totalPmiPaid.add(pmiCharge);

            // Check if PMI should auto-terminate (federal: 78% LTV of original value)
            BigDecimal currentLtv = remainingBalance.divide(houseCost, 10, RoundingMode.HALF_UP);
            if (currentLtv.compareTo(LTV_AUTO_TERMINATE) <= 0) {
                pmiActive = false;
            }
        }

        monthsPaid++;
        return mortgageCharge.add(pmiCharge);
    }

    /**
     * Pay off the mortgage in full (e.g. when selling the property).
     */
    public void payOff() {
        this.remainingBalance = BigDecimal.ZERO;
        this.pmiActive = false;
        this.monthsPaid = this.totalMonths;
    }

    /**
     * Minnesota law allows borrowers to request PMI cancellation at 80% LTV
     * based on the home's current market value (not just original purchase price).
     * Call this to manually cancel PMI if you believe you've hit 80% LTV.
     */
    public boolean requestPmiCancellation() {
        if (!pmiActive) {
            return false;
        }
        BigDecimal currentLtv = remainingBalance.divide(houseCost, 10, RoundingMode.HALF_UP);
        if (currentLtv.compareTo(LTV_CANCEL_THRESHOLD) <= 0) {
            pmiActive = false;
            return true;
        }
        return false;
    }

    /**
     * Total monthly cost including PMI (if active).
     */
    public BigDecimal getTotalMonthlyCharge() {
        return monthlyPayment.add(pmiActive ? monthlyPmi : BigDecimal.ZERO);
    }

    public boolean isFullyPaid() {
        return remainingBalance.compareTo(BigDecimal.ZERO) <= 0;
    }

    public int getMonthsRemaining() {
        return totalMonths - monthsPaid;
    }

    public BigDecimal getEquity(BigDecimal currentMarketValue) {
        return currentMarketValue.subtract(remainingBalance);
    }

    public BigDecimal getCurrentLtv(BigDecimal currentMarketValue) {
        return remainingBalance.divide(currentMarketValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }
}
