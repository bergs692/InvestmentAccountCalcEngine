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


}