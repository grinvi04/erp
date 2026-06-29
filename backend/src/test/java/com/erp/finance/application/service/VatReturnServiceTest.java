package com.erp.finance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.VatReturnResponse;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.PartyAmountRow;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import com.erp.finance.domain.repository.TaxTypeAmountRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VatReturnServiceTest {

  @Mock private TaxInvoiceRepository taxInvoiceRepository;
  @Mock private ApInvoiceRepository apInvoiceRepository;
  @Mock private PermissionChecker permissionChecker;

  @InjectMocks private VatReturnService service;

  private static final LocalDate FROM = LocalDate.of(2025, 4, 1);
  private static final LocalDate TO = LocalDate.of(2025, 6, 30);

  private static TaxTypeAmountRow taxTypeRow(TaxType type, long supply, long vat) {
    TaxTypeAmountRow r = mock(TaxTypeAmountRow.class);
    given(r.getTaxType()).willReturn(type);
    given(r.getSupplyTotal()).willReturn(BigDecimal.valueOf(supply));
    given(r.getVatTotal()).willReturn(BigDecimal.valueOf(vat));
    return r;
  }

  private static PartyAmountRow partyRow(
      String bizNo, String name, long count, long supply, long vat) {
    PartyAmountRow r = mock(PartyAmountRow.class);
    given(r.getBusinessNo()).willReturn(bizNo);
    given(r.getName()).willReturn(name);
    given(r.getCount()).willReturn(count);
    given(r.getSupplyTotal()).willReturn(BigDecimal.valueOf(supply));
    given(r.getVatTotal()).willReturn(BigDecimal.valueOf(vat));
    return r;
  }

  @Test
  void getVatReturn_summarizesAndComputesPayable() {
    // AC-1,3: 매출세액 10만 - 매입세액 5만 = 납부 5만, 과세/영세율/면세 분리.
    var taxable = taxTypeRow(TaxType.TAXABLE, 1_000_000, 100_000);
    var zero = taxTypeRow(TaxType.ZERO_RATED, 500_000, 0);
    var exempt = taxTypeRow(TaxType.EXEMPT, 300_000, 0);
    var buyer = partyRow("2208612345", "(주)A", 2, 1_500_000, 100_000);
    var vendor = partyRow("3308612345", "(주)매입", 1, 500_000, 50_000);
    given(taxInvoiceRepository.sumSalesByTaxType(FROM, TO))
        .willReturn(List.of(taxable, zero, exempt));
    given(taxInvoiceRepository.aggregateSalesByBuyer(FROM, TO)).willReturn(List.of(buyer));
    given(apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO)).willReturn(List.of(vendor));

    VatReturnResponse result = service.getVatReturn(FROM, TO);

    assertThat(result.sales().taxableSupply()).isEqualByComparingTo("1000000");
    assertThat(result.sales().taxableVat()).isEqualByComparingTo("100000");
    assertThat(result.sales().zeroRatedSupply()).isEqualByComparingTo("500000");
    assertThat(result.sales().exemptSupply()).isEqualByComparingTo("300000");
    assertThat(result.sales().totalVat()).isEqualByComparingTo("100000");
    assertThat(result.purchases().supply()).isEqualByComparingTo("500000");
    assertThat(result.purchases().vat()).isEqualByComparingTo("50000");
    assertThat(result.payableTax()).isEqualByComparingTo("50000");
    assertThat(result.salesByBuyer()).hasSize(1);
    assertThat(result.salesByBuyer().get(0).count()).isEqualTo(2);
    assertThat(result.purchasesByVendor()).hasSize(1);
    assertThat(result.from()).isEqualTo(FROM);
    assertThat(result.to()).isEqualTo(TO);
  }

  @Test
  void getVatReturn_purchaseVatExceedsSales_negativePayableRefund() {
    // AC-3 경계: 매입세액 > 매출세액 → 음수(환급).
    var taxable = taxTypeRow(TaxType.TAXABLE, 1_000_000, 100_000);
    var vendor = partyRow("3308612345", "(주)매입", 1, 2_000_000, 200_000);
    given(taxInvoiceRepository.sumSalesByTaxType(FROM, TO)).willReturn(List.of(taxable));
    given(taxInvoiceRepository.aggregateSalesByBuyer(FROM, TO)).willReturn(List.of());
    given(apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO)).willReturn(List.of(vendor));

    VatReturnResponse result = service.getVatReturn(FROM, TO);

    assertThat(result.payableTax()).isEqualByComparingTo("-100000");
  }

  @Test
  void getVatReturn_emptyPeriod_zeros() {
    // AC-7: 데이터 없으면 0/빈 합계표.
    given(taxInvoiceRepository.sumSalesByTaxType(FROM, TO)).willReturn(List.of());
    given(taxInvoiceRepository.aggregateSalesByBuyer(FROM, TO)).willReturn(List.of());
    given(apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO)).willReturn(List.of());

    VatReturnResponse result = service.getVatReturn(FROM, TO);

    assertThat(result.sales().totalVat()).isEqualByComparingTo("0");
    assertThat(result.purchases().vat()).isEqualByComparingTo("0");
    assertThat(result.payableTax()).isEqualByComparingTo("0");
    assertThat(result.salesByBuyer()).isEmpty();
    assertThat(result.purchasesByVendor()).isEmpty();
  }

  @Test
  void getVatReturn_nullBusinessNoPreservedInLine() {
    // AC-8: 사업자번호 null 그룹도 누락 없이 합계표에 포함.
    var buyer = partyRow(null, "(주)무번호", 1, 100_000, 10_000);
    given(taxInvoiceRepository.sumSalesByTaxType(FROM, TO)).willReturn(List.of());
    given(taxInvoiceRepository.aggregateSalesByBuyer(FROM, TO)).willReturn(List.of(buyer));
    given(apInvoiceRepository.aggregatePurchasesByVendor(FROM, TO)).willReturn(List.of());

    VatReturnResponse result = service.getVatReturn(FROM, TO);

    assertThat(result.salesByBuyer()).hasSize(1);
    assertThat(result.salesByBuyer().get(0).businessNo()).isNull();
    assertThat(result.salesByBuyer().get(0).name()).isEqualTo("(주)무번호");
  }

  @Test
  void getVatReturn_fromAfterTo_throwsInvalidInput() {
    // AC-10: from > to → 400.
    ErpException ex = assertThrows(ErpException.class, () -> service.getVatReturn(TO, FROM));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
  }

  @Test
  void getVatReturn_withoutPermission_throwsForbidden() {
    // AC-9: FINANCE_READ 필요.
    doThrow(new ErpException(ErrorCode.FORBIDDEN))
        .when(permissionChecker)
        .require(Permission.FINANCE_READ);

    ErpException ex = assertThrows(ErpException.class, () -> service.getVatReturn(FROM, TO));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
  }
}
