package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ApInvoice;
import com.erp.finance.domain.model.ApInvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApInvoiceRepository extends JpaRepository<ApInvoice, Long> {
    boolean existsByInvoiceNo(String invoiceNo);

    // ApInvoiceResponse.from은 vendor를 역참조 — 목록 매핑 시 N+1 방지를 위해 vendor(@ManyToOne, LAZY)를
    // 페이지 쿼리에서 함께 페치한다. 카운트는 페치 없는 별도 countQuery로 페이지네이션을 유지한다.
    @Query(value = "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor v WHERE v.id = :vendorId",
        countQuery = "SELECT COUNT(i) FROM ApInvoice i WHERE i.vendor.id = :vendorId")
    Page<ApInvoice> findByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

    @Query(value = "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor WHERE i.status = :status",
        countQuery = "SELECT COUNT(i) FROM ApInvoice i WHERE i.status = :status")
    Page<ApInvoice> findByStatus(@Param("status") ApInvoiceStatus status, Pageable pageable);

    @Query(value = "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor v "
            + "WHERE v.id = :vendorId AND i.status = :status",
        countQuery = "SELECT COUNT(i) FROM ApInvoice i WHERE i.vendor.id = :vendorId AND i.status = :status")
    Page<ApInvoice> findByVendorIdAndStatus(@Param("vendorId") Long vendorId,
                                            @Param("status") ApInvoiceStatus status, Pageable pageable);

    /**
     * 전결규정상 현재 사용자가 결재할 수 있는 대기 전표 — 통합 결재함 라우팅용.
     * 상태=대기, 작성자≠본인(직무분리), 금액≤본인 전결 한도. 테넌트 필터는 자동 적용.
     */
    @Query("SELECT i FROM ApInvoice i WHERE i.status = :status "
            + "AND i.createdBy <> :userId AND i.totalAmount <= :limit")
    List<ApInvoice> findPendingApprovableBy(@Param("status") ApInvoiceStatus status,
                                            @Param("userId") String userId,
                                            @Param("limit") BigDecimal limit);

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

    @Query("SELECT EXTRACT(MONTH FROM i.invoiceDate) AS month, COUNT(i) AS count, "
            + "COALESCE(SUM(i.totalAmount), 0) AS totalAmount "
            + "FROM ApInvoice i WHERE EXTRACT(YEAR FROM i.invoiceDate) = :year "
            + "GROUP BY EXTRACT(MONTH FROM i.invoiceDate) ORDER BY EXTRACT(MONTH FROM i.invoiceDate)")
    List<MonthlyInvoiceRow> monthlyInvoices(@Param("year") int year);
}
