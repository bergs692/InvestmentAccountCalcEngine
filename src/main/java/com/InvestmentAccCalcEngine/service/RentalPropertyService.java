package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.domain.RentalProperty.MonthlyRentalResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class RentalPropertyService {

    private final BankAccountService bankAccountService;
    private final List<RentalProperty> properties = new ArrayList<>();

    public RentalPropertyService(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    public void addProperty(RentalProperty property) {
        properties.add(property);
    }

    public List<RentalProperty> getProperties() {
        return properties;
    }

    public boolean hasProperties() {
        return !properties.isEmpty();
    }

    /**
     * Process all rental properties for the month.
     * Deposits net rental income (or charges net loss) to the account.
     * Returns total net cash flow across all properties (excluding mortgage — that's separate).
     */
    public BigDecimal processMonthlyRentals(String accountNumber) {
        BigDecimal totalNet = BigDecimal.ZERO;

        for (RentalProperty property : properties) {
            MonthlyRentalResult result = property.processMonth();
            totalNet = totalNet.add(result.getNetCashFlow());
        }

        // Net positive = deposit, net negative = charge
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

    /**
     * Total estimated monthly cash flow across all properties (for projections).
     */
    public BigDecimal getTotalEstimatedMonthlyCashFlow() {
        BigDecimal total = BigDecimal.ZERO;
        for (RentalProperty p : properties) {
            total = total.add(p.getEstimatedMonthlyCashFlow());
        }
        return total;
    }

    public void removeProperty(int index) {
        if (index < 0 || index >= properties.size()) {
            throw new IllegalArgumentException("Invalid rental property index: " + index);
        }
        properties.remove(index);

        // Update rentalIndex on any Property whose rentalIndex shifted
        // (handled by caller if needed)
    }
}
