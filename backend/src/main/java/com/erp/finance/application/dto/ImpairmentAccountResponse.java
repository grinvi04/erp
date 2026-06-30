package com.erp.finance.application.dto;

/** 손상차손 계정 설정 — 손상차손비·손상차손누계액·손상차손환입 계정 ID. 미설정 시 null. */
public record ImpairmentAccountResponse(
    Long impairmentLossAccountId,
    Long accumulatedImpairmentAccountId,
    Long impairmentReversalAccountId) {
  public static ImpairmentAccountResponse of(Long loss, Long accumulated, Long reversal) {
    return new ImpairmentAccountResponse(loss, accumulated, reversal);
  }
}
