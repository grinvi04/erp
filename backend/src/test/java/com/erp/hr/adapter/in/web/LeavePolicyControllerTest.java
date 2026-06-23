package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.LeavePolicyCreateRequest;
import com.erp.hr.application.dto.LeavePolicyResponse;
import com.erp.hr.application.service.LeavePolicyService;
import com.erp.hr.domain.model.LeavePolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeavePolicyController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LeavePolicyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LeavePolicyService leavePolicyService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        LeavePolicyResponse response = new LeavePolicyResponse(
            1L, "ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);
        given(leavePolicyService.findAll()).willReturn(List.of(response));

        mockMvc.perform(get("/api/hr/leave-policies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].code").value("ANNUAL"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        LeavePolicyResponse response = new LeavePolicyResponse(
            1L, "SICK", "병가", LeavePolicy.LeaveType.SICK, 10, 0, false, 0);
        given(leavePolicyService.create(any())).willReturn(response);

        LeavePolicyCreateRequest request = new LeavePolicyCreateRequest(
            "SICK", "병가", LeavePolicy.LeaveType.SICK, 10, 0, false, 0);

        mockMvc.perform(post("/api/hr/leave-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").value("SICK"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(leavePolicyService.create(any())).willThrow(new ErpException(ErrorCode.DUPLICATE_CODE));

        LeavePolicyCreateRequest request = new LeavePolicyCreateRequest(
            "ANNUAL", "연차", LeavePolicy.LeaveType.ANNUAL, 15, 5, true, 1);

        mockMvc.perform(post("/api/hr/leave-policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H011"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/hr/leave-policies/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        willThrow(new ErpException(ErrorCode.RESOURCE_NOT_FOUND))
            .given(leavePolicyService).delete(99L);

        mockMvc.perform(delete("/api/hr/leave-policies/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("C002"));
    }
}
