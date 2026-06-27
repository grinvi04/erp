package com.erp.crm.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.AccountCreateRequest;
import com.erp.crm.application.dto.AccountResponse;
import com.erp.crm.application.service.CrmAccountService;
import com.erp.crm.domain.model.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AccountControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private CrmAccountService accountService;

    private AccountResponse buildResponse() {
        return new AccountResponse(1L, "ACC-001", "테스트고객사", null, "IT", null,
                "02-000-0000", "서울시", 100, new BigDecimal("5000000000"),
                AccountType.CUSTOMER, "user-001", true,
                LocalDateTime.now(), LocalDateTime.now(), 0L);
    }

    @Test
    void findById_existingAccount_returnsOk() throws Exception {
        given(accountService.findById(1L)).willReturn(buildResponse());

        mockMvc.perform(get("/api/crm/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ACC-001"))
                .andExpect(jsonPath("$.data.accountType").value("CUSTOMER"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        given(accountService.create(any())).willReturn(buildResponse());

        mockMvc.perform(post("/api/crm/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountCreateRequest(
                                "ACC-001", "테스트고객사", null, "IT", null, "02-000-0000",
                                "서울시", 100, new BigDecimal("5000000000"),
                                AccountType.CUSTOMER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("ACC-001"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(accountService.create(any()))
                .willThrow(new ErpException(ErrorCode.CRM_ACCOUNT_CODE_DUPLICATE));

        mockMvc.perform(post("/api/crm/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AccountCreateRequest(
                                "ACC-001", "테스트고객사", null, null, null, null,
                                null, null, null, AccountType.PROSPECT))))
                .andExpect(status().isConflict());
    }
}
