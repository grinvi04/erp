package com.erp.hr.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.hr.application.dto.ContractCreateRequest;
import com.erp.hr.application.dto.ContractResponse;
import com.erp.hr.application.service.ContractService;
import com.erp.hr.domain.model.ContractType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ContractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private ContractService contractService;

  @Test
  void findByEmployee_returnsOkWithList() throws Exception {
    ContractResponse response =
        new ContractResponse(
            1L,
            1L,
            ContractType.REGULAR,
            LocalDate.of(2020, 3, 1),
            null,
            BigDecimal.valueOf(50000000),
            1L,
            "Engineer",
            null,
            null,
            null);
    given(contractService.findByEmployee(1L)).willReturn(List.of(response));

    mockMvc
        .perform(get("/api/hr/employees/1/contracts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].contractType").value("REGULAR"));
  }

  @Test
  void findByEmployee_employeeNotFound_returns404() throws Exception {
    given(contractService.findByEmployee(99L))
        .willThrow(new ErpException(ErrorCode.EMPLOYEE_NOT_FOUND));

    mockMvc
        .perform(get("/api/hr/employees/99/contracts"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("H001"));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    ContractResponse response =
        new ContractResponse(
            1L,
            1L,
            ContractType.REGULAR,
            LocalDate.of(2024, 1, 1),
            null,
            BigDecimal.valueOf(50000000),
            1L,
            "Engineer",
            null,
            null,
            null);
    given(contractService.create(eq(1L), any())).willReturn(response);

    ContractCreateRequest request =
        new ContractCreateRequest(
            ContractType.REGULAR,
            LocalDate.of(2024, 1, 1),
            null,
            BigDecimal.valueOf(50000000),
            1L,
            null,
            null);

    mockMvc
        .perform(
            post("/api/hr/employees/1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.contractType").value("REGULAR"));
  }
}
