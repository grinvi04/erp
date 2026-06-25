package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ArInvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArInvoiceRepository extends JpaRepository<ArInvoice, Long> {
    boolean existsByInvoiceNo(String invoiceNo);
    Page<ArInvoice> findByCustomerId(Long customerId, Pageable pageable);
    Page<ArInvoice> findByStatus(ArInvoiceStatus status, Pageable pageable);
    Page<ArInvoice> findByCustomerIdAndStatus(Long customerId, ArInvoiceStatus status, Pageable pageable);

    /**
     * 전결규정상 현재 사용자가 결재할 수 있는 대기 AR 전표 — 통합 결재함 라우팅용.
     * 상태=대기, 작성자≠본인(직무분리), 금액≤본인 전결 한도. 테넌트 필터는 자동 적용.
     */
    @Query("SELECT i FROM ArInvoice i WHERE i.status = :status "
            + "AND i.createdBy <> :userId AND i.totalAmount <= :limit")
    List<ArInvoice> findPendingApprovableBy(@Param("status") ArInvoiceStatus status,
                                            @Param("userId") String userId,
                                            @Param("limit") BigDecimal limit);
}
