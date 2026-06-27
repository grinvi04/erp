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

  // 기준통화 변경 가드 — base_amount 스냅샷이 하나라도 있으면 변경 거부(현재 테넌트는 @TenantId 자동 필터).
  boolean existsByBaseAmountIsNotNull();

  // ArInvoiceResponse.from은 customer를 역참조 — 목록 매핑 시 N+1 방지를 위해 customer(@ManyToOne, LAZY)를
  // 페이지 쿼리에서 함께 페치한다. 카운트는 페치 없는 별도 countQuery로 페이지네이션을 유지한다.
  @Query(
      value = "SELECT i FROM ArInvoice i LEFT JOIN FETCH i.customer c WHERE c.id = :customerId",
      countQuery = "SELECT COUNT(i) FROM ArInvoice i WHERE i.customer.id = :customerId")
  Page<ArInvoice> findByCustomerId(@Param("customerId") Long customerId, Pageable pageable);

  @Query(
      value = "SELECT i FROM ArInvoice i LEFT JOIN FETCH i.customer WHERE i.status = :status",
      countQuery = "SELECT COUNT(i) FROM ArInvoice i WHERE i.status = :status")
  Page<ArInvoice> findByStatus(@Param("status") ArInvoiceStatus status, Pageable pageable);

  @Query(
      value =
          "SELECT i FROM ArInvoice i LEFT JOIN FETCH i.customer c "
              + "WHERE c.id = :customerId AND i.status = :status",
      countQuery =
          "SELECT COUNT(i) FROM ArInvoice i WHERE i.customer.id = :customerId AND i.status = :status")
  Page<ArInvoice> findByCustomerIdAndStatus(
      @Param("customerId") Long customerId,
      @Param("status") ArInvoiceStatus status,
      Pageable pageable);

  /**
   * 전결규정상 현재 사용자가 결재할 수 있는 대기 AR 전표 — 통합 결재함 라우팅용. 상태=대기, 작성자≠본인(직무분리), 금액≤본인 전결 한도. 테넌트 필터는 자동 적용.
   */
  @Query(
      "SELECT i FROM ArInvoice i WHERE i.status = :status "
          + "AND i.createdBy <> :userId AND i.totalAmount <= :limit")
  List<ArInvoice> findPendingApprovableBy(
      @Param("status") ArInvoiceStatus status,
      @Param("userId") String userId,
      @Param("limit") BigDecimal limit);
}
