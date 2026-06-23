package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.workflow.ApprovalStatus;
import com.erp.hr.application.dto.LeaveRequestCreateRequest;
import com.erp.hr.application.dto.LeaveRequestResponse;
import com.erp.hr.application.service.LeaveRequestService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveRequestController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LeaveRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LeaveRequestService leaveRequestService;

    private LeaveRequestResponse sampleResponse() {
        return new LeaveRequestResponse(
            1L, 1L, "김 개발", 1L, "연차",
            LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5), ApprovalStatus.PENDING, null, 10L);
    }

    @Test
    void findByEmployee_returnsOkWithList() throws Exception {
        given(leaveRequestService.findByEmployee(1L)).willReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/hr/leave-requests").param("employeeId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].leavePolicyName").value("연차"));
    }

    @Test
    void create_insufficientBalance_returns409() throws Exception {
        given(leaveRequestService.create(any()))
            .willThrow(new ErpException(ErrorCode.LEAVE_BALANCE_INSUFFICIENT));

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
            1L, 1L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5), null);

        mockMvc.perform(post("/api/hr/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H008"));
    }

    @Test
    void create_overlap_returns409() throws Exception {
        given(leaveRequestService.create(any()))
            .willThrow(new ErpException(ErrorCode.LEAVE_OVERLAP));

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
            1L, 1L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5), null);

        mockMvc.perform(post("/api/hr/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H009"));
    }

    @Test
    void create_valid_returns201() throws Exception {
        given(leaveRequestService.create(any())).willReturn(sampleResponse());

        LeaveRequestCreateRequest request = new LeaveRequestCreateRequest(
            1L, 1L, LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 5),
            BigDecimal.valueOf(5), null);

        mockMvc.perform(post("/api/hr/leave-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.approvalStatus").value("PENDING"));
    }
}
