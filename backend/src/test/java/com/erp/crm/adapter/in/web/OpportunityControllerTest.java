package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.application.service.OpportunityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpportunityController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class OpportunityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private OpportunityService opportunityService;

    private OpportunityResponse buildResponse() {
        return new OpportunityResponse(1L, 1L, "테스트고객사", "2026 클라우드 전환",
                1L, "탐색", new BigDecimal("50000000"), "KRW",
                LocalDate.of(2026, 12, 31), 20, "sales-001", "REFERRAL", null,
                LocalDateTime.now(), LocalDateTime.now(), 0L);
    }

    @Test
    void findById_existingOpportunity_returnsOk() throws Exception {
        given(opportunityService.findById(1L)).willReturn(buildResponse());

        mockMvc.perform(get("/api/crm/opportunities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("2026 클라우드 전환"))
                .andExpect(jsonPath("$.data.probability").value(20));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(opportunityService.create(any())).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OpportunityCreateRequest(
                                1L, "2026 클라우드 전환", 1L,
                                new BigDecimal("50000000"), "KRW",
                                LocalDate.of(2026, 12, 31),
                                20, "REFERRAL", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("2026 클라우드 전환"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(opportunityService.findById(99L))
                .willThrow(new ErpException(ErrorCode.OPPORTUNITY_NOT_FOUND));

        mockMvc.perform(get("/api/crm/opportunities/99"))
                .andExpect(status().isNotFound());
    }
}
