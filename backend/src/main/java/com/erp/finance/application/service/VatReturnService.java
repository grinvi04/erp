package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.VatReturnResponse;
import com.erp.finance.application.dto.VatReturnResponse.PartyLine;
import com.erp.finance.application.dto.VatReturnResponse.PurchaseSummary;
import com.erp.finance.application.dto.VatReturnResponse.SalesSummary;
import com.erp.finance.domain.model.TaxType;
import com.erp.finance.domain.repository.ApInvoiceRepository;
import com.erp.finance.domain.repository.PartyAmountRow;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import com.erp.finance.domain.repository.TaxTypeAmountRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부가세 신고 기초자료 집계 — 신고기간(from~to)의 매출(발행 세금계산서)·매입(승인 매입 인보이스)을 합산해 요약·납부세액·합계표를 산출한다. 읽기 전용,
 * FINANCE_READ. 세액은 저장값 합산(재계산 없음).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VatReturnService {

  private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

  private final TaxInvoiceRepository taxInvoiceRepository;
  private final ApInvoiceRepository apInvoiceRepository;
  private final PermissionChecker permissionChecker;

  public VatReturnResponse getVatReturn(LocalDate from, LocalDate to) {
    permissionChecker.require(Permission.FINANCE_READ);
    if (from == null || to == null || from.isAfter(to)) {
      throw new ErpException(ErrorCode.INVALID_INPUT);
    }

    List<TaxTypeAmountRow> salesByType = taxInvoiceRepository.sumSalesByTaxType(from, to);
    SalesSummary sales = toSalesSummary(salesByType);

    List<PartyAmountRow> purchaseRows = apInvoiceRepository.aggregatePurchasesByVendor(from, to);
    PurchaseSummary purchases = new PurchaseSummary(sumSupply(purchaseRows), sumVat(purchaseRows));

    BigDecimal payableTax = sales.totalVat().subtract(purchases.vat());

    return new VatReturnResponse(
        from,
        to,
        sales,
        purchases,
        payableTax,
        toLines(taxInvoiceRepository.aggregateSalesByBuyer(from, to)),
        toLines(purchaseRows));
  }

  private static SalesSummary toSalesSummary(List<TaxTypeAmountRow> rows) {
    BigDecimal taxableSupply = ZERO;
    BigDecimal taxableVat = ZERO;
    BigDecimal zeroRatedSupply = ZERO;
    BigDecimal exemptSupply = ZERO;
    BigDecimal totalVat = ZERO;
    for (TaxTypeAmountRow r : rows) {
      BigDecimal supply = nz(r.getSupplyTotal());
      BigDecimal vat = nz(r.getVatTotal());
      totalVat = totalVat.add(vat);
      if (r.getTaxType() == TaxType.TAXABLE) {
        taxableSupply = taxableSupply.add(supply);
        taxableVat = taxableVat.add(vat);
      } else if (r.getTaxType() == TaxType.ZERO_RATED) {
        zeroRatedSupply = zeroRatedSupply.add(supply);
      } else if (r.getTaxType() == TaxType.EXEMPT) {
        exemptSupply = exemptSupply.add(supply);
      }
    }
    return new SalesSummary(taxableSupply, taxableVat, zeroRatedSupply, exemptSupply, totalVat);
  }

  private static List<PartyLine> toLines(List<PartyAmountRow> rows) {
    return rows.stream()
        .map(
            r ->
                new PartyLine(
                    r.getBusinessNo(),
                    r.getName(),
                    r.getCount(),
                    nz(r.getSupplyTotal()),
                    nz(r.getVatTotal())))
        .toList();
  }

  private static BigDecimal sumSupply(List<PartyAmountRow> rows) {
    return rows.stream().map(r -> nz(r.getSupplyTotal())).reduce(ZERO, BigDecimal::add);
  }

  private static BigDecimal sumVat(List<PartyAmountRow> rows) {
    return rows.stream().map(r -> nz(r.getVatTotal())).reduce(ZERO, BigDecimal::add);
  }

  private static BigDecimal nz(BigDecimal v) {
    return v != null ? v : ZERO;
  }
}
