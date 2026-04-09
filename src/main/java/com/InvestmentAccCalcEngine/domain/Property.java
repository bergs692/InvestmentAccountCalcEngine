package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Represents an owned property. Created automatically when a mortgage is taken out.
 * A Property must exist before it can be converted into a RentalProperty.
 */
@Getter
public class Property {
    private final String address;
    private final BigDecimal purchasePrice;
    private final int mortgageIndex;
    private final boolean isRental;

    private BigDecimal currentMarketValue;

    @Setter
    private int rentalIndex;

    public Property(String address, BigDecimal purchasePrice, int mortgageIndex
                    ) {
        this.address = address;
        this.purchasePrice = purchasePrice;
        this.mortgageIndex = mortgageIndex;
        this.isRental = false;
        this.rentalIndex = -1;
        this.currentMarketValue = purchasePrice;
    }

    public void applyMonthlyAppreciation(BigDecimal annualAppreciationRate) {
        BigDecimal monthlyRate = annualAppreciationRate
            .divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        currentMarketValue = currentMarketValue
            .add(currentMarketValue.multiply(monthlyRate))
            .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalAppreciation() {
        return currentMarketValue.subtract(purchasePrice);
    }

    public boolean isRentedOut() {
        return rentalIndex >= 0;
    }
}
