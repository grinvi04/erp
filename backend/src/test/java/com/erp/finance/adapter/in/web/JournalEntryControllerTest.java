package com.erp.finance.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.finance.application.dto.JournalEntryCreateRequest;
import com.erp.finance.application.dto.JournalEntryResponse;
import com.erp.finance.application.dto.JournalLineRequest;
import com.erp.finance.application.service.JournalEntryService;
import com.erp.finance.domain.model.JournalEntryStatus;
import com.erp.finance.domain.model.JournalEntryType;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JournalEntryController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class JournalEntryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private JournalEntryService journalEntryService;

    private JournalEntryResponse sampleResponse() {
        return new JournalEntryResponse(1L, "JE-20250101-00001",
            LocalDate.of(2025, 1, 1), 1L, 1, "테스트전표",
            JournalEntryType.MANUAL, JournalEntryStatus.DRAFT,
            new BigDecimal("1000"), new BigDecimal("1000"), "KRW",
            null, null, null, null);
    }

    @Test
    void findByFiscalPeriod_returnsOkWithPage() throws Exception {
        given(journalEntryService.findByFiscalPeriod(anyLong(), any()))
            .willReturn(PageResponse.from(
                new PageImpl<>(List.of(sampleResponse()), PageRequest.of(0, 20), 1)));

        mockMvc.perform(get("/api/finance/journal-entries?fiscalPeriodId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(journalEntryService.create(any())).willReturn(sampleResponse());

        JournalEntryCreateRequest request = new JournalEntryCreateRequest(
            LocalDate.of(2025, 1, 1), 1L, "테스트전표", JournalEntryType.MANUAL, "KRW",
            List.of(
                new JournalLineRequest(1L, new BigDecimal("1000"), BigDecimal.ZERO, null, null),
                new JournalLineRequest(2L, BigDecimal.ZERO, new BigDecimal("1000"), null, null)));

        mockMvc.perform(post("/api/finance/journal-entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.entryNo").value("JE-20250101-00001"));
    }

    @Test
    void submit_notFound_returns404() throws Exception {
        given(journalEntryService.submitForApproval(99L))
            .willThrow(new ErpException(ErrorCode.JOURNAL_ENTRY_NOT_FOUND));

        mockMvc.perform(post("/api/finance/journal-entries/99/submit"))
            .andExpect(status().isNotFound());
    }

    @Test
    void approve_returnsOk() throws Exception {
        given(journalEntryService.approve(1L)).willReturn(sampleResponse());

        mockMvc.perform(post("/api/finance/journal-entries/1/approve"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
