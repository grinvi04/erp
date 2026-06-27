package com.erp.finance.adapter.in.web;

import com.erp.common.config.TestSecurityConfig;
import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.finance.application.dto.AccountCreateRequest;
import com.erp.finance.application.dto.AccountResponse;
import com.erp.finance.application.service.AccountService;
import com.erp.finance.domain.model.AccountType;
import com.erp.finance.domain.model.NormalBalance;
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
    @MockBean private AccountService accountService;

    @Test
    void findAll_returnsOkWithList() throws Exception {
        AccountResponse response = new AccountResponse(1L, "1100", "현금",
            AccountType.ASSET, NormalBalance.DEBIT, null, null, false, true, 0L);
        given(accountService.findAll()).willReturn(List.of(response));

        mockMvc.perform(get("/api/finance/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].code").value("1100"));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        AccountResponse response = new AccountResponse(1L, "1100", "현금",
            AccountType.ASSET, NormalBalance.DEBIT, null, null, false, true, 0L);
        given(accountService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/finance/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new AccountCreateRequest("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.code").value("1100"));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        given(accountService.create(any())).willThrow(new ErpException(ErrorCode.ACCOUNT_CODE_DUPLICATE));

        mockMvc.perform(post("/api/finance/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    new AccountCreateRequest("1100", "현금", AccountType.ASSET, NormalBalance.DEBIT, null, false))))
            .andExpect(status().isConflict());
    }
}
