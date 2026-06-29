package com.erp.finance.application.dto;

import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.model.TaxType;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 세금계산서 상세 — 발행번호·종류·작성일·금액·품목·공급자/공급받는자 스냅샷·상태. */
public record TaxInvoiceResponse(
    Long id,
    Long arInvoiceId,
    String issueNo,
    TaxType taxType,
    ChargeType chargeType,
    LocalDate writeDate,
    BigDecimal supplyAmount,
    BigDecimal vatAmount,
    BigDecimal totalAmount,
    String itemName,
    TaxInvoiceStatus status,
    String note,
    Party supplier,
    Party buyer) {

  /** 거래 당사자 스냅샷 응답. */
  public record Party(
      String companyName,
      String businessNo,
      String representative,
      String address,
      String businessType,
      String businessItem) {
    static Party from(PartySnapshot s) {
      return new Party(
          s.getCompanyName(),
          s.getBusinessNo(),
          s.getRepresentative(),
          s.getAddress(),
          s.getBusinessType(),
          s.getBusinessItem());
    }
  }

  public static TaxInvoiceResponse from(TaxInvoice t) {
    return new TaxInvoiceResponse(
        t.getId(),
        t.getArInvoiceId(),
        t.getIssueNo(),
        t.getTaxType(),
        t.getChargeType(),
        t.getWriteDate(),
        t.getSupplyAmount(),
        t.getVatAmount(),
        t.getTotalAmount(),
        t.getItemName(),
        t.getStatus(),
        t.getNote(),
        Party.from(t.getSupplier()),
        Party.from(t.getBuyer()));
  }
}
