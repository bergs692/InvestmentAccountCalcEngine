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

    public List<Property> getAllProperties() {
      return properties;
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

    public Property getPropertyByMortgageIndex(int mortgageIndex) {
        return properties.stream()
            .filter(p -> p.getMortgageIndex() == mortgageIndex)
            .findFirst()
            .orElse(null);
    }

    public void removeProperty(int index) {
        if (index < 0 || index >= properties.size()) {
            throw new IllegalArgumentException("Invalid property index: " + index);
        }
        properties.remove(index);

        // Update mortgageIndex references for properties that shifted down
        for (int i = index; i < properties.size(); i++) {
            // Rental indices that pointed beyond the removed entry need updating too
            Property p = properties.get(i);
            if (p.getRentalIndex() >= 0) {
                // Rental index stays the same — it's managed separately
            }
        }
    }

    public void removePropertyByMortgageIndex(int mortgageIndex) {
        properties.removeIf(p -> p.getMortgageIndex() == mortgageIndex);
    }

    public void adjustRentalIndicesAfterRemoval(int removedRentalIndex) {
        for (Property p : properties) {
            if (p.getRentalIndex() > removedRentalIndex) {
                p.setRentalIndex(p.getRentalIndex() - 1);
            } else if (p.getRentalIndex() == removedRentalIndex) {
                p.setRentalIndex(-1);
                p.setRentedOut(false);
            }
        }
    }
}
