package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ApInvoiceRepository extends JpaRepository<ApInvoice, Long> {
    boolean existsByInvoiceNo(String invoiceNo);
    Page<ApInvoice> findByVendorId(Long vendorId, Pageable pageable);
    Page<ApInvoice> findByStatus(ApInvoiceStatus status, Pageable pageable);
    Page<ApInvoice> findByVendorIdAndStatus(Long vendorId, ApInvoiceStatus status, Pageable pageable);

    @Query("SELECT COUNT(i) FROM ApInvoice i "
            + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
            + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
            + "AND i.totalAmount > i.paidAmount")
    long countUnpaid();

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM ApInvoice i "
            + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
            + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
            + "AND i.totalAmount > i.paidAmount")
    BigDecimal sumUnpaidAmount();
}
