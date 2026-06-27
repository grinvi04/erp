package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.finance.application.ReferenceTypes;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoiceLineRequest;
import com.erp.finance.application.dto.ApInvoicePayRequest;
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoiceLineRequest;
import com.erp.finance.application.dto.ArInvoicePayRequest;
import com.erp.finance.application.dto.FxGainLossAccountUpdateRequest;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.application.service.ArInvoiceService;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.ExchangeRate;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.JournalEntry;
import com.erp.finance.domain.model.JournalLine;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import com.erp.finance.domain.repository.ExchangeRateRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.JournalEntryRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실현 환차손익 — 외화 AP 지급·AR 수금 시 결제환율≠인보이스환율 차액을 환차손/환차이익 라인으로 자동 분개하는 전체 흐름 검증(기준통화 KRW). 통제계정은
 * 인보이스환율로 청산되고 차대변이 균형한다. 폴백(기준통화·환율/계정 부재)은 기존 원통화 2라인을 유지한다.
 */
@Transactional
class FxGainLossPostingIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ApInvoiceService apInvoiceService;
  @Autowired private ArInvoiceService arInvoiceService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private VendorRepository vendorRepository;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private ExchangeRateRepository exchangeRateRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private UserAccessProfileRepository accessProfileRepository;

  private static final LocalDate INV_DATE = LocalDate.of(2025, 1, 10);
  private static final LocalDate PAY_DATE = LocalDate.of(2025, 1, 20);
  private static final BigDecimal LIMIT = new BigDecimal("100000000");

  private Long expenseAccountId;
  private Long revenueAccountId;
  private Long payablesAccountId;
  private Long receivablesAccountId;
  private Long cashAccountId;
  private Long fxGainAccountId;
  private Long fxLossAccountId;
  private Long vendorId;
  private Long customerId;

  @BeforeEach
  void setUp() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    fiscalPeriodRepository.save(
        FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

    expenseAccountId = saveAccount("51100", "소모품비", AccountType.EXPENSE, NormalBalance.DEBIT);
    revenueAccountId = saveAccount("40100", "매출", AccountType.REVENUE, NormalBalance.CREDIT);
    payablesAccountId = saveAccount("25100", "외상매입금", AccountType.LIABILITY, NormalBalance.CREDIT);
    receivablesAccountId = saveAccount("11100", "외상매출금", AccountType.ASSET, NormalBalance.DEBIT);
    cashAccountId = saveAccount("10100", "현금", AccountType.ASSET, NormalBalance.DEBIT);
    fxGainAccountId = saveAccount("41000", "외환차이익", AccountType.REVENUE, NormalBalance.CREDIT);
    fxLossAccountId = saveAccount("52000", "외환차손", AccountType.EXPENSE, NormalBalance.DEBIT);

    Vendor vendor = Vendor.of("V-FX", "공급사", "111-11-11111", "담당", "v@test.com", "010-1", 30);
    vendor.assignPayablesAccount(accountRepository.findById(payablesAccountId).orElseThrow());
    vendorId = vendorRepository.save(vendor).getId();

    Customer customer = Customer.of("C-FX", "고객사", "222-22-22222", "담당", "c@test.com", "010-2", 30);
    customer.assignReceivablesAccount(
        accountRepository.findById(receivablesAccountId).orElseThrow());
    customerId = customerRepository.save(customer).getId();
  }

  private Long saveAccount(String code, String name, AccountType type, NormalBalance balance) {
    return accountRepository.save(Account.of(code, name, type, balance, null, false)).getId();
  }

  private void authenticate(String sub, BigDecimal approvalLimit, String... authorities) {
    authenticate(sub, authorities);
    accessProfileRepository
        .findByTenantIdAndUserId(TEST_TENANT_ID, sub)
        .map(
            p -> {
              p.update(DataScope.ALL, null, approvalLimit);
              return p;
            })
        .orElseGet(
            () ->
                accessProfileRepository.save(
                    UserAccessProfile.of(TEST_TENANT_ID, sub, DataScope.ALL, null, approvalLimit)));
  }

  private void registerUsdRate(LocalDate date, String rate) {
    exchangeRateRepository.save(ExchangeRate.of("USD", "KRW", date, new BigDecimal(rate)));
  }

  private void configureFxAccounts() {
    authenticate("fxadmin", BigDecimal.ZERO, "finance:setting:write");
    baseCurrencyService.updateFxGainLossAccounts(
        new FxGainLossAccountUpdateRequest(fxGainAccountId, fxLossAccountId));
  }

  private Long createApprovedApInvoice(
      String no, String currency, LocalDate invoiceDate, BigDecimal amount) {
    authenticate("creator", BigDecimal.ZERO, "finance:write");
    var created =
        apInvoiceService.create(
            new ApInvoiceCreateRequest(
                no,
                vendorId,
                invoiceDate,
                invoiceDate.plusMonths(1),
                amount,
                currency,
                null,
                List.of(new ApInvoiceLineRequest(expenseAccountId, amount, "line"))));
    apInvoiceService.submit(created.id());
    authenticate("approver", LIMIT, "finance:invoice:approve");
    apInvoiceService.approve(created.id());
    return created.id();
  }

  private JournalEntry payApInvoice(Long invoiceId, BigDecimal amount, LocalDate paymentDate) {
    authenticate("payer", LIMIT, "finance:invoice:pay");
    apInvoiceService.pay(invoiceId, new ApInvoicePayRequest(amount, cashAccountId, paymentDate));
    return journalEntryRepository
        .findByReferenceTypeAndReferenceId(ReferenceTypes.AP_PAYMENT, invoiceId)
        .orElseThrow();
  }

  private Long createApprovedArInvoice(
      String no, String currency, LocalDate invoiceDate, BigDecimal amount) {
    authenticate("creator", BigDecimal.ZERO, "finance:write");
    var created =
        arInvoiceService.create(
            new ArInvoiceCreateRequest(
                no,
                customerId,
                invoiceDate,
                invoiceDate.plusMonths(1),
                amount,
                currency,
                null,
                List.of(new ArInvoiceLineRequest(revenueAccountId, amount, "line"))));
    arInvoiceService.submit(created.id());
    authenticate("approver", LIMIT, "finance:invoice:approve");
    arInvoiceService.approve(created.id());
    return created.id();
  }

  private JournalEntry receiveArInvoice(Long invoiceId, BigDecimal amount, LocalDate paymentDate) {
    authenticate("receiver", LIMIT, "finance:invoice:pay");
    arInvoiceService.pay(invoiceId, new ArInvoicePayRequest(amount, cashAccountId, paymentDate));
    return journalEntryRepository
        .findByReferenceTypeAndReferenceId(ReferenceTypes.AR_PAYMENT, invoiceId)
        .orElseThrow();
  }

  private BigDecimal debitOn(JournalEntry je, Long accountId) {
    return je.getLines().stream()
        .filter(l -> l.getAccount().getId().equals(accountId))
        .map(JournalLine::getDebitAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal creditOn(JournalEntry je, Long accountId) {
    return je.getLines().stream()
        .filter(l -> l.getAccount().getId().equals(accountId))
        .map(JournalLine::getCreditAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  // ---- AP ----

  @Test
  void ap_paymentRateHigher_postsFxLoss_andClearsPayablesAtInvoiceRate() {
    // AC-2/9: USD AP, 인보이스 1300 / 결제 1350 → (차)환차손 100×50=5000, 외상매입금 130000 청산, 균형.
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedApInvoice("AP-FX-LOSS", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1350");
    configureFxAccounts();

    JournalEntry je = payApInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("KRW");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getTotalDebit()).isEqualByComparingTo("135000");
    assertThat(debitOn(je, payablesAccountId)).isEqualByComparingTo("130000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("5000");
    assertThat(creditOn(je, cashAccountId)).isEqualByComparingTo("135000");
    assertThat(creditOn(je, fxGainAccountId)).isEqualByComparingTo("0");
  }

  @Test
  void ap_paymentRateLower_postsFxGain() {
    // AC-3/9: 결제환율(1250)이 인보이스환율(1300)보다 낮으면 (대)환차이익 5000, 균형.
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedApInvoice("AP-FX-GAIN", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1250");
    configureFxAccounts();

    JournalEntry je = payApInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("KRW");
    assertThat(je.isBalanced()).isTrue();
    assertThat(debitOn(je, payablesAccountId)).isEqualByComparingTo("130000");
    assertThat(creditOn(je, cashAccountId)).isEqualByComparingTo("125000");
    assertThat(creditOn(je, fxGainAccountId)).isEqualByComparingTo("5000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("0");
  }

  @Test
  void ap_partialPayment_fxProportionalToPaidAmount() {
    // AC-8: 부분지급 40 USD만 → 환차도 paidAmount 기준 40×50=2000, 청산 40×1300=52000.
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedApInvoice("AP-FX-PART", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1350");
    configureFxAccounts();

    JournalEntry je = payApInvoice(inv, new BigDecimal("40"), PAY_DATE);

    assertThat(je.isBalanced()).isTrue();
    assertThat(debitOn(je, payablesAccountId)).isEqualByComparingTo("52000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("2000");
    assertThat(creditOn(je, cashAccountId)).isEqualByComparingTo("54000");
  }

  // ---- AR (부호 반전) ----

  @Test
  void ar_paymentRateHigher_postsFxGain() {
    // AC-4/9: USD AR, 수금환율(1350)>인보이스(1300) → (대)환차이익 5000, 외상매출금 130000 청산, 균형.
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedArInvoice("AR-FX-GAIN", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1350");
    configureFxAccounts();

    JournalEntry je = receiveArInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("KRW");
    assertThat(je.isBalanced()).isTrue();
    assertThat(debitOn(je, cashAccountId)).isEqualByComparingTo("135000");
    assertThat(creditOn(je, receivablesAccountId)).isEqualByComparingTo("130000");
    assertThat(creditOn(je, fxGainAccountId)).isEqualByComparingTo("5000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("0");
  }

  @Test
  void ar_paymentRateLower_postsFxLoss() {
    // AC-5/9: 수금환율(1250)<인보이스(1300) → (차)환차손 5000, 균형.
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedArInvoice("AR-FX-LOSS", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1250");
    configureFxAccounts();

    JournalEntry je = receiveArInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.isBalanced()).isTrue();
    assertThat(debitOn(je, cashAccountId)).isEqualByComparingTo("125000");
    assertThat(creditOn(je, receivablesAccountId)).isEqualByComparingTo("130000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("5000");
    assertThat(creditOn(je, fxGainAccountId)).isEqualByComparingTo("0");
  }

  // ---- 경계·폴백 ----

  @Test
  void baseCurrency_keepsOriginalTwoLines_noFx() {
    // AC-6: 인보이스 통화=기준통화(KRW) → 기존 원통화 2라인 유지(환차 없음).
    Long inv = createApprovedApInvoice("AP-KRW", "KRW", INV_DATE, new BigDecimal("100000"));
    configureFxAccounts();

    JournalEntry je = payApInvoice(inv, new BigDecimal("100000"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("KRW");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getLines()).hasSize(2);
    assertThat(debitOn(je, payablesAccountId)).isEqualByComparingTo("100000");
    assertThat(creditOn(je, cashAccountId)).isEqualByComparingTo("100000");
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("0");
    assertThat(creditOn(je, fxGainAccountId)).isEqualByComparingTo("0");
  }

  @Test
  void fallback_invoiceRateNull_keepsForeignCurrencyTwoLines() {
    // AC-7: 인보이스 환율 스냅샷 없음(생성 시 환율 부재) → 환차 라인 없이 원통화 결제 분개.
    Long inv = createApprovedApInvoice("AP-NORATE", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1350"); // 결제일 환율은 있으나 인보이스 스냅샷이 null이라 폴백.
    configureFxAccounts();

    JournalEntry je = payApInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("USD");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getLines()).hasSize(2);
    assertThat(debitOn(je, payablesAccountId)).isEqualByComparingTo("100");
    assertThat(creditOn(je, cashAccountId)).isEqualByComparingTo("100");
  }

  @Test
  void fallback_fxAccountsNotConfigured_keepsForeignCurrencyTwoLines() {
    // AC-7: 환차손익 계정 미설정 → 환차 라인 없이 원통화 결제 분개(결제 차단 안 함).
    registerUsdRate(INV_DATE, "1300");
    Long inv = createApprovedApInvoice("AP-NOACC", "USD", INV_DATE, new BigDecimal("100"));
    registerUsdRate(PAY_DATE, "1350");
    // configureFxAccounts() 호출하지 않음.

    JournalEntry je = payApInvoice(inv, new BigDecimal("100"), PAY_DATE);

    assertThat(je.getCurrency()).isEqualTo("USD");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getLines()).hasSize(2);
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("0");
  }

  @Test
  void fallback_noPaymentRate_keepsForeignCurrencyTwoLines() {
    // AC-7: 결제일에 적용할 환율이 없으면(결제일이 환율 발효일 이전) 환차 라인 없이 원통화 결제 분개.
    registerUsdRate(LocalDate.of(2025, 1, 15), "1300"); // 발효일 1/15
    Long inv =
        createApprovedApInvoice(
            "AP-NOPAYRATE", "USD", LocalDate.of(2025, 1, 15), new BigDecimal("100"));
    configureFxAccounts();

    // 결제일 1/10 < 환율 발효일 1/15 → 결제일 환율 부재.
    JournalEntry je = payApInvoice(inv, new BigDecimal("100"), LocalDate.of(2025, 1, 10));

    assertThat(je.getCurrency()).isEqualTo("USD");
    assertThat(je.isBalanced()).isTrue();
    assertThat(je.getLines()).hasSize(2);
    assertThat(debitOn(je, fxLossAccountId)).isEqualByComparingTo("0");
  }
}
