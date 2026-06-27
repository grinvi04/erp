package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.ApInvoiceCreateRequest;
import com.erp.finance.application.dto.ApInvoiceResponse;
import com.erp.finance.application.service.ApInvoiceService;
import com.erp.finance.domain.model.ApInvoiceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApInvoiceController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ApInvoiceControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private ApInvoiceService apInvoiceService;

  private ApInvoiceResponse sampleResponse() {
    return new ApInvoiceResponse(
        1L,
        "INV-001",
        1L,
        "공급사",
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 1, 31),
        new BigDecimal("100000"),
        BigDecimal.ZERO,
        new BigDecimal("100000"),
        "KRW",
        ApInvoiceStatus.DRAFT,
        null,
        null,
        null);
  }

  @Test
  void findAll_noStatusParam_returnsOk() throws Exception {
    given(apInvoiceService.findAll(any(), any()))
        .willReturn(
            PageResponse.from(new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1)));

    mockMvc
        .perform(get("/api/finance/invoices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    given(apInvoiceService.create(any())).willReturn(sampleResponse());

    ApInvoiceCreateRequest request =
        new ApInvoiceCreateRequest(
            "INV-001",
            1L,
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 31),
            new BigDecimal("100000"),
            "KRW",
            null,
            null);

    mockMvc
        .perform(
            post("/api/finance/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.invoiceNo").value("INV-001"));
  }

  @Test
  void pay_invalidAmount_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/finance/invoices/1/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":0}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cancel_alreadyProcessed_returns409() throws Exception {
    given(apInvoiceService.cancel(1L))
        .willThrow(new ErpException(ErrorCode.INVOICE_ALREADY_PROCESSED));

    mockMvc.perform(post("/api/finance/invoices/1/cancel")).andExpect(status().isConflict());
  }
}
