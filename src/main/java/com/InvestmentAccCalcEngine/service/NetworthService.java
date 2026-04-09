package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.Property;
import lombok.Getter;
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

    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    @Getter
    private final ArrayList<BigDecimal> networthHistory = new ArrayList<BigDecimal>();
    @Getter
    private BigDecimal prevNetworth = BigDecimal.ZERO;

    public NetworthService(SalaryService salaryService, BankAccountService bankAccountService,
                           MortgageService mortgageService, RentalPropertyService rentalPropertyService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
    }

    public BigDecimal calculateNetworth(String accountNumber){
        BigDecimal networth =  bankAccountService.getBalance(accountNumber);

        BigDecimal equityTotal = BigDecimal.ZERO;
        List <ActiveMortgage> mortgages = mortgageService.getActiveMortgages();

        for (ActiveMortgage AM : mortgages){
            equityTotal = equityTotal.add(AM.getEquity());
        }
        prevNetworth = networth.add(equityTotal);
        return prevNetworth;
    }

    public void trackNetworth(String accountNumber){
        networthHistory.add(calculateNetworth(accountNumber));
    }

}