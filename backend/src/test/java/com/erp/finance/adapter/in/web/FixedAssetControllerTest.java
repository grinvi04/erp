package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.DepreciationAccountResponse;
import com.erp.finance.application.dto.DepreciationAccountUpdateRequest;
import com.erp.finance.application.dto.DepreciationRunRequest;
import com.erp.finance.application.dto.DepreciationRunResponse;
import com.erp.finance.application.dto.FixedAssetCreateRequest;
import com.erp.finance.application.dto.FixedAssetDisposeRequest;
import com.erp.finance.application.dto.FixedAssetResponse;
import com.erp.finance.application.dto.ImpairmentAccountResponse;
import com.erp.finance.application.dto.ImpairmentAccountUpdateRequest;
import com.erp.finance.application.dto.ImpairmentRecognizeRequest;
import com.erp.finance.application.dto.ImpairmentRecognizeResponse;
import com.erp.finance.application.service.BaseCurrencyService;
import com.erp.finance.application.service.DepreciationPostingService;
import com.erp.finance.application.service.FixedAssetService;
import com.erp.finance.application.service.ImpairmentPostingService;
import com.erp.finance.domain.model.DepreciationMethod;
import com.erp.finance.domain.model.FixedAssetStatus;
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

@WebMvcTest(FixedAssetController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class FixedAssetControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private FixedAssetService fixedAssetService;
  @MockBean private DepreciationPostingService depreciationPostingService;
  @MockBean private ImpairmentPostingService impairmentPostingService;
  @MockBean private BaseCurrencyService baseCurrencyService;

  private static FixedAssetResponse asset() {
    return new FixedAssetResponse(
        1L,
        "FA1",
        "노트북",
        LocalDate.of(2025, 1, 1),
        new BigDecimal("1200000"),
        BigDecimal.ZERO,
        12,
        DepreciationMethod.STRAIGHT_LINE,
        null,
        7L,
        BigDecimal.ZERO,
        new BigDecimal("1200000"),
        FixedAssetStatus.ACTIVE);
  }

  private static FixedAssetCreateRequest createRequest() {
    return new FixedAssetCreateRequest(
        "FA1",
        "노트북",
        LocalDate.of(2025, 1, 1),
        new BigDecimal("1200000"),
        BigDecimal.ZERO,
        12,
        DepreciationMethod.STRAIGHT_LINE,
        null,
        7L);
  }

  @Test
  void findAll_returnsOk() throws Exception {
    given(fixedAssetService.findAll(any()))
        .willReturn(
            PageResponse.from(new org.springframework.data.domain.PageImpl<>(List.of(asset()))));

    mockMvc
        .perform(get("/api/finance/fixed-assets"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].code").value("FA1"));
  }

  @Test
  void findById_returnsOk() throws Exception {
    given(fixedAssetService.findById(1L)).willReturn(asset());

    mockMvc
        .perform(get("/api/finance/fixed-assets/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.bookValue").value(1200000));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    given(fixedAssetService.create(any())).willReturn(asset());

    mockMvc
        .perform(
            post("/api/finance/fixed-assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.code").value("FA1"));
  }

  @Test
  void create_blankCode_returns400() throws Exception {
    FixedAssetCreateRequest invalid =
        new FixedAssetCreateRequest(
            "",
            "노트북",
            LocalDate.of(2025, 1, 1),
            new BigDecimal("1200000"),
            BigDecimal.ZERO,
            12,
            DepreciationMethod.STRAIGHT_LINE,
            null,
            7L);

    mockMvc
        .perform(
            post("/api/finance/fixed-assets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dispose_returnsOk() throws Exception {
    FixedAssetResponse disposed =
        new FixedAssetResponse(
            1L,
            "FA1",
            "노트북",
            LocalDate.of(2025, 1, 1),
            new BigDecimal("1200000"),
            BigDecimal.ZERO,
            12,
            DepreciationMethod.STRAIGHT_LINE,
            null,
            7L,
            new BigDecimal("100000"),
            new BigDecimal("1100000"),
            FixedAssetStatus.DISPOSED);
    given(fixedAssetService.dispose(eq(1L), any())).willReturn(disposed);

    mockMvc
        .perform(
            post("/api/finance/fixed-assets/1/dispose")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new FixedAssetDisposeRequest(
                            LocalDate.of(2025, 1, 20), new BigDecimal("1150000"), 9L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DISPOSED"));
  }

  @Test
  void runDepreciation_returnsOk() throws Exception {
    given(depreciationPostingService.runForPeriod(5L))
        .willReturn(new DepreciationRunResponse(5L, 3, 1, new BigDecimal("300000")));

    mockMvc
        .perform(
            post("/api/finance/fixed-assets/depreciation-run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new DepreciationRunRequest(5L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.processedCount").value(3));
  }

  @Test
  void getDepreciationAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.getDepreciationAccounts())
        .willReturn(DepreciationAccountResponse.of(11L, 12L, 13L, 14L));

    mockMvc
        .perform(get("/api/finance/fixed-assets/depreciation-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.depreciationExpenseAccountId").value(11));
  }

  @Test
  void updateDepreciationAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.updateDepreciationAccounts(any()))
        .willReturn(DepreciationAccountResponse.of(11L, 12L, 13L, 14L));

    mockMvc
        .perform(
            put("/api/finance/fixed-assets/depreciation-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new DepreciationAccountUpdateRequest(11L, 12L, 13L, 14L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accumulatedDepreciationAccountId").value(12));
  }

  @Test
  void recognizeImpairment_returnsOk() throws Exception {
    given(impairmentPostingService.recognizeImpairment(eq(1L), eq(5L), any()))
        .willReturn(
            new ImpairmentRecognizeResponse(
                1L,
                5L,
                new BigDecimal("1100000"),
                new BigDecimal("800000"),
                new BigDecimal("300000"),
                new BigDecimal("800000"),
                42L));

    mockMvc
        .perform(
            post("/api/finance/fixed-assets/1/impairment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ImpairmentRecognizeRequest(5L, new BigDecimal("800000")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.impairmentLoss").value(300000))
        .andExpect(jsonPath("$.data.bookValueAfter").value(800000));
  }

  @Test
  void recognizeImpairment_negativeRecoverable_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/finance/fixed-assets/1/impairment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new ImpairmentRecognizeRequest(5L, new BigDecimal("-1")))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getImpairmentAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.getImpairmentAccounts())
        .willReturn(ImpairmentAccountResponse.of(21L, 22L));

    mockMvc
        .perform(get("/api/finance/fixed-assets/impairment-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.impairmentLossAccountId").value(21));
  }

  @Test
  void updateImpairmentAccounts_returnsOk() throws Exception {
    given(baseCurrencyService.updateImpairmentAccounts(any()))
        .willReturn(ImpairmentAccountResponse.of(21L, 22L));

    mockMvc
        .perform(
            put("/api/finance/fixed-assets/impairment-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new ImpairmentAccountUpdateRequest(21L, 22L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.accumulatedImpairmentAccountId").value(22));
  }
}
