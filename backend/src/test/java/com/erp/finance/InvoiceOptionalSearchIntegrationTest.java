package com.erp.finance;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.AbstractIntegrationTest;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** 선택 검색조건의 null 바인딩이 PostgreSQL에서 유효하고 빈 테넌트에서도 빈 페이지를 반환하는지 검증한다. */
@Transactional
class InvoiceOptionalSearchIntegrationTest extends AbstractIntegrationTest {

  @Autowired private ApInvoiceRepository apInvoiceRepository;
  @Autowired private ArInvoiceRepository arInvoiceRepository;

  @Test
  void apSearch_withOnlyDateRange_returnsEmptyPage() {
    var result =
        apInvoiceRepository.search(
            null,
            null,
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 14),
            PageRequest.of(0, 20));

    assertThat(result).isEmpty();
  }

  @Test
  void arSearch_withOnlyDateRange_returnsEmptyPage() {
    var result =
        arInvoiceRepository.search(
            null,
            null,
            LocalDate.of(2026, 7, 14),
            LocalDate.of(2026, 7, 14),
            PageRequest.of(0, 20));

    assertThat(result).isEmpty();
  }
}
