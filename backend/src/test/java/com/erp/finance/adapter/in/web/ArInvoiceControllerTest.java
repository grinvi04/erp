package com.erp.finance.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.service.ArInvoiceService;
import com.erp.finance.domain.model.ArInvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ArInvoiceController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ArInvoiceControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private ArInvoiceService arInvoiceService;

  @Test
  void findAll_exportFilters_passesAllFiltersToService() throws Exception {
    given(arInvoiceService.findAll(any(), any(), any(), any(), any()))
        .willReturn(PageResponse.from(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0)));

    mockMvc
        .perform(
            get("/api/finance/ar-invoices")
                .param("status", "DRAFT")
                .param("customerId", "42")
                .param("from", "2026-01-01")
                .param("to", "2026-01-31")
                .param("size", "100"))
        .andExpect(status().isOk());

    verify(arInvoiceService)
        .findAll(
            ArInvoiceStatus.DRAFT,
            42L,
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 31),
            PageRequest.of(0, 100, Sort.by("id").descending()));
  }
}
