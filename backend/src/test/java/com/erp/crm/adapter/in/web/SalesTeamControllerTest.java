package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.SalesTeamCreateRequest;
import com.erp.crm.application.dto.SalesTeamResponse;
import com.erp.crm.application.dto.SalesTeamUpdateRequest;
import com.erp.crm.application.dto.TeamMemberRequest;
import com.erp.crm.application.service.SalesTeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SalesTeamController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class SalesTeamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private SalesTeamService salesTeamService;

    private SalesTeamResponse buildResponse() {
        return new SalesTeamResponse(1L, "T-001", "영업1팀", Set.of("user-001"), 0L);
    }

    @Test
    void list_returnsOk() throws Exception {
        given(salesTeamService.listTeams()).willReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/crm/sales-teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("T-001"))
                .andExpect(jsonPath("$.data[0].name").value("영업1팀"));
    }

    @Test
    void get_existingTeam_returnsOk() throws Exception {
        given(salesTeamService.getTeam(1L)).willReturn(buildResponse());

        mockMvc.perform(get("/api/crm/sales-teams/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(salesTeamService.createTeam("T-001", "영업1팀")).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/sales-teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamCreateRequest("T-001", "영업1팀"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("T-001"));
    }

    @Test
    void create_blankCode_returns400() throws Exception {
        mockMvc.perform(post("/api/crm/sales-teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamCreateRequest("", "영업1팀"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/crm/sales-teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamCreateRequest("T-001", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validRequest_returnsOk() throws Exception {
        given(salesTeamService.updateTeam(1L, "영업1팀", 0L)).willReturn(buildResponse());

        mockMvc.perform(put("/api/crm/sales-teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamUpdateRequest("영업1팀", 0L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void update_missingVersion_returns400() throws Exception {
        mockMvc.perform(put("/api/crm/sales-teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamUpdateRequest("영업1팀", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_existingTeam_returns204() throws Exception {
        mockMvc.perform(delete("/api/crm/sales-teams/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void addMember_validRequest_returnsOk() throws Exception {
        given(salesTeamService.addMember(1L, "user-001")).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/sales-teams/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TeamMemberRequest("user-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberUserIds[0]").value("user-001"));
    }

    @Test
    void addMember_blankUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/crm/sales-teams/1/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TeamMemberRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeMember_validRequest_returnsOk() throws Exception {
        given(salesTeamService.removeMember(1L, "user-001")).willReturn(buildResponse());

        mockMvc.perform(delete("/api/crm/sales-teams/1/members/user-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        given(salesTeamService.createTeam("T-001", "영업1팀"))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/crm/sales-teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SalesTeamCreateRequest("T-001", "영업1팀"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }

    @Test
    void list_noPermission_returns403() throws Exception {
        given(salesTeamService.listTeams())
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/crm/sales-teams"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        willThrow(new ErpException(ErrorCode.FORBIDDEN))
                .given(salesTeamService).deleteTeam(1L);

        mockMvc.perform(delete("/api/crm/sales-teams/1"))
                .andExpect(status().isForbidden());
    }
}
