package com.InvestmentAccCalcEngine.cli;

import com.InvestmentAccCalcEngine.domain.ActiveMortgage;
import com.InvestmentAccCalcEngine.domain.MonthlyProjection;
import com.InvestmentAccCalcEngine.domain.MortgageSummary;
import com.InvestmentAccCalcEngine.domain.Property;
import com.InvestmentAccCalcEngine.domain.RentalProperty;
import com.InvestmentAccCalcEngine.service.*;
import com.InvestmentAccCalcEngine.service.loan.LoanType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuHandlerTest {

  private static final String ACCOUNT = "5791160";

  @Mock private BankAccountService bankAccountService;
  @Mock private SalaryService salaryService;
  @Mock private ProjectionService projectionService;
  @Mock private MortgageService mortgageService;
  @Mock private PropertyService propertyService;
  @Mock private RentalPropertyService rentalPropertyService;
  @Mock private NetworthService networthService;
  @Mock private DisplayFormatter display;

  private MenuHandler menuHandler;
  private InputStream originalSystemIn;

  @BeforeEach
  void saveSystemIn() {
    originalSystemIn = System.in;
  }

  @AfterEach
  void restoreSystemIn() {
    System.setIn(originalSystemIn);
  }

  /**
   * Rebuilds MenuHandler with simulated console input.
   */
  private void initWithInput(String simulatedInput) {
    System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));
    menuHandler = new MenuHandler(
        bankAccountService, salaryService, projectionService,
        mortgageService, propertyService, rentalPropertyService,
        networthService, display
    );
  }

  // ═══════════════════════════════════════════════
  //  Helpers
  // ═══════════════════════════════════════════════

  /**
   * Lenient stubs for printAccountInfo's internal calls.
   * Lenient because not every code path reaches printAccountInfo.
   */
  private void stubAccountInfoLenient(BigDecimal balance, BigDecimal networth) {
    lenient().when(bankAccountService.getBalance(ACCOUNT)).thenReturn(balance);
    lenient().when(networthService.getPrevNetworth()).thenReturn(networth);
  }

  /**
   * Creates a Property mock with all getters set as lenient stubs.
   * Lenient because the mocked DisplayFormatter swallows the Property
   * object without calling its getters, so not all stubs are hit
   * in every code path.
   */
  private Property mockProperty(String address, BigDecimal purchasePrice,
                                BigDecimal marketValue, int mortgageIndex) {
    Property prop = mock(Property.class);
    lenient().when(prop.getAddress()).thenReturn(address);
    lenient().when(prop.getPurchasePrice()).thenReturn(purchasePrice);
    lenient().when(prop.getCurrentMarketValue()).thenReturn(marketValue);
    lenient().when(prop.getMortgageIndex()).thenReturn(mortgageIndex);
    lenient().when(prop.isRentedOut()).thenReturn(false);
    lenient().when(prop.getRentalIndex()).thenReturn(-1);
    return prop;
  }

  // ═══════════════════════════════════════════════
  //  1 - Make a charge
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("1 - handleCharge")
  class HandleChargeTests {

    @Test
    @DisplayName("charges account and displays confirmation")
    void chargesAccountAndDisplaysConfirmation() {
      initWithInput("250.00\nGroceries\n");
      stubAccountInfoLenient(BigDecimal.valueOf(750), BigDecimal.valueOf(750));

      menuHandler.handleCharge(ACCOUNT, 3);

      verify(bankAccountService).charge(ACCOUNT, new BigDecimal("250.00"));
      verify(display).printChargeConfirmation(new BigDecimal("250.00"), "Groceries");
      verify(display).printAccountSummary(eq(3), any(), any());
    }
  }

  // ═══════════════════════════════════════════════
  //  2 - Make a deposit
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("2 - handleDeposit")
  class HandleDepositTests {

    @Test
    @DisplayName("deposits to account and displays confirmation")
    void depositsAndDisplaysConfirmation() {
      initWithInput("500.00\n");
      stubAccountInfoLenient(BigDecimal.valueOf(1500), BigDecimal.valueOf(1500));

      menuHandler.handleDeposit(ACCOUNT, 5);

      verify(bankAccountService).deposit(ACCOUNT, new BigDecimal("500.00"));
      verify(display).printDepositConfirmation(new BigDecimal("500.00"));
      verify(display).printAccountSummary(eq(5), any(), any());
    }
  }

  // ═══════════════════════════════════════════════
  //  3 - View future projections
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("3 - handleProjections")
  class HandleProjectionsTests {

    @Test
    @DisplayName("calls projection service with correct params and displays results")
    void projectsAndDisplays() {
      initWithInput("12\n2000.00\n");

      List<MonthlyProjection> mockProjections = List.of();
      when(projectionService.projectBalance(eq(ACCOUNT), any(), anyList(), eq(12)))
          .thenReturn(mockProjections);

      menuHandler.handleProjections(ACCOUNT, BigDecimal.valueOf(87000));

      verify(projectionService).projectBalance(
          eq(ACCOUNT),
          eq(BigDecimal.valueOf(87000)),
          eq(List.of(new BigDecimal("2000.00"))),
          eq(12)
      );
      verify(display).printProjections(mockProjections);
    }
  }

  // ═══════════════════════════════════════════════
  //  4 - View account info
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("4 - printAccountInfo")
  class PrintAccountInfoTests {

    @Test
    @DisplayName("fetches balance and networth, delegates to display")
    void displaysBalanceAndNetworth() {
      initWithInput("");

      when(bankAccountService.getBalance(ACCOUNT)).thenReturn(BigDecimal.valueOf(80000));
      when(networthService.getPrevNetworth()).thenReturn(BigDecimal.valueOf(95000));

      menuHandler.printAccountInfo(ACCOUNT, 7);

      verify(display).printAccountSummary(7, BigDecimal.valueOf(80000), BigDecimal.valueOf(95000));
    }
  }

  // ═══════════════════════════════════════════════
  //  5 - Take out a mortgage
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("5 - handleTakeOutMortgage")
  class HandleTakeOutMortgageTests {

    private LoanType mockLoanType;

    /**
     * Stubs loan type lookup. calculate() is lenient because
     * early-exit paths (invalid type, cancel) won't reach it.
     */
    private void stubLoanTypes() {
      mockLoanType = mock(LoanType.class);
      lenient().when(mockLoanType.getName()).thenReturn("Conventional 30yr");
      Map<String, LoanType> loanTypes = new LinkedHashMap<>();
      loanTypes.put("1", mockLoanType);
      when(mortgageService.getAvailableLoanTypes()).thenReturn(loanTypes);

      MortgageSummary mockSummary = mock(MortgageSummary.class);
      lenient().when(mortgageService.calculate(any(), any(), any(), any(), anyInt()))
          .thenReturn(mockSummary);
    }

    @Test
    @DisplayName("confirmed mortgage: creates mortgage and displays approval")
    void confirmedPath() {
      initWithInput("123 Main St\n1\n300\n60\n6.5\n30\ny\n");
      stubLoanTypes();

      ActiveMortgage mockMortgage = mock(ActiveMortgage.class);
      lenient().when(mockMortgage.isPmiRequired()).thenReturn(false);
      when(mortgageService.takeOutMortgage(
          eq(ACCOUNT), eq(mockLoanType), any(), any(), any(), eq(30), eq("123 Main St")))
          .thenReturn(mockMortgage);

      menuHandler.handleTakeOutMortgage(ACCOUNT);

      verify(mortgageService).takeOutMortgage(
          eq(ACCOUNT), eq(mockLoanType),
          eq(BigDecimal.valueOf(300000)),
          eq(BigDecimal.valueOf(60000)),
          eq(new BigDecimal("6.5")),
          eq(30),
          eq("123 Main St")
      );
      verify(display).printMortgageApproved(eq("123 Main St"), eq(BigDecimal.valueOf(60000)), eq(mockMortgage));
    }

    @Test
    @DisplayName("cancelled mortgage: does not call takeOutMortgage")
    void cancelledPath() {
      initWithInput("123 Main St\n1\n300\n60\n6.5\n30\nn\n");
      stubLoanTypes();

      menuHandler.handleTakeOutMortgage(ACCOUNT);

      verify(mortgageService, never()).takeOutMortgage(
          any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("invalid loan type: returns early without proceeding")
    void invalidLoanType() {
      initWithInput("123 Main St\n99\n");

      // Only stub what this path actually reaches
      mockLoanType = mock(LoanType.class);
      lenient().when(mockLoanType.getName()).thenReturn("Conventional 30yr");
      Map<String, LoanType> loanTypes = new LinkedHashMap<>();
      loanTypes.put("1", mockLoanType);
      when(mortgageService.getAvailableLoanTypes()).thenReturn(loanTypes);

      menuHandler.handleTakeOutMortgage(ACCOUNT);

      verify(mortgageService, never()).calculate(any(), any(), any(), any(), anyInt());
      verify(mortgageService, never()).takeOutMortgage(
          any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("mortgage rejected by service: catches exception gracefully")
    void rejectedByService() {
      initWithInput("123 Main St\n1\n300\n60\n6.5\n30\ny\n");
      stubLoanTypes();

      when(mortgageService.takeOutMortgage(
          any(), any(), any(), any(), any(), anyInt(), any()))
          .thenThrow(new IllegalArgumentException("Insufficient funds"));

      menuHandler.handleTakeOutMortgage(ACCOUNT);

      verify(display, never()).printMortgageApproved(any(), any(), any());
    }
  }

  // ═══════════════════════════════════════════════
  //  6 - View active mortgages
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("6 - handleViewActiveMortgages")
  class HandleViewActiveMortgagesTests {

    @Test
    @DisplayName("delegates to display with mortgage list and property lookup")
    void delegatesToDisplay() {
      initWithInput("");

      List<ActiveMortgage> mortgages = List.of(mock(ActiveMortgage.class));
      when(mortgageService.getActiveMortgages()).thenReturn(mortgages);

      menuHandler.handleViewActiveMortgages();

      verify(display).printActiveMortgages(eq(mortgages), any());
    }
  }

  // ═══════════════════════════════════════════════
  //  7 - Rent out a property
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("7 - handleAddRentalProperty")
  class HandleAddRentalPropertyTests {

    @Test
    @DisplayName("no available properties: prints message and returns")
    void noPropertiesAvailable() {
      initWithInput("");

      when(propertyService.getAvailableForRental()).thenReturn(Collections.emptyList());

      menuHandler.handleAddRentalProperty(BigDecimal.valueOf(0.008));

      verify(rentalPropertyService, never()).addProperty(any());
    }

    @Test
    @DisplayName("already rented property selected: rejects and returns")
    void alreadyRented() {
      initWithInput("1\n");

      Property rentedProp = mock(Property.class);
      when(rentedProp.isRentedOut()).thenReturn(true);

      when(propertyService.getAvailableForRental()).thenReturn(List.of(mock(Property.class)));
      when(propertyService.getProperties()).thenReturn(List.of(rentedProp));

      menuHandler.handleAddRentalProperty(BigDecimal.valueOf(0.008));

      verify(rentalPropertyService, never()).addProperty(any());
    }

    @Test
    @DisplayName("valid property selected: adds rental and displays summary")
    void addsRentalSuccessfully() {
      initWithInput("1\n");

      Property prop = mock(Property.class);
      when(prop.isRentedOut()).thenReturn(false);
      when(prop.getAddress()).thenReturn("789 Elm Blvd");
      when(prop.getPurchasePrice()).thenReturn(BigDecimal.valueOf(200000));
      when(prop.getMortgageIndex()).thenReturn(-1);

      when(propertyService.getAvailableForRental()).thenReturn(List.of(prop));
      when(propertyService.getProperties()).thenReturn(List.of(prop));
      when(rentalPropertyService.getProperties()).thenReturn(List.of(mock(RentalProperty.class)));
      when(rentalPropertyService.isSimplifiedMode()).thenReturn(true);

      menuHandler.handleAddRentalProperty(BigDecimal.valueOf(0.008));

      verify(rentalPropertyService).addProperty(any(RentalProperty.class));
      verify(prop).setRentalIndex(0);
      verify(display).printRentalPropertyAdded(
          any(RentalProperty.class), eq(prop), any(), any(), eq(true), isNull());
    }

    @Test
    @DisplayName("invalid selection index: returns without adding")
    void invalidSelection() {
      initWithInput("5\n");

      Property prop = mock(Property.class);
      when(propertyService.getAvailableForRental()).thenReturn(List.of(prop));
      when(propertyService.getProperties()).thenReturn(List.of(prop));

      menuHandler.handleAddRentalProperty(BigDecimal.valueOf(0.008));

      verify(rentalPropertyService, never()).addProperty(any());
    }
  }

  // ═══════════════════════════════════════════════
  //  8 - View rental properties
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("8 - handleViewRentalProperties")
  class HandleViewRentalPropertiesTests {

    @Test
    @DisplayName("delegates to display with rental list and mortgages")
    void delegatesToDisplay() {
      initWithInput("");

      List<RentalProperty> rentals = List.of(mock(RentalProperty.class));
      List<ActiveMortgage> mortgages = List.of();
      when(rentalPropertyService.getProperties()).thenReturn(rentals);
      when(mortgageService.getActiveMortgages()).thenReturn(mortgages);

      menuHandler.handleViewRentalProperties();

      verify(display).printRentalProperties(rentals, mortgages);
    }
  }

  // ═══════════════════════════════════════════════
  //  9 - View owned properties
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("9 - handleViewOwnedProperties")
  class HandleViewOwnedPropertiesTests {

    @Test
    @DisplayName("delegates to display with property list and mortgages")
    void delegatesToDisplay() {
      initWithInput("");

      List<Property> props = List.of(mock(Property.class));
      List<ActiveMortgage> mortgages = List.of();
      when(propertyService.getProperties()).thenReturn(props);
      when(mortgageService.getActiveMortgages()).thenReturn(mortgages);

      menuHandler.handleViewOwnedProperties();

      verify(display).printOwnedProperties(props, mortgages);
    }
  }

  // ═══════════════════════════════════════════════
  //  10 - Sell a property
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("10 - handleSellProperty")
  class HandleSellPropertyTests {

    @Test
    @DisplayName("no properties to sell: prints message and returns")
    void noProperties() {
      initWithInput("");
      when(propertyService.getProperties()).thenReturn(Collections.emptyList());

      menuHandler.handleSellProperty(ACCOUNT);

      verify(bankAccountService, never()).deposit(any(), any());
      verify(mortgageService, never()).sellProperty(any(), anyInt(), any());
    }

    @Test
    @DisplayName("confirmed sale without mortgage: deposits proceeds")
    void confirmedSaleNoMortgage() {
      initWithInput("1\ny\n");

      Property prop = mockProperty("100 Sale St",
          BigDecimal.valueOf(200000), BigDecimal.valueOf(210000), -1);

      when(propertyService.getProperties()).thenReturn(new ArrayList<>(List.of(prop)));
      stubAccountInfoLenient(BigDecimal.valueOf(300000), BigDecimal.valueOf(300000));

      menuHandler.handleSellProperty(ACCOUNT);

      verify(bankAccountService).deposit(eq(ACCOUNT), any(BigDecimal.class));
      verify(propertyService).removeProperty(0);
      verify(display).printSaleSuccess(any());
    }

    @Test
    @DisplayName("confirmed sale with mortgage: calls mortgageService.sellProperty")
    void confirmedSaleWithMortgage() {
      initWithInput("1\ny\n");

      Property prop = mockProperty("200 Mortgage Ln",
          BigDecimal.valueOf(300000), BigDecimal.valueOf(310000), 0);

      ActiveMortgage mortgage = mock(ActiveMortgage.class);
      when(mortgage.getRemainingBalance()).thenReturn(BigDecimal.valueOf(200000));
      when(mortgageService.getMortgage(0)).thenReturn(mortgage);

      when(propertyService.getProperties()).thenReturn(new ArrayList<>(List.of(prop)));
      stubAccountInfoLenient(BigDecimal.valueOf(100000), BigDecimal.valueOf(100000));

      menuHandler.handleSellProperty(ACCOUNT);

      verify(mortgageService).sellProperty(eq(ACCOUNT), eq(0), any(BigDecimal.class));
      verify(display).printSaleSuccess(any());
    }

    @Test
    @DisplayName("cancelled sale: no changes made")
    void cancelledSale() {
      initWithInput("1\nn\n");

      Property prop = mockProperty("300 Cancel Dr",
          BigDecimal.valueOf(250000), BigDecimal.valueOf(260000), -1);

      when(propertyService.getProperties()).thenReturn(List.of(prop));

      menuHandler.handleSellProperty(ACCOUNT);

      verify(propertyService, never()).removeProperty(anyInt());
      verify(bankAccountService, never()).deposit(eq(ACCOUNT), any());
    }

    @Test
    @DisplayName("sale of rented property: removes rental and adjusts indices")
    void saleOfRentedProperty() {
      initWithInput("1\ny\n");

      Property prop = mockProperty("400 Rented Ave",
          BigDecimal.valueOf(200000), BigDecimal.valueOf(220000), -1);
      // Override the lenient defaults for this specific scenario
      when(prop.isRentedOut()).thenReturn(true);
      when(prop.getRentalIndex()).thenReturn(2);

      when(propertyService.getProperties()).thenReturn(new ArrayList<>(List.of(prop)));
      stubAccountInfoLenient(BigDecimal.valueOf(500000), BigDecimal.valueOf(500000));

      menuHandler.handleSellProperty(ACCOUNT);

      verify(rentalPropertyService).removeProperty(2);
      verify(propertyService).adjustRentalIndicesAfterRemoval(2);
      verify(propertyService).removeProperty(0);
    }
  }

  // ═══════════════════════════════════════════════
  //  nh - Networth history
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("nh - handleViewNetworthHistory")
  class HandleViewNetworthHistoryTests {

    @Test
    @DisplayName("delegates to display with history list")
    void delegatesToDisplay() {
      initWithInput("");

      ArrayList<BigDecimal> history = new ArrayList<>(List.of(
          BigDecimal.valueOf(80000),
          BigDecimal.valueOf(85000),
          BigDecimal.valueOf(90000)
      ));
      when(networthService.getNetworthHistory()).thenReturn(history);

      menuHandler.handleViewNetworthHistory();

      verify(display).printNetworthHistory(history);
    }
  }

  // ═══════════════════════════════════════════════
  //  sr - Toggle simplified rental
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("sr - handleToggleSimplifiedRental")
  class HandleToggleSimplifiedRentalTests {

    @Test
    @DisplayName("toggles from OFF to ON")
    void togglesOffToOn() {
      initWithInput("");
      when(rentalPropertyService.isSimplifiedMode()).thenReturn(false);

      menuHandler.handleToggleSimplifiedRental();

      verify(rentalPropertyService).setSimplifiedMode(true);
      verify(display).printSimplifiedRentalToggle(true);
    }

    @Test
    @DisplayName("toggles from ON to OFF")
    void togglesOnToOff() {
      initWithInput("");
      when(rentalPropertyService.isSimplifiedMode()).thenReturn(true);

      menuHandler.handleToggleSimplifiedRental();

      verify(rentalPropertyService).setSimplifiedMode(false);
      verify(display).printSimplifiedRentalToggle(false);
    }
  }

  // ═══════════════════════════════════════════════
  //  Input helpers
  // ═══════════════════════════════════════════════

  @Nested
  @DisplayName("Input helpers")
  class InputHelperTests {

    @Test
    @DisplayName("readLine trims whitespace")
    void readLineTrims() {
      initWithInput("  hello world  \n");
      String result = menuHandler.readLine();
      assert result.equals("hello world");
    }

    @Test
    @DisplayName("readInt reads integer and consumes newline")
    void readIntWorks() {
      initWithInput("42\n");
      int result = menuHandler.readInt();
      assert result == 42;
    }
  }
}