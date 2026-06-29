package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {

  // 중복 발행 차단 — 동일 AR에 ISSUED 세금계산서가 이미 있으면 재발행 금지(CANCELLED는 무관).
  boolean existsByArInvoiceIdAndStatus(Long arInvoiceId, TaxInvoiceStatus status);

  Page<TaxInvoice> findByStatus(TaxInvoiceStatus status, Pageable pageable);

  /** 매출 과세구분별 합계 — 발행(ISSUED) 세금계산서 중 작성일이 기간 내(경계 포함). 부가세 신고 요약. */
  @Query(
      "SELECT t.taxType AS taxType, "
          + "COALESCE(SUM(t.supplyAmount), 0) AS supplyTotal, "
          + "COALESCE(SUM(t.vatAmount), 0) AS vatTotal "
          + "FROM TaxInvoice t "
          + "WHERE t.writeDate BETWEEN :from AND :to "
          + "AND t.status = com.erp.finance.domain.model.TaxInvoiceStatus.ISSUED "
          + "GROUP BY t.taxType")
  List<TaxTypeAmountRow> sumSalesByTaxType(
      @Param("from") LocalDate from, @Param("to") LocalDate to);

  /** 매출처별 합계표 — 공급받는자 사업자번호 단위. 사업자번호 null은 단일 그룹으로 모인다(누락 방지). */
  @Query(
      "SELECT t.buyer.businessNo AS businessNo, t.buyer.companyName AS name, "
          + "COUNT(t) AS count, "
          + "COALESCE(SUM(t.supplyAmount), 0) AS supplyTotal, "
          + "COALESCE(SUM(t.vatAmount), 0) AS vatTotal "
          + "FROM TaxInvoice t "
          + "WHERE t.writeDate BETWEEN :from AND :to "
          + "AND t.status = com.erp.finance.domain.model.TaxInvoiceStatus.ISSUED "
          + "GROUP BY t.buyer.businessNo, t.buyer.companyName "
          + "ORDER BY t.buyer.companyName")
  List<PartyAmountRow> aggregateSalesByBuyer(
      @Param("from") LocalDate from, @Param("to") LocalDate to);
}
