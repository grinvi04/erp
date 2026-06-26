package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.DepartmentCreateRequest;
import com.erp.hr.application.dto.DepartmentResponse;
import com.erp.hr.application.service.DepartmentService;
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

@WebMvcTest(DepartmentController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        given(departmentService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/api/hr/departments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        DepartmentCreateRequest request = new DepartmentCreateRequest("DEV", "개발팀", null, 0);
        DepartmentResponse response = new DepartmentResponse(1L, "DEV", "개발팀", null, 0, 0, null, true, 0L);
        given(departmentService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/hr/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").value("DEV"))
            .andExpect(jsonPath("$.data.name").value("개발팀"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        DepartmentCreateRequest request = new DepartmentCreateRequest("DEV", "개발팀", null, 0);
        given(departmentService.create(any())).willThrow(new ErpException(ErrorCode.DUPLICATE_CODE));

        mockMvc.perform(post("/api/hr/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("H011"));
    }

    @Test
    void delete_withChildren_returns409() throws Exception {
        willThrow(new ErpException(ErrorCode.DEPARTMENT_HAS_CHILDREN))
            .given(departmentService).delete(1L);

        mockMvc.perform(delete("/api/hr/departments/{id}", 1L))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H005"));
    }

    @Test
    void delete_existing_returns204() throws Exception {
        mockMvc.perform(delete("/api/hr/departments/{id}", 1L))
            .andExpect(status().isNoContent());
    }
}
