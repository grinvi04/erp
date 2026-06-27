package com.erp.finance;

import com.erp.common.AbstractIntegrationTest;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.port.out.ExchangeRateProvider;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.CurrencyConverter;
import com.erp.finance.application.service.ExchangeRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * FX 기반(PR1) 통합 검증 — 기준통화 GET/PUT·환율 등록(중복 거부)·권한403(FINANCE_SETTING_WRITE 부재)·
 * 환산(최신 rate≤date·기준통화 그대로·부재 시 CURRENCY_RATE_NOT_FOUND)을 실제 DB·SecurityContext로 확인한다.
 * 권한 검사(authority)·@TenantId·repo 쿼리는 Mockito로는 검증 안 되므로 전체 컨텍스트로 보강한다.
 * 외부 provider는 @MockBean으로 격리(CI 네트워크 의존 금지).
 */
@Transactional
class FxFoundationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private BaseCurrencyService baseCurrencyService;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private CurrencyConverter currencyConverter;
    @MockBean private ExchangeRateProvider exchangeRateProvider;

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String sub, String... authorities) {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
            .subject(sub).claim("sub", sub).claim("tenant_id", TEST_TENANT_ID).build();
        List<GrantedAuthority> auths = Arrays.stream(authorities)
            .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a)).toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, auths));
    }

    @Test
    void getBaseCurrency_noSetting_returnsKrwDefault() {
        // AC-1: 미설정 테넌트는 KRW 기본.
        authenticate("admin", "finance:read");

        BaseCurrencyResponse result = baseCurrencyService.getBaseCurrency();

        assertThat(result.baseCurrency()).isEqualTo("KRW");
    }

    @Test
    void updateBaseCurrency_thenGet_reflectsChange() {
        // AC-1: 변경 후 조회가 반영(테넌트당 1행).
        authenticate("admin", "finance:read", "finance:setting:write");

        baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD"));
        baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("EUR"));

        assertThat(baseCurrencyService.getBaseCurrency().baseCurrency()).isEqualTo("EUR");
    }

    @Test
    void updateBaseCurrency_withoutSettingWrite_throwsForbidden() {
        // AC-7: FINANCE_SETTING_WRITE 없으면 403(finance:read만으론 변경 불가).
        authenticate("viewer", "finance:read");

        ErpException ex = assertThrows(ErpException.class, () ->
            baseCurrencyService.updateBaseCurrency(new BaseCurrencyUpdateRequest("USD")));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void registerRate_duplicate_throwsExchangeRateDuplicate() {
        // AC-2: 통화쌍·일자 UNIQUE — 중복 거부.
        authenticate("admin", "finance:read", "finance:setting:write");
        ExchangeRateCreateRequest request = new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000"));
        exchangeRateService.register(request);

        ErpException ex = assertThrows(ErpException.class, () -> exchangeRateService.register(request));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EXCHANGE_RATE_DUPLICATE);
    }

    @Test
    void registerRate_withoutSettingWrite_throwsForbidden() {
        // AC-7: 환율 등록도 FINANCE_SETTING_WRITE 필요.
        authenticate("viewer", "finance:read");

        ErpException ex = assertThrows(ErpException.class, () ->
            exchangeRateService.register(new ExchangeRateCreateRequest(
                "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300"))));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void convert_usesLatestRateOnOrBeforeDate() {
        // AC-3,6: effectiveDate ≤ 조회일 중 최신 rate로 환산(기준통화 KRW 기본).
        authenticate("admin", "finance:read", "finance:setting:write");
        exchangeRateService.register(new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 1, 1), new BigDecimal("1300.00000000")));
        exchangeRateService.register(new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 6, 10), new BigDecimal("1350.00000000")));

        assertThat(currencyConverter.convert(new BigDecimal("10"), "USD", LocalDate.of(2024, 6, 15)))
            .isEqualByComparingTo("13500.00");
        assertThat(currencyConverter.convert(new BigDecimal("10"), "USD", LocalDate.of(2024, 3, 1)))
            .isEqualByComparingTo("13000.00");
    }

    @Test
    void convert_baseCurrency_returnsAmountUnchanged() {
        // AC-6: 기준통화(KRW)는 환율 없이 amount 그대로.
        authenticate("admin", "finance:read");

        assertThat(currencyConverter.convert(new BigDecimal("1000000"), "KRW", LocalDate.of(2024, 6, 1)))
            .isEqualByComparingTo("1000000");
    }

    @Test
    void convert_noRate_throwsCurrencyRateNotFound() {
        // AC-5: 환율 없는 통화는 명시적 실패(0·1 금지).
        authenticate("admin", "finance:read");

        ErpException ex = assertThrows(ErpException.class, () ->
            currencyConverter.convert(new BigDecimal("5000"), "JPY", LocalDate.of(2024, 6, 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CURRENCY_RATE_NOT_FOUND);
    }
}
