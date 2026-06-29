package com.erp.finance.domain.model;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;

/**
 * 한국 사업자등록번호(10자리) 형식·체크섬 검증. 거래처(Customer·Vendor) 생성·수정 시 적용한다.
 *
 * <p>선택 필드 — 빈값/null은 통과한다. 값이 있으면 하이픈 등 비숫자를 제거한 뒤 10자리·체크섬을 검증한다.
 *
 * <p>체크섬: 가중치 {1,3,7,1,3,7,1,3,5}로 앞 9자리 가중합 + 9번째 자리×5의 십의 자리 보정 → (10 - 합%10)%10 이 10번째 자리와 일치해야
 * 유효.
 */
public final class BusinessNoValidator {

  private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};
  private static final int LENGTH = 10;
  private static final int MOD = 10;
  private static final int CARRY_FACTOR = 5;

  private BusinessNoValidator() {}

  /** 사업자등록번호를 검증한다. 빈값은 허용. 형식·체크섬이 틀리면 {@link ErpException}(BUSINESS_NO_INVALID). */
  public static void validate(String businessNo) {
    if (businessNo == null || businessNo.isBlank()) {
      return;
    }
    String digits = businessNo.replaceAll("\\D", "");
    if (digits.length() != LENGTH) {
      throw new ErpException(ErrorCode.BUSINESS_NO_INVALID);
    }
    int[] d = digits.chars().map(c -> c - '0').toArray();
    int sum = 0;
    for (int i = 0; i < WEIGHTS.length; i++) {
      sum += d[i] * WEIGHTS[i];
    }
    sum += (d[WEIGHTS.length - 1] * CARRY_FACTOR) / MOD;
    int check = (MOD - (sum % MOD)) % MOD;
    if (check != d[LENGTH - 1]) {
      throw new ErpException(ErrorCode.BUSINESS_NO_INVALID);
    }
  }
}
