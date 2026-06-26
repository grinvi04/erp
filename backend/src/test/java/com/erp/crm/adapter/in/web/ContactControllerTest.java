package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.ContactCreateRequest;
import com.erp.crm.application.dto.ContactResponse;
import com.erp.crm.application.dto.ContactUpdateRequest;
import com.erp.crm.application.service.ContactService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ContactController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class ContactControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ContactService contactService;

    private ContactResponse buildResponse() {
        return new ContactResponse(1L, 1L, "테스트고객사", "홍", "길동", "대리", "영업팀",
                "hong@test.com", "02-000-0000", "010-0000-0000", true,
                LocalDateTime.now(), 0L);
    }

    private ContactCreateRequest validCreate() {
        return new ContactCreateRequest(1L, "홍", "길동", "대리", "영업팀",
                "hong@test.com", "02-000-0000", "010-0000-0000", true);
    }

    @Test
    void findByAccount_returnsOk() throws Exception {
        given(contactService.findByAccount(1L)).willReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/crm/contacts").param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lastName").value("홍"))
                .andExpect(jsonPath("$.data[0].accountName").value("테스트고객사"));
    }

    @Test
    void findById_existingContact_returnsOk() throws Exception {
        given(contactService.findById(1L)).willReturn(buildResponse());

        mockMvc.perform(get("/api/crm/contacts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("길동"))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        given(contactService.findById(99L))
                .willThrow(new ErpException(ErrorCode.CONTACT_NOT_FOUND));

        mockMvc.perform(get("/api/crm/contacts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CR002"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(contactService.create(any())).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lastName").value("홍"));
    }

    @Test
    void create_blankLastName_returns400() throws Exception {
        ContactCreateRequest invalid = new ContactCreateRequest(1L, "", "길동",
                null, null, null, null, null, false);

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingAccountId_returns400() throws Exception {
        ContactCreateRequest invalid = new ContactCreateRequest(null, "홍", "길동",
                null, null, null, null, null, false);

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        ContactCreateRequest invalid = new ContactCreateRequest(1L, "홍", "길동",
                null, null, "not-an-email", null, null, false);

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_validRequest_returnsOk() throws Exception {
        given(contactService.update(eq(1L), any())).willReturn(buildResponse());
        ContactUpdateRequest req = new ContactUpdateRequest("홍", "길동", null, null,
                null, null, null, true, 0L);

        mockMvc.perform(put("/api/crm/contacts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void delete_existingContact_returns204() throws Exception {
        mockMvc.perform(delete("/api/crm/contacts/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void create_noPermission_returns403() throws Exception {
        given(contactService.create(any()))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/crm/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreate())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("C005"));
    }

    @Test
    void findById_noPermission_returns403() throws Exception {
        given(contactService.findById(1L))
                .willThrow(new ErpException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/crm/contacts/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        willThrow(new ErpException(ErrorCode.FORBIDDEN))
                .given(contactService).delete(1L);

        mockMvc.perform(delete("/api/crm/contacts/1"))
                .andExpect(status().isForbidden());
    }
}
