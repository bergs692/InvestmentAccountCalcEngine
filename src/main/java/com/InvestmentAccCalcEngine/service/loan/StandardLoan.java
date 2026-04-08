package com.InvestmentAccCalcEngine.service.loan;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Standard repayment mortgage (annuity loan).
 * Fixed monthly payments where each payment covers interest + a portion of principal.
 * Formula: M = P * [r(1+r)^n] / [(1+r)^n - 1]
 */
@Component("standardLoan")
public class StandardLoan implements LoanType {

    @Override
    public String getName() {
        return "Standard Repayment";
    }

    @Override
    public BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualInterestRate, int termMonths) {
        // Monthly interest rate = annual rate / 12 / 100
        BigDecimal monthlyRate = annualInterestRate
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            // 0% interest — just divide principal by months
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }

        // (1 + r)^n
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = onePlusR.pow(termMonths, MathContext.DECIMAL128);

        // M = P * [r * (1+r)^n] / [(1+r)^n - 1]
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(power);
        BigDecimal denominator = power.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTotalPayment(BigDecimal monthlyPayment, int termMonths) {
        return monthlyPayment.multiply(BigDecimal.valueOf(termMonths)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal totalPayment) {
        return totalPayment.subtract(principal).setScale(2, RoundingMode.HALF_UP);
    }
}
