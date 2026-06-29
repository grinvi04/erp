package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.model.Vendor;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.VendorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기준통화 변경 가드(PR) 통합 검증 — base_amount 스냅샷이 존재하는 finance 거래(ap_invoice)가 있으면 기준통화 변경을 거부(409
 * BASE_CURRENCY_CHANGE_NOT_ALLOWED)하고 기존 설정이 불변임을, 스냅샷이 없으면 변경이 허용됨을 실제 DB·@TenantId 필터로 확인한다. 동일 값
 * PUT(no-op)도 허용을 확인.
 */
@Transactional
class FxBaseCurrencyGuardIntegrationTest extends AbstractIntegrationTest {

  @Autowired private BaseCurrencyService baseCurrencyService;
  @Autowired private ApInvoiceRepository apInvoiceRepository;
  @Autowired private VendorRepository vendorRepository;

  private void seedPricedApInvoice() {
    Vendor vendor =
        vendorRepository.save(
            Vendor.of(
                "V-GUARD", "가드공급사", "000-00-33333", "홍길동", "g@test.com", "010-0000-0000", 30));
    ApInvoice inv =
        ApInvoice.create(
            "AP-GUARD",
            vendor,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31),
            new BigDecimal("100"),
            TaxType.EXEMPT,
            "USD",
            null);
    inv.applyBaseSnapshot(new BigDecimal("130000"), new BigDecimal("1300"));
    apInvoiceRepository.save(inv);
  }

  @Test
  void updateBaseCurrency_withSnapshot_rejected_andSettingUnchanged() {
    authenticate("admin", "finance:read", "finance:setting:write");
    seedPricedApInvoice();

    ErpException ex =
        assertThrows(
            ErpException.class,
            () -> baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BASE_CURRENCY_CHANGE_NOT_ALLOWED);
    // 기존 설정 불변 — 미설정이었으므로 여전히 KRW 기본.
    assertThat(baseCurrencyService.getBaseCurrency().baseCurrency()).isEqualTo("KRW");
  }

  @Test
  void updateBaseCurrency_noSnapshot_allowed() {
    authenticate("admin", "finance:read", "finance:setting:write");

    baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));

    assertThat(baseCurrencyService.getBaseCurrency().baseCurrency()).isEqualTo("USD");
  }

  @Test
  void updateBaseCurrency_sameValue_noOpAllowedEvenWithSnapshot() {
    authenticate("admin", "finance:read", "finance:setting:write");
    // 기준통화를 USD로 먼저 설정한 뒤(스냅샷 없을 때) 스냅샷을 만든다.
    baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));
    seedPricedApInvoice();

    // 동일 값(USD) PUT은 스냅샷이 있어도 통화가 바뀌지 않으므로 허용(no-op).
    baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));

    assertThat(baseCurrencyService.getBaseCurrency().baseCurrency()).isEqualTo("USD");
  }
}
