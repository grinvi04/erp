package com.erp.common.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.security.dto.TenantUserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TenantUserOnboardingController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TenantUserOnboardingControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private TenantUserOnboardingService service;

  @Test
  void invite_validRequest_returnsCreated() throws Exception {
    given(service.invite(any()))
        .willReturn(
            new TenantUserResponse(
                3L, "admin@example.com", "user-1", TenantUserStatus.ACTIVE, null));

    mockMvc
        .perform(
            post("/api/iam/tenant-users/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"admin@example.com\",\"firstName\":\"ERP\","
                        + "\"lastName\":\"Admin\",\"requestKey\":\"request-1\","
                        + "\"roleIds\":[7]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.userId").value("user-1"));
  }

  @Test
  void invite_invalidEmailAndMissingRequestKey_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/iam/tenant-users/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"not-an-email\",\"roleIds\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void invite_nonPositiveRoleId_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/iam/tenant-users/invitations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"admin@example.com\",\"requestKey\":\"request-1\","
                        + "\"roleIds\":[0]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void disable_returnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/iam/tenant-users/3")).andExpect(status().isNoContent());
  }
}
