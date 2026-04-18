package com.InvestmentAccCalcEngine.simulator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Handles all console output related to simulation runs.
 */
@Component
public class SimulationDisplayFormatter {

    public void printStrategyMenu(Map<String, String> strategyNames) {
        System.out.println("\n===== Available Strategies =====");
        strategyNames.forEach((key, name) ->
                System.out.printf("  %s - %s%n", key, name));
        System.out.println("================================");
    }

    public void printSimulationResult(SimulationResult result) {
        System.out.println("\n╔══════════════════════════════════════════════════╗");
        System.out.println("║          SIMULATION RESULTS                      ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("  Strategy:          %s%n", result.getStrategyName());
        System.out.printf("  Duration:          %d months (%d years, %d months)%n",
                result.getTotalMonths(),
                result.getTotalMonths() / 12,
                result.getTotalMonths() % 12);

        System.out.println("\n  ── Financial Summary ──");
        System.out.printf("  Starting balance:  $%,.2f%n", result.getStartingBalance());
        System.out.printf("  Ending balance:    $%,.2f%n", result.getEndingBalance());
        System.out.printf("  Balance change:    $%,.2f%n", result.getBalanceGain());

        System.out.println();
        System.out.printf("  Starting networth: $%,.2f%n", result.getStartingNetworth());
        System.out.printf("  Ending networth:   $%,.2f%n", result.getEndingNetworth());
        System.out.printf("  Networth gain:     $%,.2f%n", result.getNetworthGain());

        System.out.println("\n  ── Portfolio ──");
        System.out.printf("  Properties owned:  %d%n", result.getPropertiesOwned());
        System.out.printf("  Properties rented: %d%n", result.getPropertiesRented());
        System.out.printf("  Active mortgages:  %d%n", result.getActiveMortgages());

        // ── Yearly milestones table ──
        List<SimulationResult.MonthSnapshot> snapshots = result.getMonthlySnapshots();
        if (!snapshots.isEmpty()) {
            System.out.println("\n  ── Yearly Progress ──");
            System.out.println("  Year  Month   Balance          Networth         Properties");
            System.out.println("  ────  ─────   ───────          ────────         ──────────");

            // Show month 1
            SimulationResult.MonthSnapshot first = snapshots.get(0);
            System.out.printf("  %-4s  %-5d   $%,-15.2f  $%,-15.2f  %d%n",
                    "", first.getMonth(), first.getBalance(), first.getNetworth(), first.getPropertiesOwned());

            // Show every 12th month
            for (SimulationResult.MonthSnapshot snap : snapshots) {
                if (snap.getMonth() % 12 == 0) {
                    System.out.printf("  %-4d  %-5d   $%,-15.2f  $%,-15.2f  %d%n",
                            snap.getMonth() / 12,
                            snap.getMonth(),
                            snap.getBalance(),
                            snap.getNetworth(),
                            snap.getPropertiesOwned());
                }
            }

            // Show final month if not already on a year boundary
            SimulationResult.MonthSnapshot last = snapshots.get(snapshots.size() - 1);
            if (last.getMonth() % 12 != 0) {
                System.out.printf("  %-4s  %-5d   $%,-15.2f  $%,-15.2f  %d%n",
                        "END", last.getMonth(), last.getBalance(), last.getNetworth(), last.getPropertiesOwned());
            }
        }

        // ── Event log ──
        List<String> eventLog = result.getEventLog();
        if (!eventLog.isEmpty()) {
            System.out.println("\n  ── Event Log ──");
            for (String event : eventLog) {
                System.out.println("  " + event);
            }
        }

        System.out.println("\n══════════════════════════════════════════════════");
    }

    /**
     * Prints a side-by-side comparison of multiple simulation results.
     */
    public void printComparisonTable(List<SimulationResult> results) {
        if (results.isEmpty()) return;

        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  STRATEGY COMPARISON                         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        // Header
        System.out.printf("  %-35s", "Metric");
        for (SimulationResult r : results) {
            String shortName = r.getStrategyName();
            if (shortName.length() > 20) shortName = shortName.substring(0, 17) + "...";
            System.out.printf("  %-20s", shortName);
        }
        System.out.println();
        System.out.printf("  %-35s", "─────────────────────────────────");
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("  %-20s", "────────────────────");
        }
        System.out.println();

        // Rows
        printComparisonRow("Duration (months)", results,
                r -> String.valueOf(r.getTotalMonths()));
        printComparisonRow("Ending balance", results,
                r -> String.format("$%,.2f", r.getEndingBalance()));
        printComparisonRow("Ending networth", results,
                r -> String.format("$%,.2f", r.getEndingNetworth()));
        printComparisonRow("Networth gain", results,
                r -> String.format("$%,.2f", r.getNetworthGain()));
        printComparisonRow("Properties owned", results,
                r -> String.valueOf(r.getPropertiesOwned()));
        printComparisonRow("Properties rented", results,
                r -> String.valueOf(r.getPropertiesRented()));
        printComparisonRow("Active mortgages", results,
                r -> String.valueOf(r.getActiveMortgages()));

        System.out.println("\n══════════════════════════════════════════════════════════════");
    }

    private void printComparisonRow(String label,
                                    List<SimulationResult> results,
                                    java.util.function.Function<SimulationResult, String> extractor) {
        System.out.printf("  %-35s", label);
        for (SimulationResult r : results) {
            System.out.printf("  %-20s", extractor.apply(r));
        }
        System.out.println();
    }
}
