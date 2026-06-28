package com.erp.finance.application.dto;

/**
 * 테넌트 환차손익 계정 설정 — 환차이익(fxGain)·환차손(fxLoss) 계정 ID. 미설정 시 null. 화면은 계정 목록(/api/finance/accounts)에서
 * ID로 코드·명을 매핑한다.
 */
public record FxGainLossAccountResponse(Long fxGainAccountId, Long fxLossAccountId) {
  public static FxGainLossAccountResponse of(Long fxGainAccountId, Long fxLossAccountId) {
    return new FxGainLossAccountResponse(fxGainAccountId, fxLossAccountId);
  }
}
