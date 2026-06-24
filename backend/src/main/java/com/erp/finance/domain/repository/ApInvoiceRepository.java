package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApInvoiceRepository extends JpaRepository<ApInvoice, Long> {
    boolean existsByInvoiceNo(String invoiceNo);
    Page<ApInvoice> findByVendorId(Long vendorId, Pageable pageable);
    Page<ApInvoice> findByStatus(ApInvoiceStatus status, Pageable pageable);
    Page<ApInvoice> findByVendorIdAndStatus(Long vendorId, ApInvoiceStatus status, Pageable pageable);
}
