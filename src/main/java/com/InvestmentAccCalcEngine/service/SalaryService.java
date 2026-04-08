package com.InvestmentAccCalcEngine.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.InvestmentAccCalcEngine.service.TaxCalculator;


@Service
public class SalaryService {

    private final TaxCalculator taxCalculator;

    public SalaryService(TaxCalculator taxCalculator) {
        this.taxCalculator = taxCalculator;
    }

    public BigDecimal calculateMonthlyAfterTax(BigDecimal annualSalary) {
        BigDecimal grossMonthly = annualSalary.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyTax = taxCalculator.calculateMonthlyTax(annualSalary);
        return grossMonthly.subtract(monthlyTax);
    }
}