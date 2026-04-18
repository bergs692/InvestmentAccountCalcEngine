package com.InvestmentAccCalcEngine.cli;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Responsible for all console output formatting and display.
 * Keeps presentation logic cleanly separated from business logic.
 */
@Component
public class DisplayFormatter {

  // ──────────────────────────────────────────────
  //  General / Account
  // ──────────────────────────────────────────────

  public void printWelcomeBanner() {
    System.out.println("=== Bank Account Simulator ===");
  }

  public void printMonthlyTakeHome(BigDecimal monthlyTakeHome) {
    System.out.printf("%nMonthly take home pay: $%.2f%n", monthlyTakeHome);
  }

  public void printMonthHeader(int month) {
    System.out.println("\n=============================");
    System.out.println(" Month " + month);
    System.out.println("=============================");
  }

  public void printSalaryDeposit(BigDecimal amount) {
    System.out.printf("  Salary deposited:          +$%,.2f%n", amount);
  }

  public void printMortgagePaymentDeducted(BigDecimal amount) {
    System.out.printf("  Mortgage payment deducted: $%,.2f%n", amount);
  }

  public void printRentalNet(BigDecimal rentalNet) {
    if (rentalNet.compareTo(BigDecimal.ZERO) >= 0) {
      System.out.printf("  Rental income (net):       +$%,.2f%n", rentalNet);
    } else {
      System.out.printf("  Rental expenses (net):     -$%,.2f%n", rentalNet.negate());
    }
  }

  public void printAccountSummary(int month, BigDecimal balance, BigDecimal networth) {
    System.out.println("\n--- Account Summary (Month " + month + ") ---");
    System.out.printf("Balance: $%.2f%n", balance);
    System.out.printf("Networth: $%.2f%n", networth);
  }

  public void printChargeConfirmation(BigDecimal amount, String description) {
    System.out.printf("Charged $%.2f for %s%n", amount, description);
  }

  public void printDepositConfirmation(BigDecimal amount) {
    System.out.printf("Deposited $%.2f%n", amount);
  }

  // ──────────────────────────────────────────────
  //  Projections
  // ──────────────────────────────────────────────

  public void printProjections(List<MonthlyProjection> projections) {
    System.out.println("\n--- Projection ---");
    projections.forEach(p -> System.out.printf(
        "Month %d: $%.2f%n", p.getMonth(), p.getProjectedBalance()));
  }

  // ──────────────────────────────────────────────
  //  Mortgage
  // ──────────────────────────────────────────────

  public void printAvailableLoanTypes(Map<String, LoanType> loanTypes) {
    System.out.println("Select loan type:");
    loanTypes.forEach((key, type) -> System.out.println("  " + key + " - " + type.getName()));
  }

  public void printMortgageSummary(MortgageSummary summary) {
    System.out.println("\n===== Mortgage Summary =====");
    System.out.printf("  Loan type:          %s%n", summary.getLoanType());
    System.out.printf("  House cost:         $%,.2f%n", summary.getHouseCost());
    System.out.printf("  Deposit:            $%,.2f%n", summary.getDeposit());
    System.out.printf("  Loan amount:        $%,.2f%n", summary.getLoanAmount());
    System.out.printf("  Interest rate:      %.2f%%%n", summary.getAnnualInterestRate());
    System.out.printf("  Term:               %d years (%d months)%n",
        summary.getTermYears(), summary.getTermMonths());
    System.out.println("-----------------------------");
    System.out.printf("  Monthly payment:    $%,.2f%n", summary.getMonthlyPayment());
    System.out.printf("  Total repayment:    $%,.2f%n", summary.getTotalPayment());
    System.out.printf("  Total interest:     $%,.2f%n", summary.getTotalInterest());
    System.out.println("=============================");
  }

  public void printMortgageDepositPrompt(BigDecimal deposit) {
    System.out.printf("%nThis will deduct $%,.2f deposit from your account.%n", deposit);
  }

  public void printMortgageApproved(String address, BigDecimal deposit, ActiveMortgage mortgage) {
    System.out.println("\nMortgage approved! Property registered at: " + address);
    System.out.printf("  Deposit of $%,.2f deducted from account.%n", deposit);
    System.out.printf("  $%,.2f mortgage payment each month for %d months.%n",
        mortgage.getMonthlyPayment(), mortgage.getTotalMonths());

    if (mortgage.isPmiRequired()) {
      System.out.println("\n  *** PMI (Private Mortgage Insurance) Required ***");
      System.out.println("  Down payment is less than 20% — per Minnesota law,");
      System.out.println("  PMI at 0.75% of loan amount/year will be charged monthly.");
      System.out.printf("  Monthly PMI:          $%,.2f%n", mortgage.getMonthlyPmi());
      System.out.printf("  Total monthly charge: $%,.2f (mortgage + PMI)%n",
          mortgage.getTotalMonthlyCharge());
      System.out.println("  PMI auto-terminates at 78% LTV (federal law).");
      System.out.println("  Under MN law, you can request cancellation at 80% LTV.");
    }
  }

  public void printActiveMortgages(List<ActiveMortgage> mortgages,
                                   java.util.function.IntFunction<Property> propertyByIndex) {
    if (mortgages.isEmpty()) {
      System.out.println("\nNo active mortgages.");
      return;
    }

    System.out.println("\n===== Active Mortgages =====");
    for (int i = 0; i < mortgages.size(); i++) {
      ActiveMortgage m = mortgages.get(i);
      Property linkedProperty = propertyByIndex.apply(i);
      BigDecimal marketValue = linkedProperty.getCurrentMarketValue();

      System.out.printf("%n  Mortgage %d: %s%n", i + 1, m.getLoanType());
      System.out.printf("    House cost:        $%,.2f%n", m.getHouseCost());
      System.out.printf("    Original loan:     $%,.2f%n", m.getOriginalLoanAmount());
      System.out.printf("    Remaining balance: $%,.2f%n", m.getRemainingBalance());
      System.out.printf("    Current LTV:       %s%%%n", m.getCurrentLtv(marketValue));
      System.out.printf("    Monthly payment:   $%,.2f%n", m.getMonthlyPayment());
      if (m.isPmiRequired()) {
        System.out.printf("    PMI status:        %s%n", m.isPmiActive() ? "ACTIVE" : "CANCELLED");
        System.out.printf("    Monthly PMI:       $%,.2f%n",
            m.isPmiActive() ? m.getMonthlyPmi() : BigDecimal.ZERO);
        System.out.printf("    Total PMI paid:    $%,.2f%n", m.getTotalPmiPaid());
      }
      System.out.printf("    Total monthly:     $%,.2f%n", m.getTotalMonthlyCharge());
      System.out.printf("    Months paid:       %d / %d%n", m.getMonthsPaid(), m.getTotalMonths());
      System.out.printf("    Months remaining:  %d%n", m.getMonthsRemaining());
      System.out.printf("    Equity in home:    $%,.2f%n", m.getEquity(marketValue));
      System.out.printf("    Status:            %s%n", m.isFullyPaid() ? "PAID OFF" : "ACTIVE");
    }
    System.out.println("=============================");
  }

  // ──────────────────────────────────────────────
  //  Rental Properties
  // ──────────────────────────────────────────────

  public void printRentalPropertyAdded(RentalProperty rental, Property selectedProp,
                                       BigDecimal vacancyPct, BigDecimal mgmtPct,
                                       boolean simplifiedMode, ActiveMortgage linkedMortgage) {
    System.out.println("\n===== Rental Property Added =====");
    System.out.printf("  Address:              %s%n", rental.getAddress());
    System.out.printf("  Property value:       $%,.2f%n", rental.getPropertyValue());

    if (simplifiedMode) {
      BigDecimal guaranteedNet = rental.getPropertyValue()
          .multiply(new BigDecimal("0.001"))
          .setScale(2, java.math.RoundingMode.HALF_UP);
      System.out.println("  Mode:                 SIMPLIFIED");
      System.out.printf("  Guaranteed net income: $%,.2f/mo ($100 per $100k)%n", guaranteedNet);
      if (linkedMortgage != null) {
        System.out.printf("  Mortgage payment:     $%,.2f/mo (covered by simplified mode)%n",
            linkedMortgage.getTotalMonthlyCharge());
      }
    } else {
      BigDecimal mgmtFeeEst = rental.getMonthlyRent()
          .multiply(rental.getPropertyManagementRate())
          .setScale(2, java.math.RoundingMode.HALF_UP);

      System.out.printf("  Monthly rent:         $%,.2f%n", rental.getMonthlyRent());
      System.out.printf("  Vacancy rate:         %.1f%%%n", vacancyPct);
      System.out.printf("  Gross yield:          %s%%%n", rental.getGrossYield());
      System.out.println("  --- Monthly Expenses ---");
      System.out.printf("  Tax + insurance:      $%,.2f%n", rental.getTaxAndInsurance());
      System.out.printf("  Maintenance reserve:  $%,.2f%n", rental.getMaintenanceReserve());
      System.out.printf("  Property management:  $%,.2f (%.0f%% of rent)%n", mgmtFeeEst, mgmtPct);
      System.out.printf("  Utilities (landlord): $%,.2f%n", rental.getUtilitiesLandlord());
      System.out.printf("  Lawn/snow:            $%,.2f%n", rental.getLawnCareSnow());
      System.out.printf("  Internet:             $%,.2f%n", rental.getInternet());
      System.out.printf("  Electric:             $%,.2f%n", rental.getElectric());
      System.out.printf("  CapEx reserve:        $%,.2f%n", rental.getCapExReserve());
      System.out.printf("  Other misc:           $%,.2f%n", rental.getOtherMiscExpenses());
      System.out.println("  ---------------------------");
      System.out.printf("  Total expenses:       $%,.2f%n", rental.getEstimatedMonthlyExpenses());
      System.out.printf("  Est. net cash flow:   $%,.2f/mo (before mortgage)%n",
          rental.getEstimatedMonthlyCashFlow());
      if (linkedMortgage != null) {
        BigDecimal afterMortgage = rental.getEstimatedMonthlyCashFlow()
            .subtract(linkedMortgage.getTotalMonthlyCharge());
        System.out.printf("  After mortgage:       $%,.2f/mo%n", afterMortgage);
      }
    }
    System.out.println("=================================");
  }

  public void printRentalProperties(List<RentalProperty> properties,
                                    List<ActiveMortgage> mortgages) {
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
      System.out.printf("    Net cash flow:      $%,.2f/mo (before mortgage)%n",
          p.getEstimatedMonthlyCashFlow());

      if (p.getMortgageIndex() >= 0 && p.getMortgageIndex() < mortgages.size()) {
        ActiveMortgage linked = mortgages.get(p.getMortgageIndex());
        BigDecimal afterMortgage = p.getEstimatedMonthlyCashFlow()
            .subtract(linked.getTotalMonthlyCharge());
        System.out.printf("    Linked mortgage:    $%,.2f/mo%n", linked.getTotalMonthlyCharge());
        System.out.printf("    After mortgage:     $%,.2f/mo%n", afterMortgage);
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

  // ──────────────────────────────────────────────
  //  Owned Properties
  // ──────────────────────────────────────────────

  public void printOwnedProperties(List<Property> properties, List<ActiveMortgage> mortgages) {
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
      System.out.printf("    Current Market Value:    $%,.2f%n", p.getCurrentMarketValue());

      if (p.getMortgageIndex() >= 0 && p.getMortgageIndex() < mortgages.size()) {
        ActiveMortgage m = mortgages.get(p.getMortgageIndex());
        System.out.printf("    Mortgage payment:  $%,.2f/mo%n", m.getTotalMonthlyCharge());
        System.out.printf("    Remaining balance: $%,.2f%n", m.getRemainingBalance());
        System.out.printf("    Equity:            $%,.2f%n", m.getEquity(p.getCurrentMarketValue()));
        System.out.printf("    Mortgage status:   %s%n", m.isFullyPaid() ? "PAID OFF" : "ACTIVE");
      }

      System.out.printf("    Rental status:     %s%n", p.isRentedOut() ? "RENTED OUT" : "NOT RENTED");
    }
    System.out.println("=============================");
  }

  // ──────────────────────────────────────────────
  //  Networth History
  // ──────────────────────────────────────────────

  public void printNetworthHistory(ArrayList<BigDecimal> history) {
    if (history.isEmpty()) {
      System.out.println("\nNo networth history available.");
      return;
    }

    System.out.println("\n===== Networth History =====");
    System.out.println("Month\tNetworth\tChange");
    for (int i = 0; i < history.size(); i++) {
      BigDecimal value = history.get(i);
      if (i == 0) {
        System.out.printf("%d\t%.2f\t%n", i + 1, value);
      } else {
        BigDecimal change = value.subtract(history.get(i - 1));
        System.out.printf("%d\t%.2f\t%.2f%n", i + 1, value, change);
      }
    }
    System.out.println("=============================");
  }

  // ──────────────────────────────────────────────
  //  Property Sale
  // ──────────────────────────────────────────────

  public void printPropertySelectionList(List<Property> properties) {
    System.out.println("Select a property to sell:");
    for (int i = 0; i < properties.size(); i++) {
      Property p = properties.get(i);
      System.out.printf("  %d - %s (purchased: $%,.2f, current value: $%,.2f)%n",
          i + 1, p.getAddress(), p.getPurchasePrice(), p.getCurrentMarketValue());
    }
  }

  public void printSaleSummary(Property property, BigDecimal salePrice,
                               BigDecimal agentCommission, BigDecimal closingCosts,
                               BigDecimal deedTax, BigDecimal totalFees,
                               BigDecimal salePriceAfterFees, BigDecimal remainingBalance,
                               BigDecimal netProceeds) {
    System.out.println("\n===== Sale Summary =====");
    System.out.printf("  Property:            %s%n", property.getAddress());
    System.out.printf("  Purchase price:      $%,.2f%n", property.getPurchasePrice());
    System.out.printf("  Sale price:          $%,.2f%n", salePrice);
    System.out.println("  --- Selling Fees ---");
    System.out.printf("  Agent commission (5.5%%): $%,.2f%n", agentCommission);
    System.out.printf("  Closing costs (1.5%%):    $%,.2f%n", closingCosts);
    System.out.printf("  MN deed tax (0.33%%):     $%,.2f%n", deedTax);
    System.out.printf("  Total fees:              $%,.2f%n", totalFees);
    System.out.println("  ----------------------");
    System.out.printf("  After fees:          $%,.2f%n", salePriceAfterFees);
    System.out.printf("  Remaining mortgage:  $%,.2f%n", remainingBalance);
    System.out.printf("  Net proceeds:        $%,.2f%n", netProceeds);
    if (netProceeds.compareTo(BigDecimal.ZERO) < 0) {
      System.out.printf("  *** You owe $%,.2f at closing (short sale) ***%n", netProceeds.negate());
    }
    System.out.println("========================");
  }

  public void printSaleSuccess(BigDecimal netProceeds) {
    System.out.println("\nProperty sold successfully!");
    System.out.printf("  Net proceeds of $%,.2f applied to your account.%n", netProceeds);
  }

  // ──────────────────────────────────────────────
  //  Rental Property Selection (for add flow)
  // ──────────────────────────────────────────────

  public void printRentalPropertySelectionList(List<Property> allProps) {
    System.out.println("Select a property to rent out:");
    for (int i = 0; i < allProps.size(); i++) {
      Property p = allProps.get(i);
      String status = p.isRentedOut() ? " [ALREADY RENTED]" : "";
      System.out.printf("  %d - %s ($%,.2f)%s%n",
          i + 1, p.getAddress(), p.getPurchasePrice(), status);
    }
  }

  // ──────────────────────────────────────────────
  //  Simplified Rental Toggle
  // ──────────────────────────────────────────────

  public void printSimplifiedRentalToggle(boolean newState) {
    System.out.println("\nSimplified rental mode: " + (newState ? "ON" : "OFF"));
    if (newState) {
      System.out.println("  Rental income = $100/mo per $100k of property value (after all expenses/mortgage).");
    } else {
      System.out.println("  Full expense/vacancy simulation restored.");
    }
  }

  // ──────────────────────────────────────────────
  //  Menu
  // ──────────────────────────────────────────────

  public void printMenu() {
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
    System.out.println("  10 - Sell a property");
    System.out.println("  q - Quit");
    System.out.println("  nh - View Networth History");
    System.out.println("  sr - Toggle simplified rental mode");
    System.out.println("  s - Run a simulation strategy");
  }
}