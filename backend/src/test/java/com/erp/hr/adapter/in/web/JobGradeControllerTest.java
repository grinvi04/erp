package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.JobGradeCreateRequest;
import com.erp.hr.application.dto.JobGradeResponse;
import com.erp.hr.application.service.JobGradeService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobGradeController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class JobGradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JobGradeService jobGradeService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        given(jobGradeService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/hr/job-grades"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        JobGradeCreateRequest request = new JobGradeCreateRequest("G3", "3급", 3,
            new BigDecimal("3000000"), new BigDecimal("5000000"));
        JobGradeResponse response = new JobGradeResponse(1L, "G3", "3급", 3,
            new BigDecimal("3000000"), new BigDecimal("5000000"), 0L);
        given(jobGradeService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/hr/job-grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").value("G3"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        JobGradeCreateRequest request = new JobGradeCreateRequest("G3", "3급", 3, null, null);
        given(jobGradeService.create(any())).willThrow(new ErpException(ErrorCode.DUPLICATE_CODE));

        mockMvc.perform(post("/api/hr/job-grades")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H011"));
    }

    @Test
    void delete_gradeInUse_returns409() throws Exception {
        willThrow(new ErpException(ErrorCode.JOB_GRADE_IN_USE))
            .given(jobGradeService).delete(1L);

        mockMvc.perform(delete("/api/hr/job-grades/{id}", 1L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H014"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/hr/job-grades/{id}", 1L))
            .andExpect(status().isNoContent());
    }
}
