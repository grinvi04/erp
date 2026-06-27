package com.erp.finance.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 외부 환율 소스 포트 — 애플리케이션은 이 포트에만 의존하고 실제 HTTP 구현은 adapter/out에 격리한다. 테스트는 이 포트를 mock해 네트워크 의존 없이 수집
 * 로직을 검증한다(CI에서 실제 외부 호출 금지). 조회 실패(네트워크·미지원 통화 등)는 예외 대신 빈 Optional로 graceful 처리한다.
 */
public interface ExchangeRateProvider {

  Optional<BigDecimal> fetchRate(String fromCurrency, String toCurrency, LocalDate date);
}
