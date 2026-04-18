package com.InvestmentAccCalcEngine.simulator.strategies;

import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import com.InvestmentAccCalcEngine.simulator.SimulationContext;
import com.InvestmentAccCalcEngine.simulator.Strategy;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Example strategy: Buy a rental property as soon as you can afford
 * the down payment, rent it out immediately, then hold forever.
 *
 * Buys up to a configurable max number of properties. Each purchase
 * targets a specific price point and puts 20% down to avoid PMI.
 */
public class BuyAndHoldStrategy implements Strategy {

    private final BigDecimal targetPropertyPrice;
    private final int maxProperties;
    private final BigDecimal interestRate;
    private final int mortgageTermYears;

    private int propertiesBought = 0;

    /**
     * @param targetPropertyPrice  price of each property to buy (e.g. 200000)
     * @param maxProperties        stop buying after this many
     * @param interestRate         annual mortgage rate (e.g. 6.5)
     * @param mortgageTermYears    loan term (e.g. 30)
     */
    public BuyAndHoldStrategy(BigDecimal targetPropertyPrice,
                              int maxProperties,
                              BigDecimal interestRate,
                              int mortgageTermYears) {
        this.targetPropertyPrice = targetPropertyPrice;
        this.maxProperties = maxProperties;
        this.interestRate = interestRate;
        this.mortgageTermYears = mortgageTermYears;
    }

    /** Convenience: $200k properties, max 3, 6.5% rate, 30yr term */
    public BuyAndHoldStrategy() {
        this(BigDecimal.valueOf(200000), 3, BigDecimal.valueOf(6.5), 30);
    }

    @Override
    public String getName() {
        return String.format("Buy & Hold (%d × $%,.0fk, %.1f%%, %dyr)",
                maxProperties, targetPropertyPrice.doubleValue() / 1000,
                interestRate.doubleValue(), mortgageTermYears);
    }

    @Override
    public void execute(SimulationContext ctx) {
        // ── Buy when we can afford 20% down + $5k cushion ──
        if (propertiesBought < maxProperties) {
            BigDecimal downPayment = targetPropertyPrice.multiply(BigDecimal.valueOf(0.20));
            BigDecimal cushion = BigDecimal.valueOf(5000);
            BigDecimal requiredBalance = downPayment.add(cushion);

            if (ctx.getBalance().compareTo(requiredBalance) >= 0) {
                tryBuyProperty(ctx, downPayment);
            }
        }

        // ── Rent out any un-rented properties ──
        for (Property prop : ctx.getPropertiesAvailableForRental()) {
            if (!prop.isRentedOut()) {
                try {
                    ctx.rentOutProperty(prop);
                } catch (Exception e) {
                    // Property might already be rented in this loop iteration
                }
            }
        }
    }

    private void tryBuyProperty(SimulationContext ctx, BigDecimal downPayment) {
        Map<String, LoanType> loanTypes = ctx.getAvailableLoanTypes();
        // Pick the first available loan type
        LoanType loanType = loanTypes.values().iterator().next();

        String address = String.format("Strategy-Property-%d (%s)",
                propertiesBought + 1, targetPropertyPrice);

        try {
            ctx.takeOutMortgage(
                    loanType,
                    targetPropertyPrice,
                    downPayment,
                    interestRate,
                    mortgageTermYears,
                    address
            );
            propertiesBought++;
        } catch (IllegalArgumentException e) {
            // Can't afford it yet or rejected — try next month
        }
    }

    @Override
    public void onComplete(SimulationContext ctx) {
        System.out.printf("  [%s] Bought %d/%d properties over %d months.%n",
                getName(), propertiesBought, maxProperties, ctx.getCurrentMonth());
    }
}
