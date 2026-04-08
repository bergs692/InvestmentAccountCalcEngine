package com.InvestmentAccCalcEngine.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.InvestmentAccCalcEngine.service.SalaryService;
import com.InvestmentAccCalcEngine.service.BankAccountService;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
@Service
public class ProjectionService {

    private final SalaryService salaryService;
    private final BankAccountService bankAccountService;

    public ProjectionService(SalaryService salaryService, BankAccountService bankAccountService) {
        this.salaryService = salaryService;
        this.bankAccountService = bankAccountService;
    }

    public List<MonthlyProjection> projectBalance(String accountNumber, BigDecimal annualSalary, List<BigDecimal> monthlyPayments, int months) {
        List<MonthlyProjection> projections = new ArrayList<>();
        BigDecimal monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
        BigDecimal runningBalance = bankAccountService.getBalance(accountNumber);

        for (int month = 1; month <= months; month++) {
            runningBalance = runningBalance.add(monthlyTakeHome);
            for (BigDecimal payment : monthlyPayments) {
                runningBalance = runningBalance.subtract(payment);
            }
            projections.add(new MonthlyProjection(month, runningBalance));
        }

        return projections;
    }
}