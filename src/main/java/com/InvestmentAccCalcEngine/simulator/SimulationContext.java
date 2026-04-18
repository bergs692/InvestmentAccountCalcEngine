package com.InvestmentAccCalcEngine.simulator;

import com.InvestmentAccCalcEngine.service.*;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Provides strategies with full access to simulation state and all services.
 * This is the "toolbox" passed to every strategy each month — strategies
 * call methods here instead of touching services directly.
 */
public class SimulationContext {

    // ── Services ──
    private final BankAccountService bankAccountService;
    private final SalaryService salaryService;
    private final ProjectionService projectionService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    private final RentalPropertyService rentalPropertyService;
    private final NetworthService networthService;

    // ── State ──
    private final String accountNumber;
    private int currentMonth;
    private int currentYear;
    private BigDecimal annualSalary;
    private BigDecimal monthlyTakeHome;

    // ── Config ──
    private final BigDecimal annualAppreciationRate;
    private final BigDecimal rentRate;
    private final BigDecimal salaryMeritIncreaseRate;

    public SimulationContext(BankAccountService bankAccountService,
                             SalaryService salaryService,
                             ProjectionService projectionService,
                             MortgageService mortgageService,
                             PropertyService propertyService,
                             RentalPropertyService rentalPropertyService,
                             NetworthService networthService,
                             String accountNumber,
                             BigDecimal annualSalary,
                             BigDecimal annualAppreciationRate,
                             BigDecimal rentRate,
                             BigDecimal salaryMeritIncreaseRate) {
        this.bankAccountService = bankAccountService;
        this.salaryService = salaryService;
        this.projectionService = projectionService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
        this.rentalPropertyService = rentalPropertyService;
        this.networthService = networthService;
        this.accountNumber = accountNumber;
        this.annualSalary = annualSalary;
        this.annualAppreciationRate = annualAppreciationRate;
        this.rentRate = rentRate;
        this.salaryMeritIncreaseRate = salaryMeritIncreaseRate;
        this.currentMonth = 0;
        this.currentYear = 1;
        this.monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
    }

    // ═══════════════════════════════════════════════
    //  State Getters
    // ═══════════════════════════════════════════════

    public int getCurrentMonth()        { return currentMonth; }
    public int getCurrentYear()         { return currentYear; }
    public String getAccountNumber()    { return accountNumber; }
    public BigDecimal getAnnualSalary() { return annualSalary; }
    public BigDecimal getMonthlyTakeHome() { return monthlyTakeHome; }
    public BigDecimal getRentRate()     { return rentRate; }
    public BigDecimal getAnnualAppreciationRate() { return annualAppreciationRate; }

    public BigDecimal getBalance() {
        return bankAccountService.getBalance(accountNumber);
    }

    public BigDecimal getNetworth() {
        return networthService.getPrevNetworth();
    }

    public List<Property> getOwnedProperties() {
        return propertyService.getProperties();
    }

    public List<Property> getPropertiesAvailableForRental() {
        return propertyService.getAvailableForRental();
    }

    public List<ActiveMortgage> getActiveMortgages() {
        return mortgageService.getActiveMortgages();
    }

    public List<RentalProperty> getRentalProperties() {
        return rentalPropertyService.getProperties();
    }

    public boolean hasActiveMortgages() {
        return mortgageService.hasActiveMortgages();
    }

    public boolean hasRentalProperties() {
        return rentalPropertyService.hasProperties();
    }

    public Map<String, LoanType> getAvailableLoanTypes() {
        return mortgageService.getAvailableLoanTypes();
    }

    // ═══════════════════════════════════════════════
    //  Actions — Bank Account
    // ═══════════════════════════════════════════════

    public void charge(BigDecimal amount) {
        bankAccountService.charge(accountNumber, amount);
    }

    public void deposit(BigDecimal amount) {
        bankAccountService.deposit(accountNumber, amount);
    }

    // ═══════════════════════════════════════════════
    //  Actions — Mortgage
    // ═══════════════════════════════════════════════

    /**
     * Takes out a mortgage. Returns the created ActiveMortgage.
     * Throws IllegalArgumentException if rejected.
     */
    public ActiveMortgage takeOutMortgage(LoanType loanType,
                                          BigDecimal houseCost,
                                          BigDecimal downPayment,
                                          BigDecimal interestRate,
                                          int termYears,
                                          String propertyAddress) {
        return mortgageService.takeOutMortgage(
                accountNumber, loanType, houseCost, downPayment,
                interestRate, termYears, propertyAddress);
    }

    // ═══════════════════════════════════════════════
    //  Actions — Rental Property
    // ═══════════════════════════════════════════════

    /**
     * Sets up a rental on an owned property using auto-calculated defaults
     * (same formulas as the CLI's addRentalProperty).
     */
    public void rentOutProperty(Property property) {
        if (property.isRentedOut()) {
            throw new IllegalStateException("Property is already rented out: " + property.getAddress());
        }

        BigDecimal propertyValue = property.getPurchasePrice();
        BigDecimal monthlyRent = propertyValue.multiply(rentRate);
        BigDecimal vacancyRate = BigDecimal.valueOf(8)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxInsurance = propertyValue.multiply(BigDecimal.valueOf(0.02))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal maintenance = propertyValue.multiply(BigDecimal.valueOf(0.015))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal capex = propertyValue.multiply(BigDecimal.valueOf(0.015))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal misc = monthlyRent.multiply(BigDecimal.valueOf(0.033));

        RentalProperty rental = new RentalProperty(
                property.getAddress(), propertyValue, monthlyRent,
                vacancyRate, taxInsurance, maintenance, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                capex, misc, property.getMortgageIndex(), property
        );

        rentalPropertyService.addProperty(rental);
        int rentalIndex = rentalPropertyService.getProperties().size() - 1;
        property.setRentalIndex(rentalIndex);
    }

    /**
     * Overload: rent out with custom monthly rent and vacancy rate.
     */
    public void rentOutProperty(Property property, BigDecimal customMonthlyRent, BigDecimal customVacancyPct) {
        if (property.isRentedOut()) {
            throw new IllegalStateException("Property is already rented out: " + property.getAddress());
        }

        BigDecimal propertyValue = property.getPurchasePrice();
        BigDecimal vacancyRate = customVacancyPct
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal taxInsurance = propertyValue.multiply(BigDecimal.valueOf(0.02))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal maintenance = propertyValue.multiply(BigDecimal.valueOf(0.015))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal capex = propertyValue.multiply(BigDecimal.valueOf(0.015))
                .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
        BigDecimal misc = customMonthlyRent.multiply(BigDecimal.valueOf(0.033));

        RentalProperty rental = new RentalProperty(
                property.getAddress(), propertyValue, customMonthlyRent,
                vacancyRate, taxInsurance, maintenance, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.ZERO, BigDecimal.ZERO,
                capex, misc, property.getMortgageIndex(), property
        );

        rentalPropertyService.addProperty(rental);
        int rentalIndex = rentalPropertyService.getProperties().size() - 1;
        property.setRentalIndex(rentalIndex);
    }

    // ═══════════════════════════════════════════════
    //  Actions — Sell Property
    // ═══════════════════════════════════════════════

    /**
     * Sells a property at current market value with standard MN fees.
     * Returns the net proceeds (can be negative for short sales).
     */
    public BigDecimal sellProperty(int propertyIndex) {
        List<Property> properties = propertyService.getProperties();
        if (propertyIndex < 0 || propertyIndex >= properties.size()) {
            throw new IllegalArgumentException("Invalid property index: " + propertyIndex);
        }

        Property prop = properties.get(propertyIndex);
        BigDecimal salePrice = prop.getCurrentMarketValue();

        BigDecimal agentCommission = salePrice.multiply(new BigDecimal("0.055")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal closingCosts = salePrice.multiply(new BigDecimal("0.015")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deedTax = salePrice.multiply(new BigDecimal("0.0033")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalFees = agentCommission.add(closingCosts).add(deedTax);
        BigDecimal salePriceAfterFees = salePrice.subtract(totalFees);

        int mortgageIndex = prop.getMortgageIndex();
        BigDecimal remainingBalance = BigDecimal.ZERO;
        if (mortgageIndex >= 0) {
            remainingBalance = mortgageService.getMortgage(mortgageIndex).getRemainingBalance();
            mortgageService.sellProperty(accountNumber, mortgageIndex, salePriceAfterFees);
        } else {
            bankAccountService.deposit(accountNumber, salePriceAfterFees);
        }

        if (prop.isRentedOut() && prop.getRentalIndex() >= 0) {
            int rentalIdx = prop.getRentalIndex();
            rentalPropertyService.removeProperty(rentalIdx);
            propertyService.adjustRentalIndicesAfterRemoval(rentalIdx);
        }

        propertyService.removeProperty(propertyIndex);
        networthService.recalculateNetworth(accountNumber);

        return salePriceAfterFees.subtract(remainingBalance);
    }

    // ═══════════════════════════════════════════════
    //  Internal — Called by SimulationRunner each month
    // ═══════════════════════════════════════════════

    void advanceMonth() {
        currentMonth++;

        propertyService.applyMonthlyAppreciation(annualAppreciationRate);

        // Deposit salary (twice, matching BankSimulationApp behavior)
        bankAccountService.deposit(accountNumber, monthlyTakeHome);
        
        if (currentMonth % 12 == 0) {
            currentYear++;
            rentalPropertyService.applyYearlyRentIncreases(rentRate);
            annualSalary = annualSalary.multiply(BigDecimal.ONE.add(salaryMeritIncreaseRate));
        }
        monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
        bankAccountService.deposit(accountNumber, monthlyTakeHome);

        if (mortgageService.hasActiveMortgages()) {
            mortgageService.processMonthlyPayments(accountNumber);
        }

        if (rentalPropertyService.hasProperties()) {
            rentalPropertyService.processMonthlyRentals(accountNumber);
        }

        networthService.trackNetworth(accountNumber);
    }
}
