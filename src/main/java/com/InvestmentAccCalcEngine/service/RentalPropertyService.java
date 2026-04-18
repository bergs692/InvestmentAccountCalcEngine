package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.domain.RentalProperty.MonthlyRentalResult;
import com.InvestmentAccCalcEngine.simulator.Resettable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RentalPropertyService implements Resettable {

    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    @Getter
    private List<RentalProperty> properties = new ArrayList<>();

    @Getter
    @Setter
    private boolean simplifiedMode = false;

    private static final BigDecimal SIMPLIFIED_RATE = new BigDecimal("0.001");

    public RentalPropertyService(BankAccountService bankAccountService,
                                 MortgageService mortgageService,
                                 PropertyService propertyService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
    }

    public void addProperty(RentalProperty property) {
        properties.add(property);
    }

  public boolean hasProperties() {
        return !properties.isEmpty();
    }

    public BigDecimal processMonthlyRentals(String accountNumber) {
        BigDecimal totalNet = BigDecimal.ZERO;

        if (simplifiedMode) {
            for (RentalProperty property : properties) {
                BigDecimal currentValue = property.getProperty().getCurrentMarketValue();
                BigDecimal guaranteedNet = currentValue.multiply(SIMPLIFIED_RATE).setScale(2, RoundingMode.HALF_UP);

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

    // ── Resettable ──

    /**
     * Snapshots rental properties by deep-copying each one.
     * RentalProperty holds a reference to a Property — we need to
     * link the copied RentalProperty to the corresponding copied Property
     * from PropertyService's snapshot. We look them up by mortgage index.
     */
    @Override
    public Object snapshot() {
        // Build a lookup of copied properties by mortgage index
        Map<Integer, Property> copiedPropertyLookup = new HashMap<>();
        for (Property p : propertyService.getProperties()) {
            copiedPropertyLookup.put(p.getMortgageIndex(), new Property(p));
        }

        List<RentalProperty> copied = new ArrayList<>();
        for (RentalProperty rp : properties) {
            Property copiedProp = copiedPropertyLookup.getOrDefault(
                    rp.getMortgageIndex(),
                    new Property(rp.getProperty()) // fallback: copy directly
            );
            copied.add(new RentalProperty(rp, copiedProp));
        }

        Map<String, Object> snap = new HashMap<>();
        snap.put("properties", copied);
        snap.put("simplifiedMode", simplifiedMode);
        return snap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restore(Object state) {
        Map<String, Object> snap = (Map<String, Object>) state;
        properties = (List<RentalProperty>) snap.get("properties");
        simplifiedMode = (Boolean) snap.get("simplifiedMode");
    }
}
