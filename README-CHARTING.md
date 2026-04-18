# JFreeChart Viewer — Integration Guide

Adds interactive charts to the Investment Account Calc Engine CLI. All charts
open as non-modal Swing windows so the CLI stays responsive. Right-clicking a
chart gives you pan/zoom, copy-to-clipboard, and save-as-PNG out of the box.

---

## 1. Add the Maven dependency

```xml
<dependency>
    <groupId>org.jfree</groupId>
    <artifactId>jfreechart</artifactId>
    <version>1.5.4</version>
</dependency>
```

JFreeChart 1.5.4 is the current stable release and works on Java 17+. No other
dependencies are needed — AWT/Swing ships with the JDK.

If you're on Gradle instead:

```groovy
implementation 'org.jfree:jfreechart:1.5.4'
```

---

## 2. Files in this drop

| File | Status | What it does |
|---|---|---|
| `src/main/java/com/InvestmentAccCalcEngine/viewer/ChartViewer.java` | **NEW** | Spring `@Component` — builds and shows all chart windows |
| `src/main/java/com/InvestmentAccCalcEngine/simulator/SimulationMenuHandler.java` | **MODIFIED** | Injects `ChartViewer`; offers charts after every run / comparison |
| `src/main/java/com/InvestmentAccCalcEngine/cli/MenuHandler.java` | **MODIFIED** | Injects `ChartViewer`; offers charts after projections + networth history |

Drop them into your source tree at those paths. Spring will pick up
`ChartViewer` via component-scan automatically — no config changes needed.

---

## 3. What gets charted

### From `SimulationResult` (your primary ask)

Triggered automatically after running a strategy, running `c` (compare all),
or viewing a saved result. Answer `y` to the "Open interactive charts?"
prompt that now appears.

**Single-run dashboard** — two tabs:
- **Financial** — balance and networth as overlaid lines over months. Month 0
  is anchored to starting values so the curves don't begin in midair.
- **Portfolio** — properties owned as a step chart (counts are discrete, so
  steps are more honest than a line).

**Multi-run comparison** — three tabs:
- **Net Worth** — overlaid networth curves, one per strategy
- **Balance** — overlaid balance curves, one per strategy
- **Final Net Worth** — bar chart, sorted descending, with value labels on
  each bar

### Other charts I added while I was in there

- **Net worth history** — added after `handleViewNetworthHistory()`. When you
  hit `nh` in the main CLI, after the text printout you're now asked "Open
  networth history chart? (y/n)".
- **Balance projection** — added after `handleProjections()`. When you hit
  `3` in the main CLI, same deal. The month-0 anchor uses your current
  account balance.

---

## 4. Menu extensions in the simulator

`v` (View previous results) now accepts two new shortcuts:

| Input | Action |
|---|---|
| `chart 2` | Open the dashboard for saved result #2 |
| `chart all` | Open the comparison view across all saved results |

Old inputs (`1`, `2`, `all`, `compare`) still work as before.

---

## 5. Design notes

- **Thread safety** — all Swing work is wrapped in `SwingUtilities.invokeLater`.
- **Headless-safe** — `ChartViewer` checks `GraphicsEnvironment.isHeadless()`
  before every call and logs `"(Chart viewer unavailable: running in headless
  mode.)"` rather than throwing. The CLI continues normally.
- **Non-modal** — charts open in their own `JFrame` with `DISPOSE_ON_CLOSE`.
  You can keep several open at once and compare visually.
- **Currency-formatted axes** — Y-axes use `NumberFormat.getCurrencyInstance`
  so values read as `$250,000` not `250000.0`.
- **Unique labels** — if two saved runs share a strategy name, the comparison
  chart appends `(2)`, `(3)`, etc. so the legend doesn't silently overwrite
  series.
- **Curated palette** — 8-color sequence picked to stay readable when many
  strategies are overlaid; first two colors (blue/green) are reserved for
  the single-run balance/networth chart for consistency.

---

## 6. Compile verification

The three delivered files compile clean against the existing project +
JFreeChart 1.5.4 API. No changes to `pom.xml` other than the one dependency
line above are needed.

---

## 7. Side note (not touched by this change)

I noticed `MenuHandler.java` imports `com.InvestmentAccCalcEngine.domain.MortgageSummary`
but the class actually lives at `com.InvestmentAccCalcEngine.service.loan.MortgageSummary`.
This is pre-existing — your real build presumably has this resolved (maybe
you've got a second copy, or the upload was slightly stale). Either way, my
changes don't touch it.

---

## 8. Quick API reference

If you want to call `ChartViewer` directly from elsewhere:

```java
@Autowired private ChartViewer chartViewer;

chartViewer.showSimulationDashboard(result);           // single run
chartViewer.showStrategyComparison(listOfResults);     // multi-run
chartViewer.showNetworthHistory(history);              // List<BigDecimal>
chartViewer.showBalanceProjection(projections, start); // List<MonthlyProjection> + anchor
```

All four methods return `void` and open their window asynchronously on the EDT.
