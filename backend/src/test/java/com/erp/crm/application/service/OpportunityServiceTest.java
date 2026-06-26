package com.erp.crm.application.service;

import com.erp.common.exception.ErpException;
import com.erp.common.exception.ErrorCode;
import com.erp.crm.application.dto.OpportunityCreateRequest;
import com.erp.crm.application.dto.OpportunityResponse;
import com.erp.crm.domain.model.Account;
import com.erp.crm.domain.model.AccountType;
import com.erp.crm.domain.model.Opportunity;
import com.erp.crm.domain.model.PipelineStage;
import com.erp.crm.domain.repository.OpportunityRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private CrmAccountService accountService;
    @Mock private PipelineStageService stageService;
    @Mock private com.erp.common.security.PermissionChecker permissionChecker;
    @Mock private com.erp.common.security.CurrentUserProvider currentUserProvider;
    @InjectMocks private OpportunityService opportunityService;

    private Account buildAccount() {
        return Account.of("ACC-001", "테스트고객사", null, null, null, null, null,
                null, null, AccountType.CUSTOMER, "user-001");
    }

    private PipelineStage buildStage() {
        return PipelineStage.of("탐색", 1, 20, false, false);
    }

    @Test
    void create_validRequest_savesWithCurrentUserAsOwner() {
        Account account = buildAccount();
        PipelineStage stage = buildStage();

        given(accountService.getOrThrow(1L)).willReturn(account);
        given(stageService.getOrThrow(1L)).willReturn(stage);
        given(currentUserProvider.getCurrentUserId()).willReturn("auth-user-sub");
        given(opportunityRepository.save(any(Opportunity.class))).willAnswer(inv -> inv.getArgument(0));

        OpportunityCreateRequest req = new OpportunityCreateRequest(1L, "2026 클라우드 전환", 1L,
                new BigDecimal("50000000"), "KRW", LocalDate.of(2026, 12, 31),
                20, "REFERRAL", "상세설명");

        OpportunityResponse result = opportunityService.create(req);

        assertThat(result.name()).isEqualTo("2026 클라우드 전환");
        assertThat(result.probability()).isEqualTo(20);
        assertThat(result.ownerId()).isEqualTo("auth-user-sub");
    }

    @Test
    void findById_notFound_throwsOpportunityNotFound() {
        given(opportunityRepository.findById(99L)).willReturn(Optional.empty());

        ErpException ex = assertThrows(ErpException.class, () -> opportunityService.findById(99L));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.OPPORTUNITY_NOT_FOUND);
    }
}
