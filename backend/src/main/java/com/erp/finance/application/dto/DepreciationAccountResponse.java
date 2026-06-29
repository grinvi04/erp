package com.erp.finance.application.dto;

/** 감가상각·처분 계정 설정 — 감가상각비·감가상각누계액·처분이익·처분손실 계정 ID. 미설정 시 null. */
public record DepreciationAccountResponse(
    Long depreciationExpenseAccountId,
    Long accumulatedDepreciationAccountId,
    Long disposalGainAccountId,
    Long disposalLossAccountId) {
  public static DepreciationAccountResponse of(
      Long expense, Long accumulated, Long gain, Long loss) {
    return new DepreciationAccountResponse(expense, accumulated, gain, loss);
  }
}
