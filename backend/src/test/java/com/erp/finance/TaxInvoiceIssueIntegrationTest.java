package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.DataScope;
import com.erp.common.security.UserAccessProfile;
import com.erp.common.security.UserAccessProfileRepository;
import com.erp.finance.application.dto.ArInvoiceCreateRequest;
import com.erp.finance.application.dto.ArInvoiceLineRequest;
import com.erp.finance.application.dto.CompanyProfileUpdateRequest;
import com.erp.finance.application.dto.TaxInvoiceIssueRequest;
import com.erp.finance.application.dto.VatAccountUpdateRequest;
import com.erp.finance.application.service.ArInvoiceService;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.CompanyProfileService;
import com.erp.finance.application.service.TaxInvoiceService;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.FiscalPeriod;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.model.NormalBalance;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.CustomerRepository;
import com.erp.finance.domain.repository.FiscalPeriodRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전자세금계산서 발행 전체 흐름(실 DB) — 회사정보 설정 → 과세 AR 생성·승인 → 세금계산서 발행 → 공급자/공급받는자 스냅샷·금액·세액이 영속되고 XML로 직렬화되는지
 * 검증한다. 단위 테스트(mock)가 못 잡는 @Embedded 스냅샷 매핑·부분 유니크 인덱스·세액 승계를 실제 Hibernate/PostgreSQL에서 확인한다.
 */
@Transactional
class TaxInvoiceIssueIntegrationTest extends AbstractIntegrationTest {

  @Autowired private TaxInvoiceService taxInvoiceService;
  @Autowired private TaxInvoiceRepository taxInvoiceRepository;
  @Autowired private ArInvoiceService arInvoiceService;
  @Autowired private CompanyProfileService companyProfileService;
  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private CustomerRepository customerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private FiscalYearRepository fiscalYearRepository;
  @Autowired private FiscalPeriodRepository fiscalPeriodRepository;
  @Autowired private UserAccessProfileRepository accessProfileRepository;

  private Long revenueAccountId;
  private Long vatPayableAccountId;
  private Long customerId;

  @BeforeEach
  void setUp() {
    FiscalYear fy =
        fiscalYearRepository.save(
            FiscalYear.of(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
    fiscalPeriodRepository.save(
        FiscalPeriod.of(fy, 1, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

    Account revenue =
        accountRepository.save(
            Account.of("40100", "매출", AccountType.REVENUE, NormalBalance.CREDIT, null, false));
    Account arControl =
        accountRepository.save(
            Account.of("11100", "외상매출금", AccountType.ASSET, NormalBalance.DEBIT, null, false));
    Account vatPayable =
        accountRepository.save(
            Account.of(
                "25500", "부가세예수금", AccountType.LIABILITY, NormalBalance.CREDIT, null, false));
    revenueAccountId = revenue.getId();
    vatPayableAccountId = vatPayable.getId();

    Customer customer = Customer.of("C-TI", "(주)바이어", "2208612345", "담당", null, null, 30);
    customer.assignTaxIdentity("이대표", "부산시 해운대구 1", "도소매", "전자제품");
    customer.assignReceivablesAccount(arControl);
    customerId = customerRepository.save(customer).getId();
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

  private void setCompanyProfile() {
    authenticate("admin", BigDecimal.ZERO, "finance:setting:write");
    companyProfileService.updateCompanyProfile(
        new CompanyProfileUpdateRequest(
            "(주)글로벌무역", "1208147521", "홍대표", "서울시 종로구 1", "제조", "전자부품"));
    // 과세 매출의 부가세 분리를 위해 부가세예수금 통제계정 설정(미설정이면 AR이 EXEMPT로 게이팅됨).
    baseCurrencyService.updateVatAccounts(new VatAccountUpdateRequest(null, vatPayableAccountId));
  }

  private Long approvedArInvoice(String invoiceNo, BigDecimal supply) {
    authenticate("creator", BigDecimal.ZERO, "finance:write");
    var created =
        arInvoiceService.create(
            new ArInvoiceCreateRequest(
                invoiceNo,
                customerId,
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 2, 10),
                supply,
                TaxType.TAXABLE,
                "KRW",
                null,
                List.of(new ArInvoiceLineRequest(revenueAccountId, supply, "매출"))));
    arInvoiceService.submit(created.id());
    authenticate("approver", new BigDecimal("100000000"), "finance:invoice:approve");
    arInvoiceService.approve(created.id());
    return created.id();
  }

  @Test
  void issue_persistsSupplierBuyerSnapshotAndCarriesVat() {
    setCompanyProfile();
    Long arId = approvedArInvoice("AR-TI-1", new BigDecimal("1000000"));

    authenticate("issuer", BigDecimal.ZERO, "finance:write", "finance:read");
    var issued =
        taxInvoiceService.issue(
            arId, new TaxInvoiceIssueRequest(null, ChargeType.CHARGE, "전자부품 외", null));

    // 응답 + DB 영속 스냅샷 검증(@Embedded 매핑이 실제 컬럼에 저장·복원되는지)
    TaxInvoice persisted = taxInvoiceRepository.findById(issued.id()).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(TaxInvoiceStatus.ISSUED);
    assertThat(persisted.getIssueNo()).startsWith("TI-");
    assertThat(persisted.getArInvoiceId()).isEqualTo(arId);
    // 공급자 = 회사정보 스냅샷
    assertThat(persisted.getSupplier().getCompanyName()).isEqualTo("(주)글로벌무역");
    assertThat(persisted.getSupplier().getBusinessNo()).isEqualTo("1208147521");
    assertThat(persisted.getSupplier().getRepresentative()).isEqualTo("홍대표");
    assertThat(persisted.getSupplier().getBusinessType()).isEqualTo("제조");
    // 공급받는자 = 거래처 스냅샷
    assertThat(persisted.getBuyer().getCompanyName()).isEqualTo("(주)바이어");
    assertThat(persisted.getBuyer().getRepresentative()).isEqualTo("이대표");
    assertThat(persisted.getBuyer().getBusinessItem()).isEqualTo("전자제품");
    // 세액 승계(과세 10%)
    assertThat(persisted.getSupplyAmount()).isEqualByComparingTo("1000000");
    assertThat(persisted.getVatAmount()).isEqualByComparingTo("100000");
    assertThat(persisted.getTotalAmount()).isEqualByComparingTo("1100000");

    // XML 직렬화에 영속 스냅샷이 반영
    String xml = taxInvoiceService.generateXml(issued.id());
    assertThat(xml).contains("(주)글로벌무역").contains("이대표").contains("1100000");
  }

  @Test
  void issue_duplicateForSameArInvoice_throwsAlreadyIssued() {
    setCompanyProfile();
    Long arId = approvedArInvoice("AR-TI-2", new BigDecimal("500000"));

    authenticate("issuer", BigDecimal.ZERO, "finance:write");
    taxInvoiceService.issue(arId, new TaxInvoiceIssueRequest(null, null, null, null));

    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                taxInvoiceService.issue(arId, new TaxInvoiceIssueRequest(null, null, null, null)));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAX_INVOICE_ALREADY_ISSUED);
  }

  @Test
  void issue_withoutCompanyProfile_throwsRequired() {
    // 회사정보 미설정 → 발행 차단(빈 공급자 발행 금지).
    Long arId = approvedArInvoice("AR-TI-3", new BigDecimal("300000"));

    authenticate("issuer", BigDecimal.ZERO, "finance:write");
    ErpException ex =
        assertThrows(
            ErpException.class,
            () ->
                taxInvoiceService.issue(arId, new TaxInvoiceIssueRequest(null, null, null, null)));
    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMPANY_PROFILE_REQUIRED);
  }
}
