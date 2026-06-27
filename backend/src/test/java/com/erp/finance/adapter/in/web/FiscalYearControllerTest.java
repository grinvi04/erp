package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.FiscalYearCreateRequest;
import com.erp.finance.application.dto.FiscalYearResponse;
import com.erp.finance.application.service.FiscalYearService;
import com.erp.finance.domain.model.FiscalYearStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(FiscalYearController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class FiscalYearControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private FiscalYearService fiscalYearService;

  @Test
  void findAll_returnsOk() throws Exception {
    given(fiscalYearService.findAll()).willReturn(List.of());

    mockMvc
        .perform(get("/api/finance/fiscal-years"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    FiscalYearCreateRequest request =
        new FiscalYearCreateRequest(2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
    FiscalYearResponse response =
        new FiscalYearResponse(
            1L, 2025, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), FiscalYearStatus.OPEN);
    given(fiscalYearService.create(any())).willReturn(response);

    mockMvc
        .perform(
            post("/api/finance/fiscal-years")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.year").value(2025));
  }

  @Test
  void close_notFound_returns404() throws Exception {
    willThrow(new ErpException(ErrorCode.FISCAL_YEAR_NOT_FOUND))
        .given(fiscalYearService)
        .close(99L);

    mockMvc
        .perform(post("/api/finance/fiscal-years/99/close"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false));
  }
}
