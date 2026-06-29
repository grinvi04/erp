package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {

  // 중복 발행 차단 — 동일 AR에 ISSUED 세금계산서가 이미 있으면 재발행 금지(CANCELLED는 무관).
  boolean existsByArInvoiceIdAndStatus(Long arInvoiceId, TaxInvoiceStatus status);

  Page<TaxInvoice> findByStatus(TaxInvoiceStatus status, Pageable pageable);
}
