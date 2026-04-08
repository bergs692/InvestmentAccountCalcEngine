package com.InvestmentAccCalcEngine.service.loan;

import java.math.BigDecimal;

/**
 * Strategy interface for different loan repayment types.
 * Implement this interface to add new loan types (e.g. interest-only, tracker, fixed+variable).
 */
public interface LoanType {

    /**
     * @return Display name for this loan type (e.g. "Standard Repayment")
     */
    String getName();

    /**
     * Calculate the fixed monthly repayment amount.
     */
    BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualInterestRate, int termMonths);

    /**
     * Calculate the total amount paid over the full term.
     */
    BigDecimal calculateTotalPayment(BigDecimal monthlyPayment, int termMonths);

    /**
     * Calculate total interest paid over the full term.
     */
    BigDecimal calculateTotalInterest(BigDecimal principal, BigDecimal totalPayment);
}
