package com.InvestmentAccCalcEngine.simulator;

import com.InvestmentAccCalcEngine.service.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a {@link Strategy} for a configurable number of months,
 * collecting monthly snapshots and producing a {@link SimulationResult}.
 *
 * State isolation: saves all service state before the run and restores
 * it after, so simulations never affect the live CLI session.
 */
@Component
public class SimulationRunner {

    private final BankAccountService bankAccountService;
    private final SalaryService salaryService;
    private final ProjectionService projectionService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    private final RentalPropertyService rentalPropertyService;
    private final NetworthService networthService;
    private final StateManager stateManager;

    public SimulationRunner(BankAccountService bankAccountService,
                            SalaryService salaryService,
                            ProjectionService projectionService,
                            MortgageService mortgageService,
                            PropertyService propertyService,
                            RentalPropertyService rentalPropertyService,
                            NetworthService networthService,
                            StateManager stateManager) {
        this.bankAccountService = bankAccountService;
        this.salaryService = salaryService;
        this.projectionService = projectionService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
        this.rentalPropertyService = rentalPropertyService;
        this.networthService = networthService;
        this.stateManager = stateManager;
    }

    /**
     * Runs the given strategy for the specified number of months.
     * State is saved before and restored after — the live session is untouched.
     *
     * @param strategy       the strategy to execute
     * @param months         how many months to simulate
     * @param accountNumber  the bank account to operate on
     * @param annualSalary   starting annual salary
     * @param appreciationRate annual property appreciation (e.g. 3.5 for 3.5%)
     * @param rentRate       monthly rent rate (e.g. 0.008)
     * @param meritRate      annual salary increase (e.g. 0.035 for 3.5%)
     * @return               a SimulationResult with full run data
     */
    public SimulationResult run(Strategy strategy,
                                int months,
                                String accountNumber,
                                BigDecimal annualSalary,
                                BigDecimal appreciationRate,
                                BigDecimal rentRate,
                                BigDecimal meritRate) {

        // ── Save live state ──
        stateManager.saveAll();

        try {
            return runIsolated(strategy, months, accountNumber, annualSalary,
                    appreciationRate, rentRate, meritRate);
        } finally {
            // ── Always restore, even if the strategy throws ──
            stateManager.restoreAll();
        }
    }

    private SimulationResult runIsolated(Strategy strategy,
                                         int months,
                                         String accountNumber,
                                         BigDecimal annualSalary,
                                         BigDecimal appreciationRate,
                                         BigDecimal rentRate,
                                         BigDecimal meritRate) {

        SimulationContext ctx = new SimulationContext(
                bankAccountService, salaryService, projectionService,
                mortgageService, propertyService, rentalPropertyService,
                networthService, accountNumber, annualSalary,
                appreciationRate, rentRate, meritRate
        );

        BigDecimal startingBalance = ctx.getBalance();
        BigDecimal startingNetworth = ctx.getNetworth();

        List<SimulationResult.MonthSnapshot> snapshots = new ArrayList<>();
        List<String> eventLog = new ArrayList<>();

        // ── Strategy initialization ──
        try {
            strategy.onStart(ctx);
            eventLog.add("Month 0: Strategy '" + strategy.getName() + "' initialized");
        } catch (Exception e) {
            eventLog.add("Month 0: ERROR in onStart - " + e.getMessage());
        }

        // ── Main simulation loop ──
        for (int m = 1; m <= months; m++) {
            ctx.advanceMonth();

            try {
                strategy.execute(ctx);
            } catch (Exception e) {
                eventLog.add("Month " + m + ": ERROR - " + e.getMessage());
            }

            snapshots.add(new SimulationResult.MonthSnapshot(
                    m,
                    ctx.getBalance(),
                    ctx.getNetworth(),
                    ctx.getOwnedProperties().size()
            ));

            if (m % 12 == 0) {
                eventLog.add(String.format("Month %d (Year %d): Balance=$%,.2f  Networth=$%,.2f  Properties=%d",
                        m, m / 12, ctx.getBalance(), ctx.getNetworth(), ctx.getOwnedProperties().size()));
            }
        }

        // ── Strategy cleanup ──
        try {
            strategy.onComplete(ctx);
        } catch (Exception e) {
            eventLog.add("onComplete: ERROR - " + e.getMessage());
        }

        return new SimulationResult(
                strategy.getName(),
                months,
                startingBalance,
                ctx.getBalance(),
                startingNetworth,
                ctx.getNetworth(),
                ctx.getOwnedProperties().size(),
                ctx.getRentalProperties().size(),
                ctx.getActiveMortgages().size(),
                snapshots,
                eventLog
        );
    }

    /**
     * Convenience overload using the default config values.
     */
    public SimulationResult run(Strategy strategy, int months, String accountNumber, BigDecimal annualSalary) {
        return run(strategy, months, accountNumber, annualSalary,
                BigDecimal.valueOf(3.5),
                BigDecimal.valueOf(0.008),
                BigDecimal.valueOf(0.035));
    }
}
