package com.erp.finance.domain.model;

import com.erp.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * 테넌트 회사정보 — 전자세금계산서의 <b>공급자</b> 신원(상호·사업자등록번호·대표자·주소·업태·종목). 테넌트당 1행(tenant_id 자동 필터). 미설정 테넌트는
 * 서비스가 빈 응답을 반환하며, 세금계산서 발행은 이 정보가 채워져야 가능하다(발행 시점 스냅샷의 공급자 원천).
 */
@Entity
@Table(name = "company_profile", schema = "finance")
@SQLRestriction("deleted_at IS NULL")
public class CompanyProfile extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "company_profile_seq")
  @SequenceGenerator(
      name = "company_profile_seq",
      sequenceName = "finance.company_profile_id_seq",
      allocationSize = 10)
  private Long id;

  @Column(name = "company_name", nullable = false, length = 200)
  private String companyName;

  @Column(name = "business_no", nullable = false, length = 30)
  private String businessNo;

  @Column(name = "representative", length = 100)
  private String representative;

  @Column(name = "address", length = 500)
  private String address;

  // 업태
  @Column(name = "business_type", length = 200)
  private String businessType;

  // 종목
  @Column(name = "business_item", length = 200)
  private String businessItem;

  protected CompanyProfile() {}

  public static CompanyProfile of(
      String companyName,
      String businessNo,
      String representative,
      String address,
      String businessType,
      String businessItem) {
    CompanyProfile entity = new CompanyProfile();
    entity.companyName = companyName;
    entity.businessNo = businessNo;
    entity.representative = representative;
    entity.address = address;
    entity.businessType = businessType;
    entity.businessItem = businessItem;
    return entity;
  }

  /** 회사정보 전체 교체(설정 upsert). */
  public void update(
      String companyName,
      String businessNo,
      String representative,
      String address,
      String businessType,
      String businessItem) {
    this.companyName = companyName;
    this.businessNo = businessNo;
    this.representative = representative;
    this.address = address;
    this.businessType = businessType;
    this.businessItem = businessItem;
  }

  public Long getId() {
    return id;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getBusinessNo() {
    return businessNo;
  }

  public String getRepresentative() {
    return representative;
  }

  public String getAddress() {
    return address;
  }

  public String getBusinessType() {
    return businessType;
  }

  public String getBusinessItem() {
    return businessItem;
  }
}
