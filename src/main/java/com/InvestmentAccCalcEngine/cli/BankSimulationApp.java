package com.InvestmentAccCalcEngine.cli;

import com.InvestmentAccCalcEngine.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Main simulation loop. Orchestrates the month-by-month simulation
 * by delegating user interactions to {@link MenuHandler} and
 * display formatting to {@link DisplayFormatter}.
 */
@Component
public class BankSimulationApp implements CommandLineRunner {

    private final BankAccountService bankAccountService;
    private final SalaryService salaryService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    private final RentalPropertyService rentalPropertyService;
    private final NetworthService networthService;
    private final MenuHandler menuHandler;
    private final DisplayFormatter display;

    private String accountNumber;

    // Global config
    private final BigDecimal annualAppreciationRate;
    private final BigDecimal rentRate;
    private final BigDecimal salaryMeritIncreaseRate;

    public BankSimulationApp(BankAccountService bankAccountService,
                             SalaryService salaryService,
                             ProjectionService projectionService,
                             MortgageService mortgageService,
                             PropertyService propertyService,
                             RentalPropertyService rentalPropertyService,
                             NetworthService networthService,
                             MenuHandler menuHandler,
                             DisplayFormatter display) {
        this.bankAccountService = bankAccountService;
        this.salaryService = salaryService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
        this.rentalPropertyService = rentalPropertyService;
        this.networthService = networthService;
        this.menuHandler = menuHandler;
        this.display = display;

        this.annualAppreciationRate = BigDecimal.valueOf(3.5);
        this.rentRate = BigDecimal.valueOf(0.008);
        this.salaryMeritIncreaseRate = BigDecimal.valueOf(0.035);

        rentalPropertyService.setSimplifiedMode(true);
    }

    @Override
    public void run(String... args) {
        display.printWelcomeBanner();

        // ── Setup ──
        String name = "Jonah Brian Bergstrom";
        accountNumber = "5791160";
        BigDecimal startingBalance = BigDecimal.valueOf(80000);
        BigDecimal annualSalary = BigDecimal.valueOf(87000);

        bankAccountService.createAccount(accountNumber, name, startingBalance);
        BigDecimal monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
        display.printMonthlyTakeHome(monthlyTakeHome);

        int month = 1;
        int year = 1;
        int autoAdvanceRemaining = 0;

        // ── Main simulation loop ──
        while (true) {
            display.printMonthHeader(month);

            // Monthly tick: appreciation, salary, rent adjustments
            propertyService.applyMonthlyAppreciation(annualAppreciationRate);
            bankAccountService.deposit(accountNumber, monthlyTakeHome);
            display.printSalaryDeposit(monthlyTakeHome);

            if (month % 12 == 0) {
                year++;
                rentalPropertyService.applyYearlyRentIncreases(rentRate);
                annualSalary = annualSalary.multiply(BigDecimal.ONE.add(salaryMeritIncreaseRate));
            }
            monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);
            bankAccountService.deposit(accountNumber, monthlyTakeHome);

            // Mortgages
            if (mortgageService.hasActiveMortgages()) {
                BigDecimal mortgageCharged = mortgageService.processMonthlyPayments(accountNumber);
                display.printMortgagePaymentDeducted(mortgageCharged);
            }

            // Rentals
            if (rentalPropertyService.hasProperties()) {
                BigDecimal rentalNet = rentalPropertyService.processMonthlyRentals(accountNumber);
                display.printRentalNet(rentalNet);
            }

            // Track networth
            networthService.trackNetworth(accountNumber);

            menuHandler.printAccountInfo(accountNumber, month);
            display.printMenu();

            // ── Command loop within a month ──
            boolean endMonth = false;
            while (!endMonth) {
                if (autoAdvanceRemaining > 0) {
                    autoAdvanceRemaining--;
                    endMonth = true;
                } else {
                    System.out.print("> ");
                    String input = menuHandler.readLine();

                    switch (input) {
                        case "1"  -> menuHandler.handleCharge(accountNumber, month);
                        case "2"  -> menuHandler.handleDeposit(accountNumber, month);
                        case "3"  -> menuHandler.handleProjections(accountNumber, annualSalary);
                        case "4"  -> menuHandler.printAccountInfo(accountNumber, month);
                        case "5"  -> menuHandler.handleTakeOutMortgage(accountNumber);
                        case "6"  -> menuHandler.handleViewActiveMortgages();
                        case "7"  -> menuHandler.handleAddRentalProperty(rentRate);
                        case "8"  -> menuHandler.handleViewRentalProperties();
                        case "9"  -> menuHandler.handleViewOwnedProperties();
                        case "10" -> menuHandler.handleSellProperty(accountNumber);
                        case "0"  -> endMonth = true;
                        case "0*" -> {
                            System.out.print("How many months to skip ahead? ");
                            autoAdvanceRemaining = menuHandler.readInt();
                            endMonth = true;
                        }
                        case "nh" -> menuHandler.handleViewNetworthHistory();
                        case "sr" -> menuHandler.handleToggleSimplifiedRental();
                        case "q"  -> {
                            System.out.println("Exiting simulation. Goodbye!");
                            return;
                        }
                        default   -> System.out.println("Unknown command, try again.");
                    }

                    if (!endMonth) display.printMenu();
                }
            }

            month++;
        }
    }
}