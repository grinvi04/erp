package com.erp.crm.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.common.response.PageResponse;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.dto.LeadUpdateRequest;
import com.erp.crm.application.service.LeadService;
import com.erp.crm.domain.model.LeadStatus;
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

@WebMvcTest(LeadController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class LeadControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockBean private LeadService leadService;

  private LeadResponse buildResponse() {
    return new LeadResponse(
        1L,
        "홍",
        "길동",
        "테스트회사",
        "대리",
        "hong@test.com",
        "010-0000-0000",
        "WEB",
        LeadStatus.NEW,
        "user-001",
        null,
        null,
        null,
        null,
        "메모",
        LocalDateTime.now(),
        0L);
  }

  private LeadCreateRequest validCreate() {
    return new LeadCreateRequest(
        "홍", "길동", "테스트회사", "대리", "hong@test.com", "010-0000-0000", "WEB", "메모");
  }

  @Test
  void search_returnsOk() throws Exception {
    PageResponse<LeadResponse> page =
        new PageResponse<>(List.of(buildResponse()), 0, 20, 1, 1, true, true);
    given(leadService.search(eq(LeadStatus.NEW), eq("홍"), any())).willReturn(page);

    mockMvc
        .perform(get("/api/crm/leads").param("status", "NEW").param("keyword", "홍"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].lastName").value("홍"))
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void findById_existingLead_returnsOk() throws Exception {
    given(leadService.findById(1L)).willReturn(buildResponse());

    mockMvc
        .perform(get("/api/crm/leads/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.company").value("테스트회사"))
        .andExpect(jsonPath("$.data.status").value("NEW"));
  }

  @Test
  void findById_notFound_returns404() throws Exception {
    given(leadService.findById(99L)).willThrow(new ErpException(ErrorCode.LEAD_NOT_FOUND));

    mockMvc
        .perform(get("/api/crm/leads/99"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("CR005"));
  }

  @Test
  void create_validRequest_returns201() throws Exception {
    given(leadService.create(any())).willReturn(buildResponse());

    mockMvc
        .perform(
            post("/api/crm/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreate())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.lastName").value("홍"));
  }

  @Test
  void create_blankLastName_returns400() throws Exception {
    LeadCreateRequest invalid =
        new LeadCreateRequest("", "길동", "테스트회사", null, null, null, null, null);

    mockMvc
        .perform(
            post("/api/crm/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalid)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void update_validRequest_returnsOk() throws Exception {
    given(leadService.update(eq(1L), any())).willReturn(buildResponse());
    LeadUpdateRequest req =
        new LeadUpdateRequest("홍", "길동", "테스트회사", null, null, null, "WEB", "user-001", "메모", 0L);

    mockMvc
        .perform(
            put("/api/crm/leads/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(1));
  }

  @Test
  void update_missingVersion_returns400() throws Exception {
    LeadUpdateRequest req =
        new LeadUpdateRequest("홍", "길동", "테스트회사", null, null, null, "WEB", "user-001", "메모", null);

    mockMvc
        .perform(
            put("/api/crm/leads/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void convert_validRequest_returnsOk() throws Exception {
    LeadResponse converted =
        new LeadResponse(
            1L,
            "홍",
            "길동",
            "테스트회사",
            "대리",
            "hong@test.com",
            "010-0000-0000",
            "WEB",
            LeadStatus.CONVERTED,
            "user-001",
            10L,
            15L,
            20L,
            LocalDateTime.now(),
            "메모",
            LocalDateTime.now(),
            1L);
    given(leadService.convert(eq(1L), any())).willReturn(converted);

    mockMvc
        .perform(
            post("/api/crm/leads/1/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LeadConvertRequest(10L, false, null, null, null, null, null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CONVERTED"))
        .andExpect(jsonPath("$.data.convertedAccountId").value(10))
        .andExpect(jsonPath("$.data.convertedContactId").value(15));
  }

  @Test
  void create_noPermission_returns403() throws Exception {
    given(leadService.create(any())).willThrow(new ErpException(ErrorCode.FORBIDDEN));

    mockMvc
        .perform(
            post("/api/crm/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validCreate())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("C005"));
  }

  @Test
  void findById_noPermission_returns403() throws Exception {
    given(leadService.findById(1L)).willThrow(new ErpException(ErrorCode.FORBIDDEN));

    mockMvc.perform(get("/api/crm/leads/1")).andExpect(status().isForbidden());
  }
}
