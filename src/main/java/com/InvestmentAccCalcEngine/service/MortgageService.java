package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MortgageService {

    private final Map<String, LoanType> loanTypes = new LinkedHashMap<>();

    /**
     * Spring auto-injects all LoanType implementations.
     * To add a new loan type, just create a new class implementing LoanType
     * and annotate it with @Component — it'll appear here automatically.
     */
    public MortgageService(List<LoanType> loanTypeList) {
        for (int i = 0; i < loanTypeList.size(); i++) {
            loanTypes.put(String.valueOf(i + 1), loanTypeList.get(i));
        }
    }

    public Map<String, LoanType> getAvailableLoanTypes() {
        return loanTypes;
    }

    public MortgageSummary calculate(LoanType loanType, BigDecimal houseCost, BigDecimal deposit,
                                     BigDecimal annualInterestRate, int termYears) {
        BigDecimal loanAmount = houseCost.subtract(deposit);
        int termMonths = termYears * 12;

        BigDecimal monthlyPayment = loanType.calculateMonthlyPayment(loanAmount, annualInterestRate, termMonths);
        BigDecimal totalPayment = loanType.calculateTotalPayment(monthlyPayment, termMonths);
        BigDecimal totalInterest = loanType.calculateTotalInterest(loanAmount, totalPayment);

        return new MortgageSummary(
                loanType.getName(),
                houseCost,
                deposit,
                loanAmount,
                annualInterestRate,
                termYears,
                termMonths,
                monthlyPayment,
                totalPayment,
                totalInterest
        );
    }
}
