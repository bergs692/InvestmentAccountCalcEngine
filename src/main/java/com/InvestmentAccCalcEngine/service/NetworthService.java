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
public class NetworthService {

    private final SalaryService salaryService;
    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    private final RentalPropertyService rentalPropertyService;

    public NetworthService(SalaryService salaryService, BankAccountService bankAccountService,
                             MortgageService mortgageService, RentalPropertyService rentalPropertyService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
        this.rentalPropertyService = rentalPropertyService;
    }

    public BigDecimal calculateNetworth(String accountNumber){
        BigDecimal networth =  bankAccountService.getBalance(accountNumber);

        BigDecimal mortgageValue = BigDecimal.ZERO;
        for (ActiveMortgage mortgage : mortgageService.getActiveMortgages()) {
            if (!mortgage.isFullyPaid()) {
                monthlyMortgageCost = monthlyMortgageCost.add(mortgage.getTotalMonthlyCharge());
            }
        }
    }


}