package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.dto.FxGainLossAccountResponse;
import com.erp.finance.application.dto.FxGainLossAccountUpdateRequest;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FxController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class FxControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private BaseCurrencyService baseCurrencyService;
  @MockBean private ExchangeRateService exchangeRateService;

  @Test
  void getOverview_returnsAggregateOk() throws Exception {
    // 바 경로 GET /api/finance/fx — 기준통화·환율·환차계정을 한 번에 200으로 반환(이전엔 매핑 부재로 500).
    given(baseCurrencyService.getBaseCurrency()).willReturn(BaseCurrencyResponse.of("KRW"));
    given(exchangeRateService.findAll())
        .willReturn(
            List.of(
                new ExchangeRateResponse(
                    1L, "USD", "KRW", LocalDate.of(2026, 6, 24), new BigDecimal("1500"))));
    given(baseCurrencyService.getFxGainLossAccounts())
        .willReturn(FxGainLossAccountResponse.of(3L, 4L));

    mockMvc
        .perform(get("/api/finance/fx"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.baseCurrency.baseCurrency").value("KRW"))
        .andExpect(jsonPath("$.data.rates[0].fromCurrency").value("USD"))
        .andExpect(jsonPath("$.data.gainLossAccounts.fxGainAccountId").value(3));
  }

  @Test
  void getOverview_noData_returnsDefaultsOk() throws Exception {
    // 데이터가 없어도 200: 기준통화 기본(KRW)·빈 환율·환차계정 null.
    given(baseCurrencyService.getBaseCurrency()).willReturn(BaseCurrencyResponse.of("KRW"));
    given(exchangeRateService.findAll()).willReturn(List.of());
    given(baseCurrencyService.getFxGainLossAccounts())
        .willReturn(FxGainLossAccountResponse.of(null, null));

    mockMvc
        .perform(get("/api/finance/fx"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.baseCurrency.baseCurrency").value("KRW"))
        .andExpect(jsonPath("$.data.rates").isEmpty());
  }

  @Test
  void getBaseCurrency_returnsOk() throws Exception {
    given(baseCurrencyService.getBaseCurrency()).willReturn(BaseCurrencyResponse.of("KRW"));

    mockMvc
        .perform(get("/api/finance/fx/base-currency"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.baseCurrency").value("KRW"));
  }

  @Test
  void updateBaseCurrency_validRequest_returnsOk() throws Exception {
    given(baseCurrencyService.updateBaseCurrency(any())).willReturn(BaseCurrencyResponse.of("USD"));

    mockMvc
        .perform(
            put("/api/finance/fx/base-currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BaseCurrencyUpdateRequest("USD"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.baseCurrency").value("USD"));
  }

  @Test
  void updateBaseCurrency_invalidCode_returns400() throws Exception {
    mockMvc
        .perform(
            put("/api/finance/fx/base-currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BaseCurrencyUpdateRequest("usd"))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getFxGainLossAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.getFxGainLossAccounts())
        .willReturn(FxGainLossAccountResponse.of(10L, 20L));

    mockMvc
        .perform(get("/api/finance/fx/gain-loss-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fxGainAccountId").value(10))
        .andExpect(jsonPath("$.data.fxLossAccountId").value(20));
  }

  @Test
  void updateFxGainLossAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.updateFxGainLossAccounts(any()))
        .willReturn(FxGainLossAccountResponse.of(10L, 20L));

    mockMvc
        .perform(
            put("/api/finance/fx/gain-loss-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new FxGainLossAccountUpdateRequest(10L, 20L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fxGainAccountId").value(10));
  }

  @Test
  void findRates_returnsOk() throws Exception {
    given(exchangeRateService.findAll()).willReturn(List.of());

    mockMvc
        .perform(get("/api/finance/fx/rates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void createRate_validRequest_returns201() throws Exception {
    ExchangeRateCreateRequest request =
        new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000"));
    given(exchangeRateService.register(any()))
        .willReturn(
            new ExchangeRateResponse(
                1L, "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000")));

    mockMvc
        .perform(
            post("/api/finance/fx/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.fromCurrency").value("USD"));
  }
}
