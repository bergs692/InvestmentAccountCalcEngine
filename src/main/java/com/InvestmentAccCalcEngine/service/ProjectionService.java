package com.InvestmentAccCalcEngine.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.InvestmentAccCalcEngine.service.SalaryService;
import com.InvestmentAccCalcEngine.service.BankAccountService;
import com.InvestmentAccCalcEngine.service.MortgageService;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;

@Service
public class ProjectionService {

    private final SalaryService salaryService;
    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;

    public ProjectionService(SalaryService salaryService, BankAccountService bankAccountService,
                             MortgageService mortgageService) {
        this.salaryService = salaryService;
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
    }

    public List<MonthlyProjection> projectBalance(String accountNumber, BigDecimal annualSalary, List<BigDecimal> monthlyPayments, int months) {
        List<MonthlyProjection> projections = new ArrayList<>();
        BigDecimal monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
        BigDecimal runningBalance = bankAccountService.getBalance(accountNumber);

        // Calculate total monthly mortgage cost for projection (without actually charging)
        BigDecimal monthlyMortgageCost = BigDecimal.ZERO;
        for (ActiveMortgage mortgage : mortgageService.getActiveMortgages()) {
            if (!mortgage.isFullyPaid()) {
                monthlyMortgageCost = monthlyMortgageCost.add(mortgage.getTotalMonthlyCharge());
            }
        }

        for (int month = 1; month <= months; month++) {
            runningBalance = runningBalance.add(monthlyTakeHome);
            for (BigDecimal payment : monthlyPayments) {
                runningBalance = runningBalance.subtract(payment);
            }
            runningBalance = runningBalance.subtract(monthlyMortgageCost);
            projections.add(new MonthlyProjection(month, runningBalance));
        }

        return projections;
    }
}