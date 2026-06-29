package com.erp.finance.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 세금계산서 거래 당사자(공급자/공급받는자) 스냅샷 — 발행 시점에 회사정보·거래처에서 복사해 고정한다. 이후 마스터가 바뀌어도 발행본은 불변(법적 증빙). 상호·사업자번호는
 * 필수, 나머지는 선택.
 */
@Embeddable
public class PartySnapshot {

  @Column(nullable = false, length = 200)
  private String companyName;

  // 공급자는 필수(회사정보가 강제)지만 공급받는자(거래처)는 사업자번호가 없을 수 있어 nullable.
  @Column(length = 30)
  private String businessNo;

  @Column(length = 100)
  private String representative;

  @Column(length = 500)
  private String address;

  // 업태
  @Column(length = 200)
  private String businessType;

  // 종목
  @Column(length = 200)
  private String businessItem;

  protected PartySnapshot() {}

  public static PartySnapshot of(
      String companyName,
      String businessNo,
      String representative,
      String address,
      String businessType,
      String businessItem) {
    PartySnapshot s = new PartySnapshot();
    s.companyName = companyName;
    s.businessNo = businessNo;
    s.representative = representative;
    s.address = address;
    s.businessType = businessType;
    s.businessItem = businessItem;
    return s;
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
