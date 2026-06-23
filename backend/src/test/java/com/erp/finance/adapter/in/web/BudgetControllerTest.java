package com.erp.finance.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.finance.application.dto.BudgetCreateRequest;
import com.erp.finance.application.dto.BudgetResponse;
import com.erp.finance.application.service.BudgetService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class BudgetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private BudgetService budgetService;

    @Test
    void findByFiscalYear_returnsOkWithList() throws Exception {
        BudgetResponse response = new BudgetResponse(1L, 1L, 2025, 1L, "5100", "인건비",
            null, new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"), false);
        given(budgetService.findByFiscalYear(anyLong())).willReturn(List.of(response));

        mockMvc.perform(get("/api/finance/budgets?fiscalYearId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].accountCode").value("5100"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        BudgetResponse response = new BudgetResponse(1L, 1L, 2025, 1L, "5100", "인건비",
            null, new BigDecimal("5000000"), BigDecimal.ZERO, new BigDecimal("5000000"), false);
        given(budgetService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/finance/budgets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new BudgetCreateRequest(1L, 1L, null, new BigDecimal("5000000")))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.budgetAmount").value(5000000));
    }
}
