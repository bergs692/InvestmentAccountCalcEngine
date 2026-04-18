package com.InvestmentAccCalcEngine.simulator;

import com.InvestmentAccCalcEngine.simulator.strategies.BuyAndHoldStrategy;
import com.InvestmentAccCalcEngine.simulator.strategies.SaveAndWaitStrategy;
import com.InvestmentAccCalcEngine.viewer.ChartViewer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Handles the "s" menu command in the CLI.
 * Lets the user pick a strategy, configure the run, and view results.
 */
@Component
public class SimulationMenuHandler {

    private final SimulationRunner runner;
    private final StrategyRegistry registry;
    private final SimulationDisplayFormatter display;
    private final ChartViewer chartViewer;
    private final Scanner scanner;

    // Store results for comparison
    private final List<SimulationResult> previousResults = new ArrayList<>();

    public SimulationMenuHandler(SimulationRunner runner,
                                 StrategyRegistry registry,
                                 SimulationDisplayFormatter display,
                                 ChartViewer chartViewer) {
        this.runner = runner;
        this.registry = registry;
        this.display = display;
        this.chartViewer = chartViewer;
        this.scanner = new Scanner(System.in);

        // Register strategy factories — each call creates a fresh instance
        registry.register("1", SaveAndWaitStrategy::new);
        registry.register("2", BuyAndHoldStrategy::new);
        registry.register("3", () -> new BuyAndHoldStrategy(
                BigDecimal.valueOf(300000), 2, BigDecimal.valueOf(6.5), 30));
        registry.register("4", () -> new BuyAndHoldStrategy(
                BigDecimal.valueOf(150000), 5, BigDecimal.valueOf(7.0), 30));
    }

    /**
     * Main entry point — called from BankSimulationApp when user types "s".
     */
    public void handleSimulate(String accountNumber, BigDecimal annualSalary) {
        System.out.println("\n===== Strategy Simulator =====");

        if (registry.isEmpty()) {
            System.out.println("No strategies registered.");
            return;
        }

        display.printStrategyMenu(registry.getDisplayNames());

        System.out.println("  c - Compare all strategies");
        System.out.println("  v - View previous results");
        System.out.println("  b - Back to main menu");
        System.out.print("> ");
        String choice = scanner.nextLine().trim();

        if (choice.equals("b")) return;

        if (choice.equals("v")) {
            viewPreviousResults();
            return;
        }

        if (choice.equals("c")) {
            runComparison(accountNumber, annualSalary);
            return;
        }

        // Create a fresh strategy instance for this run
        Strategy selected = registry.create(choice);
        if (selected == null) {
            System.out.println("Invalid choice.");
            return;
        }

        System.out.print("How many months to simulate? ");
        int months;
        try {
            months = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Invalid number.");
            return;
        }

        if (months < 1 || months > 1200) {
            System.out.println("Please enter between 1 and 1200 months (100 years).");
            return;
        }

        System.out.printf("Running '%s' for %d months...%n", selected.getName(), months);
        System.out.println("(This does NOT affect your current simulation state)");
        System.out.println();

        SimulationResult result = runner.run(selected, months, accountNumber, annualSalary);
        display.printSimulationResult(result);

        previousResults.add(result);
        System.out.printf("  Result saved (%d total). Use 'c' to compare.%n", previousResults.size());

        promptForChart(result);
    }

    private void runComparison(String accountNumber, BigDecimal annualSalary) {
        System.out.print("How many months to simulate for all strategies? ");
        int months;
        try {
            months = scanner.nextInt();
            scanner.nextLine();
        } catch (Exception e) {
            scanner.nextLine();
            System.out.println("Invalid number.");
            return;
        }

        // Create fresh instances for every strategy in the comparison
        Map<String, Strategy> freshStrategies = registry.createAll();

        System.out.printf("Running %d strategies for %d months each...%n",
                freshStrategies.size(), months);

        List<SimulationResult> comparisonResults = new ArrayList<>();
        for (Map.Entry<String, Strategy> entry : freshStrategies.entrySet()) {
            System.out.printf("  Running: %s...%n", entry.getValue().getName());
            SimulationResult result = runner.run(entry.getValue(), months, accountNumber, annualSalary);
            comparisonResults.add(result);
        }

        display.printComparisonTable(comparisonResults);
        previousResults.addAll(comparisonResults);

        System.out.print("\nShow detailed results for each? (y/n): ");
        String showDetail = scanner.nextLine().trim().toLowerCase();
        if (showDetail.equals("y")) {
            for (SimulationResult r : comparisonResults) {
                display.printSimulationResult(r);
            }
        }

        promptForComparisonChart(comparisonResults);
    }

    private void viewPreviousResults() {
        if (previousResults.isEmpty()) {
            System.out.println("No previous results. Run a strategy first.");
            return;
        }

        System.out.println("\n--- Previous Results ---");
        for (int i = 0; i < previousResults.size(); i++) {
            SimulationResult r = previousResults.get(i);
            System.out.printf("  %d - %s (%d mo) → Networth: $%,.2f (gain: $%,.2f)%n",
                    i + 1, r.getStrategyName(), r.getTotalMonths(),
                    r.getEndingNetworth(), r.getNetworthGain());
        }

        System.out.print("View details for which? (number, 'all', 'compare', or 'chart <n>' / 'chart all'): ");
        String input = scanner.nextLine().trim().toLowerCase();

        if (input.equals("compare")) {
            display.printComparisonTable(previousResults);
            promptForComparisonChart(previousResults);
            return;
        }

        if (input.equals("all")) {
            for (SimulationResult r : previousResults) {
                display.printSimulationResult(r);
            }
            return;
        }

        // Direct chart shortcuts: "chart 2", "chart all"
        if (input.startsWith("chart")) {
            String arg = input.substring(5).trim();
            if (arg.equals("all")) {
                chartViewer.showStrategyComparison(previousResults);
            } else {
                try {
                    int idx = Integer.parseInt(arg) - 1;
                    if (idx >= 0 && idx < previousResults.size()) {
                        chartViewer.showSimulationDashboard(previousResults.get(idx));
                    } else {
                        System.out.println("Invalid selection.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Usage: 'chart <n>' or 'chart all'");
                }
            }
            return;
        }

        try {
            int idx = Integer.parseInt(input) - 1;
            if (idx >= 0 && idx < previousResults.size()) {
                SimulationResult r = previousResults.get(idx);
                display.printSimulationResult(r);
                promptForChart(r);
            } else {
                System.out.println("Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    // ──────────────────────────────────────────────
    //  Chart prompts
    // ──────────────────────────────────────────────

    private void promptForChart(SimulationResult result) {
        if (result == null) return;
        System.out.print("\nOpen interactive charts for this run? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        if (answer.equals("y") || answer.equals("yes")) {
            chartViewer.showSimulationDashboard(result);
        }
    }

    private void promptForComparisonChart(List<SimulationResult> results) {
        if (results == null || results.isEmpty()) return;
        if (results.size() < 2) {
            promptForChart(results.get(0));
            return;
        }
        System.out.print("\nOpen comparison charts? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();
        if (answer.equals("y") || answer.equals("yes")) {
            chartViewer.showStrategyComparison(results);
        }
    }
}
