package com.InvestmentAccCalcEngine.domain;

import lombok.*;
import java.math.BigDecimal;

/**
 * Represents an owned property. Created automatically when a mortgage is taken out.
 * A Property must exist before it can be converted into a RentalProperty.
 */
@Getter
public class Property {
    private final String address;
    private final BigDecimal purchasePrice;
    private final int mortgageIndex;  // index into MortgageService's activeMortgages list
    private final boolean isRental;

    @Setter
    private int rentalIndex;  // index into RentalPropertyService's properties list, -1 if not rented

    public Property(String address, BigDecimal purchasePrice, int mortgageIndex) {
        this.address = address;
        this.purchasePrice = purchasePrice;
        this.mortgageIndex = mortgageIndex;
        this.isRental = false;
        this.rentalIndex = -1;
    }

    public boolean isRentedOut() {
        return rentalIndex >= 0;
    }
}
