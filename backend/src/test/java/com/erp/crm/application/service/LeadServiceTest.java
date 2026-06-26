package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.LeadConvertRequest;
import com.erp.crm.application.dto.LeadCreateRequest;
import com.erp.crm.application.dto.LeadResponse;
import com.erp.crm.application.dto.LeadUpdateRequest;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Lead;
import com.erp.crm.domain.model.LeadStatus;
import com.erp.crm.domain.repository.LeadRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private CrmAccountService accountService;
    @Mock private OpportunityService opportunityService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @InjectMocks private LeadService leadService;

    private Lead buildLead() {
        return Lead.of("김", "철수", "ABC주식회사", "팀장", "kim@abc.com",
                "010-0000-0000", "WEB", "sales-001", null);
    }

    @Test
    void create_validRequest_savesWithNewStatus() {
        Lead lead = buildLead();
        given(leadRepository.save(any())).willReturn(lead);

        LeadCreateRequest req = new LeadCreateRequest("김", "철수", "ABC주식회사", "팀장",
                "kim@abc.com", "010-0000-0000", "WEB", "sales-001", null);

        LeadResponse result = leadService.create(req);

        assertThat(result.status()).isEqualTo(LeadStatus.NEW);
        assertThat(result.lastName()).isEqualTo("김");
    }

    @Test
    void convert_notConverted_setsConvertedStatus() {
        Lead lead = buildLead();
        Account account = Account.of("ACC-001", "ABC주식회사", null, null, null, null, null,
                null, null, AccountType.CUSTOMER, "sales-001");

        given(leadRepository.findById(1L)).willReturn(Optional.of(lead));
        given(accountService.getOrThrow(10L)).willReturn(account);

        LeadResponse result = leadService.convert(1L, new LeadConvertRequest(10L, null));

        assertThat(result.status()).isEqualTo(LeadStatus.CONVERTED);
        assertThat(result.convertedAccountId()).isEqualTo(account.getId());
    }

    @Test
    void convert_alreadyConverted_throwsLeadAlreadyConverted() {
        Lead lead = buildLead();
        Account account = Account.of("ACC-001", "ABC주식회사", null, null, null, null, null,
                null, null, AccountType.CUSTOMER, "sales-001");
        lead.convert(account, null);

        given(leadRepository.findById(1L)).willReturn(Optional.of(lead));

        ErpException ex = assertThrows(ErpException.class,
                () -> leadService.convert(1L, new LeadConvertRequest(10L, null)));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEAD_ALREADY_CONVERTED);
    }

    @Test
    void findById_notFound_throwsLeadNotFound() {
        given(leadRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> leadService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEAD_NOT_FOUND);
    }

    @Test
    void update_convertedLead_throwsLeadAlreadyConvertedUpdate() {
        Lead lead = buildLead();
        Account account = Account.of("ACC-001", "ABC주식회사", null, null, null, null, null,
                null, null, AccountType.CUSTOMER, "sales-001");
        lead.convert(account, null);
        ReflectionTestUtils.setField(lead, "version", 0L);
        given(leadRepository.findById(1L)).willReturn(Optional.of(lead));

        LeadUpdateRequest req = new LeadUpdateRequest("김", "철수", "ABC주식회사", "팀장",
                "kim@abc.com", "010-0000-0000", "WEB", "sales-001", null, 0L);

        ErpException ex = assertThrows(ErpException.class, () -> leadService.update(1L, req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LEAD_ALREADY_CONVERTED_UPDATE);
    }
}
