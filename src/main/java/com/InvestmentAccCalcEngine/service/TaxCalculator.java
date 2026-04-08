package com.InvestmentAccCalcEngine.service;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TaxCalculator {

    public BigDecimal calculateMonthlyTax(BigDecimal annualSalary) {
        BigDecimal taxRate = getTaxRate(annualSalary);
        BigDecimal annualTax = annualSalary.multiply(taxRate);
        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal getTaxRate(BigDecimal annualSalary) {
        // UK tax bands 2024
        if (annualSalary.compareTo(BigDecimal.valueOf(12570)) <= 0) {
            // Personal allowance - no tax
            return BigDecimal.valueOf(0.00);
        } else if (annualSalary.compareTo(BigDecimal.valueOf(50270)) <= 0) {
            // Basic rate
            return BigDecimal.valueOf(0.20);
        } else if (annualSalary.compareTo(BigDecimal.valueOf(125140)) <= 0) {
            // Higher rate
            return BigDecimal.valueOf(0.40);
        } else {
            // Additional rate
            return BigDecimal.valueOf(0.45);
        }
    }
}