package com.erp.finance.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.finance.application.dto.BaseCurrencyResponse;
import com.erp.finance.application.dto.BaseCurrencyUpdateRequest;
import com.erp.finance.application.dto.ExchangeRateCreateRequest;
import com.erp.finance.application.dto.ExchangeRateResponse;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FxController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class FxControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private BaseCurrencyService baseCurrencyService;
    @MockBean private ExchangeRateService exchangeRateService;

    @Test
    void getBaseCurrency_returnsOk() throws Exception {
        given(baseCurrencyService.getBaseCurrency()).willReturn(BaseCurrencyResponse.of("KRW"));

        mockMvc.perform(get("/api/finance/fx/base-currency"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.baseCurrency").value("KRW"));
    }

    @Test
    void updateBaseCurrency_validRequest_returnsOk() throws Exception {
        given(baseCurrencyService.updateBaseCurrency(any())).willReturn(BaseCurrencyResponse.of("USD"));

        mockMvc.perform(put("/api/finance/fx/base-currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BaseCurrencyUpdateRequest("USD"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.baseCurrency").value("USD"));
    }

    @Test
    void updateBaseCurrency_invalidCode_returns400() throws Exception {
        mockMvc.perform(put("/api/finance/fx/base-currency")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BaseCurrencyUpdateRequest("usd"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void findRates_returnsOk() throws Exception {
        given(exchangeRateService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/finance/fx/rates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createRate_validRequest_returns201() throws Exception {
        ExchangeRateCreateRequest request = new ExchangeRateCreateRequest(
            "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000"));
        given(exchangeRateService.register(any())).willReturn(
            new ExchangeRateResponse(1L, "USD", "KRW", LocalDate.of(2024, 6, 1), new BigDecimal("1300.00000000")));

        mockMvc.perform(post("/api/finance/fx/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.fromCurrency").value("USD"));
    }
}
