package com.erp.hr.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.EmployeeCreateRequest;
import com.erp.hr.application.dto.EmployeeResponse;
import com.erp.hr.application.service.EmployeeService;
import com.erp.hr.domain.model.EmployeeStatus;
import com.erp.hr.domain.model.EmploymentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class EmployeeControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private EmployeeService employeeService;

    private EmployeeResponse sampleResponse() {
        return new EmployeeResponse(1L, "EMP001", "김", "개발", "김 개발",
            LocalDate.of(1990, 1, 1), null, null, null,
            1L, "개발팀", 1L, "Engineer", null, null,
            LocalDate.of(2020, 3, 1), null,
            EmploymentType.REGULAR, EmployeeStatus.ACTIVE,
            BigDecimal.valueOf(50000000), "dev@test.com", null, null, 0L);
    }

    @Test
    void findAll_returnsOkWithPage() throws Exception {
        given(employeeService.findAll(any(), any(), any(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/hr/employees"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].employeeNo").value("EMP001"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(employeeService.findById(99L)).willThrow(new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));

        mockMvc.perform(get("/api/hr/employees/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("H001"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(employeeService.create(any())).willReturn(sampleResponse());

        EmployeeCreateRequest request = new EmployeeCreateRequest(
            "EMP001", "김", "개발", LocalDate.of(1990, 1, 1), null,
            null, null, null, 1L, 1L, null, LocalDate.of(2020, 3, 1),
            EmploymentType.REGULAR, "dev@test.com", null, null);

        mockMvc.perform(post("/api/hr/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.employeeNo").value("EMP001"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(employeeService.create(any())).willThrow(new ErpException(ErrorCode.DUPLICATE_CODE));

        EmployeeCreateRequest request = new EmployeeCreateRequest(
            "EMP001", "김", "개발", null, null,
            null, null, null, 1L, 1L, null, LocalDate.of(2020, 3, 1),
            EmploymentType.REGULAR, "dev@test.com", null, null);

        mockMvc.perform(post("/api/hr/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H011"));
    }

    @Test
    void onLeave_statusConflict_returns409() throws Exception {
        given(employeeService.onLeave(anyLong()))
            .willThrow(new ErpException(ErrorCode.EMPLOYEE_STATUS_CONFLICT));

        mockMvc.perform(post("/api/hr/employees/1/on-leave"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H015"));
    }

    @Test
    void terminate_alreadyTerminated_returns409() throws Exception {
        given(employeeService.terminate(anyLong(), any()))
            .willThrow(new ErpException(ErrorCode.EMPLOYEE_ALREADY_TERMINATED));

        mockMvc.perform(post("/api/hr/employees/1/terminate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"terminationDate\":\"2024-03-01\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error.code").value("H002"));
    }
}
