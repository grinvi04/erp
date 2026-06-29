package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.TaxInvoiceIssueRequest;
import com.erp.finance.application.dto.TaxInvoiceResponse;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.CompanyProfile;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaxInvoiceServiceTest {

  @Mock private TaxInvoiceRepository taxInvoiceRepository;
  @Mock private ArInvoiceRepository arInvoiceRepository;
  @Mock private CompanyProfileService companyProfileService;
  @Mock private PermissionChecker permissionChecker;

  @InjectMocks private TaxInvoiceService service;

  private static final LocalDate INVOICE_DATE = LocalDate.of(2026, 6, 1);

  private static Customer buyer() {
    Customer c = Customer.of("C1", "(주)바이어", "2208612345", "담당자", null, null, 30);
    c.assignTaxIdentity("이대표", "서울시 강남구 2", "도소매", "전자제품");
    return c;
  }

  private static ArInvoice approvedAr(BigDecimal supply, TaxType type) {
    ArInvoice ar =
        ArInvoice.create(
            "AR-001", buyer(), INVOICE_DATE, INVOICE_DATE.plusDays(30), supply, type, "KRW", "비고");
    ar.submit();
    ar.approve();
    ReflectionTestUtils.setField(ar, "id", 7L);
    return ar;
  }

  private static CompanyProfile supplierProfile() {
    return CompanyProfile.of("(주)공급자", "1208800344", "홍대표", "서울시 종로구 1", "제조", "전자부품");
  }

  private void givenSavePersistsWithId(long id) {
    given(taxInvoiceRepository.save(any(TaxInvoice.class)))
        .willAnswer(
            inv -> {
              TaxInvoice t = inv.getArgument(0);
              ReflectionTestUtils.setField(t, "id", id);
              return t;
            });
  }

  private static TaxInvoiceIssueRequest emptyRequest() {
    return new TaxInvoiceIssueRequest(null, null, null, null);
  }

  @Test
  void issue_approved_snapshotsPartiesAndCarriesAmounts() {
    // AC-3: 공급자(회사정보)·공급받는자(거래처) 스냅샷 + AC-5: 세액 승계(공급가액 100만 → 세액 10만).
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(1_000_000), TaxType.TAXABLE)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.of(supplierProfile()));
    givenSavePersistsWithId(100L);

    TaxInvoiceResponse result =
        service.issue(7L, new TaxInvoiceIssueRequest(null, ChargeType.CHARGE, "전자부품 외", null));

    assertThat(result.status()).isEqualTo(TaxInvoiceStatus.ISSUED);
    assertThat(result.taxType()).isEqualTo(TaxType.TAXABLE);
    assertThat(result.supplyAmount()).isEqualByComparingTo(bd(1_000_000));
    assertThat(result.vatAmount()).isEqualByComparingTo(bd(100_000));
    assertThat(result.totalAmount()).isEqualByComparingTo(bd(1_100_000));
    assertThat(result.itemName()).isEqualTo("전자부품 외");
    // 공급자 스냅샷
    assertThat(result.supplier().companyName()).isEqualTo("(주)공급자");
    assertThat(result.supplier().businessNo()).isEqualTo("1208800344");
    assertThat(result.supplier().representative()).isEqualTo("홍대표");
    assertThat(result.supplier().businessType()).isEqualTo("제조");
    assertThat(result.supplier().businessItem()).isEqualTo("전자부품");
    // 공급받는자 스냅샷
    assertThat(result.buyer().companyName()).isEqualTo("(주)바이어");
    assertThat(result.buyer().businessNo()).isEqualTo("2208612345");
    assertThat(result.buyer().representative()).isEqualTo("이대표");
    assertThat(result.buyer().businessType()).isEqualTo("도소매");
    assertThat(result.buyer().businessItem()).isEqualTo("전자제품");
    // 발행번호 부여(id 기반)
    assertThat(result.issueNo()).isEqualTo("TI-00000100");
    assertThat(result.arInvoiceId()).isEqualTo(7L);
  }

  @Test
  void issue_zeroRated_carriesZeroVat() {
    // AC-5: 영세율 → 세액 0, 합계=공급가액.
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(500_000), TaxType.ZERO_RATED)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.of(supplierProfile()));
    givenSavePersistsWithId(101L);

    TaxInvoiceResponse result = service.issue(7L, emptyRequest());

    assertThat(result.taxType()).isEqualTo(TaxType.ZERO_RATED);
    assertThat(result.vatAmount()).isEqualByComparingTo(bd(0));
    assertThat(result.totalAmount()).isEqualByComparingTo(bd(500_000));
  }

  @Test
  void issue_writeDateOmitted_usesInvoiceDate() {
    // AC-11: 작성일자 미지정 → AR 인보이스 일자.
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(1_000_000), TaxType.TAXABLE)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.of(supplierProfile()));
    givenSavePersistsWithId(102L);

    TaxInvoiceResponse result = service.issue(7L, emptyRequest());

    assertThat(result.writeDate()).isEqualTo(INVOICE_DATE);
  }

  @Test
  void issue_chargeTypeAndItemOmitted_useDefaults() {
    // 경계: 청구/영수 미지정 → CHARGE, 품목명 미지정 → 기본 품목명(비어있지 않음).
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(1_000_000), TaxType.TAXABLE)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.of(supplierProfile()));
    givenSavePersistsWithId(103L);

    TaxInvoiceResponse result = service.issue(7L, emptyRequest());

    assertThat(result.chargeType()).isEqualTo(ChargeType.CHARGE);
    assertThat(result.itemName()).isNotBlank();
  }

  @Test
  void issue_paidInvoice_allowed() {
    // AC-3 경계: 완납(PAID) 인보이스도 발행 가능.
    ArInvoice ar = approvedAr(bd(1_000_000), TaxType.TAXABLE);
    ar.pay(bd(1_100_000)); // 전액 수금 → PAID
    given(arInvoiceRepository.findById(7L)).willReturn(Optional.of(ar));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.of(supplierProfile()));
    givenSavePersistsWithId(104L);

    TaxInvoiceResponse result = service.issue(7L, emptyRequest());

    assertThat(result.status()).isEqualTo(TaxInvoiceStatus.ISSUED);
  }

  @Test
  void issue_companyProfileMissing_throwsRequiredAndDoesNotSave() {
    // AC-4: 공급자(회사정보) 미설정이면 발행 차단(빈 값 발행 금지).
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(1_000_000), TaxType.TAXABLE)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(false);
    given(companyProfileService.currentCompanyProfile()).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> service.issue(7L, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COMPANY_PROFILE_REQUIRED);
    verify(taxInvoiceRepository, never()).save(any());
  }

  @Test
  void issue_arNotApprovedOrPaid_throwsNotIssuable() {
    // AC-3 가드: DRAFT 인보이스는 발행 불가.
    ArInvoice draft =
        ArInvoice.create(
            "AR-002",
            buyer(),
            INVOICE_DATE,
            INVOICE_DATE.plusDays(30),
            bd(1_000_000),
            TaxType.TAXABLE,
            "KRW",
            null);
    ReflectionTestUtils.setField(draft, "id", 8L);
    given(arInvoiceRepository.findById(8L)).willReturn(Optional.of(draft));

    ErpException ex = assertThrows(ErpException.class, () -> service.issue(8L, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AR_INVOICE_NOT_ISSUABLE);
    verify(taxInvoiceRepository, never()).save(any());
  }

  @Test
  void issue_alreadyIssued_throwsAlreadyIssued() {
    // AC-8: 동일 AR에 ISSUED 세금계산서가 있으면 중복 발행 거부(409).
    given(arInvoiceRepository.findById(7L))
        .willReturn(Optional.of(approvedAr(bd(1_000_000), TaxType.TAXABLE)));
    given(taxInvoiceRepository.existsByArInvoiceIdAndStatus(7L, TaxInvoiceStatus.ISSUED))
        .willReturn(true);

    ErpException ex = assertThrows(ErpException.class, () -> service.issue(7L, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAX_INVOICE_ALREADY_ISSUED);
    verify(taxInvoiceRepository, never()).save(any());
  }

  @Test
  void issue_arNotFound_throwsInvoiceNotFound() {
    given(arInvoiceRepository.findById(99L)).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> service.issue(99L, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVOICE_NOT_FOUND);
  }

  @Test
  void issue_withoutPermission_throwsForbidden() {
    // AC-9: 발행은 FINANCE_WRITE 필요.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_WRITE);

    ErpException ex = assertThrows(ErpException.class, () -> service.issue(7L, emptyRequest()));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void cancel_issued_setsCancelled() {
    // AC-7: ISSUED → CANCELLED.
    TaxInvoice issued = issuedTaxInvoice();
    given(taxInvoiceRepository.findById(50L)).willReturn(Optional.of(issued));

    TaxInvoiceResponse result = service.cancel(50L);

    assertThat(result.status()).isEqualTo(TaxInvoiceStatus.CANCELLED);
    assertThat(issued.getStatus()).isEqualTo(TaxInvoiceStatus.CANCELLED);
  }

  @Test
  void cancel_alreadyCancelled_throwsNotCancellable() {
    TaxInvoice issued = issuedTaxInvoice();
    issued.cancel();
    given(taxInvoiceRepository.findById(50L)).willReturn(Optional.of(issued));

    ErpException ex = assertThrows(ErpException.class, () -> service.cancel(50L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAX_INVOICE_NOT_CANCELLABLE);
  }

  @Test
  void cancel_withoutPermission_throwsForbidden() {
    // AC-9: 취소는 FINANCE_WRITE 필요.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_WRITE);

    ErpException ex = assertThrows(ErpException.class, () -> service.cancel(50L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void findById_notFound_throwsTaxInvoiceNotFound() {
    given(taxInvoiceRepository.findById(eq(404L))).willReturn(Optional.empty());

    ErpException ex = assertThrows(ErpException.class, () -> service.findById(404L));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TAX_INVOICE_NOT_FOUND);
  }

  private static TaxInvoice issuedTaxInvoice() {
    return TaxInvoice.issue(
        7L,
        TaxType.TAXABLE,
        ChargeType.CHARGE,
        INVOICE_DATE,
        bd(1_000_000),
        bd(100_000),
        bd(1_100_000),
        "전자부품",
        com.erp.finance.domain.model.PartySnapshot.of(
            "(주)공급자", "1208800344", "홍대표", "주소", "제조", "부품"),
        com.erp.finance.domain.model.PartySnapshot.of(
            "(주)바이어", "2208612345", "이대표", "주소", "도소매", "전자"),
        null);
  }

  private static BigDecimal bd(long v) {
    return BigDecimal.valueOf(v);
  }
}
