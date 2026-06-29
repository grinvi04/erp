package com.erp.finance.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 부가세 신고 기초자료 — 신고기간 매출(과세/영세율/면세)·매입 요약, 납부(환급)세액, 매출처별·매입처별 합계표. 금액은 저장값 합산(재계산 없음). */
public record VatReturnResponse(
    LocalDate from,
    LocalDate to,
    SalesSummary sales,
    PurchaseSummary purchases,
    BigDecimal payableTax,
    List<PartyLine> salesByBuyer,
    List<PartyLine> purchasesByVendor) {

  /** 매출 요약 — 과세 공급가액·세액, 영세율 공급가액, 면세 공급가액, 매출세액 합계(=과세 세액). */
  public record SalesSummary(
      BigDecimal taxableSupply,
      BigDecimal taxableVat,
      BigDecimal zeroRatedSupply,
      BigDecimal exemptSupply,
      BigDecimal totalVat) {}

  /** 매입 요약 — 공급가액 합계, 매입세액 합계. */
  public record PurchaseSummary(BigDecimal supply, BigDecimal vat) {}

  /** 합계표 한 행 — 거래처 사업자번호(없으면 null)·명·매수·공급가액 합·세액 합. */
  public record PartyLine(
      String businessNo, String name, long count, BigDecimal supplyTotal, BigDecimal vatTotal) {}
}
