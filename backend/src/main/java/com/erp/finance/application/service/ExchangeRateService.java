package com.erp.finance.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.security.Permission;
import com.erp.common.security.PermissionChecker;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.port.out.ExchangeRateProvider;
import com.erp.finance.domain.model.ExchangeRate;
import com.erp.finance.domain.repository.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 환율 관리 — 수동 등록(중복 거부)·목록 조회·외부 provider 온디맨드 수집. 변경은 FINANCE_SETTING_WRITE. provider는 포트 뒤에 격리되어
 * 테스트는 mock으로 네트워크 없이 검증한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

  private final ExchangeRateRepository repository;
  private final ExchangeRateProvider exchangeRateProvider;
  private final PermissionChecker permissionChecker;

  public List<ExchangeRateResponse> findAll() {
    permissionChecker.require(Permission.FINANCE_READ);
    return repository.findAllByOrderByEffectiveDateDescFromCurrencyAsc().stream()
        .map(ExchangeRateResponse::from)
        .toList();
  }

  @Transactional
  public ExchangeRateResponse register(ExchangeRateCreateRequest request) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    if (repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate(
        request.fromCurrency(), request.toCurrency(), request.effectiveDate())) {
      throw new ErpException(ErrorCode.EXCHANGE_RATE_DUPLICATE);
    }
    ExchangeRate saved =
        repository.save(
            ExchangeRate.of(
                request.fromCurrency(),
                request.toCurrency(),
                request.effectiveDate(),
                request.rate()));
    return ExchangeRateResponse.from(saved);
  }

  /**
   * 외부 provider로 환율을 온디맨드 수집해 저장한다. provider가 값을 못 주면(네트워크·미지원) 빈 Optional. 이미 같은 통화쌍·일자 환율이 있으면 수동
   * 보정을 보존하기 위해 덮어쓰지 않고 빈 Optional을 반환한다.
   */
  @Transactional
  public Optional<ExchangeRateResponse> collectFromProvider(
      String fromCurrency, String toCurrency, LocalDate date) {
    permissionChecker.require(Permission.FINANCE_SETTING_WRITE);
    if (repository.existsByFromCurrencyAndToCurrencyAndEffectiveDate(
        fromCurrency, toCurrency, date)) {
      return Optional.empty();
    }
    Optional<BigDecimal> fetched = exchangeRateProvider.fetchRate(fromCurrency, toCurrency, date);
    if (fetched.isEmpty()) {
      return Optional.empty();
    }
    ExchangeRate saved =
        repository.save(ExchangeRate.of(fromCurrency, toCurrency, date, fetched.get()));
    return Optional.of(ExchangeRateResponse.from(saved));
  }
}
