package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class MortgageSummary {
    private final String loanType;
    private final BigDecimal houseCost;
    private final BigDecimal deposit;
    private final BigDecimal loanAmount;
    private final BigDecimal annualInterestRate;
    private final int termYears;
    private final int termMonths;
    private final BigDecimal monthlyPayment;
    private final BigDecimal totalPayment;
    private final BigDecimal totalInterest;
}
