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

        BigDecimal depositPercent = depositPaid.divide(houseCost, 10, RoundingMode.HALF_UP);
        this.pmiRequired = depositPercent.compareTo(DOWN_PAYMENT_THRESHOLD) < 0;
        this.pmiActive = this.pmiRequired;

        if (this.pmiRequired) {
            this.monthlyPmi = originalLoanAmount
                    .multiply(PMI_ANNUAL_RATE)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else {
            this.monthlyPmi = BigDecimal.ZERO;
        }
    }

    /** Deep copy constructor for simulation state isolation. */
    public ActiveMortgage(ActiveMortgage other) {
        this.loanType = other.loanType;
        this.houseCost = other.houseCost;
        this.depositPaid = other.depositPaid;
        this.originalLoanAmount = other.originalLoanAmount;
        this.annualInterestRate = other.annualInterestRate;
        this.termYears = other.termYears;
        this.totalMonths = other.totalMonths;
        this.monthlyPayment = other.monthlyPayment;
        this.pmiRequired = other.pmiRequired;
        this.monthlyPmi = other.monthlyPmi;
        this.pmiActive = other.pmiActive;
        this.totalPmiPaid = other.totalPmiPaid;
        this.remainingBalance = other.remainingBalance;
        this.monthsPaid = other.monthsPaid;
    }

    public BigDecimal applyMonthlyPayment() {
        if (isFullyPaid()) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal interestThisMonth = remainingBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);

        BigDecimal mortgageCharge = monthlyPayment;
        BigDecimal principalPortion = mortgageCharge.subtract(interestThisMonth);

        if (principalPortion.compareTo(remainingBalance) >= 0) {
            mortgageCharge = remainingBalance.add(interestThisMonth).setScale(2, RoundingMode.HALF_UP);
            remainingBalance = BigDecimal.ZERO;
        } else {
            remainingBalance = remainingBalance.subtract(principalPortion).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal pmiCharge = BigDecimal.ZERO;
        if (pmiActive) {
            pmiCharge = monthlyPmi;
            totalPmiPaid = totalPmiPaid.add(pmiCharge);

            BigDecimal currentLtv = remainingBalance.divide(houseCost, 10, RoundingMode.HALF_UP);
            if (currentLtv.compareTo(LTV_AUTO_TERMINATE) <= 0) {
                pmiActive = false;
            }
        }

        monthsPaid++;
        return mortgageCharge.add(pmiCharge);
    }

    public void payOff() {
        this.remainingBalance = BigDecimal.ZERO;
        this.pmiActive = false;
        this.monthsPaid = this.totalMonths;
    }

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
