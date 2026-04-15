package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MortgageService {

    private final Map<String, LoanType> loanTypes = new LinkedHashMap<>();
    private final BankAccountService bankAccountService;
    private final PropertyService propertyService;
    private final List<ActiveMortgage> activeMortgages = new ArrayList<>();

    public MortgageService(List<LoanType> loanTypeList, BankAccountService bankAccountService,
                           PropertyService propertyService) {
        this.bankAccountService = bankAccountService;
        this.propertyService = propertyService;
        for (int i = 0; i < loanTypeList.size(); i++) {
            loanTypes.put(String.valueOf(i + 1), loanTypeList.get(i));
        }
    }

    public Map<String, LoanType> getAvailableLoanTypes() {
        return loanTypes;
    }

    public ActiveMortgage getMortgage(int index) {
        return activeMortgages.get(index);
    }

    public MortgageSummary calculate(LoanType loanType, BigDecimal houseCost, BigDecimal deposit,
                                     BigDecimal annualInterestRate, int termYears) {
        BigDecimal loanAmount = houseCost.subtract(deposit);
        int termMonths = termYears * 12;

        BigDecimal monthlyPayment = loanType.calculateMonthlyPayment(loanAmount, annualInterestRate, termMonths);
        BigDecimal totalPayment = loanType.calculateTotalPayment(monthlyPayment, termMonths);
        BigDecimal totalInterest = loanType.calculateTotalInterest(loanAmount, totalPayment);

        return new MortgageSummary(
                loanType.getName(), houseCost, deposit, loanAmount,
                annualInterestRate, termYears, termMonths,
                monthlyPayment, totalPayment, totalInterest
        );
    }

    /**
     * Take out a mortgage — deducts deposit, registers mortgage, creates Property.
     */
    public ActiveMortgage takeOutMortgage(String accountNumber, LoanType loanType,
                                          BigDecimal houseCost, BigDecimal deposit,
                                          BigDecimal annualInterestRate, int termYears,
                                          String propertyAddress) {
        bankAccountService.charge(accountNumber, deposit);

        BigDecimal loanAmount = houseCost.subtract(deposit);
        int termMonths = termYears * 12;
        BigDecimal monthlyPayment = loanType.calculateMonthlyPayment(loanAmount, annualInterestRate, termMonths);

        ActiveMortgage mortgage = new ActiveMortgage(
                loanType.getName(), houseCost, deposit, loanAmount,
                annualInterestRate, termYears, monthlyPayment
        );

        activeMortgages.add(mortgage);
        int mortgageIndex = activeMortgages.size() - 1;

        // Auto-create a Property record for this purchase
        propertyService.addProperty(propertyAddress, houseCost, mortgageIndex);

        return mortgage;
    }

    public BigDecimal processMonthlyPayments(String accountNumber) {
        BigDecimal totalCharged = BigDecimal.ZERO;
        for (ActiveMortgage mortgage : activeMortgages) {
            if (!mortgage.isFullyPaid()) {
                BigDecimal payment = mortgage.applyMonthlyPayment();
                bankAccountService.charge(accountNumber, payment);
                totalCharged = totalCharged.add(payment);
            }
        }
        return totalCharged;
    }

    public List<ActiveMortgage> getActiveMortgages() {
        return activeMortgages;
    }

    public boolean hasActiveMortgages() {
        return activeMortgages.stream().anyMatch(m -> !m.isFullyPaid());
    }

    /**
     * Sell a property tied to a mortgage — pays off remaining loan balance,
     * credits net proceeds to the bank account, and removes the property.
     *
     * @return net proceeds (salePrice minus remaining balance).
     *         Negative means the seller owes money at closing.
     */
    public BigDecimal sellProperty(String accountNumber, int mortgageIndex, BigDecimal salePrice) {
        if (mortgageIndex < 0 || mortgageIndex >= activeMortgages.size()) {
            throw new IllegalArgumentException("Invalid mortgage index: " + mortgageIndex);
        }

        ActiveMortgage mortgage = activeMortgages.get(mortgageIndex);
        BigDecimal remainingBalance = mortgage.getRemainingBalance();

        BigDecimal netProceeds = salePrice.subtract(remainingBalance);

        if (netProceeds.compareTo(BigDecimal.ZERO) < 0) {
            // Seller must cover the shortfall (short sale scenario)
            bankAccountService.charge(accountNumber, netProceeds.negate());
        } else {
            // Seller pockets the equity
            bankAccountService.deposit(accountNumber, netProceeds);
        }

        // Mark mortgage as fully paid off
        mortgage.payOff();

        // Remove the associated property
        propertyService.removePropertyByMortgageIndex(mortgageIndex);

        return netProceeds;
    }

}
