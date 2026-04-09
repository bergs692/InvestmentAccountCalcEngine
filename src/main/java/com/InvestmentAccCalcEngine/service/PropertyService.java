package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.Property;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Service
public class PropertyService {

    private final List<Property> properties = new ArrayList<>();

    public Property addProperty(String address, BigDecimal purchasePrice, int mortgageIndex) {
        Property property = new Property(address, purchasePrice, mortgageIndex);
        properties.add(property);
        return property;
    }

  public List<Property> getAvailableForRental() {
        return properties.stream()
                .filter(p -> !p.isRentedOut())
                .toList();
    }

    public void applyMonthlyAppreciation(BigDecimal annualAppreciationRate){
        List<Property> properties = getProperties();
        for (Property prop : properties){
            prop.applyMonthlyAppreciation(annualAppreciationRate);
        }
    };

    public boolean hasProperties() {
        return !properties.isEmpty();
    }
}
