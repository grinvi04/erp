package com.erp.finance.adapter.out;

import com.erp.finance.application.port.out.ExchangeRateProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

/**
 * frankfurter.app(ECB 기반, 키리스) 환율 조회 어댑터 — API 키·시크릿 없음.
 * 응답 예: GET /{date}?from=USD&to=KRW → {"amount":1.0,"base":"USD","date":"2024-01-02","rates":{"KRW":1300.5}}
 * 실패 시(네트워크·미지원 통화·파싱 오류) 빈 Optional 반환 → 호출 측이 수동 환율로 graceful fallback.
 */
@Slf4j
@Component
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    private final RestClient restClient;

    public FrankfurterExchangeRateProvider(
        @Value("${erp.fx.frankfurter-base-url:https://api.frankfurter.app}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Optional<BigDecimal> fetchRate(String fromCurrency, String toCurrency, LocalDate date) {
        try {
            FrankfurterResponse response = restClient.get()
                .uri("/{date}?from={from}&to={to}",
                    date.format(DateTimeFormatter.ISO_LOCAL_DATE), fromCurrency, toCurrency)
                .retrieve()
                .body(FrankfurterResponse.class);
            if (response == null || response.rates() == null) {
                return Optional.empty();
            }
            Object value = response.rates().get(toCurrency);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(new BigDecimal(value.toString()));
        } catch (Exception e) {
            log.warn("외부 환율 조회 실패 {}->{} {}: {}", fromCurrency, toCurrency, date, e.getMessage());
            return Optional.empty();
        }
    }

    record FrankfurterResponse(BigDecimal amount, String base, String date, Map<String, Object> rates) {}
}
