package com.InvestmentAccCalcEngine.simulator;

import com.InvestmentAccCalcEngine.service.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages save/restore of all service state for isolated simulation runs.
 *
 * Before a simulation:  {@code StateManager.saveAll()}
 * After a simulation:   {@code StateManager.restoreAll()}
 *
 * Each service must implement {@link Resettable}. If a service doesn't
 * implement it yet, this class logs a warning and skips it.
 */
@Component
public class StateManager {

    private final BankAccountService bankAccountService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    private final RentalPropertyService rentalPropertyService;
    private final NetworthService networthService;

    private Map<String, Object> savedState;

    public StateManager(BankAccountService bankAccountService,
                        MortgageService mortgageService,
                        PropertyService propertyService,
                        RentalPropertyService rentalPropertyService,
                        NetworthService networthService) {
        this.bankAccountService = bankAccountService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
        this.rentalPropertyService = rentalPropertyService;
        this.networthService = networthService;
    }

    /**
     * Snapshots the state of all Resettable services.
     * Call this BEFORE running a simulation.
     */
    public void saveAll() {
        savedState = new LinkedHashMap<>();
        snapshotService("bankAccount", bankAccountService);
        snapshotService("mortgage", mortgageService);
        snapshotService("property", propertyService);
        snapshotService("rentalProperty", rentalPropertyService);
        snapshotService("networth", networthService);
    }

    /**
     * Restores all services to their previously saved state.
     * Call this AFTER a simulation completes (or fails).
     */
    public void restoreAll() {
        if (savedState == null) {
            System.err.println("[StateManager] WARNING: restoreAll() called without a prior saveAll()");
            return;
        }
        restoreService("bankAccount", bankAccountService);
        restoreService("mortgage", mortgageService);
        restoreService("property", propertyService);
        restoreService("rentalProperty", rentalPropertyService);
        restoreService("networth", networthService);
        savedState = null;
    }

    /**
     * @return true if all required services implement Resettable
     */
    public boolean isFullySupported() {
        return (bankAccountService instanceof Resettable)
                && (mortgageService instanceof Resettable)
                && (propertyService instanceof Resettable)
                && (rentalPropertyService instanceof Resettable)
                && (networthService instanceof Resettable);
    }

    /**
     * Returns a list of service names that don't yet implement Resettable.
     */
    public java.util.List<String> getUnsupportedServices() {
        java.util.List<String> unsupported = new java.util.ArrayList<>();
        if (!(bankAccountService instanceof Resettable))    unsupported.add("BankAccountService");
        if (!(mortgageService instanceof Resettable))       unsupported.add("MortgageService");
        if (!(propertyService instanceof Resettable))       unsupported.add("PropertyService");
        if (!(rentalPropertyService instanceof Resettable)) unsupported.add("RentalPropertyService");
        if (!(networthService instanceof Resettable))       unsupported.add("NetworthService");
        return unsupported;
    }

    private void snapshotService(String name, Object service) {
        if (service instanceof Resettable resettable) {
            savedState.put(name, resettable.snapshot());
        } else {
            System.err.printf("[StateManager] WARNING: %s does not implement Resettable — state will leak%n",
                    service.getClass().getSimpleName());
        }
    }

    private void restoreService(String name, Object service) {
        if (service instanceof Resettable resettable && savedState.containsKey(name)) {
            resettable.restore(savedState.get(name));
        }
    }
}
