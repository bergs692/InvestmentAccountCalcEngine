package com.InvestmentAccCalcEngine.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;
import com.InvestmentAccCalcEngine.service.SalaryService;
import com.InvestmentAccCalcEngine.service.BankAccountService;
import com.InvestmentAccCalcEngine.service.MortgageService;
import com.InvestmentAccCalcEngine.service.RentalPropertyService;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;

@Service
public class ProjectionService {

    private final SalaryService salaryService;
    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    private final RentalPropertyService rentalPropertyService;

    public ProjectionService(SalaryService salaryService, BankAccountService bankAccountService,
                             MortgageService mortgageService, RentalPropertyService rentalPropertyService) {
        this.salaryService = salaryService;
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
        this.rentalPropertyService = rentalPropertyService;
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

        // Calculate estimated monthly rental cash flow (without actually processing)
        BigDecimal monthlyRentalCashFlow = rentalPropertyService.getTotalEstimatedMonthlyCashFlow();

        for (int month = 1; month <= months; month++) {
            runningBalance = runningBalance.add(monthlyTakeHome);
            for (BigDecimal payment : monthlyPayments) {
                runningBalance = runningBalance.subtract(payment);
            }
            runningBalance = runningBalance.subtract(monthlyMortgageCost);
            runningBalance = runningBalance.add(monthlyRentalCashFlow);
            projections.add(new MonthlyProjection(month, runningBalance));
        }

        return projections;
    }
}