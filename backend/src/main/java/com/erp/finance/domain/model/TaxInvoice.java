package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.SQLRestriction;

/**
 * 전자세금계산서 — 승인된 매출(AR) 인보이스에서 발행(파생). 발행 시 공급자(회사정보)·공급받는자(거래처)·금액·품목을 스냅샷으로 고정해 이후 마스터 변경과 무관하게
 * 불변(법적 증빙). 1 AR : 1 ISSUED(중복 발행 차단). 품목은 헤더 단일(인보이스 공급가액 전액).
 */
@Entity
@Table(name = "tax_invoice", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class TaxInvoice extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tax_invoice_seq")
  @SequenceGenerator(
      name = "tax_invoice_seq",
      sequenceName = "finance.tax_invoice_id_seq",
      allocationSize = 50)
  private Long id;

  // 발행한 원천 AR 인보이스(1:1). 모듈 내 약결합 — journal_entry_id와 동일하게 ID 참조.
  @Column(name = "ar_invoice_id", nullable = false)
  private Long arInvoiceId;

  // 내부 발행번호(채번) — 발행(save) 후 id 기반으로 부여. 국세청 승인번호는 미전송이므로 별도(nullable, 미사용).
  @Column(name = "issue_no", length = 40)
  private String issueNo;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_type", nullable = false, length = 20)
  private TaxType taxType;

  @Enumerated(EnumType.STRING)
  @Column(name = "charge_type", nullable = false, length = 10)
  private ChargeType chargeType;

  @Column(name = "write_date", nullable = false)
  private LocalDate writeDate;

  @Column(name = "supply_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal supplyAmount;

  @Column(name = "vat_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal vatAmount;

  @Column(name = "total_amount", nullable = false, precision = 20, scale = 2)
  private BigDecimal totalAmount;

  // 품목명 — 헤더 단일 품목(대표품목/비고). 공급가액·세액은 헤더 전액.
  @Column(name = "item_name", nullable = false, length = 200)
  private String itemName;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "companyName", column = @Column(name = "supplier_company_name")),
    @AttributeOverride(name = "businessNo", column = @Column(name = "supplier_business_no")),
    @AttributeOverride(name = "representative", column = @Column(name = "supplier_representative")),
    @AttributeOverride(name = "address", column = @Column(name = "supplier_address")),
    @AttributeOverride(name = "businessType", column = @Column(name = "supplier_business_type")),
    @AttributeOverride(name = "businessItem", column = @Column(name = "supplier_business_item"))
  })
  private PartySnapshot supplier;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "companyName", column = @Column(name = "buyer_company_name")),
    @AttributeOverride(name = "businessNo", column = @Column(name = "buyer_business_no")),
    @AttributeOverride(name = "representative", column = @Column(name = "buyer_representative")),
    @AttributeOverride(name = "address", column = @Column(name = "buyer_address")),
    @AttributeOverride(name = "businessType", column = @Column(name = "buyer_business_type")),
    @AttributeOverride(name = "businessItem", column = @Column(name = "buyer_business_item"))
  })
  private PartySnapshot buyer;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private TaxInvoiceStatus status;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  protected TaxInvoice() {}

  /** AR 인보이스에서 세금계산서를 발행(ISSUED). 금액·세액은 AR에서 승계(재계산 없음). */
  public static TaxInvoice issue(
      Long arInvoiceId,
      TaxType taxType,
      ChargeType chargeType,
      LocalDate writeDate,
      BigDecimal supplyAmount,
      BigDecimal vatAmount,
      BigDecimal totalAmount,
      String itemName,
      PartySnapshot supplier,
      PartySnapshot buyer,
      String note) {
    TaxInvoice t = new TaxInvoice();
    t.arInvoiceId = arInvoiceId;
    t.taxType = taxType;
    t.chargeType = chargeType;
    t.writeDate = writeDate;
    t.supplyAmount = supplyAmount;
    t.vatAmount = vatAmount;
    t.totalAmount = totalAmount;
    t.itemName = itemName;
    t.supplier = supplier;
    t.buyer = buyer;
    t.note = note;
    t.status = TaxInvoiceStatus.ISSUED;
    return t;
  }

  /** 발행번호 부여(save 후 id 기반). */
  public void assignIssueNo(String issueNo) {
    this.issueNo = issueNo;
  }

  /** 취소 — ISSUED → CANCELLED. 상태 전제는 서비스가 검증한다. */
  public void cancel() {
    this.status = TaxInvoiceStatus.CANCELLED;
  }

  public boolean isIssued() {
    return status == TaxInvoiceStatus.ISSUED;
  }

  public Long getId() {
    return id;
  }

  public Long getArInvoiceId() {
    return arInvoiceId;
  }

  public String getIssueNo() {
    return issueNo;
  }

  public TaxType getTaxType() {
    return taxType;
  }

  public ChargeType getChargeType() {
    return chargeType;
  }

  public LocalDate getWriteDate() {
    return writeDate;
  }

  public BigDecimal getSupplyAmount() {
    return supplyAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getItemName() {
    return itemName;
  }

  public PartySnapshot getSupplier() {
    return supplier;
  }

  public PartySnapshot getBuyer() {
    return buyer;
  }

  public TaxInvoiceStatus getStatus() {
    return status;
  }

  public String getNote() {
    return note;
  }
}
