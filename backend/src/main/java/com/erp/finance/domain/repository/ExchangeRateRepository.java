package com.erp.finance.domain.repository;

import com.erp.finance.domain.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    /** 통화쌍의 발효일 ≤ 조회일 중 최신 환율 1건(환산에 사용). */
    Optional<ExchangeRate> findTopByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
        String fromCurrency, String toCurrency, LocalDate effectiveDate);

    /** 수동 등록 중복 검사(테넌트·통화쌍·일자 UNIQUE). */
    boolean existsByFromCurrencyAndToCurrencyAndEffectiveDate(
        String fromCurrency, String toCurrency, LocalDate effectiveDate);

    List<ExchangeRate> findAllByOrderByEffectiveDateDescFromCurrencyAsc();
}
