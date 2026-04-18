package com.InvestmentAccCalcEngine.simulator;

/**
 * ═══════════════════════════════════════════════════════════════
 *  HOW TO ADD Resettable TO YOUR SERVICES
 * ═══════════════════════════════════════════════════════════════
 *
 * Each service that holds mutable state needs two methods:
 *   - snapshot()  → deep-copy the current state
 *   - restore()   → replace current state with the copy
 *
 * Below are examples for each service. Copy the pattern into
 * your actual service classes.
 *
 * ───────────────────────────────────────────────────────────────
 *  1. BankAccountService
 * ───────────────────────────────────────────────────────────────
 *
 *  // Add "implements Resettable" to the class declaration
 *  @Service
 *  public class BankAccountService implements Resettable {
 *
 *      private Map<String, BigDecimal> accounts = new HashMap<>();
 *
 *      @Override
 *      public Object snapshot() {
 *          return new HashMap<>(accounts);  // shallow copy is fine for immutable BigDecimal values
 *      }
 *
 *      @Override
 *      @SuppressWarnings("unchecked")
 *      public void restore(Object state) {
 *          accounts = new HashMap<>((Map<String, BigDecimal>) state);
 *      }
 *
 *      // ... rest of service unchanged ...
 *  }
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  2. MortgageService
 * ───────────────────────────────────────────────────────────────
 *
 *  @Service
 *  public class MortgageService implements Resettable {
 *
 *      private List<ActiveMortgage> activeMortgages = new ArrayList<>();
 *
 *      @Override
 *      public Object snapshot() {
 *          // Deep copy each mortgage (ActiveMortgage needs a copy constructor or clone)
 *          return activeMortgages.stream()
 *              .map(ActiveMortgage::new)  // copy constructor
 *              .collect(Collectors.toCollection(ArrayList::new));
 *      }
 *
 *      @Override
 *      @SuppressWarnings("unchecked")
 *      public void restore(Object state) {
 *          activeMortgages = (List<ActiveMortgage>) state;
 *      }
 *  }
 *
 *  // ActiveMortgage needs a copy constructor:
 *  public class ActiveMortgage {
 *      public ActiveMortgage(ActiveMortgage other) {
 *          this.houseCost = other.houseCost;
 *          this.originalLoanAmount = other.originalLoanAmount;
 *          this.remainingBalance = other.remainingBalance;
 *          this.monthlyPayment = other.monthlyPayment;
 *          this.monthsPaid = other.monthsPaid;
 *          this.totalMonths = other.totalMonths;
 *          // ... copy ALL fields ...
 *      }
 *  }
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  3. PropertyService
 * ───────────────────────────────────────────────────────────────
 *
 *  @Service
 *  public class PropertyService implements Resettable {
 *
 *      private List<Property> properties = new ArrayList<>();
 *
 *      @Override
 *      public Object snapshot() {
 *          return properties.stream()
 *              .map(Property::new)  // copy constructor
 *              .collect(Collectors.toCollection(ArrayList::new));
 *      }
 *
 *      @Override
 *      @SuppressWarnings("unchecked")
 *      public void restore(Object state) {
 *          properties = (List<Property>) state;
 *      }
 *  }
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  4. RentalPropertyService
 * ───────────────────────────────────────────────────────────────
 *
 *  @Service
 *  public class RentalPropertyService implements Resettable {
 *
 *      private List<RentalProperty> properties = new ArrayList<>();
 *      private boolean simplifiedMode;
 *
 *      @Override
 *      public Object snapshot() {
 *          Map<String, Object> snap = new HashMap<>();
 *          snap.put("properties", properties.stream()
 *              .map(RentalProperty::new)
 *              .collect(Collectors.toCollection(ArrayList::new)));
 *          snap.put("simplifiedMode", simplifiedMode);
 *          return snap;
 *      }
 *
 *      @Override
 *      @SuppressWarnings("unchecked")
 *      public void restore(Object state) {
 *          Map<String, Object> snap = (Map<String, Object>) state;
 *          properties = (List<RentalProperty>) snap.get("properties");
 *          simplifiedMode = (Boolean) snap.get("simplifiedMode");
 *      }
 *  }
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  5. NetworthService
 * ───────────────────────────────────────────────────────────────
 *
 *  @Service
 *  public class NetworthService implements Resettable {
 *
 *      private ArrayList<BigDecimal> networthHistory = new ArrayList<>();
 *      private BigDecimal prevNetworth = BigDecimal.ZERO;
 *
 *      @Override
 *      public Object snapshot() {
 *          Map<String, Object> snap = new HashMap<>();
 *          snap.put("history", new ArrayList<>(networthHistory));
 *          snap.put("prevNetworth", prevNetworth);
 *          return snap;
 *      }
 *
 *      @Override
 *      @SuppressWarnings("unchecked")
 *      public void restore(Object state) {
 *          Map<String, Object> snap = (Map<String, Object>) state;
 *          networthHistory = (ArrayList<BigDecimal>) snap.get("history");
 *          prevNetworth = (BigDecimal) snap.get("prevNetworth");
 *      }
 *  }
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  DOMAIN OBJECTS — Copy Constructors Needed
 * ───────────────────────────────────────────────────────────────
 *
 *  Each domain object stored in a service list needs a copy constructor.
 *  BigDecimal fields are immutable so they can be assigned directly.
 *  Mutable fields (lists, other objects) need their own deep copies.
 *
 *  public class Property {
 *      public Property(Property other) {
 *          this.address = other.address;
 *          this.purchasePrice = other.purchasePrice;
 *          this.currentMarketValue = other.currentMarketValue;
 *          this.mortgageIndex = other.mortgageIndex;
 *          this.rentalIndex = other.rentalIndex;
 *          this.rentedOut = other.rentedOut;
 *      }
 *  }
 *
 *  // Same pattern for ActiveMortgage, RentalProperty, etc.
 *
 *
 * ───────────────────────────────────────────────────────────────
 *  CHECKLIST
 * ───────────────────────────────────────────────────────────────
 *
 *  [ ] BankAccountService implements Resettable
 *  [ ] MortgageService implements Resettable
 *  [ ] PropertyService implements Resettable
 *  [ ] RentalPropertyService implements Resettable
 *  [ ] NetworthService implements Resettable
 *  [ ] ActiveMortgage has copy constructor
 *  [ ] Property has copy constructor
 *  [ ] RentalProperty has copy constructor
 *  [ ] Run a simulation twice — results should be identical
 */
public final class ResettableGuide {
    private ResettableGuide() {} // This class is documentation only
}
