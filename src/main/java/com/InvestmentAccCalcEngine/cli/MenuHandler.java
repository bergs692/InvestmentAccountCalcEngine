package com.InvestmentAccCalcEngine.cli;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.service.*;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Handles all menu-driven user interactions (input collection + delegation).
 * Each public method corresponds to a menu option in the simulation.
 */
@Component
public class MenuHandler {

  private final BankAccountService bankAccountService;
  private final SalaryService salaryService;
  private final ProjectionService projectionService;
  private final MortgageService mortgageService;
  private final PropertyService propertyService;
  private final RentalPropertyService rentalPropertyService;
  private final NetworthService networthService;
  private final DisplayFormatter display;

  private final Scanner scanner;

  public MenuHandler(BankAccountService bankAccountService,
                     SalaryService salaryService,
                     ProjectionService projectionService,
                     MortgageService mortgageService,
                     PropertyService propertyService,
                     RentalPropertyService rentalPropertyService,
                     NetworthService networthService,
                     DisplayFormatter display) {
    this.bankAccountService = bankAccountService;
    this.salaryService = salaryService;
    this.projectionService = projectionService;
    this.mortgageService = mortgageService;
    this.propertyService = propertyService;
    this.rentalPropertyService = rentalPropertyService;
    this.networthService = networthService;
    this.display = display;
    this.scanner = new Scanner(System.in);
  }

  // ──────────────────────────────────────────────
  //  1 - Make a charge
  // ──────────────────────────────────────────────

  public void handleCharge(String accountNumber, int month) {
    System.out.print("Charge amount: ");
    BigDecimal amount = scanner.nextBigDecimal();
    scanner.nextLine();
    System.out.print("Description: ");
    String desc = scanner.nextLine();
    bankAccountService.charge(accountNumber, amount);
    display.printChargeConfirmation(amount, desc);
    printAccountInfo(accountNumber, month);
  }

  // ──────────────────────────────────────────────
  //  2 - Make a deposit
  // ──────────────────────────────────────────────

  public void handleDeposit(String accountNumber, int month) {
    System.out.print("Deposit amount: ");
    BigDecimal amount = scanner.nextBigDecimal();
    scanner.nextLine();
    bankAccountService.deposit(accountNumber, amount);
    display.printDepositConfirmation(amount);
    printAccountInfo(accountNumber, month);
  }

  // ──────────────────────────────────────────────
  //  3 - View future projections
  // ──────────────────────────────────────────────

  public void handleProjections(String accountNumber, BigDecimal annualSalary) {
    System.out.print("Project how many months ahead? ");
    int months = scanner.nextInt();
    scanner.nextLine();
    System.out.print("Enter total monthly payments/outgoings: ");
    BigDecimal payments = scanner.nextBigDecimal();
    scanner.nextLine();
    List<MonthlyProjection> projections = projectionService.projectBalance(
        accountNumber, annualSalary, List.of(payments), months);
    display.printProjections(projections);
  }

  // ──────────────────────────────────────────────
  //  4 - View account info
  // ──────────────────────────────────────────────

  public void printAccountInfo(String accountNumber, int month) {
    BigDecimal balance = bankAccountService.getBalance(accountNumber);
    BigDecimal networth = networthService.getPrevNetworth();
    display.printAccountSummary(month, balance, networth);
  }

  // ──────────────────────────────────────────────
  //  5 - Take out a mortgage
  // ──────────────────────────────────────────────

  public void handleTakeOutMortgage(String accountNumber) {
    System.out.println("\n--- Take Out a Mortgage ---");

    System.out.print("Property address: ");
    String propertyAddress = scanner.nextLine();

    // Select loan type
    Map<String, LoanType> loanTypes = mortgageService.getAvailableLoanTypes();
    display.printAvailableLoanTypes(loanTypes);
    System.out.print("> ");
    String typeChoice = scanner.nextLine().trim();
    LoanType selectedType = loanTypes.get(typeChoice);
    if (selectedType == null) {
      System.out.println("Invalid loan type.");
      return;
    }

    System.out.print("House cost ($) in Thousands: ");
    BigDecimal houseCost = scanner.nextBigDecimal().multiply(BigDecimal.valueOf(1000));
    scanner.nextLine();

    System.out.print("Deposit amount ($) in Thousands: ");
    BigDecimal deposit = scanner.nextBigDecimal().multiply(BigDecimal.valueOf(1000));
    scanner.nextLine();

    System.out.print("Annual interest rate (%): ");
    BigDecimal interestRate = scanner.nextBigDecimal();
    scanner.nextLine();

    System.out.print("Mortgage term (years): ");
    int termYears = scanner.nextInt();
    scanner.nextLine();

    MortgageSummary summary = mortgageService.calculate(
        selectedType, houseCost, deposit, interestRate, termYears);
    display.printMortgageSummary(summary);
    display.printMortgageDepositPrompt(deposit);

    System.out.print("Confirm? (y/n): ");
    String confirm = scanner.nextLine().trim().toLowerCase();
    if (!confirm.equals("y")) {
      System.out.println("Mortgage cancelled.");
      return;
    }

    try {
      ActiveMortgage mortgage = mortgageService.takeOutMortgage(
          accountNumber, selectedType, houseCost, deposit,
          interestRate, termYears, propertyAddress);
      display.printMortgageApproved(propertyAddress, deposit, mortgage);
    } catch (IllegalArgumentException e) {
      System.out.println("Mortgage rejected: " + e.getMessage());
    }
  }

  // ──────────────────────────────────────────────
  //  6 - View active mortgages
  // ──────────────────────────────────────────────

  public void handleViewActiveMortgages() {
    List<ActiveMortgage> mortgages = mortgageService.getActiveMortgages();
    display.printActiveMortgages(mortgages, propertyService::getPropertyByMortgageIndex);
  }

  // ──────────────────────────────────────────────
  //  7 - Rent out a property
  // ──────────────────────────────────────────────

  public void handleAddRentalProperty(BigDecimal rentRate) {
    System.out.println("\n--- Rent Out a Property ---");

    List<Property> available = propertyService.getAvailableForRental();
    if (available.isEmpty()) {
      System.out.println("No properties available to rent out.");
      System.out.println("You need to own a property first (option 5 - Take out a mortgage).");
      return;
    }

    List<Property> allProps = propertyService.getProperties();
    display.printRentalPropertySelectionList(allProps);
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

    // Auto-calculated rental parameters
    BigDecimal monthlyRent = selectedProp.getPurchasePrice().multiply(rentRate);
    BigDecimal vacancyPct = BigDecimal.valueOf(8);
    BigDecimal vacancyRate = vacancyPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    BigDecimal taxInsurance = selectedProp.getPurchasePrice()
        .multiply(BigDecimal.valueOf(0.02))
        .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    BigDecimal maintenance = selectedProp.getPurchasePrice()
        .multiply(BigDecimal.valueOf(0.015))
        .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    BigDecimal mgmtPct = BigDecimal.ZERO;
    BigDecimal mgmtRate = BigDecimal.ZERO;
    BigDecimal utilities = BigDecimal.ZERO;
    BigDecimal lawnSnow = BigDecimal.valueOf(100);
    BigDecimal internet = BigDecimal.ZERO;
    BigDecimal electric = BigDecimal.ZERO;
    BigDecimal capex = selectedProp.getPurchasePrice()
        .multiply(BigDecimal.valueOf(0.015))
        .divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    BigDecimal misc = monthlyRent.multiply(BigDecimal.valueOf(0.033));

    // Print prompts (values are auto-filled but shown for transparency)
    System.out.printf("Monthly rent ($): %s%n", monthlyRent);
    System.out.printf("Vacancy rate (e.g. 5 for 5%%): %s%n", vacancyPct);
    System.out.printf("Monthly property tax + insurance ($): %s%n", taxInsurance);
    System.out.printf("Monthly maintenance reserve ($): %s%n", maintenance);
    System.out.printf("Property management rate (e.g. 10 for 10%%, 0 if self-managed): %s%n", mgmtPct);
    System.out.printf("Monthly utilities - landlord portion ($): %s%n", utilities);
    System.out.printf("Monthly lawn care / snow removal ($): %s%n", lawnSnow);
    System.out.printf("Monthly internet - if landlord-provided ($, 0 if tenant pays): %s%n", internet);
    System.out.printf("Monthly electric - landlord portion ($, 0 if tenant pays): %s%n", electric);
    System.out.printf("Monthly CapEx / major repair reserve ($): %s%n", capex);
    System.out.printf("Monthly other misc expenses ($): %s%n", misc);

    RentalProperty rental = new RentalProperty(
        address, propertyValue, monthlyRent,
        vacancyRate, taxInsurance, maintenance, mgmtRate,
        utilities, lawnSnow, internet, electric,
        capex, misc, mortgageIndex, selectedProp
    );

    rentalPropertyService.addProperty(rental);
    int rentalIndex = rentalPropertyService.getProperties().size() - 1;
    selectedProp.setRentalIndex(rentalIndex);

    ActiveMortgage linkedMortgage = null;
    if (mortgageIndex >= 0) {
      linkedMortgage = mortgageService.getActiveMortgages().get(mortgageIndex);
    }

    display.printRentalPropertyAdded(
        rental, selectedProp, vacancyPct, mgmtPct,
        rentalPropertyService.isSimplifiedMode(), linkedMortgage);
  }

  // ──────────────────────────────────────────────
  //  8 - View rental properties
  // ──────────────────────────────────────────────

  public void handleViewRentalProperties() {
    display.printRentalProperties(
        rentalPropertyService.getProperties(),
        mortgageService.getActiveMortgages());
  }

  // ──────────────────────────────────────────────
  //  9 - View owned properties
  // ──────────────────────────────────────────────

  public void handleViewOwnedProperties() {
    display.printOwnedProperties(
        propertyService.getProperties(),
        mortgageService.getActiveMortgages());
  }

  // ──────────────────────────────────────────────
  //  10 - Sell a property
  // ──────────────────────────────────────────────

  public void handleSellProperty(String accountNumber) {
    List<Property> properties = propertyService.getProperties();
    if (properties.isEmpty()) {
      System.out.println("\nNo properties to sell.");
      return;
    }

    System.out.println("\n--- Sell a Property ---");
    display.printPropertySelectionList(properties);
    System.out.print("> ");
    int choice = scanner.nextInt();
    scanner.nextLine();

    if (choice < 1 || choice > properties.size()) {
      System.out.println("Invalid choice.");
      return;
    }

    Property selectedProp = properties.get(choice - 1);
    int mortgageIndex = selectedProp.getMortgageIndex();
    BigDecimal salePrice = selectedProp.getCurrentMarketValue();

    // Minnesota selling fees
    BigDecimal agentCommissionRate = new BigDecimal("0.055");
    BigDecimal closingCostRate = new BigDecimal("0.015");
    BigDecimal deedTaxRate = new BigDecimal("0.0033");

    BigDecimal agentCommission = salePrice.multiply(agentCommissionRate)
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal closingCosts = salePrice.multiply(closingCostRate)
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal deedTax = salePrice.multiply(deedTaxRate)
        .setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalFees = agentCommission.add(closingCosts).add(deedTax);
    BigDecimal salePriceAfterFees = salePrice.subtract(totalFees);

    BigDecimal remainingBalance = BigDecimal.ZERO;
    if (mortgageIndex >= 0) {
      ActiveMortgage mortgage = mortgageService.getMortgage(mortgageIndex);
      remainingBalance = mortgage.getRemainingBalance();
    }
    BigDecimal netProceeds = salePriceAfterFees.subtract(remainingBalance);

    display.printSaleSummary(selectedProp, salePrice,
        agentCommission, closingCosts, deedTax, totalFees,
        salePriceAfterFees, remainingBalance, netProceeds);

    System.out.print("Confirm sale? (y/n): ");
    String confirm = scanner.nextLine().trim().toLowerCase();
    if (!confirm.equals("y")) {
      System.out.println("Sale cancelled.");
      return;
    }

    try {
      if (mortgageIndex >= 0) {
        mortgageService.sellProperty(accountNumber, mortgageIndex, salePriceAfterFees);
      } else {
        bankAccountService.deposit(accountNumber, salePriceAfterFees);
      }

      if (selectedProp.isRentedOut() && selectedProp.getRentalIndex() >= 0) {
        int rentalIdx = selectedProp.getRentalIndex();
        rentalPropertyService.removeProperty(rentalIdx);
        propertyService.adjustRentalIndicesAfterRemoval(rentalIdx);
      }

      propertyService.removeProperty(choice - 1);
      display.printSaleSuccess(netProceeds);
      networthService.recalculateNetworth(accountNumber);
      printAccountInfo(accountNumber, 0);
    } catch (IllegalArgumentException e) {
      System.out.println("Sale failed: " + e.getMessage());
    }
  }

  // ──────────────────────────────────────────────
  //  nh - View networth history
  // ──────────────────────────────────────────────

  public void handleViewNetworthHistory() {
    display.printNetworthHistory(networthService.getNetworthHistory());
  }

  // ──────────────────────────────────────────────
  //  sr - Toggle simplified rental mode
  // ──────────────────────────────────────────────

  public void handleToggleSimplifiedRental() {
    boolean current = rentalPropertyService.isSimplifiedMode();
    rentalPropertyService.setSimplifiedMode(!current);
    display.printSimplifiedRentalToggle(!current);
  }

  // ──────────────────────────────────────────────
  //  Input helpers (used by BankSimulationApp)
  // ──────────────────────────────────────────────

  public String readLine() {
    return scanner.nextLine().trim();
  }

  public int readInt() {
    int val = scanner.nextInt();
    scanner.nextLine();
    return val;
  }
}