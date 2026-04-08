package com.InvestmentAccCalcEngine.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.service.BankAccountService;
import com.InvestmentAccCalcEngine.service.SalaryService;
import com.InvestmentAccCalcEngine.service.ProjectionService;
import com.InvestmentAccCalcEngine.service.MortgageService;
import com.InvestmentAccCalcEngine.service.loan.LoanType;


@Component
public class BankSimulationApp implements CommandLineRunner {

    private final BankAccountService bankAccountService;
    private final SalaryService salaryService;
    private final ProjectionService projectionService;
    private final MortgageService mortgageService;

    private final Scanner scanner = new Scanner(System.in);
    private String accountNumber;

    public BankSimulationApp(BankAccountService bankAccountService,
                             SalaryService salaryService,
                             ProjectionService projectionService,
                             MortgageService mortgageService) {
        this.bankAccountService = bankAccountService;
        this.salaryService = salaryService;
        this.projectionService = projectionService;
        this.mortgageService = mortgageService;
    }

    @Override
    public void run(String... args) {
        System.out.println("=== Bank Account Simulator ===");

        // Setup
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        System.out.print("Enter account number: ");
        accountNumber = scanner.nextLine();

        System.out.print("Enter starting balance: ");
        BigDecimal startingBalance = scanner.nextBigDecimal();
        scanner.nextLine();

        System.out.print("Enter your annual salary: ");
        BigDecimal annualSalary = scanner.nextBigDecimal();
        scanner.nextLine();

        bankAccountService.createAccount(accountNumber, name, startingBalance);
        BigDecimal monthlyTakeHome = salaryService.calculateMonthlyAfterTax(annualSalary);

        System.out.printf("%nMonthly take home pay: $%.2f%n", monthlyTakeHome);

        // Month loop
        int month = 1;
        while (true) {
            System.out.println("\n=============================");
            System.out.println(" Month " + month);
            System.out.println("=============================");

            // Apply monthly salary
            bankAccountService.deposit(accountNumber, monthlyTakeHome);

            // Auto-charge active mortgages
            if (mortgageService.hasActiveMortgages()) {
                BigDecimal mortgageCharged = mortgageService.processMonthlyPayments(accountNumber);
                System.out.printf("  Mortgage payment deducted: $%,.2f%n", mortgageCharged);
            }

            printAccountInfo(month);
            printMenu();

            boolean endMonth = false;
            while (!endMonth) {
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
                    case "0" -> endMonth = true;
                    case "q" -> {
                        System.out.println("Exiting simulation. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Unknown command, try again.");
                }

                if (!endMonth) printMenu();
            }

            month++;
        }
    }

    private void printAccountInfo(int month) {
        BigDecimal balance = bankAccountService.getBalance(accountNumber);
        System.out.println("\n--- Account Summary (Month " + month + ") ---");
        System.out.printf("Balance: $%.2f%n", balance);
    }

    private void printMenu() {
        System.out.println("\nWhat would you like to do?");
        System.out.println("  1 - Make a charge");
        System.out.println("  2 - Make a deposit");
        System.out.println("  3 - View future projections");
        System.out.println("  4 - View account info");
        System.out.println("  5 - Take out a mortgage");
        System.out.println("  6 - View active mortgages");
        System.out.println("  0 - Advance to next month");
        System.out.println("  q - Quit");
    }

    private void takeOutMortgage() {
        System.out.println("\n--- Take Out a Mortgage ---");

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
                    accountNumber, selectedType, houseCost, deposit, interestRate, termYears);
            System.out.println("\nMortgage approved!");
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
}