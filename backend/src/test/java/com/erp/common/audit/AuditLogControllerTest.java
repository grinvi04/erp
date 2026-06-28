package com.erp.common.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditLogController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuditLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AuditService auditService;

  @Test
  void search_withPermission_returnsOk() throws Exception {
    AuditLogResponse sample =
        new AuditLogResponse(
            1L,
            "LEAVE_REQUEST",
            10L,
            AuditLog.AuditAction.APPROVE,
            "MANAGER",
            LocalDateTime.of(2026, 1, 1, 9, 0),
            null);
    given(auditService.search(any(), any(), any(), any(), any(), any(), any()))
        .willReturn(new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/audit/logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].action").value("APPROVE"));
  }

  @Test
  void search_passesActionAndDateRangeFilters() throws Exception {
    given(auditService.search(any(), any(), any(), any(), any(), any(), any()))
        .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

    mockMvc
        .perform(
            get("/api/audit/logs")
                .param("action", "APPROVE")
                .param("performedBy", "MANAGER")
                .param("from", "2026-01-01T00:00:00")
                .param("to", "2026-01-31T23:59:59"))
        .andExpect(status().isOk());

    org.mockito.Mockito.verify(auditService)
        .search(
            eq(null),
            eq(null),
            eq("MANAGER"),
            eq(AuditLog.AuditAction.APPROVE),
            eq(LocalDateTime.of(2026, 1, 1, 0, 0, 0)),
            eq(LocalDateTime.of(2026, 1, 31, 23, 59, 59)),
            any());
  }

  @Test
  void export_returnsCsvAttachment() throws Exception {
    AuditLogResponse row =
        new AuditLogResponse(
            1L,
            "AP_INVOICE",
            10L,
            AuditLog.AuditAction.APPROVE,
            "manager,with,commas",
            LocalDateTime.of(2026, 1, 1, 9, 0),
            null);
    given(auditService.export(any(), any(), any(), any(), any(), any())).willReturn(List.of(row));

    mockMvc
        .perform(get("/api/audit/logs/export"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"audit-logs.csv\""))
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        // 쉼표 포함 수행자는 따옴표로 감싸진다(RFC 4180).
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("\"manager,with,commas\"")));
  }

  @Test
  void search_withoutPermission_returnsForbidden() throws Exception {
    given(auditService.search(any(), any(), any(), any(), any(), any(), any()))
        .willThrow(new ErpException(ErrorCode.FORBIDDEN));

    mockMvc.perform(get("/api/audit/logs")).andExpect(status().isForbidden());
  }
}
