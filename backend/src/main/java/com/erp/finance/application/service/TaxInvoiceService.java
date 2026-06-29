package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.TaxInvoiceIssueRequest;
import com.erp.finance.application.dto.TaxInvoiceResponse;
import com.erp.finance.domain.model.ArInvoice;
import com.erp.finance.domain.model.ArInvoiceStatus;
import com.erp.finance.domain.model.ChargeType;
import com.erp.finance.domain.model.CompanyProfile;
import com.erp.finance.domain.model.Customer;
import com.erp.finance.domain.model.PartySnapshot;
import com.erp.finance.domain.model.TaxInvoice;
import com.erp.finance.domain.model.TaxInvoiceStatus;
import com.erp.finance.domain.repository.ArInvoiceRepository;
import com.erp.finance.domain.repository.TaxInvoiceRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전자세금계산서 발행·취소·조회 — 승인/완납된 AR 인보이스에서 공급자(회사정보)·공급받는자(거래처)·금액·품목을 스냅샷 고정해 발행한다. 1 AR : 1 ISSUED.
 * 금액·세액은 AR에서 승계(재계산 없음). 발행본은 마스터 변경과 무관하게 불변.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxInvoiceService {

  private static final String DEFAULT_ITEM_NAME = "상품";

  private final TaxInvoiceRepository taxInvoiceRepository;
  private final ArInvoiceRepository arInvoiceRepository;
  private final CompanyProfileService companyProfileService;
  private final NtsTaxInvoiceXmlGenerator xmlGenerator;
  private final PermissionChecker permissionChecker;

  /** 세금계산서 목록 — 상태 필터(null이면 전체), 페이징. */
  public PageResponse<TaxInvoiceResponse> findAll(TaxInvoiceStatus status, Pageable pageable) {
    permissionChecker.require(Permission.FINANCE_READ);
    var page =
        status != null
            ? taxInvoiceRepository.findByStatus(status, pageable)
            : taxInvoiceRepository.findAll(pageable);
    return PageResponse.from(page.map(TaxInvoiceResponse::from));
  }

  public TaxInvoiceResponse findById(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    return TaxInvoiceResponse.from(getOrThrow(id));
  }

  /** 발행본의 국세청 표준 XML 생성 — ISSUED만 가능(취소본 거부). */
  public String generateXml(Long id) {
    permissionChecker.require(Permission.FINANCE_READ);
    TaxInvoice taxInvoice = getOrThrow(id);
    if (!taxInvoice.isIssued()) {
      throw new ErpException(ErrorCode.TAX_INVOICE_XML_REQUIRES_ISSUED);
    }
    return xmlGenerator.generate(taxInvoice);
  }

  /** AR 인보이스에서 세금계산서 발행 — 공급자/공급받는자/금액/품목 스냅샷 고정, ISSUED. */
  @Transactional
  public TaxInvoiceResponse issue(Long arInvoiceId, TaxInvoiceIssueRequest request) {
    permissionChecker.require(Permission.FINANCE_WRITE);

    ArInvoice ar =
        arInvoiceRepository
            .findById(arInvoiceId)
            .orElseThrow(() -> new ErpException(ErrorCode.INVOICE_NOT_FOUND));
    if (ar.getStatus() != ArInvoiceStatus.APPROVED && ar.getStatus() != ArInvoiceStatus.PAID) {
      throw new ErpException(ErrorCode.AR_INVOICE_NOT_ISSUABLE);
    }
    if (taxInvoiceRepository.existsByArInvoiceIdAndStatus(arInvoiceId, TaxInvoiceStatus.ISSUED)) {
      throw new ErpException(ErrorCode.TAX_INVOICE_ALREADY_ISSUED);
    }

    CompanyProfile profile =
        companyProfileService
            .currentCompanyProfile()
            .orElseThrow(() -> new ErpException(ErrorCode.COMPANY_PROFILE_REQUIRED));

    PartySnapshot supplier =
        PartySnapshot.of(
            profile.getCompanyName(),
            profile.getBusinessNo(),
            profile.getRepresentative(),
            profile.getAddress(),
            profile.getBusinessType(),
            profile.getBusinessItem());
    Customer customer = ar.getCustomer();
    PartySnapshot buyer =
        PartySnapshot.of(
            customer.getName(),
            customer.getBusinessNo(),
            customer.getRepresentativeName(),
            customer.getAddress(),
            customer.getBusinessType(),
            customer.getBusinessItem());

    LocalDate writeDate = request.writeDate() != null ? request.writeDate() : ar.getInvoiceDate();
    ChargeType chargeType = request.chargeType() != null ? request.chargeType() : ChargeType.CHARGE;
    String itemName =
        (request.itemName() == null || request.itemName().isBlank())
            ? DEFAULT_ITEM_NAME
            : request.itemName();

    TaxInvoice taxInvoice =
        taxInvoiceRepository.save(
            TaxInvoice.issue(
                arInvoiceId,
                ar.getTaxType(),
                chargeType,
                writeDate,
                ar.getSupplyAmount(),
                ar.getVatAmount(),
                ar.getTotalAmount(),
                itemName,
                supplier,
                buyer,
                request.note()));
    taxInvoice.assignIssueNo(String.format("TI-%08d", taxInvoice.getId()));
    return TaxInvoiceResponse.from(taxInvoice);
  }

  /** 세금계산서 취소 — ISSUED만 가능. */
  @Transactional
  public TaxInvoiceResponse cancel(Long id) {
    permissionChecker.require(Permission.FINANCE_WRITE);
    TaxInvoice taxInvoice = getOrThrow(id);
    if (!taxInvoice.isIssued()) {
      throw new ErpException(ErrorCode.TAX_INVOICE_NOT_CANCELLABLE);
    }
    taxInvoice.cancel();
    return TaxInvoiceResponse.from(taxInvoice);
  }

  private TaxInvoice getOrThrow(Long id) {
    return taxInvoiceRepository
        .findById(id)
        .orElseThrow(() -> new ErpException(ErrorCode.TAX_INVOICE_NOT_FOUND));
  }
}
