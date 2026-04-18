package com.InvestmentAccCalcEngine.service;

import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.simulator.Resettable;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;

@Service
public class NetworthService implements Resettable {

    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    @Getter
    private ArrayList<BigDecimal> networthHistory = new ArrayList<BigDecimal>();
    @Getter
    private BigDecimal prevNetworth = BigDecimal.ZERO;

    public NetworthService(BankAccountService bankAccountService,
                           MortgageService mortgageService,
                           PropertyService propertyService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
    }

    public BigDecimal calculateNetworth(String accountNumber) {
        BigDecimal networth = bankAccountService.getBalance(accountNumber);

        List<Property> properties = propertyService.getAllProperties();
        for (Property p : properties) {
            ActiveMortgage mortgage = mortgageService.getMortgage(p.getMortgageIndex());
            networth = networth.add(mortgage.getEquity(p.getCurrentMarketValue()));
        }

        prevNetworth = networth;
        return prevNetworth;
    }

    public BigDecimal recalculateNetworth(String accountNumber) {
        BigDecimal networth = bankAccountService.getBalance(accountNumber);

        List<Property> properties = propertyService.getAllProperties();
        for (Property p : properties) {
            ActiveMortgage mortgage = mortgageService.getMortgage(p.getMortgageIndex());
            networth = networth.add(mortgage.getEquity(p.getCurrentMarketValue()));
        }

        prevNetworth = networth;
        return prevNetworth;
    }

    public void trackNetworth(String accountNumber){
        networthHistory.add(calculateNetworth(accountNumber));
    }

    // ── Resettable ──

    @Override
    public Object snapshot() {
        Map<String, Object> snap = new HashMap<>();
        snap.put("history", new ArrayList<>(networthHistory));
        snap.put("prevNetworth", prevNetworth);
        return snap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restore(Object state) {
        Map<String, Object> snap = (Map<String, Object>) state;
        networthHistory = (ArrayList<BigDecimal>) snap.get("history");
        prevNetworth = (BigDecimal) snap.get("prevNetworth");
    }
}
