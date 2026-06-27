package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.domain.model.Account;
import com.erp.finance.domain.model.FiscalYear;
import com.erp.finance.domain.repository.AccountRepository;
import com.erp.finance.domain.repository.FiscalYearRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 재무제표 3서비스(시산표·손익계산서·재무상태표) 공통 헬퍼 — 회계연도 해석·계정맵·표시 반올림. 유형별 분류·소계·부호 로직은 서비스마다 다르므로 여기에 두지 않는다.
 */
final class FinancialStatementSupport {

  /** 표시용 반올림 자리수 — 기준통화 2자리. */
  private static final int DISPLAY_SCALE = 2;

  private FinancialStatementSupport() {}

  /** year가 null이면 올해를 대상으로 회계연도를 조회한다. 없으면 예외. */
  static FiscalYear resolveFiscalYear(FiscalYearRepository fiscalYearRepository, Integer year) {
    int target = year != null ? year : Year.now().getValue();
    return fiscalYearRepository
        .findByYear(target)
        .orElseThrow(() -> new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND));
  }

  /** 계정 id → Account 조회 맵. */
  static Map<Long, Account> accountsById(AccountRepository accountRepository) {
    return accountRepository.findAll().stream()
        .collect(Collectors.toMap(Account::getId, Function.identity()));
  }

  /** 표시용 반올림 — 기준통화 2자리(HALF_UP). 균형·합계 산정은 원시값으로 끝낸 뒤에만 적용한다. */
  static BigDecimal display(BigDecimal amount) {
    return amount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
  }
}
