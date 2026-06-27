package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.ActivityCreateRequest;
import com.erp.crm.application.dto.ActivityResponse;
import com.erp.crm.application.service.ActivityService;
import com.erp.crm.domain.model.ActivityStatus;
import com.erp.crm.domain.model.ActivityType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ActivityController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ActivityControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ActivityService activityService;

    private ActivityResponse buildResponse(ActivityStatus status) {
        return new ActivityResponse(1L, ActivityType.CALL, "신규 고객 전화",
                1L, "테스트고객사", 2L, "홍 길동", 3L, "2026 클라우드 전환",
                "user-001", LocalDateTime.of(2026, 7, 1, 10, 0), null, status,
                "통화 메모", LocalDateTime.now());
    }

    private ActivityCreateRequest validCreate() {
        return new ActivityCreateRequest(ActivityType.CALL, "신규 고객 전화",
                1L, 2L, 3L, LocalDateTime.of(2026, 7, 1, 10, 0), "통화 메모");
    }

    @Test
    void search_returnsOk() throws Exception {
        PageResponse<ActivityResponse> page = new PageResponse<>(
                List.of(buildResponse(ActivityStatus.OPEN)), 0, 20, 1, 1, true, true);
        given(activityService.search(any(), any(), any(), any(), any())).willReturn(page);

        mockMvc.perform(get("/api/crm/activities")
                        .param("activityType", "CALL")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].subject").value("신규 고객 전화"))
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
    }

    @Test
    void findById_existingActivity_returnsOk() throws Exception {
        given(activityService.findById(1L)).willReturn(buildResponse(ActivityStatus.OPEN));

        mockMvc.perform(get("/api/crm/activities/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activityType").value("CALL"))
                .andExpect(jsonPath("$.data.opportunityName").value("2026 클라우드 전환"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(activityService.findById(99L))
                .willThrow(new ErpException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(get("/api/crm/activities/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CR006"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(activityService.create(any())).willReturn(buildResponse(ActivityStatus.OPEN));

        mockMvc.perform(post("/api/crm/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subject").value("신규 고객 전화"));
    }

    @Test
    void create_missingActivityType_returns400() throws Exception {
        ActivityCreateRequest invalid = new ActivityCreateRequest(null, "신규 고객 전화",
                1L, 2L, 3L, null, null);

        mockMvc.perform(post("/api/crm/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankSubject_returns400() throws Exception {
        ActivityCreateRequest invalid = new ActivityCreateRequest(ActivityType.CALL, "",
                1L, 2L, 3L, null, null);

        mockMvc.perform(post("/api/crm/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void complete_returnsOk() throws Exception {
        given(activityService.complete(1L)).willReturn(buildResponse(ActivityStatus.COMPLETED));

        mockMvc.perform(post("/api/crm/activities/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void cancel_returnsOk() throws Exception {
        given(activityService.cancel(1L)).willReturn(buildResponse(ActivityStatus.CANCELLED));

        mockMvc.perform(post("/api/crm/activities/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void complete_notFound_returns404() throws Exception {
        given(activityService.complete(99L))
                .willThrow(new ErpException(ErrorCode.ACTIVITY_NOT_FOUND));

        mockMvc.perform(post("/api/crm/activities/99/complete"))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_existingActivity_returns204() throws Exception {
        mockMvc.perform(delete("/api/crm/activities/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        given(activityService.create(any()))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/crm/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }

    @Test
    void complete_noPermission_returns403() throws Exception {
        given(activityService.complete(1L))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/crm/activities/1/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        willThrow(new ErpException(ErrorCode.FORBIDDEN))
                .given(activityService).delete(1L);

        mockMvc.perform(delete("/api/crm/activities/1"))
                .andExpect(status().isForbidden());
    }
}
