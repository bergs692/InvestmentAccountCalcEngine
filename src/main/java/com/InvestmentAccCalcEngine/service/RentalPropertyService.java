package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.domain.RentalProperty.MonthlyRentalResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class RentalPropertyService {

    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    @Getter
    private final List<RentalProperty> properties = new ArrayList<>();

    /**
     * Simplified rental mode: guarantees ~$100 net income per $100k of property value per month,
     * AFTER all expenses and mortgage payments. Skips normal expense/vacancy calculations.
     */
    @Getter
    @Setter
    private boolean simplifiedMode = false;

    private static final BigDecimal SIMPLIFIED_RATE = new BigDecimal("0.001");
    // $100 per $100,000 = $0.001 per $1 of property value

    public RentalPropertyService(BankAccountService bankAccountService, MortgageService mortgageService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
    }

    public void addProperty(RentalProperty property) {
        properties.add(property);
    }

  public boolean hasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Process all rental properties for the month.
     * In simplified mode: flat $100 per $100k of current property value (after mortgage).
     * In normal mode: full expense/vacancy simulation.
     */
    public BigDecimal processMonthlyRentals(String accountNumber) {
        BigDecimal totalNet = BigDecimal.ZERO;

        if (simplifiedMode) {
            for (RentalProperty property : properties) {
                BigDecimal currentValue = property.getProperty().getCurrentMarketValue();
                BigDecimal guaranteedNet = currentValue.multiply(SIMPLIFIED_RATE).setScale(2, RoundingMode.HALF_UP);

                // Add back the mortgage payment so the final net (after mortgage is deducted
                // separately in the month loop) equals the guaranteed amount
                int mortgageIdx = property.getMortgageIndex();
                if (mortgageIdx >= 0) {
                    ActiveMortgage mortgage = mortgageService.getMortgage(mortgageIdx);
                    if (!mortgage.isFullyPaid()) {
                        guaranteedNet = guaranteedNet.add(mortgage.getTotalMonthlyCharge());
                    }
                }

                totalNet = totalNet.add(guaranteedNet);
                property.incrementMonthsOwned();
                property.addToTotalRentCollected(guaranteedNet);
            }
        } else {
            for (RentalProperty property : properties) {
                MonthlyRentalResult result = property.processMonth();
                totalNet = totalNet.add(result.getNetCashFlow());
            }
        }

        if (totalNet.compareTo(BigDecimal.ZERO) > 0) {
            bankAccountService.deposit(accountNumber, totalNet);
        } else if (totalNet.compareTo(BigDecimal.ZERO) < 0) {
            bankAccountService.charge(accountNumber, totalNet.negate());
        }

        return totalNet;
    }

    public void applyYearlyRentIncreases(BigDecimal rentRate) {
        for (RentalProperty p : properties) {
            BigDecimal propertyValue = p.getProperty().getCurrentMarketValue();
            BigDecimal newRent = propertyValue.multiply(rentRate);
            p.setMonthlyRent(newRent);
            p.setPropertyValue(propertyValue);
            p.recalculateExpensesFromValue(propertyValue, newRent);
        }
    }

    public BigDecimal getTotalEstimatedMonthlyCashFlow() {
        BigDecimal total = BigDecimal.ZERO;
        for (RentalProperty p : properties) {
            if (simplifiedMode) {
                total = total.add(p.getProperty().getCurrentMarketValue()
                    .multiply(SIMPLIFIED_RATE).setScale(2, RoundingMode.HALF_UP));
            } else {
                total = total.add(p.getEstimatedMonthlyCashFlow());
            }
        }
        return total;
    }

    public void removeProperty(int index) {
        if (index < 0 || index >= properties.size()) {
            throw new IllegalArgumentException("Invalid rental property index: " + index);
        }
        properties.remove(index);
    }
}