package com.erp.common.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    given(auditService.search(any(), any(), any(), any()))
        .willReturn(new PageImpl<>(List.of(sample), PageRequest.of(0, 20), 1));

    mockMvc
        .perform(get("/api/audit/logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].action").value("APPROVE"));
  }

  @Test
  void search_withoutPermission_returnsForbidden() throws Exception {
    given(auditService.search(any(), any(), any(), any()))
        .willThrow(new ErpException(ErrorCode.FORBIDDEN));

    mockMvc.perform(get("/api/audit/logs")).andExpect(status().isForbidden());
  }
}
