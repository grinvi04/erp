package com.erp.finance.application.dto;

/**
 * 테넌트 부가세 통제계정 설정 — 부가세대급금(매입, vatReceivable)·부가세예수금(매출, vatPayable) 계정 ID. 미설정 시 null. 화면은 계정
 * 목록(/api/finance/accounts)에서 ID로 코드·명을 매핑한다.
 */
public record VatAccountResponse(Long vatReceivableAccountId, Long vatPayableAccountId) {
  public static VatAccountResponse of(Long vatReceivableAccountId, Long vatPayableAccountId) {
    return new VatAccountResponse(vatReceivableAccountId, vatPayableAccountId);
  }
}
