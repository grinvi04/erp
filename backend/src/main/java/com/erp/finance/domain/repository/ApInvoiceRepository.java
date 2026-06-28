package com.erp.finance.domain.repository;

import com.erp.common.response.CurrencyAmount;
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

  // 기준통화 변경 가드 — base_amount 스냅샷이 하나라도 있으면 변경 거부(현재 테넌트는 @TenantId 자동 필터).
  boolean existsByBaseAmountIsNotNull();

  // ApInvoiceResponse.from은 vendor를 역참조 — 목록 매핑 시 N+1 방지를 위해 vendor(@ManyToOne, LAZY)를
  // 페이지 쿼리에서 함께 페치한다. 카운트는 페치 없는 별도 countQuery로 페이지네이션을 유지한다.
  @Query(
      value = "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor v WHERE v.id = :vendorId",
      countQuery = "SELECT COUNT(i) FROM ApInvoice i WHERE i.vendor.id = :vendorId")
  Page<ApInvoice> findByVendorId(@Param("vendorId") Long vendorId, Pageable pageable);

  @Query(
      value = "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor WHERE i.status = :status",
      countQuery = "SELECT COUNT(i) FROM ApInvoice i WHERE i.status = :status")
  Page<ApInvoice> findByStatus(@Param("status") ApInvoiceStatus status, Pageable pageable);

  @Query(
      value =
          "SELECT i FROM ApInvoice i LEFT JOIN FETCH i.vendor v "
              + "WHERE v.id = :vendorId AND i.status = :status",
      countQuery =
          "SELECT COUNT(i) FROM ApInvoice i WHERE i.vendor.id = :vendorId AND i.status = :status")
  Page<ApInvoice> findByVendorIdAndStatus(
      @Param("vendorId") Long vendorId, @Param("status") ApInvoiceStatus status, Pageable pageable);

  /**
   * 전결규정상 현재 사용자가 결재할 수 있는 대기 전표 — 통합 결재함 라우팅용. 상태=대기, 작성자≠본인(직무분리), 금액≤본인 전결 한도. 테넌트 필터는 자동 적용.
   */
  @Query(
      "SELECT i FROM ApInvoice i WHERE i.status = :status "
          + "AND i.createdBy <> :userId AND i.totalAmount <= :limit")
  List<ApInvoice> findPendingApprovableBy(
      @Param("status") ApInvoiceStatus status,
      @Param("userId") String userId,
      @Param("limit") BigDecimal limit);

  @Query(
      "SELECT COUNT(i) FROM ApInvoice i "
          + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
          + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
          + "AND i.totalAmount > i.paidAmount")
  long countUnpaid();

  @Query(
      "SELECT new com.erp.common.response.CurrencyAmount("
          + "i.currency, COALESCE(SUM(i.totalAmount - i.paidAmount), 0)) FROM ApInvoice i "
          + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
          + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
          + "AND i.totalAmount > i.paidAmount "
          + "GROUP BY i.currency ORDER BY i.currency")
  List<CurrencyAmount> sumUnpaidAmountByCurrency();

  /**
   * 미지급 인보이스의 기준통화 환산 <b>미지급잔액</b> 합계 — 통화별 분리합과 별개로 단일 기준통화 합계 표시용. 통화별 분리({@link
   * #sumUnpaidAmountByCurrency()})가 미지급잔액(total-paid) 기준이므로, base 합계도 전표 전액 스냅샷(base_amount)이 아니라
   * 스냅샷 환율 × 미지급잔액으로 산정해 정합을 맞춘다 (부분지급 시 전액 합산되어 통화별 합을 초과하던 모순 제거). 미지급 조건은 {@link
   * #sumUnpaidAmountByCurrency()}와 동일. base_amount 미산정(null=환율 미산정) 행은 제외(부분 합계)이며, 산정된 행이 하나도 없으면
   * null을 반환한다(0이 아님 — 미산정과 0을 구분). 결과는 base 컬럼 정밀도(NUMERIC(20,2))에 맞춰 소수 2자리로 반올림.
   */
  @Query(
      "SELECT ROUND(SUM(i.exchangeRate * (i.totalAmount - i.paidAmount)), 2) FROM ApInvoice i "
          + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
          + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
          + "AND i.totalAmount > i.paidAmount "
          + "AND i.baseAmount IS NOT NULL")
  BigDecimal sumUnpaidBaseTotal();

  /**
   * 미지급인데 기준통화 환산이 안 된(base_amount NULL=환율 미산정) 인보이스 수. {@link #sumUnpaidBaseTotal()}에서 제외된 행이 있는지
   * 판정해, 환산 합계가 "일부 미환산"임을 정직하게 표기하기 위함(미지급 조건은 동일).
   */
  @Query(
      "SELECT COUNT(i) FROM ApInvoice i "
          + "WHERE i.status NOT IN (com.erp.finance.domain.model.ApInvoiceStatus.PAID, "
          + "com.erp.finance.domain.model.ApInvoiceStatus.CANCELLED) "
          + "AND i.totalAmount > i.paidAmount "
          + "AND i.baseAmount IS NULL")
  long countUnpaidUnconverted();

  @Query(
      "SELECT EXTRACT(MONTH FROM i.invoiceDate) AS month, i.currency AS currency, "
          + "COUNT(i) AS count, COALESCE(SUM(i.totalAmount), 0) AS totalAmount "
          + "FROM ApInvoice i WHERE EXTRACT(YEAR FROM i.invoiceDate) = :year "
          + "GROUP BY EXTRACT(MONTH FROM i.invoiceDate), i.currency "
          + "ORDER BY i.currency, EXTRACT(MONTH FROM i.invoiceDate)")
  List<MonthlyInvoiceRow> monthlyInvoices(@Param("year") int year);

  /** 월별 기준통화 환산액 합계 — 통화별 추이와 별개로, 모든 통화를 기준통화로 합산한 월별 시리즈. base_amount 미산정(null) 행은 SUM에서 제외된다. */
  @Query(
      "SELECT EXTRACT(MONTH FROM i.invoiceDate) AS month, "
          + "COALESCE(SUM(i.baseAmount), 0) AS baseTotal "
          + "FROM ApInvoice i WHERE EXTRACT(YEAR FROM i.invoiceDate) = :year "
          + "AND i.baseAmount IS NOT NULL "
          + "GROUP BY EXTRACT(MONTH FROM i.invoiceDate) "
          + "ORDER BY EXTRACT(MONTH FROM i.invoiceDate)")
  List<MonthlyBaseRow> monthlyBaseTotals(@Param("year") int year);
}
