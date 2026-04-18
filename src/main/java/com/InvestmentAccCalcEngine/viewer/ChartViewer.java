package com.InvestmentAccCalcEngine.viewer;

import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.simulator.SimulationResult;
import com.InvestmentAccCalcEngine.simulator.SimulationResult.MonthSnapshot;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Renders simulation results, networth history, and projection data
 * as interactive JFreeChart windows.
 *
 * All charts open in non-modal Swing {@link JFrame}s so the CLI stays
 * fully usable. Charts support pan/zoom, copy-to-clipboard, and
 * save-as-PNG via the default {@link ChartPanel} right-click menu.
 *
 * <p>Public entry points:
 * <ul>
 *   <li>{@link #showSimulationDashboard(SimulationResult)}</li>
 *   <li>{@link #showStrategyComparison(List)}</li>
 *   <li>{@link #showNetworthHistory(List)}</li>
 *   <li>{@link #showBalanceProjection(List, BigDecimal)}</li>
 * </ul>
 *
 * <p>If the JVM is running headless (no display available), calls log a
 * warning and return without throwing — the CLI continues normally.
 */
@Component
public class ChartViewer {

    // ── Styling ──
    // A curated palette that stays readable when overlaying many series.
    private static final Color[] SERIES_PALETTE = {
            new Color(0x1f77b4), // blue
            new Color(0xff7f0e), // orange
            new Color(0x2ca02c), // green
            new Color(0xd62728), // red
            new Color(0x9467bd), // purple
            new Color(0x8c564b), // brown
            new Color(0xe377c2), // pink
            new Color(0x17becf)  // cyan
    };

    private static final Color BALANCE_COLOR  = new Color(0x1f77b4);
    private static final Color NETWORTH_COLOR = new Color(0x2ca02c);
    private static final Color PROPERTY_COLOR = new Color(0xd62728);

    private static final Font  TITLE_FONT     = new Font(Font.SANS_SERIF, Font.BOLD, 16);
    private static final Font  AXIS_FONT      = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Color GRID_COLOR     = new Color(0xE6E6E6);
    private static final Color BG_COLOR       = Color.WHITE;

    private static final Dimension SINGLE_CHART_SIZE = new Dimension(900, 560);
    private static final Dimension DASHBOARD_SIZE    = new Dimension(1000, 640);

    // ────────────────────────────────────────────────────────────────
    //  Public API
    // ────────────────────────────────────────────────────────────────

    /**
     * Opens a tabbed dashboard for a single simulation run.
     * Tabs: Financial (Balance + Networth), Portfolio (Properties over time).
     */
    public void showSimulationDashboard(SimulationResult result) {
        if (result == null || result.getMonthlySnapshots().isEmpty()) {
            System.out.println("No data to chart.");
            return;
        }
        if (unavailable()) return;

        String title = "Simulation Results — " + result.getStrategyName();

        SwingUtilities.invokeLater(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Financial",
                    wrap(createBalanceNetworthChart(result)));
            tabs.addTab("Portfolio",
                    wrap(createPropertiesChart(result)));

            showFrame(title, (Container) tabs, DASHBOARD_SIZE);
        });
    }

    /**
     * Opens a tabbed window comparing multiple simulation results.
     * Tabs: Networth over time, Balance over time, Final Networth (bar).
     */
    public void showStrategyComparison(List<SimulationResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("No results to compare.");
            return;
        }
        if (unavailable()) return;

        SwingUtilities.invokeLater(() -> {
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Net Worth",
                    wrap(createComparisonChart(results, Metric.NETWORTH)));
            tabs.addTab("Balance",
                    wrap(createComparisonChart(results, Metric.BALANCE)));
            tabs.addTab("Final Net Worth",
                    wrap(createFinalNetworthBarChart(results)));

            showFrame("Strategy Comparison (" + results.size() + " runs)",
                    (Container) tabs, DASHBOARD_SIZE);
        });
    }

    /**
     * Opens a line chart of the running networth history from the live CLI.
     * Index 0 corresponds to month 1 (tracking begins after the first tick).
     */
    public void showNetworthHistory(List<BigDecimal> history) {
        if (history == null || history.isEmpty()) {
            System.out.println("No networth history to chart.");
            return;
        }
        if (unavailable()) return;

        SwingUtilities.invokeLater(() -> {
            XYSeries series = new XYSeries("Net Worth");
            for (int i = 0; i < history.size(); i++) {
                series.add(i + 1, history.get(i).doubleValue());
            }
            XYSeriesCollection dataset = new XYSeriesCollection(series);

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Net Worth History",
                    "Month",
                    "Net Worth ($)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            styleXYChart(chart, new Color[]{NETWORTH_COLOR});
            showFrame("Net Worth History", (Container) wrap(chart), SINGLE_CHART_SIZE);
        });
    }

    /**
     * Opens a line chart of projected balances over a future horizon.
     * {@code startingBalance} is used to anchor a month-0 point.
     */
    public void showBalanceProjection(List<MonthlyProjection> projections,
                                      BigDecimal startingBalance) {
        if (projections == null || projections.isEmpty()) {
            System.out.println("No projection data to chart.");
            return;
        }
        if (unavailable()) return;

        SwingUtilities.invokeLater(() -> {
            XYSeries series = new XYSeries("Projected Balance");
            if (startingBalance != null) {
                series.add(0, startingBalance.doubleValue());
            }
            for (MonthlyProjection p : projections) {
                series.add(p.getMonth(), p.getProjectedBalance().doubleValue());
            }
            XYSeriesCollection dataset = new XYSeriesCollection(series);

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Balance Projection (" + projections.size() + " months)",
                    "Month",
                    "Projected Balance ($)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false);

            styleXYChart(chart, new Color[]{BALANCE_COLOR});
            showFrame("Balance Projection", (Container) wrap(chart), SINGLE_CHART_SIZE);
        });
    }

    // ────────────────────────────────────────────────────────────────
    //  Chart builders
    // ────────────────────────────────────────────────────────────────

    /** Line chart with two series: balance and networth, over months. */
    private JFreeChart createBalanceNetworthChart(SimulationResult result) {
        XYSeries balanceSeries  = new XYSeries("Balance");
        XYSeries networthSeries = new XYSeries("Net Worth");

        // Anchor month 0 with the starting values so the curve doesn't start in midair.
        balanceSeries.add(0,  result.getStartingBalance().doubleValue());
        networthSeries.add(0, result.getStartingNetworth().doubleValue());

        for (MonthSnapshot s : result.getMonthlySnapshots()) {
            balanceSeries.add(s.getMonth(),  s.getBalance().doubleValue());
            networthSeries.add(s.getMonth(), s.getNetworth().doubleValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(balanceSeries);
        dataset.addSeries(networthSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Balance & Net Worth — " + result.getStrategyName(),
                "Month",
                "Amount ($)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        styleXYChart(chart, new Color[]{BALANCE_COLOR, NETWORTH_COLOR});
        return chart;
    }

    /** Step chart of properties owned over months (counts change discretely). */
    private JFreeChart createPropertiesChart(SimulationResult result) {
        XYSeries series = new XYSeries("Properties Owned");
        // month 0 — at simulation start (within isolated context), 0 properties.
        series.add(0, 0);
        for (MonthSnapshot s : result.getMonthlySnapshots()) {
            series.add(s.getMonth(), s.getPropertiesOwned());
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Properties Owned Over Time — " + result.getStrategyName(),
                "Month",
                "Count",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        // Swap in a step renderer — property count is discrete, steps are more honest.
        XYPlot plot = chart.getXYPlot();
        XYStepRenderer stepRenderer = new XYStepRenderer();
        stepRenderer.setSeriesPaint(0, PROPERTY_COLOR);
        stepRenderer.setSeriesStroke(0, new BasicStroke(2.2f));
        stepRenderer.setDefaultShapesVisible(false);
        plot.setRenderer(stepRenderer);

        // Integer ticks on the Y-axis (no half-properties).
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setAutoRangeIncludesZero(true);

        stylePlot(plot);
        styleTitle(chart);
        return chart;
    }

    /** Overlaid line chart comparing a single metric across multiple runs. */
    private JFreeChart createComparisonChart(List<SimulationResult> results, Metric metric) {
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (SimulationResult r : results) {
            XYSeries s = new XYSeries(uniqueLabel(dataset, r.getStrategyName()));
            // Anchor month 0
            s.add(0, metric == Metric.BALANCE
                    ? r.getStartingBalance().doubleValue()
                    : r.getStartingNetworth().doubleValue());
            for (MonthSnapshot snap : r.getMonthlySnapshots()) {
                double value = metric == Metric.BALANCE
                        ? snap.getBalance().doubleValue()
                        : snap.getNetworth().doubleValue();
                s.add(snap.getMonth(), value);
            }
            dataset.addSeries(s);
        }

        JFreeChart chart = ChartFactory.createXYLineChart(
                metric.label + " — Strategy Comparison",
                "Month",
                metric.label + " ($)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        styleXYChart(chart, SERIES_PALETTE);
        return chart;
    }

    /** Bar chart: final networth per strategy, sorted descending for easy reading. */
    private JFreeChart createFinalNetworthBarChart(List<SimulationResult> results) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Sort a copy descending by ending networth
        List<SimulationResult> sorted = new java.util.ArrayList<>(results);
        sorted.sort((a, b) -> b.getEndingNetworth().compareTo(a.getEndingNetworth()));

        for (SimulationResult r : sorted) {
            String label = shortName(r.getStrategyName(), 28);
            // Make labels unique even if two runs share a strategy name
            label = uniqueCategoryLabel(dataset, label);
            dataset.addValue(r.getEndingNetworth().doubleValue(),
                    "Final Net Worth", label);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Final Net Worth by Strategy",
                "Strategy",
                "Net Worth ($)",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(false);
        renderer.setMaximumBarWidth(0.15);
        for (int i = 0; i < dataset.getColumnCount(); i++) {
            renderer.setSeriesPaint(i, SERIES_PALETTE[i % SERIES_PALETTE.length]);
        }
        // Value labels on top of each bar.
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator(
                "${2,number,#,##0}",
                NumberFormat.getNumberInstance(Locale.US)));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(AXIS_FONT);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance(Locale.US));

        plot.setBackgroundPaint(BG_COLOR);
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setOutlineVisible(false);
        chart.setBackgroundPaint(BG_COLOR);
        styleTitle(chart);
        return chart;
    }

    // ────────────────────────────────────────────────────────────────
    //  Styling helpers
    // ────────────────────────────────────────────────────────────────

    private void styleXYChart(JFreeChart chart, Color[] palette) {
        XYPlot plot = chart.getXYPlot();

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
            Color c = palette[i % palette.length];
            renderer.setSeriesPaint(i, c);
            renderer.setSeriesStroke(i, new BasicStroke(2.2f));
        }
        plot.setRenderer(renderer);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setNumberFormatOverride(NumberFormat.getCurrencyInstance(Locale.US));
        rangeAxis.setAutoRangeIncludesZero(false);

        stylePlot(plot);
        styleTitle(chart);
    }

    private void stylePlot(XYPlot plot) {
        plot.setBackgroundPaint(BG_COLOR);
        plot.setDomainGridlinePaint(GRID_COLOR);
        plot.setRangeGridlinePaint(GRID_COLOR);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        plot.setOutlineVisible(false);

        plot.getDomainAxis().setTickLabelFont(AXIS_FONT);
        plot.getDomainAxis().setLabelFont(AXIS_FONT);
        plot.getRangeAxis().setTickLabelFont(AXIS_FONT);
        plot.getRangeAxis().setLabelFont(AXIS_FONT);
    }

    private void styleTitle(JFreeChart chart) {
        if (chart.getTitle() != null) {
            chart.getTitle().setFont(TITLE_FONT);
        }
        chart.setBackgroundPaint(BG_COLOR);
    }

    // ────────────────────────────────────────────────────────────────
    //  Frame helpers
    // ────────────────────────────────────────────────────────────────

    private ChartPanel wrap(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(SINGLE_CHART_SIZE);
        return panel;
    }

    private void showFrame(String title, Container content, Dimension size) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JComponent contentPane = (content instanceof JComponent)
                ? (JComponent) content
                : new JPanel(new BorderLayout()) {{ add(content, BorderLayout.CENTER); }};
        frame.setContentPane(contentPane);
        frame.setSize(size);
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
        frame.toFront();
    }

    // ────────────────────────────────────────────────────────────────
    //  Misc
    // ────────────────────────────────────────────────────────────────

    /** Returns true (and logs) if no display is available — caller should bail. */
    private boolean unavailable() {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("(Chart viewer unavailable: running in headless mode.)");
            return true;
        }
        return false;
    }

    private static String shortName(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    /** Ensure XY series labels are unique by appending a disambiguator if needed. */
    private static String uniqueLabel(XYSeriesCollection dataset, String proposed) {
        String label = proposed;
        int suffix = 2;
        while (dataset.getSeriesIndex(label) >= 0) {
            label = proposed + " (" + suffix++ + ")";
        }
        return label;
    }

    /** Same for category (bar) datasets — column keys must be unique. */
    private static String uniqueCategoryLabel(DefaultCategoryDataset dataset, String proposed) {
        String label = proposed;
        int suffix = 2;
        while (dataset.getColumnKeys().contains(label)) {
            label = proposed + " (" + suffix++ + ")";
        }
        return label;
    }

    /** Which metric to plot in a multi-run comparison. */
    private enum Metric {
        BALANCE("Balance"),
        NETWORTH("Net Worth");

        final String label;
        Metric(String label) { this.label = label; }
    }
}
