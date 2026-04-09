package com.InvestmentAccCalcEngine.cli;

import com.InvestmentAccCalcEngine.service.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import com.InvestmentAccCalcEngine.service.NetworthService;


@Component
public class BankSimulationApp implements CommandLineRunner {

    private final BankAccountService bankAccountService;
    private final SalaryService salaryService;
    private final ProjectionService projectionService;
    private final MortgageService mortgageService;
    private final PropertyService propertyService;
    private final RentalPropertyService rentalPropertyService;
    private final NetworthService networthService;

    private final Scanner scanner = new Scanner(System.in);
    private String accountNumber;


    private final BigDecimal annualAppreciationRate; // e.g. 3.5 for 3.5%
    private final BigDecimal rentRate; // e.g. 0.008 for 8%
    private final BigDecimal salaryMeritIncreaseRate; // e.g. 0.035 for 3.5%


    public BankSimulationApp(BankAccountService bankAccountService,
                             SalaryService salaryService,
                             ProjectionService projectionService,
                             MortgageService mortgageService,
                             PropertyService propertyService,
                             RentalPropertyService rentalPropertyService,
                             NetworthService networthService) {
        this.bankAccountService = bankAccountService;
        this.salaryService = salaryService;
        this.projectionService = projectionService;
        this.mortgageService = mortgageService;
        this.propertyService = propertyService;
        this.rentalPropertyService = rentalPropertyService;
        this.networthService = networthService;

        // Global Variables (Config)
        this.annualAppreciationRate = BigDecimal.valueOf(3.5); // e.g. 3.5 for 3.5%
        this.rentRate = BigDecimal.valueOf(0.008); // e.g. 0.008 for 8%
        this.salaryMeritIncreaseRate = BigDecimal.valueOf(0.035); // e.g. 0.035 for 3.5%
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Bank Account Simulator ===");

        // Setup
        System.out.println("Enter your name: ");
        //String name = scanner.nextLine();
        String name = "Jonah Brian Bergstrom";

        System.out.println("Enter account number: ");
        //accountNumber = scanner.nextLine();
        accountNumber = "5791160";

        System.out.println("Enter starting balance: ");
        //BigDecimal startingBalance = scanner.nextBigDecimal();
        BigDecimal startingBalance = BigDecimal.valueOf(16000);
        //scanner.nextLine();

        System.out.println("Enter your annual salary: ");
        //BigDecimal annualSalary = scanner.nextBigDecimal();
        BigDecimal annualSalary = BigDecimal.valueOf(87000);
        //scanner.nextLine();

        bankAccountService.createAccount(accountNumber, name, startingBalance);
        BigDecimal monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);

        System.out.printf("%nMonthly take home pay: $%.2f%n", monthlyTakeHome);

        // Month loop
        int month = 1;

        // Year loop calculated by modding month by 12.
        int year = 1;

        // Auto advancing with 0* case variable
        int autoAdvanceRemaining = 0;

        while (true) {
            System.out.println("\n=============================");
            System.out.println(" Month " + month);
            System.out.println("=============================");

            // Apply Monthly Property Appreciation
            propertyService.applyMonthlyAppreciation(annualAppreciationRate);

            // Apply Yearly Changes
            if (month % 12 == 0){
                year += 1;

                // Adjust Rent Payment if year has gone by.
                rentalPropertyService.applyYearlyRentIncreases(rentRate);
                // Adjust Salary
                annualSalary = annualSalary.multiply(BigDecimal.valueOf(1).add(salaryMeritIncreaseRate));
            }

            // Apply monthly salary
            bankAccountService.deposit(accountNumber, monthlyTakeHome);

            // Auto-charge active mortgages
            if (mortgageService.hasActiveMortgages()) {
                BigDecimal mortgageCharged = mortgageService.processMonthlyPayments(accountNumber);
                System.out.printf("  Mortgage payment deducted: $%,.2f%n", mortgageCharged);
            }





            // Auto-process rental properties (income - expenses)
            if (rentalPropertyService.hasProperties()) {
                BigDecimal rentalNet = rentalPropertyService.processMonthlyRentals(accountNumber);
                if (rentalNet.compareTo(BigDecimal.ZERO) >= 0) {
                    System.out.printf("  Rental income (net):       +$%,.2f%n", rentalNet);
                } else {
                    System.out.printf("  Rental expenses (net):     -$%,.2f%n", rentalNet.negate());
                }
            }



            // Calculate New Networth and track it.
            networthService.trackNetworth(accountNumber);

            printAccountInfo(month);
            printMenu();

            boolean endMonth = false;
            while (!endMonth) {
                if (autoAdvanceRemaining > 0) {
                    autoAdvanceRemaining--;
                    endMonth = true;
                } else {
                    System.out.print("> ");
                    String input = scanner.nextLine().trim();

                    switch (input) {
                        case "1" -> {
                            System.out.print("Charge amount: ");
                            BigDecimal amount = scanner.nextBigDecimal();
                            scanner.nextLine();
                            System.out.print("Description: ");
                            String desc = scanner.nextLine();
                            bankAccountService.charge(accountNumber, amount);
                            System.out.printf("Charged $%.2f for %s%n", amount, desc);
                            printAccountInfo(month);
                        }
                        case "2" -> {
                            System.out.print("Deposit amount: ");
                            BigDecimal amount = scanner.nextBigDecimal();
                            scanner.nextLine();
                            bankAccountService.deposit(accountNumber, amount);
                            System.out.printf("Deposited $%.2f%n", amount);
                            printAccountInfo(month);
                        }
                        case "3" -> {
                            System.out.print("Project how many months ahead? ");
                            int months = scanner.nextInt();
                            scanner.nextLine();
                            System.out.print("Enter total monthly payments/outgoings: ");
                            BigDecimal payments = scanner.nextBigDecimal();
                            scanner.nextLine();
                            List<MonthlyProjection> projections = projectionService.projectBalance(
                                accountNumber, annualSalary, List.of(payments), months);
                            System.out.println("\n--- Projection ---");
                            projections.forEach(p -> System.out.printf(
                                "Month %d: $%.2f%n", p.getMonth(), p.getProjectedBalance()));
                        }
                        case "4" -> printAccountInfo(month);
                        case "5" -> takeOutMortgage();
                        case "6" -> viewActiveMortgages();
                        case "7" -> addRentalProperty();
                        case "8" -> viewRentalProperties();
                        case "9" -> viewOwnedProperties();
                        case "0" -> endMonth = true;
                        case "0*" -> {
                            System.out.print("How many months to skip ahead? ");
                            autoAdvanceRemaining = scanner.nextInt();
                            scanner.nextLine();
                            endMonth = true;
                        }
                        case "nh" -> viewNetworthHistory();
                        case "q" -> {
                            System.out.println("Exiting simulation. Goodbye!");
                            return;
                        }
                        default -> System.out.println("Unknown command, try again.");
                    }

                    if (!endMonth) printMenu();
                }
            }

            month++;
        }
    }

    private void printAccountInfo(int month) {
        BigDecimal balance = bankAccountService.getBalance(accountNumber);
        BigDecimal networth = networthService.getPrevNetworth();
        System.out.println("\n--- Account Summary (Month " + month + ") ---");
        System.out.printf("Balance: $%.2f%n", balance);
        System.out.printf("Networth: $%.2f%n", networth);
    }

    private void printMenu() {
        System.out.println("\nWhat would you like to do?");
        System.out.println("  1 - Make a charge");
        System.out.println("  2 - Make a deposit");
        System.out.println("  3 - View future projections");
        System.out.println("  4 - View account info");
        System.out.println("  5 - Take out a mortgage");
        System.out.println("  6 - View active mortgages");
        System.out.println("  7 - Rent out a property");
        System.out.println("  8 - View rental properties");
        System.out.println("  9 - View owned properties");
        System.out.println("  0 - Advance to next month");
        System.out.println("  q - Quit");
        System.out.println("  nh - View Networth History");
    }

    private void takeOutMortgage() {
        System.out.println("\n--- Take Out a Mortgage ---");

        System.out.print("Property address: ");
        String propertyAddress = scanner.nextLine();

        // Select loan type
        Map<String, LoanType> loanTypes = mortgageService.getAvailableLoanTypes();
        System.out.println("Select loan type:");
        loanTypes.forEach((key, type) -> System.out.println("  " + key + " - " + type.getName()));
        System.out.print("> ");
        String typeChoice = scanner.nextLine().trim();
        LoanType selectedType = loanTypes.get(typeChoice);
        if (selectedType == null) {
            System.out.println("Invalid loan type.");
            return;
        }

        System.out.print("House cost ($): ");
        BigDecimal houseCost = scanner.nextBigDecimal();
        scanner.nextLine();

        System.out.print("Deposit amount ($): ");
        BigDecimal deposit = scanner.nextBigDecimal();
        scanner.nextLine();

        System.out.print("Annual interest rate (%): ");
        BigDecimal interestRate = scanner.nextBigDecimal();
        scanner.nextLine();

        System.out.print("Mortgage term (years): ");
        int termYears = scanner.nextInt();
        scanner.nextLine();

        // Show summary before confirming
        MortgageSummary summary = mortgageService.calculate(selectedType, houseCost, deposit, interestRate, termYears);

        System.out.println("\n===== Mortgage Summary =====");
        System.out.printf("  Loan type:          %s%n", summary.getLoanType());
        System.out.printf("  House cost:         $%,.2f%n", summary.getHouseCost());
        System.out.printf("  Deposit:            $%,.2f%n", summary.getDeposit());
        System.out.printf("  Loan amount:        $%,.2f%n", summary.getLoanAmount());
        System.out.printf("  Interest rate:      %.2f%%%n", summary.getAnnualInterestRate());
        System.out.printf("  Term:               %d years (%d months)%n", summary.getTermYears(), summary.getTermMonths());
        System.out.println("-----------------------------");
        System.out.printf("  Monthly payment:    $%,.2f%n", summary.getMonthlyPayment());
        System.out.printf("  Total repayment:    $%,.2f%n", summary.getTotalPayment());
        System.out.printf("  Total interest:     $%,.2f%n", summary.getTotalInterest());
        System.out.println("=============================");

        System.out.printf("%nThis will deduct $%,.2f deposit from your account.%n", deposit);
        System.out.print("Confirm? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (!confirm.equals("y")) {
            System.out.println("Mortgage cancelled.");
            return;
        }

        try {
            ActiveMortgage mortgage = mortgageService.takeOutMortgage(
                    accountNumber, selectedType, houseCost, deposit, interestRate, termYears, propertyAddress);
            System.out.println("\nMortgage approved! Property registered at: " + propertyAddress);
            System.out.printf("  Deposit of $%,.2f deducted from account.%n", deposit);
            System.out.printf("  $%,.2f mortgage payment each month for %d months.%n",
                    mortgage.getMonthlyPayment(), mortgage.getTotalMonths());
            if (mortgage.isPmiRequired()) {
                System.out.println("\n  *** PMI (Private Mortgage Insurance) Required ***");
                System.out.println("  Down payment is less than 20% — per Minnesota law,");
                System.out.println("  PMI at 0.75% of loan amount/year will be charged monthly.");
                System.out.printf("  Monthly PMI:          $%,.2f%n", mortgage.getMonthlyPmi());
                System.out.printf("  Total monthly charge: $%,.2f (mortgage + PMI)%n", mortgage.getTotalMonthlyCharge());
                System.out.println("  PMI auto-terminates at 78% LTV (federal law).");
                System.out.println("  Under MN law, you can request cancellation at 80% LTV.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Mortgage rejected: " + e.getMessage());
        }
    }

    private void viewActiveMortgages() {
        List<ActiveMortgage> mortgages = mortgageService.getActiveMortgages();
        if (mortgages.isEmpty()) {
            System.out.println("\nNo active mortgages.");
            return;
        }

        System.out.println("\n===== Active Mortgages =====");
        for (int i = 0; i < mortgages.size(); i++) {
            ActiveMortgage m = mortgages.get(i);
            System.out.printf("%n  Mortgage %d: %s%n", i + 1, m.getLoanType());
            System.out.printf("    House cost:        $%,.2f%n", m.getHouseCost());
            System.out.printf("    Original loan:     $%,.2f%n", m.getOriginalLoanAmount());
            System.out.printf("    Remaining balance: $%,.2f%n", m.getRemainingBalance());
            System.out.printf("    Current LTV:       %s%%%n", m.getCurrentLtv());
            System.out.printf("    Monthly payment:   $%,.2f%n", m.getMonthlyPayment());
            if (m.isPmiRequired()) {
                System.out.printf("    PMI status:        %s%n", m.isPmiActive() ? "ACTIVE" : "CANCELLED");
                System.out.printf("    Monthly PMI:       $%,.2f%n", m.isPmiActive() ? m.getMonthlyPmi() : BigDecimal.ZERO);
                System.out.printf("    Total PMI paid:    $%,.2f%n", m.getTotalPmiPaid());
            }
            System.out.printf("    Total monthly:     $%,.2f%n", m.getTotalMonthlyCharge());
            System.out.printf("    Months paid:       %d / %d%n", m.getMonthsPaid(), m.getTotalMonths());
            System.out.printf("    Months remaining:  %d%n", m.getMonthsRemaining());
            System.out.printf("    Equity in home:    $%,.2f%n", m.getEquity());
            System.out.printf("    Status:            %s%n", m.isFullyPaid() ? "PAID OFF" : "ACTIVE");
        }
        System.out.println("=============================");
    }

    private void addRentalProperty() {
        System.out.println("\n--- Rent Out a Property ---");

        // Must select from owned properties that aren't already rented
        List<Property> available = propertyService.getAvailableForRental();
        if (available.isEmpty()) {
            System.out.println("No properties available to rent out.");
            System.out.println("You need to own a property first (option 5 - Take out a mortgage).");
            return;
        }

        System.out.println("Select a property to rent out:");
        List<Property> allProps = propertyService.getProperties();
        for (int i = 0; i < allProps.size(); i++) {
            Property p = allProps.get(i);
            String status = p.isRentedOut() ? " [ALREADY RENTED]" : "";
            System.out.printf("  %d - %s ($%,.2f)%s%n", i + 1, p.getAddress(), p.getPurchasePrice(), status);
        }
        System.out.print("> ");
        int propChoice = scanner.nextInt();
        scanner.nextLine();

        if (propChoice < 1 || propChoice > allProps.size()) {
            System.out.println("Invalid choice.");
            return;
        }
        Property selectedProp = allProps.get(propChoice - 1);
        if (selectedProp.isRentedOut()) {
            System.out.println("This property is already being rented out.");
            return;
        }

        String address = selectedProp.getAddress();
        BigDecimal propertyValue = selectedProp.getPurchasePrice();
        int mortgageIndex = selectedProp.getMortgageIndex();

        System.out.printf("%nSetting up rental for: %s ($%,.2f)%n", address, propertyValue);

        System.out.println("Monthly rent ($): ");
        //BigDecimal monthlyRent = scanner.nextBigDecimal();
        BigDecimal monthlyRent = selectedProp.getPurchasePrice().multiply(rentRate);
        //System.out.println(monthlyRent);


        System.out.println("Vacancy rate (e.g. 5 for 5%): ");
        //BigDecimal vacancyPct = scanner.nextBigDecimal();
        BigDecimal vacancyPct = BigDecimal.valueOf(8);
        //scanner.nextLine();
        BigDecimal vacancyRate = vacancyPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);

        System.out.println("Monthly property tax + insurance ($): ");
        //BigDecimal taxInsurance = scanner.nextBigDecimal();
        BigDecimal taxInsurance = selectedProp.getPurchasePrice().multiply(BigDecimal.valueOf(0.02)).divide(BigDecimal.valueOf(12),java.math.RoundingMode.HALF_UP);
        //scanner.nextLine();

        System.out.println("Monthly maintenance reserve ($): ");
        //BigDecimal maintenance = scanner.nextBigDecimal();
        BigDecimal maintenance = selectedProp.getPurchasePrice().multiply(BigDecimal.valueOf(0.015)).divide(BigDecimal.valueOf(12),java.math.RoundingMode.HALF_UP);

        //scanner.nextLine();

        System.out.println("Property management rate (e.g. 10 for 10%, 0 if self-managed): ");
        //BigDecimal mgmtPct = scanner.nextBigDecimal();
        BigDecimal mgmtPct = BigDecimal.ZERO;

        //scanner.nextLine();
        BigDecimal mgmtRate = mgmtPct.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);

        System.out.println("Monthly utilities - landlord portion ($): ");
        //BigDecimal utilities = scanner.nextBigDecimal();
        BigDecimal utilities = BigDecimal.ZERO;
        //scanner.nextLine();

        System.out.println("Monthly lawn care / snow removal ($): ");
        //BigDecimal lawnSnow = scanner.nextBigDecimal();
        BigDecimal lawnSnow = BigDecimal.valueOf(100);

        //scanner.nextLine();

        System.out.println("Monthly internet - if landlord-provided ($, 0 if tenant pays): ");
        //BigDecimal internet = scanner.nextBigDecimal();
        BigDecimal internet = BigDecimal.ZERO;
        //scanner.nextLine();

        System.out.println("Monthly electric - landlord portion ($, 0 if tenant pays): ");
        //BigDecimal electric = scanner.nextBigDecimal();
        BigDecimal electric = BigDecimal.ZERO;
        //scanner.nextLine();

        System.out.println("Monthly CapEx / major repair reserve ($): ");
        //BigDecimal capex = scanner.nextBigDecimal();
        BigDecimal capex = selectedProp.getPurchasePrice().multiply(BigDecimal.valueOf(0.015)).divide(BigDecimal.valueOf(12),java.math.RoundingMode.HALF_UP);

        //scanner.nextLine();

        System.out.println("Monthly other misc expenses ($): ");
        //BigDecimal misc = scanner.nextBigDecimal();
        BigDecimal misc = monthlyRent.multiply(BigDecimal.valueOf(0.033));

        //scanner.nextLine();

        RentalProperty rental = new RentalProperty(
                address, propertyValue, monthlyRent,
                vacancyRate, taxInsurance, maintenance, mgmtRate,
                utilities, lawnSnow, internet, electric,
                capex, misc, mortgageIndex, selectedProp
        );

        rentalPropertyService.addProperty(rental);
        int rentalIndex = rentalPropertyService.getProperties().size() - 1;
        selectedProp.setRentalIndex(rentalIndex);

        // Show summary
        BigDecimal estExpenses = rental.getEstimatedMonthlyExpenses();
        BigDecimal estCashFlow = rental.getEstimatedMonthlyCashFlow();
        BigDecimal mgmtFeeEst = monthlyRent.multiply(mgmtRate).setScale(2, java.math.RoundingMode.HALF_UP);

        System.out.println("\n===== Rental Property Added =====");
        System.out.printf("  Address:              %s%n", address);
        System.out.printf("  Property value:       $%,.2f%n", propertyValue);
        System.out.printf("  Monthly rent:         $%,.2f%n", monthlyRent);
        System.out.printf("  Vacancy rate:         %.1f%%%n", vacancyPct);
        System.out.printf("  Gross yield:          %s%%%n", rental.getGrossYield());
        System.out.println("  --- Monthly Expenses ---");
        System.out.printf("  Tax + insurance:      $%,.2f%n", taxInsurance);
        System.out.printf("  Maintenance reserve:  $%,.2f%n", maintenance);
        System.out.printf("  Property management:  $%,.2f (%.0f%% of rent)%n", mgmtFeeEst, mgmtPct);
        System.out.printf("  Utilities (landlord): $%,.2f%n", utilities);
        System.out.printf("  Lawn/snow:            $%,.2f%n", lawnSnow);
        System.out.printf("  Internet:             $%,.2f%n", internet);
        System.out.printf("  Electric:             $%,.2f%n", electric);
        System.out.printf("  CapEx reserve:        $%,.2f%n", capex);
        System.out.printf("  Other misc:           $%,.2f%n", misc);
        System.out.println("  ---------------------------");
        System.out.printf("  Total expenses:       $%,.2f%n", estExpenses);
        System.out.printf("  Est. net cash flow:   $%,.2f/mo (before mortgage)%n", estCashFlow);
        if (mortgageIndex >= 0) {
            ActiveMortgage linked = mortgageService.getActiveMortgages().get(mortgageIndex);
            BigDecimal afterMortgage = estCashFlow.subtract(linked.getTotalMonthlyCharge());
            System.out.printf("  After mortgage:       $%,.2f/mo%n", afterMortgage);
        }
        System.out.println("=================================");
    }

    private void viewRentalProperties() {
        List<RentalProperty> properties = rentalPropertyService.getProperties();
        if (properties.isEmpty()) {
            System.out.println("\nNo rental properties.");
            return;
        }

        System.out.println("\n===== Rental Properties =====");
        for (int i = 0; i < properties.size(); i++) {
            RentalProperty p = properties.get(i);
            System.out.printf("%n  Property %d: %s%n", i + 1, p.getAddress());
            System.out.printf("    Value:              $%,.2f%n", p.getPropertyValue());
            System.out.printf("    Monthly rent:       $%,.2f%n", p.getMonthlyRent());
            System.out.printf("    Gross yield:        %s%%%n", p.getGrossYield());
            System.out.printf("    Vacancy rate:       %.1f%%  (actual: %s%% occupancy)%n",
                    p.getVacancyRate().multiply(BigDecimal.valueOf(100)), p.getOccupancyRate());
            System.out.println("    --- Monthly Breakdown ---");
            System.out.printf("    Tax + insurance:    $%,.2f%n", p.getTaxAndInsurance());
            System.out.printf("    Maintenance:        $%,.2f%n", p.getMaintenanceReserve());
            BigDecimal mgmtFee = p.getMonthlyRent().multiply(p.getPropertyManagementRate())
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            System.out.printf("    Prop. management:   $%,.2f%n", mgmtFee);
            System.out.printf("    Utilities:          $%,.2f%n", p.getUtilitiesLandlord());
            System.out.printf("    Lawn/snow:          $%,.2f%n", p.getLawnCareSnow());
            System.out.printf("    Internet:           $%,.2f%n", p.getInternet());
            System.out.printf("    Electric:           $%,.2f%n", p.getElectric());
            System.out.printf("    CapEx reserve:      $%,.2f%n", p.getCapExReserve());
            System.out.printf("    Other misc:         $%,.2f%n", p.getOtherMiscExpenses());
            System.out.printf("    Total expenses:     $%,.2f%n", p.getEstimatedMonthlyExpenses());
            System.out.printf("    Net cash flow:      $%,.2f/mo (before mortgage)%n", p.getEstimatedMonthlyCashFlow());

            if (p.getMortgageIndex() >= 0) {
                List<ActiveMortgage> mortgages = mortgageService.getActiveMortgages();
                if (p.getMortgageIndex() < mortgages.size()) {
                    ActiveMortgage linked = mortgages.get(p.getMortgageIndex());
                    BigDecimal afterMortgage = p.getEstimatedMonthlyCashFlow().subtract(linked.getTotalMonthlyCharge());
                    System.out.printf("    Linked mortgage:    $%,.2f/mo%n", linked.getTotalMonthlyCharge());
                    System.out.printf("    After mortgage:     $%,.2f/mo%n", afterMortgage);
                }
            }

            System.out.println("    --- Running Totals ---");
            System.out.printf("    Months owned:       %d%n", p.getMonthsOwned());
            System.out.printf("    Total rent:         $%,.2f%n", p.getTotalRentCollected());
            System.out.printf("    Total expenses:     $%,.2f%n", p.getTotalExpensesPaid());
            System.out.printf("    Vacancy loss:       $%,.2f (%d vacant months)%n",
                    p.getTotalVacancyLoss(), p.getVacantMonths());
        }
        System.out.println("=============================");
    }

    private void viewOwnedProperties() {
        List<Property> properties = propertyService.getProperties();
        if (properties.isEmpty()) {
            System.out.println("\nNo owned properties.");
            System.out.println("Take out a mortgage (option 5) to purchase a property.");
            return;
        }

        System.out.println("\n===== Owned Properties =====");
        for (int i = 0; i < properties.size(); i++) {
            Property p = properties.get(i);
            System.out.printf("%n  Property %d: %s%n", i + 1, p.getAddress());
            System.out.printf("    Purchase price:    $%,.2f%n", p.getPurchasePrice());

            // Show linked mortgage info
            if (p.getMortgageIndex() >= 0) {
                List<ActiveMortgage> mortgages = mortgageService.getActiveMortgages();
                if (p.getMortgageIndex() < mortgages.size()) {
                    ActiveMortgage m = mortgages.get(p.getMortgageIndex());
                    System.out.printf("    Mortgage payment:  $%,.2f/mo%n", m.getTotalMonthlyCharge());
                    System.out.printf("    Remaining balance: $%,.2f%n", m.getRemainingBalance());
                    System.out.printf("    Equity:            $%,.2f%n", m.getEquity());
                    System.out.printf("    Mortgage status:   %s%n", m.isFullyPaid() ? "PAID OFF" : "ACTIVE");
                }
            }

            System.out.printf("    Rental status:     %s%n", p.isRentedOut() ? "RENTED OUT" : "NOT RENTED");
        }
        System.out.println("=============================");
    }

    private void viewNetworthHistory() {
        ArrayList<BigDecimal> history = networthService.getNetworthHistory();
        if (history.isEmpty()) {
            System.out.println("\nNo networth history available.");
            return;
        }

        System.out.println("\n===== Networth History =====");
        for (int i = 0; i < history.size(); i++) {
            BigDecimal value = history.get(i);
            if (i == 0) {
                System.out.printf("  Month %3d:  $%,.2f%n", i + 1, value);
            } else {
                BigDecimal change = value.subtract(history.get(i - 1));
                String sign = change.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
                System.out.printf("  Month %3d:  $%,.2f  (%s$%,.2f)%n", i + 1, value, sign, change);
            }
        }
        System.out.println("=============================");
    }
}